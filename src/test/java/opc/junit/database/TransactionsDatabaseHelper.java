package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;

public class TransactionsDatabaseHelper {

    private final static String DATABASE_NAME = "transaction_batches";

    public static void updateCompletedTransactionTimestamp(final Long timestamp, final String cardId) throws SQLException {
        final String query =
                String.format("UPDATE %s.batch set completed_timestamp = %s " +
                        "WHERE id in (SELECT batch_id from %s.batch_instrument WHERE instrument_id = %s);",
                        DATABASE_NAME,
                        timestamp,
                        DATABASE_NAME,
                        cardId);

        DatabaseHelper.update(query);
    }
}
