package opc.junit.multi.access;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class IssueAuthyPushNotificationToStepUpTokenLimitsTests extends BaseAuthenticationSetup {

    @BeforeAll
    public static void testSetup() {
        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

        AdminHelper.resetProgrammeAuthyLimitsCounter(applicationOne.getProgrammeId(), adminToken);
        AdminHelper.resetProgrammeAuthyLimitsCounter(scaApp.getProgrammeId(), adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(applicationOne.getProgrammeId(), resetCount, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(scaApp.getProgrammeId(), resetCount, adminToken);
    }

    @AfterAll
    public static void resetLimits() {
        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

        AdminHelper.setProgrammeAuthyChallengeLimit(applicationOne.getProgrammeId(), resetCount, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(scaApp.getProgrammeId(), resetCount, adminToken);
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void IssuePushNotification_NotificationLimitReached_ChallengeLimitExceeded(final LimitInterval limitInterval) {
        AdminHelper.setAuthyChallengeLimit(scaApp.getProgrammeId(), ImmutableMap.of(limitInterval, 1));

        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(scaApp.getCorporatesProfileId(), scaApp.getSecretKey());

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), scaApp.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));
    }

    //    TODO: https://weavr-payments.atlassian.net/browse/DEV-2704 Fix this test after investigation about increasing limits
    @Disabled
    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    @DisplayName("IssuePushNotification_NotificationLimitReachedThenIncreased_Success - will be fixed by DEV-2704")
    public void IssuePushNotification_NotificationLimitReachedThenIncreased_Success() {
        AdminHelper.setAuthyChallengeLimit(scaApp.getProgrammeId(), ImmutableMap.of(LimitInterval.ALWAYS, 1));

        final Pair<String, String> corporate = CorporatesHelper.createStepupAuthenticatedCorporate(scaApp.getCorporatesProfileId(), scaApp.getSecretKey());

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), scaApp.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        AdminHelper.setAuthyChallengeLimit(scaApp.getProgrammeId(), ImmutableMap.of(LimitInterval.ALWAYS, 1));

        AuthenticationService.issuePushStepup(EnrolmentChannel.AUTHY.name(), scaApp.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_OK);
    }
}
