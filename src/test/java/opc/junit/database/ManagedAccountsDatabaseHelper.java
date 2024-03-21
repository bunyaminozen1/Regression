package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class ManagedAccountsDatabaseHelper {

    private final static String DATABASE_NAME = "managed_accounts";

    public static Map<Integer, Map<String, String>> getManagedAccount(final String managedAccountId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.managed_account ma where ma.id = '%s';",
                        DATABASE_NAME, managedAccountId));
    }

    public static Map<Integer, Map<String, String>> getManagedAccountBankDetails(final String managedAccountId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.managed_account_bank_details mabd where mabd.account_id = '%s';",
                        DATABASE_NAME, managedAccountId));
    }

    public static Map<Integer, Map<String, String>> getDepositByManagedAccount(final String managedAccountId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.deposit ma where ma.account_id = '%s';",
                        DATABASE_NAME, managedAccountId));
    }

    public static Map<Integer, Map<String, String>> getDepositByManagedAccountAndState(final String managedAccountId, final String state) throws SQLException {
        return DatabaseHelper
            .retrieveData(String.format("select * from %s.deposit ma where ma.account_id = '%s' and deposit_state = '%s';",
                DATABASE_NAME, managedAccountId, state));
    }

    public static Map<Integer, Map<String, String>> getIbanByManagedAccount(final String managedAccountId) throws SQLException {
        return DatabaseHelper
            .retrieveData(String.format("select * from %s.managed_account_iban mai where mai.managed_account_id = '%s';",
                DATABASE_NAME, managedAccountId));
    }
}
