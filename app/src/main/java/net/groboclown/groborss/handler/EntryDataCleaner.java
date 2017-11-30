/*
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

package net.groboclown.groborss.handler;

import android.support.annotation.NonNull;
import android.util.Log;

import net.groboclown.groborss.Strings;
import net.groboclown.groborss.provider.FeedDataContentProvider;
import net.groboclown.groborss.util.HttpDownload;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EntryDataCleaner {
    private static final String LOG_TAG = "EntryDataCleaner";

    // middle () is group 1; s* is important for non-whitespaces; ' also usable
    private static final Pattern IMG_PATTERN = Pattern.compile("<img src=\\s*['\"]([^'\"]+)['\"][^>]*>");

    static String cleanEntry(final String description,
            final String entryLinkString, final List<Pattern> entryLinkImagePattern,
            final HttpDownload.Factory httpDownloadFactory,
            final List<String> downloadImages, boolean fetchImages) {
        String descriptionString = description.trim().replaceAll(Strings.HTML_SPAN_REGEX, Strings.EMPTY);

        if (descriptionString.length() > 0) {
            if (fetchImages) {
                Matcher matcher = IMG_PATTERN.matcher(description);

                while (matcher.find()) {
                    String match = matcher.group(1).replace(Strings.SPACE, Strings.URL_SPACE);

                    downloadImages.add(match);
                    descriptionString = descriptionString.replace(
                            match,
                            Strings.FILEURL
                                    + FeedDataContentProvider.IMAGEFOLDER
                                    + Strings.IMAGEID_REPLACEMENT
                                    + match.substring(match.lastIndexOf('/') + 1));
                }
            }

            String[] imageUrlAndAltText = getLinkedImageUrlAndAltText(entryLinkString, entryLinkImagePattern, httpDownloadFactory);
            StringBuilder addlDescription = new StringBuilder();
            StringBuilder pullSrcText = new StringBuilder();
            if (imageUrlAndAltText[0] != null) {
                // TODO move HTML markup to Strings.
                addlDescription.append("<img src='");
                if (fetchImages) {
                    downloadImages.add(imageUrlAndAltText[0]);
                    addlDescription
                            .append(Strings.FILEURL)
                            .append(FeedDataContentProvider.IMAGEFOLDER)
                            .append(Strings.IMAGEID_REPLACEMENT)
                            .append(
                                    imageUrlAndAltText[0].substring(
                                            imageUrlAndAltText[0].lastIndexOf('/') + 1));
                } else {
                    addlDescription.append(imageUrlAndAltText[0]);
                }
                addlDescription.append("'>");
                pullSrcText.append(
                        "<font color='gray'><smaller><i>image pulled from RSS entry link</i></smaller></font>");
            } else if (!entryLinkImagePattern.isEmpty()) {
                // Want an image for this feed, but didn't find one.
                // Note that official escaping HTML is not supported earlier than v16.
                StringBuilder patternText = new StringBuilder();
                String sep = "";
                for (Pattern pattern : entryLinkImagePattern) {
                    // String patternHtml = Html.escapeHtml(entryLinkImagePattern.pattern());
                    String patternHtml = pattern.pattern()
                            .replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                            // everything else should be mostly fine
                            .trim();
                    if (!patternHtml.isEmpty()) {
                        patternText
                                .append(sep)
                                .append("<tt>")
                                .append(patternHtml)
                                .append("</tt>");
                        sep = ", ";
                    }
                }
                if (patternText.length() > 0) {
                    pullSrcText
                            .append("<font color='gray'><smaller><i>pattern(s) ")
                            .append(patternText)
                            .append(" not found in link</i></smaller></font>");
                }
            }

            if (addlDescription.length() > 0 && pullSrcText.length() > 0) {
                addlDescription.insert(0, "<p>");
                if (imageUrlAndAltText[1] != null) {
                    if (addlDescription.length() > 3) {
                        addlDescription.append("<br>");
                    }
                    addlDescription
                            .append("<font color='gray'><smaller><i>")
                            .append(imageUrlAndAltText[1])
                            .append("</i></smaller></font>");
                }
                if (pullSrcText.length() > 0) {
                    addlDescription
                            .append("<br>")
                            .append(pullSrcText);
                }
                addlDescription.append("</p>");
                descriptionString += addlDescription;
            }
        }
        return descriptionString;
    }

    @NonNull
    private static String[] getLinkedImageUrlAndAltText(
            String entryLinkString,
            List<Pattern> entryLinkImagePattern,
            HttpDownload.Factory httpDownloadFactory) {
        if (!entryLinkImagePattern.isEmpty() && !entryLinkString.isEmpty() &&
                !entryLinkString.isEmpty() && httpDownloadFactory != null) {
            // Fetch the URL at the link and search for the pattern.
            String imageUrl = null;
            String imageAltText = null;
            URL referred = null;
            try {
                HttpDownload connection = httpDownloadFactory.connect(entryLinkString);
                if (connection != null) {
                    referred = connection.getURL();
                    imgSearch:
                    for (SimpleHtmlParser.HtmlBit bit : SimpleHtmlParser.parse(connection.getAsString(false))) {
                        if (bit.isStartTag() && "img".equalsIgnoreCase(bit.getTag())) {
                            String src = bit.getAttributeValue("src");
                            if (src != null) {
                                for (Pattern pattern : entryLinkImagePattern) {
                                    if (pattern.matcher(src).matches()) {
                                        imageUrl = src;
                                        String alt = bit.getAttributeValue("alt");
                                        if (alt == null) {
                                            alt = bit.getAttributeValue("title");
                                        }
                                        if (alt != null) {
                                            alt = alt.trim();
                                            if (alt.isEmpty()) {
                                                alt = null;
                                            }
                                        }
                                        imageAltText = alt;
                                        break imgSearch;
                                    }
                                }
                            }
                        }
                    }

                }
            } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
                Log.w(LOG_TAG, "Problem reading link " + entryLinkString, e);
            }
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("/")) {
                    // relative URL to the hostname
                    imageUrl = referred.getProtocol()
                            + Strings.PROTOCOL_SEPARATOR
                            + referred.getHost()
                            + imageUrl;
                } else if (!imageUrl.contains(Strings.PROTOCOL_SEPARATOR)) {
                    String srcPath = referred.getPath();
                    if (srcPath.endsWith("/")) {
                        imageUrl = referred.getProtocol()
                                + Strings.PROTOCOL_SEPARATOR
                                + referred.getHost()
                                + srcPath
                                + imageUrl;
                    } else {
                        int p = srcPath.lastIndexOf('/');
                        if (p >= 0) {
                            imageUrl = referred.getProtocol()
                                    + Strings.PROTOCOL_SEPARATOR
                                    + referred.getHost()
                                    // include the first and last '/'
                                    + srcPath.substring(0, p + 1)
                                    + imageUrl;
                        } else {
                            // no path for the image.
                            imageUrl = referred.getProtocol()
                                    + Strings.PROTOCOL_SEPARATOR
                                    + referred.getHost()
                                    + '/'
                                    + imageUrl;
                        }
                    }
                }

                return new String[] {
                        imageUrl.replace(Strings.SPACE, Strings.URL_SPACE),
                        imageAltText
                };
            }
        }
        return new String[] { null, null };
    }
}
