package opc.junit.database;

import java.util.Map;
import opc.database.DatabaseHelper;

import java.sql.SQLException;

public class SendsDatabaseHelper {

    private final static String DATABASE_NAME = "send";

    public static void updateSendState(final String state,
                                       final String sendId) {
        try {
            DatabaseHelper
                    .update(String.format("UPDATE %s.send SET state = '%s' WHERE id = %s;", DATABASE_NAME, state, sendId));
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static Map<Integer, Map<String, String>> getSendById(final String sendId) throws SQLException {
        return DatabaseHelper
            .retrieveData(String.format("SELECT * FROM %s.send send where send.id = %s;", DATABASE_NAME, sendId));
    }
}
