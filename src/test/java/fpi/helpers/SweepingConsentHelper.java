package fpi.helpers;

import fpi.paymentrun.models.IssueSweepingConsentChallengeModel;
import fpi.paymentrun.models.VerifySweepingConsentModel;
import fpi.paymentrun.services.uicomponents.SweepingConsentService;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;

public class SweepingConsentHelper {

    public static String issueSweepingConsentChallenge(final String linkedAccountId,
                                                       final String managedAccountId,
                                                       final String buyerToken,
                                                       final String sharedKey) {

        final IssueSweepingConsentChallengeModel issueSweepingConsentChallengeModel =
                IssueSweepingConsentChallengeModel.builder()
                        .linkedAccountId(linkedAccountId)
                        .managedAccountId(managedAccountId)
                        .build();

        return TestHelper.ensureAsExpected(15,
                        () -> SweepingConsentService.issueSweepingConsentChallenge(issueSweepingConsentChallengeModel, buyerToken, EnrolmentChannel.SMS.name(), sharedKey), SC_CREATED)
                .jsonPath()
                .get("scaChallengeId");
    }

    public static void verifySweepingConsentChallenge(final String scaChallengeId,
                                                      final String buyerToken,
                                                      final String sharedKey) {
        TestHelper.ensureAsExpected(15,
                () -> SweepingConsentService.verifySweepingConsentChallenge(
                        VerifySweepingConsentModel.defaultVerificationModel(), buyerToken, scaChallengeId, EnrolmentChannel.SMS.name(), sharedKey),
                SC_NO_CONTENT);
    }

    public static void issueAndVerifySweepingConsentChallenge(final String linkedAccountId,
                                                              final String buyerToken,
                                                              final String secretKey,
                                                              final String sharedKey) {
        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken).jsonPath().getString("accounts[0].id");
        final String scaChallengeId = issueSweepingConsentChallenge(linkedAccountId, managedAccountId, buyerToken, sharedKey);
        verifySweepingConsentChallenge(scaChallengeId, buyerToken, sharedKey);
    }

    public static void executeSweepingJob() {
        TestHelper.ensureAsExpected(15,
                () -> SweepingConsentService.executeSweepingJob(),
                SC_CREATED);
    }
}
