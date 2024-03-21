package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class OwtDatabaseHelper {
    private final static String DATABASE_NAME = "outgoing_wire_transfers";

    public static Map<Integer, Map<String, String>> getOwt(final String sourceInstrumentId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.outgoing_wire_transfer owt where owt.source_instrument_id = %s;", DATABASE_NAME, sourceInstrumentId));
    }

    public static Map<Integer, Map<String, String>> getOwtById(final String owtId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.outgoing_wire_transfer owt where owt.id = %s;", DATABASE_NAME, owtId));
    }

    public static void updateOwtState(final String state, final String owtId) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.outgoing_wire_transfer SET state = '%s' where id = %s;", DATABASE_NAME, state, owtId));
    }
}
