package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class ConsumersDatabaseHelper {

    private final static String DATABASE_NAME = "consumers";

    public static Map<Integer, Map<String, String>> getConsumer(final String consumerId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.consumer c where c.id = %s;", DATABASE_NAME, consumerId));
    }

    public static Map<Integer, Map<String, String>> getEmailNonce(final String userId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.lib_email_nonce len where len.identity_id = %s;", DATABASE_NAME, userId));
    }

    public static void setDateOfBirthNull(final String consumerId) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.consumer SET date_of_birth = NULL where id = %s;", DATABASE_NAME, consumerId));
    }

    public static void deleteConsumerAddress(final String consumerId) throws SQLException {
        DatabaseHelper
                .update(String.format("DELETE from %s.address where consumer_id = %s;", DATABASE_NAME, consumerId));
    }

    public static Map<Integer, Map<String, String>> getConsumerUser(final String consumerId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.consumer_user cu where cu.consumer_id = %s;", DATABASE_NAME, consumerId));
    }

    public static void updateConsumerKyc(final String kycStatus, final String consumerId) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.consumer SET full_verification_status = '%s', ongoing_kyc_status = '%s' where id = %s;",
                        DATABASE_NAME, kycStatus, kycStatus, consumerId));
    }

    public static Map<Integer, Map<String, String>> getConsumerAddress(final String consumerId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.address a where a.consumer_id = %s;", DATABASE_NAME, consumerId));
    }

    public static Map<Integer, Map<String, String>> getDuplicateIdentity(final String consumerId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.consumer_duplicate cd WHERE cd.consumer_id = %s;", DATABASE_NAME, consumerId));
    }

    public static void updateEmailValidationProvider(final String profileId, final String emailValidationProvider) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.email_validation_provider SET email_validation_provider = '%s' where profile_id = %s;",
                        DATABASE_NAME, emailValidationProvider, profileId));
    }
    public static Map<Integer, Map<String, String>> getConsumerSmsNewestToOldest(final String consumerId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.sms_audit sa WHERE sa.consumer_user_id = %s ORDER by creation_timestamp DESC;", DATABASE_NAME, consumerId));
    }

    public static void updateUser(final String userId, final String isSelected) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.consumer_user cu SET cu.selected_login = %s WHERE cu.consumer_user_id = %s;", DATABASE_NAME, isSelected, userId));
    }
}
