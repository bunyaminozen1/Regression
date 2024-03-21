package opc.junit.semi;

import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.junit.database.AuthSessionsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.admin.AdminSemiToggleModel;
import opc.models.admin.UserId;
import opc.models.backoffice.IdentityModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.Identity;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedCardsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionsSemiIdentitiesTests extends BaseSemiSetup {

    /**
     * SEMI (Single Email Multiple Identities) tests for AccessToken flow with all type of transactions.
     * 3DS Biometric Purchase Tests for SEMI Identities
     * Scenarios for:
     * - Corporate Identity and authUsers/rootUsers
     * - authUserId as LinkedUserId in 3ds config (DEV-4647)
     * - rootUserId as LinkedUserId in 3ds config (DEV-4647)
     */

    private final Long depositAmount = 1000L;
    private final Long availableToSpend = 1000L;
    private final Long purchaseAmount = getRandomPurchaseAmount();

    @BeforeAll
    public static void Setup() {
        initialSetup();
        AdminHelper.setSemiToggle(new AdminSemiToggleModel(tenantIdThreeDSApp, true));
    }

    @AfterAll
    public static void TearDown() {
        AdminHelper.setSemiToggle(new AdminSemiToggleModel(tenantIdThreeDSApp, false));
    }

    @Test
    public void ThreeDSecureCardPurchase_AuthUserPrepaidAccept_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String managedCardId = createPrepaidThreeDSecureManagedCard(authUserId, authUserAccessToken);
        transferFundsToCard(authUserAccessToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        //Simulate purchase
        simulatePurchaseAndAcceptOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), corporateRootUser.getLeft());

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, authUserAccessToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ThreeDSecureCardPurchase_AuthUserDebitAccept_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String managedAccountId = createManagedAccount(authUserAccessToken, threeDSIdentityCurrency);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKeyThreeDSApp, authUserAccessToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, threeDSIdentityCurrency, getRandomDepositAmount(), secretKeyThreeDSApp, authUserAccessToken);
        final String managedCardId = createDebitThreeDSecureManagedCard(authUserId, managedAccountId, authUserAccessToken);
        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(threeDSIdentityCurrency, availableToSpend), LimitInterval.ALWAYS)),
                authUserAccessToken);

        //Simulate purchase
        simulatePurchaseAndAcceptOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), corporateRootUser.getLeft());

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, authUserAccessToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ThreeDSecureCardPurchase_MultipleCardsAccept_Success() {
        final int numberOfCards = 3;

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create several managed cards and perform purchase for all of them
        final List<String> cards = new LinkedList<>();
        IntStream.range(0, numberOfCards)
                .forEach(i -> {
                    final String managedCardId = createPrepaidThreeDSecureManagedCard(authUserId, authUserAccessToken);
                    transferFundsToCard(authUserAccessToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

                    simulatePurchaseAndAcceptOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount) ,corporateRootUser.getLeft());
                    cards.add(managedCardId);
                });

        //expect authorisation and settlement to occur since biometric challenge was accepted
        for (String card : cards) {
            ManagedCardsHelper.getManagedCardStatement(card, secretKeyThreeDSApp, authUserAccessToken, 3)
                    .then()
                    .statusCode(SC_OK)
                    .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                    .body("entry[0].entryState", equalTo("COMPLETED"))
                    .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                    .body("entry[1].entryState", equalTo("COMPLETED"))
                    .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                    .body("entry[2].entryState", equalTo("COMPLETED"))
                    .body("count", equalTo(3))
                    .body("responseCount", equalTo(3));
        }
    }

    @Test
    public void ThreeDSecureCardPurchase_AuthUserDebitReject_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String accountId = createManagedAccount(authUserAccessToken, threeDSIdentityCurrency);

        ManagedAccountsHelper.assignManagedAccountIban(accountId, secretKeyThreeDSApp, authUserAccessToken);

        TestHelper.simulateManagedAccountDeposit(accountId, threeDSIdentityCurrency, getRandomDepositAmount(), secretKeyThreeDSApp, authUserAccessToken);
        final String managedCardId = createDebitThreeDSecureManagedCard(authUserId, accountId, authUserAccessToken);
        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(threeDSIdentityCurrency, availableToSpend), LimitInterval.ALWAYS)),
                authUserAccessToken);

        //Simulate purchase
        simulatePurchaseAndRejectOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, authUserAccessToken, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void ThreeDSecureCardPurchase_AuthUserPrepaidReject_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String managedCardId = createPrepaidThreeDSecureManagedCard(authUserId, authUserAccessToken);
        transferFundsToCard(authUserAccessToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        //Simulate purchase
        simulatePurchaseAndRejectOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, authUserAccessToken, 1)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void ThreeDSecureCardPurchase_InitiateBiometricNotEnabled_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String managedCardId = createPrepaidThreeDSecureManagedCard(authUserId, authUserAccessToken);
        transferFundsToCard(authUserAccessToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        //Simulate purchase
        simulateThreeDSecurePurchaseById(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), false);

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, authUserAccessToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ThreeDSecureCardPurchase_InitiateBiometricNotProvided_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String managedCardId = createPrepaidThreeDSecureManagedCard(authUserId, authUserAccessToken);
        transferFundsToCard(authUserAccessToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        //Simulate purchase
        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount))
                        .build();

        SimulatorHelper.simulateCardPurchaseById(secretKeyThreeDSApp, managedCardId, simulateCardPurchaseModel);

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, authUserAccessToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ThreeDSecureCardPurchase_CardNotEnrolledForBiometricThreeDS_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String managedCardId = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileIdThreeDSApp, threeDSIdentityCurrency, authUserAccessToken);
        transferFundsToCard(authUserAccessToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        //Simulate purchase
        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount))
                        .setInitiateBiometricThreeDSecure(true)
                        .build();
        SimulatorService.simulateCardPurchaseById(simulateCardPurchaseModel, managedCardId, secretKeyThreeDSApp)
                .then()
                .statusCode(SC_OK)
                .body("code", equalTo("CARD_NOT_ENROLLED_FOR_BIOMETRIC_THREEDS"));
    }

    @Test
    public void ThreeDSecureCardPurchase_RootPrepaidAccept_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String rootAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String managedCardId = createPrepaidThreeDSecureManagedCard(corporateRootUser.getLeft(), rootAccessToken);
        transferFundsToCard(rootAccessToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        //Simulate purchase
        simulatePurchaseAndAcceptOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), corporateRootUser.getLeft());

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, rootAccessToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ThreeDSecureCardPurchase_RootDebitAccept_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String rootAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String accountId = createManagedAccount(rootAccessToken, threeDSIdentityCurrency);

        ManagedAccountsHelper.assignManagedAccountIban(accountId, secretKeyThreeDSApp, rootAccessToken);

        TestHelper.simulateManagedAccountDeposit(accountId, threeDSIdentityCurrency, getRandomDepositAmount(), secretKeyThreeDSApp, rootAccessToken);
        final String managedCardId = createDebitThreeDSecureManagedCard(corporateRootUser.getLeft(), accountId, rootAccessToken);
        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(threeDSIdentityCurrency, availableToSpend), LimitInterval.ALWAYS)),
                rootAccessToken);

        //Simulate purchase
        simulatePurchaseAndAcceptOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), corporateRootUser.getLeft());

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, rootAccessToken, 2)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void ThreeDSecureCardPurchase_RootDebitReject_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String rootAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String accountId = createManagedAccount(rootAccessToken, threeDSIdentityCurrency);

        ManagedAccountsHelper.assignManagedAccountIban(accountId, secretKeyThreeDSApp, rootAccessToken);

        TestHelper.simulateManagedAccountDeposit(accountId, threeDSIdentityCurrency, getRandomDepositAmount(), secretKeyThreeDSApp, rootAccessToken);
        final String managedCardId = createDebitThreeDSecureManagedCard(corporateRootUser.getLeft(), accountId, rootAccessToken);
        setSpendLimit(managedCardId,
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(threeDSIdentityCurrency, availableToSpend), LimitInterval.ALWAYS)),
                rootAccessToken);

        //Simulate purchase
        simulatePurchaseAndRejectOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, rootAccessToken, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void ThreeDSecureCardPurchase_RootPrepaidReject_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String rootAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC
        final String managedCardId = createPrepaidThreeDSecureManagedCard(corporateRootUser.getLeft(), rootAccessToken);
        transferFundsToCard(rootAccessToken, managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        //Simulate purchase
        simulatePurchaseAndRejectOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, rootAccessToken, 1)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("TRANSFER"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void ThreeDSecureCardPurchase_CardCreatedBeforeLinking_Success() {

        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //create 3ds MC
        final String managedCardId = createPrepaidThreeDSecureManagedCard(corporateRootUser.getLeft(), corporateRootUser.getRight());
        transferFundsToCard(corporateRootUser.getRight(), managedCardId, threeDSIdentityCurrency, depositAmount, 1);

        //link authUser to the first identity
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get accessToken
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String rootAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //Simulate purchase
        simulatePurchaseAndAcceptOkayChallenge(managedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount), corporateRootUser.getLeft());

        //expect authorisation and settlement to occur since biometric challenge was accepted
        ManagedCardsHelper.getManagedCardStatement(managedCardId, secretKeyThreeDSApp, rootAccessToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("entry[2].transactionId.type", equalTo("TRANSFER"))
                .body("entry[2].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void ThreeDSecureCardPurchase_McWithAuthUserIdForAllIdentities_Success() throws SQLException {
        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate linked to the first
        final Pair<String, String> secondCorp = createLinkedCorporate(name, surname, corporateRootEmail, corporateRootUser.getRight(), secretKeyThreeDSApp, corporateProfileIdThreeDSApp);

        //create third corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first Root User
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get Access Tokens for authUser via different identities
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);
        final String firstCorpAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);
        final String secondCorpAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(secondCorp.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC for all SEMI users
        final String authUserManagedCardId = createPrepaidThreeDSecureManagedCard(authUserId, authUserAccessToken);
        final String firstCorpManagedCardId = createPrepaidThreeDSecureManagedCard(authUserId, firstCorpAccessToken);
        final String secondCorpManagedCardId = createPrepaidThreeDSecureManagedCard(authUserId, secondCorpAccessToken);

        transferFundsToCard(authUserAccessToken, authUserManagedCardId, threeDSIdentityCurrency, depositAmount, 1);
        transferFundsToCard(firstCorpAccessToken, firstCorpManagedCardId, threeDSIdentityCurrency, depositAmount, 1);
        transferFundsToCard(secondCorpAccessToken, secondCorpManagedCardId, threeDSIdentityCurrency, depositAmount, 1);

        final String challengeIdAuthUser = simulatePurchaseAndAcceptOkayChallenge(authUserManagedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));
        final String challengeIdFirstCorp = simulatePurchaseAndAcceptOkayChallenge(firstCorpManagedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));
        final String challengeIdSecondCorp = simulatePurchaseAndAcceptOkayChallenge(secondCorpManagedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));

        //verify identity_id.
        final String identityIdAuthUser = AuthSessionsDatabaseHelper.getChallenge(challengeIdAuthUser).get(0).get("identity_id");
        final String identityIdFirstCorp = AuthSessionsDatabaseHelper.getChallenge(challengeIdFirstCorp).get(0).get("identity_id");
        final String identityIdSecondCorp = AuthSessionsDatabaseHelper.getChallenge(challengeIdSecondCorp).get(0).get("identity_id");

        assertEquals(authUserCorporate.getLeft(), identityIdAuthUser);
        assertEquals(corporateRootUser.getLeft(), identityIdFirstCorp);
        assertEquals(secondCorp.getLeft(), identityIdSecondCorp);

    }

    @Test
    public void ThreeDSecureCardPurchase_McForAllSemiUsers_Success() throws SQLException {
        //create first identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();
        final String corporateRootEmail = threeDSCorporateDetails.getRootUser().getEmail();
        final String name = threeDSCorporateDetails.getRootUser().getName();
        final String surname = threeDSCorporateDetails.getRootUser().getSurname();

        final Pair<String, String> corporateRootUser = createCorporateBiometric(threeDSCorporateDetails, corporateRootEmail);

        //create second corporate linked to the first
        final Pair<String, String> secondCorp = createLinkedCorporate(name, surname, corporateRootEmail, corporateRootUser.getRight(),
                secretKeyThreeDSApp, corporateProfileIdThreeDSApp);

        //create third corporate and authUser
        final Pair<String, String> authUserCorporate = createCorporate();
        final String authUserId = createAuthUser(name, surname, authUserCorporate.getRight());

        //link authUser to the first Root User
        linkAuthUser(authUserId, corporateRootUser.getLeft());

        //get Access Tokens for all SEMI users
        final String authTokenAfterLinking = AuthenticationHelper.login(corporateRootEmail, secretKeyThreeDSApp);
        final String authUserAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(authUserCorporate.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);
        final String firstCorpAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(corporateRootUser.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);
        final String secondCorpAccessToken = AuthenticationHelper.requestAccessToken(new Identity(
                        new IdentityModel(secondCorp.getLeft(), IdentityType.CORPORATE)),
                secretKeyThreeDSApp, authTokenAfterLinking);

        //create 3ds MC for all SEMI users
        final String authUserManagedCardId = createPrepaidThreeDSecureManagedCard(authUserId, authUserAccessToken);
        final String firstCorpManagedCardId = createPrepaidThreeDSecureManagedCard(corporateRootUser.getLeft(), firstCorpAccessToken);
        final String secondCorpManagedCardId = createPrepaidThreeDSecureManagedCard(secondCorp.getLeft(), secondCorpAccessToken);

        transferFundsToCard(authUserAccessToken, authUserManagedCardId, threeDSIdentityCurrency, depositAmount, 1);
        transferFundsToCard(firstCorpAccessToken, firstCorpManagedCardId, threeDSIdentityCurrency, depositAmount, 1);
        transferFundsToCard(secondCorpAccessToken, secondCorpManagedCardId, threeDSIdentityCurrency, depositAmount, 1);

        final String challengeIdAuthUser = simulatePurchaseAndAcceptOkayChallenge(authUserManagedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));
        final String challengeIdFirstCorp = simulatePurchaseAndAcceptOkayChallenge(firstCorpManagedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));
        final String challengeIdSecondCorp = simulatePurchaseAndAcceptOkayChallenge(secondCorpManagedCardId, new CurrencyAmount(threeDSIdentityCurrency, purchaseAmount));

        //verify identity_id
        final String identityIdAuthUser = AuthSessionsDatabaseHelper.getChallenge(challengeIdAuthUser).get(0).get("identity_id");
        final String identityIdFirstCorp = AuthSessionsDatabaseHelper.getChallenge(challengeIdFirstCorp).get(0).get("identity_id");
        final String identityIdSecondCorp = AuthSessionsDatabaseHelper.getChallenge(challengeIdSecondCorp).get(0).get("identity_id");

        assertEquals(authUserCorporate.getLeft(), identityIdAuthUser);
        assertEquals(corporateRootUser.getLeft(), identityIdFirstCorp);
        assertEquals(secondCorp.getLeft(), identityIdSecondCorp);
    }

    private static void initialSetup() {

        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 100000);

        AdminHelper.setProgrammeAuthyChallengeLimit(threeDSApp.getProgrammeId(), resetCount,
                adminToken);
    }

    private static Pair<String, String> createCorporate() {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdThreeDSApp).build();
        return CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKeyThreeDSApp);
    }

    private static Pair<String, String> createLinkedCorporate(final String name,
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

        final String accessToken = AuthenticationHelper.requestAccessToken
                (new Identity(new IdentityModel(linkedIdentityId, IdentityType.CORPORATE)),
                        secretKey, authToken);

        return Pair.of(linkedIdentityId, accessToken);
    }

    private Pair<String, String> createCorporateBiometric(final CreateCorporateModel createCorporateModel,
                                                          final String corporateRootEmail) {
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secretKeyThreeDSApp);
        CorporatesHelper.verifyEmail(corporateRootEmail, secretKeyThreeDSApp);
        SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), sharedKeyThreeDSApp,
                secretKeyThreeDSApp, corporate.getRight());
        return corporate;
    }

    private static String createAuthUser(final String nameRootUser,
                                         final String surnameRootUser,
                                         final String corporateToken) {
        final UsersModel authUserModel = UsersModel.DefaultUsersModel()
                .setName(nameRootUser)
                .setSurname(surnameRootUser)
                .build();
        final Pair<String, String> authUser = UsersHelper.createEnrolledUser(authUserModel, secretKeyThreeDSApp, corporateToken);

        return authUser.getLeft();
    }

    private static void linkAuthUser(final String authUserId,
                                     final String corporateIdRootUser) {
        AdminService.linkUseridToCorporateSemi(new UserId(authUserId), corporateIdRootUser, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    protected static String createPrepaidManagedCard(final String managedCardProfileId,
                                                     final String currency,
                                                     final String authenticationToken) {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardProfileId,
                                currency)
                        .build();

        return ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKeyThreeDSApp, authenticationToken);
    }

    private String createPrepaidThreeDSecureManagedCard(final String userId,
                                                        final String token) {
        return ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.ThreeDSecureCreatePrepaidManagedCardModel(
                                corporatePrepaidManagedCardsProfileIdThreeDSApp, userId).build(), secretKeyThreeDSApp,
                        token, Optional.empty())
                .then()
                .extract()
                .jsonPath()
                .getString("id");
    }

    protected static Pair<String, CreateManagedAccountModel> transferFundsToCard(final String token,
                                                                                 final String managedCardId,
                                                                                 final String currency,
                                                                                 final Long depositAmount,
                                                                                 final int transferCount) {

        return TestHelper
                .simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountsProfileIdThreeDSApp,
                        transfersProfileIdThreeDSApp, managedCardId, currency, depositAmount, secretKeyThreeDSApp, token, transferCount);
    }

    protected String simulatePurchaseAndAcceptOkayChallenge(final String managedCardId,
                                                            final CurrencyAmount transactionAmount,
                                                            final String credentialId) {
        final String challengeId = simulateThreeDSecurePurchaseById(managedCardId, transactionAmount, true);
        SimulatorHelper.acceptOkayThreeDSecureChallenge(secretKeyThreeDSApp, challengeId, credentialId);
        return challengeId;
    }

    protected String simulatePurchaseAndAcceptOkayChallenge(final String managedCardId,
                                                            final CurrencyAmount transactionAmount) {
        final String challengeId = simulateThreeDSecurePurchaseById(managedCardId, transactionAmount, true);
        SimulatorHelper.acceptOkayThreeDSecureChallenge(secretKeyThreeDSApp, challengeId);
        return challengeId;
    }

    protected void simulatePurchaseAndRejectOkayChallenge(final String managedCardId,
                                                          final CurrencyAmount transactionAmount) {
        final String challengeId = simulateThreeDSecurePurchaseById(managedCardId, transactionAmount, true);
        SimulatorHelper.rejectOkayThreeDSecureChallenge(secretKeyThreeDSApp, challengeId);
    }

    protected String simulateThreeDSecurePurchaseById(final String managedCardId,
                                                      final CurrencyAmount transactionAmount,
                                                      final Boolean initiateBiometric) {
        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(transactionAmount)
                        .setInitiateBiometricThreeDSecure(initiateBiometric)
                        .build();

        if (initiateBiometric)
            return SimulatorHelper.simulateThreeDSecureCardPurchaseById(secretKeyThreeDSApp, managedCardId, simulateCardPurchaseModel);
        else {
            SimulatorHelper.simulateCardPurchaseById(secretKeyThreeDSApp, managedCardId, simulateCardPurchaseModel);
            return null;
        }
    }

    private String createDebitThreeDSecureManagedCard(final String userId,
                                                      final String managedAccountId,
                                                      final String userToken) {
        return ManagedCardsService.createManagedCard(
                        CreateManagedCardModel.ThreeDSecureCreateDebitManagedCardModel(
                                corporateDebitManagedCardsProfileIdThreeDSApp, userId, managedAccountId).build(), secretKeyThreeDSApp,
                        userToken, Optional.empty())
                .then()
                .extract()
                .jsonPath()
                .getString("id");
    }

    private String createManagedAccount(final String authenticationToken,
                                        final String currency) {
        return ManagedAccountsHelper.createManagedAccount(corporateManagedAccountsProfileIdThreeDSApp,
                currency, secretKeyThreeDSApp, authenticationToken);
    }

    protected static Long getRandomDepositAmount() {
        Random random = new Random();
        return (long) (random.nextInt(50 - 20) + 20) * 10;
    }

    protected static Long getRandomPurchaseAmount() {
        Random random = new Random();
        return (long) (random.nextInt(10 - 5) + 5) * 10;
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(final List<SpendLimitModel> spendLimit) {
        return SpendRulesModel
                .builder()
                .setAllowedMerchantCategories(new ArrayList<>())
                .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                .setAllowedMerchantIds(new ArrayList<>())
                .setBlockedMerchantIds(new ArrayList<>())
                .setAllowContactless(true)
                .setAllowAtm(true)
                .setAllowECommerce(true)
                .setAllowCashback(true)
                .setAllowCreditAuthorisations(true)
                .setSpendLimit(spendLimit);
    }

    protected void setSpendLimit(final String managedCardId,
                                 final List<SpendLimitModel> spendLimit,
                                 final String authenticationToken) {
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKeyThreeDSApp, managedCardId, authenticationToken);
    }
}
