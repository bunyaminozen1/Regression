package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class SubscriptionsDatabaseHelper {
    private final static String DATABASE_NAME = "subscriptions";

    public static Map<Integer, Map<String, String>> getSubscription(final String identityId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.subscription s where s.subscriber_id = '%s';",
                        DATABASE_NAME, identityId));
    }

    public static Map<Integer, Map<String, String>> getSubscriber(final String identityId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.subscriber s where s.subscriber_id = '%s';",
                        DATABASE_NAME, identityId));
    }
}
