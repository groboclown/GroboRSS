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
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * A facade on top of the SQLite and Activity for simple database operations.
 */
public interface DbTableFacade {
    void insert(ContentValues values);
    int bulkInsert(ContentValues[] values);
    int delete(String where, String[] selectionArgs);
    Cursor query(String[] projection, String selection, String[] selectionArgs, String sortOrder);

    class SQLiteFacade implements DbTableFacade {
        private final SQLiteDatabase database;
        private final String table;

        public SQLiteFacade(@NonNull SQLiteDatabase database, @NonNull String table) {
            this.database = database;
            this.table = table;
        }

        @Override
        public void insert(ContentValues values) {
            database.insert(table, null, values);
        }

        @Override
        public int bulkInsert(ContentValues[] values) {
            database.beginTransactionNonExclusive();
            for (int i = 0; i < values.length; i++) {
                database.insert(table, null, values[i]);
            }
            database.endTransaction();
            return values.length;
        }

        @Override
        public int delete(String where, String[] selectionArgs) {
            return database.delete(table, where, selectionArgs);
        }

        @Override
        public Cursor query(
                String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            return database.query(table, projection, selection, selectionArgs, null, null, sortOrder);
        }
    }


    class ContextFacade implements DbTableFacade {
        private final Context context;
        private final Uri resource;

        public ContextFacade(@NonNull Context context, @NonNull Uri resource) {
            this.context = context;
            this.resource = resource;
        }

        @Override
        public void insert(ContentValues values) {
            context.getContentResolver().insert(resource, values);
        }

        @Override
        public int bulkInsert(ContentValues[] values) {
            return context.getContentResolver().bulkInsert(resource, values);
        }

        @Override
        public int delete(String where, String[] selectionArgs) {
            return context.getContentResolver().delete(resource, where, selectionArgs);
        }

        @Override
        public Cursor query(
                String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            return context.getContentResolver().query(resource, projection, selection, selectionArgs, sortOrder);
        }
    }
}
