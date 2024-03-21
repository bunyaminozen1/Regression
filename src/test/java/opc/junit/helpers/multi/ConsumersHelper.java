package opc.junit.helpers.multi;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.KycLevel;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.PatchConsumerModel;
import opc.models.multi.consumers.PrefillDetailsModel;
import opc.models.multi.consumers.StartKycModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ConsumersService;
import opc.services.multi.PasswordsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class ConsumersHelper {

    public static String VERIFICATION_CODE = TestHelper.OTP_VERIFICATION_CODE;

    public static Pair<String, String> createAuthenticatedConsumer(final String profile, final String secretKey) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(profile).build();

        return createAuthenticatedConsumer(createConsumerModel, secretKey, TestHelper.getDefaultPassword(secretKey));
    }

    public static Pair<String, String> createEnrolledConsumer(final String profile, final String secretKey) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(profile).build();

        final Pair<String, String> consumer = createAuthenticatedConsumer(createConsumerModel, secretKey, TestHelper.getDefaultPassword(secretKey));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, consumer.getRight());

        return consumer;
    }

    public static Pair<String, String> createEnrolledConsumer(final CreateConsumerModel createConsumerModel, final String secretKey) {

        final Pair<String, String> consumer = createAuthenticatedConsumer(createConsumerModel, secretKey, TestHelper.getDefaultPassword(secretKey));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, consumer.getRight());

        return consumer;
    }

    public static Pair<String, String> createStepupAuthenticatedConsumer(final CreateConsumerModel createConsumerModel, final String secretKey) {
        final Pair<String, String> consumer = createAuthenticatedConsumer(createConsumerModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumer.getLeft(), secretKey, consumer.getRight());
        return consumer;
    }

    public static Pair<String, String> createStepupAuthenticatedConsumer(final String profile, final String secretKey) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(profile).build();

        final Pair<String, String> consumer = createAuthenticatedConsumer(createConsumerModel, secretKey, TestHelper.getDefaultPassword(secretKey));

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumer.getLeft(), secretKey, consumer.getRight());

        return consumer;
    }

    public static Pair<String, String> createBiometricStepupAuthenticatedConsumer(final String profile, final String sharedKey, final String secretKey) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(profile).build();

        final Pair<String, String> consumer = createAuthenticatedConsumer(createConsumerModel, secretKey, TestHelper.getDefaultPassword(secretKey));

        SecureHelper.enrolAndVerifyBiometric(consumer.getLeft(), sharedKey, secretKey, consumer.getRight());

        return consumer;
    }

    public static Pair<String, String> createStepupAuthenticatedVerifiedConsumer(final CreateConsumerModel createConsumerModel, final String secretKey) {
        final Pair<String, String> consumer = createStepupAuthenticatedConsumer(createConsumerModel, secretKey);
        verifyKyc(secretKey, consumer.getLeft());
        return consumer;
    }

    public static Pair<String, String> createAuthenticatedConsumerWithSetupPassword(final CreateConsumerModel createConsumerModel, final String secretKey, final String password) {
        return createAuthenticatedConsumer(createConsumerModel, secretKey, password);
    }

    public static Pair<String, String> createAuthenticatedConsumer(final CreateConsumerModel createConsumerModel, final String secretKey) {
        return createAuthenticatedConsumer(createConsumerModel, secretKey, TestHelper.getDefaultPassword(secretKey));
    }

    public static Pair<String, String> createAuthenticatedConsumer(final CreateConsumerModel createConsumerModel, final String secretKey, final String password) {

        final String consumerId = createConsumer(createConsumerModel, secretKey);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(password)).build();

        TestHelper.ensureAsExpected(15,
                () -> PasswordsService.createPassword(createPasswordModel, consumerId, secretKey),
                SC_OK);

        ConsumersHelper.verifyEmail(createConsumerModel.getRootUser().getEmail(), secretKey);

        final String authenticationToken =
                TestHelper.ensureAsExpected(30,
                                () -> AuthenticationService.loginWithPassword(new LoginModel(createConsumerModel.getRootUser().getEmail(), createPasswordModel.getPassword()), secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        return Pair.of(consumerId, authenticationToken);
    }

    public static Pair<String, String> createAuthenticatedVerifiedConsumer(final CreateConsumerModel createConsumerModel, final String secretKey) {
        final Pair<String, String> consumer = createAuthenticatedConsumer(createConsumerModel, secretKey);
        verifyKyc(secretKey, consumer.getLeft());
        return consumer;
    }

    public static Pair<String, String> createAuthenticatedVerifiedConsumer(final String profile, final String secretKey) {
        final Pair<String, String> consumer = createAuthenticatedConsumer(profile, secretKey);
        verifyKyc(secretKey, consumer.getLeft());
        return consumer;
    }

    public static Pair<String, String> createEnrolledVerifiedConsumer(final String profile, final String secretKey) {
        final Pair<String, String> consumer = createEnrolledConsumer(profile, secretKey);
        verifyKyc(secretKey, consumer.getLeft());
        return consumer;
    }

    public static Pair<String, String> createEnrolledVerifiedConsumer(final CreateConsumerModel createConsumerModel, final String secretKey) {
        final Pair<String, String> consumer = createEnrolledConsumer(createConsumerModel, secretKey);
        verifyKyc(secretKey, consumer.getLeft());
        return consumer;
    }

    public static Pair<String, String> createAuthenticatedVerifiedConsumer(final CreateConsumerModel createConsumerModel, final KycLevel kycLevel, final String secretKey) {
        final Pair<String, String> consumer = createAuthenticatedConsumer(createConsumerModel, secretKey);
        startKyc(kycLevel, secretKey, consumer.getRight());
        verifyKyc(secretKey, consumer.getLeft());
        return consumer;
    }

    public static Pair<String, String> createAuthenticatedVerifiedConsumer(final String profile, final KycLevel kycLevel, final String secretKey) {
        final Pair<String, String> consumer = createAuthenticatedConsumer(profile, secretKey);
        startKyc(kycLevel, secretKey, consumer.getRight());
        verifyKyc(secretKey, consumer.getLeft());
        return consumer;
    }

    public static Pair<String, String> createBiometricEnrolledVerifiedConsumer(final CreateConsumerModel createConsumerModel,
                                                                               final String secretKey,
                                                                               final String sharedKey) {
        final Pair<String, String> consumer = createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        SecureHelper.enrolAndVerifyBiometric(consumer.getLeft(), sharedKey, secretKey, consumer.getRight());
        return consumer;
    }

    public static Pair<String, String> createAuthyEnrolledVerifiedConsumer(final CreateConsumerModel createConsumerModel,
                                                                           final String secretKey) {
        final Pair<String, String> consumer = createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumer.getLeft(), secretKey, consumer.getRight());
        return consumer;
    }

    public static Pair<String, String> createUnauthenticatedConsumer(final String consumerProfileId, final String secretKey) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String consumerId = createConsumer(createConsumerModel, secretKey);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        final String token =
                TestHelper.ensureAsExpected(15,
                                () -> PasswordsService.createPassword(createPasswordModel, consumerId, secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        verifyKyc(secretKey, consumerId);

        AuthenticationService.logout(secretKey, token);

        return Pair.of(consumerId, token);
    }

    public static void verifyKyc(final String secretKey, final String consumerId) {

        TestHelper.ensureAsExpected(180,
                () -> SimulatorService.simulateKycApproval(secretKey, consumerId),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> AdminHelper.getConsumerKyc(consumerId, AdminService.loginAdmin()),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("fullDueDiligence")
                        .equals("APPROVED"),
                Optional.of("Expecting 200 with consumer full due diligence APPROVED, check logged payload"));
    }

    public static void verifyEmail(final String emailAddress, final String secretKey) {
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.sendEmailVerification(new SendEmailVerificationModel(emailAddress), secretKey),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.verifyEmail(new EmailVerificationModel(emailAddress, TestHelper.VERIFICATION_CODE), secretKey),
                SC_NO_CONTENT);
    }

    public static String createConsumer(final CreateConsumerModel createConsumerModel, final String secretKey) {
        return TestHelper.ensureAsExpected(60, () -> ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty()), SC_OK)
                .jsonPath()
                .get("id.id");
    }

    public static void patchConsumer(final PatchConsumerModel patchConsumerModel,
                                     final String secretKey,
                                     final String token) {
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.patchConsumer(patchConsumerModel, secretKey, token, Optional.empty()),
                SC_OK);
    }

    public static String createConsumerPassword(final String consumerId, final String secretKey) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        TestHelper.ensureAsExpected(15,
                () -> PasswordsService.createPassword(createPasswordModel, consumerId, secretKey),
                SC_OK);
        return createPasswordModel.getPassword().getValue();
    }

    public static String startKyc(final String secretKey, final String token) {
        return startKyc(KycLevel.KYC_LEVEL_2, secretKey, token);
    }

    public static String startKyc(final KycLevel kycLevel, final String secretKey, final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> ConsumersService.startConsumerKyc(StartKycModel.startKycModel(kycLevel), secretKey, token),
                        SC_OK)
                .jsonPath()
                .get("reference");
    }

    public static String startKycWithQuestionnaireValues(final KycLevel kycLevel, final String secretKey, final String token, final List<PrefillDetailsModel> questionnaire) {
        return TestHelper.ensureAsExpected(15,
                        () -> ConsumersService.startConsumerKyc(
                                StartKycModel.startKycModelWithPrefillDetails(kycLevel, questionnaire), secretKey, token),
                        SC_OK)
                .jsonPath()
                .get("reference");
    }

    public static void verifyConsumerState(final String consumerId, final String state) {
        checkConsumerDetails(consumerId, "full_verification_status", state);
    }

    public static void verifyConsumerOngoingState(final String consumerId, final String state) {
        checkConsumerDetails(consumerId, "ongoing_kyc_status", state);
    }

    public static void verifyConsumerOccupation(final String consumerId, final String occupation) {
        checkConsumerDetails(consumerId, "occupation", occupation);
    }

    public static void verifyConsumerSourceOfFunds(final String consumerId, final String sourceOfFunds) {
        checkConsumerDetails(consumerId, "source_of_funds", sourceOfFunds);
    }

    public static void verifyConsumerLastApprovalTime(final String consumerId, final Consumer<Long> handler) {
        Map<Integer, Map<String, String>> result = null;
        try {
            result = ConsumersDatabaseHelper.getConsumer(consumerId);
        } catch (Exception e) {
            Assertions.fail("Unable to get consumer", e);
        }

        if (result.size() == 0) {
            Assertions.fail("Can't find consumer");
        }

        final String lastApprovalTimeString = result.get(0).get("last_approval_time");
        final Long lastApprovalTime = lastApprovalTimeString == null ? null : Long.valueOf(lastApprovalTimeString);
        handler.accept(lastApprovalTime);
    }

    public static String startKycMobile(final KycLevel kycLevel, final String secretKey, final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> ConsumersService.startKycMobile(StartKycModel.startKycModel(kycLevel), secretKey, token),
                        SC_OK)
                .jsonPath()
                .get("externalUserId");
    }

    private static void checkConsumerDetails(final String consumerId,
                                             final String field,
                                             final String value) {
        // TODO These should be handled by API call
        TestHelper.ensureDatabaseResultAsExpected(90,
                () -> ConsumersDatabaseHelper.getConsumer(consumerId),
                x -> x.size() > 0 && x.get(0).get(field).equals(value),
                Optional.of(String.format("Field %s for consumer with id %s does not have value %s as expected",
                        field, consumerId, value)));
    }

    public static Pair<String, String> createEnrolledAllFactorsVerifiedConsumer(final CreateConsumerModel createConsumerModel,
                                                                                final String secretKey,
                                                                                final String sharedKey) {
        final Pair<String, String> consumer = createEnrolledConsumer(createConsumerModel, secretKey);
        verifyKyc(secretKey, consumer.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumer.getLeft(), secretKey, consumer.getRight());
        SecureHelper.enrolAndVerifyBiometric(consumer.getLeft(), sharedKey, secretKey, consumer.getRight());
        return consumer;
    }

    public static Pair<String, String> createSteppedUpConsumer(final CreateConsumerModel createConsumerModel, final String secretKey) {
        return createSteppedUpConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);
    }

    public static Pair<String, String> createSteppedUpConsumer(final CreateConsumerModel createConsumerModel, final KycLevel kycLevel, final String secretKey) {

        final Pair<String, String> consumer = createAuthenticatedConsumer(createConsumerModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        startKyc(kycLevel, secretKey, consumer.getRight());
        verifyKyc(secretKey, consumer.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, consumer.getRight());
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, consumer.getRight());

        return consumer;
    }
}
