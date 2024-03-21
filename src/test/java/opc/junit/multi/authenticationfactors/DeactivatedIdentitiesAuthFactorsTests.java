package opc.junit.multi.authenticationfactors;

import io.restassured.path.json.JsonPath;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityDeactivateReasons;
import opc.enums.opc.LimitInterval;
import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.SwitchFunctionModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static opc.junit.helpers.TestHelper.OTP_VERIFICATION_CODE;
import static opc.models.shared.LoginWithBiometricModel.loginWithBiometricModel;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class DeactivatedIdentitiesAuthFactorsTests extends BaseAuthenticationFactorsSetup{

    @BeforeAll
    public static void setup(){
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);
        AdminHelper.setProgrammeAuthyChallengeLimit(passcodeApp.getProgrammeId(), resetCount, impersonatedAdminToken);

        AdminService.setMultiBiometricLoginFunction(new SwitchFunctionModel(true), impersonatedAdminToken, passcodeApp.getProgrammeId());
    }

    @Test
    public void DeactivateCorporate_TemporaryReasonCodeRootUserEnrollmentRemainsActive_Success() throws InterruptedException {
        //Create a corporate that enrolled all factor types
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledAllFactorsVerifiedCorporate
                (createCorporateModel, passcodeAppSecretKey, passcodeAppSharedKey);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(corporate.getRight(), State.ACTIVE.name());

        //Create an authorized user that enrolled all factor types
        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAllFactorsUser(userModel, corporate.getRight(), passcodeAppSecretKey, passcodeAppSharedKey);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(user.getRight(), State.ACTIVE.name());

        //Deactivate corporate with the reason "TEMPORARY"
        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(false, "TEMPORARY"),
                corporate.getLeft(), innovatorToken);

        //Check root user's auth factors, all of them should be active because of TEMPORARY reason code
        checkDeactivatedCorporateUserAuthFactors(corporate.getLeft(), corporate.getLeft(), State.ACTIVE.name());

        //Deactivate authorized user
        AdminHelper.deactivateCorporateUser(corporate.getLeft(), user.getLeft(), impersonatedAdminToken);

        //Check authorized user's auth factors, all of them should be inactive
        checkDeactivatedCorporateUserAuthFactors(corporate.getLeft(), user.getLeft(), State.INACTIVE.name());

        //Activate corporate
        AdminHelper.activateCorporate(new ActivateIdentityModel(false), corporate.getLeft(), impersonatedAdminToken);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(corporate.getRight(), State.ACTIVE.name());

        //Root user attempt a step-up session via authy
        final String sessionId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), passcodeAppSecretKey, corporate.getRight());
        SimulatorHelper.acceptAuthyStepUp(passcodeAppSecretKey, sessionId);

        //Root user attempt a login challenge via biometric
        final String challengeId = AuthenticationHelper.loginWithBiometric(loginWithBiometricModel(createCorporateModel.getRootUser().getEmail()), passcodeAppSecretKey);
        SimulatorHelper.acceptOkayLoginChallenge(passcodeAppSecretKey, challengeId);

        //Root user attempt a step-up session via otp
        final String token = AuthenticationHelper.login(createCorporateModel.getRootUser().getEmail(), passcodeAppSecretKey);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, token);

        //Activate authorized user
        AdminHelper.activateCorporateUser(corporate.getLeft(), user.getLeft(), impersonatedAdminToken);

        //Authorized user attempt auth session challenges
        checkFailedAuthFactorChallenges(userModel.getEmail(), user.getRight());
    }

    @ParameterizedTest
    @EnumSource(value = IdentityDeactivateReasons.class, mode = EnumSource.Mode.EXCLUDE, names = { "TEMPORARY"})
    public void DeactivateCorporate_ReasonCodeRootUserEnrollmentRemainsActive_Success(final IdentityDeactivateReasons reasonCode) throws InterruptedException {

        //Create a corporate that enrolled all factor types
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledAllFactorsVerifiedCorporate
                (createCorporateModel, passcodeAppSecretKey, passcodeAppSharedKey);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(corporate.getRight(), State.ACTIVE.name());

        //Create an authorized user that enrolled all factor types
        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAllFactorsUser(userModel, corporate.getRight(), passcodeAppSecretKey, passcodeAppSharedKey);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(user.getRight(), State.ACTIVE.name());

        //Deactivate corporate with the reason different from "TEMPORARY"
        AdminHelper.deactivateCorporate(new DeactivateIdentityModel(false, reasonCode.name()), corporate.getLeft(), impersonatedAdminToken);

        //Check root user's auth factors, all of them should be inactive because the reason code
        checkDeactivatedCorporateUserAuthFactors(corporate.getLeft(), corporate.getLeft(), State.INACTIVE.name());

        //Deactivate authorized user
        AdminHelper.deactivateCorporateUser(corporate.getLeft(), user.getLeft(), impersonatedAdminToken);

        //Check authorized user's auth factors, all of them should be inactive
        checkDeactivatedCorporateUserAuthFactors(corporate.getLeft(), user.getLeft(), State.INACTIVE.name());

        //Activate corporate
        AdminHelper.activateCorporate(new ActivateIdentityModel(false), corporate.getLeft(), impersonatedAdminToken);

        //Check auth factors, all of them should be inactive
        checkAuthenticationFactors(corporate.getRight(), State.INACTIVE.name());

        //Root user attempt auth session challenges, all of them should be failed
        checkFailedAuthFactorChallenges(createCorporateModel.getRootUser().getEmail(), corporate.getRight());

        //Activate authorized user
        AdminHelper.activateCorporateUser(corporate.getLeft(), user.getLeft(), impersonatedAdminToken);

        //Authorized user attempt auth session challenges, all of them should be failed
        checkFailedAuthFactorChallenges(userModel.getEmail(), user.getRight());

        //Root user enrol biometric again
        SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey, corporate.getRight());

        //Root user attempt a step-up session via authy
        final String challengeId = AuthenticationHelper.loginWithBiometric(loginWithBiometricModel(createCorporateModel.getRootUser().getEmail()), passcodeAppSecretKey);
        SimulatorHelper.acceptOkayLoginChallenge(passcodeAppSecretKey, challengeId);

    }

    @Test
    public void DeactivateConsumer_TemporaryReasonCodeRootUserEnrollmentRemainsActive_Success() throws InterruptedException {

        //Create a consumer that enrolled all factor types
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledAllFactorsVerifiedConsumer(
                createConsumerModel, passcodeAppSecretKey, passcodeAppSharedKey);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(consumer.getRight(), State.ACTIVE.name());

        //Create an authorized user that enrolled all factor types
        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAllFactorsUser(userModel, consumer.getRight(), passcodeAppSecretKey, passcodeAppSharedKey);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(user.getRight(), State.ACTIVE.name());

        //Deactivate consumer with the reason "TEMPORARY"
        AdminHelper.deactivateConsumer(new DeactivateIdentityModel(false, IdentityDeactivateReasons.TEMPORARY.name()),
                consumer.getLeft(), impersonatedAdminToken);

        //Check root user's auth factors, all of them should be active because of TEMPORARY reason code
        checkDeactivatedConsumerUserAuthFactors(consumer.getLeft(), consumer.getLeft(), State.ACTIVE.name());

        //Deactivate authorized user
        AdminHelper.deactivateConsumerUser(consumer.getLeft(), user.getLeft(), impersonatedAdminToken);

        //Check authorized user's auth factors, all of them should be inactive
        checkDeactivatedConsumerUserAuthFactors(consumer.getLeft(), user.getLeft(), State.INACTIVE.name());

        //Activate consumer
        AdminHelper.activateConsumer(new ActivateIdentityModel(false), consumer.getLeft(), impersonatedAdminToken);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(consumer.getRight(), State.ACTIVE.name());

        //Root user attempt a step-up session via authy
        final String sessionId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), passcodeAppSecretKey, consumer.getRight());
        SimulatorHelper.acceptAuthyStepUp(passcodeAppSecretKey, sessionId);

        //Root user attempt a login challenge via biometric
        final String challengeId = AuthenticationHelper.loginWithBiometric(loginWithBiometricModel(createConsumerModel.getRootUser().getEmail()), passcodeAppSecretKey);
        SimulatorHelper.acceptOkayLoginChallenge(passcodeAppSecretKey, challengeId);

        //Root user attempt a step-up session via otp
        final String token = AuthenticationHelper.login(createConsumerModel.getRootUser().getEmail(), passcodeAppSecretKey);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, token);

        //Activate authorized user
        AdminHelper.activateConsumerUser(consumer.getLeft(), user.getLeft(), impersonatedAdminToken);

        //Authorized user attempt auth session challenges
        checkFailedAuthFactorChallenges(userModel.getEmail(), user.getRight());
    }

    @ParameterizedTest
    @EnumSource(value = IdentityDeactivateReasons.class, mode = EnumSource.Mode.EXCLUDE, names = { "TEMPORARY"})
    public void DeactivateConsumer_ReasonCodeRootUserEnrollmentRemainsActive_Success(final IdentityDeactivateReasons reasonCode) throws InterruptedException {

        //Create a consumer that enrolled all factor types
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledAllFactorsVerifiedConsumer(
                createConsumerModel, passcodeAppSecretKey, passcodeAppSharedKey);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(consumer.getRight(), State.ACTIVE.name());

        //Create an authorized user that enrolled all factor types
        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAllFactorsUser(userModel, consumer.getRight(), passcodeAppSecretKey, passcodeAppSharedKey);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(user.getRight(), State.ACTIVE.name());

        //Deactivate consumer with a reason different from "TEMPORARY"
        InnovatorHelper.deactivateConsumer(new DeactivateIdentityModel(false, reasonCode.name()),
                consumer.getLeft(), innovatorToken);

        //Check root user's auth factors, all of them should be inactive because the reason code
        checkDeactivatedConsumerUserAuthFactors(consumer.getLeft(), consumer.getLeft(), State.INACTIVE.name());

        //Deactivate authorized user
        AdminHelper.deactivateConsumerUser(consumer.getLeft(), user.getLeft(), impersonatedAdminToken);

        //Check authorized user's auth factors, all of them should be inactive
        checkDeactivatedConsumerUserAuthFactors(consumer.getLeft(), user.getLeft(), State.INACTIVE.name());

        //Activate consumer
        AdminHelper.activateConsumer(new ActivateIdentityModel(false), consumer.getLeft(), impersonatedAdminToken);

        //Check auth factors, all of them should be active
        checkAuthenticationFactors(consumer.getRight(), State.INACTIVE.name());

        //Root user attempt auth session challenges, all of them should be failed
        checkFailedAuthFactorChallenges(createConsumerModel.getRootUser().getEmail(), consumer.getRight());

        //Activate authorized user
        AdminHelper.activateConsumerUser(consumer.getLeft(), user.getLeft(), impersonatedAdminToken);

        //Authorized user attempt auth session challenges
        checkFailedAuthFactorChallenges(userModel.getEmail(), user.getRight());

        //Root user enrol biometric again
        SecureHelper.enrolAndVerifyBiometric(consumer.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey, consumer.getRight());

        final String challengeId = AuthenticationHelper.loginWithBiometric(loginWithBiometricModel(createConsumerModel.getRootUser().getEmail()), passcodeAppSecretKey);
        SimulatorHelper.acceptOkayLoginChallenge(passcodeAppSecretKey, challengeId);
    }

    private void checkAuthenticationFactors(final String identityToken,
                                            final String status){

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), identityToken), SC_OK)
                .then()
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo(status))
                .body("factors[1].channel", equalTo("AUTHY"))
                .body("factors[1].status", equalTo(status))
                .body("factors[2].channel", equalTo("BIOMETRIC"))
                .body("factors[2].status", equalTo(status));
    }

    private void checkDeactivatedConsumerUserAuthFactors(final String identityId,
                                                         final String userId,
                                                         final String status){

        final JsonPath response = TestHelper.ensureAsExpected(15,
                        () -> AdminService.getConsumerUser(impersonatedAdminToken, identityId, userId),
                        SC_OK).then().extract().jsonPath();

        final Map<String, String> factors = new HashMap<>();

        for (int i = 0; i <=3; i++) {
            factors.put(response.getString(String.format("credentials.credential[0].factors[%s].type", i)),
                        response.getString(String.format("credentials.credential[0].factors[%s].status", i)));
        }

        for (String s : factors.keySet()) {
            if (!s.equals("PASSWORD")){
                Assertions.assertEquals(status, factors.get(s));
            }
        }
    }

    private void checkDeactivatedCorporateUserAuthFactors(final String identityId,
                                                          final String userId,
                                                          final String status){
        final JsonPath response = TestHelper.ensureAsExpected(15,
                () -> AdminService.getCorporateUser(impersonatedAdminToken, identityId, userId),
                SC_OK).then().extract().jsonPath();

        final Map<String, String> factors = new HashMap<>();

        for (int i = 0; i <=3; i++) {
            factors.put(response.getString(String.format("credentials.credential[0].factors[%s].type", i)),
                    response.getString(String.format("credentials.credential[0].factors[%s].status", i)));
        }

        for (String s : factors.keySet()) {
            if (!s.equals("PASSWORD")){
                Assertions.assertEquals(status, factors.get(s));
            }
        }
    }

    private void checkFailedAuthFactorChallenges(final String email,
                                                 final String identityToken) throws InterruptedException {
        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), passcodeAppSecretKey, identityToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        AuthenticationService.loginWithBiometric(loginWithBiometricModel(email), passcodeAppSecretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));

        // We have to wait 15 seconds after changes from DEV-5477.
        TimeUnit.SECONDS.sleep(15);

        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), passcodeAppSecretKey, identityToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_REGISTERED"));
    }
}
