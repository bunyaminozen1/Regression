package fpi.helpers;

import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.UpdateBuyerModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.BuyersService;
import io.restassured.response.Response;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.admin.AdminService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class BuyersHelper {
    public static String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;

    public static Pair<String, String> createAuthenticatedBuyer(final String secretKey,
                                                                final String password) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        return createAuthenticatedBuyer(createBuyerModel, secretKey, password);
    }

    public static Pair<String, String> createAuthenticatedBuyer(final String secretKey) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final String buyerId =
                TestHelper.ensureAsExpected(60,
                                () -> BuyersService.createBuyer(createBuyerModel, secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.createPassword(createPasswordModel, buyerId, secretKey),
                SC_OK);

        verifyEmail(createBuyerModel.getAdminUser().getEmail(), secretKey);

        final String authenticationToken =
                TestHelper.ensureAsExpected(15,
                                () -> AuthenticationService.loginWithPassword(new LoginModel(createBuyerModel.getAdminUser().getEmail(), createPasswordModel.getPassword()), secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        return Pair.of(buyerId, authenticationToken);
    }

    public static Pair<String, String> createAuthenticatedBuyer(final CreateBuyerModel createBuyerModel,
                                                                final String secretKey) {

        final String buyerId =
                TestHelper.ensureAsExpected(60,
                                () -> BuyersService.createBuyer(createBuyerModel, secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.createPassword(createPasswordModel, buyerId, secretKey),
                SC_OK);

        verifyEmail(createBuyerModel.getAdminUser().getEmail(), secretKey);

        final String authenticationToken =
                TestHelper.ensureAsExpected(15,
                                () -> AuthenticationService.loginWithPassword(new LoginModel(createBuyerModel.getAdminUser().getEmail(), createPasswordModel.getPassword()), secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        return Pair.of(buyerId, authenticationToken);
    }

    public static Pair<String, String> createAuthenticatedBuyer(final CreateBuyerModel createBuyerModel,
                                                                final String secretKey,
                                                                final String password) {

        final String buyerId =
                TestHelper.ensureAsExpected(60,
                                () -> BuyersService.createBuyer(createBuyerModel, secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(password)).build();

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.createPassword(createPasswordModel, buyerId, secretKey),
                SC_OK);

        verifyEmail(createBuyerModel.getAdminUser().getEmail(), secretKey);

        final String authenticationToken =
                TestHelper.ensureAsExpected(15,
                                () -> AuthenticationService.loginWithPassword(new LoginModel(createBuyerModel.getAdminUser().getEmail(), createPasswordModel.getPassword()), secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        return Pair.of(buyerId, authenticationToken);
    }

    public static String createBuyer(final CreateBuyerModel createBuyerModel,
                                     final String secretKey) {
        return TestHelper.ensureAsExpected(15,
                        () -> BuyersService.createBuyer(createBuyerModel, secretKey),
                        SC_OK)
                .jsonPath()
                .get("id");
    }

    public static void verifyEmail(final String emailAddress,
                                   final String secretKey) {
        TestHelper.ensureAsExpected(15,
                () -> BuyersService.sendEmailVerification(new SendEmailVerificationModel(emailAddress), secretKey),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> BuyersService.verifyEmail(new EmailVerificationModel(emailAddress, TestHelper.VERIFICATION_CODE), secretKey),
                SC_NO_CONTENT);
    }

    public static Pair<String, String> createAuthenticatedVerifiedBuyer(final String secretKey) {
        final Pair<String, String> buyer = createAuthenticatedBuyer(secretKey);
        verifyKyb(secretKey, buyer.getLeft());
        return buyer;
    }

    public static Pair<String, String> createEnrolledVerifiedBuyer(final String secretKey) {
        final Pair<String, String> buyer = createEnrolledBuyer(secretKey);
        verifyKyb(secretKey, buyer.getLeft());
        return buyer;
    }

    public static Pair<String, String> createEnrolledVerifiedBuyer(final CreateBuyerModel createBuyerModel,
                                                                   final String secretKey) {
        final Pair<String, String> buyer = createEnrolledBuyer(createBuyerModel, secretKey);
        verifyKyb(secretKey, buyer.getLeft());
        return buyer;
    }

    public static Pair<String, String> createEnrolledSteppedUpBuyer(final String secretKey) {
        final Pair<String, String> buyer = createEnrolledBuyer(secretKey);
        verifyKyb(secretKey, buyer.getLeft());
        AuthenticationHelper.startAndVerifyStepup(TestHelper.VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());
        return buyer;
    }

    public static Pair<String, String> createEnrolledSteppedUpBuyer(final CreateBuyerModel createBuyerModel,
                                                                    final String secretKey) {
        final Pair<String, String> buyer = createEnrolledBuyer(createBuyerModel, secretKey);
        verifyKyb(secretKey, buyer.getLeft());
        AuthenticationHelper.startAndVerifyStepup(TestHelper.VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());
        return buyer;
    }


    public static Pair<String, String> createEnrolledBuyer(final CreateBuyerModel createBuyerModel,
                                                           final String secretKey) {
        final Pair<String, String> buyer = createAuthenticatedBuyer(createBuyerModel, secretKey, TestHelper.getDefaultPassword(secretKey));

        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());
        return buyer;
    }

    public static Pair<String, String> createEnrolledBuyer(final String secretKey) {
        final Pair<String, String> buyer = createAuthenticatedBuyer(secretKey, TestHelper.getDefaultPassword(secretKey));

        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, buyer.getRight());
        return buyer;
    }

    public static void verifyKyb(final String secretKey,
                                 final String buyerId) {
        TestHelper.ensureAsExpected(60,
                () -> SimulatorService.simulateKybApproval(secretKey, buyerId),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15, () -> AdminService.getCorporateKyb(buyerId, AdminService.loginAdmin()),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("fullCompanyChecksVerified").equals("APPROVED"),
                Optional.of("Expecting 200 with corporate full company checks APPROVED, check logged payload"));
    }

    public static String startKyb(final String secretKey,
                                  final String token) {
        return TestHelper.ensureAsExpected(60,
                        () -> BuyersService.startKyb(secretKey, token),
                        SC_OK)
                .jsonPath()
                .get("reference");
    }

    public static Response verifyKybStatus(final String secretKey,
                                           final String token,
                                           final String state) {
        return TestHelper.ensureAsExpected(120,
                () -> BuyersService.getKyb(secretKey, token),
                x -> x.statusCode() == SC_OK
                        && x.jsonPath().getString("kybStatus").equals(state)
                        && x.jsonPath().getString("ongoingKybStatus").equals(state),
                Optional.of(String.format("Expecting 200 with a kyb in state %s, check logged payload", state)));
    }

    public static Pair<String, String> createUnauthenticatedBuyer(final String secretKey) {

        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final String buyerId =
                TestHelper.ensureAsExpected(60,
                                () -> BuyersService.createBuyer(createBuyerModel, secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        final String authenticationToken = TestHelper.ensureAsExpected(15,
                        () -> AuthenticationService.createPassword(createPasswordModel, buyerId, secretKey),
                        SC_OK)
                .jsonPath()
                .get("token");

        verifyEmail(createBuyerModel.getAdminUser().getEmail(), secretKey);

        AuthenticationHelper.logout(secretKey, authenticationToken);

        return Pair.of(buyerId, authenticationToken);
    }

    public static void updateBuyer(final UpdateBuyerModel updateBuyerModel,
                                   final String secretKey,
                                   final String buyerToken) {
        TestHelper.ensureAsExpected(15,
                () -> BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken),
                SC_OK);
    }

    public static void assignCreatorRole(final String secretKey,
                                         final String buyerToken) {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.creatorRoleUpdateBuyerModel().build();
        TestHelper.ensureAsExpected(15,
                () -> BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken),
                SC_OK);
    }

    public static void assignControllerRole(final String secretKey,
                                            final String buyerToken) {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.controllerRoleUpdateBuyerModel().build();
        TestHelper.ensureAsExpected(15,
                () -> BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken),
                SC_OK);
    }

    public static void assignAllRoles(final String secretKey,
                                      final String buyerToken) {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.allRolesUpdateBuyerModel().build();
        TestHelper.ensureAsExpected(15,
                () -> BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken),
                SC_OK);
    }

    public static void assignAdminRole(final String secretKey,
                                      final String buyerToken) {
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.adminRolesUpdateBuyerModel().build();
        TestHelper.ensureAsExpected(15,
                () -> BuyersService.updateBuyer(updateBuyerModel, secretKey, buyerToken),
                SC_OK);
    }

    public static Pair<String, String> createBuyerWithZba(final String secretKey) {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final Pair<String, String> authenticatedBuyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);

        BuyersHelper.verifyKyb(secretKey, authenticatedBuyer.getLeft());
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, authenticatedBuyer.getRight());
        AuthenticationHelper.login(createBuyerModel.getAdminUser().getEmail(), TestHelper.getDefaultPassword(secretKey), secretKey);
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, authenticatedBuyer.getRight());

        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, authenticatedBuyer.getRight())
                .jsonPath().get("accounts[0].id");
        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, authenticatedBuyer.getRight(), "ALLOCATED");

        return Pair.of(authenticatedBuyer);
    }
}
