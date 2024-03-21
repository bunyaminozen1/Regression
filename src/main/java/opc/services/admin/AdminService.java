package opc.services.admin;

import commons.models.GetUserFilterModel;
import commons.services.BaseService;
import io.restassured.response.Response;
import opc.enums.opc.BlockType;
import opc.enums.opc.ConfigurationName;
import opc.enums.opc.ConfigurationType;
import opc.enums.opc.RetryType;
import opc.models.EmailTemplateContextModel;
import opc.models.admin.ActivateConsumerLevelCheckModel;
import opc.models.admin.AdminSemiToggleModel;
import opc.models.admin.AvailableToSpendManualAdjustModel;
import opc.models.admin.BlockTypeModel;
import opc.models.admin.ConsumeAdminUserInviteModel;
import opc.models.admin.ConsumersLimitModel;
import opc.models.admin.CorporatesLimitModel;
import opc.models.admin.CreateManualTransactionModel;
import opc.models.admin.CreatePluginModel;
import opc.models.admin.CreateThirdPartyProviderCertificateModel;
import opc.models.admin.CreateThirdPartyProviderModel;
import opc.models.admin.DeactivateConsumerLevelCheckModel;
import opc.models.admin.ExpireAuthModel;
import opc.models.admin.GetAuthorisationsModel;
import opc.models.admin.GetCorporateInformationModel;
import opc.models.admin.GetDepositsModel;
import opc.models.admin.GetSettlementsRequestModel;
import opc.models.admin.GetUserChallengesModel;
import opc.models.admin.ImpersonateIdentityModel;
import opc.models.admin.ImpersonateTenantModel;
import opc.models.admin.LimitsApiContextWithCurrencyModel;
import opc.models.admin.LimitsApiLowValueExemptionModel;
import opc.models.admin.LimitsApiModel;
import opc.models.admin.LimitsIdentityModel;
import opc.models.admin.LimitsRemainingDeltasModel;
import opc.models.admin.ModulrRegistrationModel;
import opc.models.admin.OwtRetryRequest;
import opc.models.admin.RegisteredCountriesSetCountriesModel;
import opc.models.admin.RemoveDuplicateIdentityFlagModel;
import opc.models.admin.ResumeDepositsModel;
import opc.models.admin.ResumeSendsModel;
import opc.models.admin.ResumeSettlementsModel;
import opc.models.admin.ResumeTransactionModel;
import opc.models.admin.RetryAuthorisationModel;
import opc.models.admin.RetryAuthorisationsModel;
import opc.models.admin.RetryTransfersModel;
import opc.models.admin.SendAdminUserInviteModel;
import opc.models.admin.SetApplicantLevelModel;
import opc.models.admin.SetConfigModel;
import opc.models.admin.SetGlobalLimitModel;
import opc.models.admin.SetLimitModel;
import opc.models.admin.SetScaModel;
import opc.models.admin.SettlementRetryModel;
import opc.models.admin.SpendRulesModel;
import opc.models.admin.SwitchModulrSubscriptionFeatureModel;
import opc.models.admin.UpdateConsumerProfileModel;
import opc.models.admin.UpdateCorporateProfileModel;
import opc.models.admin.UpdateCorporateUserModel;
import opc.models.admin.UpdateEmailValidationProviderModel;
import opc.models.admin.UpdateInnovatorUserModel;
import opc.models.admin.UpdateKybModel;
import opc.models.admin.UpdateKycModel;
import opc.models.admin.UpdateModulrCorporateModel;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.admin.UpdateTransfersProfileModel;
import opc.models.admin.UserId;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.ConsumersFilterModel;
import opc.models.innovator.ContextDimensionKeyValueModel;
import opc.models.innovator.ContextDimensionPartModel;
import opc.models.innovator.ContextDimensionValueModel;
import opc.models.innovator.ContextDimensionsModel;
import opc.models.innovator.ContextModel;
import opc.models.innovator.ContextSetModel;
import opc.models.innovator.ContextValueModel;
import opc.models.innovator.CorporatesFilterModel;
import opc.models.innovator.CreateManagedCardsProfileV2Model;
import opc.models.innovator.CreatePasswordProfileModel;
import opc.models.innovator.CreateProgrammeModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.innovator.EmailTemplateModel;
import opc.models.innovator.GetReportsModel;
import opc.models.innovator.PasswordConstraintsModel;
import opc.models.innovator.UpdateConsumerModel;
import opc.models.innovator.UpdateCorporateModel;
import opc.models.innovator.UpdateManagedCardsProfileV2Model;
import opc.models.shared.GetInnovatorUserModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SwitchFunctionModel;
import opc.models.shared.UpdateSpendRulesModel;
import opc.models.sumsub.questionnaire.SumSubQuestionnaireModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminService extends BaseService {

    public static Response setManagedCardsCarrierType(final String tenantId,
                                                      final String token) {
        final ContextModel contextModel =
                ContextModel.builder()
                        .setContext(new ContextDimensionsModel(Arrays.asList(
                                new ContextDimensionKeyValueModel("TenantIdDimension", tenantId, false),
                                new ContextDimensionKeyValueModel("ManagedCardPhysicalBureau", "DIGISEQ", false))))
                        .setAdded(new ContextDimensionValueModel(Collections.singletonList(
                                new ContextDimensionPartModel(Collections.singletonList("DGSQ_CT_01")))))
                        .build();

        return getBodyAuthenticatedRequest(contextModel, token)
                .when()
                .post("/admin/api/configs/managed_cards/sets/MANAGED_CARD_PHYSICAL_CARRIER_TYPE/update");
    }

    public static Response enableAuthForwarding(final boolean enable,
                                                final String tenantId,
                                                final String token) {
        final ContextModel contextModel =
                ContextModel.builder()
                        .setContext(new ContextDimensionsModel(Collections.singletonList(
                                new ContextDimensionKeyValueModel("TenantIdDimension", tenantId, false))))
                        .setValue(
                                new ContextDimensionPartModel(Collections.singletonList(Boolean.toString(enable))))
                        .build();

        return getBodyAuthenticatedRequest(contextModel, token)
                .when()
                .post("/admin/api/configs/managed_cards/values/MANAGED_CARD_AUTH_FORWARDING_ENABLED/set");
    }

    public static Response setManagedCardsProductReference(final String tenantId,
                                                           final String token) {
        final ContextModel contextModel =
                ContextModel.builder()
                        .setContext(new ContextDimensionsModel(Arrays.asList(
                                new ContextDimensionKeyValueModel("TenantIdDimension", tenantId, false),
                                new ContextDimensionKeyValueModel("ManagedCardPhysicalBureau", "DIGISEQ", false))))
                        .setAdded(new ContextDimensionValueModel(Collections.singletonList(
                                new ContextDimensionPartModel(Collections.singletonList("DGSQ_PR_01")))))
                        .build();

        return getBodyAuthenticatedRequest(contextModel, token)
                .when()
                .post(
                        "/admin/api/configs/managed_cards/sets/MANAGED_CARD_PHYSICAL_PRODUCT_REFERENCE/update");
    }

    public static Response setManagedCardsProductReference(final ContextModel contextModel,
                                                           final String token) {
        return getBodyAuthenticatedRequest(contextModel, token)
                .when()
                .post(
                        "/admin/api/configs/managed_cards/sets/MANAGED_CARD_PHYSICAL_PRODUCT_REFERENCE/set");
    }

    public static Response setManagedCardsProductReferenceNameOnCardMaxChars(final String tenantId,
                                                                             final String physicalProductReference,
                                                                             final Integer maxChars,
                                                                             final String token) {
        final ContextModel contextModel =
                ContextModel.builder()
                        .setContext(new ContextDimensionsModel(Arrays.asList(
                                new ContextDimensionKeyValueModel("TenantIdDimension", tenantId, false),
                                new ContextDimensionKeyValueModel("PhysicalProductReference", physicalProductReference, false))))
                        .setValue(new ContextDimensionPartModel(Collections.singletonList(maxChars.toString())))
                        .build();

        return getBodyAuthenticatedRequest(contextModel, token)
                .when()
                .post("/admin/api/configs/managed_cards/values/PHYSICAL_PRODUCT_REFERENCE_NAME_ON_CARD_MAX_CHARS/set");
    }

    public static Response setManagedCardsProductReferenceNameOnCardMaxChars(final Integer maxChars,
                                                                             final String token) {
        final ContextModel contextModel =
                ContextModel.builder()
                        .setContext(new ContextDimensionsModel(List.of()))
                        .setValue(new ContextDimensionPartModel(Collections.singletonList(maxChars.toString())))
                        .build();

        return getBodyAuthenticatedRequest(contextModel, token)
                .when()
                .post("/admin/api/configs/managed_cards/values/PHYSICAL_PRODUCT_REFERENCE_NAME_ON_CARD_MAX_CHARS/set");
    }

    public static Response updateManagedCardsProfileV2(final UpdateManagedCardsProfileV2Model updateManagedCardsProfileV2Model,
                                                       final String token,
                                                       final String programmeId,
                                                       final String profileId) {
        return getBodyAuthenticatedRequest(updateManagedCardsProfileV2Model, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post("/admin/api/managed_cards_v2/programmes/{programme_id}/profiles/{profile_id}/update");
    }

    public static Response setSumSubQuestionnaire(final SumSubQuestionnaireModel sumSubQuestionnaireModel,
                                                  final String token) {
        return getBodyAuthenticatedRequest(sumSubQuestionnaireModel, token)
                .when()
                .body(sumSubQuestionnaireModel)
                .post("admin/api/configs/sumsub/values/QUESTIONNAIRE/set");
    }

    public static Response removeSumSubQuestionnaire(final SumSubQuestionnaireModel sumSubQuestionnaireModel,
                                                     final String token) {
        return getBodyAuthenticatedRequest(sumSubQuestionnaireModel, token)
                .when()
                .body(sumSubQuestionnaireModel)
                .post("admin/api/configs/sumsub/values/QUESTIONNAIRE/remove");
    }

    public static Response settlementRetry(final SettlementRetryModel settlementRetryModel,
                                           final String token,
                                           final String settlementId) {
        return getBodyAuthenticatedRequest(settlementRetryModel, token)
                .pathParam("settlement_id", settlementId)
                .when()
                .post("/admin/api/managed_cards_v2/settlements/{settlement_id}/retry");
    }

    public static Response getSettlements(final GetSettlementsRequestModel getSettlementsRequestModel,
                                          final String token) {
        return getBodyAuthenticatedRequest(getSettlementsRequestModel, token)
                .when()
                .post("/admin/api/managed_cards_v2/settlements/get");
    }

    public static Response updateProgramme(final UpdateProgrammeModel updateProgrammeModel,
                                           final String programmeId, final String token) {
        return getBodyAuthenticatedRequest(updateProgrammeModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("admin/api/programmes/{programme_id}/update");
    }

    public static Response exportProgramme(final String programmeId,
                                           final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("admin/api/programmes/{programme_id}/export");
    }

    public static Response getSpendRule(final String managedCardId,
                                        final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post("admin/api/managed_cards_v2/{id}/spend_rule/get");
    }

    public static String loginAdmin() {
        return getBodyRequest(new LoginModel("admin@weavr.io", new PasswordModel("!Password123!10")))
                .when()
                .post("/admin/api/gateway/login")
                .jsonPath()
                .get("token");
    }

    public static String loginNonRootAdmin(final String email,
                                           final String password) {
        return getBodyRequest(new LoginModel(email, new PasswordModel(password)))
                .when()
                .post("/admin/api/gateway/login")
                .jsonPath()
                .get("token");
    }

    public static Response updateTransfersProfile(final UpdateTransfersProfileModel updateTransfersProfileModel,
                                                  final String programmeId,
                                                  final String transfersProfileId,
                                                  final String token) {
        return getBodyAuthenticatedRequest(updateTransfersProfileModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("transfers_profile_id", transfersProfileId)
                .when()
                .post("/admin/api/transfers_v2/programmes/{programme_id}/profiles/{transfers_profile_id}/update");
    }

    public static Response setProfileSpendRules(final SpendRulesModel spendRulesModel,
                                                final String token,
                                                final String programmeId,
                                                final String profileId) {
        return getBodyAuthenticatedRequest(spendRulesModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post("/admin/api/managed_cards_v2/programmes/{programme_id}/profiles/{profile_id}/spend_rule/set");
    }

    public static Response setCardSpendRules(final SpendRulesModel spendRulesModel,
                                             final String token,
                                             final String managedCardId) {
        return getBodyAuthenticatedRequest(spendRulesModel, token)
                .pathParam("id", managedCardId)
                .when()
                .post("/admin/api/managed_cards_v2/{id}/spend_rule/set");
    }

    public static Response getCardSpendRules(final String token,
                                             final String managedCardId) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post("/admin/api/managed_cards_v2/{id}/spend_rule/get");
    }

    public static Response setConsumerLimit(final SetLimitModel setLimitModel,
                                            final String token,
                                            final String consumerId) {
        return getBodyAuthenticatedRequest(setLimitModel, token)
                .pathParam("id", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{id}/limits/set");
    }

    public static Response setCorporateLimit(final SetLimitModel setLimitModel,
                                             final String token,
                                             final String corporateId) {
        return getBodyAuthenticatedRequest(setLimitModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/admin/api/corporates/{id}/limits/set");
    }

    public static Response setGlobalConsumerLimit(final SetGlobalLimitModel setLimitModel,
                                                  final String token,
                                                  final String limitType) {
        return getBodyAuthenticatedRequest(setLimitModel, token)
                .pathParam("limitType", limitType)
                .when()
                .post("/admin/api/limits/consumers/{limitType}/create");
    }

    public static Response setGlobalCorporateLimit(final SetGlobalLimitModel setLimitModel,
                                                   final String token,
                                                   final String limitType) {
        return getBodyAuthenticatedRequest(setLimitModel, token)
                .pathParam("limitType", limitType)
                .when()
                .post("/admin/api/limits/corporates/{limitType}/create");
    }

    public static String impersonateTenant(final String tenantId,
                                           final String token) {
        return getBodyAuthenticatedRequest(new ImpersonateTenantModel(tenantId), token)
                .when()
                .post("/admin/api/gateway/switch_tenant")
                .jsonPath()
                .get("token");
    }

    public static Response updateCorporateUser(final UpdateCorporateUserModel updateCorporateUserModel,
                                               final String token, final String corporateId,
                                               final String userId) {
        return getBodyAuthenticatedRequest(updateCorporateUserModel, token)
                .pathParam("id", corporateId)
                .pathParam("user_id", userId)
                .when()
                .post("/admin/api/corporates/{id}/users/{user_id}/update");
    }

    public static Response updateCorporateProfile(final UpdateCorporateProfileModel updateCorporateProfileModel,
                                                  final String token,
                                                  final String programme_id,
                                                  final String profile_id) {
        return getBodyAuthenticatedRequest(updateCorporateProfileModel, token)
                .pathParam("programme_id", programme_id)
                .pathParam("profile_id", profile_id)
                .when()
                .post("/admin/api/corporates/programmes/{programme_id}/profiles/{profile_id}/update");
    }

    public static Response unblockPhysicalCardPin(final String managedCardId,
                                                  final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post(
                        "/admin/api/managed_cards_v2/{id}/physical/pin/unblock");
    }

    public static Response setPayneticsVersion(final String tenantId,
                                               final String token) {
        final ContextModel contextModel = ContextModel.builder().setContext(new ContextDimensionsModel(
                Collections.singletonList(
                        new ContextDimensionKeyValueModel("TenantIdDimension", tenantId, false)))).setAdded(
                new ContextDimensionValueModel(Collections.singletonList(
                        new ContextDimensionPartModel(Collections.singletonList("V2"))))).build();

        return getBodyAuthenticatedRequest(contextModel, token)
                .when()
                .post("/admin/api/configs/paynetics/values/PAYNETICS_API_VERSION/set");
    }

    public static Response setCorporateProfileTemplate(final EmailTemplateModel emailTemplateModel,
                                                       final String programmeId,
                                                       final String profileId,
                                                       final String token) {

        return getBodyAuthenticatedRequest(emailTemplateModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post("/admin/api/corporates/programmes/{programme_id}/profiles/{profile_id}/email_templates/set");
    }

    public static Response getCorporateProfileTemplate(final EmailTemplateContextModel emailTemplateContextModel,
                                                       final String programmeId,
                                                       final String profileId,
                                                       final String token) {

        return getBodyAuthenticatedRequest(emailTemplateContextModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post("/admin/api/corporates/programmes/{programme_id}/profiles/{profile_id}/email_templates/get");
    }

    public static Response setConsumerProfileTemplate(final EmailTemplateModel emailTemplateModel,
                                                      final String programmeId,
                                                      final String profileId,
                                                      final String token) {

        return getBodyAuthenticatedRequest(emailTemplateModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post("/admin/api/consumers_v2/programmes/{programme_id}/profiles/{profile_id}/email_templates/set");
    }

    public static Response getConsumerProfileTemplate(final EmailTemplateContextModel emailTemplateContextModel,
                                                      final String programmeId,
                                                      final String profileId,
                                                      final String token) {
        return getBodyAuthenticatedRequest(emailTemplateContextModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post("/admin/api/consumers_v2/programmes/{programme_id}/profiles/{profile_id}/email_templates/get");
    }

    public static Response getManagedAccountStatement(final String managedAccountId,
                                                      final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedAccountId)
                .when()
                .post("/admin/api/managed_accounts_v2/{id}/statement/get");
    }

    public static Response getManagedCardStatement(final String managedCardId,
                                                   final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post("/admin/api/managed_cards_v2/{id}/statement/get");
    }

    public static Response expireAuthorisations(final ExpireAuthModel expireAuthModel,
                                                final String token) {
        return getBodyAuthenticatedRequest(expireAuthModel, token)
                .when()
                .post("/admin/api/managed_cards_v2/authorisations/expiry/manual_process");
    }

    public static Response getAuthorisationById(final String authorisationId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("authorisationId", authorisationId)
                .when()
                .post("/admin/api/managed_cards_v2/authorisations/{authorisationId}/get");
    }

    public static Response getAuthorisations(final GetAuthorisationsModel getAuthorisationsModel,
                                             final String token) {
        return getBodyAuthenticatedRequest(getAuthorisationsModel, token)
                .when()
                .post("/admin/api/managed_cards_v2/authorisations/get");
    }

    public static Response retryAuthorisation(final RetryAuthorisationModel retryAuthorisationModel, final String authorisationId, final String token) {
        return getBodyAuthenticatedRequest(retryAuthorisationModel, token)
                .pathParam("authorisationId", authorisationId)
                .when()
                .post("/admin/api/managed_cards_v2/authorisations/{authorisationId}/retry");
    }

    public static Response retryAuthorisations(final RetryAuthorisationsModel retryAuthorisationsModel, final String token) {
        return getBodyAuthenticatedRequest(retryAuthorisationsModel, token)
                .when()
                .post("/admin/api/managed_cards_v2/authorisations/retry");
    }

    public static String impersonateIdentity(final ImpersonateIdentityModel impersonateIdentityModel,
                                             final String token) {
        return getBodyAuthenticatedRequest(impersonateIdentityModel, token)
                .when()
                .post("/admin/api/auth_sessions/_/impersonation")
                .jsonPath()
                .get("token");
    }

    public static Response resetCorporateLimit(final String token,
                                               final String corporateId) {
        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .when()
                .post("/admin/api/corporates/{id}/limits/reset");
    }

    public static Response resetConsumerLimit(final String token,
                                              final String consumerId) {
        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{id}/limits/reset");
    }

    public static Response resumeDeposit(final String token,
                                         final ResumeTransactionModel resumeTransactionModel,
                                         final String depositId) {
        return getBodyAuthenticatedRequest(resumeTransactionModel, token)
                .pathParam("id", depositId)
                .when()
                .post("/admin/api/managed_accounts_v2/deposits/{id}/resume");
    }

    public static Response resumeDeposits(final String token,
                                          final ResumeDepositsModel resumeDepositsModel) {
        return getBodyAuthenticatedRequest(resumeDepositsModel, token)
                .when()
                .post("/admin/api/managed_accounts_v2/deposits/resume");
    }

    public static Response retryDeposit(final String token,
                                        final ResumeTransactionModel resumeTransactionModel,
                                        final String depositId) {
        return getBodyAuthenticatedRequest(resumeTransactionModel, token)
                .pathParam("id", depositId)
                .when()
                .post("/admin/api/managed_accounts_v2/deposits/{id}/retry");
    }

    public static Response getDeposit(final String token,
                                      final String depositId) {
        return getAuthenticatedRequest(token)
                .pathParam("depositId", depositId)
                .when()
                .post("/admin/api/managed_accounts_v2/deposits/{depositId}/get");
    }

    public static Response getDeposits(final String token,
                                       final GetDepositsModel depositsModel) {
        return getBodyAuthenticatedRequest(depositsModel, token)
                .when()
                .post("/admin/api/managed_accounts_v2/deposits/get");
    }

    public static Response resumeSend(final String token,
                                      final ResumeTransactionModel resumeTransactionModel,
                                      final String sendId) {
        return getBodyAuthenticatedRequest(resumeTransactionModel, token)
                .pathParam("id", sendId)
                .when()
                .post("/admin/api/send_v2/{id}/resume");
    }

    public static Response resumeSends(final String token,
                                       final ResumeSendsModel resumeSendsModel) {
        return getBodyAuthenticatedRequest(resumeSendsModel, token)
                .when()
                .post("/admin/api/send_v2/resume");
    }

    public static Response getSend(final String token,
                                   final String sendId) {
        return getAuthenticatedRequest(token)
                .pathParam("id", sendId)
                .when()
                .post("/admin/api/send_v2/{id}/get");
    }

    public static Response resumeOct(final String token,
                                     final ResumeTransactionModel resumeTransactionModel,
                                     final String octId) {
        return getBodyAuthenticatedRequest(resumeTransactionModel, token)
                .pathParam("id", octId)
                .when()
                .post("/admin/api/managed_cards_v2/settlements/{id}/resume");
    }

    public static Response resumeOcts(final String token,
                                      final ResumeSettlementsModel resumeSettlementsModel) {
        return getBodyAuthenticatedRequest(resumeSettlementsModel, token)
                .when()
                .post("/admin/api/managed_cards_v2/settlements/resume");
    }

    public static Response setConsumerLimits(final String token,
                                             final LimitsApiLowValueExemptionModel limitsApiModel) {
        return getBodyAuthenticatedRequest(limitsApiModel, token)
                .when()
                .post("/admin/api/consumers_v2/low_value_exemption/set");
    }

    public static Response setCorporateLimits(final String token,
                                              final LimitsApiLowValueExemptionModel limitsApiModel) {
        return getBodyAuthenticatedRequest(limitsApiModel, token)
                .when()
                .post("/admin/api/corporates/low_value_exemption/set");
    }

    public static Response getConsumerLimits(final String token,
                                             final LimitsApiContextWithCurrencyModel limitsApiContextModel) {
        return getBodyAuthenticatedRequest(limitsApiContextModel, token)
                .when()
                .post("/admin/api/consumers_v2/low_value_exemption/get");
    }

    public static Response getCorporateLimits(final String token,
                                              final LimitsApiContextWithCurrencyModel limitsApiContextModel) {
        return getBodyAuthenticatedRequest(limitsApiContextModel, token)
                .when()
                .post("/admin/api/corporates/low_value_exemption/set");
    }

    public static Response setLimits(final String token,
                                     final LimitsApiModel limitsApiModel,
                                     final String limitKey) {
        return getBodyAuthenticatedRequest(limitsApiModel, token)
                .pathParam("limit_key", limitKey)
                .when()
                .post("/admin/api/limitsapi/{limit_key}/set");
    }

    public static Response getLimitRemainingDeltas(final String token,
                                                   final LimitsRemainingDeltasModel limitsRemainingDeltasModel,
                                                   final String limitKey) {
        return getBodyAuthenticatedRequest(limitsRemainingDeltasModel, token)
                .pathParam("limit_key", limitKey)
                .when()
                .post("/admin/api/limitsapi/{limit_key}/get_remaining_deltas");
    }

    public static Response resetLimitsCounter(final String token,
                                              final LimitsIdentityModel limitsIdentityModel,
                                              final String limitKey) {
        return getBodyAuthenticatedRequest(limitsIdentityModel, token)
                .pathParam("limit_key", limitKey)
                .when()
                .post("/admin/api/limitsapi/{limit_key}/reset_counter");
    }

    public static Response manuallyAdjustAvailableToSpend(final String token,
                                                          final AvailableToSpendManualAdjustModel availableToSpendManualAdjustModel,
                                                          final String managedCardId) {
        return getBodyAuthenticatedRequest(availableToSpendManualAdjustModel, token)
                .pathParam("managed_card_id", managedCardId)
                .when()
                .post("/admin/api/managed_cards_v2/{managed_card_id}/available_to_spend/manual_adjust");
    }

    public static Response createManagedCardsProfileV2(final CreateManagedCardsProfileV2Model createManagedCardProfileModel,
                                                       final String token,
                                                       final String programmeId) {
        return getBodyAuthenticatedRequest(createManagedCardProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/managed_cards_v2/programmes/{programme_id}/profiles/_/create");
    }

    public static Response setContextPropertyValue(final ContextValueModel contextModel,
                                                   final ConfigurationName configurationName,
                                                   final ConfigurationType configurationType,
                                                   final String token) {


        return getBodyAuthenticatedRequest(contextModel, token)
                .pathParam("configuration_name", configurationName.name().toLowerCase())
                .pathParam("configuration_type", configurationType.name())
                .when()
                .post("/admin/api/configs/{configuration_name}/values/{configuration_type}/set");
    }

    public static Response setContextPropertySet(final ContextSetModel contextModel,
                                                 final ConfigurationName configurationName,
                                                 final ConfigurationType configurationType,
                                                 final String token) {


        return getBodyAuthenticatedRequest(contextModel, token)
                .pathParam("configuration_name", configurationName.name().toLowerCase())
                .pathParam("configuration_type", configurationType.name())
                .when()
                .post("/admin/api/configs/{configuration_name}/sets/{configuration_type}/set");
    }

    public static Response updateContextPropertyValue(final ContextValueModel contextModel,
                                                      final ConfigurationName configurationName,
                                                      final ConfigurationType configurationType,
                                                      final String token) {


        return getBodyAuthenticatedRequest(contextModel, token)
                .pathParam("configuration_name", configurationName)
                .pathParam("configuration_type", configurationType)
                .when()
                .post("/admin/api/configs/{configuration_name}/values/{configuration_type}/update");
    }

    public static Response removeContextPropertyValue(final ContextDimensionsModel contextModel,
                                                      final ConfigurationName configurationName,
                                                      final ConfigurationType configurationType,
                                                      final String token) {


        return getBodyAuthenticatedRequest(contextModel, token)
                .pathParam("configuration_name", configurationName.name().toLowerCase())
                .pathParam("configuration_type", configurationType.name())
                .when()
                .post("/admin/api/configs/{configuration_name}/sets/{configuration_type}/remove");
    }

    public static Response blockManagedAccount(final String managedAccountId,
                                               final BlockType blockType,
                                               final String token) {
        return getBodyAuthenticatedRequest(new BlockTypeModel(blockType), token)
                .pathParam("id", managedAccountId)
                .when()
                .post("/admin/api/managed_accounts_v2/{id}/block");
    }

    public static Response upgradeManagedAccount(final String managedAccountId,
                                                 final String token) {
        return getAuthenticatedRequest(token)
            .pathParam("id", managedAccountId)
            .when()
            .post("/admin/api/managed_accounts_v2/{id}/upgrade");
    }

    public static Response downloadKybScreeningReport(final String corporateId,
                                                      final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("corporate_id", corporateId)
                .when()
                .post("/admin/api/corporates/{corporate_id}/kyb/screening_report");
    }

    public static Response downloadKycScreeningReport(final String consumerId,
                                                      final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("consumer_id", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{consumer_id}/kyc/screening_report");
    }

    public static Response blockManagedCard(final String managedCardId,
                                            final BlockType blockType,
                                            final String token) {
        return getBodyAuthenticatedRequest(new BlockTypeModel(blockType), token)
                .pathParam("id", managedCardId)
                .when()
                .post("/admin/api/managed_cards_v2/{id}/block");
    }

    public static Response updateCorporateKyb(final UpdateKybModel updateKybModel,
                                              final String corporateId,
                                              final String token) {

        return getBodyAuthenticatedRequest(updateKybModel, token)
                .pathParam("corporateId", corporateId)
                .when()
                .post("/admin/api/corporates/{corporateId}/kyb/update");
    }

    public static Response updateConsumerKyc(final UpdateKycModel updateKycModel,
                                             final String consumerId,
                                             final String token) {

        return getBodyAuthenticatedRequest(updateKycModel, token)
                .pathParam("consumerId", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{consumerId}/kyc/update");
    }

    public static Response retrySend(final String token,
                                     final ResumeTransactionModel resumeTransactionModel,
                                     final String sendId) {
        return getBodyAuthenticatedRequest(resumeTransactionModel, token)
                .pathParam("id", sendId)
                .when()
                .post("/admin/api/send_v2/{id}/retry");
    }

    public static Response retrySends(final String token,
                                      final ResumeSendsModel resumeSendsModel) {
        return getBodyAuthenticatedRequest(resumeSendsModel, token)
                .when()
                .post("/admin/api/send_v2/retry");
    }

    public static Response createManualTransaction(final String token,
                                                   final CreateManualTransactionModel createManualTransactionModel) {
        return getBodyAuthenticatedRequest(createManualTransactionModel, token)
                .when()
                .post("/admin/api/manual_transactions/_/create");
    }

    public static Response setSca(final String token,
                                  final String programmeId,
                                  final boolean scaMaEnabled,
                                  final boolean scaMcEnabled) {
        return getBodyAuthenticatedRequest(SetScaModel.builder()
                        .scaMaEnabled(scaMaEnabled)
                        .scaMcEnabled(scaMcEnabled)
                        .build()
                , token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/programmes/{programme_id}/sca_pai/set");
    }

    public static Response setEnrolmentSca(final String token,
                                           final String programmeId,
                                           final boolean scaMaEnabled,
                                           final boolean scaMcEnabled,
                                           final boolean scaEnrolEnabled) {
        return getBodyAuthenticatedRequest(SetScaModel.builder()
                        .scaMaEnabled(scaMaEnabled)
                        .scaMcEnabled(scaMcEnabled)
                        .scaEnrolEnabled(scaEnrolEnabled)
                        .build()
                , token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/programmes/{programme_id}/sca_pai/set");
    }

    public static Response setScaConfig(final String token,
                                        final String programmeId,
                                        final SetScaModel setScaModel) {
        return getBodyAuthenticatedRequest(setScaModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/programmes/{programme_id}/sca_pai/set");
    }

    public static Response getSca(final String token,
                                  final String programmeId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/programmes/{programme_id}/sca_pai/get");
    }

    public static Response getConsumerKyc(final String consumerId,
                                          final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{id}/kyc/get");
    }

    public static Response getCorporateKyb(final String corporateId,
                                           final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .when()
                .post("/admin/api/corporates/{id}/kyb/get");
    }

    public static Response getManagedCard(final String managedCardId,
                                          final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post("/admin/api/managed_cards_v2/{id}/get");
    }

    public static Response getManagedAccount(final String managedAccountId,
                                             final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedAccountId)
                .when()
                .post("/admin/api/managed_accounts_v2/{id}/get");
    }

    public static Response addCardSpendRules(final SpendRulesModel spendRulesModel,
                                             final String managedCardId,
                                             final String token) {
        return getBodyAuthenticatedRequest(spendRulesModel, token)
                .pathParam("id", managedCardId)
                .when()
                .post("/admin/api/managed_cards_v2/{id}/spend_rule/add");
    }

    public static Response updateCardSpendRules(final UpdateSpendRulesModel spendRulesModel,
                                                final String managedCardId,
                                                final String token) {
        return getBodyAuthenticatedRequest(spendRulesModel, token)
                .pathParam("id", managedCardId)
                .when()
                .post("/admin/api/managed_cards_v2/{id}/spend_rule/update");
    }

    public static Response deleteCardSpendRules(final String managedCardId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post("/admin/api/managed_cards_v2/{id}/spend_rule/delete");
    }

    public static Response retryOwtSubmission(final String token,
                                              final RetryType retryType,
                                              final String owtId) {
        return getBodyAuthenticatedRequest(new OwtRetryRequest(retryType), token)
                .pathParam("submission_id", owtId)
                .when()
                .post("/admin/api/outgoing_wire_transfers/submissions/{submission_id}/retry");
    }

    public static Response getOwtSubmission(final String token,
                                            final String owtId) {
        return getAuthenticatedRequest(token)
                .pathParam("submission_id", owtId)
                .when()
                .post("/admin/api/outgoing_wire_transfers/submissions/{submission_id}/get");
    }

    public static Response getOwtSubmissions(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .post("/admin/api/outgoing_wire_transfers/submissions/get");
    }

    public static Response getCorporateUser(final String token,
                                            final String corporateId,
                                            final String userId) {

        return getAuthenticatedRequest(token)
                .pathParam("corporateId", corporateId)
                .pathParam("userId", userId)
                .when()
                .post("/admin/api/corporates/{corporateId}/users/{userId}/get");
    }

    public static Response getConsumerUser(final String token,
                                           final String consumerId,
                                           final String userId) {

        return getAuthenticatedRequest(token)
                .pathParam("consumerId", consumerId)
                .pathParam("userId", userId)
                .when()
                .post("/admin/api/consumers_v2/{consumerId}/users/{userId}/get");
    }

    public static Response getConsumer(final String token,
                                       final String consumerId) {

        return getAuthenticatedRequest(token)
                .pathParam("consumerId", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{consumerId}/get");
    }

    public static Response getConsumerUserWithoutApiKey(final String token,
                                                        final String consumerId,
                                                        final String userId) {
        return getAuthenticatedRequest(token)
                .pathParam("consumerId", consumerId)
                .pathParam("userId", userId)
                .when()
                .post("/admin/api/consumers_v2/{consumerId}/users/{userId}/get");
    }

    public static Response getCorporate(final String token,
                                        final String corporateId) {

        return getAuthenticatedRequest(token)
                .pathParam("corporateId", corporateId)
                .when()
                .post("/admin/api/corporates/{corporateId}/get");
    }

    public static Response getCorporatesInformation(
            final GetCorporateInformationModel getCorporateInformationModel,
            final String token) {
        return getBodyAuthenticatedRequest(getCorporateInformationModel, token)
                .when()
                .post("/admin/api/corporates/get");
    }

    public static Response matchToRootUser(final String token, final String corporateId,
                                           final String beneficiaryId) {
        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("beneficiaryId", beneficiaryId)
                .when()
                .post("admin/api/corporates/{id}/beneficiaries/{beneficiaryId}/match_to_root_user");
    }

    public static Response createThirdPartyProvider(final String token,
                                                    final CreateThirdPartyProviderModel createThirdPartyProviderModel) {
        return getAuthenticatedRequest(token)
                .body(createThirdPartyProviderModel)
                .when()
                .post("/admin/openbanking/third_party_providers");
    }

    public static Response createThirdPartyProviderCertificate(final String token,
                                                               final CreateThirdPartyProviderCertificateModel createThirdPartyProviderCertificateModel,
                                                               final String tppId) {
        return getAuthenticatedRequest(token)
                .pathParam("tpp_id", tppId)
                .body(createThirdPartyProviderCertificateModel)
                .when()
                .post("/admin/openbanking/third_party_providers/{tpp_id}/certificates");
    }

    public static Response getModulrCorporate(final String corporateId,
                                              final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("corporate_id", corporateId)
                .when()
                .post("/admin/api/modulr/corporates/{corporate_id}/get");
    }

    public static Response updateModulrCorporate(final String corporateId,
                                                 final UpdateModulrCorporateModel updateModulrCorporateModel,
                                                 final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("corporate_id", corporateId)
                .body(updateModulrCorporateModel)
                .when()
                .post("/admin/api/modulr/corporates/{corporate_id}/update");
    }

    public static Response registerModulrCorporate(final String corporateId,
                                                   final ModulrRegistrationModel modulrRegistrationModel,
                                                   final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("corporate_id", corporateId)
                .body(modulrRegistrationModel)
                .when()
                .post("/admin/api/modulr/corporates/{corporate_id}/register");
    }

    public static Response setResidentialCountriesConsumers(final String token,
                                                            final RegisteredCountriesSetCountriesModel setCountriesModel) {
        return getAuthenticatedRequest(token)
                .body(setCountriesModel)
                .when()
                .post("/admin/api/configs/consumers/sets/RESIDENTIAL_COUNTRIES/set");
    }

    public static Response setCompanyRegistrationCountries(final String token,
                                                           final RegisteredCountriesSetCountriesModel setCountriesModel) {
        return getAuthenticatedRequest(token)
                .body(setCountriesModel)
                .when()
                .post("/admin/api/configs/corporates/sets/COMPANY_REGISTRATION_COUNTRIES/set");
    }

    public static Response setApprovedNationalitiesConsumers(final String token,
                                                             final RegisteredCountriesSetCountriesModel setCountriesModel) {
        return getAuthenticatedRequest(token)
                .body(setCountriesModel)
                .when()
                .post("/admin/api/configs/consumers/sets/APPROVED_NATIONALITIES/set");
    }

    public static Response activateConsumer(final ActivateIdentityModel activateIdentityModel,
                                            final String consumerId,
                                            final String token) {
        return getBodyAuthenticatedRequest(activateIdentityModel, token)
                .pathParam("id", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{id}/activate");
    }

    public static Response deactivateConsumer(final DeactivateIdentityModel deactivateIdentityModel,
                                              final String consumerId,
                                              final String token) {
        return getBodyAuthenticatedRequest(deactivateIdentityModel, token)
                .pathParam("id", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{id}/deactivate");
    }

    public static Response deactivateConsumerUser(final String consumerId,
                                                  final String userId,
                                                  final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("consumerId", consumerId)
                .pathParam("userId", userId)
                .when()
                .post("/admin/api/consumers_v2/{consumerId}/users/{userId}/deactivate");
    }

    public static Response activateCorporate(final ActivateIdentityModel activateIdentityModel,
                                             final String corporateId,
                                             final String token) {
        return getBodyAuthenticatedRequest(activateIdentityModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/admin/api/corporates/{id}/activate");
    }

    public static Response deactivateCorporate(final DeactivateIdentityModel deactivateIdentityModel,
                                               final String corporateId,
                                               final String token) {
        return getBodyAuthenticatedRequest(deactivateIdentityModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/admin/api/corporates/{id}/deactivate");
    }

    public static Response deactivateCorporateUser(final String corporateId,
                                                   final String userId,
                                                   final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("corporateId", corporateId)
                .pathParam("userId", userId)
                .when()
                .post("/admin/api/corporates/{corporateId}/users/{userId}/deactivate");
    }

    public static Response startCorporateUserKyc(final String corporateId,
                                                 final String corporateUserId,
                                                 final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("user_id", corporateUserId)
                .when()
                .post("/admin/api/corporates/{id}/users/{user_id}/kyc");
    }

    public static Response getUserChallenges(final GetUserChallengesModel getUserChallengesModel,
                                             final String token,
                                             final String userId) {
        return getBodyAuthenticatedRequest(getUserChallengesModel, token)
                .pathParam("credential_id", userId)
                .when()
                .post("/admin/api/auth_sessions_challenges/{credential_id}/challenges/get");
    }

    public static Response setSemi(final String token,
                                   final AdminSemiToggleModel body) {
        return getAuthenticatedRequest(token)
                .body(body)
                .when()
                .post("/admin/api/auth_sessions/credentials/semi/set");
    }

    public static Response linkUseridToCorporateSemi(final UserId userToLink,
                                                     final String userId,
                                                     final String tenantAdminToken) {
        return getBodyAuthenticatedRequest(userToLink, tenantAdminToken)
                .pathParam("user_id", userId)
                .when()
                .post("admin/api/corporates/{user_id}/users/link");
    }

    public static Response unlinkUseridFromCorporateSemi(final UserId userToUnlink,
                                                         final String userId,
                                                         final String tenantAdminToken) {
        return getBodyAuthenticatedRequest(userToUnlink, tenantAdminToken)
                .pathParam("user_id", userId)
                .when()
                .post("admin/api/corporates/{user_id}/users/unlink");
    }

    public static Response elevateRootUser(final String token,
                                           final String corporateId,
                                           final String userId) {
        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("user_id", userId)
                .when()
                .post("/admin/api/corporates/{id}/users/{user_id}/elevate_root_user");
    }

    public static Response linkToUser(final String token,
                                      final String corporateId,
                                      final String beneficiaryId,
                                      final String userId) {
        return getAuthenticatedRequest(token)
                .body(Map.entry("user_id", userId))
                .pathParam("id", corporateId)
                .pathParam("beneficiary_id", beneficiaryId)
                .when()
                .post("/admin/api/corporates/{id}/beneficiaries/{beneficiary_id}/link_to_user");
    }

    public static Response cancelOwt(final String owtId,
                                     final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .pathParam("owt_id", owtId)
                .when()
                .post("/admin/api/outgoing_wire_transfers/{owt_id}/cancel");
    }

    public static Response createConsumerLevelConfiguration(final SetApplicantLevelModel setApplicantLevelModel,
                                                            final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(setApplicantLevelModel)
                .when()
                .post("/admin/api/sumsub/config/CONSUMER_LEVEL/create");
    }

    public static Response createCorporateLevelConfiguration(final SetApplicantLevelModel setApplicantLevelModel,
                                                             final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(setApplicantLevelModel)
                .when()
                .post("/admin/api/sumsub/config/CORPORATE_LEVEL/create");
    }

    public static Response deleteConsumerLevelConfiguration(final SetApplicantLevelModel setApplicantLevelModel,
                                                            final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(setApplicantLevelModel)
                .when()
                .post("/admin/api/sumsub/config/CONSUMER_LEVEL/delete");
    }

    public static Response deleteCorporateLevelConfiguration(final SetApplicantLevelModel setApplicantLevelModel,
                                                             final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(setApplicantLevelModel)
                .when()
                .post("/admin/api/sumsub/config/CORPORATE_LEVEL/delete");
    }

    public static Response getSubscriptions(final String programmeId,
                                            final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/subscriptions/{programme_id}/subscribers/get");
    }

    public static Response activateConsumerLevelCheck(final ActivateConsumerLevelCheckModel consumerLevelCheckModel,
                                                      final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(consumerLevelCheckModel)
                .when()
                .post("/admin/api/sumsub/config/level_checks");
    }

    public static Response deactivateConsumerLevelCheck(final DeactivateConsumerLevelCheckModel consumerLevelCheckModel,
                                                        final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(consumerLevelCheckModel)
                .when()
                .post("/admin/api/sumsub/config/CONSUMER_LEVEL_CHECKS/delete");
    }

    public static Response getConsumers(final ConsumersLimitModel limitModel,
                                        final String adminToken) {

        return getAuthenticatedRequest(adminToken)
                .body(limitModel)
                .when()
                .post("/admin/api/consumers_v2/get");
    }

    public static Response getConsumers(final ConsumersFilterModel consumersFilterModel,
                                        final String token) {
        return getBodyAuthenticatedRequest(consumersFilterModel, token)
                .when()
                .post("/admin/api/consumers_v2/get");
    }

    public static Response getCorporates(final CorporatesLimitModel limitModel,
                                         final String adminToken) {

        return getAuthenticatedRequest(adminToken)
                .body(limitModel)
                .when()
                .post("/admin/api/corporates/get");
    }

    public static Response getCorporates(final CorporatesFilterModel corporatesFilterModel,
                                         final String token) {

        return getBodyAuthenticatedRequest(corporatesFilterModel, token)
                .when()
                .post("/admin/api/corporates/get");
    }


    public static Response updateInnovatorUser(final UpdateInnovatorUserModel updateInnovatorUserModel,
                                               final String innovatorUserId,
                                               final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(updateInnovatorUserModel)
                .pathParam("innovator_user_id", innovatorUserId)
                .when()
                .post("/admin/api/innovator/users/{innovator_user_id}/update");
    }

    public static Response sendUserInvite(final SendAdminUserInviteModel sendAdminUserInviteModel,
                                          final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(sendAdminUserInviteModel)
                .when()
                .post("/admin/api/gateway/invites/create");
    }

    public static Response consumerUserInvite(final ConsumeAdminUserInviteModel consumeAdminUserInviteModel,
                                              final String inviteId) {

        return getBodyRequest(consumeAdminUserInviteModel)
                .pathParam("invite_id", inviteId)
                .when()
                .post("/admin/api/gateway/invites/{invite_id}/consume");
    }

    public static Response updateProfileConstraint(final PasswordConstraintsModel passwordConstraintsModel,
                                                   final String programmeId,
                                                   final String token) {
        return getBodyAuthenticatedRequest(passwordConstraintsModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/passwords/programmes/{programme_id}/profiles/constraints/update");
    }

    public static Response getProfileConstraint(final String programmeId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/passwords/programmes/{programme_id}/profiles/constraints/get");
    }

    public static Response consumerIdentityClosure(final String consumerId,
                                                   final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("consumerId", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{consumerId}/close");
    }

    public static Response corporateIdentityClosure(final String corporateId, final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .pathParam("corporateId", corporateId)
                .when()
                .post("/admin/api/corporates/{corporateId}/close");
    }

    public static Response removeDuplicateIdentityFlag(final RemoveDuplicateIdentityFlagModel removeDuplicateIdentityFlagModel,
                                                       final String consumerId,
                                                       final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(removeDuplicateIdentityFlagModel)
                .pathParam("consumer_id", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{consumer_id}/kyc/duplicate_remove");
    }

    public static Response setTransactionMonitoring(final String token,
                                                    final String programmeId,
                                                    final boolean txmEnabled) {
        return getBodyAuthenticatedRequest(new SetConfigModel(txmEnabled), token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/programmes/{programme_id}/txm/set");
    }

    public static Response setTrustBiometrics(final String token,
                                              final String programmeId,
                                              final boolean trustBiometricsEnabled) {
        return getBodyAuthenticatedRequest(new SetConfigModel(trustBiometricsEnabled), token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin/api/programmes/{programme_id}/trust_biometrics/set");
    }

    public static Response getInnovatorUser(final GetInnovatorUserModel getInnovatorUserModel,
                                            final String token) {
        return getBodyAuthenticatedRequest(getInnovatorUserModel, token)
                .when()
                .post("/admin/api/innovator/users/get");
    }

    public static Response updateConsumer(final UpdateConsumerModel updateConsumerModel,
                                          final String token,
                                          final String consumerId) {
        return getBodyAuthenticatedRequest(updateConsumerModel, token)
                .pathParam("id", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{id}/update");
    }

    public static Response activateConsumerUser(final String consumerId,
                                                final String consumerUserId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .pathParam("user_id", consumerUserId)
                .when()
                .post("/admin/api/consumers_v2/{id}/users/{user_id}/activate");
    }

    public static Response activateCorporateUser(final String corporateId,
                                                 final String userId,
                                                 final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("corporateId", corporateId)
                .pathParam("userId", userId)
                .when()
                .post("/admin/api/corporates/{corporateId}/users/{userId}/activate");
    }

    public static Response updateCorporate(final UpdateCorporateModel updateCorporateModel,
                                           final String token,
                                           final String corporateId) {
        return getBodyAuthenticatedRequest(updateCorporateModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/admin/api/corporates/{id}/update");
    }

    public static Response setMultiBiometricLoginFunction(final SwitchFunctionModel switchFunctionModel,
                                                          final String adminToken,
                                                          final String programme_id) {
        return getBodyAuthenticatedRequest(switchFunctionModel, adminToken)
                .pathParam("programme_id", programme_id)
                .when()
                .post("/admin/api/programmes/{programme_id}/trust_biometrics/set");
    }

    public static Response getMultiBiometricLoginFunction(final String adminToken,
                                                          final String programme_id) {
        return getAuthenticatedRequest(adminToken)
                .pathParam("programme_id", programme_id)
                .when()
                .post("/admin/api/programmes/{programme_id}/trust_biometrics/get");
    }

    public static Response retryTransfer(final String token,
                                         final ResumeTransactionModel resumeTransactionModel,
                                         final String transferId) {
        return getBodyAuthenticatedRequest(resumeTransactionModel, token)
                .pathParam("id", transferId)
                .when()
                .post("/admin/api/transfers_v2/{id}/retry");
    }

    public static Response retryTransfers(final String token,
                                          final RetryTransfersModel retryTransfersModel) {
        return getBodyAuthenticatedRequest(retryTransfersModel, token)
                .when()
                .post("/admin/api/transfers_v2/retry");
    }

    public static Response createProgramme(final String adminToken,
                                           final CreateProgrammeModel createProgrammeModel) {
        return getBodyAuthenticatedRequest(createProgrammeModel, adminToken)
                .when()
                .post("/admin/api/programmes/_/create");
    }

    public static Response updatePasswordProfile(final String adminToken,
                                                 final String programmeId,
                                                 final String passwordProfileId,
                                                 final CreatePasswordProfileModel passwordProfileModel) {
        return getBodyAuthenticatedRequest(passwordProfileModel, adminToken)
                .pathParam("programme_id", programmeId)
                .pathParam("identity_profile_id", passwordProfileId)
                .when()
                .post("/admin/api/passwords/programmes/{programme_id}/profiles/{identity_profile_id}/update");
    }

    public static Response createPlugin(final CreatePluginModel createPluginModel,
                                        final String adminToken) {
        return getBodyAuthenticatedRequest(createPluginModel, adminToken)
                .when()
                .post("/admin/api/plugin_registry/create");
    }

    public static Response switchModulrSubscriptionPermission(final String adminToken,
                                                              final String corporateId,
                                                              final SwitchModulrSubscriptionFeatureModel enabled) {
        return getBodyAuthenticatedRequest(enabled, adminToken)
                .pathParam("corporateId", corporateId)
                .when()
                .post("/admin/api/modulr/corporates/{corporateId}/set_ready");
    }

    public static Response getModulrSubscriptionPermission(final String adminToken,
                                                           final String corporateId) {
        return getAuthenticatedRequest(adminToken)
                .pathParam("corporateId", corporateId)
                .when()
                .post("/admin/api/modulr/corporates/{corporateId}/get");
    }

    public static Response getConsumerProfile(final String adminToken,
                                              final String programmeId,
                                              final String profileId) {
        return getAuthenticatedRequest(adminToken)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post("/admin/api/consumers_v2/programmes/{programme_id}/profiles/{profile_id}/get");
    }

    public static Response updateEmailValidationProvider(final UpdateEmailValidationProviderModel model,
                                                         final String adminToken,
                                                         final String programmeId,
                                                         final String profileId) {
        return getBodyAuthenticatedRequest(model, adminToken)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post("/admin/api/consumers_v2/programmes/{programme_id}/profiles/{profile_id}/update");
    }

    public static Response getConsumerAllUser(final Optional<GetUserFilterModel> model,
                                              final String consumerId,
                                              final String token) {
        return getBodyAuthenticatedRequest(model.orElse(GetUserFilterModel.builder().build()), token)
                .pathParam("id", consumerId)
                .when()
                .post("/admin/api/consumers_v2/{id}/users/get");
    }

    public static Response getCorporateAllUser(final Optional<GetUserFilterModel> model,
                                               final String corporateId,
                                               final String token) {
        return getBodyAuthenticatedRequest(model.orElse(GetUserFilterModel.builder().build()), token)
                .pathParam("id", corporateId)
                .when()
                .post("/admin/api/corporates/{id}/users/get");
    }

    public static Response updateConsumerProfile(final UpdateConsumerProfileModel updateConsumerProfileModel,
                                                 final String token,
                                                 final String programme_id,
                                                 final String profile_id) {
        return getBodyAuthenticatedRequest(updateConsumerProfileModel, token)
                .pathParam("programme_id", programme_id)
                .pathParam("profile_id", profile_id)
                .when()
                .post("/admin/api/consumers_v2/programmes/{programme_id}/profiles/{profile_id}/update");
    }

    public static Response updatePluginUrl(final String webhookUrl,
                                           final String token,
                                           final String pluginId) {
        return getBodyAuthenticatedRequest(CreatePluginModel.builder().webhookUrl(webhookUrl).build(), token)
                .pathParam("plugin_id", pluginId)
                .when()
                .post("/admin/api/plugin_registry/{plugin_id}/update");
    }

    public static Response getInnovator(final String token,
                                        final String innovatorId){
        return getAuthenticatedRequest(token)
                .pathParam("innovatorId", innovatorId)
                .when()
                .post("/admin/api/innovator/innovators/{innovatorId}/get");
    }

    public static Response getReports(final GetReportsModel getReportsModel,
                                      final String token) {
        return getBodyAuthenticatedRequest(getReportsModel, token)
                .when()
                .post("/admin/api/innovator/reports/get");
    }
}