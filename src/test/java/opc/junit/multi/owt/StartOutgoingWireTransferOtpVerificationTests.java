package opc.junit.multi.owt;

import opc.enums.mailhog.MailHogSms;
import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import opc.junit.database.AuthFactorsSimulatorDatabaseHelper;
import opc.junit.database.OwtDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.Optional;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(MultiTags.OWT)
public class StartOutgoingWireTransferOtpVerificationTests extends BaseOutgoingWireTransfersSetup {

    private static final String CHANNEL = EnrolmentChannel.SMS.name();
    private static final String VERIFICATION_CODE = "123456";

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateId;
    private static String consumerId;
    private static String corporateManagedAccountId;
    private static String consumerManagedAccountId;
    private static CreateCorporateModel createCorporateModel;
    private static CreateConsumerModel createConsumerModel;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void StartVerification_Corporate_Success(){

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_Consumer_Success(){

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_AuthenticatedUser_Success(){

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, user.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_CorporateSmsChecks_Success() throws SQLException {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        corporateManagedAccountId,
                        Currency.EUR.name(), 100L, OwtType.SEPA).build();

        final String id = sendOutgoingWireTransfer(outgoingWireTransfersModel, corporateAuthenticationToken);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", createCorporateModel.getRootUser().getMobile().getCountryCode(),
                createCorporateModel.getRootUser().getMobile().getNumber()), sms.getTo());
        assertEquals(String.format(MailHogSms.SCA_INITIATE_PAYMENT.getSmsText(), programmeName, outgoingWireTransfersModel.getTransferAmount().getCurrency(),
                TestHelper.convertToDecimal(outgoingWireTransfersModel.getTransferAmount().getAmount()),
                outgoingWireTransfersModel.getDestinationBeneficiary().getName(),
                AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(corporateId).get(0).get("token")), sms.getBody());
    }

    @Test
    public void StartVerification_ConsumerSmsChecks_Success() throws SQLException {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        consumerManagedAccountId,
                        Currency.EUR.name(), 100L, OwtType.SEPA).build();

        final String id = sendOutgoingWireTransfer(outgoingWireTransfersModel, consumerAuthenticationToken);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", createConsumerModel.getRootUser().getMobile().getCountryCode(),
                createConsumerModel.getRootUser().getMobile().getNumber()), sms.getTo());
        assertEquals(String.format(MailHogSms.SCA_INITIATE_PAYMENT.getSmsText(), programmeName, outgoingWireTransfersModel.getTransferAmount().getCurrency(),
                TestHelper.convertToDecimal(outgoingWireTransfersModel.getTransferAmount().getAmount()),
                outgoingWireTransfersModel.getDestinationBeneficiary().getName(),
                AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(consumerId).get(0).get("token")), sms.getBody());
    }

    @Test
    public void StartVerification_AuthenticatedUserSmsChecks_Success() throws SQLException {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setEmail(usersModel.getEmail()).setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, user.getRight());

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        Currency.EUR.name(), 100L, OwtType.SEPA).build();
        final String id = sendOutgoingWireTransfer(outgoingWireTransfersModel, user.getRight());

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(updateUser.getMobile().getNumber());
        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", updateUser.getMobile().getCountryCode(),
                updateUser.getMobile().getNumber()), sms.getTo());
        assertEquals(String.format(MailHogSms.SCA_INITIATE_PAYMENT.getSmsText(), programmeName, outgoingWireTransfersModel.getTransferAmount().getCurrency(),
                TestHelper.convertToDecimal(outgoingWireTransfersModel.getTransferAmount().getAmount()),
                outgoingWireTransfersModel.getDestinationBeneficiary().getName(),
                AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(user.getLeft()).get(0).get("token")), sms.getBody());
    }

    @Test
    public void StartVerification_RootStartUserVerification_Success(){

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, user.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_UserStartRootVerification_Success(){

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "INITIALISED", "PENDING_SCA", "EXPIRED_SCA", "VERIFIED_SCA" })
    public void StartVerification_PreSubmissionStateNotInPendingChallenge_Conflict(final String state) throws SQLException {

        final String finalState;

        switch (state){
            case "EXPIRED_SCA" :
                finalState = "FAILED";
                break;
            case "VERIFIED_SCA" :
                finalState = "SUBMITTED";
                break;
            default:
                finalState = "PENDING_CHALLENGE";
        }

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);

        OwtDatabaseHelper.updateOwtState(state, id);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(finalState));
    }

    @ParameterizedTest
    @ValueSource(strings = { "SUBMITTED", "APPROVED", "REJECTED", "FAILED", "COMPLETED" })
    public void StartVerification_PostSubmissionStateNotInPendingChallenge_Conflict(final String state) throws SQLException {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);

        OwtDatabaseHelper.updateOwtState(state, id);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(state));
    }

    @Test
    public void StartVerification_UserNotEnrolled_UserNotEnrolledOnChallenge() {

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build(), secretKey);

        final String managedAccountId = createFundedManagedAccount(consumerManagedAccountProfileId, consumer.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, consumer.getRight());

        OutgoingWireTransfersService
                .startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void StartVerification_UserEnrolmentNotVerified_UserNotEnrolledOnChallenge() {

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build(), secretKey);
        AuthenticationFactorsHelper.enrolOtpUser(EnrolmentChannel.SMS.name(), secretKey, consumer.getRight());

        final String managedAccountId = createFundedManagedAccount(consumerManagedAccountProfileId, consumer.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, consumer.getRight());

        OutgoingWireTransfersService
                .startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void StartVerification_CrossIdentity_NotFound(){

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_DifferentIdentity_NotFound(){

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build(), secretKey);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_CrossIdentityUser_NotFound(){

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, user.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_NoOwtId_NotFound(){

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification("", CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_EmptyOwtId_NotFound(){

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification("", CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_UnknownOwtId_NotFound(){

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(RandomStringUtils.randomNumeric(18), CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }
    
    @ParameterizedTest
    @DisplayName("StartVerification_UnknownChannel_BadRequest - DEV-6848 opened to return 404")
    @EnumSource(value = EnrolmentChannel.class, names = { "EMAIL", "UNKNOWN" })
    public void StartVerification_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);

        OutgoingWireTransfersService
                .startOutgoingWireTransferOtpVerification(id, enrolmentChannel.name(), secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_NoChannel_NotFound() {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);

        OutgoingWireTransfersService
                .startOutgoingWireTransferOtpVerification(id, "", secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_EmptyChannelValue_NotFound() {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);

        OutgoingWireTransfersService
                .startOutgoingWireTransferOtpVerification(id, "", secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String otherInnovatorSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);

        OutgoingWireTransfersService
                .startOutgoingWireTransferOtpVerification(id, CHANNEL, otherInnovatorSecretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_UserLoggedOut_Unauthorised() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        AuthenticationService.logout(secretKey, corporate.getRight());

        OutgoingWireTransfersService
                .startOutgoingWireTransferOtpVerification(RandomStringUtils.randomNumeric(18), CHANNEL, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartVerification_InvalidApiKey_Unauthorised() {

        OutgoingWireTransfersService
                .startOutgoingWireTransferOtpVerification(RandomStringUtils.randomNumeric(18), CHANNEL, "abc", corporateAuthenticationToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartVerification_NoApiKey_BadRequest() {

        OutgoingWireTransfersService
                .startOutgoingWireTransferOtpVerification(RandomStringUtils.randomNumeric(18), CHANNEL, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartVerification_BackofficeImpersonator_Forbidden() {

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_InsufficientFunds_Success(){

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                Currency.EUR.name(), secretKey, corporateAuthenticationToken);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        Currency.EUR.name(), 300000L, OwtType.SEPA).build();

        OutgoingWireTransfersService
                .sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT);

        final String id =
                OutgoingWireTransfersHelper.getOwtByAccountAndTag(secretKey, outgoingWireTransfersModel, corporateAuthenticationToken).getId();

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("INVALID"));
    }

    private String sendOutgoingWireTransfer(final String managedAccountId,
                                            final String token){

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        Currency.EUR.name(), 100L, OwtType.SEPA).build();

        return OutgoingWireTransfersService
                    .sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, token, Optional.empty())
                    .then()
                    .statusCode(SC_OK)
                    .extract()
                    .jsonPath()
                    .get("id");
    }

    private String sendOutgoingWireTransfer(final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                            final String token){
        return OutgoingWireTransfersService
                .sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private static void consumerSetup() {
        createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, consumerAuthenticationToken);

        consumerManagedAccountId = createFundedManagedAccount(consumerManagedAccountProfileId, consumerAuthenticationToken);
    }

    private static void corporateSetup() {
        createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationToken);

        corporateManagedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, corporateAuthenticationToken);
    }

    private static String createFundedManagedAccount(final String profile,
                                                     final String token) {
        final String managedAccountId =
                createManagedAccount(profile, Currency.EUR.name(), token)
                        .getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

        return managedAccountId;
    }
}
