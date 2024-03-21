package opc.junit.multi.modulr;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import commons.enums.Currency;
import commons.enums.State;
import io.restassured.response.Response;
import java.sql.SQLException;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.helpers.ModelHelper;
import opc.junit.database.ModulrDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.innovator.UpdateManagedAccountProfileModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.shared.CurrencyAmount;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ModulrPendingDepositTests extends BaseModulrSetup {


  private static String innovatorToken;

  @BeforeAll
  public static void Setup() {
    innovatorToken = InnovatorHelper.loginInnovator(payneticsInnovatorEmail, payneticsInnovatorPassword);
    InnovatorService.updateManagedAccountProfile(new UpdateManagedAccountProfileModel.Builder().setProxyFiProvider("modulr").build(), payneticsCorporateManagedAccountProfileId, innovatorToken, payneticsProgrammeId);
  }

  @Test
  public void ModulrDeposit_DepositToModulrProxyIban_Success() throws SQLException {
    final long depositAmount = 10000L;
    final int depositFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();
    final int expectedBalance = (int) depositAmount - depositFee;

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.CurrencyCreateCorporateModel( payneticsCorporateProfileId, Currency.EUR).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, payneticsSecretKey);
    CorporatesHelper.verifyEmail(createCorporateModel.getRootUser().getEmail(), payneticsSecretKey);

    AdminService.getModulrSubscriptionPermission(adminToken, corporate.getLeft())
        .then()
        .statusCode(SC_OK)
        .body("subscriberId", equalTo(corporate.getLeft()))
        .body("subscriberType", equalTo(IdentityType.CORPORATE.getValue()))
        .body("status", equalTo("READY_TO_SUBSCRIBE"));

    assertEquals("READY_TO_SUBSCRIBE", ModulrDatabaseHelper.getSubscriber(corporate.getLeft()).get(0).get("status"));

    final CreateManagedAccountModel managedAccountModel = CreateManagedAccountModel.DefaultCreateManagedAccountModel
        (payneticsCorporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();


    final String managedAccountId = ManagedAccountsHelper.createManagedAccount(managedAccountModel, payneticsSecretKey, corporate.getRight());

    AdminService.upgradeManagedAccount(managedAccountId, adminToken)
        .then()
        .statusCode(SC_OK);

    //check that we have created a proxy innovator iban on modulr side
    final Response response =
    InnovatorService.getManagedAccount(managedAccountId, innovatorToken);

    response.then().statusCode(SC_OK);

   final String modulrIban = response.path("bankAccountDetails.findAll{bankDetail -> bankDetail.fiProvider=='modulr'}.iban[0]");

    TestHelper.simulateDepositWithIban(managedAccountId, modulrIban,
        new CurrencyAmount(createCorporateModel.getBaseCurrency(), depositAmount),
        payneticsSecretKey, corporate.getRight(), expectedBalance);

    assertManagedAccountBalanceAndState(managedAccountId, corporate.getRight(), expectedBalance, State.ACTIVE);
    assertManagedAccountStatement(managedAccountId, corporate.getRight(), Currency.EUR.name(), (int) depositAmount, depositFee);
  }

  protected void assertManagedAccountBalanceAndState(final String managedAccountId,
                                                     final String authenticationToken,
                                                     final int expectedBalance,
                                                     final State state) {

    ManagedAccountsService.getManagedAccount(payneticsSecretKey, managedAccountId, authenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("id", notNullValue())
        .body("balances.availableBalance", equalTo(expectedBalance))
        .body("balances.actualBalance", equalTo(expectedBalance))
        .body("state.state", equalTo(state.name()))
        .body("bankAccountDetails", nullValue())
        .body("creationTimestamp", notNullValue());
  }

  protected void assertManagedAccountStatement(final String managedAccountId,
                                               final String authenticationToken,
                                               final String currency,
                                               final int depositAmount,
                                               final int depositFee) {

    ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, payneticsSecretKey, authenticationToken)
        .then()
        .body("entry[0].transactionId.id", notNullValue())
        .body("entry[0].transactionId.type", equalTo("DEPOSIT"))
        .body("entry[0].transactionAmount.currency", equalTo(currency))
        .body("entry[0].transactionAmount.amount", equalTo(depositAmount))
        .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency))
        .body("entry[0].availableBalanceAdjustment.amount", equalTo(depositAmount - depositFee))
        .body("entry[0].availableBalanceAfter.currency", equalTo(currency))
        .body("entry[0].availableBalanceAfter.amount", equalTo(depositAmount - depositFee))
        .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency))
        .body("entry[0].actualBalanceAdjustment.amount", equalTo(depositAmount - depositFee))
        .body("entry[0].actualBalanceAfter.currency", equalTo(currency))
        .body("entry[0].actualBalanceAfter.amount", equalTo(depositAmount - depositFee))
        .body("entry[0].balanceAfter.currency", equalTo(currency))
        .body("entry[0].balanceAfter.amount", equalTo(depositAmount - depositFee))
        .body("entry[0].transactionFee.currency", equalTo(currency))
        .body("entry[0].transactionFee.amount", equalTo(depositFee))
        .body("entry[0].cardholderFee.currency", equalTo(currency))
        .body("entry[0].cardholderFee.amount", equalTo(depositFee))
        .body("entry[0].entryState", equalTo("COMPLETED"))
        .body("entry[0].additionalFields.sender", equalTo("Test Payer"))
        .body("entry[0].additionalFields.senderIban",notNullValue())
        .body("entry[0].processedTimestamp",notNullValue())
        .body("count", equalTo(1))
        .body("responseCount", equalTo(1));
  }
}