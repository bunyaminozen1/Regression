package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class ManagedCardsDatabaseHelper {
    private final static String DATABASE_NAME = "managed_cards";

    public static Map<Integer, Map<String, String>> getLostStolenReplacementAdjustment(final String managedCardId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.adjustment a where a.card_id = '%s' and a.adjustment_type = 'LOST_STOLEN_REPLACEMENT_BALANCE_TRANSFER';",
                        DATABASE_NAME, managedCardId));
    }

    public static Map<Integer, Map<String, String>> getAuthorisation(final String authorisationId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.authorisation a where a.id = '%s';",
                        DATABASE_NAME, authorisationId));
    }

    public static void updateAuthState(final String authState,
                                       final String authType,
                                       final String managedCardId) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.authorisation SET auth_state = '%s' WHERE card_id = '%s' AND auth_type = '%s';", DATABASE_NAME, authState, managedCardId, authType));
    }

    public static Map<Integer, Map<String, String>> getPrepaidManagedCard(final String managedCardId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.managed_card mc JOIN %s.managed_card_prepaid mcp ON mc.id = mcp.id WHERE mc.id = '%s';",
                        DATABASE_NAME, DATABASE_NAME, managedCardId));
    }

    public static Map<Integer, Map<String, String>> getDebitManagedCard(final String managedCardId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.managed_card mc JOIN %s.managed_card_debit mcd ON mc.id = mcd.id WHERE mc.id = '%s';",
                        DATABASE_NAME, DATABASE_NAME, managedCardId));
    }

    public static Map<Integer, Map<String, String>> getAuthorisationByCardId(final String managedCardId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.authorisation a where a.card_id = '%s';",
                        DATABASE_NAME, managedCardId));
    }

    public static Map<Integer, Map<String, String>> getSettlementByCardId(final String managedCardId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.settlement s where s.card_id = '%s';",
                        DATABASE_NAME, managedCardId));
    }

    public static void updatePhysicalCardState(final String physicalCardState,
                                               final String managedCardId) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.managed_card_physical SET physical_state = '%s' WHERE card_id = %s;", DATABASE_NAME, physicalCardState, managedCardId));
    }

    public static Map<Integer, Map<String, String>> getPhysicalManagedCard(final String managedCardId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.managed_card_physical mcp WHERE mcp.card_id = '%s';",
                        DATABASE_NAME, managedCardId));
    }

    public static void setPrepaidManagedCardAvailableBalance(final int balance, final String managedCardId) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.managed_card_prepaid SET available = %s where id = %s;", DATABASE_NAME, balance, managedCardId));
    }

    public static Map<Integer, Map<String, String>> getLatestOct(final String managedCardId, final long timestamp) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.settlement s where s.card_id = '%s' AND classification = 'OCT_NORMAL' AND transaction_timestamp >= '%s' ORDER BY transaction_timestamp DESC;",
                        DATABASE_NAME, managedCardId, timestamp));
    }

    public static Map<Integer, Map<String, String>> getSettlement(final String settlementId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.settlement s where s.id = '%s';",
                        DATABASE_NAME, settlementId));
    }
}
