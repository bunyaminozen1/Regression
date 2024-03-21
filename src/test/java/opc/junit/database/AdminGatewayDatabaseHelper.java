package opc.junit.database;

import java.sql.SQLException;
import java.util.Map;
import opc.database.DatabaseHelper;

public class AdminGatewayDatabaseHelper {

  private final static String DATABASE_NAME = "admin_gateway";

  public static Map<Integer, Map<String, String>> getUserInvite(final String email) throws SQLException {
    return DatabaseHelper
        .retrieveData(String.format("SELECT * FROM %s.administrator_invite ai where ai.email = '%s';", DATABASE_NAME, email));
  }
}
