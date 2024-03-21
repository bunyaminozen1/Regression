package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class OkayDatabaseHelper {

    private final static String DATABASE_NAME = "okay";

    public static Map<Integer, Map<String, String>> getExternalUserId(final String identityId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.`user` u where u.credential_id = '%s';",
                        DATABASE_NAME, identityId));
    }

    public static Map<Integer, Map<String, String>> getBiometricChallenge(final String challengeId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.request r WHERE r.id = %s;",
                        DATABASE_NAME, challengeId));
    }

    public static Map<Integer, Map<String, String>> getApp(final String programmeId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.app a WHERE a.programme_id = %s;", DATABASE_NAME, programmeId));
    }

    public static Map<Integer, Map<String, String>> getUserRequest(final String credentialId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.request r join %s.`user` u on r.user_id = u.id where u.credential_id = %s", DATABASE_NAME, DATABASE_NAME, credentialId));
    }
}
