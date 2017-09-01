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

import java.util.HashMap;
import java.util.Map;
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
        boolean disablePictures = mPreferences.getBoolean(Strings.SETTINGS_DISABLEPICTURES, false);
        StringBuilder renderedText = prepare(disablePictures);

        if (disablePictures) {
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


    StringBuilder prepare(boolean disablePictures) {
        // loadData does not recognize the encoding without correct html-header

        mHasImages = mAbstractText.contains(Strings.IMAGEID_REPLACEMENT);
        String text = mAbstractText;
        StringBuilder ret = new StringBuilder();

        text = text.replace(Strings.IMAGEID_REPLACEMENT, mEntryId + Strings.IMAGEFILE_IDSEPARATOR);

        // Simple bbcode converter
        // Handle this first.
        text = convertBBCode(text);

        text = convertPlainUrls(text);

        Pattern brP = Pattern.compile("<br[^>]*>");
        Matcher brM = brP.matcher(text);
        if(!brM.find()) {
            text = text.replaceAll("\n", "<br>");
        }

        if (mHasImages) {
            text = text.replace(Strings.IMAGEID_REPLACEMENT, mEntryId + Strings.IMAGEFILE_IDSEPARATOR);
        }

        if (disablePictures) {
            text = text.replaceAll(Strings.HTML_IMG_REGEX, Strings.EMPTY);
        }

        if (mPreferences.getBoolean(Strings.SETTINGS_STRIP_WEB_BUGS, false)) {
            text = stripWebBugs(text);
        }

        // Show alt text...
        text = extractAltText(text);

        // For debugging purposes
        text = text.replaceAll(Strings.HTML_IMG_REGEX, "[$0]");

        ret.append(text);
        return ret;
    }

    // I had a problem, so I used regular expressions.  Now I have two problems.
    private static final Pattern ALT_IMAGE_TAG_PATTERN = Pattern.compile(
            "<img(?:\\s+[A-Z0-9_:-]+(?:\\s*=\\s*(?:(?:'[^']*')|(?:\"[^\"]*\"))))*(\\s+alt=(?:(?:'([^']*)')|(?:\"([^\"]*)\")))(?:\\s+[A-Z0-9_:-]+(?:\\s*=\\s*(?:(?:'[^']*')|(?:\"[^\"]*\"))))*\\s*/?>",
            Pattern.CASE_INSENSITIVE);
    static String extractAltText(String text) {
        Matcher m;
        while ((m = ALT_IMAGE_TAG_PATTERN.matcher(text)).find()) {
            // group 1: ' alt="134"'
            // group 2 or 3: '134'

            String pre = text.substring(0, m.start(0));
            String post = text.substring(m.end(0));
            String imgPre = text.substring(m.start(0), m.start(1));
            String altText = m.group(2);
            if (altText == null) {
                altText = m.group(3);
            }
            String imgPost = text.substring(m.end(1), m.end(0));
            if (altText == null || altText.isEmpty()) {
                // strip out the empty alt text.
                text = pre + imgPre + imgPost + post;
            } else {
                text = pre + imgPre + imgPost + "<br>[<i>" + altText + "</i>]" + post;
            }
        }
        return text;
    }


    private static final Pattern IMAGE_TAG_PATTERN = Pattern.compile("<img(.*?)/?>");
    static String stripWebBugs(String text) {
        Matcher m;
        int nextPos = 0;
        while ((m = IMAGE_TAG_PATTERN.matcher(text)).find(nextPos)) {
            // group 1: attribute text

            nextPos = m.end();

            Map<String, String> attributes = splitAttributes(m.group(1));
            if (matchesWebBug(attributes)) {
                String pre = text.substring(0, m.start(0));
                String post = text.substring(m.end(0));
                // String imgPre = text.substring(m.start(0), m.start(1));
                // String imgPost = text.substring(m.end(1), m.end(0));
                text = pre + "<font color='gray'>[<i>bug zapped!</i>]</font>";
                nextPos = text.length();
                text += post;
            }
        }
        return text;
    }

    private static final Pattern[] TRACKER_URL_STYLES = {
            Pattern.compile("/tracking/[^/]*rss-pixel.png\\?")
    };
    static boolean matchesWebBug(Map<String, String> attributes) {
        String urlText = attributes.get("src");
        if (urlText != null) {
            for (Pattern trackerStyle : TRACKER_URL_STYLES) {
                if (trackerStyle.matcher(urlText).find()) {
                    return true;
                }
            }
        }

        String height = attributes.get("height");
        String width = attributes.get("width");
        if (isWebBugSize(height) && isWebBugSize(width)) {
            return true;
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

    private static Map<String, String> splitAttributes(String text) {
        Map<String, String> ret = new HashMap<>();
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        int state = 0;
        for (char c: text.toCharArray()) {
            switch (state) {
                case 0:
                    // Between attributes
                    if (c == '=') {
                        // weird state.  No key.
                        key.append("(no key)");
                        state = 3;
                    } else if (!Character.isWhitespace(c)) {
                        key.append(c);
                        state = 1;
                    }
                    // else keep searching
                    break;
                case 1:
                    // in a key
                    if (c == '=') {
                        state = 3;
                    } else if (Character.isWhitespace(c)) {
                        state = 2;
                    } else {
                        key.append(c);
                    }
                    break;
                case 2:
                    // space after key
                    if (c == '=') {
                        state = 3;
                    } else if (! Character.isWhitespace(c)) {
                        // key with no value
                        ret.put(key.toString().toLowerCase(), "true");
                        key.setLength(0);
                        key.append(c);
                        state = 1;
                    }
                    // else it's whitespace so keep searching
                    break;
                case 3:
                    // after equals, looking for value start
                    if (c == '\'') {
                        state = 4;
                    } else if (c == '"') {
                        state = 5;
                    } else if (c == '=') {
                        // weird state
                        // ignore the character.
                        state = 3;
                    } else if (! Character.isWhitespace(c)) {
                        state = 6;
                    }
                    // else it's whitespace, so keep searching
                    break;
                case 4:
                    // single quoted value
                    if (c == '\'') {
                        // end of key/value
                        ret.put(key.toString().toLowerCase(), value.toString());
                        key.setLength(0);
                        value.setLength(0);
                        state = 0;
                    } else {
                        value.append(c);
                    }
                    break;
                case 5:
                    // double quoted value
                    if (c == '"') {
                        // end of key/value
                        ret.put(key.toString().toLowerCase(), value.toString());
                        key.setLength(0);
                        value.setLength(0);
                        state = 0;
                    } else {
                        value.append(c);
                    }
                    break;
                case 6:
                    // unquoted value; separated by spaces
                    if (Character.isWhitespace(c)) {
                        // end of key/value
                        ret.put(key.toString().toLowerCase(), value.toString());
                        key.setLength(0);
                        value.setLength(0);
                        state = 0;
                    } else {
                        value.append(c);
                    }
                    break;
            }
        }
        return ret;
    }

    private static final Pattern SIMPLE_URL_PATTERN = Pattern.compile(
            "(?:\\s|^)(https?://[^\\s\\[\\]<>]+)(?:\\s|$)",
            Pattern.CASE_INSENSITIVE);

    static String convertPlainUrls(String text) {
        // Find all the URLs without any wrapping HTML tags.
        Matcher maybeLink = SIMPLE_URL_PATTERN.matcher(text);
        int startPos = 0;
        int nextSearchStart = 0;
        StringBuilder sb = new StringBuilder();
        while (maybeLink.find(nextSearchStart)) {
            int start = maybeLink.start(1);
            int end = maybeLink.end(1);
            if (! isTagAttribute(text, start, end) && ! isInTag(text, start, end)) {
                sb.append(text, startPos, start)
                        .append("<a href='")
                        .append(text, start, end)
                        .append("'>")
                        .append(text, start, end)
                        .append("</a>");
                startPos = end;
                nextSearchStart = end;
            } else {
                nextSearchStart = end;
            }
        }
        sb.append(text, startPos, text.length());
        text = sb.toString();
        return text;
    }


    static boolean isTagAttribute(String src, int start, int end) {
        int prevPos = src.substring(0, start).lastIndexOf('<');
        if (prevPos < 0) {
            // not inside a tag.
            return false;
        }
        String prefix = src.substring(prevPos, start);
        if (prefix.indexOf('>') > 0) {
            // not inside the tag that came before it.
            return false;
        }
        int nextEndPos = src.indexOf('>', end);
        if (nextEndPos <= prevPos) {
            // not an enclosed value.
            return false;
        }
        int nextStartPos = src.indexOf('<', end);
        if (nextStartPos < nextEndPos) {
            // the end '>' happens after a '<', so it's ending a different tag.
            return false;
        }

        // We know that the [start, end) string is within a '<' and '>' block.  Rather than
        // performing complex HTML analysis of this block, we'll just assume that it's within
        // a tag's attributes.  For full reasons why this is quite complex, see the unit tests.
        return true;
    }


    static boolean isInTag(String src, int start, int end) {
        int prevTagStart = src.substring(0, start).lastIndexOf('<');
        if (prevTagStart < 0) {
            // not inside a tag.
            return false;
        }
        int prevTagEnd = src.indexOf('>', prevTagStart);
        if (prevTagEnd >= start) {
            // not enclosed in a tag.
            return false;
        }
        int nextTagStart = src.indexOf('<', end);
        if (nextTagStart < 0) {
            // no tailing tag after this.
            return false;
        }

        // Now we need to ensure that the given URL is referenced inside the start tag.
        // If it's not, then we'll enclose it in another link.  This could get weird if
        // a link points to a different URL than the text.

        String tag = src.substring(prevTagStart, prevTagEnd + 1);
        String url = src.substring(start, end);
        return tag.contains(url);
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
