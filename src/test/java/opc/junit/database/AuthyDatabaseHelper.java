package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class AuthyDatabaseHelper {

    private final static String DATABASE_NAME = "authy";

    public static Map<Integer, Map<String, String>> getAuthyUserId(final String identityId) throws SQLException {
        return DatabaseHelper.retrieveData(String.format("SELECT * FROM %s.`user` u WHERE credential_id = '%s';", DATABASE_NAME, identityId));
    }

    public static Map<Integer, Map<String, String>> getSimulatorRequestId(final String userId) throws SQLException {
        return DatabaseHelper.retrieveData(String.format("SELECT * FROM %s.request r WHERE user_id = '%s';", DATABASE_NAME, userId));
    }

    public static Map<Integer, Map<String, String>> getExternalAppId(final String programmeId) throws SQLException {
        return DatabaseHelper.retrieveData(String.format("SELECT * FROM %s.app a WHERE a.programme_id = '%s';", DATABASE_NAME, programmeId));
    }

    public static Map<Integer, Map<String, String>> getRequestState(final String sessionId) throws SQLException{
        return DatabaseHelper.retrieveData(String.format("SELECT * FROM %s.request r WHERE r.id = '%s';", DATABASE_NAME, sessionId));
    }

    public static Map<Integer, Map<String, String>> getUserRequest(final String credentialId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.request r join %s.`user` u on r.user_id = u.id where u.credential_id = %s", DATABASE_NAME, DATABASE_NAME, credentialId));
    }
}
