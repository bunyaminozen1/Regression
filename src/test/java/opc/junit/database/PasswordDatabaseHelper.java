package opc.junit.database;

import opc.database.DatabaseHelper;
import java.sql.SQLException;
import java.util.Map;

public class PasswordDatabaseHelper {

    private final static String DATABASE_NAME = "passwords";

    public static Map<Integer, Map<String, String>> getPasswordNonce(final String identityId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.lost_password_nonce lpn where lpn.credential_id = %s;", DATABASE_NAME, identityId));
    }

    public static void updatePasswordProfile(final String tenantId, final String newComplexity, final String existingComplexity) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.profile_config SET complexity=%s WHERE auth_type='PASSWORD' AND complexity=%s AND tenant_id=%s;",
                        DATABASE_NAME, newComplexity, existingComplexity, tenantId));
    }
}
