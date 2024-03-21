package fpi.paymentrun.uicomponents.sweepingconsent;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.simulator.SimulatorHelper;
import fpi.paymentrun.models.IssueSweepingConsentChallengeModel;
import fpi.paymentrun.models.VerifySweepingConsentModel;
import fpi.paymentrun.services.uicomponents.SweepingConsentService;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_UI_COMPONENTS)
public class IssueSweepingConsentChallengeTests extends BasePaymentRunSetup {

    private static final String ENROLMENT_CHANNEL = EnrolmentChannel.SMS.name();

    private static Pair<String, String> buyer;

    @BeforeAll
    public static void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());
    }

    @Test
    public void IssueSweepingConsent_MultipleRolesBuyer_Success() {
        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("scaChallengeId", notNullValue());
    }

    @Test
    public void IssueSweepingConsent_ValidRoleBuyer_Success() {
        Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignControllerRole(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("scaChallengeId", notNullValue());
    }

    @Test
    public void IssueSweepingConsent_ValidRoleAuthUser_Success() {

        final Pair<String, String> user =
                BuyerAuthorisedUserHelper.createEnrolledSteppedUpUser(secretKey, buyer.getRight());
        BuyerAuthorisedUserHelper.assignControllerRole(user.getLeft(), secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, user.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        user.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("scaChallengeId", notNullValue());
    }

    @Test
    public void IssueSweepingConsent_MultipleRolesAuthUser_Success() {

        final Pair<String, String> user =
                BuyerAuthorisedUserHelper.createEnrolledSteppedUpUser(secretKey, buyer.getRight());
        BuyerAuthorisedUserHelper.assignAllRoles(user.getLeft(), secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, user.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        user.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("scaChallengeId", notNullValue());
    }

    @Test
    public void IssueSweepingConsent_IncorrectRoleAuthUser_Forbidden() {

        final Pair<String, String> user =
                BuyerAuthorisedUserHelper.createEnrolledSteppedUpUser(secretKey, buyer.getRight());
        BuyerAuthorisedUserHelper.assignAllRoles(user.getLeft(), secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, user.getRight()).jsonPath().getString("accounts[0].id");

        BuyerAuthorisedUserHelper.assignCreatorRole(user.getLeft(), secretKey, buyer.getRight());
        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        user.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssueSweepingConsent_AdminRoleBuyer_Forbidden() {
        Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignControllerRole(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        BuyersHelper.assignAdminRole(secretKey, buyer.getRight());
        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssueSweepingConsent_IncorrectRoleBuyer_Forbidden() {
        Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignControllerRole(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        BuyersHelper.assignCreatorRole(secretKey, buyer.getRight());
        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void IssueSweepingConsent_AlreadyIssuedNotVerified_Success() {

        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        final String scaChallengeId =
                SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                                buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                        .jsonPath()
                        .getString("scaChallengeId");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CREATED)
                .body("scaChallengeId", equalTo(scaChallengeId));
    }

    @Test
    public void IssueSweepingConsent_AlreadyIssuedAndVerified_StateInvalid() {

        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        final String scaChallengeId =
                SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                                buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                        .jsonPath()
                        .getString("scaChallengeId");

        SweepingConsentService.verifySweepingConsentChallenge(VerifySweepingConsentModel.defaultVerificationModel(),
                        buyer.getRight(), scaChallengeId, ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("STATE_INVALID"));
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = {"EMAIL", "UNKNOWN"})
    public void IssueSweepingConsent_UnknownChannel_ChannelNotSupported(final EnrolmentChannel enrolmentChannel) {

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), enrolmentChannel.name(), sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));
    }

    @Test
    public void IssueSweepingConsent_NoChannel_NotFound() {

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), "", sharedKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void IssueSweepingConsent_UserLoggedOut_Unauthorised() {

        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        AuthenticationHelper.logout(secretKey, buyer.getRight());

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueSweepingConsent_DifferentProgrammeApiKey_Unauthorised() {

        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        AuthenticationHelper.logout(secretKey, buyer.getRight());

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKeyPluginsScaApp)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueSweepingConsent_InvalidApiKey_Unauthorised() {

        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        AuthenticationHelper.logout(secretKey, buyer.getRight());

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueSweepingConsent_EmptyApiKey_Unauthorised() {

        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        AuthenticationHelper.logout(secretKey, buyer.getRight());

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueSweepingConsent_NoApiKey_Unauthorised() {

        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        AuthenticationHelper.logout(secretKey, buyer.getRight());

        SweepingConsentService.issueSweepingConsentChallengeNoApiKey(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void IssueSweepingConsent_UnknownLinkedAccountId_LinkedAccountNotFound() {

        final String linkedAccountId =
                RandomStringUtils.randomNumeric(24);

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("LINKED_ACCOUNT_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(ints = {23, 25})
    public void IssueSweepingConsent_InvalidLinkedAccountId_BadRequest(final int accountSize) {

        final String linkedAccountId =
                RandomStringUtils.randomNumeric(accountSize);

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("linkedAccountId"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void IssueSweepingConsent_NoLinkedAccountId_BadRequest() {

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel("", managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("linkedAccountId"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void IssueSweepingConsent_OtherBuyerLinkedAccountId_LinkedAccountNotFound() {

        final Pair<String, String> otherBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, otherBuyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(otherBuyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("LINKED_ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void IssueSweepingConsent_OtherBuyerAuthUser_LinkedAccountNotFound() {

        final Pair<String, String> otherBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        final Pair<String, String> user =
                BuyerAuthorisedUserHelper.createEnrolledSteppedUpUser(secretKey, otherBuyer.getRight());
        BuyerAuthorisedUserHelper.assignAllRoles(user.getLeft(), secretKey, otherBuyer.getRight());

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, buyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        user.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("LINKED_ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void IssueSweepingConsent_UnknownManagedAccountId_ManagedAccountNotFound() {

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                RandomStringUtils.randomNumeric(18);

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MANAGED_ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void IssueSweepingConsent_InvalidManagedAccountId_ManagedAccountNotFound() {

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                RandomStringUtils.randomAlphabetic(18);

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MANAGED_ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void IssueSweepingConsent_NoManagedAccountId_ManagedAccountNotFound() {

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, ""),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MANAGED_ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void IssueSweepingConsent_OtherBuyerManagedAccountId_ManagedAccountNotFound() {

        final Pair<String, String> otherBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);

        final String linkedAccountId =
                SimulatorHelper.createLinkedAccount(buyer.getLeft(), secretKey).getLeft();

        final String managedAccountId =
                ManagedAccountsHelper.getManagedAccounts(secretKey, otherBuyer.getRight()).jsonPath().getString("accounts[0].id");

        SweepingConsentService.issueSweepingConsentChallenge(issueConsentModel(linkedAccountId, managedAccountId),
                        buyer.getRight(), ENROLMENT_CHANNEL, sharedKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MANAGED_ACCOUNT_NOT_FOUND"));
    }

    private IssueSweepingConsentChallengeModel issueConsentModel(final String linkedAccountId,
                                                                 final String managedAccountId) {
        return IssueSweepingConsentChallengeModel.builder()
                .linkedAccountId(linkedAccountId)
                .managedAccountId(managedAccountId)
                .build();
    }
}
