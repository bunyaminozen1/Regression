package opc.junit.multiprivate;

import commons.enums.Currency;
import commons.enums.State;
import commons.models.MobileNumberModel;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.FeeType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwtType;
import opc.enums.opc.ResourceType;
import opc.helpers.ChallengesModelHelper;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ChallengesHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.outgoingwiretransfers.Beneficiary;
import opc.models.multi.outgoingwiretransfers.CancelOutgoingWireTransferModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CancellationModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.VerificationModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multiprivate.MultiPrivateService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static opc.enums.opc.ManagedInstrumentType.UNKNOWN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SendOutgoingWireDeferredTransfersTests extends BaseMultiPrivateSetup {
    private static final String OTP_CHANNEL = EnrolmentChannel.SMS.name();
    private static final String PUSH_CHANNEL = EnrolmentChannel.AUTHY.name();
    private static final String VERIFICATION_CODE = "123456";
    private static String corporateAuthenticationTokenOtp;
    private static String consumerAuthenticationTokenOtp;
    private static String corporateCurrencyOtp;
    private static String consumerCurrencyOtp;
    private static String corporateAuthenticationTokenPush;

    @BeforeAll
    public static void Setup() {
        corporateSetupOtp();
        consumerSetupOtp();
        corporateSetupAuthy();
    }

    private static Stream<Arguments> owtSuccessfulArgs() {
        return Stream.of(Arguments.of(Currency.EUR, OwtType.SEPA, FeeType.SEPA_OWT_FEE),
                Arguments.of(Currency.GBP, OwtType.FASTER_PAYMENTS, FeeType.FASTER_PAYMENTS_OWT_FEE));
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void SendOwt_CorporateOtp_Success(final Currency currency, final OwtType type, final FeeType feeType) {

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final int fee = TestHelper.getFees(currency.name()).get(feeType).getAmount().intValue();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                currency.name(), sendAmount, type)
                        .setDescription(RandomStringUtils.randomAlphabetic(type.equals(OwtType.SEPA) ? 35 : 18))
                        .build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK);
        final String owtId = response.extract().jsonPath().getString("id");
        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, type, corporateAuthenticationTokenOtp,
                OTP_CHANNEL);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(owtId, corporateAuthenticationTokenOtp, State.COMPLETED);


        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationTokenOtp, currentBalance);
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void SendOwt_CorporateAuthy_Success(final Currency currency, final OwtType type, final FeeType feeType) {

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final int fee = TestHelper.getFees(currency.name()).get(feeType).getAmount().intValue();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenPush);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), sendAmount, type).build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenPush, Optional.empty())
                .then()
                .statusCode(SC_OK);
        final String owtId = response.extract().jsonPath().getString("id");

        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, type, corporateAuthenticationTokenPush,
                PUSH_CHANNEL);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenPush, owtId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(owtId, corporateAuthenticationTokenPush, State.COMPLETED);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationTokenPush, currentBalance);
    }

    @Test
    public void SendOwt_CorporateEurUnderNonFpsEnabledTenantOtp_Success() {

        final Currency currency = Currency.EUR;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsTenant.getCorporatesProfileId())
                        .setBaseCurrency(currency.name())
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsTenant.getSecretKey());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, OTP_CHANNEL, nonFpsTenant.getSecretKey(), corporate.getRight());

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final int fee = TestHelper.getFees(currency.name()).get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(nonFpsTenant.getCorporatePayneticsEeaManagedAccountsProfileId(), currency.name(),
                        corporate.getRight(), nonFpsTenant.getSecretKey());

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount, nonFpsTenant.getInnovatorId());

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(nonFpsTenant.getOwtProfileId(),
                        managedAccount.getLeft(),
                        currency.name(), sendAmount, OwtType.SEPA).build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, nonFpsTenant.getSecretKey(), corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
        final String owtId = response.extract().jsonPath().getString("id");

        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, OwtType.SEPA, corporate.getRight(), nonFpsTenant.getSecretKey(),
                OTP_CHANNEL);

        MultiPrivateService.retryDeferredOwt(nonFpsTenant.getSecretKey(), corporate.getRight(), owtId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(owtId, corporate.getRight(), State.COMPLETED, nonFpsTenant.getSecretKey());

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), corporate.getRight(), nonFpsTenant.getSecretKey(), currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_Consumer_Success(final Currency currency) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, currency.name(), consumerAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), sendAmount, OwtType.SEPA).build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, consumerAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK);
        final String owtId = response.extract().jsonPath().getString("id");
        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, OwtType.SEPA, consumerAuthenticationTokenOtp,
                OTP_CHANNEL);

        MultiPrivateService.retryDeferredOwt(secretKey, consumerAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(owtId, consumerAuthenticationTokenOtp, State.COMPLETED);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), consumerAuthenticationTokenOtp, currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_CorporateUser_Success(final Currency currency) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(UsersModel.DefaultUsersModel().build(), secretKey, corporateAuthenticationTokenOtp);

        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, OTP_CHANNEL, secretKey, user.getRight());

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), user.getRight());

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), sendAmount, OwtType.SEPA).build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
        final String owtId = response.extract().jsonPath().getString("id");
        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, OwtType.SEPA, user.getRight(),
                OTP_CHANNEL);

        MultiPrivateService.retryDeferredOwt(secretKey, user.getRight(), owtId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(owtId, user.getRight(), State.COMPLETED);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), user.getRight(), currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_RequiredOnly_Success(final Currency currency) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(currency.name(), sendAmount))
                        .setDescription(RandomStringUtils.randomAlphabetic(5)).build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK);
        final String owtId = response.extract().jsonPath().getString("id");

        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, OwtType.SEPA, corporateAuthenticationTokenOtp, OTP_CHANNEL);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(owtId, corporateAuthenticationTokenOtp, State.COMPLETED);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationTokenOtp, currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_BeneficiaryRequiredOnly_Success(final Currency currency) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                currency.name(), sendAmount, OwtType.SEPA)
                        .setDestinationBeneficiary(Beneficiary.builder()
                                .setName(RandomStringUtils.randomAlphabetic(5))
                                .setBankAccountDetails(new SepaBankDetailsModel("TS123123123213213123", "TS12TEST123"))
                                .build())
                        .build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK);
        final String owtId = response.extract().jsonPath().getString("id");
        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, OwtType.SEPA, corporateAuthenticationTokenOtp, OTP_CHANNEL);

        assertOutgoingWireTransferState(owtId, corporateAuthenticationTokenOtp, State.DEFERRED);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(owtId, corporateAuthenticationTokenOtp, State.COMPLETED);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationTokenOtp, currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"USD", "GBP"})
    public void SendOwt_SepaUnsupportedCurrency_Conflict(final Currency currency) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), 200L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_MISMATCH"));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"USD", "EUR"})
    public void SendOwt_FasterPaymentsUnsupportedCurrency_Conflict(final Currency currency) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), 200L, OwtType.FASTER_PAYMENTS).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_MISMATCH"));
    }

    @Test
    public void SendOwt_UnknownSourceInstrumentId_SourceNotFound() {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        RandomStringUtils.randomNumeric(18),
                        corporateCurrencyOtp, 10L, OwtType.SEPA).build();


        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_NoSourceInstrumentId_BadRequest(final String instrumentId) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        instrumentId,
                        corporateCurrencyOtp, 10L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_UnknownProfileIdProfileNotFound_Conflict() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(RandomStringUtils.randomNumeric(18),
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, 10L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROFILE_NOT_FOUND"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_NoProfileId_BadRequest(final String profileId) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(profileId,
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, 10L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_UnknownSourceInstrumentType_BadRequest() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                corporateCurrencyOtp, 10L, OwtType.SEPA)
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), UNKNOWN))
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_CrossIdentityCheck_SourceNotFound() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, 10L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, consumerAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void SendOwt_NoFunds_FundsInsufficient() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, 100L, OwtType.SEPA).build();

        final String owtId = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"))
                .extract().jsonPath().getString("id");

        final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owtId)),
                EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
                scaChallengeId, EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

    }

    @Test
    public void SendOwt_NotEnoughFunds_FundsInsufficient() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, 5000L, OwtType.SEPA).build();

        final String owtId = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"))
                .extract().jsonPath().getString("id");

        final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owtId)),
                EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
                scaChallengeId, EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void SendOwt_NotEnoughFundsForFee_FundsInsufficient() {

        final Long owtAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrencyOtp, owtAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, owtAmount, OwtType.SEPA).build();

        final String owtId = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"))
                .extract().jsonPath().getString("id");

        final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owtId)),
                EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
                scaChallengeId, EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void SendOwt_InvalidAmount_AmountInvalid() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, -100L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));
    }

    @Test
    public void SendOwt_SourceManagedAccountRemoved_SourceInstrumentDestroyed() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        ManagedAccountsHelper.removeManagedAccount(managedAccount.getLeft(), secretKey, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, 10L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_DESTROYED"));
    }

    @Test
    public void SendOwt_SourceManagedAccountBlocked_SourceInstrumentBlocked() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrencyOtp, 1000L);

        ManagedAccountsHelper.blockManagedAccount(managedAccount.getLeft(), secretKey, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, 100L, OwtType.SEPA).build();

        final String owtId = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"))
                .extract().jsonPath().getString("id");

        final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owtId)),
                EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
                scaChallengeId, EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void SendOwt_MissingBeneficiaryDetails_BadRequest() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(corporateCurrencyOtp, 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_MissingBankAccountIban_BadRequest(final String iban) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                corporateCurrencyOtp, 200L, OwtType.SEPA)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new SepaBankDetailsModel(iban, "TS12TEST123"))
                                .build())
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_MissingBankAccountBic_BadRequest(final String bic) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                corporateCurrencyOtp, 200L, OwtType.SEPA)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new SepaBankDetailsModel("TS123123123213213123", bic))
                                .build())
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_MissingBankAccountDetails_BadRequest() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary().build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(corporateCurrencyOtp, 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_MissingBeneficiaryName_BadRequest() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa()
                                .setName(null)
                                .build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(corporateCurrencyOtp, 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_MissingSourceInstrument_BadRequest() {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .setTransferAmount(new CurrencyAmount(corporateCurrencyOtp, 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_MissingTransferAmount_BadRequest() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_NoCurrency_BadRequest(final String currency) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(currency, 10L))
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_CorporateManagedAccountStatement_Success(final Currency currency) {
        final Long depositAmount = 10000L;
        final long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenOtp);

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(managedAccount.getLeft(),
                currency.name(),
                depositAmount,
                secretKey,
                corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), sendAmount, OwtType.SEPA).build();

        final ValidatableResponse response =
                MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                        .then()
                        .statusCode(SC_OK);

        final String outgoingWireTransferId = response.extract().jsonPath().getString("id");

        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, OwtType.SEPA, corporateAuthenticationTokenOtp, OTP_CHANNEL);

        final int currentBalance =
                preSendBalance - (int) sendAmount - fee;

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, outgoingWireTransferId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(outgoingWireTransferId, corporateAuthenticationTokenOtp, State.COMPLETED);

        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationTokenOtp, currentBalance);

        final SepaBankDetailsModel sepaBankDetails =
                (SepaBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary().getBankAccountDetails();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccount.getLeft(), secretKey, corporateAuthenticationTokenOtp, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", equalTo(outgoingWireTransferId))
                .body("entry[0].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
                .body("entry[0].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) (sendAmount))))
                .body("entry[0].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].balanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[0].cardholderFee.amount", equalTo(fee))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.description", equalTo(outgoingWireTransfersModel.getDescription()))
                .body("entry[0].additionalFields.beneficiaryName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
                .body("entry[0].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
                .body("entry[0].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].availableBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].actualBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) sendAmount + fee)))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].transactionId.id", equalTo(outgoingWireTransferId))
                .body("entry[1].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
                .body("entry[1].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) (sendAmount))))
                .body("entry[1].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].balanceAfter.amount", equalTo(currentBalance))
                .body("entry[1].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[1].cardholderFee.amount", equalTo(fee))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.description", equalTo(outgoingWireTransfersModel.getDescription()))
                .body("entry[1].additionalFields.beneficiaryName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
                .body("entry[1].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
                .body("entry[1].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
                .body("entry[1].availableBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].availableBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) sendAmount + fee)))
                .body("entry[1].actualBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].actualBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].entryState", equalTo(State.PENDING.name()))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[2].transactionAmount.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[2].balanceAfter.amount", equalTo(preSendBalance))
                .body("entry[2].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[2].cardholderFee.amount", equalTo(TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_ConsumerManagedAccountStatement_Success(final Currency currency) {
        final Long depositAmount = 10000L;
        final long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, currency.name(), consumerAuthenticationTokenOtp);

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(managedAccount.getLeft(),
                currency.name(),
                depositAmount,
                secretKey,
                consumerAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), sendAmount, OwtType.SEPA).build();

        final ValidatableResponse response =
                MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, consumerAuthenticationTokenOtp, Optional.empty())
                        .then()
                        .statusCode(SC_OK);

        final String outgoingWireTransferId = response.extract().jsonPath().getString("id");

        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, OwtType.SEPA, consumerAuthenticationTokenOtp, OTP_CHANNEL);

        MultiPrivateService.retryDeferredOwt(secretKey, consumerAuthenticationTokenOtp, outgoingWireTransferId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(outgoingWireTransferId, consumerAuthenticationTokenOtp, State.COMPLETED);

        final int currentBalance =
                preSendBalance - (int) sendAmount - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), consumerAuthenticationTokenOtp, currentBalance);

        final SepaBankDetailsModel sepaBankDetails =
                (SepaBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary().getBankAccountDetails();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccount.getLeft(), secretKey, consumerAuthenticationTokenOtp, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", equalTo(outgoingWireTransferId))
                .body("entry[0].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
                .body("entry[0].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) (sendAmount))))
                .body("entry[0].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].balanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[0].cardholderFee.amount", equalTo(fee))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.description", equalTo(outgoingWireTransfersModel.getDescription()))
                .body("entry[0].additionalFields.beneficiaryName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
                .body("entry[0].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
                .body("entry[0].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].availableBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].actualBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) sendAmount + fee)))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].transactionId.id", equalTo(outgoingWireTransferId))
                .body("entry[1].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
                .body("entry[1].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) (sendAmount))))
                .body("entry[1].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].balanceAfter.amount", equalTo(currentBalance))
                .body("entry[1].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[1].cardholderFee.amount", equalTo(fee))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.description", equalTo(outgoingWireTransfersModel.getDescription()))
                .body("entry[1].additionalFields.beneficiaryName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
                .body("entry[1].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
                .body("entry[1].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
                .body("entry[1].availableBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].availableBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) sendAmount + fee)))
                .body("entry[1].actualBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].actualBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].entryState", equalTo(State.PENDING.name()))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[2].transactionAmount.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[2].balanceAfter.amount", equalTo(preSendBalance))
                .body("entry[2].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[2].cardholderFee.amount", equalTo(TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void SendOwt_UnknownCurrency_CurrencyMismatch() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount("ABC", 10L))
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void SendOwt_InvalidCurrency_CurrencyMismatch() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount("ABCD", 10L))
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    public void SendOwt_InvalidAmount_BadRequest(final long amount) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(corporateCurrencyOtp, amount))
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));
    }

    @Test
    public void SendOwt_FasterPayments_Conflict() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrencyOtp, 1000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrencyOtp, 200L, OwtType.FASTER_PAYMENTS).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "MT121234567890", "MT121234567890123456789012345678901"})
    public void SendOwt_InvalidBankAccountIban_BadRequest(final String iban) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                corporateCurrencyOtp, 200L, OwtType.SEPA)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new SepaBankDetailsModel(iban, "TS12TEST123"))
                                .build())
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "MT12abcD1234"})
    public void SendOwt_InvalidBankAccountBic_BadRequest(final String bic) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                corporateCurrencyOtp, 200L, OwtType.SEPA)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new SepaBankDetailsModel("TS123123123213213123", bic))
                                .build())
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefgh", "1234567", "123456789"})
    public void SendOwt_InvalidBankAccountNumber_BadRequest(final String accountNumber) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.GBP.name(), corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.GBP.name(), 200L, OwtType.FASTER_PAYMENTS)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(accountNumber, RandomStringUtils.randomNumeric(6)))
                                .build())
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdef", "12345", "1234567"})
    public void SendOwt_InvalidBankAccountSortCode_BadRequest(final String sortCode) {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.GBP.name(), corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.GBP.name(), 200L, OwtType.FASTER_PAYMENTS)
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(RandomStringUtils.randomNumeric(8), sortCode))
                                .build())
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_ManagedCardAsSource_Conflict() {

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedCard.getLeft(),
                        corporateCurrencyOtp, 200L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void SendOwt_InvalidApiKey_Unauthorised() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrencyOtp, consumerAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        consumerCurrencyOtp, 100L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, "abc", consumerAuthenticationTokenOtp, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);

    }

    @Test
    public void SendOwt_NoApiKey_BadRequest() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrencyOtp, consumerAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        consumerCurrencyOtp, 100L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, "", consumerAuthenticationTokenOtp, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);

    }

    @Test
    public void SendOwt_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        consumerCurrencyOtp, 100L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendOwt_RootUserLoggedOut_Unauthorised() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        AuthenticationService.logout(secretKey, consumerAuthenticationTokenOtp)
                .then()
                .statusCode(SC_NO_CONTENT);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        consumerCurrencyOtp, 100L, OwtType.SEPA).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, consumerAuthenticationTokenOtp, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendOwt_SameIdempotencyRefSamePayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, OTP_CHANNEL, secretKey, authenticatedCorporate.getRight());

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());

        fundManagedAccount(managedAccount.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        createCorporateModel.getBaseCurrency(), sendAmount, OwtType.SEPA).build();

        responses.add(MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        responses.forEach(response ->
                assertSuccessfulResponse(response.then().statusCode(SC_OK), outgoingWireTransfersModel));

        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1",
                OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));

        assertManagedAccountBalance(managedAccount.getLeft(), authenticatedCorporate.getRight(), depositAmount.intValue());
    }

    @Test
    @DisplayName("SendOwt_SameIdempotencyRefDifferentPayload_PreconditionFailed - DEV-1614 opened to return 412")
    public void SendOwt_SameIdempotencyRefDifferentPayload_PreconditionFailed() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(),
                        authenticatedCorporate.getRight(), 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccounts.get(0).getLeft(),
                        createCorporateModel.getBaseCurrency(), sendAmount, OwtType.SEPA).build();

        final OutgoingWireTransfersModel outgoingWireTransfersModel1 =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccounts.get(0).getLeft(),
                                createCorporateModel.getBaseCurrency(), sendAmount, OwtType.SEPA)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK);

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel1,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_DifferentIdempotencyRef_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(),
                        authenticatedCorporate.getRight());

        fundManagedAccount(managedAccount.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        createCorporateModel.getBaseCurrency(), sendAmount, OwtType.SEPA).build();

        responses.add(MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference1)));
        responses.add(MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel,
                secretKey, authenticatedCorporate.getRight(), Optional.empty()));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK));

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(1).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(1).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3",
                OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(),
                        authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void SendOwt_InvalidDescriptionLengthSepa_BadRequest() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                corporateCurrencyOtp, 100L, OwtType.SEPA)
                        .setDescription(RandomStringUtils.randomAlphabetic(36))
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request: description size must be between 1 and 35"));
    }

    @Test
    public void SendOwt_InvalidDescriptionLengthFasterPayments_BadRequest() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencyOtp, corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                corporateCurrencyOtp, 100L, OwtType.FASTER_PAYMENTS)
                        .setDescription(RandomStringUtils.randomAlphabetic(19))
                        .build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request: description size must be between 0 and 18"));
    }

    @Test
    public void SendOwt_UnsupportedBeneficiaryNameCharactersFasterPayments_BadRequest() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.GBP.name(), corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                //Unsupported characters
                                .setName("đĐžŽšŠ")
                                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(
                                        ModelHelper.generateRandomValidAccountNumber(), ModelHelper.generateRandomValidSortCode())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(Currency.GBP.name(), 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request: name must match ^[a-zA-Z0-9\\/\\-?:().,’+\\s#=!\"%&*<>;{@\\r\\n]*$"));
    }

    @Test
    public void SendOwt_BlankBeneficiaryNameFasterPayments_BadRequest() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.GBP.name(), corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                //Blank Name
                                .setName("")
                                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(
                                        ModelHelper.generateRandomValidAccountNumber(), ModelHelper.generateRandomValidSortCode())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(Currency.GBP.name(), 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.destinationBeneficiary.name: must not be blank"))
                .body("_embedded.errors[1].message", equalTo("request: name size must be between 1 and 140, name must match ^[a-zA-Z0-9\\/\\-?:().,’+\\s#=!\"%&*<>;{@\\r\\n]*$"));
    }

    @Test
    public void SendOwt_NullBeneficiaryNameFasterPayments_BadRequest() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.GBP.name(), corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                //Null Name
                                .setName(null)
                                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(
                                        ModelHelper.generateRandomValidAccountNumber(), ModelHelper.generateRandomValidSortCode())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(Currency.GBP.name(), 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.destinationBeneficiary.name: must not be blank"));
    }

    @Test
    public void SendOwt_BeneficiaryNameAboveMaximumAllowedCharactersFasterPayments_BadRequest() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.GBP.name(), corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                //Name above maximum allowed characters
                                .setName(RandomStringUtils.randomAlphabetic(141))
                                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(
                                        ModelHelper.generateRandomValidAccountNumber(), ModelHelper.generateRandomValidSortCode())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(Currency.GBP.name(), 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request: name size must be between 1 and 140"));
    }

    @Test
    public void SendOwt_SupportedBeneficiaryNameCharactersFasterPayments_Success() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.GBP.name(), corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), Currency.GBP.name(), 10000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                //Supported characters
                                .setName("/s#=!%&*<>;{@")
                                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(
                                        ModelHelper.generateRandomValidAccountNumber(), ModelHelper.generateRandomValidSortCode())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(Currency.GBP.name(), 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendOwt_SupportedBeneficiaryNameSepaCharactersUnsupportedByFasterPayments_Success() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), 10000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                //Supported characters by Sepa. Unsupported by Faster Payments
                                .setName("đĐžŽšŠ")
                                .setBankAccountDetails(new SepaBankDetailsModel(
                                        ModelHelper.generateRandomValidIban(), ModelHelper.generateRandomValidBankIdentifierNumber())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(Currency.EUR.name(), 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendOwt_BlankBeneficiaryNameSepa_BadRequest() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), 10000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                //Blank Name
                                .setName("")
                                .setBankAccountDetails(new SepaBankDetailsModel(
                                        ModelHelper.generateRandomValidIban(), ModelHelper.generateRandomValidBankIdentifierNumber())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(Currency.EUR.name(), 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.destinationBeneficiary.name: must not be blank"))
                .body("_embedded.errors[1].message", equalTo("request: name size must be between 1 and 150"));
    }

    @Test
    public void SendOwt_NullBeneficiaryNameSepa_BadRequest() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), 10000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                //Null Name
                                .setName(null)
                                .setBankAccountDetails(new SepaBankDetailsModel(
                                        ModelHelper.generateRandomValidIban(), ModelHelper.generateRandomValidBankIdentifierNumber())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(Currency.EUR.name(), 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.destinationBeneficiary.name: must not be blank"));
    }

    @Test
    public void SendOwt_BeneficiaryNameAboveMaximumAllowedCharactersSepa_BadRequest() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), 10000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                //Name above maximum allowed characters
                                .setName(RandomStringUtils.randomAlphabetic(151))
                                .setBankAccountDetails(new SepaBankDetailsModel(
                                        ModelHelper.generateRandomValidIban(), ModelHelper.generateRandomValidBankIdentifierNumber())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(Currency.EUR.name(), 100L)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request: name size must be between 1 and 150"));
    }

    @Test
    public void SendOwt_SepaLimitCheck_BadRequest() {

        final String currency = Currency.EUR.name();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency, corporateAuthenticationTokenOtp);

        AdminHelper.fundManagedAccount(innovatorId, managedAccount.getLeft(), currency, 20000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                currency, 8001L, OwtType.SEPA)
                        .build();

        final String owtId = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"))
                .extract().jsonPath().getString("id");

        final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owtId)),
                EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
                scaChallengeId, EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));
    }

    @Test
    public void SendOwt_FasterPaymentsLimitCheck_BadRequest() {

        final String currency = Currency.GBP.name();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency, corporateAuthenticationTokenOtp);

        AdminHelper.fundManagedAccount(innovatorId, managedAccount.getLeft(), currency, 20000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                currency, 10001L, OwtType.FASTER_PAYMENTS)
                        .build();

        final String owtId = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"))
                .extract().jsonPath().getString("id");

        final String scaChallengeId = ChallengesHelper.issueOtpChallenges(ChallengesModelHelper.issueChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, List.of(owtId)),
                EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        ChallengesHelper.verifyOtpChallenges(ChallengesModelHelper
                        .verifyChallengesModel(ResourceType.OUTGOING_WIRE_TRANSFERS, VERIFICATION_CODE),
                scaChallengeId, EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationTokenOtp);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));

    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void SendOwt_LimitChecks_Success(final Currency currency, final OwtType type, final FeeType feeType) {

        final Long depositAmount = 20000L;
        final Long sendAmount = currency.equals(Currency.EUR) ? 8000L : 10000L;

        final int fee = TestHelper.getFees(currency.name()).get(feeType).getAmount().intValue();

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                currency.name(), sendAmount, type)
                        .build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK);
        final String owtId = response.extract().jsonPath().getString("id");
        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, type, corporateAuthenticationTokenOtp,
                OTP_CHANNEL);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(owtId, corporateAuthenticationTokenOtp, State.COMPLETED);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationTokenOtp, currentBalance);
    }

    @Test
    public void SendOwt_SubmittedUnknownStateToFailed_Success() throws InterruptedException {

        final String currency = Currency.EUR.name();
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency, corporateAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), currency, depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                currency, sendAmount, OwtType.SEPA)
                        .setDestinationBeneficiary(Beneficiary.builder()
                                .setName(RandomStringUtils.randomAlphabetic(5))
                                .setBankAccountDetails(new SepaBankDetailsModel("WV38WEAV502015555555", "TS12TEST123"))
                                .build())
                        .build();

        final String id =
                MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel,
                                secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("id");

        verifyOwtOtp(id, secretKey, corporateAuthenticationTokenOtp);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, id)
                .then()
                .statusCode(SC_OK);

        assertOutgoingWireTransferState(id, corporateAuthenticationTokenOtp, State.SUBMITTED);

        TimeUnit.SECONDS.sleep(2);

        AdminService.cancelOwt(id, AdminService.loginAdmin())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(id, corporateAuthenticationTokenOtp, State.FAILED);

    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_InvalidIban_BadRequest(final Currency currency) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, currency.name(), consumerAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new SepaBankDetailsModel(
                                        ModelHelper.generateRandomValidIban().toLowerCase(), ModelHelper.generateRandomValidBankIdentifierNumber())).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(currency.name(), sendAmount)).build();

        MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, consumerAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.destinationBeneficiary.bankAccountDetails.iban: must match \"^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$\""));
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void SendOwt_InsufficientFunds_Success(final Currency currency, final OwtType type, final FeeType feeType) {

        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenOtp);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccount.getLeft(),
                                currency.name(), sendAmount, type)
                        .setDescription(RandomStringUtils.randomAlphabetic(type.equals(OwtType.SEPA) ? 35 : 18))
                        .build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK);
        final String owtId = response.extract().jsonPath().getString("id");
        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, type, corporateAuthenticationTokenOtp,
                OTP_CHANNEL);

        MultiPrivateService.retryDeferredOwt(secretKey, corporateAuthenticationTokenOtp, owtId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

        assertOutgoingWireTransferState(owtId, corporateAuthenticationTokenOtp, State.FAILED);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_CancelOWT_Success (final Currency currency) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, currency.name(), consumerAuthenticationTokenOtp);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), sendAmount, OwtType.SEPA).build();

        final ValidatableResponse response = MultiPrivateService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, consumerAuthenticationTokenOtp, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final String owtId = response.extract().jsonPath().getString("id");

        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, OwtType.SEPA, consumerAuthenticationTokenOtp, OTP_CHANNEL);

        final CancellationModel cancellationModel = CancellationModel.builder()
                .id(owtId)
                .cancellationReason("TEST")
                .build();

        final CancelOutgoingWireTransferModel cancelOutgoingWireTransferModel = CancelOutgoingWireTransferModel.builder()
                .cancellations(List.of(cancellationModel))
                .build();

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, owtId, consumerAuthenticationTokenOtp)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("DEFERRED"));

        MultiPrivateService.cancelOWT(cancelOutgoingWireTransferModel, secretKey, consumerAuthenticationTokenOtp)
                .then()
                .statusCode(SC_OK)
                .body("cancellations[0].id", equalTo(owtId))
                .body("cancellations[0].state", equalTo("SUCCESS"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, owtId, consumerAuthenticationTokenOtp)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("CANCELLED"));
    }

    private void assertOutgoingWireTransferState(final String id, final String token, final State state) {
        assertOutgoingWireTransferState(id, token, state, secretKey);
    }

    private void assertOutgoingWireTransferState(final String id, final String token, final State state, final String secretKey) {
        TestHelper.ensureAsExpected(240,
                () -> OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
                Optional.of(String.format("Expecting 200 with an OWT in state %s, check logged payload", state.name())));
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final OutgoingWireTransfersModel outgoingWireTransfersModel) {
        response.body("id", notNullValue())
                .body("profileId", equalTo(outgoingWireTransfersModel.getProfileId()))
                .body("tag", equalTo(outgoingWireTransfersModel.getTag()))
                .body("sourceInstrument.type", equalTo(outgoingWireTransfersModel.getSourceInstrument().getType()))
                .body("sourceInstrument.id", equalTo(outgoingWireTransfersModel.getSourceInstrument().getId()))
                .body("transferAmount.currency", equalTo(outgoingWireTransfersModel.getTransferAmount().getCurrency()))
                .body("transferAmount.amount", equalTo(outgoingWireTransfersModel.getTransferAmount().getAmount().intValue()))
                .body("description", equalTo(outgoingWireTransfersModel.getDescription()))
                .body("destination.name", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
                .body("destination.address", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getAddress()))
                .body("destination.bankName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getBankName()))
                .body("destination.bankAddress", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getBankAddress()))
                .body("destination.bankCountry", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getBankCountry()))
                .body("state", equalTo("PENDING_CHALLENGE"))
                .body("creationTimestamp", notNullValue());
    }

    private void assertSuccessfulResponseAndVerify(final ValidatableResponse response,
                                                   final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                                   final OwtType owtType,
                                                   final String token,
                                                   final String enrolmentChannel) {
        assertSuccessfulResponseAndVerify(response, outgoingWireTransfersModel, owtType, token, secretKey, enrolmentChannel);
    }

    private void assertSuccessfulResponseAndVerify(final ValidatableResponse response,
                                                   final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                                   final OwtType owtType,
                                                   final String token,
                                                   final String secretKey,
                                                   final String enrolmentChannel) {
        assertSuccessfulResponse(response, outgoingWireTransfersModel);

        if (EnrolmentChannel.SMS.name().equals(enrolmentChannel)) {
            verifyOwtOtp(response.extract().jsonPath().getString("id"), secretKey, token);
        } else {
            verifyOwtPush(response.extract().jsonPath().getString("id"), token);
        }

        OutgoingWireTransfersHelper
                .checkOwtStateByAccountId(outgoingWireTransfersModel.getSourceInstrument().getId(), State.DEFERRED.name());

        assertBankAccountDetails(response, outgoingWireTransfersModel, owtType);
    }

    private void assertBankAccountDetails(final ValidatableResponse response,
                                          final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                          final OwtType owtType) {
        switch (owtType) {
            case SEPA:
                final SepaBankDetailsModel sepaBankDetails =
                        (SepaBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary().getBankAccountDetails();

                response
                        .body("type", equalTo(owtType.name()))
                        .body("destination.bankAccountDetails.iban", equalTo(sepaBankDetails.getIban()))
                        .body("destination.bankAccountDetails.bankIdentifierCode", equalTo(sepaBankDetails.getBankIdentifierCode()));
                break;
            case FASTER_PAYMENTS:
                final FasterPaymentsBankDetailsModel fasterPaymentsBankDetails =
                        (FasterPaymentsBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary().getBankAccountDetails();

                response
                        .body("type", equalTo(owtType.name()))
                        .body("destination.bankAccountDetails.accountNumber", equalTo(fasterPaymentsBankDetails.getAccountNumber()))
                        .body("destination.bankAccountDetails.sortCode", equalTo(fasterPaymentsBankDetails.getSortCode()));
                break;
            default:
                throw new IllegalArgumentException("OWT type not supported.");
        }
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String token, final int balance) {
        assertManagedAccountBalance(managedAccountId, token, secretKey, balance);
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String token, final String secretKey, final int balance) {
        ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));
    }

    private static void consumerSetupOtp() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationTokenOtp = authenticatedConsumer.getRight();
        consumerCurrencyOtp = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, OTP_CHANNEL, secretKey, consumerAuthenticationTokenOtp);
    }

    private static void corporateSetupOtp() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationTokenOtp = authenticatedCorporate.getRight();
        corporateCurrencyOtp = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, OTP_CHANNEL, secretKey, corporateAuthenticationTokenOtp);
    }

    private static void corporateSetupAuthy() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

        final String adminToken = AdminService.loginAdmin();
        AdminHelper.resetProgrammeAuthyLimitsCounter(applicationOne.getProgrammeId(), adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(applicationOne.getProgrammeId(), resetCount, adminToken);

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationTokenPush = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateId, secretKey, corporateAuthenticationTokenPush);
    }

    private void verifyOwtOtp(final String id,
                              final String secretKey,
                              final String token) {

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, OTP_CHANNEL, secretKey, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                        OTP_CHANNEL, secretKey, token),
                SC_NO_CONTENT);
    }

    private void verifyOwtPush(final String id,
                               final String token) {

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, PUSH_CHANNEL, secretKey, token),
                SC_NO_CONTENT);

        SimulatorHelper.acceptAuthyOwt(secretKey, id);
    }
}
