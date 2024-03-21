package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class TransfersDatabaseHelper {

    private final static String DATABASE_NAME = "transfers";

    public static void updateTransferState(final String state,
                                           final String transferId) {
        try {
            DatabaseHelper
                    .update(String.format("UPDATE %s.transfer SET state = '%s' WHERE id = %s;", DATABASE_NAME, state, transferId));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static Map<Integer, Map<String, String>> getTransferById(final String transferId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.transfer t where t.id = %s;", DATABASE_NAME, transferId));
    }

}
