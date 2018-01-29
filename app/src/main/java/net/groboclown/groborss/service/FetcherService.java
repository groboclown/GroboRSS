/**
 * Sparse rss
 * 
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package net.groboclown.groborss.service;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Exchanger;
import java.util.zip.GZIPInputStream;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Xml;
import net.groboclown.groborss.BASE64;
import net.groboclown.groborss.MainTabActivity;
import net.groboclown.groborss.R;
import net.groboclown.groborss.Strings;
import net.groboclown.groborss.handler.RSSHandler;
import net.groboclown.groborss.provider.FeedData;
import net.groboclown.groborss.util.HttpDownload;

public class FetcherService extends IntentService {
    private static final String TAG = "FetcherService";

	private static final int FETCHMODE_DIRECT = 1;
	
	private static final int FETCHMODE_REENCODE = 2;
	
	private static final String KEY_USERAGENT = "User-agent";
	
	private static final String VALUE_USERAGENT = "Mozilla/5.0";
	
	private static final String CHARSET = "charset=";
	
	private static final String COUNT = "COUNT(*)";
	
	private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
	
	private static final String LINK_RSS = "<link rel=\"alternate\" ";
	
	private static final String LINK_RSS_SLOPPY = "<link rel=alternate "; 
	
	private static final String HREF = "href=\"";
	
	private static final String HTML_BODY = "<body";
	
	private static final String ENCODING = "encoding=\"";
	
	private static final String SERVICENAME = "RssFetcherService";
	
	private static final String ZERO = "0";
	
	private static final String GZIP = "gzip";
	
	private NotificationManager notificationManager;
	
	private static SharedPreferences preferences = null;

	private static class FetchResult {
		final int count;
		final ArrayList<String> feedIds;
		FetchResult(int count, ArrayList<String> feedIds) {
			this.count = count;
			this.feedIds = feedIds;
		}
	}
	
	public FetcherService() {
		super(SERVICENAME);
		HttpURLConnection.setFollowRedirects(true);
	}
		
	@Override
	public void onHandleIntent(Intent intent) {
		if (preferences == null) {
			try {
				preferences = PreferenceManager.getDefaultSharedPreferences(createPackageContext(Strings.PACKAGE, 0));
			} catch (NameNotFoundException e) {
				preferences = PreferenceManager.getDefaultSharedPreferences(FetcherService.this);
			}
		}
		
		if (intent.getBooleanExtra(Strings.SCHEDULED, false)) {
			SharedPreferences.Editor editor = preferences.edit();
			editor.putLong(Strings.PREFERENCE_LASTSCHEDULEDREFRESH, SystemClock.elapsedRealtime());
			editor.apply();
		}
		
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		
		if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
            FetchResult updates = FetcherService.refreshFeedsStatic(FetcherService.this, intent.getStringExtra(Strings.FEEDID), networkInfo, intent.getBooleanExtra(Strings.SETTINGS_OVERRIDEWIFIONLY, false) || preferences.getBoolean(Strings.SETTINGS_OVERRIDEWIFIONLY, false));

            if (updates.count > 0) {
				if (preferences.getBoolean(Strings.SETTINGS_NOTIFICATIONSENABLED, false)) {
					Cursor cursor = getContentResolver().query(FeedData.EntryColumns.CONTENT_URI, new String[] {COUNT}, FeedData.EntryColumns.READDATE + Strings.DB_ISNULL, null, null);

                    int newCount;
                    if (cursor == null) {
                        newCount = 0;
                    } else {
                        cursor.moveToFirst();
                        newCount = cursor.getInt(0);
                        cursor.close();
                    }

					String text = String.valueOf(newCount) + ' ' + getString(R.string.newentries);

					Intent notificationIntent = new Intent(FetcherService.this, MainTabActivity.class);
							
					PendingIntent contentIntent = PendingIntent.getActivity(FetcherService.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    StringBuilder ids = new StringBuilder();
                    for( String id : updates.feedIds ) {
                        ids.append(",").append(id);
                    }
                    String idList = ids.toString().substring(1);

                    // get the ringtone of the feed
                    // returns an empty cursor, if feed does not override the global one or is silent
                    Cursor ringCursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI,
                            new String[] {FeedData.FeedColumns.ALERT_RINGTONE},
                            FeedData.FeedColumns.OTHER_ALERT_RINGTONE+" = 1"
                            + " and "+FeedData.FeedColumns._ID+" IN("+idList+")",
                            null, null);

                    String ringtone = null;
                    if (ringCursor != null) {
                        while ((ringtone == null || ringtone.length() == 0) && ringCursor.moveToNext()) { // this one has set custom ringtone to silence, check next
                            ringtone = ringCursor.getString(0);
                        }

                        if ((ringtone == null || ringtone.length() == 0) && updates.feedIds.size() != ringCursor.getCount()) { // at least one not overridden but the others were all silence
                            ringtone = preferences.getString(Strings.SETTINGS_NOTIFICATIONSRINGTONE, null);
                        }
                        ringCursor.close();
                    }
                    if (ringtone == null || ! ringtone.isEmpty()) {
                        ringtone = preferences.getString(Strings.SETTINGS_NOTIFICATIONSRINGTONE, null);
                    }

                    // Notifications: updating info on existing notifications is deprecated, so instead
                    // we just send new ones each time.  The notification manager handles preventing
                    // duplicates of the same event.
                    Notification notification =
                            //new Notification(R.drawable.ic_statusbar_rss, text, System.currentTimeMillis());
                            // notification.setLatestEventInfo(FetcherService.this, getString(R.string.rss_feeds), text, contentIntent);
                            new NotificationCompat.Builder(FetcherService.this, getString(R.string.rss_feeds))
                                    .setSmallIcon(R.drawable.ic_statusbar_rss)
                                    .setTicker(text)
                                    .setShowWhen(true)
                                    .setAutoCancel(true)
                                    .setLights(0xffffffff, 300, 1000)
                                    .setSound(
                                            ringtone != null && ! ringtone.isEmpty()
                                            ? Uri.parse(ringtone)
                                            : null
                                    )
                                    .setDefaults(
                                            preferences.getBoolean(Strings.SETTINGS_NOTIFICATIONSVIBRATE, false)
                                                    ? Notification.DEFAULT_VIBRATE
                                                    : 0
                                    )
                                    .setContentIntent(contentIntent)
                                    .build();
					notificationManager.notify(0, notification);
				} else {
					notificationManager.cancel(0);
				}
			}
		} 
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
	public void onDestroy() {
		if (MainTabActivity.INSTANCE != null)
			MainTabActivity.INSTANCE.setProgressBarIndeterminateVisibility(false);
		super.onDestroy();
	}
	
	private static FetchResult refreshFeedsStatic(Context context, String feedId, NetworkInfo networkInfo, boolean overrideWifiOnly) {
		String selection = null;
		
		if (!overrideWifiOnly && networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
			selection = FeedData.FeedColumns.WIFIONLY + "=0 or " + FeedData.FeedColumns.WIFIONLY + " IS NULL"; // "IS NOT 1" does not work on 2.1
		}

		Cursor cursor = context.getContentResolver().query(feedId == null ? FeedData.FeedColumns.CONTENT_URI : FeedData.FeedColumns.CONTENT_URI(feedId), null, selection, null, null); // no managed query here
		
		int urlPosition = cursor.getColumnIndex(FeedData.FeedColumns.URL);
		
		int idPosition = cursor.getColumnIndex(FeedData.FeedColumns._ID);
		
		int lastUpdatePosition = cursor.getColumnIndex(FeedData.FeedColumns.REALLASTUPDATE);
		
		int titlePosition = cursor.getColumnIndex(FeedData.FeedColumns.NAME);
		
		int fetchmodePosition = cursor.getColumnIndex(FeedData.FeedColumns.FETCHMODE);
		
		int iconPosition = cursor.getColumnIndex(FeedData.FeedColumns.ICON);

		// int homepagePosition = cursor.getColumnIndex(FeedData.FeedColumns.HOMEPAGE);

		int entryLinkImgPattern = cursor.getColumnIndex(FeedData.FeedColumns.ENTRY_LINK_IMG_PATTERN);
		
		int skipAlertPosition = cursor.getColumnIndex(FeedData.FeedColumns.SKIP_ALERT);

		int result = 0;
		ArrayList<String> ids = new ArrayList<>();
		boolean updateWidget = false;
		
		RSSHandler handler = new RSSHandler(context);
        HttpDownload.Factory connectionFactory = HttpDownload.setup(context);

		handler.setEfficientFeedParsing(preferences.getBoolean(Strings.SETTINGS_EFFICIENTFEEDPARSING, true));
		handler.setFetchImages(preferences.getBoolean(Strings.SETTINGS_FETCHPICTURES, false));
        handler.setHttpDownloadFactory(connectionFactory);

		while (cursor.moveToNext()) {
			String id = cursor.getString(idPosition);

			// TODO This is a big hack
			handler.setEntryLinkImagePattern(cursor.getString(entryLinkImgPattern));

			HttpDownload connection = null;

			try {
				String feedUrl = cursor.getString(urlPosition);

				connection = connectionFactory.connect(feedUrl);
                if (connection == null) {
                    continue;
                }

				int fetchMode = cursor.getInt(fetchmodePosition);
				
				handler.init(new Date(cursor.getLong(lastUpdatePosition)), id, cursor.getString(titlePosition), feedUrl);
				if (fetchMode == 0) {
					if (connection.isHtmlDocument()) {
						BufferedReader reader = connection.getAsReader();
						
						String line;
						
						int pos, posStart = -1;
						
						while ((line = reader.readLine()) != null) {
							if (line.contains(HTML_BODY)) {
								break;
							} else {
								pos = line.indexOf(LINK_RSS);
								
								if (pos == -1) {
									pos = line.indexOf(LINK_RSS_SLOPPY);
								}
								if (pos > -1) {
									posStart = line.indexOf(HREF, pos);

									if (posStart > -1) {
										String url = line.substring(posStart+6, line.indexOf('"', posStart+10)).replace(Strings.AMP_SG, Strings.AMP);
										
										ContentValues values = new ContentValues();
										
										if (url.startsWith(Strings.SLASH)) {
											int index = feedUrl.indexOf('/', 8);
											
											if (index > -1) {
												url = feedUrl.substring(0, index)+url;
											} else {
												url = feedUrl+url;
											}
										} else if (!url.startsWith(Strings.HTTP) && !url.startsWith(Strings.HTTPS)) {
											url = feedUrl + '/' + url;
										}
										values.put(FeedData.FeedColumns.URL, url);
										context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
										connection.disconnect();
										connection = connectionFactory.connect(url);
										break;
									}
								}
							}
						}
						if (posStart == -1) { // this indicates a badly configured feed
							connection = connection.reset();
						}
					}

					if (connection.isXmlEncodingSupported()) {
						fetchMode = FETCHMODE_DIRECT;
					} else {
						fetchMode = FETCHMODE_REENCODE;
					}

					ContentValues values = new ContentValues();
					
					values.put(FeedData.FeedColumns.FETCHMODE, fetchMode); 
					context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				}
				
				/* check and optionally find favicon */
				byte[] iconBytes = cursor.getBlob(iconPosition);
				
				if (iconBytes == null) {
					HttpDownload iconURLConnection = connection.getFaviconConnection();
					
					try {
						iconBytes = iconURLConnection.getAsBytes();
						ContentValues values = new ContentValues();
						
						values.put(FeedData.FeedColumns.ICON, iconBytes); 
						context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
					} catch (Exception e) {
						ContentValues values = new ContentValues();
						
						values.put(FeedData.FeedColumns.ICON, new byte[0]); // no icon found or error
						context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
					} finally {
						iconURLConnection.disconnect();
					}
					
				}
				switch (fetchMode) {
					default:
					case FETCHMODE_DIRECT: {
                        String encoding = connection.getEncodingCharset(true);
                        if (encoding != null) {
                        	// FIXME getting an error in the form BufferedInputStream is closed
							// from setInputStream().
                            InputStream inputStream = connection.getAsInputStream();
                            handler.setInputStream(inputStream);
                            try {
                                Xml.parse(inputStream, Xml.findEncodingByName(encoding), handler);
                            } catch (Exception e) {
                                Log.i(TAG, "Failed to read XML from " + feedUrl, e);
                                throw e;
                            }
						} else {
                            BufferedReader reader = connection.getAsReader();
							
							handler.setReader(reader);
                            try {
                                Xml.parse(reader, handler);
                            } catch (Exception e) {
                                Log.i(TAG, "Failed to read XML from " + feedUrl, e);
                                throw e;
                            }
						}
						break;
					}
					case FETCHMODE_REENCODE: {
                        StringReader reader = new StringReader(connection.getAsString(false));
                        handler.setReader(reader);
                        try {
                            Xml.parse(reader, handler);
                        } catch (Exception e) {
                            Log.i(TAG, "Failed to read XML from " + feedUrl, e);
                            throw e;
                        }
						break;
					}
				}
				connection.disconnect();
			} catch (FileNotFoundException e) {
				if (!handler.isDone() && !handler.isCancelled()) {
					ContentValues values = new ContentValues();
					values.put(FeedData.FeedColumns.FETCHMODE, 0); // resets the fetchmode to determine it again later
					values.put(FeedData.FeedColumns.ERROR, context.getString(R.string.error_feederror));
					context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				}
			} catch (Throwable e) {
				if (!handler.isDone() && !handler.isCancelled()) {
					ContentValues values = new ContentValues();
					values.put(FeedData.FeedColumns.FETCHMODE, 0); // resets the fetchmode to determine it again later
					values.put(FeedData.FeedColumns.ERROR, e.getMessage());
					context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, null, null);
				} 
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
			if (cursor.getInt(skipAlertPosition) != 1) {
				result += handler.getNewCount();
				if (handler.getNewCount() > 0) {
					ids.add(handler.getId());
				}
			}
			
			if (!updateWidget && handler.getNewCount() > 0) {
				updateWidget = true;
			}
		}
		cursor.close();
		
		if (updateWidget) {
			context.sendBroadcast(new Intent(Strings.ACTION_UPDATEWIDGET));
		}
		return new FetchResult(result, ids);
	}
}
