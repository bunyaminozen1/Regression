package opc.junit.helpers.multi;

import commons.enums.State;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.authfactors.AuthenticationFactorsResponseModel;
import opc.models.shared.VerificationModel;
import opc.services.multi.AuthenticationFactorsService;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class AuthenticationFactorsHelper {

  public static void enrolOtpUser(final String channel,
      final String secretKey,
      final String token) {
    TestHelper.ensureAsExpected(15,
        () -> AuthenticationFactorsService.enrolOtp(channel, secretKey, token),
        SC_NO_CONTENT);
  }

  public static void enrolAuthyPushUser(final String secretKey,
      final String token) {
    TestHelper.ensureAsExpected(90,
        () -> AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), secretKey,
            token),
        SC_NO_CONTENT);
  }

  public static void unenrolBiometricPushUser(final String secretKey,
                                            final String token) {
    TestHelper.ensureAsExpected(90,
            () -> AuthenticationFactorsService.unenrolPush(EnrolmentChannel.BIOMETRIC.name(), secretKey,
                    token),
            SC_NO_CONTENT);
  }

  public static void verifyOtpEnrolment(final String verificationCode,
      final String channel,
      final String secretKey,
      final String token) {
    TestHelper.ensureAsExpected(15,
        () -> AuthenticationFactorsService.verifyEnrolment(new VerificationModel(verificationCode),
            channel, secretKey, token),
        SC_NO_CONTENT);
  }

  public static void verifyAuthyPushEnrolment(final String identityId,
      final String secretKey,
      final String token) {
    SimulatorHelper.acceptAuthyIdentity(secretKey, identityId, token, State.ACTIVE);
  }

  public static void rejectAuthyPushEnrolment(final String identityId,
      final String secretKey,
      final String token) {
    SimulatorHelper.rejectAuthyIdentity(secretKey, identityId, token, State.INACTIVE);
  }


  public static void enrolAndVerifyOtp(final String verificationCode,
      final String channel,
      final String secretKey,
      final String token) {
    enrolOtpUser(channel, secretKey, token);
    verifyOtpEnrolment(verificationCode, channel, secretKey, token);
  }

  public static void enrolAndVerifyPush(
      final String identityId,
      final String secretKey,
      final String token) {

    enrolAndVerifyAuthyPush(identityId, secretKey, token);
  }

  public static void enrolAndVerifyAuthyPush(final String identityId,
      final String secretKey,
      final String token) {
    enrolAuthyPushUser(secretKey, token);
    verifyAuthyPushEnrolment(identityId, secretKey, token);
  }

  public static void enrolAndRejectAuthyPush(final String identityId,
      final String secretKey,
      final String token) {
    enrolAuthyPushUser(secretKey, token);
    rejectAuthyPushEnrolment(identityId, secretKey, token);
  }

  public static void checkAuthenticationFactorState(final EnrolmentChannel channel,
                                                    final State state,
                                                    final String secretKey,
                                                    final String token) {

    TestHelper.ensureAsExpected(15,
            () -> AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), token),
            x ->  x.statusCode() == SC_OK &&
                    x.as(AuthenticationFactorsResponseModel.class)
                            .getFactors().stream().filter(y -> y.getChannel().equals(channel.name()))
                            .findFirst().orElseThrow().getStatus().equals(state.name()),
            Optional.of(String.format("%s factor state not in %s as expected", channel.name(), state.name())));
  }
}
