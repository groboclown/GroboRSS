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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EntryTextBuilderTest {

    // Examples pulled from real example feeds

    @Test
    public void testNewsArticle_withPictures() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600' alt='Image description'/><p>News article summary</p><p>(Image credit: A Person)</p><img src='https://media.a.news.site/include/images/tracking/rss-pixel.png?story=1234' />")
                ;

        assertThat(
                builder.prepare(false).toString(),
                is("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600'/><br>[<i>Image description</i>]<p>News article summary</p><p>(Image credit: A Person)</p><img src='https://media.a.news.site/include/images/tracking/rss-pixel.png?story=1234' />")
        );
    }


    @Test
    public void testXkcd() {
        EntryTextBuilder builder = new EntryTextBuilder()
                .withAbstractText("<img src=\"https://imgs.xkcd.com/comics/supervillain_plan.png\" title=\"Someday, some big historical event will happen during the DST changeover, and all the tick-tock articles chronicling how it unfolded will have to include a really annoying explanation next to their timelines.\" alt=\"Someday, some big historical event will happen during the DST changeover, and all the tick-tock articles chronicling how it unfolded will have to include a really annoying explanation next to their timelines.\" />")
                ;
        assertThat(
                builder.prepare(false).toString(),
                is("<img src=\"https://imgs.xkcd.com/comics/supervillain_plan.png\" title=\"Someday, some big historical event will happen during the DST changeover, and all the tick-tock articles chronicling how it unfolded will have to include a really annoying explanation next to their timelines.\" /><br>[<i>Someday, some big historical event will happen during the DST changeover, and all the tick-tock articles chronicling how it unfolded will have to include a really annoying explanation next to their timelines.</i>]")
        );
    }


    @Test
    public void testExtractAltText_noPicture() {
        assertThat(
                EntryTextBuilder.extractAltText("<p>"),
                is("<p>")
        );
    }


    @Test
    public void testExtractAltText_noAltText() {
        assertThat(
                EntryTextBuilder.extractAltText("<img src='1234' other='bcd'>"),
                is("<img src='1234' other='bcd'>")
        );
    }


    @Test
    public void testExtractAltText_altText() {
        assertThat(
                EntryTextBuilder.extractAltText("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600' alt='Image description'/>"),
                is("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600'/><br>[<i>Image description</i>]")
        );
    }


    @Test
    public void testExtractAltText_multipleImages() {
        assertThat(
                EntryTextBuilder.extractAltText("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600' alt='Image description'/><br><img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600' alt='Image description'/>"),
                is("<img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600'/><br>[<i>Image description</i>]<br><img src='https://media.a.news.site/assets/img/dated-image.jpg?s=600'/><br>[<i>Image description</i>]")
        );
    }


    @Test
    public void testIsTagAttribute_onlyUrl() {
        final String toTest = "http://a.com/b";
        assertThat(
                EntryTextBuilder.isTagAttribute(toTest, 0, toTest.length()),
                is(false)
        );
    }


    @Test
    public void testConvertPlainUrls_srcSet() {
        // "srcset" is a list of urls, which makes this hard to manage
        // with normal html attribute parsing.
        final String toTest = "<a href='http://place/there'><img src='http://place/link1.jpg' srcset='http://place/link1.jpg 240w, http://place/link2.jpg 640w' /></a>";
        assertThat(
                EntryTextBuilder.convertPlainUrls(toTest),
                is(toTest)
        );
    }


    @Test
    public void testConvertPlainUrls_htmlNeedsLink() {
        // "srcset" is a list of urls, which makes this hard to manage
        // with normal html attribute parsing.
        final String toTest = "<i>link http://place/there and</i><b>another http://place/howdy link</b>";
        assertThat(
                EntryTextBuilder.convertPlainUrls(toTest),
                is("<i>link <a href='http://place/there'>http://place/there</a> and</i><b>another <a href='http://place/howdy'>http://place/howdy</a> link</b>")
        );
    }


    @Test
    public void testIsTagAttribute_startLink() {
        final String toTest = "<a href='http://a.com/b'>";
        assertThat(
                EntryTextBuilder.isTagAttribute(toTest, toTest.indexOf("http://"), toTest.lastIndexOf('\'')),
                is(true)
        );
    }


    @Test
    public void testIsTagAttribute_link() {
        final String toTest = "<a href='http://a.com/b'>a link</a>";
        assertThat(
                EntryTextBuilder.isTagAttribute(toTest, toTest.indexOf("http://"), toTest.lastIndexOf('\'')),
                is(true)
        );
    }


    @Test
    public void testIsTagAttribute_link2() {
        final String toTest = "<a href='http://a.com/b' pass>a link</a>";
        assertThat(
                EntryTextBuilder.isTagAttribute(toTest, toTest.indexOf("http://"), toTest.lastIndexOf('\'')),
                is(true)
        );
    }


    @Test
    public void testIsTagAttribute_complexTag() {
        final String toTest = "<svg:a alt='my text' svg:path='a\"b' svg:href='http://a.com/b' abc:xyz tty=\"a\">";
        assertThat(
                EntryTextBuilder.isTagAttribute(toTest, toTest.indexOf("http://"), toTest.lastIndexOf('\'')),
                is(true)
        );
    }


    @Test
    public void testIsTagAttribute_outsideTags() {
        final String toTest = "<i>http://a.com/b</i>";
        assertThat(
                EntryTextBuilder.isTagAttribute(toTest, toTest.indexOf("http://"), toTest.lastIndexOf('<')),
                is(false)
        );
    }


    @Test
    public void testIsTagAttribute_badHtml() {
        final String toTest = "<a href=http://a.com/b>blah</a>";
        assertThat(
                EntryTextBuilder.isTagAttribute(toTest, toTest.indexOf("http://"), toTest.indexOf('>')),
                is(true)
        );
    }


    @Test
    public void testIsInTag_onlyUrl() {
        final String toTest = "http://a.b/b";
        assertThat(
                EntryTextBuilder.isInTag(toTest, 0, toTest.length()),
                is(false)
        );
    }


    @Test
    public void testIsInTag_asAttribute() {
        final String toTest = "<a href='http://a.b/b'>";
        assertThat(
                EntryTextBuilder.isInTag(toTest, toTest.indexOf("http:"), toTest.lastIndexOf('\'')),
                is(false)
        );
    }


    @Test
    public void testIsInTag_inLinkToOther() {
        final String toTest = "<a href='http://a.c/b'>http://a.b/b</a>";
        assertThat(
                EntryTextBuilder.isInTag(toTest, toTest.indexOf("http:", toTest.indexOf("http:") + 1), toTest.lastIndexOf('<')),
                is(false)
        );
    }


    @Test
    public void testIsInTag_inLinkToSelf() {
        final String toTest = "<a href='http://a.b/b'>http://a.b/b</a>";
        assertThat(
                EntryTextBuilder.isInTag(toTest, toTest.indexOf("http:", toTest.indexOf("http:") + 1), toTest.lastIndexOf('<')),
                is(true)
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
}