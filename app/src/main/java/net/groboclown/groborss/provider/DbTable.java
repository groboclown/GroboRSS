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

/**
 * Simple API for generic database table schema declaration introspection.
 */
class DbTable {
    private final String tableName;
    private final String[] columns;
    private final String[] types;

    DbTable(String tableName, String[] columns, String[] types) {
        if (tableName == null || columns == null || types == null
                || types.length != columns.length || types.length == 0) {
            throw new IllegalArgumentException(
                    "Invalid parameters for creating table " + tableName);
        }
        this.tableName = tableName;
        this.columns = columns;
        this.types = types;
    }

    String getTableName() {
        return tableName;
    }

    int getColumnCount() {
        return columns.length;
    }

    String getColumnName(int index) {
        return columns[index];
    }

    String getColumnType(int index) {
        return types[index];
    }

    String[] getColumnNames() {
        // NOTE: should create copy of the string to prevent modifying internal structure.
        return columns;
    }
}
