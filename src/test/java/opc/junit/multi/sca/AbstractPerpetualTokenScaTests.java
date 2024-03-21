package opc.junit.multi.sca;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.AcceptedResponse;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.DestroyedReason;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.OwtType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.helpers.multi.TransfersHelper;
import opc.models.admin.ImpersonateIdentityModel;
import opc.models.innovator.UnassignedCardResponseModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedaccounts.PatchManagedAccountModel;
import opc.models.multi.managedcards.ActivatePhysicalCardModel;
import opc.models.multi.managedcards.AssignManagedCardModel;
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.multi.managedcards.ReplacePhysicalCardModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.SendsService;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public abstract class AbstractPerpetualTokenScaTests extends BasePerpetualScaSetup {

    protected abstract String getInitialPerpetualTokenScaMaApp();
    protected abstract String getIdentityManagedAccountProfileScaMaApp();
    protected abstract String getIdentityManagedCardsProfileScaMaApp();
    protected abstract String getIdentityEmailScaMaApp();

    protected abstract String getInitialPerpetualTokenScaMcApp();
    protected abstract String getIdentityEmailScaMcApp();
    protected abstract String getIdentityPrepaidManagedCardProfileScaMcApp();
    protected abstract CardLevelClassification getIdentityCardLevelClassificationScaMcApp();

    protected abstract String getSendDestinationInitialPerpetualTokenScaMaApp();
    protected abstract Triple<String, ImpersonateIdentityModel, String> createIdentity(final ProgrammeDetailsModel programme);

    private final static String CURRENCY = "EUR";
    private final static String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;

    @Test
    public void PerpetualToken_CreateManagedAccount_Success() {
//      scaMA: true
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_CreateManagedAccount_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();

        ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, newPerpetualToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_CreateManagedAccount_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();

        ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, newToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_CreateManagedAccount_CallEndpointTwice_Success() {
//      scaMA: true
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetManagedAccounts_Success() {
//      scaMA: true
        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());

        ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void PerpetualToken_GetManagedAccounts_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        final ValidatableResponse firstResponse = ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(),
                        identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        final ValidatableResponse secondResponse = ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), newPerpetualToken)
                .then()
                .statusCode(SC_OK);

        String firstCallManagedAccountId = firstResponse.extract().jsonPath().get("accounts.id").toString();
        String secondCallManagedAccountId = secondResponse.extract().jsonPath().get("accounts.id").toString();

        Assertions.assertEquals(firstCallManagedAccountId, secondCallManagedAccountId);
    }

    @Test
    public void PerpetualToken_GetManagedAccounts_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        ManagedAccountsHelper.getManagedAccounts(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetManagedAccounts_CallEndpointTwice_Success() {
//      scaMA: true
        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());

        final ValidatableResponse firstResponse = ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK);

        final ValidatableResponse secondResponse = ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK);

        String firstCallManagedAccountId = firstResponse.extract().jsonPath().get("accounts.id").toString();
        String secondCallManagedAccountId = secondResponse.extract().jsonPath().get("accounts.id").toString();
        Assertions.assertEquals(firstCallManagedAccountId, secondCallManagedAccountId);
    }

    @Test
    public void PerpetualToken_GetManagedAccountById_Success() {
//      scaMA: true
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetManagedAccountById_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight());

        ManagedAccountsHelper.getManagedAccount(secretKeyScaMaApp, managedAccountId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, newPerpetualToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()));
    }

    @Test
    public void PerpetualToken_GetManagedAccountById_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight());

        ManagedAccountsHelper.getManagedAccount(secretKeyScaMaApp, managedAccountId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetManagedAccountById_CallEndpointTwice_Success() {
//      scaMA: true
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK);

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_PatchManagedAccount_ScaEnabled_Success() {
//      scaMA: true
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse response = ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKeyScaMaApp, managedAccountId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyName = response.extract().jsonPath().get("friendlyName");
        String actualTag = response.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualFriendlyName);
        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualTag);
    }

    @Test
    public void PerpetualToken_PatchManagedAccount_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse firstResponse = ManagedAccountsService.patchManagedAccount(patchManagedAccountModel,
                        secretKeyScaMaApp, managedAccountId, identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyNameFirstResponse = firstResponse.extract().jsonPath().get("friendlyName");
        String actualTagFirstResponse = firstResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualFriendlyNameFirstResponse);
        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualTagFirstResponse);


        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        final PatchManagedAccountModel patchManagedAccountModelSecondResponse =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse secondResponse = ManagedAccountsService.patchManagedAccount(patchManagedAccountModelSecondResponse, secretKeyScaMaApp,
                        managedAccountId, newPerpetualToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModelSecondResponse.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModelSecondResponse.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyNameSecondResponse = secondResponse.extract().jsonPath().get("friendlyName");
        String actualTagSecondResponse = secondResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(actualFriendlyNameFirstResponse, actualFriendlyNameSecondResponse);
        Assertions.assertNotEquals(actualTagFirstResponse, actualTagSecondResponse);
    }

    @Test
    public void PerpetualToken_PatchManagedAccount_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse firstResponse = ManagedAccountsService.patchManagedAccount(patchManagedAccountModel,
                        secretKeyScaMaApp, managedAccountId, identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyNameFirstResponse = firstResponse.extract().jsonPath().get("friendlyName");
        String actualTagFirstResponse = firstResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualFriendlyNameFirstResponse);
        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualTagFirstResponse);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        final PatchManagedAccountModel patchManagedAccountModelSecondResponse =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModelSecondResponse, secretKeyScaMaApp,
                        managedAccountId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_PatchManagedAccount_CallEndpointTwice_Success() {
//      scaMA: true
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse firstResponse = ManagedAccountsService.patchManagedAccount(patchManagedAccountModel,
                        secretKeyScaMaApp, managedAccountId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyNameFirstResponse = firstResponse.extract().jsonPath().get("friendlyName");
        String actualTagFirstResponse = firstResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualFriendlyNameFirstResponse);
        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualTagFirstResponse);


        final PatchManagedAccountModel patchManagedAccountModelSecondResponse =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse secondResponse = ManagedAccountsService.patchManagedAccount(patchManagedAccountModelSecondResponse,
                        secretKeyScaMaApp, managedAccountId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModelSecondResponse.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModelSecondResponse.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyNameSecondResponse = secondResponse.extract().jsonPath().get("friendlyName");
        String actualTagSecondResponse = secondResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(actualFriendlyNameSecondResponse, actualFriendlyNameFirstResponse);
        Assertions.assertNotEquals(actualTagSecondResponse, actualTagFirstResponse);
    }

    @Test
    public void PerpetualToken_GetManagedAccountsStatement_Success() {
//      scaMA: true
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, getInitialPerpetualTokenScaMaApp(),
                        Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetManagedAccountsStatement_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, newPerpetualToken,
                        Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetManagedAccountsStatement_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, newToken,
                        Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetManagedAccountsStatement_CallEndpointTwice_Success() {
//      scaMA: true
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, getInitialPerpetualTokenScaMaApp(),
                        Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, getInitialPerpetualTokenScaMaApp(),
                        Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetManagedCards_Success() {
//      scaMC: true
        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetManagedCards_CallEndpointTwice_Success() {
//      scaMC: true
        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetManagedCards_TwoSessions_Success() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final ValidatableResponse firstResponse = ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        final ValidatableResponse secondResponse = ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), newPerpetualToken)
                .then()
                .statusCode(SC_OK);

        String firstCallManagedCardId = firstResponse.extract().jsonPath().get("cards.id").toString();
        String secondCallManagedCardId = secondResponse.extract().jsonPath().get("cards.id").toString();
        Assertions.assertEquals(firstCallManagedCardId, secondCallManagedCardId);
    }

    @Test
    public void PerpetualToken_GetManagedCards_TwoSessions_Forbidden() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetManagedCardsById_Success() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetManagedCardsById_CallEndpointTwice_Success() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetManagedCardsById_TwoSessions_Success() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, newPerpetualToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetManagedCardsById_TwoSessions_Forbidden() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_PatchManagedCards_Success() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_PatchManagedCards_CallEndpointTwice_Success() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();
        final ValidatableResponse firstResponse = ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp,
                        cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        final PatchManagedCardModel patchManagedCardModelSecondResponse =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();
        final ValidatableResponse secondResponse = ManagedCardsService.patchManagedCard(patchManagedCardModelSecondResponse,
                        secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        String actualTagFirstResponse = firstResponse.extract().jsonPath().get("tag");
        String actualTagSecondResponse = secondResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(actualTagFirstResponse, actualTagSecondResponse);
    }

    @Test
    public void PerpetualToken_PatchManagedCards_TwoSessions_Success() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        final PatchManagedCardModel patchManagedCardModelSecondSession =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModelSecondSession, secretKeyScaMcApp, cardId, newPerpetualToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_PatchManagedCards_TwoSessions_Forbidden() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        final PatchManagedCardModel patchManagedCardModelSecondSession =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModelSecondSession, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_AssignManagedCards_Success() {
//      scaMC: true
        List<UnassignedCardResponseModel> corporateUnassignedCards = ManagedCardsHelper.replenishPrepaidCardPool(
                getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, getIdentityCardLevelClassificationScaMcApp(), InstrumentType.VIRTUAL, innovatorTokenScaMcApp);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(0).getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_AssignManagedCards_CallEndpointTwice_Success() {
//      scaMC: true
        List<UnassignedCardResponseModel> corporateUnassignedCards = ManagedCardsHelper.replenishPrepaidCardPool(
                getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, getIdentityCardLevelClassificationScaMcApp(), InstrumentType.VIRTUAL, innovatorTokenScaMcApp);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(0).getExternalHandle())
                        .build();
        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        final AssignManagedCardModel assignManagedCardModelSecondCall =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(1).getExternalHandle())
                        .build();
        ManagedCardsService.assignManagedCard(assignManagedCardModelSecondCall, secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_AssignManagedCards_TwoSessions_Success() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        List<UnassignedCardResponseModel> corporateUnassignedCards = ManagedCardsHelper.replenishPrepaidCardPool(
                getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, getIdentityCardLevelClassificationScaMcApp(), InstrumentType.VIRTUAL, innovatorTokenScaMcApp);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(0).getExternalHandle())
                        .build();
        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKeyScaMcApp, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        final AssignManagedCardModel assignManagedCardModelSecondSession =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(1).getExternalHandle())
                        .build();
        ManagedCardsService.assignManagedCard(assignManagedCardModelSecondSession, secretKeyScaMcApp, newPerpetualToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_AssignManagedCards_TwoSessions_Forbidden() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        List<UnassignedCardResponseModel> corporateUnassignedCards = ManagedCardsHelper.replenishPrepaidCardPool(
                getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, getIdentityCardLevelClassificationScaMcApp(), InstrumentType.VIRTUAL, innovatorTokenScaMcApp);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(0).getExternalHandle())
                        .build();
        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKeyScaMcApp, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        final AssignManagedCardModel assignManagedCardModelSecondSession =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(1).getExternalHandle())
                        .build();
        ManagedCardsService.assignManagedCard(assignManagedCardModelSecondSession, secretKeyScaMcApp, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_UpgradeManagedCardToPhysical_Success() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_UpgradeManagedCardToPhysical_CallEndpointTwice_AlreadyUpgraded() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_UPGRADED_TO_PHYSICAL"));
    }

    @Test
    public void PerpetualToken_UpgradeManagedCardToPhysical_TwoSessions_AlreadyUpgraded() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, newPerpetualToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_UPGRADED_TO_PHYSICAL"));
    }

    @Test
    public void PerpetualToken_UpgradeManagedCardToPhysical_TwoSessions_Forbidden() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_ActivateManagedCard_Success() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), physicalCardAddressModel);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_ActivateManagedCard_CallEndpointTwice_AlreadyActivated() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), physicalCardAddressModel);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_ACTIVATED"));
    }

    @Test
    public void PerpetualToken_ActivateManagedCard_TwoSessions_AlreadyActivated() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, newPerpetualToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_ACTIVATED"));
    }

    @Test
    public void PerpetualToken_ActivateManagedCard_TwoSessions_Forbidden() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetPhysicalCardPin_Success() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), physicalCardAddressModel);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetPhysicalCardPin_CallEndpointTwice_Success() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), physicalCardAddressModel);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetPhysicalCardPin_TwoSessions_Success() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, newPerpetualToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PerpetualToken_GetPhysicalCardPin_TwoSessions_Forbidden() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_ReplaceDamagedCard_Success() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), physicalCardAddressModel);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void PerpetualToken_ReplaceDamagedCard_CallEndpointTwice_AlreadyRequested() {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), physicalCardAddressModel);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_PENDING_REPLACEMENT"));
    }

    @Test
    public void PerpetualToken_ReplaceDamagedCard_TwoSessions_AlreadyRequested() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, newPerpetualToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_PENDING_REPLACEMENT"));
    }

    @Test
    public void PerpetualToken_ReplaceDamagedCard_TwoSessions_Forbidden() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void PerpetualToken_ReplaceLostStolenCard_Success(final DestroyedReason destroyedReason) {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), physicalCardAddressModel);

        reportLostOrStolen(cardId, getInitialPerpetualTokenScaMcApp(), destroyedReason, secretKeyScaMcApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void PerpetualToken_ReplaceLostStolenCard_CallEndpointTwice_AlreadyReplaced(final DestroyedReason destroyedReason) {
//      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), physicalCardAddressModel);

        reportLostOrStolen(cardId, getInitialPerpetualTokenScaMcApp(), destroyedReason, secretKeyScaMcApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_REPLACED"));
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void PerpetualToken_ReplaceLostStolenCard_TwoSessions_AlreadyReplaced(final DestroyedReason destroyedReason) {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        reportLostOrStolen(cardId, identity.getRight(), destroyedReason, secretKeyScaMcApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, newPerpetualToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_REPLACED"));
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void PerpetualToken_ReplaceLostStolenCard_TwoSessions_Forbidden(final DestroyedReason destroyedReason) {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        reportLostOrStolen(cardId, identity.getRight(), destroyedReason, secretKeyScaMcApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetManagedCardsStatement_Success() {
//      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);

        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetManagedCardsStatement_CallEndpointTwice_Success() {
//      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, getInitialPerpetualTokenScaMcApp());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);

        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));

        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, getInitialPerpetualTokenScaMcApp(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetManagedCardsStatement_TwoSessions_Success() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);

        ManagedCardsHelper.getManagedCardStatement(cardId, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, newPerpetualToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetManagedCardsStatement_TwoSessions_Forbidden() {
//      scaMC: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY,
                secretKeyScaMcApp, identity.getRight());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);

        ManagedCardsHelper.getManagedCardStatement(cardId, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, newToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetSends_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getSendDestinationInitialPerpetualTokenScaMaApp());

        sendFundsMaToMc(managedAccountId, managedCardId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);

        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void PerpetualToken_GetSends_CallEndpointTwice_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getSendDestinationInitialPerpetualTokenScaMaApp());

        sendFundsMaToMc(managedAccountId, managedCardId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);

        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));

        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetSends_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getSendDestinationInitialPerpetualTokenScaMaApp());

        sendFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);
        SendsHelper.getSends(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), newPerpetualToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetSends_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getSendDestinationInitialPerpetualTokenScaMaApp());

        sendFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);
        SendsHelper.getSends(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetTransfers_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());

        transferFundsMaToMc(managedAccountId, managedCardId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void PerpetualToken_GetTransfers_CallEndpointTwice_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());

        transferFundsMaToMc(managedAccountId, managedCardId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));

        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void PerpetualToken_GetTransfers_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersHelper.getTransfers(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), newPerpetualToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetTransfers_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());

        transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersHelper.getTransfers(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetOutgoingWireTransfers_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        sendOwt(managedAccountId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void PerpetualToken_GetOutgoingWireTransfers_CallEndpointTwice_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        sendOwt(managedAccountId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void PerpetualToken_GetOutgoingWireTransfers_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        sendOwt(managedAccountId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersHelper.getOutgoingWireTransfers(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), newPerpetualToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void PerpetualToken_GetOutgoingWireTransfers_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        sendOwt(managedAccountId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersHelper.getOutgoingWireTransfers(secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetOutgoingWireTransferById_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String owtId = sendOwt(managedAccountId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(owtId));
    }

    @Test
    public void PerpetualToken_GetOutgoingWireTransferById_CallEndpointTwice_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String owtId = sendOwt(managedAccountId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(owtId));

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(owtId));
    }

    @Test
    public void PerpetualToken_GetOutgoingWireTransferById_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String owtId = sendOwt(managedAccountId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersHelper.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, newPerpetualToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(owtId));
    }

    @Test
    public void PerpetualToken_GetOutgoingWireTransferById_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String owtId = sendOwt(managedAccountId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersHelper.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetTransferById_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void PerpetualToken_GetTransferById_CallEndpointTwice_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));

        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void PerpetualToken_GetTransferById_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersHelper.getTransfer(secretKeyScaMaApp, transactionId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, newPerpetualToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void PerpetualToken_GetTransferById_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersHelper.getTransfer(secretKeyScaMaApp, transactionId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PerpetualToken_GetSendById_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getSendDestinationInitialPerpetualTokenScaMaApp());
        final String transactionId = sendFundsMaToMc(managedAccountId, managedCardId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);

        SendsService.getSend(secretKeyScaMaApp, transactionId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void PerpetualToken_GetSendById_CallEndpointTwice_Success() {
//      scaMA: true
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getInitialPerpetualTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getSendDestinationInitialPerpetualTokenScaMaApp());
        final String transactionId = sendFundsMaToMc(managedAccountId, managedCardId, getInitialPerpetualTokenScaMaApp(),
                secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);

        SendsService.getSend(secretKeyScaMaApp, transactionId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));

        SendsService.getSend(secretKeyScaMaApp, transactionId, getInitialPerpetualTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void PerpetualToken_GetSendById_TwoSessions_Success() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getSendDestinationInitialPerpetualTokenScaMaApp());
        final String transactionId = sendFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);

        SendsHelper.getSend(secretKeyScaMaApp, transactionId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        String newPerpetualToken = AdminService.impersonateIdentity(identity.getMiddle(), adminToken);

        SendsService.getSend(secretKeyScaMaApp, transactionId, newPerpetualToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void PerpetualToken_GetSendById_TwoSessions_Forbidden() {
//      scaMA: true
        final Triple<String, ImpersonateIdentityModel, String> identity = createIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, identity.getRight());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY,
                secretKeyScaMaApp, getSendDestinationInitialPerpetualTokenScaMaApp());
        final String transactionId = sendFundsMaToMc(managedAccountId, managedCardId, identity.getRight(),
                secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);

        SendsHelper.getSend(secretKeyScaMaApp, transactionId, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        SendsService.getSend(secretKeyScaMaApp, transactionId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    private void reportLostOrStolen(final String managedCardId,
                                    final String token,
                                    final DestroyedReason destroyedReason,
                                    final String secretKey) {

        final Response response = destroyedReason.equals(DestroyedReason.LOST) ?
                ManagedCardsService.reportLostCard(secretKey, managedCardId, token) :
                ManagedCardsService.reportStolenCard(secretKey, managedCardId, token);

        response
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private static String sendFundsMaToMc(final String managedAccountId,
                                          final String managedCardId,
                                          final String identityToken,
                                          final String secretKey,
                                          final String innovatorId,
                                          final String sendsProfileId) {

        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, CURRENCY, 1000L);

        SendFundsModel sendFundsModel = SendFundsModel.newBuilder()
                .setProfileId(sendsProfileId)
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setDestinationAmount(new CurrencyAmount(CURRENCY, 100L))
                .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                .build();

        return SendsService.sendFunds(sendFundsModel, secretKey, identityToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private static String transferFundsMaToMc(final String managedAccountId,
                                              final String managedCardId,
                                              final String identityToken,
                                              final String secretKey,
                                              final String innovatorId,
                                              final String transfersProfileId) {

        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, CURRENCY, 1000L);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(CURRENCY, 100L))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        return TransfersService.transferFunds(transferFundsModel, secretKey, identityToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private static String sendOwt(final String managedAccountId,
                                  final String identityToken,
                                  final String secretKey,
                                  final String innovatorId,
                                  final String outgoingWireTransfersProfileId) {

        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, CURRENCY, 1000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        CURRENCY, 100L, OwtType.SEPA).build();

        return OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, identityToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }
}
