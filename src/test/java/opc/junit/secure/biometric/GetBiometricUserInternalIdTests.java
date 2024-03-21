package opc.junit.secure.biometric;

import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.secure.DeviceIdModel;
import opc.models.secure.EnrolBiometricModel;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static opc.junit.secure.biometric.AbstractEnrolBiometricTests.verifyDeviceId;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GetBiometricUserInternalIdTests extends BaseBiometricSetup {

    /**
     * This class is about getting user internal id for the mobile SDK
     */

    private static String corporateAuthenticationToken;
    private static String corporateId;
    private static String consumerAuthenticationToken;
    private static String consumerId;

    @BeforeAll
    public static void setup() {
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void Biometric_GetConsumerUserInternalId_Success() {

        final String random = getRandom(consumerAuthenticationToken);

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();
        final DeviceIdModel deviceIdModel = DeviceIdModel.builder()
                .deviceId(enrolBiometricModel.getDeviceId()).build();

        final String linkingCode = issueEnrolChallenge(consumerAuthenticationToken, enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, consumerId, linkingCode);

        assertAuthFactorsState(consumerAuthenticationToken, State.ACTIVE.name());

        verifyDeviceId(consumerId, enrolBiometricModel.getDeviceId(), Optional.empty());

        final String extUserId = SecureService.getBiometricConfiguration(sharedKey, deviceIdModel)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("extUserId");

        assertNotNull(extUserId);
        assertNotEquals("", extUserId);
    }

    @Test
    public void Biometric_GetConsumerUserInternalIdDeviceNotEnrolled_Conflict() {

        final String random = getRandom(consumerAuthenticationToken);

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();
        final DeviceIdModel deviceIdModel = DeviceIdModel.builder()
                .deviceId(enrolBiometricModel.getDeviceId()).build();

        SecureService.getBiometricConfiguration(sharedKey, deviceIdModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DEVICE_NOT_ENROLLED"));
    }

    @Test
    public void Biometric_GetConsumerUserInternalIdRandomDeviceId_Conflict() {

        final DeviceIdModel deviceIdModel = DeviceIdModel.builder()
                .deviceId(RandomStringUtils.randomNumeric(10)).build();

        SecureService.getBiometricConfiguration(sharedKey, deviceIdModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DEVICE_NOT_ENROLLED"));
    }

    @Test
    public void Biometric_GetCorporateUserInternalId_Success() {

        final String random = getRandom(corporateAuthenticationToken);

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();
        final DeviceIdModel deviceIdModel = DeviceIdModel.builder()
                .deviceId(enrolBiometricModel.getDeviceId()).build();

        final String linkingCode = issueEnrolChallenge(corporateAuthenticationToken, enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, corporateId, linkingCode);

        assertAuthFactorsState(corporateAuthenticationToken, State.ACTIVE.name());

        verifyDeviceId(corporateId, enrolBiometricModel.getDeviceId(), Optional.empty());

        final String extUserId = SecureService.getBiometricConfiguration(sharedKey, deviceIdModel)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("extUserId");

        assertNotNull(extUserId);
        assertNotEquals("", extUserId);
    }

    @Test
    public void Biometric_GetCorporateUserInternalIdDeviceNotEnrolled_Conflict() {

        final String random = getRandom(corporateAuthenticationToken);

        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();
        final DeviceIdModel deviceIdModel = DeviceIdModel.builder()
                .deviceId(enrolBiometricModel.getDeviceId()).build();

        SecureService.getBiometricConfiguration(sharedKey, deviceIdModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DEVICE_NOT_ENROLLED"));
    }

    private void assertAuthFactorsState(final String identityToken,
                                        final String expectedStatus) {

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), identityToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("factors[0].status").equals(expectedStatus),
                Optional.of(String.format("Expecting 200 with an authentication factor in state %s, check logged payload", expectedStatus)));
    }

    private String getRandom(final String token) {

        return SecureService.associate(sharedKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("random");
    }

    private String issueEnrolChallenge(final String token,
                                       final EnrolBiometricModel enrolBiometricModel) {
        return SecureService.enrolDeviceBiometric(token, sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("linkingCode");
    }

    private static void consumerSetup() {

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(
                consumerProfileId, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
    }

    private static void corporateSetup() {

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
                corporateProfileId, secretKey);

        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateId = authenticatedCorporate.getLeft();
    }
}
