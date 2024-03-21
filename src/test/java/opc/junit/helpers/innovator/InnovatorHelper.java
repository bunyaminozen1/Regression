package opc.junit.helpers.innovator;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;
import commons.config.ConfigHelper;
import commons.enums.Jurisdiction;
import commons.enums.PaymentModel;
import fpi.helpers.PaymentRunHelper;
import fpi.paymentrun.enums.PluginEnvironment;
import fpi.paymentrun.models.innovator.PaymentRunProfilesResponse;
import io.restassured.response.Response;
import io.vavr.Tuple5;
import opc.enums.opc.CardBureau;
import opc.enums.opc.FundingType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.PasswordConstraint;
import opc.enums.opc.PluginStatus;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.models.admin.CreatePluginModel;
import opc.models.admin.ScaConfigModel;
import opc.models.admin.SetScaModel;
import opc.models.innovator.AbstractUpdateManagedCardsProfileModel;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.CreateConsumerProfileModel;
import opc.models.innovator.CreateCorporateProfileModel;
import opc.models.innovator.CreateManagedAccountProfileModel;
import opc.models.innovator.CreateManagedCardsProfileV2Model;
import opc.models.innovator.CreateOwtProfileModel;
import opc.models.innovator.CreatePasswordProfileModel;
import opc.models.innovator.CreateProgrammeModel;
import opc.models.innovator.CreateSendProfileModel;
import opc.models.innovator.CreateTransfersProfileModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.innovator.DigitalWalletsEnabledModel;
import opc.models.innovator.GetPluginsResponseModel;
import opc.models.innovator.GetProgrammesResponseModel;
import opc.models.innovator.InnovatorRegistrationModel;
import opc.models.innovator.PasswordConstraintsModel;
import opc.models.innovator.RevenueStatementRequestModel;
import opc.models.innovator.SpendRulesModel;
import opc.models.innovator.UpdateConsumerProfileModel;
import opc.models.innovator.UpdateCorporateProfileModel;
import opc.models.innovator.UpdateDebitManagedCardsProfileModel;
import opc.models.innovator.UpdateManagedCardsProfileV2Model;
import opc.models.innovator.UpdateOwtProfileModel;
import opc.models.innovator.UpdatePrepaidManagedCardsProfileModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.shared.FinInstitutionProfileModel;
import opc.models.shared.GetCorporateBeneficiariesModel;
import opc.models.shared.GetInnovatorUserModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.ProgrammeConfigsModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class InnovatorHelper {

    public static Tuple5<String, String, String, String, String> registerLoggedInInnovatorWithProgramme() {

        final InnovatorRegistrationModel registrationModel =
                InnovatorRegistrationModel.RandomInnovatorRegistrationModel()
                        .build();

        return registerLoggedInInnovatorWithProgramme(registrationModel);
    }

    public static Tuple5<String, String, String, String, String> registerLoggedInInnovatorWithProgramme(final InnovatorRegistrationModel innovatorRegistrationModel) {

        final String innovatorId =
                TestHelper.ensureAsExpected(15, () -> InnovatorService.registerInnovator(innovatorRegistrationModel), SC_OK)
                        .jsonPath()
                        .get("innovatorId");

        final String token =
                TestHelper.ensureAsExpected(15,
                                () -> InnovatorService.loginInnovator(new LoginModel(innovatorRegistrationModel.getEmail(), innovatorRegistrationModel.getPassword())),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        final String programmeId = TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createProgramme(CreateProgrammeModel.InitialProgrammeModel(), token),
                        SC_OK)
                .jsonPath()
                .get("id");

        final String secretKey =
                TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getProgrammeDetails(token, programmeId),
                        SC_OK).jsonPath().get("secretKey");

        final String sharedKey =
                TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getProgrammeDetails(token, programmeId),
                        SC_OK).jsonPath().get("sharedKey");

        return new Tuple5<>(innovatorId, programmeId, token, secretKey, sharedKey);
    }

    public static Pair<String, String> registerLoggedInInnovator() {
        InnovatorRegistrationModel innovatorRegistrationModel =
                InnovatorRegistrationModel.RandomInnovatorRegistrationModel()
                        .build();

        final String innovatorId =
                TestHelper.ensureAsExpected(15, () -> InnovatorService.registerInnovator(innovatorRegistrationModel), SC_OK)
                        .jsonPath()
                        .get("innovatorId");

        final String token =
                TestHelper.ensureAsExpected(15,
                                () -> InnovatorService.loginInnovator(new LoginModel(innovatorRegistrationModel.getEmail(), innovatorRegistrationModel.getPassword())),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        return Pair.of(innovatorId, token);
    }

    public static Triple<String, String, String> createProgramme(final String innovatorToken) {

        final String programmeId = TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createProgramme(CreateProgrammeModel.InitialProgrammeModel(), innovatorToken),
                        SC_OK)
                .jsonPath()
                .get("id");

        final String secretKey =
                TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getProgrammeDetails(innovatorToken, programmeId),
                        SC_OK).jsonPath().get("secretKey");

        final String sharedKey =
                TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getProgrammeDetails(innovatorToken, programmeId),
                        SC_OK).jsonPath().get("sharedKey");

        return Triple.of(programmeId, secretKey, sharedKey);
    }

    public static String createManagedAccountProfile(final String token,
                                                     final String programmeId,
                                                     final IdentityType identityType,
                                                     final Jurisdiction jurisdiction) {

        final CreateManagedAccountProfileModel createManagedAccountProfileModel =
                CreateManagedAccountProfileModel.DefaultCreateManagedAccountProfileModel(identityType, jurisdiction);
        return createManagedAccountProfile(createManagedAccountProfileModel, token, programmeId);
    }

    public static String createManagedAccountProfile(final CreateManagedAccountProfileModel createManagedAccountProfileModel,
                                                     final String token,
                                                     final String programmeId) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createManagedAccountProfile(createManagedAccountProfileModel,
                                token,
                                programmeId), SC_OK)
                .jsonPath()
                .get("profile.id");
    }

    public static String createModulrManagedAccountProfile(final String token, final String programmeId, final IdentityType identityType) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createManagedAccountProfile(CreateManagedAccountProfileModel.ModulrCreateManagedAccountProfileModel(identityType),
                                token,
                                programmeId), SC_OK)
                .jsonPath()
                .get("profile.id");
    }

    public static String createPayneticsModulrManagedAccountProfile(final String token, final String programmeId, final IdentityType identityType) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createManagedAccountProfile(CreateManagedAccountProfileModel.PayneticsModulrCreateManagedAccountProfileModel(identityType),
                                token,
                                programmeId), SC_OK)
                .jsonPath()
                .get("profile.id");
    }

    public static String createTransfersProfile(final String token, final String programmeId) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createTransfersProfileV2(CreateTransfersProfileModel.DefaultCreateTransfersProfileModel(),
                                token,
                                programmeId), SC_OK)
                .jsonPath()
                .get("profile.id");
    }

    public static String createCorporateProfile(final CreateCorporateProfileModel createCorporateProfileModel,
                                                final String token,
                                                final String programmeId) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createCorporateProfile(createCorporateProfileModel, token, programmeId), SC_OK)
                .jsonPath()
                .get("profile.id");
    }

    public static String createConsumerProfile(final CreateConsumerProfileModel createConsumerProfileModel,
                                               final String token,
                                               final String programmeId) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createConsumerProfile(createConsumerProfileModel, token, programmeId), SC_OK)
                .jsonPath()
                .get("profile.id");
    }

    public static void createPasswordProfile(final String token,
                                             final String programmeId,
                                             final String profileId) {
        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.createPasswordProfile(CreatePasswordProfileModel.DefaultCreatePasswordProfileModel(),
                        token,
                        programmeId,
                        profileId), SC_OK);
    }

    public static void createCorporateProfileWithPassword(final String token, final String programmeId) {

        final CreateCorporateProfileModel createCorporateProfileModel =
                CreateCorporateProfileModel.DefaultCreateCorporateProfileModel().build();

        createCorporateProfileWithPassword(createCorporateProfileModel, token, programmeId);
    }

    public static String createCorporateProfileWithPassword(final String token,
                                                            final String programmeId,
                                                            final ProgrammeConfigsModel programmeConfigs) {

        final CreateCorporateProfileModel createCorporateProfileModel =
                CreateCorporateProfileModel.DefaultCreateCorporateProfileModel(programmeConfigs).build();

        return createCorporateProfileWithPassword(createCorporateProfileModel, token, programmeId);
    }

    public static String createCorporateProfileWithPassword(final CreateCorporateProfileModel createCorporateProfileModel,
                                                            final String token,
                                                            final String programmeId) {

        final String corporateProfileId =
                createCorporateProfile(createCorporateProfileModel, token, programmeId);

        createPasswordProfile(token, programmeId, corporateProfileId);

        return corporateProfileId;
    }

    public static String createConsumerProfileWithPassword(final String token,
                                                           final String programmeId,
                                                           final ProgrammeConfigsModel programmeConfigs) {

        final String consumerProfileId = createConsumerProfile(CreateConsumerProfileModel.DefaultCreateConsumerProfileModel(programmeConfigs), token, programmeId);

        createPasswordProfile(token, programmeId, consumerProfileId);

        return consumerProfileId;
    }

    public static String createThreeDSCorporateProfileWithPassword(final String token, final String programmeId) {

        final String corporateProfileId =
                createCorporateProfile(CreateCorporateProfileModel.DefaultThreeDSCreateCorporateProfileModel().build(), token, programmeId);

        createPasswordProfile(token, programmeId, corporateProfileId);

        return corporateProfileId;
    }

    public static String createThreeDSConsumerProfileWithPassword(final String token, final String programmeId) {

        final String consumerProfileId = createConsumerProfile(CreateConsumerProfileModel.DefaultThreeDSCreateConsumerProfileModel(), token, programmeId);

        createPasswordProfile(token, programmeId, consumerProfileId);

        return consumerProfileId;
    }

    public static String createSecondaryThreeDSCorporateProfileWithPassword(final String token, final String programmeId) {

        final String corporateProfileId =
                createCorporateProfile(CreateCorporateProfileModel.SecondaryThreeDSCreateCorporateProfileModel().build(), token, programmeId);

        createPasswordProfile(token, programmeId, corporateProfileId);

        return corporateProfileId;
    }

    public static String createSecondaryThreeDSConsumerProfileWithPassword(final String token, final String programmeId) {

        final String consumerProfileId = createConsumerProfile(CreateConsumerProfileModel.SecondaryThreeDSCreateConsumerProfileModel(), token, programmeId);

        createPasswordProfile(token, programmeId, consumerProfileId);

        return consumerProfileId;
    }

    public static String createNitecrestPrepaidManagedCardsProfileV2(final String token, final String programmeId, final IdentityType identityType) {
        return createNitecrestPrepaidManagedCardsProfileV2(token, programmeId, identityType, Jurisdiction.EEA);
    }

    public static String createNitecrestPrepaidManagedCardsProfileV2(final String token,
                                                                     final String programmeId,
                                                                     final IdentityType identityType,
                                                                     final Jurisdiction jurisdiction) {
        return TestHelper.ensureAsExpected(15,
                        () -> opc.services.innovatornew.InnovatorService
                                .createManagedCardsProfileV2(CreateManagedCardsProfileV2Model.DefaultCreatePrepaidManagedCardsProfileV2Model(identityType, CardBureau.NITECREST, jurisdiction).build(),
                                        token,
                                        programmeId), SC_OK)
                .jsonPath()
                .get("prepaidManagedCardsProfile.managedCardsProfile.profile.id");
    }

    public static String createNitecrestDebitManagedCardsProfileV2(final String token, final String programmeId, final IdentityType identityType) {
        return createNitecrestDebitManagedCardsProfileV2(token, programmeId, identityType, Jurisdiction.EEA);
    }

    public static String createNitecrestDebitManagedCardsProfileV2(final String token,
                                                                   final String programmeId,
                                                                   final IdentityType identityType,
                                                                   final Jurisdiction jurisdiction) {
        return TestHelper.ensureAsExpected(15,
                        () -> opc.services.innovatornew.InnovatorService
                                .createManagedCardsProfileV2(CreateManagedCardsProfileV2Model.DefaultCreateDebitManagedCardsProfileV2Model(identityType, CardBureau.NITECREST, jurisdiction).build(),
                                        token,
                                        programmeId), SC_OK)
                .jsonPath()
                .get("debitManagedCardsProfile.managedCardsProfile.profile.id");
    }

    public static String createDigiseqPrepaidManagedCardsProfileV2(final String token,
                                                                   final String programmeId,
                                                                   final IdentityType identityType,
                                                                   final Jurisdiction jurisdiction) {
        return TestHelper.ensureAsExpected(15,
                        () -> opc.services.innovatornew.InnovatorService
                                .createManagedCardsProfileV2(CreateManagedCardsProfileV2Model.DefaultCreatePrepaidManagedCardsProfileV2Model(identityType, CardBureau.DIGISEQ, jurisdiction).build(),
                                        token,
                                        programmeId), SC_OK)
                .jsonPath()
                .get("prepaidManagedCardsProfile.managedCardsProfile.profile.id");
    }

    public static String createDigiseqDebitManagedCardsProfileV2(final String token,
                                                                 final String programmeId,
                                                                 final IdentityType identityType,
                                                                 final Jurisdiction jurisdiction) {
        return TestHelper.ensureAsExpected(15,
                        () -> opc.services.innovatornew.InnovatorService
                                .createManagedCardsProfileV2(CreateManagedCardsProfileV2Model.DefaultCreateDebitManagedCardsProfileV2Model(identityType, CardBureau.DIGISEQ, jurisdiction).build(),
                                        token,
                                        programmeId), SC_OK)
                .jsonPath()
                .get("debitManagedCardsProfile.managedCardsProfile.profile.id");
    }

    public static String createSendsProfile(final String token, final String programmeId) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createSendsProfileV2(CreateSendProfileModel.DefaultCreateSendProfileModel(),
                                token,
                                programmeId), SC_OK)
                .jsonPath()
                .get("profile.id");
    }

    public static String createOwtProfile(final String token, final String programmeId) {
        final CreateOwtProfileModel createOwtProfileModel = CreateOwtProfileModel.DefaultCreateOwtProfileModel();
        return createOwtProfile(createOwtProfileModel, token, programmeId);
    }

    public static String createOwtProfile(final CreateOwtProfileModel createOwtProfileModel,
                                          final String token,
                                          final String programmeId) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createOwtProfile(createOwtProfileModel,
                                token,
                                programmeId), SC_OK)
                .jsonPath()
                .get("profile.id");
    }

    public static void updateOwtProfileReturnFeeDecision(final boolean returnFee, final String profileId, final String programmeId, final String token) {
        final UpdateOwtProfileModel returnModel = new UpdateOwtProfileModel.Builder().setReturnOwtFee(returnFee).build();

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateOwtProfile(returnModel, profileId, token, programmeId),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("returnOwtFee").equals(Boolean.toString(returnFee)),
                Optional.of(String.format("Expecting 200 with returnOwtFee %s, see logged payload", returnFee)));
    }

    public static void updateManagedCardsProfileToDebitMode(final String token, final String programmeId, final String profileId) {
        final UpdateManagedCardsProfileV2Model updateManagedCardsProfileV2Model =
                UpdateManagedCardsProfileV2Model.builder().setCardFundingType("DEBIT")
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateManagedCardsProfileV2(updateManagedCardsProfileV2Model,
                        token,
                        programmeId,
                        profileId), SC_OK);
    }

    public static void updateManagedCardsProfileToPrepaidMode(final String token, final String programmeId, final String profileId, final IdentityType identityType) {
        final UpdateManagedCardsProfileV2Model updateManagedCardsProfileV2Model =
                UpdateManagedCardsProfileV2Model.builder()
                        .setUpdatePrepaidProfileRequest(UpdatePrepaidManagedCardsProfileModel.DefaultUpdatePrepaidManagedCardsProfileModel(identityType).build())
                        .setCardFundingType("PREPAID")
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateManagedCardsProfileV2(updateManagedCardsProfileV2Model,
                        token,
                        programmeId,
                        profileId), SC_OK);
    }

    public static void enableWebhook(final UpdateProgrammeModel updateProgrammeModel,
                                     final String programmeId,
                                     final String token) {

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateProgramme(updateProgrammeModel, programmeId, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("webhookDisabled").equals("false"),
                Optional.of("Expecting 200 with webhooksDisabled field false, see logged payload"));
    }

    public static void disableWebhook(final UpdateProgrammeModel updateProgrammeModel,
                                      final String programmeId,
                                      final String token) {

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateProgramme(updateProgrammeModel, programmeId, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("webhookDisabled").equals("true"),
                Optional.of("Expecting 200 with webhooksDisabled field true, see logged payload"));
    }

    public static void enableAuthForwarding(final UpdateProgrammeModel updateProgrammeModel,
                                            final String programmeId,
                                            final String token) {
        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateProgramme(updateProgrammeModel, programmeId, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("authForwardingEnabled")
                        .equals(updateProgrammeModel.getAuthForwardingEnabled().toLowerCase()),
                Optional.of(String.format("Expecting 200 with auth forwarding enabled value %s, check logged payloads",
                        updateProgrammeModel.getAuthForwardingEnabled().toLowerCase())));
    }

    public static void enableSendsSca(final UpdateProgrammeModel updateProgrammeModel,
                                      final String programmeId,
                                      final String token) {

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateProgramme(updateProgrammeModel, programmeId, token),
                SC_OK);
    }

    public static void disableSendsSca(final UpdateProgrammeModel updateProgrammeModel,
                                       final String programmeId,
                                       final String token) {

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateProgramme(updateProgrammeModel, programmeId, token),
                SC_OK);
    }

    public static String loginInnovator(final String email, final String password) {
        return TestHelper.ensureAsExpected(30,
                        () -> InnovatorService.loginInnovator(new LoginModel(email, new PasswordModel(password))),
                        SC_OK)
                .jsonPath()
                .get("token");
    }

    public static void setProfileSpendRules(final SpendRulesModel spendRulesModel,
                                            final String token,
                                            final String programmeId,
                                            final String profileId) {
        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.setProfileSpendRules(spendRulesModel, token, programmeId, profileId),
                SC_OK);
    }

    public static void addCardSpendRules(final SpendRulesModel spendRulesModel,
                                         final String token,
                                         final String managedCardId) {
        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.addCardSpendRules(spendRulesModel, managedCardId, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(5,
                () -> InnovatorService.getCardSpendRules(token, managedCardId),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("cardLevelSpendRules.allowCreditAuthorisations").equals(spendRulesModel.getAllowCreditAuthorisations()),
                Optional.of(String.format("Spend rules not added for card with id %s", managedCardId)));
    }

    public static void addCardSpendRules(final SpendRulesModel spendRulesModel,
                                         final String token,
                                         final String managedCardId,
                                         final Function<Response, Boolean> testCondition) {
        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.addCardSpendRules(spendRulesModel, managedCardId, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(5,
                () -> InnovatorService.getCardSpendRules(token, managedCardId),
                testCondition,
                Optional.of(String.format("Spend rules not added for card with id %s", managedCardId)));
    }

    public static Response getInnovatorRevenueStatement(final String innovatorId,
                                                        final String token,
                                                        final RevenueStatementRequestModel revenueStatementRequestModel,
                                                        final int expectedCount) {
        return TestHelper.ensureAsExpected(60,
                () -> InnovatorService.getInnovatorRevenueStatement(innovatorId, token, revenueStatementRequestModel),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("responseCount").equals(expectedCount),
                Optional.of(String.format("Expecting 200 with response count %s, see logged payloads", expectedCount)));
    }

    public static void updateSecurityModel(final String programmeId) {

        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        final opc.models.admin.UpdateProgrammeModel updateProgrammeModel =
                opc.models.admin.UpdateProgrammeModel.builder()
                        .setWebhookDisabled(true)
                        .setSecurityModelConfig(securityModelConfig)
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.updateProgramme(updateProgrammeModel, programmeId, AdminService.loginAdmin()),
                SC_OK);
    }

    public static Pair<String, String> createNewInnovatorWithCorporateProfile(final CreateCorporateProfileModel createCorporateProfileModel) {
        final InnovatorRegistrationModel registrationModel =
                InnovatorRegistrationModel.RandomInnovatorRegistrationModel()
                        .build();

        final Tuple5<String, String, String, String, String> innovator = InnovatorHelper.registerLoggedInInnovatorWithProgramme(registrationModel);

        final String corporateProfileId =
                InnovatorHelper.createCorporateProfile(createCorporateProfileModel, innovator._3(), innovator._2());

        InnovatorHelper.createPasswordProfile(innovator._3(), innovator._2(), corporateProfileId);
        InnovatorHelper.updateSecurityModel(innovator._2());

        return Pair.of(innovator._4(), corporateProfileId);
    }

    public static Pair<String, String> createNewInnovatorWithConsumerProfile(final CreateConsumerProfileModel createConsumerProfileModel) {
        final InnovatorRegistrationModel registrationModel =
                InnovatorRegistrationModel.RandomInnovatorRegistrationModel()
                        .build();

        final Tuple5<String, String, String, String, String> innovator = InnovatorHelper.registerLoggedInInnovatorWithProgramme(registrationModel);

        final String consumerProfileId =
                InnovatorHelper.createConsumerProfile(createConsumerProfileModel, innovator._3(), innovator._2());

        InnovatorHelper.createPasswordProfile(innovator._3(), innovator._2(), consumerProfileId);
        InnovatorHelper.updateSecurityModel(innovator._2());

        return Pair.of(innovator._4(), consumerProfileId);
    }

    /**
     * Creates new innovator and programmes. Or restores the configuration of the programme.
     * If Innovator doesn't exist - creates new one and all programmes and profiles with config file (ProgrammeConfiguration)
     * If Innovator exists - checks if the programme exists:
     * - If not - creates new one with config file.
     * - If yes - updates (restores) config.
     * For new Innovator/Programmes: ProgrammeConfiguration file should be created for not existing (new) programmes before they are created.
     */
    public static void testDataInit(final String innovatorName,
                                    final String innovatorEmail,
                                    final List<String> programmeNames,
                                    final String configurationFilename,
                                    final String programmeConfigFilename,
                                    final Optional<PluginEnvironment> pluginEnvironment) throws IOException {

        final String adminToken = AdminService.loginAdmin();
        final Response isInnovatorExists = AdminHelper.getInnovatorUser(new GetInnovatorUserModel(innovatorEmail), adminToken);
        String innovatorId;

//        Check if Innovator exists
        if (Integer.parseInt(isInnovatorExists.jsonPath().getString("count")) > 0) {
            innovatorId = isInnovatorExists.jsonPath().get("innovatorUser[0].id");
        } else {
            final InnovatorRegistrationModel registrationModel =
                    InnovatorRegistrationModel.RandomInnovatorRegistrationModel().setName(innovatorName).setEmail(innovatorEmail).build();

            innovatorId =
                    TestHelper.ensureAsExpected(15, () -> InnovatorService.registerInnovator(registrationModel), SC_OK)
                            .jsonPath()
                            .get("innovatorId");
        }

        final String innovatorToken = loginInnovator(innovatorEmail, "Pass1234!");

//        Get configs from the file
        final List<ProgrammeConfigsModel> configs =
                Arrays.asList(new ObjectMapper()
                        .readValue(new File(String.format("src/test/resources/ProgrammeConfiguration/%s", programmeConfigFilename)),
                                ProgrammeConfigsModel[].class));

//        Get programmes for Innovator
        final List<Object> innovatorProgrammes = getProgrammes(innovatorToken).jsonPath().getList("programme");
        final List<GetProgrammesResponseModel> retrievedProgrammes = new ArrayList<>();
        innovatorProgrammes.forEach(x -> retrievedProgrammes.add(new ObjectMapper().convertValue(x, GetProgrammesResponseModel.class)));

        final List<ProgrammeDetailsModel> newProgrammes = new ArrayList<>();
        programmeNames.forEach(programmeName -> {

//            Get config by Programme name (programme config should be created for not existing (new) programmes before they are created)
            final ProgrammeConfigsModel programmeConfigs = configs.stream().filter(x -> x.getProgrammeName().equals(programmeName)).collect(Collectors.toList()).get(0);

//            Check if programme exists
            final boolean isProgrammeExists = retrievedProgrammes.stream().anyMatch(programme -> programme.getName().equals(programmeName));

            String programmeId = null;
            String filename = null;

            if (!isProgrammeExists) {
                final ProgrammeDetailsModel newProgramme = newProgrammeSetup(programmeName, innovatorToken, innovatorId, innovatorName, innovatorEmail, programmeConfigs, pluginEnvironment);
                programmeId = newProgramme.getProgrammeId();
                newProgrammes.add(newProgramme);

                final String environment = ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment();
                if (pluginEnvironment.isPresent() && pluginEnvironment.get().equals(PluginEnvironment.FB)) {
                    filename = String.format(configurationFilename, "dev");
                } else {
                    filename = String.format(configurationFilename, environment);
                }

            } else {
                for (GetProgrammesResponseModel retrievedProgramme : retrievedProgrammes) {
                    programmeId = String.valueOf(retrievedProgramme.getId());
                }
            }

            final ObjectMapper mapper = new ObjectMapper();
            final ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());

            try {
                writer.writeValue(new File(String.format("src/test/resources/TestConfiguration/%s", filename)), newProgrammes);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

//      Update programme
            try {
                updateProgrammeConfigs(programmeConfigs, innovatorToken, adminToken, programmeId, innovatorId);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    public static ProgrammeDetailsModel newProgrammeSetup(final String programmeName,
                                                          final String innovatorToken,
                                                          final String innovatorId,
                                                          final String innovatorName,
                                                          final String innovatorEmail,
                                                          final ProgrammeConfigsModel programmeConfigs,
                                                          final Optional<PluginEnvironment> pluginEnvironment) {

        final String programmeId = TestHelper.ensureAsExpected(15,
                        () -> opc.services.innovatornew.InnovatorService.createProgramme(CreateProgrammeModel.InitialProgrammeModel(programmeName, PaymentModel.valueOf(programmeConfigs.getPaymentModel()), programmeConfigs.getJurisdiction()), innovatorToken),
                        SC_OK)
                .jsonPath()
                .get("id");

        final String secretKey =
                TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getProgrammeDetails(innovatorToken, programmeId),
                        SC_OK).jsonPath().get("secretKey");

        final String sharedKey =
                TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getProgrammeDetails(innovatorToken, programmeId),
                        SC_OK).jsonPath().get("sharedKey");

        switch(PaymentModel.valueOf(programmeConfigs.getPaymentModel())) {
            case DEFAULT_QA:
                return setupQAProgrammeProfiles(programmeConfigs,
                        innovatorToken,
                        innovatorId,
                        innovatorEmail,
                        innovatorName,
                        programmeId,
                        programmeName,
                        secretKey,
                        sharedKey);
            case DEFAULT_PAYMENT_RUN:
                return setupPaymentRunProgrammeProfiles(programmeConfigs,
                        innovatorToken,
                        innovatorId,
                        innovatorEmail,
                        innovatorName,
                        programmeId,
                        programmeName,
                        secretKey,
                        sharedKey,
                        pluginEnvironment.orElseThrow().getPluginCode());
            default: throw new IllegalArgumentException("Unknown payment model");
        }
    }

    public static void createBackofficeTestsInnovator(final String innovatorName,
                                                      final String innovatorEmail) throws IOException {
        testDataInit(innovatorName, innovatorEmail, Collections.singletonList("ApplicationOne"), "%s_bo_configuration.json", "bo_configuration.json", Optional.empty());
    }

    public static void createInnovatorTestsInnovator(final String innovatorName,
                                                     final String innovatorEmail) throws IOException {
        testDataInit(innovatorName, innovatorEmail, Arrays.asList("ApplicationOne", "ApplicationTwo"), "%s_innovator_configuration.json", "innovator_configuration.json", Optional.empty());
    }

    public static void createMultiNonFpsTestsInnovator(final String innovatorName,
                                                       final String innovatorEmail) throws IOException {
        testDataInit(innovatorName, innovatorEmail, Collections.singletonList("ApplicationOne"), "%s_multi_nonfps_configuration.json", "multi_nonfps_configuration.json", Optional.empty());
    }

    public static void createMultiTestsInnovator(final String innovatorName,
                                                 final String innovatorEmail) throws IOException {
        testDataInit(innovatorName, innovatorEmail, Arrays.asList("ApplicationOne", "ApplicationTwo", "ApplicationThree",
                "ApplicationFour", "OddApp", "ScaApp", "WebhooksApp", "ScaMaApp", "ScaMcApp", "WebhooksOddApp", "ScaEnrolApp",
                "ThreeDSApp", "ScaSendsApp", "LowValueExemptionApp", "PasscodeApp", "SemiApp", "SecondaryScaApp"), "%s_multi_configuration.json", "multi_configuration.json", Optional.empty());
    }

    public static void createMultiUkTestsInnovator(final String innovatorName,
                                                   final String innovatorEmail) throws IOException {
        testDataInit(innovatorName, innovatorEmail, List.of("ApplicationOneUk"), "%s_multi_uk_configuration.json", "multi_uk_configuration.json", Optional.empty());
    }

    public static void createSemiInnovator(final String innovatorName,
                                           final String innovatorEmail) throws IOException {
        testDataInit(innovatorName, innovatorEmail, Arrays.asList("PasscodeApp", "ScaSendsApp", "ScaPasscodeApp"), "%s_semi_configuration.json", "semi_configuration.json", Optional.empty());
    }

    public static void createFinInstitutionInnovator(final String innovatorName,
                                                     final String innovatorEmail) throws IOException {
        testDataInit(innovatorName, innovatorEmail, Arrays.asList("PayneticsApp", "PayneticsModulrApp", "ModulrApp", "MultipleFisApp"), "%s_fi_configuration.json", "fi_configuration.json", Optional.empty());
    }

    public static void createFeesInnovator(final String innovatorName,
                                           final String innovatorEmail) throws IOException {
        testDataInit(innovatorName, innovatorEmail, List.of("ApplicationOne", "ApplicationTwo"), "%s_fees_configuration.json", "fees_configuration.json", Optional.empty());
    }

    public static void createPluginsInnovator(final String innovatorName,
                                              final String innovatorEmail,
                                              final PluginEnvironment pluginEnvironment) throws IOException {
        testDataInit(innovatorName, innovatorEmail, List.of("PluginsApp", "PluginsAppTwo", "PluginsScaApp", "PluginsScaMaApp", "PluginsWebhooksApp"), "%s_plugins_configuration.json", "plugins_configuration.json", Optional.of(pluginEnvironment));
    }

    public static Response deactivateCorporate(final DeactivateIdentityModel deactivateIdentityModel,
                                               final String corporateId,
                                               final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.deactivateCorporate(deactivateIdentityModel, corporateId, token),
                SC_NO_CONTENT);
    }

    public static Response deactivateConsumer(final DeactivateIdentityModel deactivateIdentityModel,
                                              final String consumerId,
                                              final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.deactivateConsumer(deactivateIdentityModel, consumerId, token),
                SC_NO_CONTENT);
    }

    public static Response deactivateCorporateUser(final String corporateId,
                                                   final String corporateUserId,
                                                   final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.deactivateCorporateUser(corporateId, corporateUserId, token),
                SC_NO_CONTENT);
    }

    public static Response deactivateConsumerUser(final String consumerId,
                                                  final String consumerUserId,
                                                  final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.deactivateConsumerUser(consumerId, consumerUserId, token),
                SC_NO_CONTENT);
    }

    public static Response enableAuthy(final String programmeId,
                                       final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.enableAuthy(programmeId, token),
                SC_OK);
    }

    public static void updateProfileWallets(final String programmeId,
                                            final String profileId,
                                            final FundingType fundingType,
                                            final boolean isManualProvisioningEnabled,
                                            final boolean isPushProvisioningEnabled,
                                            final String token) {
        final UpdateManagedCardsProfileV2Model.Builder updateManagedCardsProfile =
                UpdateManagedCardsProfileV2Model.builder();

        switch (fundingType) {
            case PREPAID:
                updateManagedCardsProfile.setUpdatePrepaidProfileRequest(UpdatePrepaidManagedCardsProfileModel
                        .builder()
                        .setUpdateManagedCardsProfileRequest(AbstractUpdateManagedCardsProfileModel.builder()
                                .setDigitalWalletsEnabled(new DigitalWalletsEnabledModel(isManualProvisioningEnabled, isPushProvisioningEnabled))
                                .build())
                        .build());
                break;
            case DEBIT:
                updateManagedCardsProfile.setUpdateDebitProfileRequest(UpdateDebitManagedCardsProfileModel
                        .builder()
                        .setUpdateManagedCardsProfileRequest(AbstractUpdateManagedCardsProfileModel.builder()
                                .setDigitalWalletsEnabled(new DigitalWalletsEnabledModel(isManualProvisioningEnabled, isPushProvisioningEnabled))
                                .build())
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unknown funding type");
        }

        updateManagedCardsProfile.setCardFundingType(fundingType.name());

        TestHelper.ensureAsExpected(5,
                () -> InnovatorService.updateManagedCardsProfileV2(updateManagedCardsProfile.build(), token, programmeId, profileId),
                SC_OK);
    }

    public static void enableOkay(final String programmeId,
                                  final String token) {
        TestHelper.ensureAsExpected(30,
                () -> InnovatorService.enableOkay(programmeId, token),
                SC_OK);
    }

    public static void disableOkay(final String programmeId,
                                   final String token) {
        TestHelper.ensureAsExpected(30,
                () -> InnovatorService.disableOkay(programmeId, token),
                SC_OK);
    }

    public static Response getProfileSpendRules(final String token,
                                                final String programmeId,
                                                final String profileId) {

        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.getProfileSpendRules(token, programmeId, profileId),
                SC_OK);
    }

    public static void updateCorporateProfile(final UpdateCorporateProfileModel updateCorporateProfileModel,
                                              final String token,
                                              final String programme_id,
                                              final String profile_id) {
        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateCorporateProfile(updateCorporateProfileModel, token, programme_id, profile_id),
                SC_OK);
    }

    public static void updateConsumerProfile(final UpdateConsumerProfileModel updateConsumerProfileModel,
                                             final String token,
                                             final String programme_id,
                                             final String profile_id) {
        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateConsumerProfile(updateConsumerProfileModel, token, programme_id, profile_id),
                SC_OK);
    }

    public static GetCorporateBeneficiariesModel getCorporateBeneficiaries(final String token,
                                                                           final String corporateId) {

        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.getCorporateBeneficiaries(token, corporateId),
                SC_OK).as(GetCorporateBeneficiariesModel.class);
    }

    public static Response getProgrammes(final String token) {

        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.getProgrammes(token), SC_OK);
    }

    public static void enableAndUpdateAuth0(final String token,
                                            final String programmeId) {

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.enableAuth0(token, programmeId),
                SC_OK);

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateAuth0(token, programmeId),
                SC_OK);
    }

    public static String startCorporateUserKyc(final String corporateId, final String corporateUserId, final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.startCorporateUserKyc(corporateId, corporateUserId, token),
                        SC_OK)
                .jsonPath()
                .get("reference");
    }

    public static void updateProfileConstraint(final String authenticationType,
                                               final String programmeId,
                                               final String innovatorToken) {

        final PasswordConstraint passwordConstraint =
                authenticationType.equals("PASSCODE") ? PasswordConstraint.PASSCODE : PasswordConstraint.PASSWORD;

        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.updateProfileConstraint(new PasswordConstraintsModel(passwordConstraint), programmeId, innovatorToken),
                SC_NO_CONTENT);
    }

    public static void updateProgrammeConfigs(final ProgrammeConfigsModel programmeConfigs,
                                              final String innovatorToken,
                                              final String adminToken,
                                              final String programmeId,
                                              final String innovatorId) throws IOException {

//       Get config values
        final Boolean authyConfig = programmeConfigs.getAuthyEnabled();
        final Boolean biometricConfig = programmeConfigs.getBiometricEnabled();
        final Boolean scaMaConfig = programmeConfigs.getScaMaEnabled();
        final Boolean scaMcConfig = programmeConfigs.getScaMcEnabled();
        final Boolean txmConfig = programmeConfigs.getTxmEnabled();
        final Boolean scaEnrolConfig = programmeConfigs.getScaEnrolEnabled();
        final Boolean trustBiometricsConfig = programmeConfigs.getTrustBiometrics();

        final Boolean lowValueExemptionConfig = programmeConfigs.getScaConfig().getEnableLowValueExemption();
        final Boolean scaSendsConfig = programmeConfigs.getScaConfig().getEnableScaSends();
        final Boolean authForwardingConfig = programmeConfigs.getAuthForwardingEnabled();
        final Boolean webhookConfig = programmeConfigs.getWebhookDisabled();

//        Update programme
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        final opc.models.admin.UpdateProgrammeModel updateProgrammeModel =
                opc.models.admin.UpdateProgrammeModel.builder()
                        .setWebhookDisabled(webhookConfig)
                        .setSecurityModelConfig(securityModelConfig)
                        .setAuthForwardingEnabled(authForwardingConfig)
                        .setScaConfig(new ScaConfigModel(scaSendsConfig, lowValueExemptionConfig))
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.updateProgramme(updateProgrammeModel, programmeId, AdminService.loginAdmin()),
                SC_OK);


        AdminHelper.setScaConfig(adminToken, programmeId, SetScaModel.builder()
                .scaMaEnabled(scaMaConfig)
                .scaMcEnabled(scaMcConfig)
                .scaEnrolEnabled(scaEnrolConfig)
                .build());

        AdminHelper.setTransactionMonitoring(AdminService.impersonateTenant(innovatorId, adminToken), programmeId, txmConfig);

        AdminHelper.setTrustBiometrics(adminToken, programmeId, trustBiometricsConfig);

        if (authyConfig) {
            enableAuthy(programmeId, innovatorToken);
        }

        if (biometricConfig) {
            enableOkay(programmeId, innovatorToken);
        }

        updateProfileConstraint(programmeConfigs.getAuthenticationType(), programmeId, innovatorToken);
    }

    private static ProgrammeDetailsModel setupQAProgrammeProfiles(final ProgrammeConfigsModel programmeConfigs,
                                                                  final String innovatorToken,
                                                                  final String innovatorId,
                                                                  final String innovatorEmail,
                                                                  final String innovatorName,
                                                                  final String programmeId,
                                                                  final String programmeName,
                                                                  final String secretKey,
                                                                  final String sharedKey) {

        String threeDSCorporatesProfileId = null;
        String threeDSConsumersProfileId = null;
        final String corporateProfileId;
        final String consumerProfileId;

        if (programmeName.equals("ThreeDSApp")) {
            enableOkay(programmeId, innovatorToken);
            enableAuthy(programmeId, innovatorToken);
            threeDSCorporatesProfileId = InnovatorHelper.createThreeDSCorporateProfileWithPassword(innovatorToken, programmeId);
            threeDSConsumersProfileId = InnovatorHelper.createThreeDSConsumerProfileWithPassword(innovatorToken, programmeId);

            corporateProfileId = InnovatorHelper.createSecondaryThreeDSCorporateProfileWithPassword(innovatorToken, programmeId);
            consumerProfileId = InnovatorHelper.createSecondaryThreeDSConsumerProfileWithPassword(innovatorToken, programmeId);
        } else {
            corporateProfileId = InnovatorHelper.createCorporateProfileWithPassword(innovatorToken, programmeId, programmeConfigs);
            consumerProfileId = InnovatorHelper.createConsumerProfileWithPassword(innovatorToken, programmeId, programmeConfigs);
        }

        final String sendsProfileId = InnovatorHelper.createSendsProfile(innovatorToken, programmeId);
        final String transfersProfileId = InnovatorHelper.createTransfersProfile(innovatorToken, programmeId);
        final String outgoingWireTransfersProfileId = InnovatorHelper.createOwtProfile(innovatorToken, programmeId);

        final String[] corporateNitecrestEeaPrepaidManagedCardsProfileId = {null};
        final String[] corporateNitecrestEeaDebitManagedCardsProfileId = {null};
        final String[] corporateDigiseqEeaPrepaidManagedCardsProfileId = {null};
        final String[] corporateDigiseqEeaDebitManagedCardsProfileId = {null};
        final String[] corporateNitecrestUkPrepaidManagedCardsProfileId = {null};
        final String[] corporateNitecrestUkDebitManagedCardsProfileId = {null};
        final String[] corporateDigiseqUkPrepaidManagedCardsProfileId = {null};
        final String[] corporateDigiseqUkDebitManagedCardsProfileId = {null};
        final String[] consumerNitecrestEeaPrepaidManagedCardsProfileId = {null};
        final String[] consumerNitecrestEeaDebitManagedCardsProfileId = {null};
        final String[] consumerDigiseqEeaPrepaidManagedCardsProfileId = {null};
        final String[] consumerDigiseqEeaDebitManagedCardsProfileId = {null};
        final String[] consumerNitecrestUkPrepaidManagedCardsProfileId = {null};
        final String[] consumerNitecrestUkDebitManagedCardsProfileId = {null};
        final String[] consumerDigiseqUkPrepaidManagedCardsProfileId = {null};
        final String[] consumerDigiseqUkDebitManagedCardsProfileId = {null};
        final String[] corporateModulrManagedAccountsProfileId = {null};
        final String[] corporatePayneticsEeaManagedAccountsProfileId = {null};
        final String[] corporatePayneticsUkManagedAccountsProfileId = {null};
        final String[] corporatePayneticsModulrManagedAccountsProfileId = {null};
        final String[] consumerModulrManagedAccountsProfileId = {null};
        final String[] consumerPayneticsEeaManagedAccountsProfileId = {null};
        final String[] consumerPayneticsUkManagedAccountsProfileId = {null};
        final String[] consumerPayneticsModulrManagedAccountsProfileId = {null};

        //        Get Managed Cards FinInstitution config values
        final List<String> consumerManagedCardsProfiles = programmeConfigs.getConsumerManagedCardProfiles().getProfiles().stream().map(FinInstitutionProfileModel::getProfile).collect(Collectors.toList());
        final List<String> corporateManagedCardsProfiles  = programmeConfigs.getCorporateManagedCardProfiles().getProfiles().stream().map(FinInstitutionProfileModel::getProfile).collect(Collectors.toList());

//        Create Corporate Managed Card profiles
        corporateManagedCardsProfiles.forEach(profile -> {
            switch(profile) {
                case "paynetics_eea_nitecrest_prepaid":
                    corporateNitecrestEeaPrepaidManagedCardsProfileId[0] = InnovatorHelper.createNitecrestPrepaidManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.EEA);
                    break;
                case "paynetics_eea_nitecrest_debit":
                    corporateNitecrestEeaDebitManagedCardsProfileId[0] = InnovatorHelper.createNitecrestDebitManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.EEA);
                    break;
                case "paynetics_eea_digiseq_prepaid":
                    corporateDigiseqEeaPrepaidManagedCardsProfileId[0] = InnovatorHelper.createDigiseqPrepaidManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.EEA);
                    break;
                case "paynetics_eea_digiseq_debit":
                    corporateDigiseqEeaDebitManagedCardsProfileId[0] = InnovatorHelper.createDigiseqDebitManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.EEA);
                    break;
                case "paynetics_uk_nitecrest_prepaid":
                    corporateNitecrestUkPrepaidManagedCardsProfileId[0] = InnovatorHelper.createNitecrestPrepaidManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.UK);
                    break;
                case "paynetics_uk_nitecrest_debit":
                    corporateNitecrestUkDebitManagedCardsProfileId[0] = InnovatorHelper.createNitecrestDebitManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.UK);
                    break;
                case "paynetics_uk_digiseq_prepaid":
                    corporateDigiseqUkPrepaidManagedCardsProfileId[0] = InnovatorHelper.createDigiseqPrepaidManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.UK);
                    break;
                case "paynetics_uk_digiseq_debit":
                    corporateDigiseqUkDebitManagedCardsProfileId[0] = InnovatorHelper.createDigiseqDebitManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.UK);
                    break;
                default: throw new IllegalArgumentException("FiProfile is not supported");
            }
        });

        //        Create Consumer Managed Card profiles
        consumerManagedCardsProfiles.forEach(profile -> {
            switch(profile) {
                case "paynetics_eea_nitecrest_prepaid":
                    consumerNitecrestEeaPrepaidManagedCardsProfileId[0] = InnovatorHelper.createNitecrestPrepaidManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.EEA);
                    break;
                case "paynetics_eea_nitecrest_debit":
                    consumerNitecrestEeaDebitManagedCardsProfileId[0] = InnovatorHelper.createNitecrestDebitManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.EEA);
                    break;
                case "paynetics_eea_digiseq_prepaid":
                    consumerDigiseqEeaPrepaidManagedCardsProfileId[0] = InnovatorHelper.createDigiseqPrepaidManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.EEA);
                    break;
                case "paynetics_eea_digiseq_debit":
                    consumerDigiseqEeaDebitManagedCardsProfileId[0] = InnovatorHelper.createDigiseqDebitManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.EEA);
                    break;
                case "paynetics_uk_nitecrest_prepaid":
                    consumerNitecrestUkPrepaidManagedCardsProfileId[0] = InnovatorHelper.createNitecrestPrepaidManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.UK);
                    break;
                case "paynetics_uk_nitecrest_debit":
                    consumerNitecrestUkDebitManagedCardsProfileId[0] = InnovatorHelper.createNitecrestDebitManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.UK);
                    break;
                case "paynetics_uk_digiseq_prepaid":
                    consumerDigiseqUkPrepaidManagedCardsProfileId[0] = InnovatorHelper.createDigiseqPrepaidManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.UK);
                    break;
                case "paynetics_uk_digiseq_debit":
                    consumerDigiseqUkDebitManagedCardsProfileId[0] = InnovatorHelper.createDigiseqDebitManagedCardsProfileV2(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.UK);
                    break;
                default: throw new IllegalArgumentException("FiProfile is not supported");
            }
        });

//        Get Managed Accounts FinInstitution config values
        final List<String> consumerManagedAccountsProfiles = programmeConfigs.getConsumerManagedAccountProfiles().getProfiles().stream().map(FinInstitutionProfileModel::getProfile).collect(Collectors.toList());
        final List<String> corporateManagedAccountsProfiles  = programmeConfigs.getCorporateManagedAccountProfiles().getProfiles().stream().map(FinInstitutionProfileModel::getProfile).collect(Collectors.toList());

//        Create Corporate Managed Account profiles
        corporateManagedAccountsProfiles.forEach(profile -> {
            switch(profile) {
                case "paynetics_eea":
                    corporatePayneticsEeaManagedAccountsProfileId[0] = InnovatorHelper.createManagedAccountProfile(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.EEA);
                    break;
                case "paynetics_uk":
                    corporatePayneticsUkManagedAccountsProfileId[0] = InnovatorHelper.createManagedAccountProfile(innovatorToken, programmeId, IdentityType.CORPORATE, Jurisdiction.UK);
                    break;
                case "modulr":
                    corporateModulrManagedAccountsProfileId[0] = InnovatorHelper.createModulrManagedAccountProfile(innovatorToken, programmeId, IdentityType.CORPORATE);
                    break;
                case "modulr_paynetics":
                    corporatePayneticsModulrManagedAccountsProfileId[0] = InnovatorHelper.createPayneticsModulrManagedAccountProfile(innovatorToken, programmeId, IdentityType.CORPORATE);
                    break;
                default: throw new IllegalArgumentException("FiProfile is not supported");
            }
        });

//        Create Consumer Managed Account profiles
        consumerManagedAccountsProfiles.forEach(profile -> {
            switch(profile) {
                case "paynetics_eea":
                    consumerPayneticsEeaManagedAccountsProfileId[0] = InnovatorHelper.createManagedAccountProfile(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.EEA);
                    break;
                case "paynetics_uk":
                    consumerPayneticsUkManagedAccountsProfileId[0] = InnovatorHelper.createManagedAccountProfile(innovatorToken, programmeId, IdentityType.CONSUMER, Jurisdiction.UK);
                    break;
                case "modulr":
                    consumerModulrManagedAccountsProfileId[0] = InnovatorHelper.createModulrManagedAccountProfile(innovatorToken, programmeId, IdentityType.CONSUMER);
                    break;
                case "modulr_paynetics":
                    consumerPayneticsModulrManagedAccountsProfileId[0] = InnovatorHelper.createPayneticsModulrManagedAccountProfile(innovatorToken, programmeId, IdentityType.CONSUMER);
                    break;
                default: throw new IllegalArgumentException("FiProfile is not supported");
            }
        });

//        Update Programme

        final ProgrammeDetailsModel programmeDetailsModel = new ProgrammeDetailsModel();

                programmeDetailsModel.setInnovatorId(innovatorId);
                programmeDetailsModel.setInnovatorName(innovatorName);
                programmeDetailsModel.setInnovatorEmail(innovatorEmail);
                programmeDetailsModel.setInnovatorPassword("Pass1234!");
                programmeDetailsModel.setProgrammeName(programmeName);
                programmeDetailsModel.setProgrammeCode(programmeName);
                programmeDetailsModel.setProgrammeId(programmeId);
                programmeDetailsModel.setCorporatesProfileId(corporateProfileId);
                programmeDetailsModel.setConsumersProfileId(consumerProfileId);
                programmeDetailsModel.setThreeDSCorporatesProfileId(threeDSCorporatesProfileId);
                programmeDetailsModel.setThreeDSConsumersProfileId(threeDSConsumersProfileId);
                programmeDetailsModel.setCorporatePayneticsEeaManagedAccountsProfileId(corporatePayneticsEeaManagedAccountsProfileId[0]);
                programmeDetailsModel.setConsumerPayneticsEeaManagedAccountsProfileId(consumerPayneticsEeaManagedAccountsProfileId[0]);
                programmeDetailsModel.setCorporatePayneticsUkManagedAccountsProfileId(corporatePayneticsUkManagedAccountsProfileId[0]);
                programmeDetailsModel.setConsumerPayneticsUkManagedAccountsProfileId(consumerPayneticsUkManagedAccountsProfileId[0]);
                programmeDetailsModel.setCorporateModulrManagedAccountsProfileId(corporateModulrManagedAccountsProfileId[0]);
                programmeDetailsModel.setConsumerModulrManagedAccountsProfileId(consumerModulrManagedAccountsProfileId[0]);
                programmeDetailsModel.setCorporatePayneticsModulrManagedAccountsProfileId(corporatePayneticsModulrManagedAccountsProfileId[0]);
                programmeDetailsModel.setConsumerPayneticsModulrManagedAccountsProfileId(consumerPayneticsModulrManagedAccountsProfileId[0]);
                programmeDetailsModel.setCorporateNitecrestEeaPrepaidManagedCardsProfileId(corporateNitecrestEeaPrepaidManagedCardsProfileId[0]);
                programmeDetailsModel.setCorporateNitecrestEeaDebitManagedCardsProfileId(corporateNitecrestEeaDebitManagedCardsProfileId[0]);
                programmeDetailsModel.setCorporateDigiseqEeaPrepaidManagedCardsProfileId(corporateDigiseqEeaPrepaidManagedCardsProfileId[0]);
                programmeDetailsModel.setCorporateDigiseqEeaDebitManagedCardsProfileId(corporateDigiseqEeaDebitManagedCardsProfileId[0]);
                programmeDetailsModel.setConsumerNitecrestEeaPrepaidManagedCardsProfileId(consumerNitecrestEeaPrepaidManagedCardsProfileId[0]);
                programmeDetailsModel.setConsumerNitecrestEeaDebitManagedCardsProfileId(consumerNitecrestEeaDebitManagedCardsProfileId[0]);
                programmeDetailsModel.setConsumerDigiseqEeaPrepaidManagedCardsProfileId(consumerDigiseqEeaPrepaidManagedCardsProfileId[0]);
                programmeDetailsModel.setConsumerDigiseqEeaDebitManagedCardsProfileId(consumerDigiseqEeaDebitManagedCardsProfileId[0]);
                programmeDetailsModel.setCorporateNitecrestUkPrepaidManagedCardsProfileId(corporateNitecrestUkPrepaidManagedCardsProfileId[0]);
                programmeDetailsModel.setCorporateNitecrestUkDebitManagedCardsProfileId(corporateNitecrestUkDebitManagedCardsProfileId[0]);
                programmeDetailsModel.setCorporateDigiseqUkPrepaidManagedCardsProfileId(corporateDigiseqUkPrepaidManagedCardsProfileId[0]);
                programmeDetailsModel.setCorporateDigiseqUkDebitManagedCardsProfileId(corporateDigiseqUkDebitManagedCardsProfileId[0]);
                programmeDetailsModel.setConsumerNitecrestUkPrepaidManagedCardsProfileId(consumerNitecrestUkPrepaidManagedCardsProfileId[0]);
                programmeDetailsModel.setConsumerNitecrestUkDebitManagedCardsProfileId(consumerNitecrestUkDebitManagedCardsProfileId[0]);
                programmeDetailsModel.setConsumerDigiseqUkPrepaidManagedCardsProfileId(consumerDigiseqUkPrepaidManagedCardsProfileId[0]);
                programmeDetailsModel.setConsumerDigiseqUkDebitManagedCardsProfileId(consumerDigiseqUkDebitManagedCardsProfileId[0]);
                programmeDetailsModel.setSendProfileId(sendsProfileId);
                programmeDetailsModel.setTransfersProfileId(transfersProfileId);
                programmeDetailsModel.setOwtProfileId(outgoingWireTransfersProfileId);
                programmeDetailsModel.setSharedKey(sharedKey);
                programmeDetailsModel.setSecretKey(secretKey);
                programmeDetailsModel.setAuthenticationType(programmeConfigs.getAuthenticationType());
                programmeDetailsModel.setLinkedAccountsProfileId(null);
                programmeDetailsModel.setPaymentOwtProfileId(null);
                programmeDetailsModel.setSweepOwtProfileId(null);

                return programmeDetailsModel;
    }

    private static ProgrammeDetailsModel setupPaymentRunProgrammeProfiles(final ProgrammeConfigsModel programmeConfigs,
                                                                          final String innovatorToken,
                                                                          final String innovatorId,
                                                                          final String innovatorEmail,
                                                                          final String innovatorName,
                                                                          final String programmeId,
                                                                          final String programmeName,
                                                                          final String secretKey,
                                                                          final String sharedKey,
                                                                          final String pluginCode) {

        final PaymentRunProfilesResponse paymentRunProfilesResponse;
        String fpiKey;

        final GetPluginsResponseModel plugins =
                opc.services.innovatornew.InnovatorService.getPlugins(innovatorToken).as(GetPluginsResponseModel.class);

        if (pluginCode.equals(PluginEnvironment.DEV_MULTI.getPluginCode())) {

            final CreatePluginModel createPluginModel =
                    CreatePluginModel.builder()
                            .name("payment-run")
                            .code(pluginCode)
                            .description("Pay suppliers")
                            .status(PluginStatus.ACTIVE)
                            .webhookUrl("https://qa.weavr.io/payment-run/webhook/v1")
                            .modelId("23")
                            .build();

            if (plugins.getPlugins() != null && plugins.getPlugins().stream().anyMatch(x -> x.getCode().equals(pluginCode))) {
                fpiKey = plugins.getPlugins().stream().filter(x -> x.getCode().equals(pluginCode)).findFirst().orElseThrow().getFpiKey();
            } else {
                fpiKey = AdminService.createPlugin(createPluginModel, AdminService.loginAdmin()).jsonPath().getString("fpiKey");
            }

            installPlugin(innovatorToken, programmeId, pluginCode);

            paymentRunProfilesResponse =
                    PaymentRunHelper.createPaymentRunProfiles(innovatorName, innovatorToken, programmeId);

        } else {
            installPlugin(innovatorToken, programmeId, pluginCode);

            fpiKey = plugins.getPlugins().stream().filter(x -> x.getCode().equals(pluginCode)).findFirst().orElseThrow().getFpiKey();

            paymentRunProfilesResponse =
                    PaymentRunHelper.createPaymentRunProfiles(innovatorName, innovatorToken, programmeId);
        }

//        Update Programme
        final ProgrammeDetailsModel programme = new ProgrammeDetailsModel();

        programme.setInnovatorId(innovatorId);
        programme.setInnovatorName(innovatorName);
        programme.setInnovatorEmail(innovatorEmail);
        programme.setInnovatorPassword("Pass1234!");
        programme.setProgrammeName(programmeName);
        programme.setProgrammeCode(programmeName);
        programme.setProgrammeId(programmeId);
        programme.setCorporatesProfileId(paymentRunProfilesResponse.getCorporateProfileId());
        programme.setSharedKey(sharedKey);
        programme.setSecretKey(secretKey);
        programme.setAuthenticationType(programmeConfigs.getAuthenticationType());
        programme.setLinkedAccountsProfileId(paymentRunProfilesResponse.getLinkedAccountProfileId());
        programme.setPaymentOwtProfileId(paymentRunProfilesResponse.getOwtProfileId());
        programme.setSweepOwtProfileId(paymentRunProfilesResponse.getWithdrawProfileId());
        programme.setZeroBalanceManagedAccountsProfileId(paymentRunProfilesResponse.getManagedAccountProfileId());
        programme.setFpiKey(fpiKey);

        return programme;
    }

    public static void installPlugin(final String token,
                                     final String programmeId,
                                     final String pluginCode) {
        TestHelper.ensureAsExpected(15,
                () -> InnovatorService.installPlugin(token, programmeId, pluginCode), SC_OK);
    }

    public static Response activateCorporate(final ActivateIdentityModel activateIdentityModel,
                                               final String corporateId,
                                               final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> InnovatorService.activateCorporate(activateIdentityModel, corporateId, token),
                SC_NO_CONTENT);
    }
}
