package opc.junit.database;

import opc.database.DatabaseHelper;
import java.sql.SQLException;
import java.util.Map;

public class IncomingWireTransfersDataBaseHelper {
  private final static String DATABASE_NAME = "incoming_wire_transfers";

  public static Map<Integer, Map<String, String>> getIwtByIbanId(final String ibanId) throws SQLException {
    return DatabaseHelper
        .retrieveData(String.format("select * from %s.incoming_wire_transfer iwt where iwt.posted_iban_id = '%s';",
            DATABASE_NAME, ibanId));
  }

  public static Map<Integer, Map<String, String>> getIwtByIbanIdAndState(final String ibanId, final String state) throws SQLException {
    return DatabaseHelper
        .retrieveData(String.format("select * from %s.incoming_wire_transfer iwt where iwt.posted_iban_id = '%s' and state = '%s';",
            DATABASE_NAME, ibanId, state));
  }
}
