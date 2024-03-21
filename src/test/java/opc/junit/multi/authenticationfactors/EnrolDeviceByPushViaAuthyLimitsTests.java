package opc.junit.multi.authenticationfactors;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationFactorsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

public class EnrolDeviceByPushViaAuthyLimitsTests extends BaseAuthenticationFactorsSetup {

    @BeforeAll
    public static void testSetup(){
        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

        AdminHelper.resetProgrammeAuthyLimitsCounter(applicationOne.getProgrammeId(), adminToken);
        AdminHelper.resetProgrammeAuthyLimitsCounter(applicationThree.getProgrammeId(), adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(applicationOne.getProgrammeId(), resetCount, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(applicationThree.getProgrammeId(), resetCount, adminToken);
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @ParameterizedTest
    @EnumSource(value = LimitInterval.class)
    public void EnrolUser_NotificationLimitReached_ChallengeLimitExceeded(final LimitInterval limitInterval) {

        AdminHelper.setAuthyChallengeLimit(applicationThree.getProgrammeId(), ImmutableMap.of(limitInterval, 2));

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(applicationThree.getCorporatesProfileId()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationThree.getSecretKey());

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        AuthenticationFactorsService.getAuthenticationFactors(applicationThree.getSecretKey(), Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void EnrolUser_NegativeNotificationLimit_ChallengeLimitExceeded() {

        AdminHelper.setAuthyChallengeLimit(applicationThree.getProgrammeId(), ImmutableMap.of(LimitInterval.ALWAYS, -1));

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(applicationThree.getCorporatesProfileId()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationThree.getSecretKey());

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        AuthenticationFactorsService.getAuthenticationFactors(applicationThree.getSecretKey(), Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors", nullValue());
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void EnrolUser_NotificationLimitMultipleIdentities_ChallengeLimitExceeded() {

        AdminHelper.setAuthyChallengeLimit(applicationThree.getProgrammeId(), ImmutableMap.of(LimitInterval.ALWAYS, 1));

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(applicationThree.getConsumersProfileId()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, applicationThree.getSecretKey());

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(applicationThree.getCorporatesProfileId()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationThree.getSecretKey());

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        AuthenticationFactorsService.getAuthenticationFactors(applicationThree.getSecretKey(), Optional.empty(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        AuthenticationFactorsService.getAuthenticationFactors(applicationThree.getSecretKey(), Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors", nullValue());
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void EnrolUser_NotificationLimitReachedThenIncreased_Success() {

        AdminHelper.setAuthyChallengeLimit(applicationThree.getProgrammeId(), ImmutableMap.of(LimitInterval.ALWAYS, 1));

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(applicationThree.getConsumersProfileId()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, applicationThree.getSecretKey());

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(applicationThree.getCorporatesProfileId()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationThree.getSecretKey());

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        AdminHelper.setAuthyChallengeLimit(applicationThree.getProgrammeId(), ImmutableMap.of(LimitInterval.ALWAYS, 2));

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(applicationThree.getSecretKey(), Optional.empty(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        AuthenticationFactorsService.getAuthenticationFactors(applicationThree.getSecretKey(), Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void EnrolUser_SmsNotificationNotAffectingAuthyLimit_ChallengeLimitExceeded() {

        AdminHelper.setAuthyChallengeLimit(applicationThree.getProgrammeId(), ImmutableMap.of(LimitInterval.ALWAYS, 1));

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(applicationThree.getCorporatesProfileId()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationThree.getSecretKey());

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        AuthenticationFactorsService.getAuthenticationFactors(applicationThree.getSecretKey(), Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"))
                .body("factors[1].type", equalTo("PUSH"))
                .body("factors[1].channel", equalTo("AUTHY"))
                .body("factors[1].status", equalTo("PENDING_VERIFICATION"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void EnrolUser_MultipleIntervalLimits_ChallengeLimitExceeded() {

        final Map<LimitInterval, Integer> limits = new HashMap<>();
        limits.put(LimitInterval.DAILY, 2);
        limits.put(LimitInterval.WEEKLY, 20);
        limits.put(LimitInterval.MONTHLY, 40);
        limits.put(LimitInterval.QUARTERLY, 60);
        limits.put(LimitInterval.YEARLY, 80);
        limits.put(LimitInterval.ALWAYS, 100);

        AdminHelper.setAuthyChallengeLimit(applicationThree.getProgrammeId(), limits);

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(applicationThree.getCorporatesProfileId()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationThree.getSecretKey());

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(), applicationThree.getSecretKey(), corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHALLENGE_LIMIT_EXCEEDED"));

        AuthenticationFactorsService.getAuthenticationFactors(applicationThree.getSecretKey(), Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @AfterAll
    public static void resetLimits(){
        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

        AdminHelper.setProgrammeAuthyChallengeLimit(applicationOne.getProgrammeId(), resetCount, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(applicationThree.getProgrammeId(), resetCount, adminToken);
    }
}
