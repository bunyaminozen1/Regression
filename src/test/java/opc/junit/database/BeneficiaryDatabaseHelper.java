package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class BeneficiaryDatabaseHelper {

    public static final String provider_reference = "provider_reference";
    private final static String DATABASE_NAME = "corporates";
    public static Map<Integer, Map<String, String>> findByCorporateId(String corporateId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select b.*\n" +
                        "from %s.corporate_beneficiary_mapping m\n" +
                        "join %s.beneficiary b on b.id = m.beneficiary_id\n" +
                        "where m.corporate_id = %s", DATABASE_NAME, DATABASE_NAME, corporateId));
    }
}
