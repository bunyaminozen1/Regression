package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PayneticsDatabaseHelper {

    private final static String DATABASE_NAME = "paynetics";

    public static Map<Integer, Map<String, String>> getPayneticsAccount(final String managedAccountId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.account a where a.associated_instrument_id = %s and balance_token is not null;", DATABASE_NAME, managedAccountId));
    }

    public static Map<Integer, Map<String, String>> getPayneticsUserCreateLog(final String identityId) throws SQLException {
        final long requestTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.client_v3_api_log cval WHERE request_time > %s AND operation = 'user_create' AND request_payload LIKE '%%%s%%';", DATABASE_NAME, requestTime, identityId));
    }

    public static Map<Integer, Map<String, String>> getNaturalPersonsApiLogs(final String identityId) throws SQLException {
        final long requestTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.client_v3_api_log cval WHERE request_time > %s AND operation = 'user_create' AND request_payload LIKE '%%%s%%';", DATABASE_NAME, requestTime, identityId));
    }

    public static Map<Integer, Map<String, String>> getUpdateNaturalPersonsApiLogs(final String identityId) throws SQLException {
        final long requestTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.client_v3_api_log cval WHERE request_time > %s AND operation = 'user_update' AND request_payload LIKE '%%%s%%';", DATABASE_NAME, requestTime, identityId));
    }

    public static Map<Integer, Map<String, String>> getCorporatesV3ApiLogs(final String corporateEmail) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.client_v3_api_log cval WHERE operation = 'application_create' and request_payload LIKE '%%%s%%';", DATABASE_NAME, corporateEmail));
    }
}
