package opc.junit.multi.deposit;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import commons.enums.State;
import java.sql.SQLException;
import opc.enums.opc.InnovatorSetup;
import opc.junit.database.IncomingWireTransfersDataBaseHelper;
import opc.junit.database.ManagedAccountsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.simulator.SimulateDepositModel;
import opc.services.multi.ManagedAccountsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;
import java.util.Optional;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.DEPOSITS)
public class BaseDepositTests {

  @RegisterExtension
  static BaseSetupExtension setupExtension = new BaseSetupExtension();

  protected static String corporateProfileId;
  protected static String consumerProfileId;
  protected static String consumerManagedAccountProfileId;
  protected static String corporateManagedAccountProfileId;
  protected static String secretKey;
  protected static String corporateAuthenticationToken;
  protected static String consumerAuthenticationToken;
  protected static String innovatorId;

  @BeforeAll
  public static void GlobalSetup(){

    final ProgrammeDetailsModel applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(
        InnovatorSetup.APPLICATION_ONE);

    corporateProfileId = applicationOne.getCorporatesProfileId();
    consumerProfileId = applicationOne.getConsumersProfileId();
    consumerManagedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();
    corporateManagedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
    innovatorId = applicationOne.getInnovatorId();
    secretKey = applicationOne.getSecretKey();
  }

  protected static void consumerSetup() {
    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

    final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
    consumerAuthenticationToken = authenticatedConsumer.getRight();
  }

  protected static void corporateSetup() {
    final CreateCorporateModel createCorporateModel =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
    corporateAuthenticationToken = authenticatedCorporate.getRight();
  }

  protected void assertManagedAccountBalanceAndState(final String managedAccountId,
      final String authenticationToken,
      final int expectedBalance,
      final State state) {

    ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken)
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
                                               final int depositFee,
                                               final boolean isPendingDeposit) {

    ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, authenticationToken)
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
        .body("entry[0].additionalFields.sender", equalTo(isPendingDeposit? "SenderTest" : "Sender Test"))
        .body("entry[0].additionalFields.senderReference", equalTo("RefTest123"))
        .body("entry[0].additionalFields.senderIban",notNullValue())
        .body("entry[0].processedTimestamp",notNullValue())
        .body("count", equalTo(1))
        .body("responseCount", equalTo(1));
  }



  protected void assertManagedAccountStatement(final String managedAccountId,
                                               final String authenticationToken,
                                               final SimulateDepositModel simulateDepositModel,
                                               final int depositFee) {

    ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, authenticationToken)
            .then()
            .body("entry[0].transactionId.id", notNullValue())
            .body("entry[0].transactionId.type", equalTo("DEPOSIT"))
            .body("entry[0].transactionAmount.currency", equalTo(simulateDepositModel.getDepositAmount().getCurrency()))
            .body("entry[0].transactionAmount.amount", equalTo(simulateDepositModel.getDepositAmount().getAmount().intValue()))
            .body("entry[0].availableBalanceAdjustment.currency", equalTo(simulateDepositModel.getDepositAmount().getCurrency()))
            .body("entry[0].availableBalanceAdjustment.amount", equalTo(simulateDepositModel.getDepositAmount().getAmount().intValue() - depositFee))
            .body("entry[0].availableBalanceAfter.currency", equalTo(simulateDepositModel.getDepositAmount().getCurrency()))
            .body("entry[0].availableBalanceAfter.amount", equalTo(simulateDepositModel.getDepositAmount().getAmount().intValue() - depositFee))
            .body("entry[0].actualBalanceAdjustment.currency", equalTo(simulateDepositModel.getDepositAmount().getCurrency()))
            .body("entry[0].actualBalanceAdjustment.amount", equalTo(simulateDepositModel.getDepositAmount().getAmount().intValue() - depositFee))
            .body("entry[0].actualBalanceAfter.currency", equalTo(simulateDepositModel.getDepositAmount().getCurrency()))
            .body("entry[0].actualBalanceAfter.amount", equalTo(simulateDepositModel.getDepositAmount().getAmount().intValue() - depositFee))
            .body("entry[0].balanceAfter.currency", equalTo(simulateDepositModel.getDepositAmount().getCurrency()))
            .body("entry[0].balanceAfter.amount", equalTo(simulateDepositModel.getDepositAmount().getAmount().intValue() - depositFee))
            .body("entry[0].transactionFee.currency", equalTo(simulateDepositModel.getDepositAmount().getCurrency()))
            .body("entry[0].transactionFee.amount", equalTo(depositFee))
            .body("entry[0].cardholderFee.currency", equalTo(simulateDepositModel.getDepositAmount().getCurrency()))
            .body("entry[0].cardholderFee.amount", equalTo(depositFee))
            .body("entry[0].entryState", equalTo("COMPLETED"))
            .body("entry[0].additionalFields.sender", equalTo(simulateDepositModel.getSenderName()))
            .body("entry[0].additionalFields.senderReference", equalTo(simulateDepositModel.getReference()))
            .body("entry[0].additionalFields.senderIban",notNullValue())
            .body("entry[0].processedTimestamp",notNullValue())
            .body("count", equalTo(1))
            .body("responseCount", equalTo(1));
  }

  protected Map<Integer, Map<String, String>> getDepositByManagedAccount(final String managedAccountId) {
   return  TestHelper.ensureDatabaseResultAsExpected(20,
           () -> ManagedAccountsDatabaseHelper.getDepositByManagedAccount(managedAccountId),
           x -> !x.isEmpty(),
           Optional.of(String.format("Deposit by managed account id %s not retrieved.", managedAccountId)));
  }

  protected Map<Integer, Map<String, String>> getDepositByIbanId(final String ibanId) {
    return  TestHelper.ensureDatabaseResultAsExpected(20,
        () -> IncomingWireTransfersDataBaseHelper.getIwtByIbanId(ibanId),
        x -> !x.isEmpty(),
        Optional.of(String.format("IWT by iban_id %s not retrieved.", ibanId)));
  }


  protected Map<Integer, Map<String, String>> getDepositByManagedAccountAndState(final String managedAccountId, final String state) {
    return  TestHelper.ensureDatabaseResultAsExpected(45,
            () -> ManagedAccountsDatabaseHelper.getDepositByManagedAccountAndState(managedAccountId, state),
            x -> !x.isEmpty() && x.get(0).get("deposit_state").equals(state),
            Optional.of(String.format("Deposit by managed account id %s not retrieved.", managedAccountId)));
  }

  protected Map<Integer, Map<String, String>> getDepositByIbanIdAndState(final String ibanId, final String state) {
    return  TestHelper.ensureDatabaseResultAsExpected(45,
        () -> IncomingWireTransfersDataBaseHelper.getIwtByIbanIdAndState(ibanId, state),
        x -> !x.isEmpty() && x.get(0).get("state").equals(state),
        Optional.of(String.format("IWT by iban_id %s not retrieved.", ibanId)));
  }

  protected String getManagedAccountIbanId(final String managedAccountId) throws SQLException {
    return  ManagedAccountsDatabaseHelper.getIbanByManagedAccount(managedAccountId).get(0).get("iban_id");
  }
}
