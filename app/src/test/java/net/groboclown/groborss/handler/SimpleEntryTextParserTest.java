package net.groboclown.groborss.handler;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static net.groboclown.groborss.handler.SimpleHtmlParser.parse;

public class SimpleEntryTextParserTest {
    @Test
    public void parse_empty() {
        List<SimpleHtmlParser.HtmlBit> bits = parse("");
        assertThat(bits.isEmpty(), is(true));
    }

    @Test
    public void parse_plainTextOnly() {
        List<SimpleHtmlParser.HtmlBit> bits = parse("plain text");
        assertThat(bits.size(), is(1));
        assertPlainText(bits, 0, "plain text");
    }

    @Test
    public void parse_simpleTagOnly() {
        List<SimpleHtmlParser.HtmlBit> bits = parse("<a>");
        assertThat(bits.size(), is(1));
        assertTagState(bits, 0, true, false);
        assertTagEquals(bits, 0, "a");
    }

    @Test
    public void parse_simpleTagWithAttributes() {
        List<SimpleHtmlParser.HtmlBit> bits = parse("<abc src='1234' h=\"1\" q=ab >");
        assertThat(bits.size(), is(1));
        assertTagState(bits, 0, true, false);
        assertTagEquals(bits, 0, "abc", "src", "1234", "h", "1", "q", "ab");
    }

    @Test
    public void parse_closeTag() {
        List<SimpleHtmlParser.HtmlBit> bits = parse("</abc>");
        assertThat(bits.size(), is(1));
        assertTagState(bits, 0, false, true);
        assertTagEquals(bits, 0, "abc");
    }

    @Test
    public void parse_textAroundTag() {
        List<SimpleHtmlParser.HtmlBit> bits = parse("ab<cd>ef");
        assertThat(bits.size(), is(3));
        assertPlainText(bits, 0, "ab");
        assertTagState(bits, 1, true, false);
        assertTagEquals(bits, 1, "cd");
        assertPlainText(bits, 2, "ef");
    }

    @Test
    public void parse_startEndTags() {
        List<SimpleHtmlParser.HtmlBit> bits = parse("<ab>cd</ef>");
        assertThat(bits.size(), is(3));
        assertTagState(bits, 0, true, false);
        assertTagEquals(bits, 0, "ab");
        assertPlainText(bits, 1, "cd");
        assertTagState(bits, 2, false, true);
        assertTagEquals(bits, 2, "ef");
    }

    @Test
    public void parse_singletonTag() {
        List<SimpleHtmlParser.HtmlBit> bits = parse("<ab/>");
        assertThat(bits.size(), is(1));
        assertTagState(bits, 0, true, true);
        assertTagEquals(bits, 0, "ab");
    }

    @Test
    public void parse_tooManyTagsFromBadArgument() {
        // This was observed in an NPR feed.
        // This goes beyond the 30 tag mark.
        List<SimpleHtmlParser.HtmlBit> bits = parse("<a b='T's 0 1 2 3 4 5 6 7 8 9 a b c d e f g h i j k l m n o p q r s t u v w x y z/>");
        assertThat(bits.size(), is(1));
        assertTagState(bits, 0, true, true);
        assertTagEquals(bits, 0 , "a",
                "b", "T", // arg 1
                "s", null,
                "0", null,
                "1", null,
                "2", null,
                "3", null,
                "4", null,
                "5", null,
                "6", null,
                "7", null, // arg 10
                "8", null,
                "9", null,
                "a", null,
                "b", null,
                "c", null,
                "d", null,
                "e", null,
                "f", null,
                "g", null,
                "h", null, // arg 20
                "i", null,
                "j", null,
                "k", null,
                "l", null,
                "m", null,
                "n", null,
                "o", null,
                "p", null,
                "q", null,
                "r", null // arg 30
                );
    }

    private static void assertPlainText(List<SimpleHtmlParser.HtmlBit> bits, int index, String text) {
        assertTrue("No such index " + index, bits.size() > index);
        assertPlainText("bit " + index, bits.get(index), text);
    }

    private static void assertPlainText(String msg, SimpleHtmlParser.HtmlBit bit, String text) {
        assertThat(msg + ": is not tag?", bit.isHtmlTag(), is(false));
        assertThat(msg + ": plain text contents", bit.toString(), is(text));
    }

    private static void assertTagEquals(List<SimpleHtmlParser.HtmlBit> bits, int index, String tag, String... attributeKeyValues) {
        assertTrue("No such index " + index, bits.size() > index);
        assertTagEquals("bit " + index, bits.get(index), tag, attributeKeyValues);
    }

    private static void assertTagEquals(String msg, SimpleHtmlParser.HtmlBit bit, String tag, String... attributeKeyValues) {
        assertThat(msg + ": is tag?", bit.isHtmlTag(), is(true));
        assertThat(msg + ": tag", bit.getTag(), is(tag));
        int index = 0;
        // assume already at start of attributes.
        while (bit.nextAttribute()) {
            assertThat(msg + ": attributes for " + bit + " index " + (index / 2),
                    index < attributeKeyValues.length,
                    is(true));
            assertThat(msg + ": attribute[" + (index / 2) + "] key",
                    bit.getCurrentKey(),
                    is(attributeKeyValues[index++]));
            assertThat(msg + ": attribute[" + (index / 2) + "] value for " + bit.getCurrentKey(),
                    bit.getCurrentValue(),
                    is(attributeKeyValues[index++]));
        }
        assertThat(msg + ": not enough attributes", index / 2, is(attributeKeyValues.length / 2));
    }
    private static void assertTagState(List<SimpleHtmlParser.HtmlBit> bits, int index, boolean isStart, boolean isEnd) {
        assertTrue("No such index " + index, bits.size() > index);
        assertTagState("bit " + index, bits.get(index), isStart, isEnd);
    }

    private static void assertTagState(String msg, SimpleHtmlParser.HtmlBit bit, boolean isStart, boolean isEnd) {
        assertThat(msg + ": start?", bit.isStartTag(), is(isStart));
        assertThat(msg + ": end?", bit.isEndTag(), is(isEnd));
    }
}