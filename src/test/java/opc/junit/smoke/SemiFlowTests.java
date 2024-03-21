package opc.junit.smoke;

import commons.enums.Currency;

import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.admin.CorporateWithKybResponseModel;
import opc.models.admin.UserId;
import opc.models.backoffice.IdentityModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.sends.SendFundsModel;

import opc.models.shared.CurrencyAmount;
import opc.models.shared.Identity;
import opc.models.shared.LoginModel;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.PasswordModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.CorporatesService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.SemiService;
import opc.services.multi.SendsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SemiFlowTests extends BaseSmokeSetup {
    private static String adminImpersonatedTenantToken;

    @BeforeAll
    public static void BeforeAll() {
        adminImpersonatedTenantToken = AdminService.impersonateTenant(semiInnovatorId, AdminService.loginAdmin());
    }

    @Test
    void CreateCorporateRootUser_NewIdentityLinked_Success() {
        //Root user is created
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(semiCorporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, semiSecretKey);

        CorporatesHelper.verifyEmail(corporateRootEmail, semiSecretKey);

        //New identity linked to root user with the same email, name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(corporateRootEmail, name, surname);

        CorporatesService.createCorporate(createCorporateUser, semiSecretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    void ExistingCorporateRootUser_NewIdentityLinked_Success() {
        //Get the existing corporate root user with mapped beneficiary
        final CorporateWithKybResponseModel corporate =
                getExistingCorporateWithBeneficiary(semiCorporatesProfileId);

        final String corporateRootEmail = corporate.getCorporate().getRootUser().getEmail();
        final String name = corporate.getCorporate().getRootUser().getName();
        final String surname = corporate.getCorporate().getRootUser().getSurname();

        //New identity linked to root user with the same email, name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(corporateRootEmail, name, surname);

        CorporatesService.createCorporate(createCorporateUser, semiSecretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    void LinkUserIdToCorporate_UserLinked_Success() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(semiCorporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, semiSecretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, semiSecretKey);

        //New user linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUserForLinking(name, surname);

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, semiSecretKey);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();
        final String authenticationTokenUser = KybVerifiedCorporateForLinking.getRight();

        CorporatesHelper.verifyEmail(corporateUserEmail, semiSecretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String corporateUserLinkedEmail = CorporatesService.getCorporates(semiSecretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateRootEmail, corporateUserLinkedEmail);

    }

    @Test
    void LinkUserIdToExistingCorporate_UserLinked_Success() {
        //Get the existing corporate root user with mapped beneficiary
        final CorporateWithKybResponseModel corporate =
                getExistingCorporateWithBeneficiary(semiCorporatesProfileId);

        final String corporateRootEmail = corporate.getCorporate().getRootUser().getEmail();
        final String name = corporate.getCorporate().getRootUser().getName();
        final String surname = corporate.getCorporate().getRootUser().getSurname();
        final String corporateId = corporate.getCorporate().getId().getId();

        //New user linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUserForLinking(name, surname);

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, semiSecretKey);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();
        final String authenticationTokenUser = KybVerifiedCorporateForLinking.getRight();

        CorporatesHelper.verifyEmail(corporateUserEmail, semiSecretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String corporateUserLinkedEmail = CorporatesService.getCorporates(semiSecretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateRootEmail, corporateUserLinkedEmail);

    }

    @Test
    void UnlinkUserIdFromCorporate_UserUnlinked_Success() {
        //Root user is created and email is verified
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(semiCorporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> KybVerifiedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, semiSecretKey);
        final String corporateId = KybVerifiedCorporate.getLeft();
        CorporatesHelper.verifyEmail(corporateRootEmail, semiSecretKey);

        //New user linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUserForLinking(name, surname);

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, semiSecretKey);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();
        final String authenticationTokenUser = KybVerifiedCorporateForLinking.getRight();

        CorporatesHelper.verifyEmail(corporateUserEmail, semiSecretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String corporateUserLinkedEmail = CorporatesService.getCorporates(semiSecretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateRootEmail, corporateUserLinkedEmail);

        //Unlink user from root user
        AdminService.unlinkUseridFromCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        final String corporateUserUnlinkedEmail = CorporatesService.getCorporates(semiSecretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateUserEmail, corporateUserUnlinkedEmail);
    }

    @Test
    void UnlinkUserIdFromExistingCorporate_UserUnlinked_Success() {
        //Get the existing corporate root user with mapped beneficiary
        final CorporateWithKybResponseModel corporate =
                getExistingCorporateWithBeneficiary(semiCorporatesProfileId);

        final String corporateRootEmail = corporate.getCorporate().getRootUser().getEmail();
        final String name = corporate.getCorporate().getRootUser().getName();
        final String surname = corporate.getCorporate().getRootUser().getSurname();
        final String corporateId = corporate.getCorporate().getId().getId();

        //New user linked to root user with the same name and surname
        final CreateCorporateModel createCorporateUser =
                createCorporateUserForLinking(name, surname);

        final Pair<String, String> KybVerifiedCorporateForLinking = CorporatesHelper.createKybVerifiedCorporate(createCorporateUser, semiSecretKey);
        final String corporateUserEmail = createCorporateUser.getRootUser().getEmail();
        final String corporateUserId = KybVerifiedCorporateForLinking.getLeft();
        final String authenticationTokenUser = KybVerifiedCorporateForLinking.getRight();

        CorporatesHelper.verifyEmail(corporateUserEmail, semiSecretKey);

        AdminService.linkUseridToCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String corporateUserLinkedEmail = CorporatesService.getCorporates(semiSecretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateRootEmail, corporateUserLinkedEmail);

        //Unlink user from root user
        AdminService.unlinkUseridFromCorporateSemi(new UserId(corporateUserId), corporateId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        final String corporateUserUnlinkedEmail = CorporatesService.getCorporates(semiSecretKey, authenticationTokenUser).jsonPath().getString("rootUser.email");

        assertEquals(corporateUserEmail, corporateUserUnlinkedEmail);
    }

    @Test
    public void LoginWithPassword_UserBelongToCorporate_Success() {
        //Root user is created - linking with first one
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(semiCorporatesProfileId).build();

        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, semiSecretKey);
        final String identityId = authenticatedCorporate.getLeft();
        final String corporatePassword = TestHelper.getDefaultPassword(secretKey);

        CorporatesHelper.verifyEmail(corporateRootEmail, semiSecretKey);

        //New user linked to root user with the same name, surname and email
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(corporateRootEmail, name, surname);

        final String identityIdLinked = CorporatesHelper.createCorporate(createCorporateUser, semiSecretKey);

        CorporatesHelper.verifyKyb(semiSecretKey, identityIdLinked);
        final String corporateRootEmailLinked = createCorporateUser.getRootUser().getEmail();

        //success login with linked corporate using id from first corporate
        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, semiSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId))
                .body("tokenType", equalTo("AUTH"));

        //success login with linked corporate using id from second corporate
        final LoginModel loginModelWithSecondIdentity = new LoginModel(corporateRootEmailLinked, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithSecondIdentity, semiSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId))
                .body("tokenType", equalTo("AUTH"));
    }

    @Test
    public void LoginWithPassword_UserBelongToExistingCorporate_Success() {
        //Get the existing corporate root user with mapped beneficiary
        final CorporateWithKybResponseModel corporate =
                getExistingCorporateWithBeneficiary(semiCorporatesProfileId);

        final String corporateRootEmail = corporate.getCorporate().getRootUser().getEmail();
        final String name = corporate.getCorporate().getRootUser().getName();
        final String surname = corporate.getCorporate().getRootUser().getSurname();
        final String identityId = corporate.getCorporate().getId().getId();

        final String corporatePassword = TestHelper.getDefaultPassword(secretKey);

        //New user linked to root user with the same name, surname and email
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(corporateRootEmail, name, surname);

        final String identityIdLinked = CorporatesHelper.createCorporate(createCorporateUser, semiSecretKey);

        CorporatesHelper.verifyKyb(semiSecretKey, identityIdLinked);
        final String corporateRootEmailLinked = createCorporateUser.getRootUser().getEmail();

        //success login with linked corporate using id from first corporate
        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, semiSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId))
                .body("tokenType", equalTo("AUTH"));

        //success login with linked corporate using id from second corporate
        final LoginModel loginModelWithSecondIdentity = new LoginModel(corporateRootEmailLinked, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithSecondIdentity, semiSecretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(identityId))
                .body("tokenType", equalTo("AUTH"));
    }

    @Test
    public void GetActiveIdentities_LinkedCorporatesDeactivated_Success() {
        //Root user is created - linking with first one
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(semiCorporatesProfileId).build();

        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, semiSecretKey);
        final String firstSemiCorporate = authenticatedCorporate.getLeft();
        final String corporatePassword = TestHelper.getDefaultPassword(secretKey);

        CorporatesHelper.verifyEmail(corporateRootEmail, semiSecretKey);

        //New users linked to root user with the same name, surname and email
        final CreateCorporateModel createCorporateUser =
                createCorporateUser(corporateRootEmail, name, surname);

        final String secondLinkedSemiCorporate = CorporatesHelper.createCorporate(createCorporateUser, semiSecretKey);
        final String thirdLinkedSemiCorporate = CorporatesHelper.createCorporate(createCorporateUser, semiSecretKey);

        final String authToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, semiSecretKey);

        //Get linked identities
        SemiService.getLinkedIdentities(semiSecretKey, Optional.empty(), authToken)
                .then()
                .statusCode(SC_OK)
                .body("id[0].id", equalTo(firstSemiCorporate))
                .body("id[1].id", equalTo(secondLinkedSemiCorporate))
                .body("id[2].id", equalTo(thirdLinkedSemiCorporate));

        //Deactivate second and third linked corporates
        AdminHelper.deactivateCorporate(
                new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"), secondLinkedSemiCorporate, adminImpersonatedTenantToken);
        AdminHelper.deactivateCorporate(
                new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"), thirdLinkedSemiCorporate, adminImpersonatedTenantToken);

        //Get linked identities and check that second and third linked deactivated corporates do not display
        SemiService.getLinkedIdentities(semiSecretKey, Optional.empty(), authToken)
                .then()
                .statusCode(SC_OK)
                .body("id[0].id", equalTo(firstSemiCorporate))
                .body("id[1].id", equalTo(null))
                .body("id[2].id", equalTo(null));
    }

    @Test
    public void SendOwt_FirstIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(semiCorporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, semiSecretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, semiSecretKey);

        //create and fund MA for the first identity
        final String managedAccount =
                ManagedAccountsHelper.createManagedAccount(semiCorporateManagedAccountProfileId, Currency.EUR.name(), semiSecretKey, firstCorporate.getRight());
        AdminHelper.fundManagedAccount(semiInnovatorId, managedAccount, Currency.EUR.name(), depositAmount);

        //create second corporate
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), semiSecretKey, semiCorporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, semiSecretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                semiSecretKey, corporateAuthenticationTokenAfterLinking);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(semiOutgoingWireTransfersProfileId, managedAccount,
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, semiSecretKey,
                        firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendOwt_ExistingRootUserSecondIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Get the existing corporate root user with mapped beneficiaryId
        final CorporateWithKybResponseModel corporate =
                getExistingCorporateWithBeneficiary(semiCorporatesProfileId);

        final String corporateRootEmail = corporate.getCorporate().getRootUser().getEmail();
        final String name = corporate.getCorporate().getRootUser().getName();
        final String surname = corporate.getCorporate().getRootUser().getSurname();
        final String existingCorporateToken = AuthenticationHelper.login(corporateRootEmail,
                TestHelper.getDefaultPassword(secretKey), semiSecretKey);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, existingCorporateToken, semiSecretKey, semiCorporatesProfileId);

        //create MA for the second identity
        final String managedAccount =
                ManagedAccountsHelper.createManagedAccount(semiCorporateManagedAccountProfileId, Currency.EUR.name(), semiSecretKey, secondCorporateAccessToken);
        AdminHelper.fundManagedAccount(semiInnovatorId, managedAccount, Currency.EUR.name(), depositAmount);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(semiOutgoingWireTransfersProfileId, managedAccount,
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, semiSecretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendFunds_AccessTokenBeforeLinking_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP and create source MA
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(semiCorporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, semiSecretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, semiSecretKey);

        final String sourceManagedAccount =
                ManagedAccountsHelper.createManagedAccount(semiCorporateManagedAccountProfileId, Currency.EUR.name(), semiSecretKey, firstCorporate.getRight());
        AdminHelper.fundManagedAccount(semiInnovatorId, sourceManagedAccount, Currency.EUR.name(), depositAmount);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                semiSecretKey, semiCorporatesProfileId);

        //create destination MA for the second identity
        final String destinationManagedAccount = ManagedAccountsHelper.createManagedAccount(
                semiCorporateManagedAccountProfileId, Currency.EUR.name(), semiSecretKey, secondCorporateAccessToken);

        //Send funds by the first identity with Access Token issued before linking
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(semiSendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount, MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, semiSecretKey, firstCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    private static String createLinkedCorporate(final String name,
                                                final String surname,
                                                final String corporateRootEmail,
                                                final String authToken,
                                                final String secretKey,
                                                final String corporatesProfileId) {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setSurname(surname)
                        .setName(name)
                        .setEmail(corporateRootEmail)
                        .build())
                .build();

        final String linkedIdentityId = CorporatesHelper.createKybVerifiedLinkedCorporate(createCorporateModel, secretKey);

        return AuthenticationHelper.requestAccessToken
                (new Identity(new IdentityModel(linkedIdentityId, IdentityType.CORPORATE)),
                        secretKey, authToken);
    }

    private static CreateCorporateModel createCorporateUser(String email, String name, String surname) {
        return
                CreateCorporateModel.DefaultCreateCorporateModel(semiCorporatesProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(email)
                                .setName(name)
                                .setSurname(surname)
                                .build()).build();

    }

    private static CreateCorporateModel createCorporateUserForLinking(String name, String surname) {
        return
                CreateCorporateModel.DefaultCreateCorporateModel(semiCorporatesProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setName(name)
                                .setSurname(surname).build()).build();
    }

}
