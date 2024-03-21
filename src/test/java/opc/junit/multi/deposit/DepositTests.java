package opc.junit.multi.deposit;

import commons.enums.Currency;
import commons.enums.State;
import opc.enums.opc.DepositType;
import opc.enums.opc.FeeType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulateDepositModel;
import opc.services.simulator.SimulatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class DepositTests extends BaseDepositTests {

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Deposit_Consumer_Success(final Currency currency)  {

    final long depositAmount = 10000L;
    final int depositFee = TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
    final int expectedBalance = (int) depositAmount - depositFee;

    final String managedAccountId =
            ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId,
                    currency.name(), secretKey, consumerAuthenticationToken);

    ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

    TestHelper.simulateManagedAccountDeposit(managedAccountId, currency.name(), depositAmount, secretKey, consumerAuthenticationToken);

    assertManagedAccountBalanceAndState(managedAccountId, consumerAuthenticationToken, expectedBalance, State.ACTIVE);
    assertManagedAccountStatement(managedAccountId, consumerAuthenticationToken, currency.name(), (int) depositAmount, depositFee, false);

    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Deposit_Corporate_Success(final Currency currency)  {

        final long depositAmount = 10000L;
        final int depositFee = TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedBalance = (int) depositAmount - depositFee;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency.name(), depositAmount, secretKey, corporateAuthenticationToken);

        assertManagedAccountBalanceAndState(managedAccountId, corporateAuthenticationToken, expectedBalance, State.ACTIVE);
        assertManagedAccountStatement(managedAccountId, corporateAuthenticationToken, currency.name(), (int) depositAmount, depositFee, false);

    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Deposit_ConsumerBlockedAccount_Success(final Currency currency)  {

        final long depositAmount = 10000L;
        final int depositFee = TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedBalance = (int) depositAmount - depositFee;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId,
                        currency.name(), secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.blockManagedAccount(managedAccountId, secretKey, consumerAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency.name(), depositAmount, secretKey, consumerAuthenticationToken);

        assertManagedAccountBalanceAndState(managedAccountId, consumerAuthenticationToken, expectedBalance, State.BLOCKED);
        assertManagedAccountStatement(managedAccountId, consumerAuthenticationToken, currency.name(), (int) depositAmount, depositFee, false);

    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Deposit_CorporateBlockedAccount_Success(final Currency currency)  {

        final long depositAmount = 10000L;
        final int depositFee = TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedBalance = (int) depositAmount - depositFee;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.blockManagedAccount(managedAccountId, secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency.name(), depositAmount, secretKey, corporateAuthenticationToken);

        assertManagedAccountBalanceAndState(managedAccountId, corporateAuthenticationToken, expectedBalance, State.BLOCKED);
        assertManagedAccountStatement(managedAccountId, corporateAuthenticationToken, currency.name(), (int) depositAmount, depositFee, false);
    }

    @Disabled("The test for normal deposit flows to destroyed account are currently not supported")
    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Deposit_ConsumerDestroyedAccount_Success(final Currency currency)  {

        final long depositAmount = 10000L;
        final int depositFee = TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedBalance = (int) depositAmount - depositFee;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId,
                        currency.name(), secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.removeManagedAccount(managedAccountId, secretKey, consumerAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency.name(), depositAmount, secretKey, consumerAuthenticationToken);

        assertManagedAccountBalanceAndState(managedAccountId, consumerAuthenticationToken, expectedBalance, State.DESTROYED);
        assertManagedAccountStatement(managedAccountId, consumerAuthenticationToken, currency.name(), (int) depositAmount, depositFee, false);
    }

    @Disabled("The test for normal deposit flows to destroyed account are currently not supported")
    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Deposit_CorporateDestroyedAccount_Success(final Currency currency) {

        final long depositAmount = 10001L;
        final int depositFee = TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedBalance = (int) depositAmount - depositFee;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.removeManagedAccount(managedAccountId, secretKey, corporateAuthenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency.name(), depositAmount, secretKey, corporateAuthenticationToken);

        assertManagedAccountBalanceAndState(managedAccountId, corporateAuthenticationToken, expectedBalance, State.DESTROYED);
        assertManagedAccountStatement(managedAccountId, corporateAuthenticationToken, currency.name(), (int) depositAmount, depositFee, false);
    }

    @Test
    public void Deposit_SepaCorporate_Success()  {

        final long depositAmount = 10000L;
        final Currency currency = Currency.EUR;
        final int depositFee = TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedBalance = (int) depositAmount - depositFee;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporateAuthenticationToken);

        final SimulateDepositModel simulateDepositModel =
                SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(currency.name(),
                        depositAmount), DepositType.SEPA);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, simulateDepositModel, secretKey, corporateAuthenticationToken);

        assertManagedAccountBalanceAndState(managedAccountId, corporateAuthenticationToken, expectedBalance, State.ACTIVE);
        assertManagedAccountStatement(managedAccountId, corporateAuthenticationToken, simulateDepositModel, depositFee);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Deposit_IbanNotUpgraded_Denied(final Currency currency) {

        final long depositAmount = 10000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId,
                        currency.name(), secretKey, consumerAuthenticationToken);

        final SimulateDepositModel simulateDepositModel
                = SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(currency, depositAmount));

        SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId)
                .then()
                .statusCode(SC_OK)
                .body("code", equalTo("DENIED_ACCOUNT_NOT_UPGRADED_TO_IBAN"));
    }
}



