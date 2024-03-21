package opc.junit.secure.biometric;

import commons.enums.State;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthSessionsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.secure.EnrolBiometricModel;
import opc.models.secure.LoginWithPasswordModel;
import opc.models.secure.VerificationBiometricModel;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.secure.SecureService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractVerifyBiometricEnrolmentTests extends BaseBiometricSetup {

    @Test
    public void Biometric_EnrolRootUserVerified_Success() {

        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, getIdentityId(), linkingCode);

        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());
        verifyDeviceId(getIdentityId(), enrolBiometricModel.getDeviceId());
    }

    @Test
    public void Biometric_EnrolRootUserRejected_Success() {

        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorHelper.rejectOkayIdentity(secretKey, getIdentityId(), getIdentityToken(),
                State.INACTIVE);

        assertAuthFactorsState(getIdentityToken(), State.INACTIVE.name());
        verifyDeviceId(getIdentityId(), null);
    }

    @Test
    public void Biometric_EnrolAuthenticatedUserVerified_Success() {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey,
                getIdentityToken());

        final String random = getRandom(user.getRight());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(user.getRight(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, user.getLeft(), linkingCode);

        assertAuthFactorsState(user.getRight(), State.ACTIVE.name());
        verifyDeviceId(user.getLeft(), enrolBiometricModel.getDeviceId());
    }

    @Test
    public void Biometric_EnrolAuthenticatedUserRejected_Success() {

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey,
                getIdentityToken());

        final String random = getRandom(user.getRight());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        issueEnrolChallenge(user.getRight(), enrolBiometricModel);

        SimulatorHelper.rejectOkayIdentity(secretKey, user.getLeft(), user.getRight(), State.INACTIVE);

        AuthenticationFactorsService.getAuthenticationFactors(
                secretKey, Optional.empty(), user.getRight()).then();

        assertAuthFactorsState(user.getRight(), State.INACTIVE.name());
        verifyDeviceId(user.getLeft(), null);
    }

    @Test
    public void Biometric_EnrolRootUserRejectAfterVerify_Success() {

        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, getIdentityId(), linkingCode);

        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());

        SimulatorHelper.rejectOkayIdentity(secretKey, getIdentityId(), getIdentityToken(),
                State.INACTIVE);

        assertAuthFactorsState(getIdentityToken(), State.INACTIVE.name());
        verifyDeviceId(getIdentityId(), null);
    }

    @Test
    public void Biometric_EnrolRootUserUnlinkAfterVerify_Success() {

        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, getIdentityId(), linkingCode);

        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());

        SimulatorHelper.simulateEnrolmentUnlinking(secretKey, getIdentityId(), linkingCode);

        assertAuthFactorsState(getIdentityToken(), State.INACTIVE.name());
        verifyDeviceId(getIdentityId(), null);
    }

    @Test
    public void Biometric_UserEnrolNewDeviceNewUserEnrolTheOldOne_Success() {

        final String firstIdentityRandom = getRandom(getIdentityToken());

        final String firstDeviceId = RandomStringUtils.randomAlphanumeric(40);
        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(firstIdentityRandom)
                .deviceId(firstDeviceId)
                .build();

        final String firstLinkingCode = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, getIdentityId(), firstLinkingCode);

        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());
        verifyDeviceId(getIdentityId(), firstDeviceId);

        // Login with passcode to another device

        final String secondDeviceId = RandomStringUtils.randomAlphanumeric(40);

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                .setEmail(getIdentityEmail())
                .setDeviceId(secondDeviceId)
                .build();

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().getValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("tokenType", equalTo("ACCESS"));

        verifyDeviceId(getIdentityId(), firstDeviceId);
        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());

        //Enrol second device
        final EnrolBiometricModel enrolBiometricModel2 = EnrolBiometricModel.builder()
                .random(firstIdentityRandom)
                .deviceId(secondDeviceId)
                .build();

        final String linkingCode2 = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel2);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, getIdentityId(), linkingCode2);

        verifyDeviceId(getIdentityId(), secondDeviceId);

        // Create another identity and enroll first device
        final Triple<String, String, String> newIdentity = createNewIdentity();
        final String secondIdentityId = newIdentity.getLeft();
        final String secondIdentityToken = newIdentity.getRight();

        final String secondIdentityRandom = getRandom(secondIdentityToken);
        final EnrolBiometricModel secondEnrolBiometricModel = EnrolBiometricModel.builder()
                .random(secondIdentityRandom)
                .deviceId(firstDeviceId)
                .build();

        final String secondLinkingCode = SecureService.enrolDeviceBiometric(secondIdentityToken, sharedKey, secondEnrolBiometricModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("linkingCode");

        SimulatorHelper.simulateEnrolmentLinking(secretKey, secondIdentityId, secondLinkingCode);

        assertAuthFactorsState(secondIdentityToken, State.ACTIVE.name());
        verifyDeviceId(secondIdentityId, firstDeviceId);
    }

    @Test
    public void Biometric_SecondUserLoginWithFirstUserEnrolledDevice_FirstUserUnenrol() {

        final String random = getRandom(getIdentityToken());

        final String deviceId = RandomStringUtils.randomAlphanumeric(40);
        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(deviceId)
                .build();

        final String linkingCode = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, getIdentityId(), linkingCode);

        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());
        verifyDeviceId(getIdentityId(), deviceId);

        //un-enrol first identity
        AuthenticationFactorsHelper.unenrolBiometricPushUser(secretKey, getIdentityToken());

        // Another user login with user's enrolled device

        final Triple<String, String, String> newIdentity = createNewIdentity();

        final LoginWithPasswordModel loginWithPasswordModel = LoginWithPasswordModel.builder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                .setEmail(newIdentity.getMiddle())
                .setDeviceId(deviceId)
                .build();

        SecureService.loginWithPassword(sharedKey, loginWithPasswordModel)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("programmeId", equalTo(programmeId))
                .body("credential.id", equalTo(newIdentity.getLeft()))
                .body("identity.id", equalTo(newIdentity.getLeft()))
                .body("tokenType", equalTo("ACCESS"));

        //Factor should be inactive
        assertAuthFactorsState(getIdentityToken(), State.INACTIVE.name());
        verifyDeviceId(getIdentityId(), null);
    }

    @Test
    public void Biometric_VerifyEnrolmentWithoutProgrammeKey() {

        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorService.simulateEnrolmentLinking("", getIdentityId(), linkingCode)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].name", equalTo("programmeKey"))
                .body("validation.fields[0].errors[0].type", equalTo("REQUIRED"));
    }

    @Test
    public void Biometric_VerifyEnrolmentInvalidProgrammeKey_NotFound() {

        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorService.simulateEnrolmentLinking(RandomStringUtils.randomAlphanumeric(10),
                        getIdentityId(), linkingCode)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void Biometric_VerifyEnrolmentDifferentProgrammeKey_NotFound() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String newSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
                        .get("secretKey");

        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorService.simulateEnrolmentLinking(newSecretKey, getIdentityId(), linkingCode)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROGRAMME_NOT_FOUND"));
    }

    @Test
    public void Biometric_VerifyEnrolmentWithoutIdentityId_MethodNotAllowed() {
        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        final String linkingCode = issueEnrolChallenge(getIdentityToken(), enrolBiometricModel);

        SimulatorService.simulateEnrolmentLinking(secretKey, "", linkingCode)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void Biometric_VerifyEnrolmentWithoutLinkingCode_MethodNotAllowed() {

        SimulatorService.simulateEnrolmentLinking(secretKey, getIdentityId(), "")
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void Biometric_VerifyEnrolmentInvalidLinkingCode_UserNotFound() {

        SimulatorService.simulateEnrolmentLinking(secretKey, getIdentityId(),
                        RandomStringUtils.randomNumeric(10))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_NOT_FOUND"));
    }

    /**
     * DEV-6285 this story allow user request SMS OTP challenge for biometrics enrolment
     */

    @Test
    public void Biometric_VerifyEnrollmentRootUserOtpNotVerifiedEnrollmentWithOtp_Success() {

        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        SecureService.enrolDeviceBiometricWithOtp(getIdentityToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK);

        final VerificationBiometricModel verificationBiometricModel = VerificationBiometricModel.builder()
                .verificationCode(TestHelper.OTP_VERIFICATION_CODE)
                .random(enrolBiometricModel.getRandom()).build();

        final String linkingCode = SecureService.verifyBiometricEnrolmentWithOtp(getIdentityToken(), sharedKey,
                        verificationBiometricModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("linkingCode");

        SimulatorHelper.simulateEnrolmentLinking(secretKey, getIdentityId(), linkingCode);

        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());
        verifyDeviceId(getIdentityId(), enrolBiometricModel.getDeviceId());
    }

    @Test
    public void Biometric_VerifyEnrollmentRootUserOtpVerifiedEnrollmentWithOtp_Success() throws SQLException {

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, getIdentityToken());

        final String random = getRandom(getIdentityToken());

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        SecureService.enrolDeviceBiometricWithOtp(getIdentityToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK);

        final VerificationBiometricModel verificationBiometricModel = VerificationBiometricModel.builder()
                .verificationCode(TestHelper.OTP_VERIFICATION_CODE)
                .random(enrolBiometricModel.getRandom()).build();

        final String linkingCode = SecureService.verifyBiometricEnrolmentWithOtp(getIdentityToken(), sharedKey,
                        verificationBiometricModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("linkingCode");

        SimulatorHelper.simulateEnrolmentLinking(secretKey, getIdentityId(), linkingCode);

        assertAuthFactorsState(getIdentityToken(), State.ACTIVE.name());
        final Map<Integer, Map<String, String>> enrolledDeviceId = AuthSessionsDatabaseHelper.getCredentialFactors(getIdentityId());
        assertEquals(enrolledDeviceId.get(2).get("device_id"), enrolBiometricModel.getDeviceId());
    }

    /**
     * This method calls /app/secure/api/session/associate endpoint
     *
     * @param token (identity token)
     * @return a random number
     */
    protected String getRandom(final String token) {

        return SecureService.associate(sharedKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("random");
    }

    /**
     * This method calls /app/secure/api/session/authentication_factors/biometric/setup endpoint to
     * enrol a device
     *
     * @return linkingCode
     */

    protected String issueEnrolChallenge(final String token,
                                         final EnrolBiometricModel enrolBiometricModel) {
        return SecureService.enrolDeviceBiometric(token, sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("linkingCode");
    }

    /**
     * This method assert authentication factors status of an identity
     */
    protected void assertAuthFactorsState(final String identityToken,
                                        final String expectedStatus) {

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), identityToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("factors[0].status").equals(expectedStatus),
                Optional.of(String.format("Expecting 200 with an authentication factor in state %s, check logged payload", expectedStatus)));
    }

    /**
     * This method check if device id in DB matches with device id provided by user device
     */
    public static void verifyDeviceId(final String identityId, final String enrolledDeviceId) {

        TestHelper.ensureDatabaseResultAsExpected(10,
                () -> AuthSessionsDatabaseHelper.getCredentialFactors(identityId),
                x -> Objects.equals(x.get(1).get("device_id"), enrolledDeviceId),
                Optional.of(String.format("Retrieved device id does not match device with id %s as expected", enrolledDeviceId)));
    }

    protected abstract String getIdentityId();

    protected abstract String getIdentityToken();

    protected abstract IdentityType getIdentityType();

    protected abstract String getIdentityEmail();

    protected abstract Triple<String, String, String> createNewIdentity();
}
