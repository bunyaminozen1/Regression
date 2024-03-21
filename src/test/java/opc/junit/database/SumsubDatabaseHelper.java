package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class SumsubDatabaseHelper {

    private final static String DATABASE_NAME = "sumsub";

    public static Map<Integer, Map<String, String>> getConsumerTenant(final String consumerId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.consumer_tenant ct where ct.consumer_id = %s;", DATABASE_NAME, consumerId));
    }

    public static Map<Integer, Map<String, String>> getCorporateTenant(final String corporateId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.corporate_tenant ct where ct.corporate_id = %s;", DATABASE_NAME, corporateId));
    }

    public static Map<Integer, Map<String, String>> getBeneficiary(final String applicantId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.beneficiary b where b.beneficiary_applicant_id = '%s';", DATABASE_NAME, applicantId));
    }
}
