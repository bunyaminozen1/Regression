package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class OddDatabaseHelper {

    private final static String DATABASE_NAME = "outgoing_direct_debits";

    public static Map<Integer, Map<String, String>> getMandateByMerchantName(final String merchantName) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.mandate m where m.merchant_name = %s;", DATABASE_NAME, merchantName));
    }

    public static Map<Integer, Map<String, String>> getMandatesByInstrumentId(final String instrumentId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.mandate m where m.instrument_id = %s;", DATABASE_NAME, instrumentId));
    }

    public static Map<Integer, Map<String, String>> getPaidCollectionStates(final String simulatorCollectionId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.collection c JOIN qa_outgoing_direct_debits.collection_status cs on c.id = cs.collection_id WHERE c.provider_reference = '%s' AND status IN ('COLLECTION_PAID_LOCAL_BALANCE_DEBITED', 'COLLECTION_PAID_REMOTE_BALANCE_DEBITED');", DATABASE_NAME, simulatorCollectionId));
    }
}
