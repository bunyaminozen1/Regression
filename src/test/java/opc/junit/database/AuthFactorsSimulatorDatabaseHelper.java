package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class AuthFactorsSimulatorDatabaseHelper {

    private final static String DATABASE_NAME = "weavr_authfactors_simulator";

    public static Map<Integer, Map<String, String>> getLatestFakeOtp(final String identityId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.fake_otp fo where fo.credentials_id = %s order by fo.expiry_timestamp DESC;", DATABASE_NAME, identityId));
    }
}
