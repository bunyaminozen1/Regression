package opc.junit.helpers.multi;

import commons.enums.Currency;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import opc.enums.opc.AcceptedResponse;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.testmodels.BalanceModel;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class ManagedAccountsHelper {

    public static String createManagedAccount(final CreateManagedAccountModel createManagedAccountModel,
                                              final String secretKey,
                                              final String authenticationToken) {

        final String managedAccountId = TestHelper.ensureAsExpected(60,
                        () -> ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken, Optional.empty()),
                        SC_OK)
                .jsonPath()
                .get("id");

        TestHelper.ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state.state").equals("ACTIVE"),
                Optional.of(String.format("Expecting 200 with managed account %s in ACTIVE state, check logged payload", managedAccountId)));

        return managedAccountId;
    }

    public static String createManagedAccount(final String profileId,
                                              final String currency,
                                              final String secretKey,
                                              final String authenticationToken) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(profileId, currency).build();

        return createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);
    }

    public static String createPendingApprovalManagedAccount(final String profileId,
                                                             final String secretKey,
                                                             final String authenticationToken) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(profileId, Currency.GBP).build();

        return createPendingApprovalManagedAccount(createManagedAccountModel, secretKey, authenticationToken);
    }

    public static String createPendingApprovalManagedAccount(final CreateManagedAccountModel createManagedAccountModel,
                                                             final String secretKey,
                                                             final String authenticationToken) {
        return
                TestHelper.ensureAsExpected(15,
                                () -> ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");
    }

    public static void blockManagedAccount(final String managedAccountId,
                                           final String secretKey,
                                           final String authenticationToken) {
        TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.blockManagedAccount(secretKey, managedAccountId, authenticationToken),
                SC_NO_CONTENT);
    }

    public static void unblockManagedAccount(final String managedAccountId,
                                             final String secretKey,
                                             final String authenticationToken) {
        TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.unblockManagedAccount(secretKey, managedAccountId, authenticationToken),
                SC_NO_CONTENT);
    }

    public static void removeManagedAccount(final String managedAccountId,
                                            final String secretKey,
                                            final String authenticationToken) {
        TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.removeManagedAccount(secretKey, managedAccountId, authenticationToken),
                SC_NO_CONTENT);
    }

    public static Response assignManagedAccountIban(final String managedAccountId,
                                                    final String secretKey,
                                                    final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.assignManagedAccountIban(secretKey, managedAccountId, authenticationToken),
                SC_OK);
    }

    public static void ensureIbanState(final String secretKey,
                                       final String managedAccountId,
                                       final String token,
                                       final String state) {
        TestHelper.ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccountIban(secretKey, managedAccountId, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state),
                Optional.of(String.format("Expecting 200 with a IBAN in state %s for Managed Account %s, check logged payload", state, managedAccountId)));
    }

    public static BalanceModel getManagedAccountBalance(final String managedAccountId,
                                                        final String secretKey,
                                                        final String authenticationToken,
                                                        final int expectedManagedAccountBalance) {

        final Response response =
                TestHelper.ensureAsExpected(60,
                        () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken),
                        x -> x.statusCode() == SC_OK && x.jsonPath().get("balances.availableBalance")
                                .equals(expectedManagedAccountBalance),
                        Optional.of(String.format("Expecting 200 with a balance of %s, check logged payload",
                                expectedManagedAccountBalance)));

        return new BalanceModel(response.jsonPath().get("balances.availableBalance"),
                response.jsonPath().get("balances.actualBalance"));
    }

    public static BalanceModel getManagedAccountBalance(final String managedAccountId,
                                                        final String secretKey,
                                                        final String authenticationToken,
                                                        final int expectedAvailableBalance,
                                                        final int expectedActualBalance) {

        final Response response =
                TestHelper.ensureAsExpected(60,
                        () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken),
                        x -> x.statusCode() == SC_OK && x.jsonPath().get("balances.availableBalance")
                                .equals(expectedAvailableBalance)
                                && x.jsonPath().get("balances.actualBalance")
                                .equals(expectedActualBalance),
                        Optional.of(String.format("Expecting 200 with available balance of %s and actual balance of %s, check logged payload",
                                expectedAvailableBalance, expectedActualBalance)));

        return new BalanceModel(response.jsonPath().get("balances.availableBalance"),
                response.jsonPath().get("balances.actualBalance"));
    }

    public static BalanceModel getManagedAccountBalance(final String managedAccountId,
                                                        final String secretKey,
                                                        final String authenticationToken) {
        final JsonPath response =
                TestHelper.ensureAsExpected(15,
                                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken),
                                SC_OK)
                        .jsonPath();

        return new BalanceModel(response.get("balances.availableBalance"), response.get("balances.actualBalance"));
    }

    public static int getManagedAccountAvailableBalance(final String managedAccountId,
                                                        final String secretKey,
                                                        final String authenticationToken) {

        return TestHelper.ensureAsExpected(15,
                        () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken),
                        SC_OK)
                .jsonPath().get("balances.availableBalance");
    }

    public static Response getManagedAccountStatement(final String managedAccountId,
                                                      final String secretKey,
                                                      final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.getManagedAccountStatement(secretKey, managedAccountId,
                        authenticationToken, Optional.empty(), AcceptedResponse.JSON),
                SC_OK);
    }

    public static void getManagedAccountStatementForbidden(final String managedAccountId,
                                                           final String secretKey,
                                                           final String authenticationToken) {
        TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.getManagedAccountStatement(secretKey, managedAccountId,
                        authenticationToken, Optional.empty(), AcceptedResponse.JSON),
                SC_FORBIDDEN);
    }

    public static Response getManagedAccountStatement(final String managedAccountId,
                                                      final String secretKey,
                                                      final String authenticationToken,
                                                      final int expectedStatementCount) {

        return TestHelper.ensureAsExpected(30,
                () -> ManagedAccountsService.getManagedAccountStatement(secretKey, managedAccountId,
                        authenticationToken, Optional.empty(), AcceptedResponse.JSON),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("count")
                        .equals(expectedStatementCount),
                Optional.of(String.format("Expecting 200 with a statement count of %s, check logged payload", expectedStatementCount)));
    }

    public static Response getManagedAccounts(final String secretKey,
                                              final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), authenticationToken),
                SC_OK);
    }

    public static Response getManagedAccount(final String secretKey,
                                             final String managedAccountId,
                                             final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken),
                SC_OK);
    }

    public static Map<String, CreateManagedAccountModel> createManagedAccounts(final String managedAccountProfileId,
                                                                               final String currency,
                                                                               final String authenticationToken,
                                                                               final String secretKey,
                                                                               final int noOfAccounts) {
        final Map<String, CreateManagedAccountModel> accounts = new TreeMap<>(Collections.reverseOrder());

        IntStream.range(0, noOfAccounts).forEach(x -> {
            final CreateManagedAccountModel createManagedAccountModel =
                    CreateManagedAccountModel.DefaultCreateManagedAccountModel(managedAccountProfileId, currency).build();

            accounts.put(createManagedAccount(createManagedAccountModel, secretKey, authenticationToken), createManagedAccountModel);
        });

        return accounts;
    }

    public static void createUpgradedManagedAccount(final CreateManagedAccountModel createManagedAccountModel,
                                                    final String secretKey,
                                                    final String authenticationToken) {

        final String managedAccountId = createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);
        assignManagedAccountIban(managedAccountId, secretKey, authenticationToken);
    }

    public static String createUpgradedManagedAccount(final String profileId,
                                                      final String currency,
                                                      final String secretKey,
                                                      final String authenticationToken) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(profileId, currency).build();

        final String managedAccountId = createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);
        assignManagedAccountIban(managedAccountId, secretKey, authenticationToken);

        return managedAccountId;
    }

    public static Pair<String, Pair<Integer, Integer>> createFundedManagedAccount(final CreateManagedAccountModel createManagedAccountModel,
                                                                                  final String secretKey,
                                                                                  final String authenticationToken) {
        return createFundedManagedAccount(createManagedAccountModel, secretKey, authenticationToken, 10000L);
    }

    public static Pair<String, Pair<Integer, Integer>> createFundedManagedAccount(final CreateManagedAccountModel createManagedAccountModel,
                                                                                  final String secretKey,
                                                                                  final String authenticationToken,
                                                                                  final long depositAmount) {
        final String managedAccountId = createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);
        final Pair<Integer, Integer> balance = TestHelper.simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                depositAmount, secretKey, authenticationToken);

        return Pair.of(managedAccountId, balance);
    }

    public static Pair<String, Pair<Integer, Integer>> createFundedManagedAccount(final String profileId,
                                                                                  final String currency,
                                                                                  final String secretKey,
                                                                                  final String authenticationToken) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(profileId, currency).build();

        return createFundedManagedAccount(createManagedAccountModel, secretKey, authenticationToken);
    }

    public static Pair<String, Pair<Integer, Integer>> createFundedManagedAccount(final String profileId,
                                                                                  final String currency,
                                                                                  final String secretKey,
                                                                                  final String authenticationToken,
                                                                                  final long depositAmount) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(profileId, currency).build();

        return createFundedManagedAccount(createManagedAccountModel, secretKey, authenticationToken, depositAmount);
    }

    public static String getProcessorAccountProduct(Currency currency) {
        Map<Currency, String> processorCardProducts = new HashMap<>();
        // Current GPS account product combination
        processorCardProducts.put(Currency.USD, "5167");
        processorCardProducts.put(Currency.EUR, "5168");
        processorCardProducts.put(Currency.GBP, "5169");

        if (!processorCardProducts.containsKey(currency))
            throw new IllegalArgumentException(String.format("Unsupported currency type %s", currency));

        return processorCardProducts.get(currency);
    }

    public static String createManagedAccountWithAdjustment(final String profileId,
                                                            final String currency,
                                                            final String secretKey,
                                                            final String authenticationToken,
                                                            final String innovatorId) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(profileId, currency).build();

        final String managedAccountId = createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, 10000L);

        return managedAccountId;
    }
}
