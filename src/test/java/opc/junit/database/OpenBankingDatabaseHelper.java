package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class OpenBankingDatabaseHelper {

    private final static String DATABASE_NAME = "openbanking";

    public static Map<Integer, Map<String, String>> getPaymentConsent(final String consentId) {
        try {
            return DatabaseHelper
                    .retrieveData(String.format("SELECT * FROM %s.consent_pis cp where cp.id = %s;", DATABASE_NAME, consentId));
        } catch (SQLException exception) {
            return null;
        }
    }
}
