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

import net.groboclown.groborss.Strings;

import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntryTextBuilderTest {

    // Examples pulled from real example feeds

    @Test
    public void testNewsArticle_withPictures() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600' alt='Image description'/><p>News article summary</p><p>(Image credit: A Person)</p><img src='https://media.a.news.site/include/images/tracking/rss-pixel.png?story=1234' />")
                .withPreferences(prefs(true, false, 1))
                ;

        assertThat(
                builder.prepare().toString(),
                is("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600'/><br><font color='gray'><smaller><i>Image description</i></smaller></font><p>News article summary</p><p>(Image credit: A Person)</p><font color='gray'><smaller><i>Bug Zapped!</i></smaller></font>")
        );
    }

    @Test
    public void testNewsArticle_withPicturesStripped() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600' alt='Image description'/><p>News article summary</p><p>(Image credit: A Person)</p><img src='https://media.a.news.site/include/images/tracking/rss-pixel.png?story=1234' />")
                .withPreferences(prefs(false, true, 1))
                ;

        assertThat(
                builder.prepare().toString(),
                // TODO should this instead be adding the alt text and strip the image?
                is("<p>News article summary</p><p>(Image credit: A Person)</p>")
        );
    }


    @Test
    public void testXkcd() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("<img src=\"https://imgs.xkcd.com/comics/supervillain_plan.png\" title=\"Someday, some big historical event will happen during the DST changeover, and all the tick-tock articles chronicling how it unfolded will have to include a really annoying explanation next to their timelines.\" alt=\"Someday, some big historical event will happen during the DST changeover, and all the tick-tock articles chronicling how it unfolded will have to include a really annoying explanation next to their timelines.\" />")
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<img src=\"https://imgs.xkcd.com/comics/supervillain_plan.png\"/><br><font color='gray'><smaller><i>Someday, some big historical event will happen during the DST changeover, and all the tick-tock articles chronicling how it unfolded will have to include a really annoying explanation next to their timelines.</i></smaller></font>")
        );
    }


    @Test
    public void testTagOnly() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("<p>")
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<p>")
        );
    }


    @Test
    public void testNoAltText() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("<img src='1234' other='bcd'>")
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<img src='1234' other='bcd'>")
        );
    }


    @Test
    public void testMultipleImages() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600' alt='Image description'/><br><img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600' alt='Image description'/>")
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600'/><br><font color='gray'><smaller><i>Image description</i></smaller></font><br><img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600'/><br><font color='gray'><smaller><i>Image description</i></smaller></font>")
        );
    }


    @Test
    public void testOnlyUrl() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("http://a.com/b")
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<a href='http://a.com/b'>http://a.com/b</a>")
        );
    }


    @Test
    public void testConvertPlainUrlsSrcSet() {
        // "srcset" is a list of urls, which makes this hard to manage
        // with normal html attribute parsing.
        final String toTest = "<a href='http://place/there'><img src='http://place/link1.jpg' srcset='http://place/link1.jpg 240w, http://place/link2.jpg 640w' /></a>";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<a href='http://place/there'><img src='http://place/link1.jpg' srcset='http://place/link1.jpg 240w, http://place/link2.jpg 640w'/></a>")
        );
    }


    @Test
    public void testHtmlNeedsLink() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("<i>link http://place/there and</i><b>another http://place/howdy link</b>")
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<i>link <a href='http://place/there'>http://place/there</a> and</i><b>another <a href='http://place/howdy'>http://place/howdy</a> link</b>")
        );
    }


    @Test
    public void test_startLink() {
        final String toTest = "<a href='http://a.com/b'>";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is(toTest)
        );
    }


    @Test
    public void test_link() {
        final String toTest = "<a href='http://a.com/b'>a link</a>";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is(toTest)
        );
    }


    @Test
    public void test_link2() {
        final String toTest = "<a href='http://a.com/b' pass>a link</a>";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is(toTest)
        );
    }


    @Test
    public void test_complexTag() {
        final String toTest = "<svg:a alt='my text' svg:path='a\"b' svg:href='http://a.com/b' abc:xyz tty=\"a\">";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is(toTest)
        );
    }


    @Test
    public void test_outsideTags() {
        final String toTest = "<i>http://a.com/b</i>";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<i><a href='http://a.com/b'>http://a.com/b</a></i>")
        );
    }


    @Test
    public void test_badHtml() {
        // This kind of HTML isn't parsed right because it should be wrapped in a string.
        // The "/" in the attribute key causes things to go sideways.
        final String toTest = "<a href=http://a.com/b>blah</a>";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<a href='http:' //a.com /b>blah</a>")
        );
    }


    @Test
    public void test_validWeirdHtml() {
        final String toTest = "<a show state=on>blah</a>";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<a show state='on'>blah</a>")
        );
    }


    @Test
    public void test_inLinkToOther() {
        final String toTest = "<a href='http://a.c/b'>http://a.b/b</a>";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is("<a href='http://a.c/b'><a href='http://a.b/b'>http://a.b/b</a></a>")
        );
    }


    @Test
    public void test_inLinkToSelf() {
        final String toTest = "<a href='http://a.b/b'>http://a.b/b</a>";
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText(toTest)
                .withPreferences(prefs(false, false, 1))
                ;
        assertThat(
                builder.prepare().toString(),
                is(toTest)
        );
    }


    @Test
    public void testConvertBBCode_empty() {
        assertThat(
                EntryTextBuilder.convertBBCode(""),
                is("")
        );
    }

    @Test
    public void testConvertBBCode_nothingToDo() {
        assertThat(
                EntryTextBuilder.convertBBCode("<img href='1234'>"),
                is("<img href='1234'>")
        );
    }

    @Test
    public void testConvertBBCode_img() {
        assertThat(
                EntryTextBuilder.convertBBCode("[img]https://1234[/img]"),
                is("<img src='https://1234'>")
        );
    }

    @Test
    public void testConvertBBCode_url1() {
        assertThat(
                EntryTextBuilder.convertBBCode("[url]https://1234[/url]"),
                is("<a href='https://1234'>https://1234</a>")
        );
    }

    /* Not supported
    @Test
    public void testConvertBBCode_url2() {
        assertThat(
                EntryTextBuilder.convertBBCode("[url=https://1234]1234[/url]"),
                is("<a href='https://1234'>1234</a>")
        );
    }
    */

    @Test
    public void testConvertBBCode_code() {
        assertThat(
                EntryTextBuilder.convertBBCode("[code]abc[/code]"),
                is("<pre>abc</pre>")
        );
    }

    @Test
    public void testConvertBBCode_bold() {
        assertThat(
                EntryTextBuilder.convertBBCode("[b]abc[/b]"),
                is("<b>abc</b>")
        );
    }

    @Test
    public void testConvertBBCode_italic() {
        assertThat(
                EntryTextBuilder.convertBBCode("[i]abc[/i]"),
                is("<i>abc</i>")
        );
    }

    @Test
    public void testConvertBBCode_underline() {
        assertThat(
                EntryTextBuilder.convertBBCode("[u]abc[/u]"),
                is("<u>abc</u>")
        );
    }

    @Test
    public void testConvertBBCode_strikethrough() {
        assertThat(
                EntryTextBuilder.convertBBCode("[s]abc[/s]"),
                is("<s>abc</s>")
        );
    }

    @Test
    public void testConvertBBCode_complex() {
        assertThat(
                EntryTextBuilder.convertBBCode("This [i]is [url]http://1234[/url] [b]kind[/i] of[/b] complex [[example] if [ something we can do"),
                is("This <i>is <a href='http://1234'>http://1234</a> <b>kind</i> of</b> complex [[example] if [ something we can do")
        );
    }


    private static SharedPreferences prefs(boolean stripWebBugs, boolean disablePictures, int fontsize) {
        SharedPreferences ret = mock(SharedPreferences.class);
        when(ret.getBoolean(eq(Strings.SETTINGS_STRIP_WEB_BUGS), Mockito.any(Boolean.class))).thenReturn(stripWebBugs);
        when(ret.getBoolean(eq(Strings.SETTINGS_DISABLEPICTURES), Mockito.any(Boolean.class))).thenReturn(disablePictures);
        when(ret.getString(eq(Strings.SETTINGS_FONTSIZE), anyString())).thenReturn(Integer.toString(fontsize));
        return ret;
    }
}