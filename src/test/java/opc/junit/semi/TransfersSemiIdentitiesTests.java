package opc.junit.semi;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.admin.ScaConfigModel;
import opc.models.admin.UserId;
import opc.models.backoffice.IdentityModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.Identity;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.VerificationModel;
import opc.services.admin.AdminService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.SendsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class TransfersSemiIdentitiesTests extends BaseSemiSetup {

    /**
     * SEMI (Single Email Multiple Identities) tests for AccessToken flow with all type of transactions.
     */
    private static final String OTP_CHANNEL = EnrolmentChannel.SMS.name();
    private static final String VERIFICATION_CODE = "123456";
    private static final String AUTHY_CHANNEL = EnrolmentChannel.AUTHY.name();
    private static final String BIOMETRIC_CHANNEL = EnrolmentChannel.BIOMETRIC.name();
    private static String passcodeAppInnovatorToken;

    @BeforeAll
    public static void Setup() {
        passcodeAppInnovatorToken = InnovatorHelper.loginInnovator(passcodeAppInnovatorEmail, passcodeAppInnovatorPassword);
        enableSendsSca();
    }

    @AfterAll
    public static void TearDown() {
        disableSendsSca();
    }

    //Common cases to check auth/access tokens with OWT
    @Test
    public void SendOwt_FirstIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //create second corporate
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporateAuthenticationTokenAfterLinking);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }


    @Test
    public void SendOwt_SecondIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendOwt_AuthUserIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String nameRootUser = createCorporateModel.getRootUser().getName();
        final String surnameRootUser = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create corporate #2 linked to the #1
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootEmail, corporateRootUser.getRight(), secretKey, corporatesProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight(), secretKey);
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //create MA for the linked authUser
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), authUserAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by linked authUser
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendOwt_AuthTokenAfterLinking_Forbidden() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //create second corporate
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //auth token for the first Identity after linking the second Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        corporateAuthenticationTokenAfterLinking, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    /**
     * The Scenario to check if AccessToken issued before linking new identity (non-SEMI login) works after linking
     * in case the first identity is not logged out after linking.
     */
    @Test
    public void SendOwt_AccessTokenBeforeLinking_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //create second corporate
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        firstCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    //OTP challenges
    @Test
    public void OwtViaOtp_FirstIdentitySendOwtSecondIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporateAuthenticationTokenAfterLinking);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the second identity
        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(owtId, OTP_CHANNEL, secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaOtp_SecondIdentitySendOwtFirstIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporateAuthenticationTokenAfterLinking);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the first identity
        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(owtId, OTP_CHANNEL, secretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaOtp_SeveralLinkedIdentitiesCrossIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create third linked corporate and get accessToken
        final String thirdCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the third identity
        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(owtId, OTP_CHANNEL, secretKey, thirdCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaOtp_AuthUserSendRootIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String nameRootUser = createCorporateModel.getRootUser().getName();
        final String surnameRootUser = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootEmail, corporateRootUser.getRight(), secretKey, corporatesProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight(), secretKey);
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        final String rootIdentityToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //create MA for the authUser
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), authUserAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the authUser
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the third identity
        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(owtId, OTP_CHANNEL, secretKey, rootIdentityToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaOtp_FirstIdentityStartOwtVerificationSecondIdentityVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporateAuthenticationTokenAfterLinking);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the first identity
        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(owtId, OTP_CHANNEL, secretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //verify OWT by the second identity
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), owtId,
                        OTP_CHANNEL, secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaOtp_SecondIdentityStartOwtVerificationFirstIdentityVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the second identity
        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(owtId, OTP_CHANNEL, secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //verify OWT by the first identity
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), owtId,
                        OTP_CHANNEL, secretKey, firstCorporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaOtp_SecondIdentityStartOwtVerificationAuthUserVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String nameRootUser = createCorporateModel.getRootUser().getName();
        final String surnameRootUser = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(nameRootUser, surnameRootUser, corporateRootEmail, corporateRootUser.getRight(), secretKey, corporatesProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(nameRootUser, surnameRootUser, authUserCorporate.getRight(), secretKey);
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //create MA for the authUser
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the authUser
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the third identity
        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(owtId, OTP_CHANNEL, secretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaOtp_SeveralLinkedIdentitiesCrossIdentityVerification_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtp(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create third linked corporate and get accessToken
        final String thirdCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the second identity
        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(owtId, OTP_CHANNEL, secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //verify OWT by the third identity
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), owtId,
                        OTP_CHANNEL, secretKey, thirdCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    //AUTHY challenges
    @Test
    public void OwtViaAuthy_FirstIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Authy
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //create second corporate
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporateAuthenticationTokenAfterLinking);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //first identity starts verification
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, AUTHY_CHANNEL, secretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, owtId, firstCorporateAccessToken)
                .then()
                .statusCode(SC_OK)
                .body("state", CoreMatchers.equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void OwtViaAuthy_SecondIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Authy
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //second identity starts verification
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, AUTHY_CHANNEL, secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, owtId, secondCorporateAccessToken)
                .then()
                .statusCode(SC_OK)
                .body("state", CoreMatchers.equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void OwtViaAuthy_AuthUserIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Authy
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), secretKey);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //create MA
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), authUserAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //starts verification
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, AUTHY_CHANNEL, secretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, owtId, authUserAccessToken)
                .then()
                .statusCode(SC_OK)
                .body("state", CoreMatchers.equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void OwtViaAuthy_FirstIdentitySendOwtSecondIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Authy
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporateAuthenticationTokenAfterLinking);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the second identity
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, AUTHY_CHANNEL, secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaAuthy_AuthUserIdentitySendSecondIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Authy
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), secretKey);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //create MA
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), authUserAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //starts verification
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, AUTHY_CHANNEL, secretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaAuthy_FirstIdentitySendAuthUserStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Authy
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporatesProfileId, secretKey);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), secretKey);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        final String firstIdentityAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, authTokenAfterLinking);

        //create MA
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), firstIdentityAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        firstIdentityAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //starts verification
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, AUTHY_CHANNEL, secretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaAuthy_SecondIdentitySendOwtFirstIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Authy
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the first identity
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, AUTHY_CHANNEL, secretKey, firstCorporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaAuthy_SeveralLinkedIdentitiesCrossIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Authy
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthy(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create third linked corporate and get accessToken
        final String thirdCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), secretKey, corporatesProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, secretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the third identity
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, AUTHY_CHANNEL, secretKey, thirdCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    //BIOMETRIC challenges
    @Test
    public void OwtViaBiometric_FirstIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), passcodeAppSecretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //create second corporate
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(), passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, corporateAuthenticationTokenAfterLinking);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey,
                        firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //first identity starts verification
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, owtId, firstCorporateAccessToken)
                .then()
                .statusCode(SC_OK)
                .body("state", CoreMatchers.equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void OwtViaBiometric_SecondIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, passcodeAppSecretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //second identity starts verification
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, owtId, secondCorporateAccessToken)
                .then()
                .statusCode(SC_OK)
                .body("state", CoreMatchers.equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void OwtViaBiometric_AuthUserIdentitySend_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), passcodeAppSecretKey);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //create MA
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), authUserAccessToken, passcodeAppSecretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //send OWT
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey,
                        authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verification
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        OutgoingWireTransfersService.getOutgoingWireTransfer(passcodeAppSecretKey, owtId, authUserAccessToken)
                .then()
                .statusCode(SC_OK)
                .body("state", CoreMatchers.equalTo("PENDING_CHALLENGE"));
    }

    @Test
    public void OwtViaBiometric_FirstIdentitySendOwtSecondIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), passcodeAppSecretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, corporateAuthenticationTokenAfterLinking);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey,
                        firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the second identity
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaBiometric_FirstIdentitySendOwtAuthUserStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create and fund MA for the first identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporate.getRight(), passcodeAppSecretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //create second corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), passcodeAppSecretKey);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //access token for the first Identity
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey,
                        firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the second identity
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaBiometric_AuthUserSendOwtSecondIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        final String secondIdentityAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), passcodeAppSecretKey);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //create and fund MA
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), authUserAccessToken, passcodeAppSecretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //send OWT by the first identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey,
                        authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the second identity
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, secondIdentityAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaBiometric_SecondIdentitySendOwtFirstIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, passcodeAppSecretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the first identity
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, firstCorporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void OwtViaBiometric_SeveralLinkedIdentitiesCrossIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create third linked corporate and get accessToken
        final String thirdCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create MA for the second identity
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, passcodeAppSecretKey);
        fundManagedAccount(managedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //send OWT by the second identity
        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId, managedAccount.getLeft(),
                                Currency.EUR.name(), sendAmount, OwtType.SEPA)
                        .build();

        final String owtId = OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey,
                        secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        //start verify OWT by the third identity
        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(owtId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, thirdCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    //Common cases to check auth/access tokens with Sends
    @Test
    public void SendFunds_FirstIdentityMaToMa_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> manageAccountFirstIdentity = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> manageAccountSecondIdentity = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(manageAccountFirstIdentity.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(manageAccountFirstIdentity.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(manageAccountSecondIdentity.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendFunds_SecondIdentityMaToMa_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> manageAccountFirstIdentity = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> manageAccountSecondIdentity = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(manageAccountSecondIdentity.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(manageAccountSecondIdentity.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(manageAccountFirstIdentity.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendFunds_AuthUserMaToMa_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), secretKeyScaSendsApp);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //access token for the first Identity
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> manageAccountFirstIdentity = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> manageAccountAuthUser = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), authUserAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(manageAccountAuthUser.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(manageAccountAuthUser.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(manageAccountFirstIdentity.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendFunds_AuthTokenAfterLinking_Forbidden() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP and create source MA
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporate.getRight(), secretKeyScaSendsApp);
        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create destination MA for the second identity
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);

        //auth token for the first Identity after linking the second Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);

        //Send funds by the first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    /**
     * The Scenario to check if AccessToken issued before linking new identity (non-SEMI login) works after linking
     * in case the first identity is not logged out after linking.
     */
    @Test
    public void SendFunds_AccessTokenBeforeLinking_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP and create source MA
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporate.getRight(), secretKeyScaSendsApp);
        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create destination MA for the second identity
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);

        //Send funds by the first identity with Access Token issued before linking
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, firstCorporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void SendFundsViaOtp_AccessTokenVerifyChallenges_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP and create source MA
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporate.getRight(), secretKeyScaSendsApp);
        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create destination MA for the second identity
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //Send funds by the first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(sendId, firstCorporateAccessToken, State.PENDING_CHALLENGE);

        //Verify send
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        OTP_CHANNEL, secretKeyScaSendsApp, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertSendState(sendId, firstCorporateAccessToken, State.COMPLETED);
    }

    @Test
    public void SendFundsViaOtp_FirstIdentitySendFundsSecondIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP and create source MA
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporate.getRight(), secretKeyScaSendsApp);
        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create destination MA for the second identity
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //Send funds by the first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaOtp_FirstIdentitySendFundsAuthTokenStartVerification_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP and create source MA
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporate.getRight(), secretKeyScaSendsApp);
        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //create second linked corporate
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), secretKeyScaSendsApp);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //create destination MA
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), authUserAccessToken, secretKeyScaSendsApp);

        //access token for the first Identity
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //Send funds by the first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, authUserAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaOtp_SecondIdentitySendFundsFirstIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the first identity
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaOtp_AuthUserSendFundsFirstIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), secretKeyScaSendsApp);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //access token for the first Identity
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), authUserAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the first identity
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaOtp_FirstIdentityStartVerificationSecondIdentityVerify_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP and create source MA
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporate.getRight(), secretKeyScaSendsApp);
        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create destination MA for the second identity
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //Send funds by the first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the first identity
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //Verify send by the second identity
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        OTP_CHANNEL, secretKeyScaSendsApp, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaOtp_SecondIdentityStartVerificationFirstIdentityVerify_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //Verify send by the first identity
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        OTP_CHANNEL, secretKeyScaSendsApp, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaOtp_SecondIdentityStartVerificationAuthUserVerify_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), secretKeyScaSendsApp);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), authUserAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //Verify send
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        OTP_CHANNEL, secretKeyScaSendsApp, authUserAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaOtp_SeveralLinkedIdentitiesCrossIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create third linked corporate and get accessToken
        final String thirdCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the third identity
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, thirdCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaOtp_SeveralLinkedIdentitiesCrossIdentityVerification_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateOtpSends(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create third linked corporate and get accessToken
        final String thirdCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendOtpVerification(sendId, OTP_CHANNEL, secretKeyScaSendsApp, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        //Verify send by the third identity
        SendsService.verifySendOtp(new VerificationModel(VERIFICATION_CODE), sendId,
                        OTP_CHANNEL, secretKeyScaSendsApp, thirdCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    //AUTHY sends
    @Test
    public void SendFundsViaAuthy_FirstIdentityMaToMa_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthySend(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification
        SendsService.startSendPushVerification(sendId, AUTHY_CHANNEL, secretKeyScaSendsApp,
                        firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendFundsViaAuthy_SecondIdentityMaToMa_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthySend(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification
        SendsService.startSendPushVerification(sendId, AUTHY_CHANNEL, secretKeyScaSendsApp,
                        secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendFundsViaAuthy_AuthUserMaToMa_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthySend(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), secretKeyScaSendsApp);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //access token for the first Identity
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), authUserAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification
        SendsService.startSendPushVerification(sendId, AUTHY_CHANNEL, secretKeyScaSendsApp,
                        authUserAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendFundsViaAuthy_FirstIdentitySendSecondIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthySend(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendPushVerification(sendId, AUTHY_CHANNEL, secretKeyScaSendsApp,
                        secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaAuthy_SecondIdentitySendFirstIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthySend(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the first identity
        SendsService.startSendPushVerification(sendId, AUTHY_CHANNEL, secretKeyScaSendsApp,
                        firstCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaAuthy_AuthUserSendFirstIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthySend(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(corporateProfileIdScaSendsApp, secretKeyScaSendsApp);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), secretKeyScaSendsApp);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //access token for the first Identity
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, authTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), authUserAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the first identity
        SendsService.startSendPushVerification(sendId, AUTHY_CHANNEL, secretKeyScaSendsApp,
                        firstCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaAuthy_SeveralLinkedIdentitiesCrossIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for OTP
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaSendsApp).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateAuthySend(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //create third linked corporate and get accessToken
        final String thirdCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                secretKeyScaSendsApp, corporateProfileIdScaSendsApp);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyScaSendsApp);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyScaSendsApp, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), secondCorporateAccessToken, secretKeyScaSendsApp);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                corporateManagedAccountProfileIdScaSendsApp, Currency.EUR.name(), firstCorporateAccessToken, secretKeyScaSendsApp);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, tenantScaSendApp);

        //Send funds by the second identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileIdScaSendsApp)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, secretKeyScaSendsApp, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the third identity
        SendsService.startSendPushVerification(sendId, AUTHY_CHANNEL, secretKeyScaSendsApp,
                        thirdCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    //BIOMETRIC sends
    @Test
    public void SendFundsViaBiometric_FirstIdentityMaToMa_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporateAccessToken, passcodeAppSecretKey);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, passcodeAppSecretKey);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(passcodeAppSendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        //sends are allowed with access token
        final String sendId = SendsService.sendFunds(sendFundsModel, passcodeAppSecretKey, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        SendsService.startSendPushVerification(sendId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendFundsViaBiometric_SecondIdentityMaToMa_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, passcodeAppSecretKey);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporateAccessToken, passcodeAppSecretKey);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(passcodeAppSendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        //sends are allowed with access token
        final String sendId = SendsService.sendFunds(sendFundsModel, passcodeAppSecretKey, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        SendsService.startSendPushVerification(sendId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendFundsViaBiometric_AuthUserMaToMa_Success() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), passcodeAppSecretKey);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //access token for the first Identity
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), authUserAccessToken, passcodeAppSecretKey);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporateAccessToken, passcodeAppSecretKey);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(passcodeAppSendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        //sends are allowed with access token
        final String sendId = SendsService.sendFunds(sendFundsModel, passcodeAppSecretKey, authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        SendsService.startSendPushVerification(sendId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendFundsViaBometric_FirstIdentitySendSecondIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporateAccessToken, passcodeAppSecretKey);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, passcodeAppSecretKey);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //Send funds by first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(passcodeAppSendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, passcodeAppSecretKey, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendPushVerification(sendId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, secondCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaBometric_FirstIdentitySendAuthUserStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), passcodeAppSecretKey);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //access token for the first Identity
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporateAccessToken, passcodeAppSecretKey);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), authUserAccessToken, passcodeAppSecretKey);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //Send funds by first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(passcodeAppSendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, passcodeAppSecretKey, firstCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendPushVerification(sendId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, authUserAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaBometric_SecondIdentitySendFirstIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //access token for the first Identity
        final String corporateAuthenticationTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, corporateAuthenticationTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, passcodeAppSecretKey);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporateAccessToken, passcodeAppSecretKey);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //Send funds by first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(passcodeAppSendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, passcodeAppSecretKey, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendPushVerification(sendId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaBometric_AuthUserSendFirstIdentityStartVerifyChallenge_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create and link Auth User
        final Pair<String, String> authUserCorporate = createAuthUserCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight(), passcodeAppSecretKey);
        linkAuthUser(authUserId, firstCorporate.getLeft());

        //access token
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, passcodeAppSecretKey);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //access token for the first Identity
        final String firstCorporateAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(firstCorporate.getLeft(), IdentityType.CORPORATE)),
                passcodeAppSecretKey, authTokenAfterLinking);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), authUserAccessToken, passcodeAppSecretKey);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), firstCorporateAccessToken, passcodeAppSecretKey);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //Send funds by first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(passcodeAppSendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, passcodeAppSecretKey, authUserAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendPushVerification(sendId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, firstCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendFundsViaBometric_SeveralLinkedIdentitiesCrossIdentityStartVerification_NotFound() {
        final Long depositAmount = 10000L;

        //Create first corporate identity enrolled for Biometric
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final String name = createCorporateModel.getRootUser().getName();
        final String surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> firstCorporate = createCorporateBiometric(createCorporateModel, corporateRootEmail);

        //create second linked corporate and get accessToken
        final String secondCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //create third linked corporate and get accessToken
        final String thirdCorporateAccessToken = createLinkedCorporate(name, surname, corporateRootEmail, firstCorporate.getRight(),
                passcodeAppSecretKey, passcodeAppCorporateProfileId);

        //creating MA with access token
        final Pair<String, CreateManagedAccountModel> sourceManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), secondCorporateAccessToken, passcodeAppSecretKey);
        final Pair<String, CreateManagedAccountModel> destinationManagedAccount = createManagedAccount(
                passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), thirdCorporateAccessToken, passcodeAppSecretKey);

        fundManagedAccount(sourceManagedAccount.getLeft(), Currency.EUR.name(), depositAmount, passcodeAppTenantId);

        //Send funds by first identity
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(passcodeAppSendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String sendId = SendsService.sendFunds(sendFundsModel, passcodeAppSecretKey, secondCorporateAccessToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");

        //Start verification by the second identity
        SendsService.startSendPushVerification(sendId, BIOMETRIC_CHANNEL, passcodeAppSecretKey, thirdCorporateAccessToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private Pair<String, String> createCorporateOtp(final CreateCorporateModel createCorporateModel,
                                                    final String corporateRootEmail) {
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        return corporate;
    }

    private Pair<String, String> createCorporateOtpSends(final CreateCorporateModel createCorporateModel,
                                                         final String corporateRootEmail) {
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secretKeyScaSendsApp);
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKeyScaSendsApp);
        return corporate;
    }

    private Pair<String, String> createCorporateAuthy(CreateCorporateModel createCorporateModel, String corporateRootEmail) {
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());
        return corporate;
    }

    private Pair<String, String> createCorporateAuthySend(CreateCorporateModel createCorporateModel, String corporateRootEmail) {
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKeyScaSendsApp);
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKeyScaSendsApp);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKeyScaSendsApp, corporate.getRight());
        return corporate;
    }

    private Pair<String, String> createCorporateBiometric(final CreateCorporateModel createCorporateModel,
                                                          final String corporateRootEmail) {
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, passcodeAppSecretKey);
        CorporatesHelper.verifyEmail(corporateRootEmail, passcodeAppSecretKey);
        SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), passcodeAppSharedKey,
                passcodeAppSecretKey, corporate.getRight());
        return corporate;
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

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken,
                                                                                  final String secretKey) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                currency)
                        .build();

        final String corporateManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(corporateManagedAccountId, createManagedAccountModel);
    }

    protected void fundManagedAccount(final String managedAccountId,
                                      final String currency,
                                      final Long depositAmount,
                                      final String innovatorId) {
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, depositAmount);
    }

    private static void assertSendState(final String id, final String token, final State state) {

        TestHelper.ensureAsExpected(120,
                () -> SendsService.getSend(secretKeyScaSendsApp, id, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(state.name()),
                Optional.of(String.format("Expecting 200 with a send in state %s, check logged payload", state.name())));
    }

    private static void enableSendsSca() {
        UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setWebhookDisabled(true)
                        .setScaConfig(new ScaConfigModel(true, false))
                        .build();
        InnovatorHelper.enableSendsSca(updateProgrammeModel, passcodeAppProgrammeId, passcodeAppInnovatorToken);
    }

    private static void disableSendsSca() {
        UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setWebhookDisabled(true)
                        .setScaConfig(new ScaConfigModel(false, false))
                        .build();
        InnovatorHelper.disableSendsSca(updateProgrammeModel, passcodeAppProgrammeId, passcodeAppInnovatorToken);
    }

    private static Pair<String, String> createAuthUserCorporate(final String corporatesProfileId,
                                                                final String secretKey) {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId).build();
        return CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
    }

    private static String createAuthUser(final String nameRootUser,
                                         final String surnameRootUser,
                                         final String corporateToken,
                                         final String secretKey) {
        final UsersModel authUserModel = UsersModel.DefaultUsersModel()
                .setName(nameRootUser)
                .setSurname(surnameRootUser)
                .build();
        final Pair<String, String> authUser = UsersHelper.createAuthenticatedUser(authUserModel, secretKey, corporateToken);

        return authUser.getLeft();
    }

    private static void linkAuthUser(final String authUserId,
                                     final String corporateIdRootUser) {
        AdminService.linkUseridToCorporateSemi(new UserId(authUserId), corporateIdRootUser, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}