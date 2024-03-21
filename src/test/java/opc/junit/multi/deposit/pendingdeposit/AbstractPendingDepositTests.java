package opc.junit.multi.deposit.pendingdeposit;

import commons.enums.Currency;
import commons.enums.State;
import java.sql.SQLException;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.helpers.LimitsModelHelper;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.adminnew.AdminHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.multi.deposit.BaseDepositTests;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulatePendingDepositModel;
import opc.services.adminnew.AdminService;
import opc.services.simulator.SimulatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;

public abstract class AbstractPendingDepositTests extends BaseDepositTests {
  protected abstract String getToken();
  protected abstract String getManagedAccountProfileId();
  protected abstract IdentityType getIdentityType();
  final static int corporateMaxSum = 3000000;
  final static int consumerMaxSum = 500000;

  @BeforeAll
  public static void SetupSepaInstantLimits() {

    String adminToken = AdminService.loginAdmin();
    AdminService.setSepaInstantLimit(
        LimitsModelHelper.defaultSepaInstantLimitModel(IdentityType.CORPORATE, corporateMaxSum),
        adminToken
    );

    AdminService.setSepaInstantLimit(
        LimitsModelHelper.defaultSepaInstantLimitModel(IdentityType.CONSUMER, consumerMaxSum),
        adminToken
    );

    AdminService.setSepaInstantIncomingWireTransferLimit(
        LimitsModelHelper.defaultSepaInstantLimitModel(IdentityType.CORPORATE, corporateMaxSum),
        adminToken
    );

    AdminService.setSepaInstantIncomingWireTransferLimit(
        LimitsModelHelper.defaultSepaInstantLimitModel(IdentityType.CONSUMER, consumerMaxSum),
        adminToken
    );
  }


  @ParameterizedTest
  @EnumSource(value = Currency.class)
  public void PendingDeposit_Approved_Success(final Currency currency) throws SQLException {

    final long depositAmount = 10000L;
    final int depositFee = TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
    final int expectedBalance = (int) depositAmount - depositFee;

    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(getManagedAccountProfileId(),
            currency.name(), secretKey, getToken());

    ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, getToken());

    final String iban = getManagedAccountIbanId(managedAccountId);

    TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
        ModelHelper.generateRandomValidIban(), currency.name(), depositAmount,true, false, "RefTest123", true, innovatorId,  secretKey, getToken());

    assertManagedAccountBalanceAndState(managedAccountId, getToken(), expectedBalance, State.ACTIVE);
    assertManagedAccountStatement(managedAccountId, getToken(), currency.name(), (int) depositAmount, depositFee, true);
  }

  @ParameterizedTest
  @EnumSource(value = Currency.class)
  public void PendingDeposit_BlockedAccount_Declined(final Currency currency) throws SQLException {

    final long depositAmount = 10000L;
    final int expectedBalance = 0;

    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(getManagedAccountProfileId(),
            currency.name(), secretKey, getToken());

    ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, getToken());

    ManagedAccountsHelper.blockManagedAccount(managedAccountId, secretKey, getToken());

    final String iban = getManagedAccountIbanId(managedAccountId);

    TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
        ModelHelper.generateRandomValidIban(), currency.name(), depositAmount,true, false, "RefTest123", false, innovatorId,  secretKey, getToken());

    assertManagedAccountBalanceAndState(managedAccountId, getToken(), expectedBalance, State.BLOCKED);

    final Map<String, String> deposit = getDepositByIbanId(iban).get(0);
    assertRejectedDepositState(deposit.get("id"), opc.services.admin.AdminService.loginAdmin());
  }

  @ParameterizedTest
  @EnumSource(value = Currency.class)
  public void PendingDeposit_DestroyedAccount_Declined(final Currency currency)
      throws SQLException {

    final long depositAmount = 10000L;
    final int expectedBalance = 0;

    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(getManagedAccountProfileId(),
            currency.name(), secretKey, getToken());

    ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, getToken());

    ManagedAccountsHelper.removeManagedAccount(managedAccountId, secretKey, getToken());

    final String iban = getManagedAccountIbanId(managedAccountId);

    TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
        ModelHelper.generateRandomValidIban(), currency.name(), depositAmount,true, false, "RefTest123", false, innovatorId,  secretKey, getToken());

    assertManagedAccountBalanceAndState(managedAccountId, getToken(), expectedBalance, State.DESTROYED);

    final Map<String, String> deposit = getDepositByIbanId(iban).get(0);
    assertRejectedDepositState(deposit.get("id"), opc.services.admin.AdminService.loginAdmin());
  }

  @ParameterizedTest
  @EnumSource(value = Currency.class)
  public void PendingDeposit_RejectDeposit_Rejected(final Currency currency) throws SQLException {

    final long depositAmount = 1000000000L;

    final String managedAccountId =
            ManagedAccountsHelper.createManagedAccount(getManagedAccountProfileId(),
                    currency.name(), secretKey, getToken());

    ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, getToken());

    final String iban = getManagedAccountIbanId(managedAccountId);

    TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
        ModelHelper.generateRandomValidIban(), currency.name(), depositAmount,true, false, "RefTest123", false, innovatorId,  secretKey, getToken());

    final Map<String, String> deposit = getDepositByIbanIdAndState(iban, "PRE_MONITORING_SUSPENDED").get(0);

    AdminHelper.rejectDeposit(AdminService.loginAdmin(), deposit.get("id"));
    assertRejectedDepositState(deposit.get("id"), opc.services.admin.AdminService.loginAdmin());
  }

  @Test
  public void PendingDeposit_SepaInstantApproved_Success() throws SQLException {

    final long depositAmount = 10000L;
    final int depositFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
    final int expectedBalance = (int) depositAmount - depositFee;

    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(getManagedAccountProfileId(),
            Currency.EUR.name(), secretKey, getToken());

    ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, getToken());

    final String iban = getManagedAccountIbanId(managedAccountId);

    TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
        ModelHelper.generateRandomValidIban(), Currency.EUR.name(), depositAmount,true, true, "RefTest123", true, innovatorId,  secretKey, getToken());

    assertManagedAccountBalanceAndState(managedAccountId, getToken(), expectedBalance, State.ACTIVE);
    assertManagedAccountStatement(managedAccountId, getToken(), Currency.EUR.name(), (int) depositAmount, depositFee, true);
  }

  @Test
  public void PendingDeposit_SepaInstantExceedingLimitGradually_Success() throws SQLException {
    final long depositAmount =
        getIdentityType() == IdentityType.CORPORATE ? (corporateMaxSum / 2 + 10000)  : (consumerMaxSum / 2 + 10000) ;
    final int depositFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
    final int expectedBalance = (int) depositAmount - depositFee;

    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(getManagedAccountProfileId(),
            Currency.EUR.name(), secretKey, getToken());

    ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, getToken());

    final String iban = getManagedAccountIbanId(managedAccountId);

    TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
        ModelHelper.generateRandomValidIban(), Currency.EUR.name(), depositAmount,true, true, "RefTest123", true, innovatorId,  secretKey, getToken());

    assertManagedAccountBalanceAndState(managedAccountId, getToken(), expectedBalance, State.ACTIVE);
    assertManagedAccountStatement(managedAccountId, getToken(), Currency.EUR.name(), (int) depositAmount, depositFee, true);

    final long overTheLimitDepositAmount =
        getIdentityType() == IdentityType.CORPORATE ? (corporateMaxSum / 2 + 10000)  : (consumerMaxSum / 2 + 10000) ;

    final SimulatePendingDepositModel simulatePendingDepositModel =
        SimulatePendingDepositModel.builder()
            .senderName("Test")
            .senderIban(ModelHelper.generateRandomValidIban())
            .depositAmount(new CurrencyAmount(Currency.EUR.name(), overTheLimitDepositAmount))
            .webhook(true)
            .immediateMonitorReplyExpected(true)
            .reference("Ref")
            .build();

    SimulatorService.simulateManagedAccountPendingDepositByIban(simulatePendingDepositModel, innovatorId, iban)
        .then()
        .statusCode(SC_OK);

    final Map<String, String> deposit = getDepositByIbanIdAndState(iban, "REJECTED").get(0);
    assertRejectedDepositState(deposit.get("id"), opc.services.admin.AdminService.loginAdmin());
  }

  @Test
  public void PendingDeposit_SepaInstantRejected_Success() throws SQLException {
    //Deposit above the identity type limits
    final long depositAmount =
        getIdentityType() == IdentityType.CORPORATE ? corporateMaxSum + 100 : consumerMaxSum + 100;

    final String managedAccountId =
        ManagedAccountsHelper.createManagedAccount(getManagedAccountProfileId(),
            Currency.EUR.name(), secretKey, getToken());

    ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, getToken());

    final String iban = getManagedAccountIbanId(managedAccountId);

    TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
        ModelHelper.generateRandomValidIban(), Currency.EUR.name(), depositAmount,true, true, "RefTest123", false, innovatorId,  secretKey, getToken());

    final Map<String, String> deposit = getDepositByIbanIdAndState(iban, "REJECTED").get(0);
    assertRejectedDepositState(deposit.get("id"), opc.services.admin.AdminService.loginAdmin());
  }

  private void assertRejectedDepositState(final String id,
                                          final String token) {
    TestHelper.ensureAsExpected(120,
            () -> opc.services.admin.AdminService.getDeposit(token, id),
            x-> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(State.REJECTED.name()),
            Optional.of(String.format("Expecting 200 with an Deposit in state %s, check logged payload", State.REJECTED.name())));
  }
}
