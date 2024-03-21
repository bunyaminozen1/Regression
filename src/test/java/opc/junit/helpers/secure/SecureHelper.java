package opc.junit.helpers.secure;

import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.secure.AdditionalPropertiesModel;
import opc.models.secure.DetokenizeModel;
import opc.models.secure.EnrolBiometricModel;
import opc.models.secure.LoginBiometricModel;
import opc.models.secure.LoginWithPasswordModel;
import opc.models.secure.TokenizeModel;
import opc.models.secure.TokenizePropertiesModel;
import opc.models.secure.VerificationBiometricModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.secure.SecureService;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;

public class SecureHelper {

  public static void enrolAndVerifyBiometric(final String identityId,
      final String sharedKey,
      final String secretKey,
      final String token) {
    final String linkingCode = enrolBiometricUser(token, sharedKey);
    SimulatorHelper.simulateEnrolmentLinking(secretKey, identityId, linkingCode);
  }

  public static String enrolBiometricUser(final String authenticationToken,
      final String sharedKey) {

    final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
        .random(associate(authenticationToken, sharedKey))
        .deviceId(RandomStringUtils.randomAlphanumeric(40))
        .build();

    return TestHelper.ensureAsExpected(15,
        () -> SecureService.enrolDeviceBiometric(authenticationToken, sharedKey,
            enrolBiometricModel),
        SC_OK).jsonPath().getString("linkingCode");

  }

  public static String associate(final String authenticationToken, final String sharedKey) {

    return TestHelper.ensureAsExpected(15,
        () -> SecureService.associate(sharedKey, authenticationToken, Optional.empty()),
        SC_OK).jsonPath().getString("random");
  }

  public static void enrolAndRejectOkayPush(final String identityId,
      final String sharedKey,
      final String secretKey,
      final String token) {
    enrolBiometricUser(token, sharedKey);
    rejectOkayPushEnrolment(identityId, secretKey, token);
  }

  public static void rejectOkayPushEnrolment(final String identityId,
      final String secretKey,
      final String token) {
    SimulatorHelper.rejectOkayIdentity(secretKey, identityId, token, State.INACTIVE);
  }

  public static String loginViaBiometric(final String sharedKey, final String deviceId) {

    return TestHelper.ensureAsExpected(15,
            () -> SecureService.loginViaBiometric(sharedKey, new LoginBiometricModel(deviceId)),
            SC_OK).jsonPath().getString("challengeId");
  }

  public static String loginWithPassword(final String sharedKey, final LoginWithPasswordModel loginWithPasswordModel) {

    return TestHelper.ensureAsExpected(15,
            () -> SecureService.loginWithPassword(sharedKey, loginWithPasswordModel),
            SC_OK).jsonPath().getString("token");
  }

  public static String enrolAndGetDeviceId(final String authenticationToken,
                                           final String identityId,
                                           final ProgrammeDetailsModel programme) {

      final String random = SecureHelper.associate(authenticationToken, programme.getSharedKey());
      final String deviceId = RandomStringUtils.randomAlphanumeric(40);

      final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
              .random(random)
              .deviceId(deviceId)
              .build();

      final String linking_code = SecureService.enrolDeviceBiometric(authenticationToken, programme.getSharedKey(),
                      enrolBiometricModel)
              .jsonPath().getString("linkingCode");

      SimulatorHelper.simulateEnrolmentLinking(programme.getSecretKey(), identityId, linking_code);

      TestHelper.ensureAsExpected(10,
              () -> AuthenticationFactorsService.getAuthenticationFactors(programme.getSecretKey(), Optional.empty(), authenticationToken),
              x -> x.statusCode() == SC_OK && x.jsonPath().getString("factors[0].status").equals("ACTIVE"),
              Optional.of(String.format("Enrolment for identity %s not in ACTIVE state as expected, check logged payloads", identityId)));

      return deviceId;
    }

  public static String enrolBiometricWithOtpAndGetDeviceId(final String authenticationToken,
                                                           final String identityId,
                                                           final ProgrammeDetailsModel programme) {

    final String random = SecureHelper.associate(authenticationToken, programme.getSharedKey());
    final String deviceId = RandomStringUtils.randomAlphanumeric(40);

    final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
            .random(random)
            .deviceId(deviceId)
            .build();

    SecureService.enrolDeviceBiometricWithOtp(authenticationToken, programme.getSharedKey(), enrolBiometricModel)
            .then()
            .statusCode(SC_OK);

    final VerificationBiometricModel verificationBiometricModel = VerificationBiometricModel.builder()
            .verificationCode(TestHelper.OTP_VERIFICATION_CODE)
            .random(enrolBiometricModel.getRandom()).build();

    final String linkingCode = SecureService.verifyBiometricEnrolmentWithOtp(authenticationToken, programme.getSharedKey(),
                    verificationBiometricModel)
            .then()
            .statusCode(SC_OK)
            .extract()
            .jsonPath()
            .getString("linkingCode");

    SimulatorHelper.simulateEnrolmentLinking(programme.getSecretKey(), identityId, linkingCode);

    TestHelper.ensureAsExpected(10,
            () -> AuthenticationFactorsService.getAuthenticationFactors(programme.getSecretKey(), Optional.empty(), authenticationToken),
            x -> x.statusCode() == SC_OK && x.jsonPath().getString("factors[0].status").equals("ACTIVE"),
            Optional.of(String.format("Enrolment for identity %s not in ACTIVE state as expected, check logged payloads", identityId)));

    return deviceId;
  }

  public static String tokenize(final String token, final String associateRandom, final String sharedKey, final String authToken) {

    final TokenizeModel tokenizeModel =
            TokenizeModel.builder()
                    .setRandom(associateRandom)
                    .setValues(TokenizePropertiesModel.builder()
                            .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                    .setValue(token)
                                    .setPermanent(true)
                                    .build())
                            .build())
                    .build();

    return TestHelper.ensureAsExpected(15,
            () -> SecureService.tokenize(sharedKey, authToken, tokenizeModel),
            SC_OK).jsonPath().getString("value");
  }

  public static String detokenize(final String token, final String associateRandom, final String sharedKey, final String authToken) {

    final DetokenizeModel detokenizeModel =
            DetokenizeModel.builder()
                    .setPermanent(true)
                    .setToken(token)
                    .setRandom(associateRandom)
                    .build();

    return TestHelper.ensureAsExpected(15,
            () -> SecureService.detokenize(sharedKey, authToken, detokenizeModel),
            SC_OK).jsonPath().getString("tokens.additionalProp1");
  }
}
