package opc.junit.helpers.multi;

import opc.enums.opc.EnrolmentChannel;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.database.SumsubDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.corporates.PatchCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.CorporatesService;
import opc.services.multi.PasswordsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class CorporatesHelper {

    public static String VERIFICATION_CODE = TestHelper.OTP_VERIFICATION_CODE;

    public static Pair<String, String> createAuthenticatedCorporate(final String profileId, final String secretKey) {

        return createAuthenticatedCorporate(profileId, secretKey, TestHelper.getDefaultPassword(secretKey));
    }

    public static Pair<String, String> createAuthenticatedCorporate(final String profileId, final String secretKey, final String password) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(profileId).build();

        return createAuthenticatedCorporate(createCorporateModel, secretKey, password);
    }

    public static Pair<String, String> createEnrolledCorporate(final String profileId, final String secretKey) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(profileId).build();

        final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel, secretKey, TestHelper.getDefaultPassword(secretKey));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        return corporate;
    }

    public static Pair<String, String> createEnrolledCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel, secretKey, TestHelper.getDefaultPassword(secretKey));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        return corporate;
    }

    public static Pair<String, String> createStepupAuthenticatedCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());
        return corporate;
    }

    public static Pair<String, String> createStepupAuthenticatedCorporate(final String profileId, final String secretKey) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(profileId).build();

        final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel, secretKey, TestHelper.getDefaultPassword(secretKey));

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());

        return corporate;
    }

    public static Pair<String, String> createStepupAuthenticatedVerifiedCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        final Pair<String, String> corporate = createStepupAuthenticatedCorporate(createCorporateModel, secretKey);
        verifyKyb(secretKey, corporate.getLeft());
        return corporate;
    }

    public static Pair<String, String> createAuthenticatedCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        return createAuthenticatedCorporate(createCorporateModel, secretKey, TestHelper.getDefaultPassword(secretKey));
    }

    public static Pair<String, String> createAuthenticatedCorporate(final CreateCorporateModel createCorporateModel, final String secretKey, final String password) {
        final String corporateId =
                TestHelper.ensureAsExpected(60,
                                () -> CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id.id");

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(password)).build();

        TestHelper.ensureAsExpected(15,
                () -> PasswordsService.createPassword(createPasswordModel, corporateId, secretKey),
                SC_OK);

        CorporatesHelper.verifyEmail(createCorporateModel.getRootUser().getEmail(), secretKey);

        final String authenticationToken =
                TestHelper.ensureAsExpected(15,
                                () -> AuthenticationService.loginWithPassword(new LoginModel(createCorporateModel.getRootUser().getEmail(), createPasswordModel.getPassword()), secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        return Pair.of(corporateId, authenticationToken);
    }

    public static Pair<String, String> createAuthenticatedVerifiedCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        verifyKyb(secretKey, corporate.getLeft());
        return corporate;
    }

    public static Pair<String, String> createAuthenticatedVerifiedCorporate(final String profile, final String secretKey) {
        final Pair<String, String> corporate = createAuthenticatedCorporate(profile, secretKey);
        verifyKyb(secretKey, corporate.getLeft());
        return corporate;
    }

    public static Pair<String, String> createBiometricEnrolledVerifiedCorporate(final CreateCorporateModel createCorporateModel,
                                                                                final String secretKey,
                                                                                final String sharedKey) {
        final Pair<String, String> corporate = createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), sharedKey, secretKey, corporate.getRight());
        return corporate;
    }

    public static Pair<String, String> createAuthyEnrolledVerifiedCorporate(final CreateCorporateModel createCorporateModel,
                                                                            final String secretKey) {
        final Pair<String, String> corporate = createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());
        return corporate;
    }

    public static Pair<String, String> createKybVerifiedCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        startKyb(secretKey, corporate.getRight());
        verifyKyb(secretKey, corporate.getLeft());
        return corporate;
    }

    public static String createKybVerifiedLinkedCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        final String corporateId = createCorporate(createCorporateModel, secretKey);
        verifyKyb(secretKey, corporateId);
        return corporateId;
    }

    public static Pair<String, String> createEnrolledVerifiedCorporate(final String profile, final String secretKey) {
        final Pair<String, String> corporate = createEnrolledCorporate(profile, secretKey);
        verifyKyb(secretKey, corporate.getLeft());
        return corporate;
    }

    public static Pair<String, String> createEnrolledVerifiedCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        final Pair<String, String> corporate = createEnrolledCorporate(createCorporateModel, secretKey);
        verifyKyb(secretKey, corporate.getLeft());
        return corporate;
    }

    public static Pair<String, String> createUnauthenticatedCorporate(final String corporateProfileId, final String secretKey) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final String corporateId = createCorporate(createCorporateModel, secretKey);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        final String token =
                TestHelper.ensureAsExpected(15,
                                () -> PasswordsService.createPassword(createPasswordModel, corporateId, secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        verifyKyb(secretKey, corporateId);

        AuthenticationService.logout(secretKey, token);

        return Pair.of(corporateId, token);
    }

    public static void verifyKyb(final String secretKey, final String corporateId) {

        TestHelper.ensureAsExpected(60,
                () -> SimulatorService.simulateKybApproval(secretKey, corporateId),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15, () -> AdminService.getCorporateKyb(corporateId, AdminService.loginAdmin()),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("fullCompanyChecksVerified").equals("APPROVED"),
                Optional.of("Expecting 200 with corporate full company checks APPROVED, check logged payload"));
    }

    public static void verifyEmail(final String emailAddress, final String secretKey) {
        TestHelper.ensureAsExpected(15,
                () -> CorporatesService.sendEmailVerification(new SendEmailVerificationModel(emailAddress), secretKey),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> CorporatesService.verifyEmail(new EmailVerificationModel(emailAddress, TestHelper.VERIFICATION_CODE), secretKey),
                SC_NO_CONTENT);
    }

    public static String createCorporate(final String profileId, final String secretKey) {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(profileId).build();
        return TestHelper.ensureAsExpected(15, () -> CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), SC_OK)
                .jsonPath()
                .get("id.id");
    }

    public static String createCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        return TestHelper.ensureAsExpected(15, () -> CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()), SC_OK)
                .jsonPath()
                .get("id.id");
    }

    public static String createCorporatePassword(final String corporateId, final String secretKey) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        TestHelper.ensureAsExpected(15,
                () -> PasswordsService.createPassword(createPasswordModel, corporateId, secretKey),
                SC_OK);
        return createPasswordModel.getPassword().getValue();
    }

    public static String startKyb(final String secretKey, final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> CorporatesService.startCorporateKyb(secretKey, token),
                        SC_OK)
                .jsonPath()
                .get("reference");
    }

    public static void verifyCorporateState(final String corporateId, final String state) {

        // TODO Should be done by api call
        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> CorporatesDatabaseHelper.getCorporate(corporateId),
                x -> x.size() > 0 && x.get(0).get("basic_company_checks_verified").equals(state)
                        && x.get(0).get("full_company_checks_verified").equals(state),
                Optional.of(String.format("Basic and Full company checks for corporate with id %s not in state %s as expected",
                        corporateId, state)));
    }

    public static void verifyCorporateLastApprovalTime(final String corporateId, final Consumer<Long> handler) {
        Map<Integer, Map<String, String>> result = null;
        try {
            result = CorporatesDatabaseHelper.getCorporate(corporateId);
        } catch (Exception e) {
            Assertions.fail("Unable to get corporate", e);
        }

        if (result.size() <= 0) {
            Assertions.fail("Can't find corporate");
        }

        final String lastApprovalTimeString = result.get(0).get("last_approval_time");
        final Long lastApprovalTime = lastApprovalTimeString == null ? null : Long.valueOf(lastApprovalTimeString);
        handler.accept(lastApprovalTime);
    }

    public static void patchCorporate(final PatchCorporateModel patchCorporateModel, final String secretKey, final String token) {
        TestHelper.ensureAsExpected(15,
                () -> CorporatesService.patchCorporate(patchCorporateModel, secretKey, token, Optional.empty()),
                SC_OK);
    }

    public static void verifyBeneficiaryLastApprovalTime(final String applicantId, final Consumer<Long> handler) {
        Map<Integer, Map<String, String>> result = null;
        try {
            result = SumsubDatabaseHelper.getBeneficiary(applicantId);
            if (result.size() > 0) {
                result = CorporatesDatabaseHelper.getBeneficiaryByRefId(result.get(0).get("id"));
            }
        } catch (Exception e) {
            Assertions.fail("Unable to get beneficiary", e);
        }

        if (result.size() <= 0) {
            Assertions.fail("Can't find beneficiary");
        }

        final String lastApprovalTimeString = result.get(0).get("last_approval_time");
        final Long lastApprovalTime = lastApprovalTimeString == null ? null : Long.valueOf(lastApprovalTimeString);
        handler.accept(lastApprovalTime);
    }

    public static Pair<String, String> createEnrolledAllFactorsVerifiedCorporate(final CreateCorporateModel createCorporateModel,
                                                                                 final String secretKey,
                                                                                 final String sharedKey) {
        final Pair<String, String> corporate = createEnrolledCorporate(createCorporateModel, secretKey);
        verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());
        SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), sharedKey, secretKey, corporate.getRight());
        return corporate;
    }

    public static String createCorporateWithPassword(final CreateCorporateModel createCorporateModel,
                                                     final String secretKey) {
        final String corporateId =
                TestHelper.ensureAsExpected(60,
                                () -> CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id.id");

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.DEFAULT_PASSWORD)).build();

        TestHelper.ensureAsExpected(15,
                () -> PasswordsService.createPassword(createPasswordModel, corporateId, secretKey),
                SC_OK);

        return corporateId;
    }

    public static Pair<String, String> createSteppedUpCorporate(final CreateCorporateModel createCorporateModel, final String secretKey) {
        final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel, secretKey, TestHelper.getDefaultPassword(secretKey));
        verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        return corporate;
    }
}
