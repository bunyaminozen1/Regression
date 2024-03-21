package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;

public class TokenizerDatabaseHelper {

    private final static String DATABASE_NAME = "tokenizer";

    public static void updatePurgeTimestamp(final String token) throws SQLException {
        final long timestamp = System.currentTimeMillis() - 1300000;
        DatabaseHelper
                .update(String.format("UPDATE %s.token SET purge_timestamp = %s WHERE token = '%s';", DATABASE_NAME, timestamp, token));
    }
}
