package opc.junit.multi.owt.cannedibans;

import opc.enums.opc.CannedIbanState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.outgoingwiretransfers.Beneficiary;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.shared.CurrencyAmount;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.OutgoingWireTransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Optional;

import static java.lang.Math.negateExact;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Execution(ExecutionMode.SAME_THREAD)
public abstract class AbstractCannedOutgoingWireTransferTests extends
    BaseCannedResponsesSetup {
  protected abstract String getToken();
  protected abstract String getIdentityId();
  protected abstract String getCurrency();
  protected abstract String getManagedAccountProfileId();
  protected abstract IdentityType getIdentityType();

  private static final String OTP_CHANNEL = EnrolmentChannel.SMS.name();
  private static String innovatorToken;

  /**
   * Test cases for Canned OWTs (Including Returns)
   * The main cases:
   * 1. OWTs for Returns
   * 2. OWTs to rest of canned IBANs
   */

  @BeforeAll
  public static void InnovatorSetup() {
    innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
  }

    @ParameterizedTest
    @DisplayName("OWT return")
    @ValueSource(booleans = {true, false})
    public void CannedOwt_OwtReturn_Success(final boolean returnFee) {
      InnovatorHelper.updateOwtProfileReturnFeeDecision(returnFee,
          outgoingWireTransfersProfileId, programmeId, innovatorToken);

      final Long depositAmount = 10000L;
      final Long sendAmount = 100L;

      final int fee = TestHelper.getFees(getCurrency()).get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

      final Pair<String, CreateManagedAccountModel> managedAccount =
          createManagedAccount(getManagedAccountProfileId(), getCurrency(), getToken());

      fundManagedAccount(managedAccount.getLeft(), getCurrency(), depositAmount);

      final String owtId = createOwt(managedAccount.getLeft(), sendAmount, CannedIbanState.RETURN_OWT, secretKey, getToken());

      OutgoingWireTransfersHelper.verifyOwtOtp(owtId, secretKey, getToken());

      // The send amount is returned. If return fee is true on owt profile level,
      // we expect to return full amount, if not, innovator keeps the fee
      final int expectedBalance = returnFee ? depositAmount.intValue() : depositAmount.intValue() - fee;

      assertOutgoingWireTransferState(owtId, getToken(), State.RETURNED);
      assertManagedAccountBalance(managedAccount.getLeft(), getToken(),secretKey, expectedBalance);
      assertInnovatorManagedAccountStatement(returnFee, depositAmount.intValue(),
          sendAmount.intValue(), fee,managedAccount.getLeft(), innovatorToken);
    }

    @ParameterizedTest
    @DisplayName("OWT return before complete")
    @ValueSource(booleans = {true, false})
    public void CannedOwt_OwtReturnBeforeComplete_Success(final boolean returnFee) {
      InnovatorHelper.updateOwtProfileReturnFeeDecision(returnFee,
          outgoingWireTransfersProfileId, programmeId, innovatorToken);

      final Long depositAmount = 10000L;
      final Long sendAmount = 100L;

      final int fee = TestHelper.getFees(getCurrency()).get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

      final Pair<String, CreateManagedAccountModel> managedAccount =
          createManagedAccount(getManagedAccountProfileId(), getCurrency(), getToken());

      fundManagedAccount(managedAccount.getLeft(), getCurrency(), depositAmount);

      final String owtId = createOwt(managedAccount.getLeft(), sendAmount, CannedIbanState.RETURN_BEFORE_COMPLETE_OWT, secretKey, getToken());

      OutgoingWireTransfersHelper.verifyOwtOtp(owtId, secretKey, getToken());

      // The send amount is returned. If return fee is true on owt profile level,
      // we expect to return full amount, if not, innovator keeps the fee
      final int expectedBalance = returnFee ? depositAmount.intValue() : depositAmount.intValue() - fee;

      assertOutgoingWireTransferState(owtId, getToken(), State.RETURNED);
      assertManagedAccountBalance(managedAccount.getLeft(), getToken(),secretKey, expectedBalance);
      assertInnovatorManagedAccountStatement(returnFee, depositAmount.intValue(),
          sendAmount.intValue(), fee,managedAccount.getLeft(), innovatorToken);
    }

    @Test
    public void CannedOwt_InternalTransfer_Success() {

      InnovatorHelper.updateOwtProfileReturnFeeDecision(false,
              outgoingWireTransfersProfileId, programmeId, innovatorToken);

      final Long depositAmount = 10000L;
      final long sendAmount = 125L;

      final Map<FeeType, CurrencyAmount> fees = TestHelper.getFees(getCurrency());
      final int owtFee = fees.get(FeeType.SEPA_OWT_FEE).getAmount().intValue();
      final int depositFee = fees.get(FeeType.DEPOSIT_FEE).getAmount().intValue();

      final Pair<String, CreateManagedAccountModel> managedAccount =
              createManagedAccount(getManagedAccountProfileId(), getCurrency(), getToken());

      fundManagedAccount(managedAccount.getLeft(), getCurrency(), depositAmount);

      final OutgoingWireTransfersModel outgoingWireTransfersModel =
              OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                              managedAccount.getLeft(),
                              getCurrency(), sendAmount, OwtType.SEPA)
                      .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa()
                              .setBankAccountDetails(new SepaBankDetailsModel(CannedIbanState.INTERNAL_TRANSFER.getIban(), ModelHelper.generateRandomValidBankIdentifierNumber()))
                              .build())
                      .setDescription(RandomStringUtils.randomAlphabetic(35))
                      .build();

      final String owtId =
              OutgoingWireTransfersHelper.sendSuccessfulOwtOtp(outgoingWireTransfersModel, secretKey, getToken()).getLeft();

      final int owtExpectedBalance = (int)(depositAmount.intValue() - sendAmount - owtFee);
      final int finalExpectedBalance = depositAmount.intValue() - owtFee - depositFee;

      assertOutgoingWireTransferState(owtId, getToken(), State.COMPLETED);

      final SepaBankDetailsModel sepaBankDetails =
              (SepaBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary().getBankAccountDetails();

      ManagedAccountsHelper.getManagedAccountStatement(managedAccount.getLeft(), secretKey, getToken(), 4)
              .then()
              .statusCode(SC_OK)
              .body("entry[0].transactionId.id", notNullValue())
              .body("entry[0].transactionId.type", equalTo("DEPOSIT"))
              .body("entry[0].transactionAmount.currency", equalTo(getCurrency()))
              .body("entry[0].transactionAmount.amount", equalTo((int) sendAmount))
              .body("entry[0].availableBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)(sendAmount - depositFee)))
              .body("entry[0].availableBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[0].availableBalanceAfter.amount", equalTo(finalExpectedBalance))
              .body("entry[0].actualBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)(sendAmount - depositFee)))
              .body("entry[0].actualBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[0].actualBalanceAfter.amount", equalTo(finalExpectedBalance))
              .body("entry[0].balanceAfter.currency", equalTo(getCurrency()))
              .body("entry[0].balanceAfter.amount", equalTo(finalExpectedBalance))
              .body("entry[0].transactionFee.currency", equalTo(getCurrency()))
              .body("entry[0].transactionFee.amount", equalTo(depositFee))
              .body("entry[0].cardholderFee.currency", equalTo(getCurrency()))
              .body("entry[0].cardholderFee.amount", equalTo(depositFee))
              .body("entry[0].entryState", equalTo("COMPLETED"))
              .body("entry[0].additionalFields.sender", notNullValue())
              .body("entry[0].additionalFields.senderReference", notNullValue())
              .body("entry[0].additionalFields.senderIban",notNullValue())
              .body("entry[0].processedTimestamp",notNullValue())
              .body("entry[1].transactionId.id", equalTo(owtId))
              .body("entry[1].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
              .body("entry[1].transactionAmount.currency", equalTo(getCurrency()))
              .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) sendAmount)))
              .body("entry[1].balanceAfter.currency", equalTo(getCurrency()))
              .body("entry[1].balanceAfter.amount", equalTo(owtExpectedBalance))
              .body("entry[1].cardholderFee.currency", equalTo(getCurrency()))
              .body("entry[1].cardholderFee.amount", equalTo(owtFee))
              .body("entry[1].processedTimestamp", notNullValue())
              .body("entry[1].additionalFields.description", equalTo(outgoingWireTransfersModel.getDescription()))
              .body("entry[1].additionalFields.beneficiaryName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
              .body("entry[1].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
              .body("entry[1].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
              .body("entry[1].availableBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[1].availableBalanceAfter.amount", equalTo(owtExpectedBalance))
              .body("entry[1].availableBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
              .body("entry[1].actualBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[1].actualBalanceAfter.amount", equalTo(owtExpectedBalance))
              .body("entry[1].actualBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[1].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int)(sendAmount + owtFee))))
              .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
              .body("entry[2].transactionId.id", equalTo(owtId))
              .body("entry[2].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
              .body("entry[2].transactionAmount.currency", equalTo(getCurrency()))
              .body("entry[2].transactionAmount.amount", equalTo(Math.negateExact((int) sendAmount)))
              .body("entry[2].balanceAfter.currency", equalTo(getCurrency()))
              .body("entry[2].balanceAfter.amount", equalTo(owtExpectedBalance))
              .body("entry[2].cardholderFee.currency", equalTo(getCurrency()))
              .body("entry[2].cardholderFee.amount", equalTo(owtFee))
              .body("entry[2].processedTimestamp", notNullValue())
              .body("entry[2].additionalFields.description", equalTo(outgoingWireTransfersModel.getDescription()))
              .body("entry[2].additionalFields.beneficiaryName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
              .body("entry[2].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
              .body("entry[2].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
              .body("entry[2].availableBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[2].availableBalanceAfter.amount", equalTo(owtExpectedBalance))
              .body("entry[2].availableBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[2].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int)(sendAmount + owtFee))))
              .body("entry[2].actualBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[2].actualBalanceAfter.amount", equalTo(depositAmount.intValue()))
              .body("entry[2].actualBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[2].actualBalanceAdjustment.amount", equalTo(0))
              .body("entry[2].entryState", equalTo(State.PENDING.name()))
              .body("entry[3].transactionId.id", notNullValue())
              .body("entry[3].transactionId.type", equalTo("MANUAL_TRANSACTION"))
              .body("entry[3].transactionAmount.currency", equalTo(getCurrency()))
              .body("entry[3].transactionAmount.amount", equalTo(depositAmount.intValue()))
              .body("entry[3].balanceAfter.currency", equalTo(getCurrency()))
              .body("entry[3].balanceAfter.amount", equalTo(depositAmount.intValue()))
              .body("entry[3].cardholderFee.currency", equalTo(getCurrency()))
              .body("entry[3].cardholderFee.amount", equalTo(0))
              .body("entry[3].processedTimestamp", notNullValue())
              .body("count", equalTo(4))
              .body("responseCount", equalTo(4));

      assertManagedAccountBalance(managedAccount.getLeft(), getToken(), secretKey, finalExpectedBalance);
    }

    @Test
    public void CannedOwt_InternalTransferWithOtherAccountIban_Success() {

      InnovatorHelper.updateOwtProfileReturnFeeDecision(false,
              outgoingWireTransfersProfileId, programmeId, innovatorToken);

      final Long depositAmount = 10000L;
      final long sendAmount = 125L;

      final Map<FeeType, CurrencyAmount> fees = TestHelper.getFees(getCurrency());
      final int owtFee = fees.get(FeeType.SEPA_OWT_FEE).getAmount().intValue();
      final int depositFee = fees.get(FeeType.DEPOSIT_FEE).getAmount().intValue();

      final Pair<String, CreateManagedAccountModel> destinationManagedAccount =
              createManagedAccount(getManagedAccountProfileId(), getCurrency(), getToken());

      final String iban =
              ManagedAccountsService.getManagedAccountIban(secretKey, destinationManagedAccount.getLeft(),
                      getToken()).jsonPath().getString("bankAccountDetails[0].details.iban");

      final Pair<String, CreateManagedAccountModel> sourceManagedAccount =
              createManagedAccount(getManagedAccountProfileId(), getCurrency(), getToken());

      fundManagedAccount(sourceManagedAccount.getLeft(), getCurrency(), depositAmount);

      final OutgoingWireTransfersModel outgoingWireTransfersModel =
              OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                              sourceManagedAccount.getLeft(),
                              getCurrency(), sendAmount, OwtType.SEPA)
                      .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa()
                              .setBankAccountDetails(new SepaBankDetailsModel(iban, ModelHelper.generateRandomValidBankIdentifierNumber()))
                              .build())
                      .setDescription(RandomStringUtils.randomAlphabetic(35))
                      .build();

      final String owtId =
              OutgoingWireTransfersHelper.sendSuccessfulOwtOtp(outgoingWireTransfersModel, secretKey, getToken()).getLeft();

      final int sourceExpectedBalance = depositAmount.intValue() - (int) sendAmount - owtFee;
      final int destinationExpectedBalance = (int) sendAmount - depositFee;

      assertOutgoingWireTransferState(owtId, getToken(), State.COMPLETED);

      final SepaBankDetailsModel sepaBankDetails =
              (SepaBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary().getBankAccountDetails();

      ManagedAccountsHelper.getManagedAccountStatement(sourceManagedAccount.getLeft(), secretKey, getToken(), 3)
              .then()
              .statusCode(SC_OK)
              .body("entry[0].transactionId.id", equalTo(owtId))
              .body("entry[0].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
              .body("entry[0].transactionAmount.currency", equalTo(getCurrency()))
              .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) sendAmount)))
              .body("entry[0].balanceAfter.currency", equalTo(getCurrency()))
              .body("entry[0].balanceAfter.amount", equalTo(sourceExpectedBalance))
              .body("entry[0].cardholderFee.currency", equalTo(getCurrency()))
              .body("entry[0].cardholderFee.amount", equalTo(owtFee))
              .body("entry[0].processedTimestamp", notNullValue())
              .body("entry[0].additionalFields.description", equalTo(outgoingWireTransfersModel.getDescription()))
              .body("entry[0].additionalFields.beneficiaryName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
              .body("entry[0].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
              .body("entry[0].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
              .body("entry[0].availableBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[0].availableBalanceAfter.amount", equalTo(sourceExpectedBalance))
              .body("entry[0].availableBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
              .body("entry[0].actualBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[0].actualBalanceAfter.amount", equalTo(sourceExpectedBalance))
              .body("entry[0].actualBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int)(sendAmount + owtFee))))
              .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
              .body("entry[1].transactionId.id", equalTo(owtId))
              .body("entry[1].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
              .body("entry[1].transactionAmount.currency", equalTo(getCurrency()))
              .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) sendAmount)))
              .body("entry[1].balanceAfter.currency", equalTo(getCurrency()))
              .body("entry[1].balanceAfter.amount", equalTo(sourceExpectedBalance))
              .body("entry[1].cardholderFee.currency", equalTo(getCurrency()))
              .body("entry[1].cardholderFee.amount", equalTo(owtFee))
              .body("entry[1].processedTimestamp", notNullValue())
              .body("entry[1].additionalFields.description", equalTo(outgoingWireTransfersModel.getDescription()))
              .body("entry[1].additionalFields.beneficiaryName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
              .body("entry[1].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
              .body("entry[1].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
              .body("entry[1].availableBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[1].availableBalanceAfter.amount", equalTo(sourceExpectedBalance))
              .body("entry[1].availableBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int)(sendAmount + owtFee))))
              .body("entry[1].actualBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[1].actualBalanceAfter.amount", equalTo(depositAmount.intValue()))
              .body("entry[1].actualBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
              .body("entry[1].entryState", equalTo(State.PENDING.name()))
              .body("entry[2].transactionId.id", notNullValue())
              .body("entry[2].transactionId.type", equalTo("MANUAL_TRANSACTION"))
              .body("entry[2].transactionAmount.currency", equalTo(getCurrency()))
              .body("entry[2].transactionAmount.amount", equalTo(depositAmount.intValue()))
              .body("entry[2].balanceAfter.currency", equalTo(getCurrency()))
              .body("entry[2].balanceAfter.amount", equalTo(depositAmount.intValue()))
              .body("entry[2].cardholderFee.currency", equalTo(getCurrency()))
              .body("entry[2].cardholderFee.amount", equalTo(0))
              .body("entry[2].processedTimestamp", notNullValue())
              .body("count", equalTo(3))
              .body("responseCount", equalTo(3));

      ManagedAccountsHelper.getManagedAccountStatement(destinationManagedAccount.getLeft(), secretKey, getToken(), 1)
              .then()
              .body("entry[0].transactionId.id", notNullValue())
              .body("entry[0].transactionId.type", equalTo("DEPOSIT"))
              .body("entry[0].transactionAmount.currency", equalTo(getCurrency()))
              .body("entry[0].transactionAmount.amount", equalTo((int) sendAmount))
              .body("entry[0].availableBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[0].availableBalanceAdjustment.amount", equalTo((int)(sendAmount - depositFee)))
              .body("entry[0].availableBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[0].availableBalanceAfter.amount", equalTo((int)(sendAmount - depositFee)))
              .body("entry[0].actualBalanceAdjustment.currency", equalTo(getCurrency()))
              .body("entry[0].actualBalanceAdjustment.amount", equalTo((int)(sendAmount - depositFee)))
              .body("entry[0].actualBalanceAfter.currency", equalTo(getCurrency()))
              .body("entry[0].actualBalanceAfter.amount", equalTo((int)(sendAmount - depositFee)))
              .body("entry[0].balanceAfter.currency", equalTo(getCurrency()))
              .body("entry[0].balanceAfter.amount", equalTo((int)(sendAmount - depositFee)))
              .body("entry[0].transactionFee.currency", equalTo(getCurrency()))
              .body("entry[0].transactionFee.amount", equalTo(depositFee))
              .body("entry[0].cardholderFee.currency", equalTo(getCurrency()))
              .body("entry[0].cardholderFee.amount", equalTo(depositFee))
              .body("entry[0].entryState", equalTo("COMPLETED"))
              .body("entry[0].additionalFields.sender", notNullValue())
              .body("entry[0].additionalFields.senderReference", notNullValue())
              .body("entry[0].additionalFields.senderIban",notNullValue())
              .body("entry[0].processedTimestamp",notNullValue())
              .body("count", equalTo(1))
              .body("responseCount", equalTo(1));

      assertManagedAccountBalance(sourceManagedAccount.getLeft(), getToken(),secretKey, sourceExpectedBalance);
      assertManagedAccountBalance(destinationManagedAccount.getLeft(), getToken(),secretKey, destinationExpectedBalance);
    }

  @Test
  public void CannedOwt_OwtRejected_Success() {
    final Long depositAmount = 10000L;
    final Long sendAmount = 100L;

    final int fee = TestHelper.getFees(getCurrency()).get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

    final Pair<String, CreateManagedAccountModel> managedAccount =
        createManagedAccount(getManagedAccountProfileId(), getCurrency(), getToken());

    fundManagedAccount(managedAccount.getLeft(), getCurrency(), depositAmount);

    final String owtId = createOwt(managedAccount.getLeft(), sendAmount, CannedIbanState.REJECTED_OWT, secretKey, getToken());

    OutgoingWireTransfersHelper.verifyOwtOtp(owtId, secretKey, getToken());

    final int expectedBalance = depositAmount.intValue();

    assertOutgoingWireTransferState(owtId, getToken(), State.REJECTED);
    assertManagedAccountBalance(managedAccount.getLeft(), getToken(),secretKey, expectedBalance);

    InnovatorService.getManagedAccountStatement(managedAccount.getLeft(), innovatorToken)
        .then()
        .statusCode(SC_OK)
        .body("entry[0].txId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
        .body("entry[0].transactionAmount.amount", equalTo(Integer.toString(sendAmount.intValue())))
        .body("entry[0].balanceAfter.amount", equalTo(Integer.toString(depositAmount.intValue())))
        .body("entry[0].cardholderFee.amount", equalTo(Integer.toString(fee)))
        .body("entry[0].statementEntryState", equalTo("COMPLETED"))
        .body("entry[0].transactionState", equalTo("REJECTED"))
        .body("entry[0].availableBalanceAfter.amount", equalTo(Integer.toString(depositAmount.intValue())))
        .body("entry[0].actualBalanceAfter.amount", equalTo(Integer.toString(depositAmount.intValue())))
        .body("entry[1].txId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
        .body("entry[1].transactionAmount.amount", equalTo(Integer.toString(negateExact(sendAmount.intValue()))))
        .body("entry[1].cardholderFee.amount", equalTo(Integer.toString(fee)))
        .body("entry[1].actualBalanceAfter.amount", equalTo(Integer.toString(depositAmount.intValue())))
        .body("entry[1].actualBalanceAdjustment.amount", equalTo(Integer.toString(0)))
        .body("entry[1].availableBalanceAfter.amount", equalTo(Integer.toString(depositAmount.intValue() - sendAmount.intValue() - fee)))
        .body("entry[1].availableBalanceAdjustment.amount", equalTo(Integer.toString(negateExact(sendAmount.intValue() + fee))))
        .body("entry[1].statementEntryState", equalTo("PENDING"))
        .body("entry[1].transactionState", equalTo("REJECTED"));
  }

  private void assertOutgoingWireTransferState(final String id,
                                               final String token,
                                               final State state) {
    TestHelper.ensureAsExpected(240,
        () -> OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, token),
        x-> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
        Optional.of(String.format("Expecting 200 with an OWT in state %s, check logged payload", state.name())));
  }

  private void assertManagedAccountBalance(final String managedAccountId,
                                           final String token,
                                           final String secretKey,
                                           final int expectedBalance) {
    ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token)
        .then()
        .statusCode(SC_OK)
        .body("balances.availableBalance", equalTo(expectedBalance))
        .body("balances.actualBalance", equalTo(expectedBalance));
  }

  private String createOwt(final String managedAccountId,
                           final Long sendAmount,
                           final CannedIbanState cannedIbanState,
                           final String secretKey,
                           final String token) {
    final OutgoingWireTransfersModel outgoingWireTransfersModel =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                managedAccountId,
                getCurrency(), sendAmount, OwtType.SEPA)
            .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa(cannedIbanState).build())
            .setDescription(
                RandomStringUtils.randomAlphabetic(35))
            .build();

    return OutgoingWireTransfersService.sendOutgoingWireTransfer
            (outgoingWireTransfersModel, secretKey, token, Optional.empty())
        .then()
        .statusCode(SC_OK)
        .extract()
        .jsonPath()
        .get("id");
  }

  public void assertInnovatorManagedAccountStatement(final boolean returnFee,
                                                     final int depositAmount,
                                                     final int sendAmount,
                                                     final int fee,
                                                     final String managedAccountId,
                                                     final String innovatorToken) {
    if (returnFee) {
      InnovatorService.getManagedAccountStatement(managedAccountId, innovatorToken)
          .then()
          .statusCode(SC_OK)
          .body("entry[0].txId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
          .body("entry[0].transactionAmount.amount", equalTo(Integer.toString(sendAmount)))
          .body("entry[0].balanceAfter.amount", equalTo(Integer.toString(depositAmount)))
          .body("entry[0].cardholderFee.amount", equalTo(Integer.toString(0)))
          .body("entry[0].additionalFields.relatedTransactionIdType", equalTo("FEE_REVERSAL"))
          .body("entry[0].statementEntryState", equalTo("COMPLETED"))
          .body("entry[0].transactionState", equalTo("RETURNED"))
          .body("entry[1].txId.type", equalTo("FEE_REVERSAL"))
          .body("entry[1].transactionAmount.amount", equalTo(Integer.toString(fee)))
          .body("entry[1].cardholderFee.amount", equalTo(Integer.toString(negateExact(fee))))
          .body("entry[1].additionalFields.relatedTransactionIdType", equalTo("OUTGOING_WIRE_TRANSFER"))
          .body("entry[1].additionalFields.note", equalTo("OWT RETURNED TO SENDER"))
          .body("entry[1].transactionState", equalTo("COMPLETED"))
          .body("entry[1].statementEntryState", equalTo("COMPLETED"))
          .body("entry[2].txId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
          .body("entry[2].transactionAmount.amount", equalTo(Integer.toString(negateExact(sendAmount))))
          .body("entry[2].cardholderFee.amount", equalTo(Integer.toString(fee)))
          .body("entry[2].transactionState", equalTo("COMPLETED"))
          .body("entry[2].transactionState", equalTo("COMPLETED"))
          .body("entry[2].actualBalanceAfter.amount", equalTo(Integer.toString(depositAmount - sendAmount - fee)))
          .body("entry[2].actualBalanceAdjustment.amount", equalTo(Integer.toString(negateExact(sendAmount + fee))));
    }
    else {
      InnovatorService.getManagedAccountStatement(managedAccountId, innovatorToken)
          .then()
          .statusCode(SC_OK)
          .body("entry[0].txId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
          .body("entry[0].transactionAmount.amount", equalTo(Integer.toString(sendAmount)))
          .body("entry[0].balanceAfter.amount", equalTo(Integer.toString(depositAmount - fee)))
          .body("entry[0].cardholderFee.amount", equalTo(Integer.toString(0)))
          .body("entry[0].statementEntryState", equalTo("COMPLETED"))
          .body("entry[0].transactionState", equalTo("RETURNED"))
          .body("entry[1].txId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
          .body("entry[1].transactionAmount.amount", equalTo(Integer.toString(negateExact(sendAmount))))
          .body("entry[1].cardholderFee.amount", equalTo(Integer.toString(fee)))
          .body("entry[1].transactionState", equalTo("COMPLETED"))
          .body("entry[1].transactionState", equalTo("COMPLETED"))
          .body("entry[1].actualBalanceAfter.amount", equalTo(Integer.toString(depositAmount - sendAmount - fee)))
          .body("entry[1].actualBalanceAdjustment.amount", equalTo(Integer.toString(negateExact(sendAmount + fee))));
    }
  }
}