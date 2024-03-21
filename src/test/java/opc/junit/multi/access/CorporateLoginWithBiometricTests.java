package opc.junit.multi.access;

import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.UserId;
import opc.models.backoffice.IdentityModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.Identity;
import opc.models.shared.LoginWithBiometricModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import opc.models.webhook.WebhookBiometricLoginEventModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class CorporateLoginWithBiometricTests extends AbstractLoginWithBiometricTests {

    @Test
    public void LoginWithBiometric_SemiUsersLoginWithIdentityId_ChannelNotRegistered() {

        final IdentityDetails identity = getBiometricIdentity(semiScaPasscodeApp);
        CorporatesHelper.verifyEmail(identity.getEmail(), semiScaPasscodeApp.getSecretKey());
        final String firstCorporateManagedAccount = ManagedAccountsHelper.createManagedAccount(
                semiScaPasscodeApp.getCorporatePayneticsEeaManagedAccountsProfileId(), Currency.EUR.name(),
                semiScaPasscodeApp.getSecretKey(), identity.getToken());

        final CreateCorporateModel createCorporateUser =CreateCorporateModel.DefaultCreateCorporateModel(semiScaPasscodeApp.getCorporatesProfileId())
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setName(identity.getName())
                        .setSurname(identity.getSurname()).build()).build();

        final Pair<String, String> secondCorporate =
                CorporatesHelper.createBiometricEnrolledVerifiedCorporate(createCorporateUser, semiScaPasscodeApp.getSecretKey(), semiScaPasscodeApp.getSharedKey());
        CorporatesHelper.verifyEmail(createCorporateUser.getRootUser().getEmail(), semiScaPasscodeApp.getSecretKey());

        final String secondCorporateManagedAccount = ManagedAccountsHelper.createManagedAccount(
                semiScaPasscodeApp.getCorporatePayneticsEeaManagedAccountsProfileId(), Currency.EUR.name(),
                semiScaPasscodeApp.getSecretKey(), secondCorporate.getRight());

        AdminHelper.setSca(adminToken, semiScaPasscodeApp.getProgrammeId(), true, true);

        //first corporate tries to get managed account with step up token from biometric
        ManagedAccountsService.getManagedAccount(semiScaPasscodeApp.getSecretKey(), firstCorporateManagedAccount, identity.getToken())
                .then()
                .statusCode(SC_OK);

        //second corporate tries to get managed account with step up token from biometric
        ManagedAccountsService.getManagedAccount(semiScaPasscodeApp.getSecretKey(), secondCorporateManagedAccount, secondCorporate.getRight())
                .then()
                .statusCode(SC_OK);

        final String adminImpersonatedTenantToken =
                AdminService.impersonateTenant(semiScaPasscodeApp.getInnovatorId(), AdminService.loginAdmin());

        AdminService.linkUseridToCorporateSemi(new UserId(secondCorporate.getLeft()), identity.getId(), adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LoginWithBiometricModel loginSecondCorporate = LoginWithBiometricModel.loginWithBiometricSemiModel(identity.getEmail(), secondCorporate.getLeft());

        final String secondCorporateLoginId = AuthenticationService.loginWithBiometric(loginSecondCorporate, semiScaPasscodeApp.getSecretKey())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("challengeId");

        final long timestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptOkayLoginChallenge(semiScaPasscodeApp.getSecretKey(), secondCorporateLoginId);

        final WebhookBiometricLoginEventModel event = getWebhookResponse(timestamp, secondCorporate.getLeft());

        assertVerifiedChallenge(event, secondCorporateLoginId, identity.getId(), UserType.ROOT);

        final String secondCorpAuthToken = event.getAuthToken();

        final String secondCorpStepUpToken = AuthenticationHelper.requestAccessToken(
                new Identity(new IdentityModel(secondCorporate.getLeft(), identity.getIdentityType())),
                semiScaPasscodeApp.getSecretKey(), secondCorpAuthToken);

        //second corporate tries to get its own managed account with step up token after login with biometric
        ManagedAccountsService.getManagedAccount(semiScaPasscodeApp.getSecretKey(), secondCorporateManagedAccount, secondCorpStepUpToken)
                .then()
                .statusCode(SC_OK);

        //second corporate tries to get first corporate's managed account with step up token after login with biometric
        ManagedAccountsService.getManagedAccount(semiScaPasscodeApp.getSecretKey(), firstCorporateManagedAccount, secondCorpStepUpToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        final LoginWithBiometricModel loginFirstCorporate = LoginWithBiometricModel.loginWithBiometricSemiModel(identity.getEmail(), identity.getId());
        final String firstCorporateLoginId = AuthenticationService.loginWithBiometric(loginFirstCorporate, semiScaPasscodeApp.getSecretKey())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("challengeId");

        final long newTimestamp = Instant.now().toEpochMilli();

        SimulatorHelper.acceptOkayLoginChallenge(semiScaPasscodeApp.getSecretKey(), firstCorporateLoginId);

        final WebhookBiometricLoginEventModel newEvent = getWebhookResponse(newTimestamp, identity.getId());

        assertVerifiedChallenge(newEvent, firstCorporateLoginId, identity.getId(), UserType.ROOT);

        final String firstCorpAuthToken = newEvent.getAuthToken();

        final String firstCorpStepUpToken = AuthenticationHelper.requestAccessToken(
                new Identity(new IdentityModel(identity.getId(), identity.getIdentityType())),
                semiScaPasscodeApp.getSecretKey(), firstCorpAuthToken);

        //first corporate tries to get its own managed account with step up token after login with biometric
        ManagedAccountsService.getManagedAccount(semiScaPasscodeApp.getSecretKey(), firstCorporateManagedAccount, firstCorpStepUpToken)
                .then()
                .statusCode(SC_OK);

        //second corporate tries to get second corporate's managed account with step up token after login with biometric
        ManagedAccountsService.getManagedAccount(semiScaPasscodeApp.getSecretKey(), secondCorporateManagedAccount, firstCorpStepUpToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Override
    protected IdentityDetails getBiometricIdentity(final ProgrammeDetailsModel programme) {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createBiometricEnrolledVerifiedCorporate(createCorporateModel, programme.getSecretKey(), programme.getSharedKey());

        return IdentityDetails.generateDetails(createCorporateModel.getRootUser().getEmail(), corporate.getLeft(),
                corporate.getRight(), IdentityType.CORPORATE, createCorporateModel.getRootUser().getName(),
                createCorporateModel.getRootUser().getSurname());
    }

    @Override
    protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(createCorporateModel.getRootUser().getEmail(), corporate.getLeft(),
                corporate.getRight(), IdentityType.CORPORATE, createCorporateModel.getRootUser().getName(),
                createCorporateModel.getRootUser().getSurname());
    }

    @Override
    protected CreateManagedAccountModel getManagedAccountModel(final ProgrammeDetailsModel programme) {
        return CreateManagedAccountModel
                .DefaultCreateManagedAccountModel(programme.getCorporatePayneticsEeaManagedAccountsProfileId(), Currency.EUR.name())
                .build();
    }

    @Override
    protected CreateManagedCardModel getManagedCardModel(final ProgrammeDetailsModel programme) {
        return CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(programme.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), Currency.EUR.name())
                .build();
    }

    @AfterEach
    public void tearDown() {
        AdminHelper.setSca(adminToken, semiScaPasscodeApp.getProgrammeId(), false, false);
    }
}
