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
