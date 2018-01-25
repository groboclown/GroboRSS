/*
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

package net.groboclown.groborss.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static net.groboclown.groborss.provider.FeedData.TYPE_BLOB;
import static net.groboclown.groborss.provider.FeedData.TYPE_BOOLEAN;
import static net.groboclown.groborss.provider.FeedData.TYPE_DATETIME;
import static net.groboclown.groborss.provider.FeedData.TYPE_INT;
import static net.groboclown.groborss.provider.FeedData.TYPE_INT_7;
import static net.groboclown.groborss.provider.FeedData.TYPE_PRIMARY_KEY;
import static net.groboclown.groborss.provider.FeedData.TYPE_TEXT;
import static net.groboclown.groborss.provider.FeedData.TYPE_TEXT_UNIQUE;

/**
 * Reflects the state of the database as a json file.
 */
public class JsonState {
    public static void readJsonFile(@NonNull String filename,
            @NonNull DbTableFacadeFactory dbFactory, @NonNull DbTable[] tables)
            throws IOException, JSONException {
        new JsonState().readJson(new InputStreamReader(new FileInputStream(filename)), dbFactory, tables);
    }

    public static void writeJsonFile(@NonNull String filename,
            @NonNull DbTableFacadeFactory dbFactory, @NonNull DbTable[] tables)
            throws IOException, JSONException {
        new JsonState().writeJson(new OutputStreamWriter(new FileOutputStream(filename)), dbFactory, tables);
    }

    public void readJson(@NonNull Reader reader, @NonNull DbTableFacadeFactory dbFactory,
            @NonNull DbTable[] tables)
            throws IOException, JSONException {
        StringWriter read = new StringWriter();
        char[] buf = new char[4096];
        int len;
        while ((len = reader.read(buf, 0, 4096)) > 0) {
            read.write(buf, 0, len);
        }
        readJson(read.toString(), dbFactory, tables);
    }

    public void readJson(@NonNull String jsonStr, @NonNull DbTableFacadeFactory dbFactory,
            @NonNull DbTable[] tables)
            throws JSONException, IOException {
        readJson(new JSONObject(new JSONTokener(jsonStr)), dbFactory, tables);
    }

    public void readJson(@NonNull JSONObject json, @NonNull DbTableFacadeFactory dbFactory,
            @NonNull DbTable[] tables)
            throws IOException, JSONException {
        // Before any writes to the db happen, make sure the json object looks right.
        verifyJson(json, tables);

        for (int t = 0; t < tables.length; t++) {
            // First, clear out that row.
            DbTableFacade db = dbFactory.get(tables[t].getTableName());
            db.delete(null, null);
            JSONObject table = json.optJSONObject(tables[t].getTableName());
            JSONArray rows = table.optJSONArray("rows");
            ContentValues[] valuesList = new ContentValues[rows.length()];
            for (int i = 0; i < rows.length(); i++) {
                valuesList[i] = createContentValues();
                pullRowDataFromJson(valuesList[i], rows.getJSONObject(i), tables[i]);
            }
            db.bulkInsert(valuesList);
        }
    }

    private void pullRowDataFromJson(
            @NonNull ContentValues values, @NonNull JSONObject jsonObject,
            DbTable table)
            throws JSONException {
        for (int i = 0; i < table.getColumnCount(); i++) {
            writeValueFromJson(values, table.getColumnName(i), table.getColumnType(i), jsonObject);
        }
    }

    void verifyJson(JSONObject json, DbTable[] tables)
            throws IOException {
        for (int t = 0; t < tables.length; t++) {
            String name = tables[t].getTableName();
            if (! json.has(name) || json.isNull(name)) {
                throw new IOException("Json contents does not reference table " + name);
            }
            JSONObject table = json.optJSONObject(name);
            if (table == null) {
                throw new IOException("Json table " + name + " is not an object");
            }
            if (! table.has("rows") || table.isNull("rows")) {
                throw new IOException("Json table " + name + " is not a table object");
            }

            // If there are rows, check that the first row has the right data.
            JSONArray rows = table.optJSONArray("rows");
            if (rows == null) {
                throw new IOException("Json table " + name + " is not a table object");
            }
            if (rows.length() > 0) {
                JSONObject row = rows.optJSONObject(0);
                Set<String> columns = new HashSet<>();
                for (int i = 0; i < tables[t].getColumnCount(); i++) {
                    // ignore primary key
                    if (!TYPE_PRIMARY_KEY.equals(tables[t].getColumnType(i))) {
                        columns.add(tables[t].getColumnName(i));
                    }
                }
                for (Iterator<String> itr = row.keys(); itr.hasNext();) {
                    String key = itr.next();
                    if (!columns.remove(key)) {
                        throw new IOException("Json table " + name + " contains unknown column " + key);
                    }
                }
                if (!columns.isEmpty()) {
                    throw new IOException("Json table " + name + " missing columns " + columns);
                }
            }
        }
    }


    public void writeJson(@NonNull Writer writer, @NonNull DbTableFacadeFactory dbFactory,
            @NonNull DbTable[] tables)
            throws JSONException, IOException {
        writer.write(toJson(dbFactory, tables).toString());
    }


    JSONStringer toJson(@NonNull DbTableFacadeFactory dbFactory,
            @NonNull DbTable[] tables)
            throws JSONException {
        JSONStringer json = new JSONStringer();

        for (int i = 0; i < tables.length; i++) {
            json = json.key(tables[i].getTableName());
            json = json.value(writeJsonTable(json.object(), dbFactory.get(tables[i].getTableName()),
                    tables[i]).endObject());
        }

        return json;
    }


    JSONStringer writeJsonTable(
            JSONStringer json, DbTableFacade db, DbTable table)
            throws JSONException {
        Cursor cursor = db.query(table.getColumnNames(), null, null, null);

        // Map the queried column result indicies to the data types.
        String[] columnTypes = new String[table.getColumnCount()];
        String[] columnNames = new String[table.getColumnCount()];
        for (int i = 0; i < table.getColumnCount(); i++) {
            int index = cursor.getColumnIndex(table.getColumnName(i));
            columnNames[index] = table.getColumnName(i);
            columnTypes[index] = table.getColumnType(i);
        }

        json.key("rows");
        json.array();
        try {
            while (cursor != null && cursor.moveToNext()) {
                json.object();
                for (int index = 0; index < table.getColumnCount(); index++) {
                    writeJsonColumn(json, cursor, index, columnNames[index], columnTypes[index]);
                }
                json.endObject();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return json.endArray();
    }

    private JSONStringer writeJsonColumn(
            @NonNull JSONStringer obj, @NonNull Cursor cursor, int index,
            @NonNull String columnName, @NonNull String type)
            throws JSONException {
        if (TYPE_PRIMARY_KEY.equals(type)) {
            // ignore primary key
            return obj;
        }
        obj.key(columnName);
        if (TYPE_TEXT.equals(type) || TYPE_TEXT_UNIQUE.equals(type)) {
            return obj.value(cursor.getString(index));
        }
        if (TYPE_INT.equals(type) || TYPE_INT_7.equals(type)) {
            return obj.value(cursor.getInt(index));
        }
        if (TYPE_BOOLEAN.equals(type)) {
            return obj.value(cursor.getInt(index));
        }
        if (TYPE_DATETIME.equals(type)) {
            return obj.value(cursor.getLong(index));
        }
        if (TYPE_BLOB.equals(type)) {
            byte[] bytes = cursor.getBlob(index);
            obj.array();
            for (int i = 0; i < bytes.length; i++) {
                obj.value(bytes[i]);
            }
            return obj.endArray();
        }
        throw new IllegalArgumentException("Unknown column type " + type);
    }

    private void writeValueFromJson(
            ContentValues values, String columnName, String type, JSONObject json)
            throws JSONException {
        if (TYPE_PRIMARY_KEY.equals(type)) {
            // ignore primary key
            return;
        }
        if (TYPE_TEXT.equals(type) || TYPE_TEXT_UNIQUE.equals(type)) {
            values.put(columnName, json.getString(columnName));
            return;
        }
        if (TYPE_INT.equals(type) || TYPE_INT_7.equals(type) || TYPE_BOOLEAN.equals(type)) {
            values.put(columnName, json.getInt(columnName));
            return;
        }
        if (TYPE_DATETIME.equals(type)) {
            values.put(columnName, json.getLong(columnName));
            return;
        }
        if (TYPE_BLOB.equals(type)) {
            JSONArray ar = json.getJSONArray(columnName);
            byte[] bytes = new byte[ar.length()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) ar.getInt(i);
            }
            values.put(columnName, bytes);
            return;
        }
        throw new IllegalArgumentException("Unknown column type " + type);
    }

    ContentValues createContentValues() {
        return new ContentValues();
    }
}
