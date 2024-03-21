package opc.junit.multi.owt;

import commons.enums.Currency;
import opc.enums.opc.*;
import opc.junit.database.OwtDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.OWT)
public class StartOutgoingWireTransferPushVerificationTests extends BaseOutgoingWireTransfersSetup {

    private static final String CHANNEL = EnrolmentChannel.AUTHY.name();

    private static String applicationOneCorporateAuthenticationToken;
    private static String applicationOneConsumerAuthenticationToken;
    private static String applicationOneConsumerId;
    private static String applicationOneCorporateManagedAccountId;
    private static String applicationOneConsumerManagedAccountId;

    @BeforeAll
    public static void Setup(){
        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

        AdminHelper.resetProgrammeAuthyLimitsCounter(programmeId, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(programmeId, resetCount, adminToken);

        corporateSetup();
        consumerSetup();
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

        final String id = sendOutgoingWireTransfer(applicationOneCorporateManagedAccountId, applicationOneCorporateAuthenticationToken);

        OwtDatabaseHelper.updateOwtState(state, id);

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo(finalState));
    }

    @ParameterizedTest
    @ValueSource(strings = { "SUBMITTED", "APPROVED", "REJECTED", "FAILED", "COMPLETED" })
    public void StartVerification_PostSubmissionStateNotInPendingChallenge_Conflict(final String state) throws SQLException {

        final String id = sendOutgoingWireTransfer(applicationOneCorporateManagedAccountId, applicationOneCorporateAuthenticationToken);

        OwtDatabaseHelper.updateOwtState(state, id);

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, applicationOneCorporateAuthenticationToken)
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
                .startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void StartVerification_CrossIdentity_NotFound(){

        final String id = sendOutgoingWireTransfer(applicationOneConsumerManagedAccountId, applicationOneConsumerAuthenticationToken);

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, applicationOneConsumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_DifferentIdentity_NotFound(){

        final String id = sendOutgoingWireTransfer(applicationOneCorporateManagedAccountId, applicationOneCorporateAuthenticationToken);

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build(), secretKey);

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_CrossIdentityUser_NotFound(){

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, applicationOneCorporateAuthenticationToken);
        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey, user.getRight());

        final String managedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, user.getRight());
        final String id = sendOutgoingWireTransfer(managedAccountId, user.getRight());

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey, applicationOneConsumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_NoOwtId_NotFound(){

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification("", CHANNEL, secretKey, applicationOneConsumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_EmptyOwtId_NotFound(){

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification("", CHANNEL, secretKey, applicationOneConsumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartVerification_UnknownOwtId_NotFound(){

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(RandomStringUtils.randomNumeric(18), CHANNEL, secretKey, applicationOneConsumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = { "EMAIL", "UNKNOWN" })
    public void StartVerification_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {

        final String id = sendOutgoingWireTransfer(applicationOneCorporateManagedAccountId, applicationOneCorporateAuthenticationToken);

        OutgoingWireTransfersService
                .startOutgoingWireTransferPushVerification(id, enrolmentChannel.name(), secretKey, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_NoChannel_NotFound() {

        final String id = sendOutgoingWireTransfer(applicationOneCorporateManagedAccountId, applicationOneCorporateAuthenticationToken);

        OutgoingWireTransfersService
                .startOutgoingWireTransferPushVerification(id, "", secretKey, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_EmptyChannelValue_NotFound() {

        final String id = sendOutgoingWireTransfer(applicationOneCorporateManagedAccountId, applicationOneCorporateAuthenticationToken);

        OutgoingWireTransfersService
                .startOutgoingWireTransferPushVerification(id, "", secretKey, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, applicationOneCorporateAuthenticationToken)
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

        final String id = sendOutgoingWireTransfer(applicationOneCorporateManagedAccountId, applicationOneCorporateAuthenticationToken);

        OutgoingWireTransfersService
                .startOutgoingWireTransferPushVerification(id, CHANNEL, otherInnovatorSecretKey, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void StartVerification_UserLoggedOut_Unauthorised() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());

        AuthenticationService.logout(secretKey, corporate.getRight());

        OutgoingWireTransfersService
                .startOutgoingWireTransferPushVerification(RandomStringUtils.randomNumeric(18), CHANNEL, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartVerification_InvalidApiKey_Unauthorised() {

        OutgoingWireTransfersService
                .startOutgoingWireTransferPushVerification(RandomStringUtils.randomNumeric(18), CHANNEL, "abc", applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartVerification_NoApiKey_BadRequest() {

        OutgoingWireTransfersService
                .startOutgoingWireTransferPushVerification(RandomStringUtils.randomNumeric(18), CHANNEL, "", applicationOneCorporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartVerification_BackofficeImpersonator_Forbidden() {

        final String id = sendOutgoingWireTransfer(applicationOneConsumerManagedAccountId, applicationOneConsumerAuthenticationToken);

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL, secretKey, getBackofficeImpersonateToken(applicationOneConsumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, id, applicationOneConsumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING_CHALLENGE"));
    }

    private String sendOutgoingWireTransfer(final String managedAccountId,
                                            final String token){

        return sendOutgoingWireTransfer(managedAccountId, token, secretKey, outgoingWireTransfersProfileId);
    }

    private String sendOutgoingWireTransfer(final String managedAccountId,
                                            final String token,
                                            final String secretKey,
                                            final String profileId){

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(profileId,
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

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        applicationOneConsumerId = authenticatedConsumer.getLeft();
        applicationOneConsumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, applicationOneConsumerId);

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(applicationOneConsumerId, secretKey, applicationOneConsumerAuthenticationToken);

        applicationOneConsumerManagedAccountId = createFundedManagedAccount(consumerManagedAccountProfileId, applicationOneConsumerAuthenticationToken);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        applicationOneCorporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateId, secretKey, applicationOneCorporateAuthenticationToken);

        applicationOneCorporateManagedAccountId = createFundedManagedAccount(corporateManagedAccountProfileId, applicationOneCorporateAuthenticationToken);
    }

    private static String createFundedManagedAccount(final String profile,
                                                     final String token) {
        return createFundedManagedAccount(profile, token, secretKey);
    }

    private static String createFundedManagedAccount(final String profile,
                                                     final String token,
                                                     final String secretKey) {
        final String managedAccountId =
                createManagedAccount(profile, Currency.EUR.name(), token, secretKey)
                        .getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

        return managedAccountId;
    }

    @AfterAll
    public static void resetLimits(){
        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

        AdminHelper.setProgrammeAuthyChallengeLimit(applicationOne.getProgrammeId(), resetCount, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(applicationFour.getProgrammeId(), resetCount, adminToken);
    }
}