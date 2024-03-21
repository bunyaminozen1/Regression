package opc.junit.multi.owt;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.junit.database.OwtDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.models.shared.VerificationModel;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.OWT)
public class VerifyOtpOutgoingWireTransferTests extends BaseOutgoingWireTransfersSetup {

    private static final String CHANNEL = EnrolmentChannel.SMS.name();
    private static final String VERIFICATION_CODE = "123456";

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String consumerId;
    private static String corporateManagedAccountId;
    private static String consumerManagedAccountId;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void VerifyTransfer_Corporate_Success(){

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(id, corporateAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void VerifyTransfer_Consumer_Success(){

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(id, consumerAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void VerifyTransfer_AuthenticatedUser_Success(){

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, user.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());
        startVerification(id, user.getRight());

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(id, user.getRight(), State.COMPLETED);
    }

    @Test
    public void VerifyTransfer_MultipleEnrolmentsAcceptedBySms_Success(){

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerId, secretKey, consumerAuthenticationToken);

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(id, consumerAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void VerifyTransfer_RootVerifyTransferStartedByUser_ChannelNotRegistered(){

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, user.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());
        startVerification(id, user.getRight());

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifyTransfer_UserVerifyTransferStartedByRoot_ChannelNotRegistered(){

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, user.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "SUBMITTED", "REJECTED", "APPROVED", "FAILED", "COMPLETED", "INITIALISED", "REQUIRES_SCA", "EXPIRED_SCA", "VERIFIED_SCA"})
    public void VerifyTransfer_StateNotInPendingVerification_Conflict(final String state) throws SQLException {

        final String finalState;

        switch (state){
            case "EXPIRED_SCA" :
                finalState = "FAILED";
                break;
            case "VERIFIED_SCA" :
                finalState = "SUBMITTED";
                break;
            case "INITIALISED" :
            case "REQUIRES_SCA" :
                finalState = "PENDING_CHALLENGE";
                break;
            default:
                finalState = state;
        }

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        OwtDatabaseHelper.updateOwtState(state, id);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(finalState));
    }

    @Test
    public void VerifyTransfer_DifferentIdentity_NotFound(){

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifyTransfer_CrossIdentity_NotFound(){

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build(),
                        secretKey);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, corporate.getRight())
                .then()
                 .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifyTransfer_DifferentIdentityUser_NotFound(){

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, user.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());
        startVerification(id, user.getRight());

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifyTransfer_NoOwtId_NotFound(){

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), "",
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void VerifyTransfer_EmptyOwtId_NotFound(){

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), "",
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void VerifyTransfer_UnknownOwtId_NotFound(){

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), RandomStringUtils.randomNumeric(18),
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = { "EMAIL", "UNKNOWN" })
    public void VerifyTransfer_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                enrolmentChannel.name(), secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    @DisplayName("VerifyTransfer_NoChannel_NotFound - DEV-6848 opened to return 404")
    public void VerifyTransfer_NoChannel_NotFound() {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                "", secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    @DisplayName("VerifyTransfer_EmptyChannelValue_NotFound - DEV-6848 opened to return 404")
    public void VerifyTransfer_EmptyChannelValue_NotFound() {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                "", secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifyTransfer_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String otherInnovatorSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, otherInnovatorSecretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifyTransfer_UserLoggedOut_Unauthorised() {

        final Pair<String, String> corporate =
                CorporatesHelper
                        .createAuthenticatedVerifiedCorporate(CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build(),
                                secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, corporate.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, corporate.getRight());
        startVerification(id, corporate.getRight());

        AuthenticationService.logout(secretKey, corporate.getRight());

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyTransfer_InvalidApiKey_Unauthorised() {

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), RandomStringUtils.randomNumeric(18),
                CHANNEL, "abc", corporateAuthenticationToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyTransfer_NoApiKey_BadRequest() {

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), RandomStringUtils.randomNumeric(18),
                CHANNEL, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyTransfer_BackofficeImpersonator_Forbidden() {

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), RandomStringUtils.randomNumeric(18),
                CHANNEL, secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifyTransfer_UnknownVerificationCode_VerificationCodeInvalid() {

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(RandomStringUtils.randomNumeric(6)), id,
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifyTransfer_InvalidVerificationCode_BadRequest() {

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(RandomStringUtils.randomAlphabetic(6)), id,
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: must match \"^[0-9]*$\""));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void VerifyTransfer_NoVerificationCode_BadRequest(final String verificationCode) {

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(verificationCode), id,
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void VerifyTransfer_ChallengeExpired_Expired() throws InterruptedException {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        Thread.sleep(61000);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                CHANNEL, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_EXPIRED"));

        assertOutgoingWireTransferState(id, corporateAuthenticationToken, State.PENDING_CHALLENGE);
    }

    @ParameterizedTest
    @ValueSource(strings = { "1234567", "12345" })
    public void VerifyTransfer_VerificationCodeLengthChecks_BadRequest(final String verificationCode) {

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(verificationCode), id,
                CHANNEL, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: size must be between 6 and 6"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
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

    private void startVerification(final String id,
                                   final String token) {

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
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
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
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

    private static void assertOutgoingWireTransferState(final String id, final String token, final State state){
        TestHelper.ensureAsExpected(120,
                () -> OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, token),
                x-> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
                Optional.of(String.format("Expecting 200 with an OWT in state %s, check logged payload", state.name())));
    }
}
