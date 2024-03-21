package opc.junit.multi.passwords;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import opc.enums.mailhog.MailHogEmail;
import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.junit.database.OkayDatabaseHelper;
import opc.junit.database.PasswordDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.services.admin.AdminService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.PasswordsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

@Execution(ExecutionMode.SAME_THREAD)
public class ForgotPinBiometricTests extends BasePasswordSetup {
    private static final String CHANNEL = EnrolmentChannel.BIOMETRIC.name();

    private static CreateCorporateModel createCorporateModel;
    private static CreateConsumerModel createConsumerModel;
    private static String corporateId;
    private static String consumerId;

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateManagedAccountId;
    private static String consumerManagedAccountId;


    @BeforeAll
    public static void Setup() {
        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);

        AdminHelper.resetProgrammeAuthyLimitsCounter(passcodeAppProgrammeId, adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(passcodeAppProgrammeId, resetCount, adminToken);

        corporateSetup();
        consumerSetup();
    }

    @Test
    public void ForgotPin_CorporateRootUser_Success() throws SQLException {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        SimulatorService.okayOwtForgotPin(passcodeAppSecretKey, id)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(id, corporateAuthenticationToken, State.PENDING_CHALLENGE);

        final Map<Integer, Map<String, String>> passwordNonce = PasswordDatabaseHelper.getPasswordNonce(corporateId);
        assertEquals(1, passwordNonce.size());
        assertEquals(TestHelper.VERIFICATION_CODE, passwordNonce.get(0).get("nonce"));

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(createCorporateModel.getRootUser().getEmail());
        assertEquals(MailHogEmail.CORPORATE_PASSCODE_RESET.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CORPORATE_PASSCODE_RESET.getSubject(), email.getSubject());
        assertEquals(createCorporateModel.getRootUser().getEmail(), email.getTo());
        assertTrue(String.format(MailHogEmail.CORPORATE_PASSCODE_RESET.getEmailText(), createCorporateModel.getRootUser().getEmail()).equalsIgnoreCase(email.getBody()));
    }

    @Test
    public void ForgotPin_ConsumerRootUser_Success() throws SQLException {

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        SimulatorService.okayOwtForgotPin(passcodeAppSecretKey, id)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(id, consumerAuthenticationToken, State.PENDING_CHALLENGE);

        final Map<Integer, Map<String, String>> passwordNonce = PasswordDatabaseHelper.getPasswordNonce(consumerId);
        assertEquals(1, passwordNonce.size());
        assertEquals(TestHelper.VERIFICATION_CODE, passwordNonce.get(0).get("nonce"));

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(createConsumerModel.getRootUser().getEmail());
        assertEquals(MailHogEmail.CONSUMER_PASSCODE_RESET.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CONSUMER_PASSCODE_RESET.getSubject(), email.getSubject());
        assertEquals(createConsumerModel.getRootUser().getEmail(), email.getTo());
        assertTrue(String.format(MailHogEmail.CONSUMER_PASSCODE_RESET.getEmailText(), createConsumerModel.getRootUser().getEmail()).equalsIgnoreCase(email.getBody()));
    }

    @Test
    public void ForgotPin_AuthorizedUser_Success() throws SQLException {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, passcodeAppSecretKey, corporateAuthenticationToken);

        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey,
                user.getRight());

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, user.getRight());

        startVerification(id, user.getRight());

        SimulatorService.okayOwtForgotPin(passcodeAppSecretKey, id)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(id, user.getRight(), State.PENDING_CHALLENGE);

        final Map<Integer, Map<String, String>> passwordNonce = PasswordDatabaseHelper.getPasswordNonce(user.getLeft());
        assertEquals(1, passwordNonce.size());
        assertEquals(TestHelper.VERIFICATION_CODE, passwordNonce.get(0).get("nonce"));

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(usersModel.getEmail());
        assertEquals(MailHogEmail.CORPORATE_PASSCODE_RESET.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CORPORATE_PASSCODE_RESET.getSubject(), email.getSubject());
        assertEquals(usersModel.getEmail(), email.getTo());
        assertTrue(String.format(MailHogEmail.CORPORATE_PASSCODE_RESET.getEmailText(), usersModel.getEmail()).equalsIgnoreCase(email.getBody()));
    }

    @Test
    public void ForgotPin_ResumeCorporateRootUser_Success() {

        final String id = sendOutgoingWireTransfer(corporateManagedAccountId, corporateAuthenticationToken);
        startVerification(id, corporateAuthenticationToken);

        SimulatorService.okayOwtForgotPin(passcodeAppSecretKey, id)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(createCorporateModel.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel(RandomStringUtils.randomNumeric(4)))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, passcodeAppSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        final String newOwtId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken);
        startVerification(newOwtId, corporateAuthenticationToken);

        SimulatorHelper.okayOwtWithPin(passcodeAppSecretKey, newOwtId,
                lostPasswordResumeModel.getNewPassword().getValue(), State.COMPLETED.name());

        assertOutgoingWireTransferState(newOwtId, corporateAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void ForgotPin_ResumeConsumerRootUserStartNewOwt_Success() {

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(id, consumerAuthenticationToken);

        SimulatorService.okayOwtForgotPin(passcodeAppSecretKey, id)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(createConsumerModel.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, passcodeAppSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        final String newOwtId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken);
        startVerification(newOwtId, consumerAuthenticationToken);

        SimulatorHelper.okayOwtWithPin(passcodeAppSecretKey, newOwtId,
                lostPasswordResumeModel.getNewPassword().getValue(), State.COMPLETED.name());

        assertOutgoingWireTransferState(newOwtId, consumerAuthenticationToken, State.COMPLETED);
    }

    @Test
    public void ForgotPin_ResumeConsumerRootUser_Success() throws SQLException {

        final String owtId = sendOutgoingWireTransfer(consumerManagedAccountId, consumerAuthenticationToken);
        startVerification(owtId, consumerAuthenticationToken);

        SimulatorService.okayOwtForgotPin(passcodeAppSecretKey, owtId)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(createConsumerModel.getRootUser().getEmail())
                        .setNewPassword(new PasswordModel("1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, passcodeAppSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        assertEquals("PIN_RECOVERY", OkayDatabaseHelper.getBiometricChallenge(owtId).get(0).get("status"));

        SimulatorService.acceptOkayOwt(passcodeAppSecretKey, owtId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ALREADY_COMPLETED"));
    }

    @Test
    public void ForgotPin_ResumeAuthenticatedUser_Success() {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, passcodeAppSecretKey, consumerAuthenticationToken);

        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey,
                user.getRight());

        final String id = sendOutgoingWireTransfer(consumerManagedAccountId, user.getRight());

        startVerification(id, user.getRight());

        SimulatorService.okayOwtForgotPin(passcodeAppSecretKey, id)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(usersModel.getEmail())
                        .setNewPassword(new PasswordModel(RandomStringUtils.randomNumeric(4)))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, passcodeAppSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        final String newOwtId = sendOutgoingWireTransfer(consumerManagedAccountId, user.getRight());

        startVerification(newOwtId, user.getRight());

        SimulatorHelper.okayOwtWithPin(passcodeAppSecretKey, newOwtId,
                lostPasswordResumeModel.getNewPassword().getValue(), State.COMPLETED.name());

        assertOutgoingWireTransferState(newOwtId, user.getRight(), State.COMPLETED);
    }

    private String sendOutgoingWireTransfer(final String managedAccountId,
                                            final String token) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(
                        passcodeAppOutgoingWireTransfersProfileId,
                        managedAccountId,
                        Currency.EUR.name(), 100L, OwtType.SEPA).build();

        return OutgoingWireTransfersService
                .sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, token,
                        Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private void startVerification(final String id,
                                   final String token) {

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL,
                        passcodeAppSecretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private static void consumerSetup() {
        createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId)
                .setBaseCurrency(Currency.EUR.name())
                .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(
                createConsumerModel, passcodeAppSecretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(passcodeAppSecretKey, consumerId);

        SecureHelper.enrolAndVerifyBiometric(consumerId, passcodeAppSharedKey, passcodeAppSecretKey,
                consumerAuthenticationToken);

        consumerManagedAccountId = ManagedAccountsHelper.createFundedManagedAccount(passcodeAppConsumerManagedAccountProfileId,
                createConsumerModel.getBaseCurrency(), passcodeAppSecretKey, consumerAuthenticationToken).getLeft();
    }

    private static void corporateSetup() {

        createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
                .setBaseCurrency(Currency.EUR.name())
                .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
                createCorporateModel, passcodeAppSecretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(passcodeAppSecretKey, corporateId);

        SecureHelper.enrolAndVerifyBiometric(corporateId, passcodeAppSharedKey, passcodeAppSecretKey,
                corporateAuthenticationToken);

        corporateManagedAccountId = ManagedAccountsHelper.createFundedManagedAccount(passcodeAppCorporateManagedAccountProfileId,
                createCorporateModel.getBaseCurrency(), passcodeAppSecretKey, corporateAuthenticationToken).getLeft();
    }

    private static void assertOutgoingWireTransferState(final String id, final String token, final State state) {
        TestHelper.ensureAsExpected(120,
                () -> OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, id, token),
                x-> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
                Optional.of(String.format("Expecting 200 with an OWT in state %s, check logged payload", state.name())));
    }
}
