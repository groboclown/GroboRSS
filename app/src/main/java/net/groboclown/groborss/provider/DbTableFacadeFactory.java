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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public interface DbTableFacadeFactory {
    DbTableFacade get(@NonNull String table);

    class SQLiteFactory implements DbTableFacadeFactory {
        private final SQLiteDatabase db;

        public SQLiteFactory(@NonNull SQLiteDatabase db) {
            this.db = db;
        }

        @Override
        public DbTableFacade get(@NonNull String table) {
            return new DbTableFacade.SQLiteFacade(db, table);
        }
    }

    class ContextFactory implements DbTableFacadeFactory {
        private final Context context;
        private final Map<String, Uri> mapping = new HashMap<>();

        public ContextFactory(@NonNull Context context, @NonNull String table, @NonNull Uri uri) {
            this.context = context;
            this.mapping.put(table, uri);
        }

        public ContextFactory(@NonNull Context context,
                @NonNull String table1, @NonNull Uri uri1,
                @NonNull String table2, @NonNull Uri uri2) {
            this.context = context;
            this.mapping.put(table1, uri1);
            this.mapping.put(table2, uri2);
        }

        @Override
        public DbTableFacade get(@NonNull String table) {
            Uri resource = mapping.get(table);
            if (resource == null) {
                throw new IllegalArgumentException("unknown table name " + table);
            }
            return new DbTableFacade.ContextFacade(context, resource);
        }
    }
}
