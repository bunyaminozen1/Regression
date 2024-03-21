package opc.junit.multi.sca;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.AcceptedResponse;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.DestroyedReason;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.OwtType;
import opc.junit.database.AuthyDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.innovator.UnassignedCardResponseModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedaccounts.PatchManagedAccountModel;
import opc.models.multi.managedcards.ActivatePhysicalCardModel;
import opc.models.multi.managedcards.AssignManagedCardModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
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
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.SendsService;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public abstract class AbstractAuthyStepupScaTests extends BaseIdentitiesScaSetup {

    protected abstract String getIdentityTokenScaApp();
    protected abstract String getIdentityManagedAccountProfileScaApp();

    protected abstract String getIdentityTokenScaMaApp();
    protected abstract String getIdentityManagedAccountProfileScaMaApp();
    protected abstract String getIdentityEmailScaMaApp();
    protected abstract String getIdentityManagedCardsProfileScaMaApp();

    protected abstract String getIdentityTokenScaMcApp();
    protected abstract String getIdentityEmailScaMcApp();
    protected abstract String getIdentityPrepaidManagedCardProfileScaMcApp();
    protected abstract CardLevelClassification getIdentityCardLevelClassificationScaMcApp();
    protected abstract String getSendDestinationIdentityTokenScaMaApp();
    protected abstract Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme);

    private final static String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;
    private final static String CURRENCY = "EUR";
    private final static int SCA_EXPIRED_TIME = 61000;

    @Test
    public void AuthyStepup_CreateManagedAccount_Success() {
//      scaMA: true

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_CreateManagedAccount_TwoSessions_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        authyStepUpAccepted(identity.getRight(), secretKeyScaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, newToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_CreateManagedAccount_Rejected_Forbidden() {
//      scaMA: true

        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKeyScaMaApp, identity.getRight());

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startStepup(EnrolmentChannel.SMS.name(), secretKeyScaMaApp, identity.getRight());
        AuthenticationHelper.verifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMaApp, identity.getRight() );

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_GetManagedAccounts_Success() {
//      scaMA: true

        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void AuthyStepup_GetManagedAccounts_Rejected_Forbidden() {
        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        authyStepUpRejected(getIdentityTokenScaApp(), secretKeyScaApp);

        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaApp(), CURRENCY, secretKeyScaApp, getIdentityTokenScaApp());

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);

        ManagedAccountsService.getManagedAccounts(secretKeyScaApp, Optional.empty(), getIdentityTokenScaApp())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void AuthyStepup_GetManagedAccountById_Success() {
//      scaMA: true

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_PatchManagedAccount_Success() {
//      scaMA: true

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse response = ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        final String actualFriendlyName = response.extract().jsonPath().get("friendlyName");
        final String actualTag = response.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualFriendlyName);
        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualTag);
    }

    @Test
    public void AuthyStepup_GetManagedAccountsStatement_OneSessionStepUpTrue_Success() {
//      scaMA: true

        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void AuthyStepup_GetManagedAccountsStatement_StepUpExpired_Forbidden() throws InterruptedException {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        authyStepUpAccepted(identity.getRight(), secretKeyScaMaApp);

        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, identity.getRight(), Optional.empty(), AcceptedResponse.JSON),
                SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, newToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void AuthyStepup_GetManagedAccountsStatement_StepUpActive_Success() {
//      scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        authyStepUpAccepted(identity.getRight(), secretKeyScaMaApp);

        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        TestHelper.ensureAsExpected(15,
                () -> ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, identity.getRight(), Optional.empty(), AcceptedResponse.JSON),
                SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, newToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    /**
     * User is trying to step-up via Authy. In case Authy fails, the fallback should take place, meaning that SMS should be sent to the user.
     */

    @Test
    public void AuthyStepupFailed_CreateManagedAccount_FallbackWithOtp_Success() {
//      scaMA: true

        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKeyScaMaApp, identity.getRight());

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMaApp, identity.getRight() );

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepupFailed_CreateManagedAccount_FallbackWithOtp_Forbidden() {
//      scaMA: true

        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKeyScaMaApp, identity.getRight());

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startStepup(EnrolmentChannel.SMS.name(), secretKeyScaMaApp, identity.getRight() );

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void AuthyStepupFailed_CreateManagedAccount_FallbackWithOtp_Conflict() {
//      scaMA: true

        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKeyScaMaApp, identity.getRight());

        final String sessionId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKeyScaMaApp, identity.getRight());

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMaApp, identity.getRight() );

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        SimulatorHelper.successfullyDeclinedAuthyStepUp(secretKeyScaMaApp, sessionId);
    }

    @Test
    public void AuthyStepup_GetManagedCards_Success() {
//      scaMC: true

        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_GetManagedCardsById_ScaMcTrue_Success() {
//      scaMC: true

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_GetManagedCardsById_SecondSessionNoStepUp_Forbidden() {
//      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        authyStepUpAccepted(identity.getRight(), secretKeyScaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void AuthyStepup_PatchManagedCards_Success() {
//      scaMC: true

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_AssignManagedCards_Success() {
//      scaMC: true

        final List<UnassignedCardResponseModel> corporateUnassignedCards = ManagedCardsHelper.replenishPrepaidCardPool(
                getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, getIdentityCardLevelClassificationScaMcApp(), InstrumentType.VIRTUAL, innovatorTokenScaMcApp);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(0).getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKeyScaMcApp, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_UpgradeManagedCardToPhysical_Success() {
        //      scaMC: true

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_ActivateManagedCard_Success() {
//      scaMC: true

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_GetPhysicalCardPin_Success() {
//      scaMC: true

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_ReplaceDamagedCard_Success() {
//      scaMC: true

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void AuthyStepup_ReplaceLostStolenCard_Success(final DestroyedReason destroyedReason) {
//      scaMC: true

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        reportLostOrStolen(cardId, getIdentityTokenScaMcApp(), destroyedReason, secretKeyScaMcApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepup_GetManagedCardsStatement_Success() {
//      scaMC: true

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);

        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void AuthyStepupFailed_CreateManagedCard_FallbackWithOtp_Success() {
//      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKeyScaMcApp, identity.getRight());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY).build();

        ManagedCardsService.createManagedCard(createManagedCardModel,secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMcApp, identity.getRight() );

        ManagedCardsService.createManagedCard(createManagedCardModel,secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AuthyStepupFailed_CreateManagedCard_FallbackWithOtp_Forbidden() {
//      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKeyScaMcApp, identity.getRight());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY).build();

        ManagedCardsService.createManagedCard(createManagedCardModel,secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startStepup(EnrolmentChannel.SMS.name(), secretKeyScaMcApp, identity.getRight() );

        ManagedCardsService.createManagedCard(createManagedCardModel,secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void AuthyStepupFailed_CreateManagedCard_FallbackWithOtp_Conflict() {
//      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKeyScaMcApp, identity.getRight());

        final String sessionId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKeyScaMcApp, identity.getRight());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY).build();

        ManagedCardsService.createManagedCard(createManagedCardModel,secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMcApp, identity.getRight() );

        ManagedCardsService.createManagedCard(createManagedCardModel,secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        SimulatorHelper.successfullyDeclinedAuthyStepUp(secretKeyScaMcApp, sessionId);
    }

    @Test
    public void AuthyStepup_GetSends_Success() {
//      scaMA: true

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getSendDestinationIdentityTokenScaMaApp());

        sendFundsMaToMc(managedAccountId, managedCardId, getIdentityTokenScaMaApp(), secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);

        SendsService.getSends(secretKeyScaMaApp, Optional.empty(), getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void AuthyStepup_GetTransfers_Success() {
//      scaMA: true

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        transferFundsMaToMc(managedAccountId, managedCardId, getIdentityTokenScaMaApp(), secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersService.getTransfers(secretKeyScaMaApp, Optional.empty(), getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void AuthyStepup_GetOutgoingWireTransfers_Success() {
//      scaMA: true

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());
        sendOwt(managedAccountId, getIdentityTokenScaMaApp(), secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfers(secretKeyScaMaApp, Optional.empty(), getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void AuthyStepup_GetOutgoingWireTransferById_Success() {
//      scaMA: true

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());
        final String owtId = sendOwt(managedAccountId, getIdentityTokenScaMaApp(), secretKeyScaMaApp, innovatorIdScaMaApp, outgoingWireTransfersProfileIdScaMaApp);

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKeyScaMaApp, owtId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(owtId));
    }

    @Test
    public void AuthyStepup_GetTransferById_Success() {
//      scaMA: true

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());
        final String transactionId = transferFundsMaToMc(managedAccountId, managedCardId, getIdentityTokenScaMaApp(), secretKeyScaMaApp, innovatorIdScaMaApp, transfersProfileIdScaMaApp);

        TransfersService.getTransfer(secretKeyScaMaApp, transactionId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    @Test
    public void AuthyStepup_GetSendById_Success() {
//      scaMA: true

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());
        final String managedCardId = ManagedCardsHelper.createManagedCard(getIdentityManagedCardsProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getSendDestinationIdentityTokenScaMaApp());
        final String transactionId = sendFundsMaToMc(managedAccountId, managedCardId, getIdentityTokenScaMaApp(), secretKeyScaMaApp, innovatorIdScaMaApp, sendsProfileIdScaMaApp);

        SendsService.getSend(secretKeyScaMaApp, transactionId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(transactionId));
    }

    private void reportLostOrStolen(final String managedCardId, final String token,
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

    protected static void authyStepUpAccepted(final String identityToken, final String secretKey){
        final String sessionId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, identityToken);
        SimulatorHelper.acceptAuthyStepUp(secretKey, sessionId);

        checkAuthySessionState(sessionId, "VERIFIED");
    }

    public void authyStepUpRejected(final String identityToken, final String secretKey){
        String sessionId = AuthenticationHelper.issuePushStepup(EnrolmentChannel.AUTHY.name(), secretKey, identityToken);
        SimulatorHelper.rejectAuthyStepUp(secretKey, sessionId);

        checkAuthySessionState(sessionId, "DECLINED");
    }

    private static void checkAuthySessionState(final String sessionId,
                                               final String expectedStatus) {
        TestHelper.ensureDatabaseResultAsExpected(60,
                () -> AuthyDatabaseHelper.getRequestState(sessionId),
                x -> !x.isEmpty() && x.get(0).get("status").equals(expectedStatus),
                Optional.of(String.format("Authy request with session id %s not in state %s as expected", sessionId, expectedStatus)));
    }
}
