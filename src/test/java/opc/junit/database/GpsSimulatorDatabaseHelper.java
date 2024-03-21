package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class GpsSimulatorDatabaseHelper {
    private final static String DATABASE_NAME = "gps_simulator";

    public static Map<Integer, Map<String, String>> getLatestSettlement() throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.lib_fake_card_settlement lfcs order by lfcs.transaction_timestamp DESC LIMIT 1;", DATABASE_NAME));
    }
}
