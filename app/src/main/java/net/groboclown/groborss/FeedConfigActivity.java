/**
 * Sparse rss
 *
 * Copyright (c) 2012 Stefan Handschuh
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

package net.groboclown.groborss;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import net.groboclown.groborss.provider.FeedData;

public class FeedConfigActivity extends Activity {
	private static final String WASACTIVE = "wasactive";
	
	private static final String[] PROJECTION = new String[] {FeedData.FeedColumns.NAME, FeedData.FeedColumns.URL, FeedData.FeedColumns.WIFIONLY, FeedData.FeedColumns.ENTRY_LINK_IMG_PATTERN};
	
	private EditText nameEditText;
	
	private EditText urlEditText;
	
	private CheckBox refreshOnlyWifiCheckBox;

	private EditText entryLinkImgPattern;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.feedsettings);
		setResult(RESULT_CANCELED);
		
		Intent intent = getIntent();
		
		nameEditText = findViewById(R.id.feed_title);
		urlEditText = findViewById(R.id.feed_url);
		refreshOnlyWifiCheckBox = findViewById(R.id.wifionlycheckbox);
		entryLinkImgPattern = findViewById(R.id.feed_entry_link_img_pattern);
			
		if (intent.getAction().equals(Intent.ACTION_INSERT)) {
			setTitle(R.string.newfeed_title);
			restoreInstanceState(savedInstanceState);
			findViewById(R.id.button_ok).setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					String url = urlEditText.getText().toString();
					
					if (!url.startsWith(Strings.HTTP) && !url.startsWith(Strings.HTTPS)) {
						url = Strings.HTTP+url;
					}
					
					Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, null, new StringBuilder(FeedData.FeedColumns.URL).append(Strings.DB_ARG).toString(), new String[] {url}, null);
					
					if (cursor.moveToFirst()) {
						cursor.close();
						Toast.makeText(FeedConfigActivity.this, R.string.error_feedurlexists, Toast.LENGTH_LONG).show();
					} else {
						cursor.close();
						ContentValues values = new ContentValues();
						
						values.put(FeedData.FeedColumns.WIFIONLY, refreshOnlyWifiCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.URL, url);
						values.put(FeedData.FeedColumns.ERROR, (String) null);

						String linkImgPattern = entryLinkImgPattern.getText().toString();
						if (linkImgPattern.trim().length() > 0) {
							values.put(FeedData.FeedColumns.ENTRY_LINK_IMG_PATTERN, linkImgPattern);
						}
						
						String name = nameEditText.getText().toString();
						
						if (! name.trim().isEmpty()) {
							values.put(FeedData.FeedColumns.NAME, name);
						}
						getContentResolver().insert(FeedData.FeedColumns.CONTENT_URI, values);
						setResult(RESULT_OK);
						finish();
					}
				}
			});
		} else {
			setTitle(R.string.editfeed_title);
			
			if (!restoreInstanceState(savedInstanceState)) {
				Cursor cursor = getContentResolver().query(intent.getData(), PROJECTION, null, null, null);
					
				if (cursor.moveToNext()) {
					nameEditText.setText(cursor.getString(0));
					urlEditText.setText(cursor.getString(1));
					refreshOnlyWifiCheckBox.setChecked(cursor.getInt(2) == 1);
					entryLinkImgPattern.setText(cursor.getString(3));
					cursor.close();
				} else {
					cursor.close();
					Toast.makeText(FeedConfigActivity.this, R.string.error, Toast.LENGTH_LONG).show();
					finish();
				}
			}
			findViewById(R.id.button_ok).setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					String url = urlEditText.getText().toString();
					
					Cursor cursor = getContentResolver().query(FeedData.FeedColumns.CONTENT_URI, new String[] {FeedData.FeedColumns._ID}, new StringBuilder(FeedData.FeedColumns.URL).append(Strings.DB_ARG).toString(), new String[] {url}, null);

					if (cursor.moveToFirst() && !getIntent().getData().getLastPathSegment().equals(cursor.getString(0))) {
						cursor.close();
						Toast.makeText(FeedConfigActivity.this, R.string.error_feedurlexists, Toast.LENGTH_LONG).show();
					} else {
						cursor.close();
						ContentValues values = new ContentValues();
						
						if (!url.startsWith(Strings.HTTP) && !url.startsWith(Strings.HTTPS)) {
							url = Strings.HTTP+url;
						}
						values.put(FeedData.FeedColumns.URL, url);
						
						String name = nameEditText.getText().toString();
						String imgPattern = entryLinkImgPattern.getText().toString();
						
						values.put(FeedData.FeedColumns.NAME, name.trim().isEmpty() ? null : name);
						values.put(FeedData.FeedColumns.FETCHMODE, 0);
						values.put(FeedData.FeedColumns.WIFIONLY, refreshOnlyWifiCheckBox.isChecked() ? 1 : 0);
						values.put(FeedData.FeedColumns.ENTRY_LINK_IMG_PATTERN, imgPattern.trim().isEmpty() ? null : imgPattern);
						values.put(FeedData.FeedColumns.ERROR, (String) null);
						getContentResolver().update(getIntent().getData(), values, null, null);
						
						setResult(RESULT_OK);
						finish();
					}
				}
				
			});
			
		}
		
		findViewById(R.id.button_cancel).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private boolean restoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null && savedInstanceState.getBoolean(WASACTIVE, false)) {
			nameEditText.setText(savedInstanceState.getCharSequence(FeedData.FeedColumns.NAME));
			urlEditText.setText(savedInstanceState.getCharSequence(FeedData.FeedColumns.URL));
			refreshOnlyWifiCheckBox.setChecked(savedInstanceState.getBoolean(FeedData.FeedColumns.WIFIONLY));
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(WASACTIVE, true);
		outState.putCharSequence(FeedData.FeedColumns.NAME, nameEditText.getText());
		outState.putCharSequence(FeedData.FeedColumns.URL, urlEditText.getText());
		outState.putBoolean(FeedData.FeedColumns.WIFIONLY, refreshOnlyWifiCheckBox.isChecked());
	}

}
