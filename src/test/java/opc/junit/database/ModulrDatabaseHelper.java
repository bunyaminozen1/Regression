package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class ModulrDatabaseHelper {

    private final static String DATABASE_NAME = "modulr";

    public static boolean isIdentitySubscribed(final String identityId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.subscription s WHERE s.subscriber_id = %s;", DATABASE_NAME, identityId)).size() > 0;
    }

    public static Map<Integer, Map<String, String>> getSubscriber(final String identityId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.subscription s WHERE s.subscriber_id = %s;", DATABASE_NAME, identityId));
    }
}
