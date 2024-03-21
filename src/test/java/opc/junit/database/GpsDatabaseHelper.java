package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class GpsDatabaseHelper {

    private final static String DATABASE_NAME = "gps";

    public static Map<Integer, Map<String, String>> getInstrument(final String managedCardId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT i.* FROM %s.ehi_get_transaction_message egtm JOIN %s.instrument i ON egtm.token = i.public_token WHERE i.id = %s;", DATABASE_NAME, DATABASE_NAME, managedCardId));
    }

    public static void expireAuthDate(final String instrumentId,
                                      final Long pastTime) throws SQLException {
        DatabaseHelper.update(String.format("UPDATE %s.purchase_history_running_balance SET authorisation_expiry_date = '%s' WHERE instrument_id = '%s';", DATABASE_NAME, pastTime, instrumentId));

    }

    public static Map<Integer, Map<String, String>> getCardDetailsbyId(final String managedCardId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT lfc.* FROM %s.instrument i JOIN gps_simulator.lib_fake_card lfc on i.public_token = lfc.external_reference WHERE i.id = %s;", DATABASE_NAME, managedCardId));
    }
}
