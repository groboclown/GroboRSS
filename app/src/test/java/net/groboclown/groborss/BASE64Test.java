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
