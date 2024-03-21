package opc.junit.multi.access;

import io.restassured.path.json.JsonPath;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.EnrolmentChannel;
import commons.enums.State;
import opc.enums.opc.UserType;
import opc.enums.opc.WebhookType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.users.UsersModel;
import opc.models.secure.BiometricPinModel;
import opc.models.secure.DetokenizeModel;
import opc.models.shared.Identity;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import opc.models.webhook.WebhookBiometricLoginEventModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.util.Optional;

import static opc.models.shared.LoginWithBiometricModel.loginWithBiometricModel;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Execution(ExecutionMode.CONCURRENT)
public abstract class AbstractLoginWithBiometricTests extends BaseAuthenticationSetup {

    final static String CHANNEL = EnrolmentChannel.BIOMETRIC.name();

    @Test
    public void LoginWithBiometric_RootUserIssueChallenge_Success() {
        AuthenticationService.loginWithBiometric(loginWithBiometricModel(getBiometricIdentity(passcodeApp).getEmail()), passcodeAppSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("challengeId", notNullValue());
    }

    @Test
    public void LoginWithBiometric_AuthenticatedUserIssueChallenge_Success() {
        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        UsersHelper.createEnrolledBiometricAuthenticatedUser(usersModel, passcodeAppSharedKey, passcodeAppSecretKey, identity.getToken());
        AuthenticationService.loginWithBiometric(loginWithBiometricModel(usersModel.getEmail()), passcodeAppSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("challengeId", notNullValue());
    }

    @Test
    public void LoginWithBiometric_RootUserAcceptChallenge_Success() {

        final IdentityDetails identity = getBiometricIdentity(passcodeApp);

        final String challengeId = getChallengeId(identity.getEmail());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptOkayLoginChallenge(passcodeAppSecretKey, challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertVerifiedChallenge(event, challengeId, identity.getId(), UserType.ROOT);
    }

    @Test
    public void LoginWithBiometric_AuthorizedUserAcceptChallenge_Success() {

        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createEnrolledBiometricAuthenticatedUser(usersModel, passcodeAppSharedKey, passcodeAppSecretKey, identity.getToken());

        final String challengeId = getChallengeId(usersModel.getEmail());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptOkayLoginChallenge(passcodeAppSecretKey, challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertVerifiedChallenge(event, challengeId, user.getLeft(), UserType.USER);
    }

    @Test
    public void LoginWithBiometric_RootUserEnterValidPin_Success() {

        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        final String challengeId = getChallengeId(identity.getEmail());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.enterPinOkayLoginChallenge(passcodeAppSecretKey, challengeId, new BiometricPinModel(TestHelper.getDefaultPassword(passcodeAppSecretKey)));

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertVerifiedChallenge(event, challengeId, identity.getId(), UserType.ROOT);
    }

    @Test
    public void LoginWithBiometric_AuthorizedUserEnterValidPin_Success() {

        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createEnrolledBiometricAuthenticatedUser(usersModel, passcodeAppSharedKey, passcodeAppSecretKey, identity.getToken());

        final String challengeId = getChallengeId(usersModel.getEmail());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.enterPinOkayLoginChallenge(passcodeAppSecretKey, challengeId, new BiometricPinModel(TestHelper.getDefaultPassword(passcodeAppSecretKey)));

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertVerifiedChallenge(event, challengeId, user.getLeft(), UserType.USER);
    }

    @Test
    public void LoginWithBiometric_RootUserRejectChallenge_Success() {

        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        final String challengeId = getChallengeId(identity.getEmail());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.rejectOkayLoginChallenge(passcodeAppSecretKey, challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertDeclinedChallenge(event, challengeId, identity.getId(), UserType.ROOT);
    }

    @Test
    public void LoginWithBiometric_AuthorizedUserRejectChallenge_Success() {

        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createEnrolledBiometricAuthenticatedUser(usersModel, passcodeAppSharedKey, passcodeAppSecretKey, identity.getToken());

        final String challengeId = getChallengeId(usersModel.getEmail());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.rejectOkayLoginChallenge(passcodeAppSecretKey, challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertDeclinedChallenge(event, challengeId, user.getLeft(), UserType.USER);
    }

    @Test
    public void LoginWithBiometric_RootUserEnterInvalidPin_Success() {

        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        final String challengeId = getChallengeId(identity.getEmail());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.enterPinOkayLoginChallenge(passcodeAppSecretKey, challengeId, new BiometricPinModel(RandomStringUtils.randomNumeric(4)));

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertDeclinedChallenge(event, challengeId, identity.getId(), UserType.ROOT);
    }

    @Test
    public void LoginWithBiometric_AuthorizedUserEnterInvalidPin_Success() {

        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createEnrolledBiometricAuthenticatedUser(usersModel, passcodeAppSharedKey, passcodeAppSecretKey, identity.getToken());

        final String challengeId = getChallengeId(usersModel.getEmail());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.enterPinOkayLoginChallenge(passcodeAppSecretKey, challengeId, new BiometricPinModel(RandomStringUtils.randomNumeric(4)));

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        assertDeclinedChallenge(event, challengeId, user.getLeft(), UserType.USER);
    }

    @Test
    public void LoginWithBiometric_FunctionDisabledForProgramme_ChannelNotSupported(){

        final IdentityDetails identity = getBiometricIdentity(scaApp);

        AuthenticationService.loginWithBiometric(loginWithBiometricModel(identity.getEmail()), scaApp.getSecretKey())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));
    }

    @Test
    public void LoginWithBiometric_UnknownLoginEmail_Forbidden(){
        AuthenticationService.loginWithBiometric(loginWithBiometricModel(String.format("%s@test.com", RandomStringUtils.randomAlphanumeric(5))), passcodeAppSecretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void LoginWithBiometric_WithoutSecretKey_ApiKeyRequired(){
        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        AuthenticationService.loginWithBiometric(loginWithBiometricModel(identity.getEmail()), "")
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void LoginWithBiometric_InvalidSecretKey_Unauthorized(){
        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        AuthenticationService.loginWithBiometric(loginWithBiometricModel(identity.getEmail()), RandomStringUtils.randomAlphanumeric(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void LoginWithBiometric_DifferentProgrammeSecretKey_Forbidden(){
        final IdentityDetails identity = getBiometricIdentity(passcodeApp);
        AuthenticationService.loginWithBiometric(loginWithBiometricModel(identity.getEmail()), applicationThree.getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void LoginWithBiometric_WithoutLoginEmail_Unauthorized(){
        AuthenticationService.loginWithBiometric(loginWithBiometricModel(""), passcodeAppSecretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[2].message", equalTo("request.email: must not be blank"));
    }

    @Test
    public void LoginWithBiometric_InvalidLoginEmail_Unauthorized(){
        AuthenticationService.loginWithBiometric(loginWithBiometricModel(RandomStringUtils.randomAlphanumeric(10)), passcodeAppSecretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.email: must be a well-formed email address"));
    }

    @Test
    public void LoginWithBiometric_BiometricNotSupportedProgrammeLevel_ChannelNotRegistered(){

        final IdentityDetails identity = getIdentity(applicationFour);
        AuthenticationService.loginWithBiometric(loginWithBiometricModel(identity.getEmail()), applicationFour.getSecretKey())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void LoginWithBiometric_BiometricNotSupportedProfileLevel_ChannelNotRegistered(){

        InnovatorHelper.enableOkay(applicationThree.getProgrammeId(), innovatorToken);

        final IdentityDetails identity = getIdentity(applicationThree);

        AuthenticationService.loginWithBiometric(loginWithBiometricModel(identity.getEmail()), applicationThree.getSecretKey())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        InnovatorHelper.disableOkay(applicationThree.getProgrammeId(), innovatorToken);
    }

    @Test
    public void LoginWithBiometric_UserNotEnrolledBiometric_ChannelNotRegistered(){

        final IdentityDetails identity = getIdentity(passcodeApp);

        AuthenticationService.loginWithBiometric(loginWithBiometricModel(identity.getEmail()), passcodeAppSecretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }

    @Test
    public void LoginWithBiometric_RootUserAcceptChallengeCheckStepup_Success() {

        final IdentityDetails identity = getBiometricIdentity(scaMaApp);

        final CreateManagedAccountModel managedAccountModel = getManagedAccountModel(scaMaApp);

        final String challengeId = getChallengeId(identity.getEmail(), scaMaApp.getSecretKey());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptOkayLoginChallenge(scaMaApp.getSecretKey(), challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        final String authToken = event.getAuthToken();

        ManagedAccountsService.createManagedAccount(managedAccountModel, scaMaApp.getSecretKey(), authToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("ACCESS_TOKEN_REQUIRED"));

        final String stepUpToken = AuthenticationHelper.requestAccessToken(
                new Identity(new IdentityModel(identity.getId(), identity.getIdentityType())),
                scaMaApp.getSecretKey(), authToken);

        ManagedAccountsService.createManagedAccount(managedAccountModel, scaMaApp.getSecretKey(), stepUpToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void LoginWithBiometric_RootUserAcceptChallengeUiComponents_Success() {

        final IdentityDetails identity = getBiometricIdentity(scaMcApp);
        final CreateManagedCardModel createManagedCardModel = getManagedCardModel(scaMcApp);

        final String challengeId = getChallengeId(identity.getEmail(), scaMcApp.getSecretKey());

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptOkayLoginChallenge(scaMcApp.getSecretKey(), challengeId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, identity.getId());

        final String authToken = event.getAuthToken();

        SecureService.associate(scaMcApp.getSharedKey(), authToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("ACCESS_TOKEN_REQUIRED"));

        final String stepUpToken = AuthenticationHelper.requestAccessToken(
                new Identity(new IdentityModel(identity.getId(), identity.getIdentityType())),
                scaMcApp.getSecretKey(), authToken);

        final String associateRandom = SecureService.associate(scaMcApp.getSharedKey(), stepUpToken, Optional.empty())
                .then().statusCode(SC_OK).extract().jsonPath().get("random");

        final JsonPath jsonPath =
                ManagedCardsService.createManagedCard(createManagedCardModel, scaMcApp.getSecretKey(), stepUpToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath();

        final DetokenizeModel.Builder cardNumberDetokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(jsonPath.get("cardNumber.value"))
                        .setRandom(associateRandom);

        final DetokenizeModel.Builder cvvDetokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(jsonPath.get("cvv.value"))
                        .setRandom(associateRandom);

        SecureService.detokenize(scaMcApp.getSharedKey(), stepUpToken, cardNumberDetokenizeModel.setRandom(associateRandom).build())
                .then()
                .statusCode(SC_OK)
                .body("value", notNullValue());

        SecureService.detokenize(scaMcApp.getSharedKey(), stepUpToken, cvvDetokenizeModel.setRandom(associateRandom).build())
                .then()
                .statusCode(SC_OK)
                .body("value", notNullValue());
    }

    protected String getChallengeId(final String email) {

        return getChallengeId(email, passcodeAppSecretKey);
    }

    protected String getChallengeId(final String email, final String secretKey) {

        return AuthenticationService.loginWithBiometric(loginWithBiometricModel(email), secretKey)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("challengeId");
    }

    protected WebhookBiometricLoginEventModel getWebhookResponse(final long timestamp,
                                                                 final String identityId) {

        return (WebhookBiometricLoginEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.STEP_UP,
                Pair.of("identity.id", identityId),
                WebhookBiometricLoginEventModel.class,
                ApiSchemaDefinition.StepupEvent);
    }

    protected void assertVerifiedChallenge(final WebhookBiometricLoginEventModel event,
                                           final String challengeId,
                                           final String identityId,
                                           final UserType userType) {

        assertNotNull(event.getAuthToken());
        assertEquals(challengeId, event.getChallengeId());
        assertEquals(identityId, event.getCredential().get("id"));
        assertEquals(userType.name(), event.getCredential().get("type"));
        assertEquals(State.VERIFIED.name(), event.getStatus());
        assertEquals(CHANNEL, event.getType());
    }

    protected void assertDeclinedChallenge(final WebhookBiometricLoginEventModel event,
                                           final String challengeId,
                                           final String identityId,
                                           final UserType userType) {

        assertNull(event.getAuthToken());
        assertEquals(challengeId, event.getChallengeId());
        assertEquals(identityId, event.getCredential().get("id"));
        assertEquals(userType.name(), event.getCredential().get("type"));
        assertEquals(State.DECLINED.name(), event.getStatus());
        assertEquals(CHANNEL, event.getType());
    }

    protected abstract IdentityDetails getBiometricIdentity(final ProgrammeDetailsModel programme);
    protected abstract IdentityDetails getIdentity(final ProgrammeDetailsModel programme);
    protected abstract CreateManagedAccountModel getManagedAccountModel(final ProgrammeDetailsModel programme);
    protected abstract CreateManagedCardModel getManagedCardModel(final ProgrammeDetailsModel programme);
}
