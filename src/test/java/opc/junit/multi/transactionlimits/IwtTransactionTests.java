package opc.junit.multi.transactionlimits;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import commons.enums.Currency;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import opc.enums.opc.FeeType;
import opc.helpers.ModelHelper;
import opc.junit.database.ManagedAccountsDatabaseHelper;
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
import opc.models.simulator.SimulatePendingDepositModel;
import opc.models.testmodels.BalanceModel;
import opc.services.multi.ManagedAccountsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class IwtTransactionTests extends BaseTransactionLimitsSetup {

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

  @AfterEach
  public void resetLimit(){
    AdminHelper.resetCorporateLimit(adminTenantImpersonationToken, corporateId);
    AdminHelper.resetConsumerLimit(adminTenantImpersonationToken, consumerId);
  }

  @Test
  @Disabled("We currently don't support standard limits for IWT, the monitoring happens through Hawk, in case we want to support this, uncomment and adjust this test")
  public void Deposit_LimitExceeded_DepositPending() throws SQLException {

    final long depositAmount = 4000L;
    final int depositFeeAmount = TestHelper.getFees(IDENTITY_CURRENCY).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
    final int expectedActualBalance = (int) (depositAmount - depositFeeAmount);

    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId(), IDENTITY_CURRENCY,
            secretKey, corporateAuthenticationToken);

    ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporateAuthenticationToken);

    // final String iban = getManagedAccountIbanId(managedAccountId);

    // simulatePendingDeposit(iban, managedAccountId, corporateAuthenticationToken, depositAmount, expectedActualBalance);

    testDepost(managedAccountId, corporateAuthenticationToken, depositAmount, expectedActualBalance);

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

  private void simulatePendingDeposit(final String iban,
      final String managedAccountId,
      final String token,
      final Long depositAmount,
      final int expectedActualBalance){

    final SimulatePendingDepositModel simulatePendingDepositModel =
        SimulatePendingDepositModel.builder()
            .senderName("test")
            .senderIban(ModelHelper.generateRandomValidIban())
            .depositAmount(new CurrencyAmount(IDENTITY_CURRENCY, depositAmount))
            .webhook(true)
            .immediateMonitorReplyExpected(false)
            .reference("reference")
            .build();

    SimulatorService.simulateManagedAccountPendingDepositByIban(simulatePendingDepositModel, innovatorId, iban);

    //TestHelper.ensureAsExpected(120,
        //() -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
        //x-> x.statusCode() == SC_OK &&
            //x.jsonPath().get("balances.actualBalance").equals(expectedActualBalance),
        //Optional.of(String.format("Expecting 200 with a balance of %s, check logged payload", expectedActualBalance)));
  }

  private void testDepost(final String managedAccountId,
      final String token,
      final Long depositAmount,
      final int expectedActualBalance){

    final SimulatePendingDepositModel simulatePendingDepositModel =
        SimulatePendingDepositModel.builder()
            .senderName("test")
            .senderIban(ModelHelper.generateRandomValidIban())
            .depositAmount(new CurrencyAmount(IDENTITY_CURRENCY, depositAmount))
            .webhook(true)
            .immediateMonitorReplyExpected(false)
            .reference("reference")
            .build();

    SimulatorService.simulateManagedAccountPendingDeposit(simulatePendingDepositModel, innovatorId, managedAccountId);

    //TestHelper.ensureAsExpected(120,
    //() -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
    //x-> x.statusCode() == SC_OK &&
    //x.jsonPath().get("balances.actualBalance").equals(expectedActualBalance),
    //Optional.of(String.format("Expecting 200 with a balance of %s, check logged payload", expectedActualBalance)));
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

  private String getManagedAccountIbanId(final String managedAccountId) throws SQLException {
    return  ManagedAccountsDatabaseHelper.getIbanByManagedAccount(managedAccountId).get(0).get("iban_id");
  }
}


