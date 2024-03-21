package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class BeneficiaryAdditionalInfoDatabaseHelper {

  private final static String DATABASE_NAME = "corporates";

  public static Map<Integer, Map<String, String>> getBeneficiaryAdditionalInfo(final String propertyName, final String beneficiaryId) throws SQLException {
    return DatabaseHelper
        .retrieveData(String.format("SELECT * FROM %s.beneficiary_additional_info bai WHERE bai.property_name = '%s' "
                + "HAVING bai.beneficiary_id = %s", DATABASE_NAME, propertyName, beneficiaryId));
  }
}
