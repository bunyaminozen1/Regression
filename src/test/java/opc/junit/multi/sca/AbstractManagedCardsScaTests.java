package opc.junit.multi.sca;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.AcceptedResponse;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.DestroyedReason;
import opc.enums.opc.InstrumentType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.innovator.UnassignedCardResponseModel;
import opc.models.multi.managedcards.ActivatePhysicalCardModel;
import opc.models.multi.managedcards.AssignManagedCardModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.multi.managedcards.ReplacePhysicalCardModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.PasswordsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@Tag(MultiTags.SCA_MANAGED_CARDS)
public abstract class AbstractManagedCardsScaTests extends BaseScaSetup {
    protected abstract String getIdentityTokenScaApp();

    protected abstract String getIdentityPrepaidManagedCardProfileScaApp();

    protected abstract String getIdentityTokenScaMaApp();

    protected abstract String getIdentityPrepaidManagedCardProfileScaMaApp();

    protected abstract CardLevelClassification getIdentityCardLevelClassificationScaMaApp();

    protected abstract String getIdentityTokenScaMcApp();

    protected abstract String getIdentityPrepaidManagedCardProfileScaMcApp();

    protected abstract String getIdentityEmailScaMcApp();

    protected abstract CardLevelClassification getIdentityCardLevelClassificationScaMcApp();

    protected abstract Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme);

    protected abstract Pair<String, String> createIdentity(final ProgrammeDetailsModel programme);

    private final static String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;
    private final static String OTP_VERIFICATION_CODE = TestHelper.OTP_VERIFICATION_CODE;
    private final static String CHANNEL = "SMS";
    private final static String CURRENCY = "EUR";
    private final static int SCA_EXPIRED_TIME = 61000;

    @Test
    public void GetManagedCards_ScaMcTrue_Success() {
        //      scaMC: true
        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCards_SeveralManagedCards_Success() {
        //      scaMC: true
        final List<String> cards = new ArrayList<>();

        IntStream.range(0, 3).forEach(x ->
                cards.add(ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp())));

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(cards.size()))
                .body("responseCount", greaterThanOrEqualTo(cards.size()));
    }

    @Test
    public void GetManagedCards_CallEndpointTwice_Success() {
        //      scaMC: true
        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCards_TwoSessions_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final ValidatableResponse firstResponse = ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_OK);


        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        final ValidatableResponse secondResponse = ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_OK);

        String firstCallManagedCardId = firstResponse.extract().jsonPath().get("cards.id").toString();
        String secondCallManagedCardId = secondResponse.extract().jsonPath().get("cards.id").toString();
        Assertions.assertEquals(firstCallManagedCardId, secondCallManagedCardId);
    }

    @Test
    public void GetManagedCards_SecondSessionNoStepUp_Forbidden() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

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
    public void GetManagedCards_ScaMcFalse_Success() {
        //      scaMA: true

        final Pair<String, String> identity = createIdentity(scaMaApp);

        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        ManagedCardsService.getManagedCards(secretKeyScaMaApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCards_NoStepUp_Forbidden() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedCards_FirstSessionNoStepUp_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String tokenWithoutStepUp = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), tokenWithoutStepUp)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));


        AuthenticationHelper.logout(tokenWithoutStepUp, secretKeyScaMcApp);
        String secondToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, secondToken);

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), secondToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCardsById_ScaMcTrue_Success() {
        //      scaMC: true
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCardsById_CallEndpointTwice_Success() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCardsById_TwoSessions_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCardsById_SecondSessionNoStepUp_Forbidden() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

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
    public void GetManagedCardsById_ScaMcFalse_Success() {
        //      scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        ManagedCardsService.getManagedCard(secretKeyScaMaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetManagedCardsById_NoStepUp_Forbidden() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedCardsById_FirstSessionNoStepUp_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String tokenWithoutStepUp = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, tokenWithoutStepUp)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));


        AuthenticationHelper.logout(tokenWithoutStepUp, secretKeyScaMcApp);
        String secondToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, secondToken);

        ManagedCardsService.getManagedCard(secretKeyScaMcApp, cardId, secondToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchManagedCards_ScaMcTrue_Success() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchManagedCards_ScaMcFalseNoStepUp_Success() {
        //      scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMaApp, cardId, identity
                .getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchManagedCards_NoStepUp_Forbidden() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PatchManagedCards_CallEndpointTwice_Success() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();
        final ValidatableResponse firstResponse = ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp,
                        cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        final PatchManagedCardModel patchManagedCardModelSecondResponse =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();
        final ValidatableResponse secondResponse = ManagedCardsService.patchManagedCard(patchManagedCardModelSecondResponse,
                        secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        String actualTagFirstResponse = firstResponse.extract().jsonPath().get("tag");
        String actualTagSecondResponse = secondResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(actualTagFirstResponse, actualTagSecondResponse);
    }

    @Test
    public void PatchManagedCards_TwoSessions_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        final PatchManagedCardModel patchManagedCardModelSecondSession =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModelSecondSession, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchManagedCards_FirstSessionNoStepUp_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String tokenWithoutStepUp = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKeyScaMcApp, cardId, tokenWithoutStepUp)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));


        AuthenticationHelper.logout(tokenWithoutStepUp, secretKeyScaMcApp);
        String secondToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, secondToken);

        final PatchManagedCardModel patchManagedCardModelSecondSession =
                PatchManagedCardModel.DefaultPatchManagedCardModel().build();
        ManagedCardsService.patchManagedCard(patchManagedCardModelSecondSession, secretKeyScaMcApp, cardId, secondToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchManagedCards_SecondSessionNoStepUp_Forbidden() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

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
    public void AssignManagedCards_ScaMcTrue_Success() {
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
    public void AssignManagedCards_ScaMcFalseStepUpFalse_Success() {
        //      scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);
        final List<UnassignedCardResponseModel> corporateUnassignedCards = ManagedCardsHelper.replenishPrepaidCardPool(
                getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY, getIdentityCardLevelClassificationScaMaApp(), InstrumentType.VIRTUAL, innovatorTokenScaMaApp);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(0).getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKeyScaMaApp, identity.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AssignManagedCards_NoStepUp_StepUpRequired() {
        //      scaMC: true
        final Pair<String, String> identity = createIdentity(scaMcApp);

        final List<UnassignedCardResponseModel> corporateUnassignedCards = ManagedCardsHelper.replenishPrepaidCardPool(
                getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, getIdentityCardLevelClassificationScaMcApp(), InstrumentType.VIRTUAL, innovatorTokenScaMcApp);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(0).getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKeyScaMcApp, identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void AssignManagedCards_ScaMcTrueCallEndpointTwice_Success() {
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

        final AssignManagedCardModel assignManagedCardModelSecondCall =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(1).getExternalHandle())
                        .build();
        ManagedCardsService.assignManagedCard(assignManagedCardModelSecondCall, secretKeyScaMcApp, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AssignManagedCards_TwoSessions_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final List<UnassignedCardResponseModel> corporateUnassignedCards = ManagedCardsHelper.replenishPrepaidCardPool(
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
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        final AssignManagedCardModel assignManagedCardModelSecondSession =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(1).getExternalHandle())
                        .build();
        ManagedCardsService.assignManagedCard(assignManagedCardModelSecondSession, secretKeyScaMcApp, newToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void AssignManagedCards_SecondSessionNoStepUp_StepUpRequired() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

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
    public void AssignManagedCards_FirstSessionNoStepUp_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createIdentity(scaMcApp);

        final List<UnassignedCardResponseModel> corporateUnassignedCards = ManagedCardsHelper.replenishPrepaidCardPool(
                getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, getIdentityCardLevelClassificationScaMcApp(), InstrumentType.VIRTUAL, innovatorTokenScaMcApp);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(corporateUnassignedCards.get(0).getExternalHandle())
                        .build();
        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKeyScaMcApp, identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKeyScaMcApp, newToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void UpgradeManagedCardToPhysical_ScaMcTrue_Success() {
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
    public void UpgradeManagedCardToPhysical_ScaMcFalse_Success() {
        //      scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void UpgradeManagedCardToPhysical_NoStepUp_StepUpRequired() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void UpgradeManagedCardToPhysical_CallEndpointTwice_AlreadyUpgraded() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_UPGRADED_TO_PHYSICAL"));
    }

    @Test
    public void UpgradeManagedCardToPhysical_TwoSessions_AlreadyUpgraded() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_UPGRADED_TO_PHYSICAL"));
    }

    @Test
    public void UpgradeManagedCardToPhysical_SecondSessionNoStepUp_StepUpRequired() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

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
    public void UpgradeManagedCardToPhysical_FirstSessionNoStepUp_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        final String tokenWithoutStepUp = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
                UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(physicalCardAddressModel, VERIFICATION_CODE).build();

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, tokenWithoutStepUp)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.logout(tokenWithoutStepUp, secretKeyScaMcApp);
        String secondToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, secondToken);

        ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKeyScaMcApp, cardId, secondToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void ActivateManagedCard_ScaMcTrue_Success() {
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
    public void ActivateManagedCard_ScaMcFalse_Success() {
        //      scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaMaApp, cardId, identity.getRight(), physicalCardAddressModel);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void ActivateManagedCard_NoStepUp_StepUpRequired() {

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaApp(), CURRENCY, secretKeyScaApp, getIdentityTokenScaApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaApp, cardId, getIdentityTokenScaApp(), physicalCardAddressModel);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, true);
        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaApp, cardId, getIdentityTokenScaApp())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void ActivateManagedCard_CallEndpointTwice_AlreadyActivated() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_ACTIVATED"));
    }

    @Test
    public void ActivateManagedCard_TwoSessions_AlreadyActivated() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_ACTIVATED"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void ActivateManagedCard_FirstSessionNoStepUp_Success() {
        final Pair<String, String> identity = createIdentity(scaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaApp(), CURRENCY, secretKeyScaApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeManagedCardToPhysical(secretKeyScaApp, cardId, identity.getRight(), physicalCardAddressModel);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, true);

        final ActivatePhysicalCardModel activatePhysicalCardModel = new ActivatePhysicalCardModel(VERIFICATION_CODE);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));


        AuthenticationHelper.logout(identity.getRight(), secretKeyScaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaApp, newToken);

        ManagedCardsService.activatePhysicalCard(activatePhysicalCardModel, secretKeyScaApp, cardId, newToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void ActivateManagedCard_SecondSessionNoStepUp_StepUpRequired() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

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
    public void GetPhysicalCardPin_ScaMcTrue_Success() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetPhysicalCardPin_ScaMcFalse_Success() {
        //      scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMaApp, cardId, identity.getRight(), physicalCardAddressModel);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetPhysicalCardPin_NoStepUp_StepUpRequired() {
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetPhysicalCardPin_CallEndpointTwice_Success() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetPhysicalCardPin_TwoSessions_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_OK);
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetPhysicalCardPin_FirstSessionsNoStepUp_Success() {
        final Pair<String, String> identity = createIdentity(scaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaApp(), CURRENCY, secretKeyScaApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaApp, cardId, identity.getRight(), physicalCardAddressModel);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, true);
        ManagedCardsService.getPhysicalCardPin(secretKeyScaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaApp, newToken);

        ManagedCardsService.getPhysicalCardPin(secretKeyScaApp, cardId, newToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetPhysicalCardPin_SecondSessionsNoStepUp_StepUpRequired() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

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
    public void ReplaceDamagedCard_ScaMcTrue_Success() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ReplaceDamagedCard_ScaMcFalse_Success() {
        //      scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMaApp, cardId, identity.getRight(), physicalCardAddressModel);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ReplaceDamagedCard_NoStepUp_StepUpRequired() {
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void ReplaceDamagedCard_CallEndpointTwice_AlreadyRequested() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_PENDING_REPLACEMENT"));
    }

    @Test
    public void ReplaceDamagedCard_TwoSessions_AlreadyRequested() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_PENDING_REPLACEMENT"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void ReplaceDamagedCard_FirstSessionNoStepUp_StepUpRequired() {
        final Pair<String, String> identity = createIdentity(scaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaApp(), CURRENCY, secretKeyScaApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaApp, cardId, identity.getRight(), physicalCardAddressModel);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, true);
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaApp, newToken);

        ManagedCardsService.replaceDamagedCard(replacePhysicalCardModel, secretKeyScaApp, cardId, newToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ReplaceDamagedCard_SecondSession_StepUpRequired() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

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
    public void ReplaceLostStolenCard_ScaMcTrue_Success(final DestroyedReason destroyedReason) {
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

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void ReplaceLostStolenCard_ScaMcFalse_Success(final DestroyedReason destroyedReason) {
        //      scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMaApp, cardId, identity.getRight(), physicalCardAddressModel);

        reportLostOrStolen(cardId, identity.getRight(), destroyedReason, secretKeyScaMaApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void ReplaceLostStolenCard_NoStepUp_StepUpRequired(final DestroyedReason destroyedReason) {
        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaApp(), CURRENCY, secretKeyScaApp, getIdentityTokenScaApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaApp, cardId, getIdentityTokenScaApp(), physicalCardAddressModel);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, true);
        reportLostOrStolen(cardId, getIdentityTokenScaApp(), destroyedReason, secretKeyScaApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaApp, cardId, getIdentityTokenScaApp())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void ReplaceLostStolenCard_CallEndpointTwice_AlreadyReplaced(final DestroyedReason destroyedReason) {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), physicalCardAddressModel);

        reportLostOrStolen(cardId, getIdentityTokenScaMcApp(), destroyedReason, secretKeyScaMcApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_REPLACED"));
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void ReplaceLostStolenCard_TwoSessions_AlreadyReplaced(final DestroyedReason destroyedReason) {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaMcApp, cardId, identity.getRight(), physicalCardAddressModel);

        reportLostOrStolen(cardId, identity.getRight(), destroyedReason, secretKeyScaMcApp);

        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaMcApp, cardId, newToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_ALREADY_REPLACED"));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void ReplaceLostStolenCard_FirstSessionNoStepUp_Success(final DestroyedReason destroyedReason) {
        final Pair<String, String> identity = createIdentity(scaApp);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaApp(), CURRENCY, secretKeyScaApp, identity.getRight());

        final PhysicalCardAddressModel physicalCardAddressModel = PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build();
        ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKeyScaApp, cardId, identity.getRight(), physicalCardAddressModel);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, true);
        reportLostOrStolen(cardId, identity.getRight(), destroyedReason, secretKeyScaApp);
        final ReplacePhysicalCardModel replacePhysicalCardModel = new ReplacePhysicalCardModel(VERIFICATION_CODE);
        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaApp, cardId, identity.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaApp, newToken);

        ManagedCardsService.replaceLostOrStolenCard(replacePhysicalCardModel, secretKeyScaApp, cardId, newToken)
                .then()
                .statusCode(SC_OK);
    }

    @ParameterizedTest
    @EnumSource(value = DestroyedReason.class, names = {"LOST", "STOLEN"})
    public void ReplaceLostStolenCard_SecondSessionNoSteUp_StepUpRequired(final DestroyedReason destroyedReason) {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

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
    public void GetManagedCardsStatement_OneSessionStepUpTrue_Success() {
        //      scaMC: true
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);

        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, getIdentityTokenScaMcApp(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetManagedCardsStatement_OneSessionStepUpFalse_Forbidden() {
        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaApp(), CURRENCY, secretKeyScaApp, getIdentityTokenScaApp());
        AdminHelper.fundManagedCard(innovatorIdScaApp, cardId, CURRENCY, 1000L);

        AdminHelper.setSca(adminToken, programmeIdScaApp, false, true);
        ManagedCardsService.getManagedCardStatement(secretKeyScaApp, cardId, getIdentityTokenScaApp(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedCardsStatement_StepUpExpired_Forbidden() throws InterruptedException {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);
        ManagedCardsHelper.getManagedCardStatement(cardId, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, newToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedCardsStatement_StepUpActive_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);

        ManagedCardsHelper.getManagedCardStatement(cardId, secretKeyScaMcApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, newToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCardsStatement_StepUpValidEntryLimit_SuccessLastEntry() throws InterruptedException {
        // initial session: first transaction
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);
        ManagedCardsHelper.getManagedCardStatement(cardId, secretKeyScaMcApp, identity.getRight());

        //second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        ManagedCardsHelper.getManagedCardStatementForbidden(cardId, secretKeyScaMcApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, secondSessionToken);
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);
        ManagedCardsHelper.getManagedCardStatement(cardId, secretKeyScaMcApp, secondSessionToken);

        //third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
        //Retrieve last transaction -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMcApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);
        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, thirdSessionToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedCardsStatement_StepUpValidEntryLimit_ForbiddenAllEntries() throws InterruptedException {
        // initial session: first transaction
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);
        ManagedCardsHelper.getManagedCardStatement(cardId, secretKeyScaMcApp, identity.getRight());

        //second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        ManagedCardsHelper.getManagedCardStatementForbidden(cardId, secretKeyScaMcApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, secondSessionToken);
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);
        ManagedCardsHelper.getManagedCardStatement(cardId, secretKeyScaMcApp, secondSessionToken);

        //third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
        //Retrieve last transaction -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMcApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 4);
        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, thirdSessionToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

    }

    @Test
    public void GetManagedCardsStatement_NewStepUp_Success() throws InterruptedException {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);
        final String cardId = ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());
        AdminHelper.fundManagedCard(innovatorIdScaMcApp, cardId, CURRENCY, 1000L);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        ManagedCardsHelper.getManagedCardStatementForbidden(cardId, secretKeyScaMcApp, newToken);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);
        ManagedCardsService.getManagedCardStatement(secretKeyScaMcApp, cardId, newToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void CreateManagedCard_ScaMcTrue_Success() {
        //      scaMC: true
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, getIdentityTokenScaMcApp(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateManagedCard_CallEndpointTwice_Success() {
        //      scaMC: true
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, getIdentityTokenScaMcApp(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, getIdentityTokenScaMcApp(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateManagedCard_TwoSessions_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY).build();

        final ValidatableResponse firstResponse = ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        final ValidatableResponse secondResponse = ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, newToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final String firstCallManagedCardId = firstResponse.extract().jsonPath().get("id").toString();
        final String secondCallManagedCardId = secondResponse.extract().jsonPath().get("id").toString();
        Assertions.assertNotEquals(firstCallManagedCardId, secondCallManagedCardId);
    }

    @Test
    public void CreateManagedCard_SecondSessionNoStepUp_Forbidden() {
        //      scaMC: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, getIdentityTokenScaMcApp(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, newToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void CreateManagedCard_ScaMcFalse_Success() {
        //      scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMaApp(), CURRENCY).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateManagedCard_NoStepUp_Forbidden() {
        //      scaMC: true
        final Pair<String, String> identity = createIdentity(scaMcApp);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void CreateManagedCard_FirstSessionNoStepUp_Success() {
        //      scaMC: true
        final Pair<String, String> identity = createIdentity(scaMcApp);

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY).build();

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMcApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMcApp);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        ManagedCardsService.createManagedCard(createManagedCardModel, secretKeyScaMcApp, newToken, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    /**
     * When user trigger lost password process, validity of stepped up token expired, and user should issue step up challenge once more.
     */
    @Test//****
    public void GetManagedCardStatements_ForgotPasswordIssueStepUpAgain_Success() {

        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        ManagedCardsHelper.createManagedCard(getIdentityPrepaidManagedCardProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, identity.getRight());

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_OK);

        PasswordsService.startLostPassword(new LostPasswordStartModel(identity.getLeft()), secretKeyScaMcApp)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(identity.getLeft())
                        .setNewPassword(new PasswordModel("8765"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        final String newToken = PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKeyScaMcApp)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("token");

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMcApp, newToken);

        ManagedCardsService.getManagedCards(secretKeyScaMcApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_OK);
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
}
