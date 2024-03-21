package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class AuthFactorsDatabaseHelper {

    private final static String DATABASE_NAME = "weavr_authfactors";

    public static Map<Integer, Map<String, String>> getChallenge(final String identityId,
                                                                 final String type) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.challenge c where c.credentials_id = %s and c.`type` = '%s';", DATABASE_NAME, identityId, type));
    }

    public static void updateAccountInformationState(final String state, final String identityId) throws SQLException{
        DatabaseHelper.update(String.format("UPDATE %s.challenge c SET status = '%s' where c.credentials_id = %s and c.`type` = 'ACCOUNT_INFORMATION';", DATABASE_NAME, state, identityId));
    }

    public static Map<Integer, Map<String, String>> getChannel(final String identityId,
        final String type) throws SQLException {
        return DatabaseHelper
            .retrieveData(String.format("SELECT * FROM %s.channel c where c.credentials_id = %s and c.`channel_type` = '%s';", DATABASE_NAME, identityId, type));
    }
}
