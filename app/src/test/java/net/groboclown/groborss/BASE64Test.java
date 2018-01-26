/*
 * GroboRSS
 *
 * Copyright (c) 2018 Groboclown
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

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class BASE64Test {
    @Test
    public void encode_empty() {
        assertThat(
                BASE64.encode(new byte[0]),
                is(""));
    }

    @Test
    public void encode_1() {
        assertThat(
                BASE64.encode(new byte[] { ']' }),
                is("XQ==")
        );
    }

    @Test
    public void encode_3() {
        assertThat(
                BASE64.encode(new byte[] { 'a', 'b', 'c' }),
                is("YWJj")
        );
    }

    @Test
    public void encode_4() {
        assertThat(
                BASE64.encode(new byte[] { 'a', 'b', 'c', 'd' }),
                is("YWJjZA==")
        );
    }

    @Test
    public void encode_binary() {
        assertThat(
                BASE64.encode(new byte[] { 1, 2, 3, 4 }),
                is("AQIDBA==")
        );
    }

    @Test
    public void decode_empty() {
        assertThat(BASE64.decode(new char[0]),
                is(new byte[0]));
    }

    @Test
    public void decode_1() {
        assertThat(BASE64.decode("XQ==".toCharArray()),
                is(new byte[]{ ']' }));
    }

    @Test
    public void decode_3() {
        assertThat(BASE64.decode("YWJj".toCharArray()),
                is(new byte[]{ 'a', 'b', 'c' }));
    }

    @Test
    public void decode_4() {
        assertThat(BASE64.decode("YWJjZA==".toCharArray()),
                is(new byte[]{ 'a', 'b', 'c', 'd' }));
    }

    @Test
    public void decode_binary() {
        assertThat(
                BASE64.decode("AQIDBA==".toCharArray()),
                is(new byte[] { 1, 2, 3, 4 })
        );
    }

    @Test
    public void decode_ignoreWhitespace() {
        assertThat(
                BASE64.decode("A\tQ I\nD\rBA==".toCharArray()),
                is(new byte[] { 1, 2, 3, 4 })
        );
    }
}
