package opc.junit.database;

import opc.database.DatabaseHelper;
import opc.enums.opc.KybState;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class CorporatesDatabaseHelper {

    private final static String DATABASE_NAME = "corporates";

    public static Map<Integer, Map<String, String>> getCorporate(final String corporateId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.corporate c where c.id = %s;", DATABASE_NAME, corporateId));
    }

    public static Map<Integer, Map<String, String>> getBeneficiaryByRefId(final String beneficiaryId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.beneficiary b where b.reference_id = %s;", DATABASE_NAME, beneficiaryId));
    }

    public static Map<Integer, Map<String, String>> getEmailNonce(final String userId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.lib_email_nonce len where len.identity_id = %s;", DATABASE_NAME, userId));
    }

    public static Map<Integer, Map<String, String>> getCorporateRegisteredAddress(final String corporateId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.address a where a.corporate_id = %s and a.address_type = 'REGISTERED';", DATABASE_NAME, corporateId));
    }

    public static Map<Integer, Map<String, String>> getCorporateUser(final String corporateUserId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.corporate_user cu where cu.corporate_user_id = %s;", DATABASE_NAME, corporateUserId));
    }

    public static void updateCorporateKyb(final KybState kybState, final String corporateId) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.corporate SET ongoing_kyb_status = '%s' where id = %s;",
                        DATABASE_NAME, kybState.name(), corporateId));
    }

    public static Map<Integer, Map<String, String>> getBeneficiary(final String externalReferenceId) throws SQLException {
        return DatabaseHelper
            .retrieveData(String.format("SELECT * FROM %s.beneficiary b where b.provider_reference = '%s';", DATABASE_NAME, externalReferenceId));
    }

    public static int updateRepresentative(final String representativeExternalId) throws SQLException {
        return DatabaseHelper
                .update(String.format("UPDATE %s.beneficiary SET place_of_birth = 'MT', identification_number = '123456', address_line1 = 'R1', address_line2 = 'R2', address_city = 'Rcity', address_country = 'MT', address_post_code = 'RMT123', address_state = 'RState' WHERE provider_reference = ?;",
                        DATABASE_NAME), List.of(representativeExternalId));
    }

    public static int updateUbo(final String uboExternalId) throws SQLException {
        return DatabaseHelper
                .update(String.format("UPDATE %s.beneficiary SET place_of_birth = 'MT', identification_number = '654321' WHERE provider_reference = ?;",
                        DATABASE_NAME), List.of(uboExternalId));
    }

    public static int updateRepresentativeAddress(final String corporateId) throws SQLException {
        return DatabaseHelper
                .update(String.format("UPDATE %s.address SET country = 'MT' WHERE address_type = 'REGISTERED' and corporate_id = ?;",
                        DATABASE_NAME), List.of(corporateId));
    }
    public static Map<Integer, Map<String, String>> getBeneficiaryAdditionalInfo(final String propertyName, final String beneficiaryId) throws SQLException {
        return DatabaseHelper
            .retrieveData(String.format("SELECT * FROM %s.beneficiary_additional_info bai WHERE bai.property_name = '%s' "
                + "HAVING bai.beneficiary_id = %s", DATABASE_NAME, propertyName, beneficiaryId));
    }

    public static Map<Integer, Map<String, String>> getBeneficiaryMapping(final String parentBeneficiaryId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.beneficiary_mapping bm WHERE parent_beneficiary_id = %s;" , DATABASE_NAME, parentBeneficiaryId));
    }

    public static Map<Integer, Map<String, String>> getBeneficiaryByExternalId(final String beneficiaryExternalId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.beneficiary b where b.provider_reference = '%s'; ", DATABASE_NAME, beneficiaryExternalId));
    }

    public static Map<Integer, Map<String, String>> getCorporateSmsNewestToOldest(final String consumerId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.sms_audit sa WHERE sa.corporate_user_id = %s ORDER by creation_timestamp DESC;", DATABASE_NAME, consumerId));
    }

    public static void updateUser(final String userId, final String isSelected) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.corporate_user cu SET cu.selected_login = %s WHERE cu.corporate_user_id = %s;", DATABASE_NAME, isSelected, userId));
    }
}
