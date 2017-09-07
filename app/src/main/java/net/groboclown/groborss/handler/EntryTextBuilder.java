/**
 * GroboRSS
 *
 * Copyright (c) 2017 Groboclown
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

import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.webkit.WebView;

import net.groboclown.groborss.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntryTextBuilder {
    private static final String TEXT_HTML = "text/html";
    private static final String UTF8 = "utf-8";
    private static final String CSS = "<head><style type=\"text/css\">body {max-width: 100%}\nimg {max-width: 100%; height: auto;}\ndiv[style] {max-width: 100%;}\npre {white-space: pre-wrap;}</style></head>";
    private static final String FONT_START = CSS+"<body link=\"#97ACE5\" text=\"#C0C0C0\">";
    private static final String FONT_FONTSIZE_START = CSS+"<body link=\"#97ACE5\" text=\"#C0C0C0\"><font size=\"+";
    private static final String FONTSIZE_START = "<font size=\"+";
    private static final String FONTSIZE_MIDDLE = "\">";
    private static final String FONTSIZE_END = "</font>";
    private static final String FONT_END = "</font><br/><br/><br/><br/></body>";
    private static final String BODY_START = "<body>";
    private static final String BODY_END = "<br/><br/><br/><br/></body>";

    private String mEntryId;
    private Uri mUri;
    private SharedPreferences mPreferences;
    private String mAbstractText;
    private boolean mHasImages = false;
    private boolean mRendered = false;

    public EntryTextBuilder withEntryId(String entryId) {
        this.mEntryId = entryId;
        return this;
    }

    public EntryTextBuilder withUri(Uri uri) {
        this.mUri = uri;
        return this;
    }

    public EntryTextBuilder withAbstractText(String abstractText) {
        this.mAbstractText = abstractText;
        return this;
    }

    public EntryTextBuilder withPreferences(SharedPreferences preferences) {
        this.mPreferences = preferences;
        return this;
    }

    public void render(WebView webView, View content, boolean isLightColorMode) {
        if (mRendered || mAbstractText == null || mPreferences == null || mUri == null) {
            throw new IllegalStateException();
        }
        StringBuilder renderedText = prepare();

        if (mPreferences.getBoolean(Strings.SETTINGS_DISABLEPICTURES, false)) {
            webView.getSettings().setBlockNetworkImage(true);
        } else {
            if (webView.getSettings().getBlockNetworkImage()) {
						/*
						 * setBlockNetwortImage(false) calls postSync, which takes time,
						 * so we clean up the html first and change the value afterwards
						 */
                webView.loadData(Strings.EMPTY, TEXT_HTML, UTF8);
                webView.getSettings().setBlockNetworkImage(false);
            }
        }

        int fontsize = Integer.parseInt(mPreferences.getString(Strings.SETTINGS_FONTSIZE, Strings.ONE));
        /*
        if (abstractText.indexOf('<') > -1 && abstractText.indexOf('>') > -1) {
            abstractText = abstractText.replace(NEWLINE, BR);
        }
        */

        if (isLightColorMode) {
            if (fontsize > 0) {
                webView.loadDataWithBaseURL(null, CSS + FONTSIZE_START + fontsize + FONTSIZE_MIDDLE + renderedText + FONTSIZE_END, TEXT_HTML, UTF8, null);
            } else {
                webView.loadDataWithBaseURL(null, CSS + BODY_START + renderedText + BODY_END, TEXT_HTML, UTF8, null);
            }
            webView.setBackgroundColor(Color.WHITE);
            content.setBackgroundColor(Color.WHITE);
        } else {
            if (fontsize > 0) {
                webView.loadDataWithBaseURL(null, FONT_FONTSIZE_START + fontsize + FONTSIZE_MIDDLE + renderedText + FONT_END, TEXT_HTML, UTF8, null);
            } else {
                webView.loadDataWithBaseURL(null, FONT_START + renderedText + BODY_END, TEXT_HTML, UTF8, null);
            }
            webView.setBackgroundColor(Color.BLACK);
            content.setBackgroundColor(Color.BLACK);
        }

        mRendered = true;
    }


    public boolean hasImages() {
        if (!mRendered) {
            throw new IllegalStateException();
        }
        return mHasImages;
    }


    private static final Pattern ENDLINE_PATTERN = Pattern.compile(
            "<((br)|p)(\\s[^/>]*)?/?>"
    );

    StringBuilder prepare() {
        boolean disablePictures = mPreferences.getBoolean(Strings.SETTINGS_DISABLEPICTURES, false);

        // First up, perform the bbcode replacements before any HTML stuff takes place.
        String text = convertBBCode(mAbstractText);

        // Initial \r\n -> <br>, but only if the text doesn't include <br> or <p> already.
        Matcher brM = ENDLINE_PATTERN.matcher(text);
        if(!brM.find()) {
            text = text.replaceAll("(\n+)|(\r+)|((\r\n)+)", "<br>");
        }

        // Reset our image storage flag
        mHasImages = false;

        // Next, parse the HTML so we can muck about with it.
        List<SimpleHtmlParser.HtmlBit> htmlBits = SimpleHtmlParser.parse(text);
        List<SimpleHtmlParser.HtmlBit> finalBits = new ArrayList<>(htmlBits.size());
        SimpleHtmlParser.HtmlBit prev = null;
        for (int i = 0; i < htmlBits.size(); i++) {
            SimpleHtmlParser.HtmlBit current = htmlBits.get(i);

            if (current.isPlainText()) {
                handlePlainText(current, prev, finalBits);
            } else if (current.isStartTag()) {
                // Special tag handling
                String tag = current.getTag().toLowerCase();
                if ("img".equals(tag)) {
                    // Image tag handling
                    handleImage(current, prev, finalBits, disablePictures);
                } else {
                    // A tag we don't need to mangle; just add it back in.
                    finalBits.add(current);
                }
            } else {
                // End tags are just added in.
                finalBits.add(current);
            }

            prev = current;
        }

        return SimpleHtmlParser.toHtml(finalBits);
    }



    private static final Pattern SIMPLE_URL_PATTERN = Pattern.compile(
            "(?:\\s|^)(https?://[^\\s\\[\\]<>]+)(?:\\s|$)",
            Pattern.CASE_INSENSITIVE);
    void handlePlainText(SimpleHtmlParser.HtmlBit current,
             SimpleHtmlParser.HtmlBit prevBit, List<SimpleHtmlParser.HtmlBit> output) {
        // Note that we can't do a simple "is in" check, because we also need to check
        // the contents if is in.
        List<String> previousTagAttributes = new ArrayList<>();
        if (prevBit != null && prevBit.isHtmlTag()) {
            prevBit.moveToStart();
            while (prevBit.nextAttribute()) {
                String value = prevBit.getCurrentValue();
                if (value != null && !value.isEmpty()) {
                    previousTagAttributes.add(value);
                }
            }
        }

        String text = current.getTag();

        // handle plain text URLs
        Matcher maybeLink = SIMPLE_URL_PATTERN.matcher(text);
        int startPos = 0;
        int nextSearchStart = 0;

        findLoop: while (maybeLink.find(nextSearchStart)) {
            int start = maybeLink.start(1);
            int end = maybeLink.end(1);
            String url = maybeLink.group(1);

            // If the URL is in the previous tag's list of attributes, then assume that this is
            // a link to the URL and we shouldn't enclose it in another link.
            for (String attr : previousTagAttributes) {
                if (attr.contains(url)) {
                    // keep looking
                    nextSearchStart = end;
                    continue findLoop;
                }
            }

            // The previous tag does not have this URL in it, and we know it's a plain text URL,
            // so wrap it up.
            // Note that if we want to add in the future more plain text manipulation, this
            // should stick the pulled out code bits into its own list for later parsing.  That
            // will make adding back into the output ordering difficult, though.

            output.add(SimpleHtmlParser.createSimple(text, startPos, start));
            output.add(SimpleHtmlParser.createSimple("<a href='" + url + "'>" + url + "</a>"));
            startPos = end;
            nextSearchStart = end;
        }

        if (startPos < text.length()) {
            output.add(SimpleHtmlParser.createSimple(text, startPos, text.length()));
        }
    }




    private static final Pattern[] TRACKER_SRC_URL_STYLES = {
            Pattern.compile("/tracking/[^/]*rss-pixel.png\\?")
    };

    private void handleImage(SimpleHtmlParser.HtmlBit current, SimpleHtmlParser.HtmlBit prev, List<SimpleHtmlParser.HtmlBit> output, boolean disablePictures) {
        // Disabled pictures means we don't add the current bit to the output.
        if (disablePictures) {
            return;
        }


        // Remove web bugs
        String src = current.getAttributeValue("src");
        if (mPreferences.getBoolean(Strings.SETTINGS_STRIP_WEB_BUGS, false) && isWebBug(current, src)) {
            // Replace the web bug with fun text.
            // TODO make this a String values.xml entry.
            // but that requires a context.
            output.add(SimpleHtmlParser.createSimple(Strings.BUG_ZAPPED_HTML));
            // Nothing else to do
            return;
        }


        // Cached image management
        if (src.contains(Strings.IMAGEID_REPLACEMENT)) {
            mHasImages = true;
            current.addExtra("src", src.replace(Strings.IMAGEID_REPLACEMENT, mEntryId + Strings.IMAGEFILE_IDSEPARATOR));
        }


        // Show the "alt" and "title" values to the user.
        // This needs to be last, because it inserts the image into the stream on a match.
        String altText = null;
        current.moveToStart();
        while (current.nextAttribute()) {
            String key = current.getCurrentKey();
            if ("alt".equalsIgnoreCase(key)) {
                altText = current.getCurrentValue();
                current.removeCurrent();
            } else if ("title".equalsIgnoreCase(key)) {
                altText = current.getCurrentValue();
                current.removeCurrent();
            }
        }
        if (altText != null && !altText.isEmpty()) {
            // We've stripped out the alt text, now add it as the next element.
            output.add(current);
            output.add(SimpleHtmlParser.createSimple("<br><font color='gray'><smaller><i>" + altText + "</i></smaller></font>"));
            // Because we just processed the tag, we can't continue to other processing.
            return;
        }

        // Completed processing.  The final image can be just added in.
        output.add(current);
    }

    private static boolean isWebBug(SimpleHtmlParser.HtmlBit bit, String srcAttrValue) {
        // Web Bug image size check - the first giveaway.
        String height = bit.getAttributeValue("height");
        String width = bit.getAttributeValue("width");
        if (isWebBugSize(height) && isWebBugSize(width)) {
            return true;
        }

        // Check if the URL matches anything we know.
        if (srcAttrValue != null) {
            for (Pattern trackerStyle : TRACKER_SRC_URL_STYLES) {
                if (trackerStyle.matcher(srcAttrValue).find()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isWebBugSize(String size) {
        if (size == null) {
            return false;
        }
        size = size.toLowerCase();
        return "1".equals(size) || "1px".equals(size);
    }


    // A trivial BBCode converter.  Implemented such that it doesn't
    // interfere with the HTML stuff.
    static String convertBBCode(String src) {
        return src
                .replaceAll("(?i)\\[(/?(b|u|i|s))\\]", "<$1>")
                .replaceAll("(?i)\\[img\\](https?://[^ \n\r\t\\[\\]]+)\\[/img\\]", "<img src='$1'>")
                .replaceAll("(?i)\\[url\\](https?://[^ \n\r\t\\[\\]]+)\\[/url\\]", "<a href='$1'>$1</a>")
                .replaceAll("(?i)\\[code\\]([^\\]]*)\\[/code\\]", "<pre>$1</pre>")
                .replaceAll("(?i)\\[/?(center|color|size|img|url|pre)[^\\]]*\\]", "");
    }



}
