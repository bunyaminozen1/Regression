package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class AuthSessionsDatabaseHelper {

    private final static String DATABASE_NAME = "auth_sessions";

    public static Map<Integer, Map<String, String>> getCredentials(final String identityId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.credential c where c.identity_id = %s;", DATABASE_NAME, identityId));
    }

    public static Map<Integer, Map<String, String>> getChallenge(final String challengeId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.credential_factor_challenge c where c.id = %s;", DATABASE_NAME, challengeId));
    }

    public static Map<Integer, Map<String, String>> getChallengeWithIdentityId(final String identityId) throws SQLException {
        return DatabaseHelper
            .retrieveData(String.format("SELECT * FROM %s.credential_factor_challenge c where c.identity_id = %s;", DATABASE_NAME, identityId));
    }

    public static Map<Integer, Map<String, String>> getCredentialFactors(final String credentialId) throws SQLException {
        return DatabaseHelper
            .retrieveData(String.format("SELECT * FROM %s.credential_factor cf where cf.credential_id = %s;", DATABASE_NAME, credentialId));
    }

    public static Map<Integer, Map<String, String>> getBiometricFactor(final String credentialId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.credential_factor cf where type = 'OKAY_PUSH' and cf.credential_id = %s;", DATABASE_NAME, credentialId));
    }
}
