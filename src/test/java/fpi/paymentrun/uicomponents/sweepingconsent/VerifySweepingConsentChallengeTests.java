package fpi.paymentrun.uicomponents.sweepingconsent;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.SweepingConsentHelper;
import fpi.helpers.simulator.SimulatorHelper;
import fpi.paymentrun.models.VerifySweepingConsentModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.uicomponents.SweepingConsentService;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class VerifySweepingConsentChallengeTests extends BasePaymentRunSetup {
//    TODO create cases for OTP Limits (https://weavr-payments.atlassian.net/browse/FPI-519) when this implemented: https://weavr-payments.atlassian.net/browse/FPI-375

    private static final String ENROLMENT_CHANNEL = EnrolmentChannel.SMS.name();

    private Pair<String, String> buyer;

    @BeforeEach
    public void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());
    }

    @Test
    public void VerifySweepingConsent_MultipleRolesBuyer_Success() {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifySweepingConsent_ValidRoleBuyer_Success() {
        Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignControllerRole(secretKey, buyer.getRight());

        final String scaChallengeId = issueSweepingConsent(buyer);

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifySweepingConsent_IncorrectRoleBuyer_Forbidden() {
        Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignControllerRole(secretKey, buyer.getRight());

        final String scaChallengeId = issueSweepingConsent(buyer);

        BuyersHelper.assignCreatorRole(secretKey, buyer.getRight());
        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifySweepingConsent_AdminRoleBuyer_Forbidden() {
        Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignControllerRole(secretKey, buyer.getRight());

        final String scaChallengeId = issueSweepingConsent(buyer);

        BuyersHelper.assignAdminRole(secretKey, buyer.getRight());
        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifySweepingConsent_RootUserHavingMultipleLinkedAccounts_Success() {

        final List<String> linkedAccounts =
                SimulatorHelper.createLinkedAccounts(buyer, 3, secretKey);

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        linkedAccounts.forEach(account -> {
            final String scaChallengeId =
                    SweepingConsentHelper.issueSweepingConsentChallenge(account, managedAccountId, buyer.getRight(), sharedKey);

            SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                            buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                    .then()
                    .statusCode(SC_NO_CONTENT);
        });
    }

    @Test
    public void VerifySweepingConsent_ValidRoleAuthUser_Success() {

        final Pair<String, String> user =
                BuyerAuthorisedUserHelper.createEnrolledSteppedUpUser(secretKey, buyer.getRight());
        BuyerAuthorisedUserHelper.assignControllerRole(user.getLeft(), secretKey, buyer.getRight());

        final String scaChallengeId = issueSweepingConsent(buyer.getLeft(), user.getRight());

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        user.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifySweepingConsent_MultipleRolesAuthUser_Success() {

        final Pair<String, String> user =
                BuyerAuthorisedUserHelper.createEnrolledSteppedUpUser(secretKey, buyer.getRight());
        BuyerAuthorisedUserHelper.assignAllRoles(user.getLeft(), secretKey, buyer.getRight());

        final String scaChallengeId = issueSweepingConsent(buyer.getLeft(), user.getRight());

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        user.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifySweepingConsent_IncorrectRoleAuthUser_Forbidden() {

        final Pair<String, String> user =
                BuyerAuthorisedUserHelper.createEnrolledSteppedUpUser(secretKey, buyer.getRight());
        BuyerAuthorisedUserHelper.assignAllRoles(user.getLeft(), secretKey, buyer.getRight());

        final String scaChallengeId = issueSweepingConsent(buyer.getLeft(), user.getRight());

        BuyerAuthorisedUserHelper.assignCreatorRole(user.getLeft(), secretKey, buyer.getRight());
        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        user.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifySweepingConsent_OtherBuyerToken_StateInvalid() {
        final String scaChallengeId = issueSweepingConsent(buyer);

        Pair<String, String> otherBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignControllerRole(secretKey, otherBuyer.getRight());
        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        otherBuyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifySweepingConsent_OtherBuyerAuthUserToken_StateInvalid() {
        final String scaChallengeId = issueSweepingConsent(buyer);

        Pair<String, String> otherBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        final Pair<String, String> user =
                BuyerAuthorisedUserHelper.createEnrolledSteppedUpUser(secretKey, otherBuyer.getRight());
        BuyerAuthorisedUserHelper.assignAllRoles(user.getLeft(), secretKey, otherBuyer.getRight());
        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        user.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @Test
    public void VerifySweepingConsent_AlreadyVerified_StateInvalid() {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = {"EMAIL", "UNKNOWN"})
    public void VerifySweepingConsent_UnknownChannel_ChannelNotSupported(final EnrolmentChannel enrolmentChannel) {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, enrolmentChannel.name(), sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));
    }

    @Test
    public void VerifySweepingConsent_NoChannel_NotFound() {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, "", sharedKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void VerifySweepingConsent_UserLoggedOut_Unauthorised() {

        final String scaChallengeId = issueSweepingConsent();

        AuthenticationService.logout(secretKey, buyer.getRight());

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifySweepingConsent_InvalidApiKey_Unauthorised() {
        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifySweepingConsent_EmptyApiKey_Unauthorised() {
        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifySweepingConsent_NoApiKey_Unauthorised() {
        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallengeNoApiKey(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifySweepingConsent_DifferentProgrammeApiKey_Unauthorised() {
        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifySweepingConsent_BackofficeImpersonator_Unauthorised() {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        getBackofficeImpersonateToken(buyer.getLeft(), IdentityType.CORPORATE), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifySweepingConsent_WrongVerificationCode_VerificationCodeInvalid() {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(RandomStringUtils.randomNumeric(6)),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifySweepingConsent_InvalidVerificationCode_VerificationCodeInvalid() {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(RandomStringUtils.randomAlphabetic(6)),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifySweepingConsent_EmptyVerificationCode_BadRequest() {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.builder().build(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void VerifySweepingConsent_NoVerificationCode_VerificationCodeInvalid() {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(""),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 7})
    public void VerifySweepingConsent_VerificationCodeLengthChecks_VerificationCodeInvalid(final int verificationCodeLength) {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(RandomStringUtils.randomNumeric(verificationCodeLength)),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifySweepingConsent_NoVerificationCodeModel_BadRequest() {

        final String scaChallengeId = issueSweepingConsent();

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(null),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void VerifySweepingConsent_DeactivatedBuyer_Unauthorised() {

        final String scaChallengeId = issueSweepingConsent();

        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(false, "TEMPORARY"),
                buyer.getLeft(), innovatorToken);

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifySweepingConsent_ReactivatedBuyer_Success() {

        final String scaChallengeId = issueSweepingConsent();

        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(false, "TEMPORARY"),
                buyer.getLeft(), innovatorToken);

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);

        InnovatorHelper.activateCorporate(new ActivateIdentityModel(false),
                buyer.getLeft(), innovatorToken);

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private String issueSweepingConsent() {
        return issueSweepingConsent(buyer);
    }

    private String issueSweepingConsent(final Pair<String, String> identity) {
        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(identity.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, identity.getRight()).jsonPath().getString("accounts[0].id");

        return SweepingConsentHelper.issueSweepingConsentChallenge(linkedAccountId, managedAccountId, identity.getRight(), sharedKey);
    }

    private String issueSweepingConsent(final String buyerId,
                                        final String authUserToken) {
        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyerId, secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, authUserToken).jsonPath().getString("accounts[0].id");

        return SweepingConsentHelper.issueSweepingConsentChallenge(linkedAccountId, managedAccountId, authUserToken, sharedKey);
    }
}
