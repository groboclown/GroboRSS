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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Character.isLetter;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isWhitespace;

/**
 * A parser for the feed entry mText.  It handles simple start/end tag parsing, and attribute
 * parsing.  It does not do bbcode stuff, because
 * we want to keep plain `[xyz]`, and only encode full `[abc][/abc]` and other bbcode
 * markup.
 * <br>
 * The old way of doing things with regular expressions didn't scale.
 */
class SimpleHtmlParser {
    // For memory size reduction.  The HTML element is parsed up into an array
    private static final int HTML_POS_INDEX_TAG_START = 0;
    private static final int HTML_POS_INDEX_TAG_END = 1;
    private static final int HTML_POS_INDEX_ATTRIBUTES_START = 2;
    private static final int HTML_POS_ATTR_INDEX_KEY_START = 0;
    private static final int HTML_POS_ATTR_INDEX_KEY_END = 1;
    private static final int HTML_POS_ATTR_INDEX_VALUE_QUOTE_CHAR_TYPE = 2;
    private static final int HTML_POS_ATTR_INDEX_VALUE_START = 3;
    private static final int HTML_POS_ATTR_INDEX_VALUE_END = 4;
    private static final int HTML_POS_ATTR_SIZE = 5;
    private static final int HTML_POS_ATTR_COUNT = 30;
    private static final int HTML_POS_SIZE = HTML_POS_INDEX_ATTRIBUTES_START + (HTML_POS_ATTR_SIZE * HTML_POS_ATTR_COUNT);

    private static final int QUOTE_CHAR_SINGLE = 0;
    private static final int QUOTE_CHAR_DOUBLE = 1;
    private static final int QUOTE_CHAR_BARE = 2;
    private static final char[] QUOTE_CHAR_TYPES = { '\'', '"', '\'' };


    /** A really simple mText snippet.  It has a single string to minimize memory. */
    static class HtmlBit {
        private boolean mIsTagStart = false;
        private boolean mIsTagEnd = false;
        private final String mText;
        private int[] mHtmlPos = new int[HTML_POS_SIZE];
        private int mAttributeCount = 0;
        private Map<String, String> extraAttributes = null;

        // position starts one before first attribute
        private int mCurrentAttributeIndex = -1;
        private int mCurrentAttributePos = HTML_POS_INDEX_ATTRIBUTES_START - HTML_POS_ATTR_SIZE;

        private String cached = null;

        HtmlBit(String text) {
            this.mText = text;
        }

        boolean isHtmlTag() {
            return mIsTagEnd || mIsTagStart;
        }

        boolean isPlainText() {
            return !mIsTagEnd && !mIsTagStart;
        }

        boolean isStartTag() {
            return mIsTagStart;
        }

        boolean isEndTag() {
            return mIsTagEnd;
        }

        String getTag() {
            return mText.substring(mHtmlPos[HTML_POS_INDEX_TAG_START], mHtmlPos[HTML_POS_INDEX_TAG_END]);
        }

        void moveToStart() {
            mCurrentAttributeIndex = -1;
            mCurrentAttributePos = HTML_POS_INDEX_ATTRIBUTES_START - HTML_POS_ATTR_SIZE;
        }

        boolean nextAttribute() {
            do {
                mCurrentAttributeIndex++;
                mCurrentAttributePos += HTML_POS_ATTR_SIZE;
                // Loop past removed attributes
            } while (mCurrentAttributeIndex < mAttributeCount && mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_KEY_START] <= 0);
            return mCurrentAttributeIndex < mAttributeCount;
        }

        // Note: does not protect against going past the end position
        String getCurrentKey() {
            // should have skipped passed removed attributes, so don't need to check that.
            return mText.substring(
                    mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_KEY_START],
                    mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_KEY_END]);
        }

        // Note: does not protect against going past the end position
        String getCurrentValue() {
            if (mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_VALUE_START] > 0) {
                return mText.substring(
                        mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_VALUE_START],
                        mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_VALUE_END]);
            }
            return null;
        }

        void removeCurrent() {
            cached = null;
            mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_KEY_START] = -1;
        }

        void addExtra(String key, String value) {
            cached = null;
            if (extraAttributes == null) {
                extraAttributes = new HashMap<>();
            }
            extraAttributes.put(key, value);
        }

        String getAttributeValue(String key) {
            String ret;
            if (extraAttributes != null) {
                ret = extraAttributes.get(key);
                if (ret != null) {
                    return ret;
                }
            }
            moveToStart();
            while (nextAttribute()) {
                if (key.equalsIgnoreCase(getCurrentKey())) {
                    return getCurrentValue();
                }
            }
            return null;
        }

        private HtmlBit buildPlainText(int startPos, int endPos) {
            cached = null;
            mHtmlPos[HTML_POS_INDEX_TAG_START] = startPos;
            mHtmlPos[HTML_POS_INDEX_TAG_END] = endPos;
            mIsTagEnd = false;
            mIsTagStart = false;
            return this;
        }

        private HtmlBit buildTag(int startPos, int endPos) {
            cached = null;
            mHtmlPos[HTML_POS_INDEX_TAG_START] = startPos;
            mHtmlPos[HTML_POS_INDEX_TAG_END] = endPos;
            return this;
        }

        private HtmlBit buildOpenTag() {
            cached = null;
            this.mIsTagStart = true;
            return this;
        }

        private HtmlBit buildCloseTag() {
            cached = null;
            this.mIsTagEnd = true;
            return this;
        }

        private HtmlBit buildNextAttributeKey(int keyStartPos, int keyEndPos) {
            cached = null;
            mAttributeCount++;
            mCurrentAttributeIndex++;
            mCurrentAttributePos += HTML_POS_ATTR_SIZE;

            mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_KEY_START] = keyStartPos;
            mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_KEY_END] = keyEndPos;

            // Initialize the value to does-not-exist
            mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_VALUE_START] = -1;
            return this;
        }

        private HtmlBit buildNextAttributeValue(int quoteCharType, int valueStartPos, int valueEndPos) {
            cached = null;
            mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_VALUE_QUOTE_CHAR_TYPE] = quoteCharType;
            mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_VALUE_START] = valueStartPos;
            mHtmlPos[mCurrentAttributePos + HTML_POS_ATTR_INDEX_VALUE_END] = valueEndPos;
            return this;
        }

        private HtmlBit close() {
            // force the cache load
            toString();
            moveToStart();
            return this;
        }

        @Override
        public String toString() {
            if (cached != null) {
                return cached;
            }

            // Carefully assemble the HTML element.
            if (!mIsTagStart && !mIsTagEnd) {
                // not an HTML tag, but plain mText set on the tag positions.
                cached = mText.substring(mHtmlPos[HTML_POS_INDEX_TAG_START], mHtmlPos[HTML_POS_INDEX_TAG_END]);
                return cached;
            }
            if (mIsTagEnd && !mIsTagStart) {
                // Just an end tag.
                cached = new StringBuilder("</")
                        .append(mText, mHtmlPos[HTML_POS_INDEX_TAG_START], mHtmlPos[HTML_POS_INDEX_TAG_END])
                        .append('>').toString();
                return cached;
            }
            // It's a tag start.
            StringBuilder ret = new StringBuilder()
                    .append('<')
                    .append(mText, mHtmlPos[HTML_POS_INDEX_TAG_START], mHtmlPos[HTML_POS_INDEX_TAG_END]);
            for (int i = 0, p = HTML_POS_INDEX_ATTRIBUTES_START; i < mAttributeCount; i++, p += HTML_POS_ATTR_SIZE) {
                if (mHtmlPos[p + HTML_POS_ATTR_INDEX_KEY_START] > 0) {
                    String key = mText.substring(mHtmlPos[p + HTML_POS_ATTR_INDEX_KEY_START], mHtmlPos[p + HTML_POS_ATTR_INDEX_KEY_END]);
                    if (extraAttributes == null || ! extraAttributes.containsKey(key)) {
                        ret
                                .append(' ')
                                .append(key);
                        if (mHtmlPos[p + HTML_POS_ATTR_INDEX_VALUE_START] > 0) {
                            ret
                                    .append('=')
                                    .append(QUOTE_CHAR_TYPES[mHtmlPos[p + HTML_POS_ATTR_INDEX_VALUE_QUOTE_CHAR_TYPE]])
                                    .append(mText, mHtmlPos[p + HTML_POS_ATTR_INDEX_VALUE_START], mHtmlPos[p + HTML_POS_ATTR_INDEX_VALUE_END])
                                    .append(QUOTE_CHAR_TYPES[mHtmlPos[p + HTML_POS_ATTR_INDEX_VALUE_QUOTE_CHAR_TYPE]]);
                        }
                    }
                }
                // else it's a removed key/value
            }
            if (extraAttributes != null) {
                for (Map.Entry<String, String> entry : extraAttributes.entrySet()) {
                    ret
                        .append(' ')
                        .append(entry.getKey())
                        .append('=')
                        .append('"')
                        .append(entry.getValue().replace("\"", "&quot;"))
                        .append('"');
                }
            }
            if (mIsTagEnd) {
                ret.append('/');
            }
            ret.append('>');
            cached = ret.toString();
            return cached;
        }

        @Override
        public boolean equals(Object o) {
            return !(o == null || !(o instanceof HtmlBit)) && toString().equals(o.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }


    static StringBuilder toHtml(List<HtmlBit> bits) {
        StringBuilder ret = new StringBuilder();
        for (HtmlBit bit : bits) {
            ret.append(bit.toString());
        }
        return ret;
    }


    /** Create an unparsed bit; you can include HTML in here. */
    static HtmlBit createSimple(String text) {
        return new HtmlBit(text).buildPlainText(0, text.length()).close();
    }


    /** Create an unparsed bit; you can include HTML in here. */
    static HtmlBit createSimple(String text, int start, int end) {
        return new HtmlBit(text).buildPlainText(start, end).close();
    }


    static List<HtmlBit> parse(String text) {
        ParseState state = ParseState.PLAIN_TEXT;
        List<HtmlBit> ret = new ArrayList<>();
        HtmlBit current = new HtmlBit(text);
        int textStartPos = 0;
        int bitStartPos = 0;
        char[] cbuff = text.toCharArray();
        int len = cbuff.length;
        for (int pos = 0; pos < len; pos++) {
            char c = cbuff[pos];
            switch (state) {
                case PLAIN_TEXT: {
                    if (c == '<') {
                        // switch to looking for a tag.
                        state = ParseState.LESS_THAN;
                        if (pos > textStartPos + 1) {
                            // There's plain text to add.
                            ret.add(current.buildPlainText(textStartPos, pos).close());
                            current = new HtmlBit(text);
                        }
                        textStartPos = pos;
                    }
                    // else keep searching
                    break;
                }
                case LESS_THAN: {
                    // Found a '<' to start a tag.
                    if (c == '<') {
                        // another embedded mark.  Keep the stuff we're searching with as plain mText.
                        // And keep ourselves looking for a tag start.
                        if (pos > textStartPos + 1) {
                            // There's plain text to add.
                            ret.add(current.buildPlainText(textStartPos, pos).close());
                            current = new HtmlBit(text);
                        }
                        textStartPos = pos;
                    } else if (c == '/') {
                        // a </ mark.
                        state = ParseState.LESS_THAN_SLASH;
                    } else if (isLetter(c)) {
                        // Start of an open tag
                        bitStartPos = pos;
                        current.buildOpenTag();
                        state = ParseState.TAG_NAME;
                    } else {
                        // We don't allow for "< img" style tags, as that's not valid HTML.
                        // Also, anything else after < is ignored.
                        // No, we're not dealing with <! and <? and <[ marks.
                        // We can safely switch back to plain mText parsing without having to
                        // muck about with the html bit.
                        state = ParseState.PLAIN_TEXT;
                    }
                    break;
                }
                case LESS_THAN_SLASH: {
                    // '</', so start of an end tag.
                    // Search for mText.  Anything else means this is garbage.
                    if (isLetter(c)) {
                        current.buildCloseTag();
                        // This is kind of wrong; it allows for end tags with attributes.
                        // But it saves us from extra parsing logic.  And with this parser,
                        // we really don't care if you try something weird like
                        // <a href="abc"></a src="1234">, or like </a/>
                        state = ParseState.TAG_NAME;
                        bitStartPos = pos;
                    } else if (c == '<') {
                        // weird state, assume this "tag" is actually plain text.
                        ret.add(current.buildPlainText(textStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos;
                        state = ParseState.LESS_THAN;
                    } else if (! isWhitespace(c)) {
                        // Something other than a character, so like </].  We don't handle these.
                        // Switch back to plain mText without an issue.  The buildPlainText will
                        // clear out the tag start / end settings.
                        state = ParseState.PLAIN_TEXT;
                    }
                    // else keep searching for the ending tag name.
                    break;
                }
                case TAG_NAME: {
                    // We're either in a start or disconnect tag, reading in characters of the tag name.
                    if (isWhitespace(c)) {
                        // Found the end of the tag.  Start looking for attributes or tag end.
                        current.buildTag(bitStartPos, pos);
                        state = ParseState.BETWEEN_ATTRIBUTES;
                    } else if (c == '/') {
                        // possible end tag start.  Ends the name of the tag, though.
                        current.buildTag(bitStartPos, pos);
                        state = ParseState.TAG_SLASH;
                    } else if (c == '>') {
                        // end of the tag.  Assume that we'll start plain mText after this.
                        ret.add(current.buildTag(bitStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos + 1;
                        state = ParseState.PLAIN_TEXT;
                    } else if (c == '<') {
                        // weird state, assume this "tag" is actually plain text.
                        ret.add(current.buildPlainText(textStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos;
                        state = ParseState.LESS_THAN;
                    } else if (c == ':' || isLetterOrDigit(c) || c == '$' || c == '_' || c == '-') {
                        // valid tag characters.  Keep searching.
                    } else {
                        // Invalid tag character.  This is garbage, and assume that we've been plain mText
                        // all along.  Even ignore the possible bbcode start.
                        bitStartPos = textStartPos;
                        state = ParseState.PLAIN_TEXT;
                    }
                    break;
                }
                case TAG_SLASH: {
                    // Found a '/' within a tag, outside an attribute.
                    if (c == '>') {
                        // valid end-of-tag
                        ret.add(current.buildCloseTag().close());
                        current = new HtmlBit(text);
                        textStartPos = pos + 1;
                        state = ParseState.PLAIN_TEXT;
                    } else if (c == '<') {
                        // weird state, assume this "tag" is actually plain text.
                        ret.add(current.buildPlainText(textStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos;
                        state = ParseState.LESS_THAN;
                    } else if (isWhitespace(c)) {
                        // Nope, not a closing tag.  Assume this is a bit of garbage in the
                        // middle of a tag that we'll ignore, and go back to attribute
                        // key discovery.
                        state = ParseState.BETWEEN_ATTRIBUTES;
                    } else {
                        // Nope, not a closing tag.  Assume this is some garbage
                        // trying to be an attribute key.  This ends up being a bit
                        // of a look-ahead, because we set our bit start to where the
                        // slash was.  Even if this is a '<'.
                        bitStartPos = pos - 1;
                        state = ParseState.ATTRIBUTE_KEY;
                    }

                    break;
                }
                case BETWEEN_ATTRIBUTES: {
                    // Whitespace after a tag name, or after a closing attribute value.
                    if (c == '/') {
                        // possible end tag start.
                        state = ParseState.TAG_SLASH;
                    } else if (c == '>') {
                        // disconnect of tag.
                        ret.add(current.close());
                        current = new HtmlBit(text);
                        textStartPos = pos + 1;
                        state = ParseState.PLAIN_TEXT;
                    } else if (c == '<') {
                        // weird state, assume this "tag" is actually plain text.
                        ret.add(current.buildPlainText(textStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos;
                        state = ParseState.LESS_THAN;
                    } else if (!isWhitespace(c)) {
                        // Start of attribute key.  We'll be lenient and allow any character
                        // to start a key.
                        bitStartPos = pos;
                        state = ParseState.ATTRIBUTE_KEY;
                    }
                    // else it's whitespace; keep scanning.
                    break;
                }
                case ATTRIBUTE_KEY: {
                    if (c == '/') {
                        // a slash right after a key mText.  Should be the start of
                        // a tag end marker, but if not, then it's kind of garbage, so we don't
                        // need to worry about keeping the / with the key mText.
                        current.buildNextAttributeKey(bitStartPos, pos);
                        state = ParseState.TAG_SLASH;
                    } else if (c == '>') {
                        // end of the tag.
                        ret.add(current.buildNextAttributeKey(bitStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos + 1;
                        state = ParseState.PLAIN_TEXT;
                    } else if (c == '=') {
                        // start of an attribute
                        current.buildNextAttributeKey(bitStartPos, pos);
                        state = ParseState.ATTRIBUTE_AFTER_EQUALS;
                    } else if (c == '<') {
                        // weird state, assume this "tag" is actually plain text.
                        ret.add(current.buildPlainText(textStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos;
                        state = ParseState.LESS_THAN;
                    } else if (isWhitespace(c)) {
                        // end of the tag, start scanning for another key or '='.
                        current.buildNextAttributeKey(bitStartPos, pos);
                        state = ParseState.ATTRIBUTE_AFTER_KEY;
                    }
                    // else keep scanning through the key.  Again, we'll be lenient and allow
                    // all kinds of garbage for the key.
                    break;
                }
                case ATTRIBUTE_AFTER_KEY: {
                    // We're scanning through whitespace after a key.
                    if (c == '/') {
                        // a slash right after a key mText.  Should be the start of
                        // a tag end marker, but if not, then it's kind of garbage, so we don't
                        // need to worry about keeping the / with the key mText.
                        state = ParseState.TAG_SLASH;
                    } else if (c == '>') {
                        // end of the tag.
                        ret.add(current.close());
                        current = new HtmlBit(text);
                        textStartPos = pos + 1;
                        state = ParseState.PLAIN_TEXT;
                    } else if (c == '=') {
                        // start of an attribute
                        state = ParseState.ATTRIBUTE_AFTER_EQUALS;
                    } else if (c == '<') {
                        // weird state, assume this "tag" is actually plain text.
                        ret.add(current.buildPlainText(textStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos;
                        state = ParseState.LESS_THAN;
                    } else if (!isWhitespace(c)) {
                        // start of the next key
                        bitStartPos = pos;
                        state = ParseState.ATTRIBUTE_KEY;
                    }
                    // else keep scanning for the next thing after the key.

                    break;
                }
                case ATTRIBUTE_AFTER_EQUALS: {
                    // Found a key and the equals after the key.  Now we're looking for the
                    // value start.
                    if (c == '/') {
                        // Unquoted slash.  This means a no-value attribute and a tail tag.
                        // Go directly to slash parsing.  This may not be 100% accurate, but it's
                        // disconnect enough.
                        state = ParseState.TAG_SLASH;
                    } else if (c == '>') {
                        // unquoted end-of-tag mark instead of value.  Interpreted the same as
                        // the unquoted slash.
                        ret.add(current.close());
                        current = new HtmlBit(text);
                        textStartPos = pos + 1;
                        state = ParseState.PLAIN_TEXT;
                    } else if (c == '<') {
                        // weird state, assume this "tag" is actually plain text.
                        ret.add(current.buildPlainText(textStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos;
                        state = ParseState.LESS_THAN;
                    } else if (c == '\'') {
                        // single quoted attribute value.  Don't start the value at this
                        // quote mark, but at the next position, so cheat a little.
                        bitStartPos = pos + 1;
                        state = ParseState.ATTRIBUTE_VALUE_SINGLE_QUOTE;
                    } else if (c == '"') {
                        // double quoted attribute value.  Same cheat to not include the quote mark.
                        bitStartPos = pos + 1;
                        state = ParseState.ATTRIBUTE_VALUE_DOUBLE_QUOTE;
                    } else if (c != '=' && ! isWhitespace(c)) {
                        // Something else that looks like a bare attribute value.
                        bitStartPos = pos;
                        state = ParseState.ATTRIBUTE_VALUE_BARE;
                    }
                    // else an equal mark or whitespace.  Keeps scanning.
                    break;
                }
                case ATTRIBUTE_VALUE_SINGLE_QUOTE: {
                    // Only terminate the attribute with another single quote mark.
                    if (c == '\'') {
                        current.buildNextAttributeValue(QUOTE_CHAR_SINGLE, bitStartPos, pos);
                        // really, we need whitespace here, but whatever.
                        state = ParseState.BETWEEN_ATTRIBUTES;
                    }
                    // else keep scanning.
                    break;
                }
                case ATTRIBUTE_VALUE_DOUBLE_QUOTE: {
                    // Only terminate the attribute with another double quote mark.
                    if (c == '"') {
                        current.buildNextAttributeValue(QUOTE_CHAR_DOUBLE, bitStartPos, pos);
                        // really, we need whitespace here, but whatever.
                        state = ParseState.BETWEEN_ATTRIBUTES;
                    }
                    // else keep scanning.
                    break;
                }
                case ATTRIBUTE_VALUE_BARE: {
                    // Terminate at whitespace, slash, or end-tag.
                    // I think browsers use a look-ahead in the case of a slash, but we'll skip that.
                    if (c == '/') {
                        current.buildNextAttributeValue(QUOTE_CHAR_BARE, bitStartPos, pos);
                        state = ParseState.TAG_SLASH;
                    } else if (c == '>') {
                        ret.add(current.buildNextAttributeValue(QUOTE_CHAR_BARE, bitStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos + 1;
                        state = ParseState.PLAIN_TEXT;
                    } else if (c == '<') {
                        // weird state, assume this "tag" is actually plain text.
                        ret.add(current.buildPlainText(textStartPos, pos).close());
                        current = new HtmlBit(text);
                        textStartPos = pos;
                        state = ParseState.LESS_THAN;
                    } else if (isWhitespace(c)) {
                        current.buildNextAttributeValue(QUOTE_CHAR_BARE, bitStartPos, pos);
                        state = ParseState.BETWEEN_ATTRIBUTES;
                    }
                    // else keep scanning
                    break;
                }
            }
        }

        // At the end of the text.  Anything that's left over is either plain text or incomplete
        // html.  Just mark it as plain text.
        if (textStartPos + 1 < cbuff.length) {
            ret.add(current.buildPlainText(textStartPos, cbuff.length).close());
        }

        return ret;
    }


    private enum ParseState {
        /** Outside any tagging characters */
        PLAIN_TEXT,

        // These all indicate the state after the character was identified.

        /** HTML: Starting a tag, perhaps. */
        LESS_THAN,

        /** HTML: A slash mark, within a tag, outside an attribute. */
        TAG_SLASH,

        /** HTML: slash mark immediately after a &lt; */
        LESS_THAN_SLASH,

        /** HTML: First mText inside an HTML tag. */
        TAG_NAME,

        /** HTML: Whitespace after a tag name or after an attribute value. */
        BETWEEN_ATTRIBUTES,

        /** HTML: The mText after between attributes. */
        ATTRIBUTE_KEY,

        /** HTML: A space happened after an ATTRIBUTE_KEY, could be the start of an '=' or another
        attribute or an end of the tag. */
        ATTRIBUTE_AFTER_KEY,

        /** HTML: Looking for the start of the attribute value. */
        ATTRIBUTE_AFTER_EQUALS,

        /** HTML: Inside a single quoted attribute value */
        ATTRIBUTE_VALUE_SINGLE_QUOTE,

        /** HTML: Inside a double quoted attribute value */
        ATTRIBUTE_VALUE_DOUBLE_QUOTE,

        /** HTML: Inside a bare attribute value */
        ATTRIBUTE_VALUE_BARE,
    }
}
