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
