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

package net.groboclown.groborss.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static net.groboclown.groborss.provider.FeedData.getDbTables;
import static net.groboclown.groborss.provider.FeedData.TYPE_BLOB;
import static net.groboclown.groborss.provider.FeedData.TYPE_BOOLEAN;
import static net.groboclown.groborss.provider.FeedData.TYPE_DATETIME;
import static net.groboclown.groborss.provider.FeedData.TYPE_INT;
import static net.groboclown.groborss.provider.FeedData.TYPE_INT_7;
import static net.groboclown.groborss.provider.FeedData.TYPE_PRIMARY_KEY;
import static net.groboclown.groborss.provider.FeedData.TYPE_TEXT;
import static net.groboclown.groborss.provider.FeedData.TYPE_TEXT_UNIQUE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the JSON processor.
 */
public class JsonStateTest {
    private static final DbTable EVERY_TYPE_TABLE = new DbTable(
            "every",
            new String[] { "id", "t1", "t2", "i1", "i2", "z", "d", "b" },
            new String[] { TYPE_PRIMARY_KEY, TYPE_TEXT, TYPE_TEXT_UNIQUE, TYPE_INT, TYPE_INT_7, TYPE_BOOLEAN, TYPE_DATETIME, TYPE_BLOB }
    );

    private JsonState jsonState;

    @Before
    public void setup() {
        jsonState = new UTJsonState();
        MOCKED_CONTENT_VALUES.clear();
    }


    @Test
    public void readJsonReader()
            throws Exception {
        String json = "{\"every\":{\"rows\":["
                + "{\"t1\": \"a\", \"t2\": \"b\", \"i1\": 1, \"i2\": 2, \"z\": 0, \"d\": 100012002, \"b\": \"AQIDBA==\"}"
                + "]}}";
        StringReader r = new StringReader(json);
        SQLiteDatabase mockSql = mock(SQLiteDatabase.class);
        DbTableFacadeFactory.SQLiteFactory dbFactory = new DbTableFacadeFactory.SQLiteFactory(
                mockSql);

        jsonState.readJson(r, dbFactory, new DbTable[] { EVERY_TYPE_TABLE });

        verify(mockSql).delete(EVERY_TYPE_TABLE.getTableName(), null, null);
        verify(mockSql).beginTransactionNonExclusive();
        verify(mockSql).insert(eq(EVERY_TYPE_TABLE.getTableName()), eq((String) null), any(ContentValues.class));
        verify(mockSql).endTransaction();
        verifyNoMoreInteractions(mockSql);

        assertThat(MOCKED_CONTENT_VALUES.size(), is(1));
        ContentValues mockValues = MOCKED_CONTENT_VALUES.get(0);
        verify(mockValues).put("t1", "a");
        verify(mockValues).put("t2", "b");
        verify(mockValues).put("i1", 1);
        verify(mockValues).put("i2", 2);
        verify(mockValues).put("z", 0);
        verify(mockValues).put("d", 100012002L);
        verify(mockValues).put("b", new byte[] { 1, 2, 3, 4 });
        verifyNoMoreInteractions(mockValues);
    }

    @Test
    public void verifyJson_rowData()
            throws Exception {
        String json = "{\"every\":{\"rows\":["
                + "{\"t1\": \"a\", \"t2\": \"b\", \"i1\": 1, \"i2\": 2, \"z\": 0, \"d\": 100012002, \"b\": \"AQIDBA==\"}"
                + "]}}";
        jsonState.verifyJson(new JSONObject(json), new DbTable[] { EVERY_TYPE_TABLE });
    }

    @Test
    public void verifyJson_nullBytes()
            throws Exception {
        String json = "{\"every\":{\"rows\":["
                + "{\"t1\": \"a\", \"t2\": \"b\", \"i1\": 1, \"i2\": 2, \"z\": 0, \"d\": 100012002, \"b\": null}"
                + "]}}";
        jsonState.verifyJson(new JSONObject(json), new DbTable[] { EVERY_TYPE_TABLE });
    }

    @Test
    public void verifyJson_noData()
            throws Exception {
        String json = "{\"feeds\":{\"rows\":[]},\"entries\":{\"rows\":[]}}";
        jsonState.verifyJson(new JSONObject(json), getDbTables());
    }

    @Test
    public void verifyJson_missingTable()
            throws Exception {
        String json = "{\"entries\":{\"rows\":[]}}";
        try {
            jsonState.verifyJson(new JSONObject(json), getDbTables());
            fail("Did not throw exception");
        } catch (IOException e) {
            assertThat(
                    e.getMessage(),
                    is("Json contents does not reference table feeds")
            );
        }
    }

    @Test
    public void verifyJson_badTableValue()
            throws Exception {
        String json = "{\"entries\":{\"rows\":[]},\"feeds\":1}";
        try {
            jsonState.verifyJson(new JSONObject(json), getDbTables());
            fail("Did not throw exception");
        } catch (IOException e) {
            assertThat(
                    e.getMessage(),
                    is("Json table feeds is not an object")
            );
        }
    }

    @Test
    public void verifyJson_missingRows()
            throws Exception {
        String json = "{\"entries\":{\"rows\":[]},\"feeds\":{}}";
        try {
            jsonState.verifyJson(new JSONObject(json), getDbTables());
            fail("Did not throw exception");
        } catch (IOException e) {
            assertThat(
                    e.getMessage(),
                    is("Json table feeds is not a table object")
            );
        }
    }

    @Test
    public void verifyJson_badRowData()
            throws Exception {
        String json = "{\"entries\":{\"rows\":[]}},\"feeds\":{\"rows\":1}}";
        try {
            jsonState.verifyJson(new JSONObject(json), getDbTables());
            fail("Did not throw exception");
        } catch (IOException e) {
            assertThat(
                    e.getMessage(),
                    is("Json contents does not reference table feeds")
            );
        }
    }

    @Test
    public void verifyJson_missingColumn()
            throws Exception {
        String json = "{\"every\":{\"rows\":["
                + "{\"t2\": \"b\", \"i1\": 1, \"i2\": 2, \"z\": 0, \"d\": 100012002, \"b\": \"AQIDBA==\"}"
                + "]}}";
        try {
            jsonState.verifyJson(new JSONObject(json), new DbTable[]{EVERY_TYPE_TABLE});
            fail("Did not throw error");
        } catch (IOException e) {
            assertThat(
                    e.getMessage(),
                    is("Json table every missing columns [t1]")
            );
        }
    }

    @Test
    public void verifyJson_extraColumn()
            throws Exception {
        String json = "{\"every\":{\"rows\":["
                + "{\"a\": 1, \"t1\": \"a\", \"t2\": \"b\", \"i1\": 1, \"i2\": 2, \"z\": 0, \"d\": 100012002, \"b\": \"AQIDBA==\"}"
                + "]}}";
        try {
            jsonState.verifyJson(new JSONObject(json), new DbTable[] { EVERY_TYPE_TABLE });
            fail("Did not throw error");
        } catch (IOException e) {
            assertThat(
                    e.getMessage(),
                    is("Json table every contains unknown column a")
            );
        }
    }

    @Test
    public void writeJson()
            throws Exception {
    }

    @Test
    public void writeJsonTable()
            throws JSONException, IOException {
        SQLiteDatabase mockSql = mock(SQLiteDatabase.class);
        Cursor mockQuery = mock(Cursor.class);
        for (int i = 0; i < EVERY_TYPE_TABLE.getColumnCount(); i++) {
            when(mockQuery.getColumnIndex(EVERY_TYPE_TABLE.getColumnName(i))).thenReturn(i);
        }
        Answer moveToNextAnswer = new Answer() {
            int count = 0;
            @Override
            public Object answer(InvocationOnMock invocation)
                    throws Throwable {
                return (count++) < 3;
            }
        };
        when(mockQuery.moveToNext()).then(moveToNextAnswer);
        // + "{\"t1\": \"a\", \"t2\": \"b\", \"i1\": 1, \"i2\": 2, \"z\": 0, \"d\": 100012002, \"b\": \"AQIDBA==\"}"
        // index 0 == primary key
        when(mockQuery.getString(1)).thenReturn("a"); // t1
        when(mockQuery.getString(2)).thenReturn("b"); // t2
        when(mockQuery.getInt(3)).thenReturn(1); // i1
        when(mockQuery.getInt(4)).thenReturn(2); // i2
        when(mockQuery.getInt(5)).thenReturn(0); // z
        when(mockQuery.getLong(6)).thenReturn(100012002L); // d
        when(mockQuery.getBlob(7)).then(new Answer<byte[]>() { // b
            int count = 0;
            @Override
            public byte[] answer(InvocationOnMock invocation)
                    throws Throwable {
                count++;
                if (count <= 1) {
                    return null;
                }
                return new byte[] { 1, 2, 3, 4 };
            }
        });
        when(mockSql.query(
                EVERY_TYPE_TABLE.getTableName(), EVERY_TYPE_TABLE.getColumnNames(),
                null, null, null, null, null))
            .thenReturn(mockQuery);
        DbTableFacade db = new DbTableFacade.SQLiteFacade(mockSql, EVERY_TYPE_TABLE.getTableName());

        StringWriter out = new StringWriter();
        jsonState.writeJsonTable(out, db, EVERY_TYPE_TABLE);

        assertThat(out.toString(), is("{\"rows\":["
                // cursor runs 3 times.
                + "{\"t1\":\"a\",\"t2\":\"b\",\"i1\":1,\"i2\":2,\"z\":0,\"d\":100012002,\"b\":null},"
                + "{\"t1\":\"a\",\"t2\":\"b\",\"i1\":1,\"i2\":2,\"z\":0,\"d\":100012002,\"b\":\"AQIDBA==\"},"
                + "{\"t1\":\"a\",\"t2\":\"b\",\"i1\":1,\"i2\":2,\"z\":0,\"d\":100012002,\"b\":\"AQIDBA==\"}]}"));
    }

    @Test
    public void writeJsonTable_noRows()
            throws JSONException, IOException {
        SQLiteDatabase mockSql = mock(SQLiteDatabase.class);
        Cursor mockQuery = mock(Cursor.class);
        for (int i = 0; i < EVERY_TYPE_TABLE.getColumnCount(); i++) {
            when(mockQuery.getColumnIndex(EVERY_TYPE_TABLE.getColumnName(i))).thenReturn(i);
        }
        when(mockQuery.moveToNext()).thenReturn(false);
        when(mockSql.query(
                EVERY_TYPE_TABLE.getTableName(), EVERY_TYPE_TABLE.getColumnNames(),
                null, null, null, null, null))
                .thenReturn(mockQuery);
        DbTableFacade db = new DbTableFacade.SQLiteFacade(mockSql, EVERY_TYPE_TABLE.getTableName());

        StringWriter out = new StringWriter();
        jsonState.writeJsonTable(out, db, EVERY_TYPE_TABLE);

        assertThat(out.toString(), is("{\"rows\":[]}"));
    }



    private static final List<ContentValues> MOCKED_CONTENT_VALUES = new ArrayList<>();
    private static class UTJsonState extends JsonState {

        ContentValues createContentValues() {
            ContentValues mock = mock(ContentValues.class);
            MOCKED_CONTENT_VALUES.add(mock);
            return mock;
        }
    }
}