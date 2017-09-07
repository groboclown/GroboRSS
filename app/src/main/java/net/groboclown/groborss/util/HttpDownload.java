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

package net.groboclown.groborss.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Xml;

import net.groboclown.groborss.BASE64;
import net.groboclown.groborss.Strings;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

/**
 * Manages downloads from HTTP and HTTPS connections.
 */
public class HttpDownload {
    private static final String ZERO = "0";
    private static final String KEY_USERAGENT = "User-agent";
    private static final String VALUE_USERAGENT = "Mozilla/5.0";
    private static final String GZIP = "gzip";
    private static final String CHARSET = "charset=";
    private static final String ENCODING = "encoding=\"";
    private static final String DEFAULT_HTTP_ENCODING = "ISO-8859-1";
    private static final String UTF8 = "UTF-8";
    private static final String UTF16 = "UTF-16";
    private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    private static final int LOOK_AHEAD_LENGTH = 4096;

    public static Factory setup(Context context) {
        return new Factory(context);
    }

    public static class Factory {
        private final boolean online;
        private final Proxy proxy;
        private final boolean imposeUserAgent;
        private boolean followHttpHttpsRedirects;

        private Factory(Context context) {
            SharedPreferences preferences;
            try {
                preferences = PreferenceManager.getDefaultSharedPreferences(context.createPackageContext(Strings.PACKAGE, 0));
            } catch (PackageManager.NameNotFoundException e) {
                preferences = PreferenceManager.getDefaultSharedPreferences(context);
            }
            imposeUserAgent = !preferences.getBoolean(Strings.SETTINGS_STANDARDUSERAGENT, false);
            followHttpHttpsRedirects = preferences.getBoolean(Strings.SETTINGS_HTTPHTTPSREDIRECTS, false);

            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                online = true;
                if (preferences.getBoolean(Strings.SETTINGS_PROXYENABLED, false) && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || !preferences.getBoolean(Strings.SETTINGS_PROXYWIFIONLY, false))) {
                    Proxy p;
                    try {
                        p = new Proxy(ZERO.equals(preferences.getString(Strings.SETTINGS_PROXYTYPE, ZERO)) ? Proxy.Type.HTTP : Proxy.Type.SOCKS, new InetSocketAddress(preferences.getString(Strings.SETTINGS_PROXYHOST, Strings.EMPTY), Integer.parseInt(preferences.getString(Strings.SETTINGS_PROXYPORT, Strings.DEFAULTPROXYPORT))));
                    } catch (Exception e) {
                        p = null;
                    }
                    proxy = p;
                } else {
                    proxy = null;
                }
            } else {
                online = false;
                proxy = null;
            }
        }

        @Nullable
        public HttpDownload connect(String url) throws IOException, KeyManagementException, NoSuchAlgorithmException {
            if (! online) {
                return null;
            }
            return new HttpDownload(this, new URL(url));
        }

        @Nullable
        public HttpDownload connect(URL url) throws IOException, KeyManagementException, NoSuchAlgorithmException {
            if (!online) {
                return null;
            }
            return new HttpDownload(this, url);
        }
    }

    private final Factory factory;
    private final URL url;
    private final HttpURLConnection connection;
    private String charset;
    private String xmlCharset;
    private BufferedInputStream streamRead;


    private HttpDownload(Factory factory, URL url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        this.factory = factory;
        this.url = url;
        this.connection = createConnection(factory, url, 0);
    }

    public void disconnect() {
        this.connection.disconnect();
    }

    public BufferedReader getAsReader() throws IOException {
        return getAsReader(getEncodingCharset(false));
    }

    public BufferedReader getAsReader(String charset) throws IOException {
        if (charset == null) {
            return new BufferedReader(new InputStreamReader(getAsInputStream()));
        }
        return new BufferedReader(new InputStreamReader(getAsInputStream(), charset));
    }


    public byte[] getAsBytes() throws IOException {
        InputStream inputStream = getAsInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];

        int n;

        while ((n = inputStream.read(buffer)) > 0) {
            output.write(buffer, 0, n);
        }

        byte[] result  = output.toByteArray();

        output.close();
        inputStream.close();
        return result;
    }


    public BufferedInputStream getAsInputStream() throws IOException {
        if (streamRead != null) {
            streamRead.reset();
            return streamRead;
        }
        InputStream inputStream = connection.getInputStream();

        if (GZIP.equals(connection.getContentEncoding()) && !(inputStream instanceof GZIPInputStream)) {
            streamRead = new BufferedInputStream(new GZIPInputStream(inputStream));
        } else {
            streamRead = new BufferedInputStream(inputStream);
        }
        streamRead.mark(LOOK_AHEAD_LENGTH);
        return streamRead;
    }


    /**
     * Force the stream to read back to the beginning.  If it can't do it easily
     * (read too far ahead of the buffer), then a new connection is made.
     *
     * @return the reset connection.
     */
    public HttpDownload reset() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        if (streamRead == null) {
            // nothing read yet.
            return this;
        }
        try {
            streamRead.reset();
            return this;
        } catch (IOException e) {
            // could not reset to the start of the stream
            disconnect();
            return factory.connect(url);
        }
    }

    public HttpDownload getFaviconConnection() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        return factory.connect(connection.getURL().getProtocol()
                + Strings.PROTOCOL_SEPARATOR
                + connection.getURL().getHost()
                + Strings.FILE_FAVICON);
    }

    public URL getURL() {
        return connection.getURL();
    }

    public boolean isHtmlDocument() {
        String contentType = connection.getContentType();
        return (contentType != null && contentType.startsWith(CONTENT_TYPE_TEXT_HTML));
    }


    public boolean isXmlEncodingSupported() {
        try {
            getEncodingCharset(true);
            return charset != null && charset.equals(xmlCharset);
        } catch (IOException e) {
            // Problem while trying to read the encoding, so it's not supported.
            return false;
        }
    }


    @Nullable
    public String getEncodingCharset(boolean xmlCompatible) throws IOException {
        if (xmlCompatible && xmlCharset != null) {
            return xmlCharset;
        }
        if (charset != null) {
            if (xmlCompatible) {
                return detectXmlCompatibleCharset(charset);
            }
            return charset;
        }
        String ct = connection.getContentType();

        if (ct != null) {
            int charsetPos = ct.indexOf(CHARSET);

            if (charsetPos >= 0) {
                int charsetEndPos = ct.indexOf(';', charsetPos);
                String charset = ct.substring(charsetPos + CHARSET.length(),
                        charsetEndPos < 0 ? ct.length() : charsetEndPos);
                // try to see if "String" supports this charset
                try {
                    new String(new byte[0], charset);
                    this.charset = charset;
                    if (xmlCompatible) {
                        return xmlCharset = detectXmlCompatibleCharset(charset);
                    }
                    return charset;
                } catch (UnsupportedEncodingException e) {
                    // not a supported charset
                    // keep looking
                }
            }
        }

        this.charset = null;
        if (xmlCompatible) {
            return xmlCharset = detectXmlCompatibleCharset(null);
        }
        return null;
    }

    private String detectXmlCompatibleCharset(String charset) throws IOException {
        if (charset != null) {
            try {
                Xml.findEncodingByName(charset);
                return charset;
            } catch (UnsupportedEncodingException e) {
                return findEmbeddedCharset();
            }
        }
        return findEmbeddedCharset();
    }

    private String findEmbeddedCharset() throws IOException {
        // Read from the input stream, up to the mark length, and
        // check for the embedded encoding text.
        BufferedInputStream reader = getAsInputStream();
        byte[] buff = new byte[LOOK_AHEAD_LENGTH];
        int len = reader.read(buff, 0, LOOK_AHEAD_LENGTH);
        if (len < 3) {
            // Not possible to include an encoding.
            // Use the default HTTP encoding.
            return DEFAULT_HTTP_ENCODING;
        }
        // Look for the windows byte ordering mark.
        if (buff[0] == (byte)0xef && buff[1] == (byte)0xbb && buff[2] == (byte)0xbf) {
            // utf-8
            return UTF8;
        }
        if (
                (buff[0] == (byte)0xfe && buff[1] == (byte)0xff) // big endian
                || (buff[0] == (byte)0xff && buff[1] == (byte)0xfe)) { // little endian
            // UTF-16 will look at the Byte Order Mark and determine for
            // itself the ordering.
            return UTF16;
        }

        // Look for the "encoding=" string.
        // Rather than perform a tricky byte search algorithm, we'll just convert it
        // to a string and use the built-in capabilities.  Anything with an encoding string by
        // this point is only using 8-bit characters (for the most part).

        String testText = new String(buff);
        int startPos = testText.indexOf(ENCODING);
        if (startPos >= 0) {
            int endPos = testText.indexOf('"', startPos + ENCODING.length());
            if (endPos > 0) {
                String encoding = testText.substring(startPos + ENCODING.length(), endPos);
                try {
                    Xml.findEncodingByName(encoding);
                    return encoding;
                } catch (UnsupportedEncodingException e) {
                    // No idea.  We're using the default.
                    return DEFAULT_HTTP_ENCODING;
                }
            }
        }

        return DEFAULT_HTTP_ENCODING;
    }


    public String getAsString(boolean xmlCompatible) throws IOException {
        String charset = getEncodingCharset(xmlCompatible);
        if (charset == null) {
            charset = DEFAULT_HTTP_ENCODING;
        }
        return new String(getAsBytes(), charset);
    }


    private static HttpURLConnection createConnection(Factory factory, URL url, int cycle) throws IOException {
        HttpURLConnection connection = factory.proxy == null
                ? (HttpURLConnection) url.openConnection()
                : (HttpURLConnection) url.openConnection(factory.proxy);

        connection.setDoInput(true);
        connection.setDoOutput(false);
        if (factory.imposeUserAgent) {
            connection.setRequestProperty(KEY_USERAGENT, VALUE_USERAGENT); // some feeds need this to work properly
        }
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setUseCaches(false);

        if (url.getUserInfo() != null) {
            connection.setRequestProperty("Authorization", "Basic "+ BASE64.encode(url.getUserInfo().getBytes()));
        }
        connection.setRequestProperty("connection", "disconnect"); // Workaround for android issue 7786
        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.connect();

        String location = connection.getHeaderField("Location");

        if (location != null && (url.getProtocol().equals(Strings._HTTP) && location.startsWith(Strings.HTTPS) || url.getProtocol().equals(Strings._HTTPS) && location.startsWith(Strings.HTTP))) {
            // if location != null, the system-automatic redirect has failed which indicates a protocol change
            if (factory.followHttpHttpsRedirects) {
                connection.disconnect();

                if (cycle < 5) {
                    return createConnection(factory, url, cycle+1);
                } else {
                    throw new IOException("Too many redirects.");
                }
            } else {
                throw new IOException("https<->http redirect - enable in settings");
            }
        }
        return connection;
    }
}
