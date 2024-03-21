package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class PayneticsV2DatabaseHelper {
    private final static String DATABASE_NAME = "paynetics_v2";

    public static Map<Integer, Map<String, String>> getPayneticsAccount(final String managedAccountId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.account a where a.associated_instrument_id = %s;", DATABASE_NAME, managedAccountId));
    }
}
