package opc.junit.multi.transactionlimits;

import io.restassured.path.json.JsonPath;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.KycLevel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.admin.CurrencyMinMaxModel;
import opc.models.admin.FinancialLimitValueModel;
import opc.models.admin.LimitContextModel;
import opc.models.admin.LimitValueModel;
import opc.models.admin.MaxAmountModel;
import opc.models.admin.SetGlobalLimitModel;
import opc.models.admin.WindowWidthModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulateDepositModel;
import opc.models.testmodels.BalanceModel;
import opc.services.multi.ManagedAccountsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositTransactionTests extends BaseTransactionLimitsSetup {

    private final static String IDENTITY_CURRENCY = Currency.EUR.name();

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void setup(){

        final SetGlobalLimitModel setLimitModel = SetGlobalLimitModel.builder()
                .setContext(new LimitContextModel())
                .setWindowWidth(new WindowWidthModel(1, "DAY"))
                .setLimitValue(new LimitValueModel(new MaxAmountModel("1500000")))
                .setWideWindowMultiple("365")
                .setFinancialLimitValue(FinancialLimitValueModel.builder()
                        .setRefCurrency("EUR")
                        .setCurrencyMinMax(CurrencyMinMaxModel.builder()
                                .setGbp(new LimitValueModel(new MaxAmountModel("1350000")))
                                .setUsd(new LimitValueModel(new MaxAmountModel("1750000")))
                                .build())
                        .build())
                .build();

        AdminHelper.setGlobalCorporateVelocityLimit(setLimitModel, adminToken, "CORPORATE_VELOCITY_AGGREGATE");
        AdminHelper.setGlobalConsumerVelocityLimit(setLimitModel, adminToken, "CONSUMER_VELOCITY_AGGREGATE");

        corporateSetup();
        consumerSetup();

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, corporateId);

        AdminHelper.setConsumerVelocityLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, consumerId);
    }

    @Test
    public void Deposit_LimitExceeded_DepositPending(){

        final long depositAmount = 4000L;
        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporateAuthenticationToken);

        simulatePendingDeposit(managedAccountId, corporateAuthenticationToken, depositAmount, expectedActualBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, expectedActualBalance, 0);
    }

    @Test
    public void Deposit_LimitExceededAfterSuccessfulDeposit_DepositPending(){

        final long depositAmount = 2000L;
        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final int expectedAvailableBalance = (int) (depositAmount - depositFeeAmount);
        final int expectedActualBalanceAdjustment = (int) ( depositAmount - depositFeeAmount);
        final int expectedActualBalance = (int) ( expectedAvailableBalance + depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedAvailableBalance);
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedActualBalance);

        final JsonPath deposits =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalanceAdjustment))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].actualBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].availableBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].balanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[1].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, expectedActualBalance, expectedAvailableBalance);
    }

    @Test
    public void Deposit_DepositFeeNotAffectingLimit_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 3000L + depositFeeAmount;
        final long secondDepositAmount = 100L + depositFeeAmount;
        final int expectedAvailableBalance = (int) (depositAmount - depositFeeAmount);
        final int expectedActualBalanceAdjustment = (int) ( secondDepositAmount - depositFeeAmount);
        final int expectedActualBalance = (int) ( expectedAvailableBalance + secondDepositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedAvailableBalance);
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, secondDepositAmount, expectedActualBalance);

        final JsonPath deposits =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalanceAdjustment))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)secondDepositAmount))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].actualBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].availableBalanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].balanceAfter.amount", equalTo(expectedAvailableBalance))
                .body("entry[1].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[1].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, expectedActualBalance, expectedAvailableBalance);
    }

    @Test
    public void Deposit_JustAboveLimit_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 3001L + depositFeeAmount;
        final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedActualBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, expectedActualBalance, 0);
    }

    @Test
    public void Deposit_ExactLimit_Success(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 3000L + depositFeeAmount;
        final int expectedBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, expectedBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, expectedBalance, expectedBalance);
    }

    @Test
    public void Deposit_ReachLimitAgainAfterPendingDepositResumed_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ( depositAmount - depositFeeAmount));
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 2));

        final JsonPath depositsToResume =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();
        AdminHelper.resumeDeposit(adminTenantImpersonationToken, depositsToResume.get("entry[0].id"));

        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 3));

        final JsonPath deposits =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 3)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[2].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[2].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[3].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[3].transactionId.id",  equalTo(deposits.get("entry[2].id")))
                .body("entry[3].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken,
                (int) ((depositAmount - depositFeeAmount) * 3), (int) ((depositAmount - depositFeeAmount) * 2));
    }

    @Test
    public void Deposit_ReachLimitAgainAfterPendingDepositsResumed_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 4000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ( depositAmount - depositFeeAmount));
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 2));

        final JsonPath depositsToResume =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();
        AdminHelper.resumeDeposits(adminTenantImpersonationToken,
                Arrays.asList(depositsToResume.get("entry[0].id"), depositsToResume.get("entry[1].id")));

        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 3));

        final JsonPath deposits =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 3)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].cardholderFee.amount",  equalTo(0))
                .body("entry[2].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[2].transactionId.id",  equalTo(deposits.get("entry[2].id")))
                .body("entry[2].entryState",  equalTo("COMPLETED"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].availableBalanceAfter.amount", equalTo(0))
                .body("entry[3].balanceAfter.amount", equalTo(0))
                .body("entry[3].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[3].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[3].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[3].entryState",  equalTo("PENDING"))
                .body("entry[4].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[4].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[4].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[4].availableBalanceAfter.amount", equalTo(0))
                .body("entry[4].balanceAfter.amount", equalTo(0))
                .body("entry[4].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[4].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[4].transactionId.id",  equalTo(deposits.get("entry[2].id")))
                .body("entry[4].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken,
                (int) ((depositAmount - depositFeeAmount) * 3), (int) ((depositAmount - depositFeeAmount) * 2));
    }

    @Test
    public void Deposit_ReachLimitAgainAfterLimitReset_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ( depositAmount - depositFeeAmount));
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 2));

        AdminHelper.resetConsumerLimit(adminTenantImpersonationToken, consumerId);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 2));
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 4));

        final JsonPath deposits =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 4)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 3)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[1].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[2].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[2].transactionId.id",  equalTo(deposits.get("entry[2].id")))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[3].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[3].transactionId.id",  equalTo(deposits.get("entry[3].id")))
                .body("entry[3].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken,
                (int) ((depositAmount - depositFeeAmount) * 4), (int) ((depositAmount - depositFeeAmount) * 2));
    }

    @Test
    public void Deposit_ReachLimitAcrossMultipleAccounts_DepositPending(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 2000L;

        final String managedAccountId1 =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId1, secretKey, consumerAuthenticationToken);

        final String managedAccountId2 =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId2, secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId1, consumerAuthenticationToken, depositAmount, (int) ( depositAmount - depositFeeAmount));
        simulatePendingDeposit(managedAccountId2, consumerAuthenticationToken, depositAmount, (int) ( depositAmount - depositFeeAmount));

        final String deposit1Id =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId1).jsonPath().get("entry[0].id");

        final String deposit2Id =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId2).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId1, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposit1Id))
                .body("entry[0].entryState",  equalTo("COMPLETED"));

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId2, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposit2Id))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId1, consumerAuthenticationToken,
                (int) (depositAmount - depositFeeAmount), (int) (depositAmount - depositFeeAmount));

        assertManagedAccountBalance(managedAccountId2, consumerAuthenticationToken,
                (int) (depositAmount - depositFeeAmount), 0);
    }

    @Test
    public void Deposit_ReachLimitThenSendSmallerDeposit_DepositPending(){

        final long depositAmount = 4000L;
        final long smallerDepositAmount = 500L;
        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporateAuthenticationToken);

        simulatePendingDeposit(managedAccountId, corporateAuthenticationToken, depositAmount, (int) (depositAmount - depositFeeAmount));
        simulatePendingDeposit(managedAccountId, corporateAuthenticationToken, smallerDepositAmount, (int) (depositAmount + smallerDepositAmount - depositFeeAmount - depositFeeAmount));

        final JsonPath deposits =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo((int) (smallerDepositAmount - depositFeeAmount)))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) (depositAmount + smallerDepositAmount - depositFeeAmount - depositFeeAmount)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (smallerDepositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[0].entryState",  equalTo("PENDING"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo(0))
                .body("entry[1].balanceAfter.amount", equalTo(0))
                .body("entry[1].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[1].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[1].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken,
                (int) (depositAmount + smallerDepositAmount - depositFeeAmount - depositFeeAmount), 0);
    }

    @Test
    public void Deposit_GlobalExactLimit_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
                        .setBaseCurrency(IDENTITY_CURRENCY)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 1500000L + depositFeeAmount;
        final int expectedBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        simulateSuccessfulDeposit(managedAccountId, corporate.getRight(), depositAmount, expectedBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, corporate.getRight(), expectedBalance, expectedBalance);
    }

    @Test
    public void Deposit_GlobalJustAboveLimit_TransactionPending(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
                        .setBaseCurrency(IDENTITY_CURRENCY)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 1500001L + depositFeeAmount;
        final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        simulatePendingDeposit(managedAccountId, corporate.getRight(), depositAmount, expectedActualBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, corporate.getRight(), expectedActualBalance, 0);
    }

    @Test
    public void Deposit_FundsSourceExactLimit_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
                        .setBaseCurrency(IDENTITY_CURRENCY)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        AdminHelper.setCorporateFundsSourceLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, corporate.getLeft());

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 3000L + depositFeeAmount;
        final int expectedBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        simulateSuccessfulDeposit(managedAccountId, corporate.getRight(), depositAmount, expectedBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, corporate.getRight(), expectedBalance, expectedBalance);
    }

    @Test
    public void Deposit_FundsSourceJustAboveLimit_TransactionPending(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
                        .setBaseCurrency(IDENTITY_CURRENCY)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        AdminHelper.setCorporateFundsSourceLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, corporate.getLeft());

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 3001L + depositFeeAmount;
        final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        simulatePendingDeposit(managedAccountId, corporate.getRight(), depositAmount, expectedActualBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, corporate.getRight(), expectedActualBalance, 0);
    }

    @Test
    public void Deposit_FundsSourceJustAboveLimit_ResumeDeposit_TransactionCompleted(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationOne.getCorporatesProfileId())
                        .setBaseCurrency(IDENTITY_CURRENCY)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        AdminHelper.setCorporateFundsSourceLimit(new CurrencyAmount(IDENTITY_CURRENCY, 3000L),
                Arrays.asList(new CurrencyAmount(Currency.GBP.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                adminTenantImpersonationToken, corporate.getLeft());

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 3001L + depositFeeAmount;
        final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        simulatePendingDeposit(managedAccountId, corporate.getRight(), depositAmount, expectedActualBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, corporate.getRight(), expectedActualBalance, 0);

        AdminHelper.resumeDeposit(adminToken, depositId);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("COMPLETED"));
    }

    @Test
    public void Deposit_ResumeDepositTransactionAlreadyResumed_Success(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 2000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        simulateSuccessfulDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ( depositAmount - depositFeeAmount));
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 2));

        final JsonPath deposits =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();
        AdminHelper.resumeDeposit(adminTenantImpersonationToken, deposits.get("entry[0].id"));
        AdminHelper.resumeDeposit(adminTenantImpersonationToken, deposits.get("entry[0].id"));

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[1].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[1].entryState",  equalTo("PENDING"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[2].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[2].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[2].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken,
                (int) ((depositAmount - depositFeeAmount) * 2), (int) ((depositAmount - depositFeeAmount) * 2));
    }

    @Test
    public void Deposit_ResumeDepositsOneTransactionAlreadyResumed_Success(){

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 4000L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
                        secretKey, consumerAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumerAuthenticationToken);

        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ( depositAmount - depositFeeAmount));
        simulatePendingDeposit(managedAccountId, consumerAuthenticationToken, depositAmount, (int) ((depositAmount - depositFeeAmount) * 2));

        final JsonPath deposits =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath();

        AdminHelper.resumeDeposit(adminTenantImpersonationToken, deposits.get("entry[0].id"));

        AdminHelper.resumeDeposits(adminTenantImpersonationToken,
                Arrays.asList(deposits.get("entry[0].id"), deposits.get("entry[1].id")));

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].balanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[0].cardholderFee.amount",  equalTo(0))
                .body("entry[0].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[0].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[0].entryState",  equalTo("COMPLETED"))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].balanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].cardholderFee.amount",  equalTo(0))
                .body("entry[1].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[1].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[1].entryState",  equalTo("COMPLETED"))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[2].actualBalanceAfter.amount", equalTo((int) ((depositAmount - depositFeeAmount) * 2)))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[2].availableBalanceAfter.amount", equalTo(0))
                .body("entry[2].balanceAfter.amount", equalTo(0))
                .body("entry[2].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[2].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[2].transactionId.id",  equalTo(deposits.get("entry[0].id")))
                .body("entry[2].entryState",  equalTo("PENDING"))
                .body("entry[3].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].actualBalanceAfter.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[3].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[3].availableBalanceAfter.amount", equalTo(0))
                .body("entry[3].balanceAfter.amount", equalTo(0))
                .body("entry[3].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[3].transactionAmount.amount",  equalTo((int) (depositAmount)))
                .body("entry[3].transactionId.id",  equalTo(deposits.get("entry[1].id")))
                .body("entry[3].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken,
                (int) ((depositAmount - depositFeeAmount) * 2), (int) ((depositAmount - depositFeeAmount) * 2));
    }

    @Test
    public void Deposit_KycLevel1LimitExceeded_DepositPending(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 15001 + depositFeeAmount;
        final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), createConsumerModel.getBaseCurrency(),
                        secretKey, consumer.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumer.getRight());

        simulatePendingDeposit(managedAccountId, consumer.getRight(), depositAmount, expectedActualBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedActualBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedActualBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].availableBalanceAfter.amount", equalTo(0))
                .body("entry[0].balanceAfter.amount", equalTo(0))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("PENDING"));

        assertManagedAccountBalance(managedAccountId, consumer.getRight(), expectedActualBalance, 0);
    }

    @Test
    public void Deposit_KycLevel1WithinLimit_Success(){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
        final long depositAmount = 15000 + depositFeeAmount;
        final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);
        final int expectedBalance = (int) (depositAmount - depositFeeAmount);

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId(), createConsumerModel.getBaseCurrency(),
                        secretKey, consumer.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumer.getRight());

        simulatePendingDeposit(managedAccountId, consumer.getRight(), depositAmount, expectedActualBalance);

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].actualBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(expectedBalance))
                .body("entry[0].availableBalanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].balanceAfter.amount", equalTo(expectedBalance))
                .body("entry[0].cardholderFee.amount",  equalTo(depositFeeAmount))
                .body("entry[0].transactionAmount.amount",  equalTo((int)depositAmount))
                .body("entry[0].transactionId.id",  equalTo(depositId))
                .body("entry[0].entryState",  equalTo("COMPLETED"));

        assertManagedAccountBalance(managedAccountId, consumer.getRight(), expectedBalance, expectedBalance);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(IDENTITY_CURRENCY).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(IDENTITY_CURRENCY).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }

    private void simulatePendingDeposit(final String managedAccountId,
                                        final String token,
                                        final Long depositAmount,
                                        final int expectedActualBalance){

        final SimulateDepositModel simulateDepositModel
                = SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(IDENTITY_CURRENCY, depositAmount));
        SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        TestHelper.ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
                x-> x.statusCode() == SC_OK &&
                        x.jsonPath().get("balances.actualBalance").equals(expectedActualBalance),
                Optional.of(String.format("Expecting 200 with a balance of %s, check logged payload", expectedActualBalance)));
    }

    private void simulateSuccessfulDeposit(final String managedAccountId,
                                           final String token,
                                           final Long depositAmount,
                                           final int expectedAvailableBalance){

        final SimulateDepositModel simulateDepositModel
                = SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(IDENTITY_CURRENCY, depositAmount));
        SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        TestHelper.ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
                x-> x.statusCode() == SC_OK &&
                        x.jsonPath().get("balances.availableBalance").equals(expectedAvailableBalance),
                Optional.of(String.format("Expecting 200 with a balance of %s, check logged payload", expectedAvailableBalance)));
    }

    private void assertManagedAccountBalance(final String managedAccountId,
                                             final String authenticationToken,
                                             final int expectedActualBalance,
                                             final int expectedAvailableBalance){

        final BalanceModel actualBalance = ManagedAccountsHelper.getManagedAccountBalance(managedAccountId, secretKey, authenticationToken);
        assertEquals(expectedActualBalance, actualBalance.getActualBalance());
        assertEquals(expectedAvailableBalance, actualBalance.getAvailableBalance());
    }

    @AfterEach
    public void resetLimit(){
        AdminHelper.resetCorporateLimit(adminTenantImpersonationToken, corporateId);
        AdminHelper.resetConsumerLimit(adminTenantImpersonationToken, consumerId);
    }
}
