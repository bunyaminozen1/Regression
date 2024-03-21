package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class LinkedAccountsDataBaseHelper {
    private final static String DATABASE_NAME = "linked_accounts";

    public static Map<Integer, Map<String, String>> getLinkedAccount(final String linkedAccountId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.linked_account la where la.id = %s;", DATABASE_NAME, linkedAccountId));
    }
}
