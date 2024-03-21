package opc.junit.helpers.multi;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.ResourceType;
import opc.helpers.ChallengesModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.challenges.ChallengesModel;
import opc.services.multi.ChallengesService;

import java.util.List;

public class ChallengesHelper {
  public static String issueOtpChallenges(final ChallengesModel challengesModel,
                                          final String channel,
                                          final String secretKey,
                                          final String token) {

    return TestHelper.ensureAsExpected(15,
        () -> ChallengesService.issueOtpChallenges(challengesModel, channel, secretKey, token),
        SC_OK).jsonPath().getString("scaChallengeId");
  }

  public static String issuePushChallenges(final ChallengesModel challengesModel,
                                           final String channel,
                                           final String secretKey,
                                           final String token) {
    return TestHelper.ensureAsExpected(15,
        () -> ChallengesService.issuePushChallenges(challengesModel, channel, secretKey, token),
        SC_OK).jsonPath().getString("scaChallengeId");
  }

  public static void verifyOtpChallenges(final ChallengesModel challengesModel,
                                         final String scaChallengeId,
                                         final String channel,
                                         final String secretKey,
                                         final String token) {
    TestHelper.ensureAsExpected(15,
        () -> ChallengesService.verifyOtpChallenges(challengesModel, scaChallengeId, channel, secretKey, token),
        SC_NO_CONTENT);
  }

  public static void issueAndVerifyOtpChallenge(final ResourceType resourceType,
                                                final List<String> txIds,
                                                final String secretKey,
                                                final String token) {
    final String scaChallengeId = issueOtpChallenges(ChallengesModelHelper
                    .issueChallengesModel(resourceType, txIds),
            EnrolmentChannel.SMS.name(), secretKey, token);

    verifyOtpChallenges(ChallengesModelHelper
                    .verifyChallengesModel(resourceType, TestHelper.OTP_VERIFICATION_CODE),
            scaChallengeId, EnrolmentChannel.SMS.name(), secretKey, token);
  }

  public static void issueAndVerifyPushChallenge(final ResourceType resourceType,
                                                 final EnrolmentChannel enrolmentChannel,
                                                 final List<String> txIds,
                                                 final String secretKey,
                                                 final String token) {
    final String scaChallengeId = issuePushChallenges(ChallengesModelHelper
                    .issueChallengesModel(resourceType, txIds), enrolmentChannel.name(), secretKey, token);

    if (enrolmentChannel.equals(EnrolmentChannel.AUTHY)) {
      SimulatorHelper.acceptAuthyChallenge(secretKey, scaChallengeId);
    } else {
      SimulatorHelper.acceptOkayChallenge(secretKey, scaChallengeId);
    }
  }
}
