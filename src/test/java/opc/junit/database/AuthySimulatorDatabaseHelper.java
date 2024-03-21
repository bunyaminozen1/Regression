package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class AuthySimulatorDatabaseHelper {

    private final static String DATABASE_NAME = "authy_simulator";

    public static void expirePaymentInitiationRequest(final String id) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.request SET seconds_to_expire = 1 WHERE hidden_details = '{\"correlation_id\":\"%s\"}';", DATABASE_NAME, id));
    }

    public static void expireIdentityEnrolmentRequest(final String identityId) throws SQLException {

        final String authyUserId = AuthyDatabaseHelper.getAuthyUserId(identityId).get(0).get("id");
        final String simulatorRequestId = AuthyDatabaseHelper.getSimulatorRequestId(authyUserId).get(0).get("ext_request_id");

        DatabaseHelper
                .update(String.format("UPDATE %s.request SET seconds_to_expire = 1 WHERE id = '%s';", DATABASE_NAME, simulatorRequestId));
    }

    public static Map<Integer, Map<String, String>> getNotification(final String programmeId) throws SQLException {

        final String externalAppId = AuthyDatabaseHelper.getExternalAppId(programmeId).get(0).get("ext_app_id");
        return DatabaseHelper.retrieveData(String.format("SELECT * FROM %s.request r WHERE r.app_id = '%s' ORDER BY r.creation_timestamp DESC;", DATABASE_NAME, externalAppId));
    }
}
