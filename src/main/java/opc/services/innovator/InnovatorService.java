package opc.services.innovator;

import commons.models.GetUserFilterModel;
import commons.services.BaseService;
import io.restassured.response.Response;
import opc.models.EmailTemplateContextModel;
import opc.models.GetUsersFiltersModel;
import opc.models.admin.GetCorporateInformationModel;
import opc.models.admin.GetUserChallengesModel;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.ConsumersFilterModel;
import opc.models.innovator.CorporatesFilterModel;
import opc.models.innovator.CreateApplicationInitModel;
import opc.models.innovator.CreateConsumerProfileModel;
import opc.models.innovator.CreateCorporateProfileModel;
import opc.models.innovator.CreateLinkedAccountProfileModel;
import opc.models.innovator.CreateManagedAccountProfileModel;
import opc.models.innovator.CreateOutgoingDirectDebitProfileModel;
import opc.models.innovator.CreateOwtProfileModel;
import opc.models.innovator.CreatePasswordProfileModel;
import opc.models.innovator.CreateProgrammeModel;
import opc.models.innovator.CreateSendProfileModel;
import opc.models.innovator.CreateTransfersProfileModel;
import opc.models.innovator.CreateUnassignedCardBatchModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.innovator.EmailTemplateModel;
import opc.models.innovator.GetProgrammeKeysModel;
import opc.models.innovator.GetReportsModel;
import opc.models.innovator.InnovatorRegistrationModel;
import opc.models.innovator.InviteInnovatorUserModel;
import opc.models.innovator.PasswordConstraintsModel;
import opc.models.innovator.RevenueStatementRequestModel;
import opc.models.innovator.SpendRulesModel;
import opc.models.innovator.UpdateAuth0Model;
import opc.models.innovator.UpdateConsumerModel;
import opc.models.innovator.UpdateConsumerProfileModel;
import opc.models.innovator.UpdateCorporateModel;
import opc.models.innovator.UpdateCorporateProfileModel;
import opc.models.innovator.UpdateCorporateUserModel;
import opc.models.innovator.UpdateManagedAccountProfileModel;
import opc.models.innovator.UpdateManagedCardsProfileV2Model;
import opc.models.innovator.UpdateOkayBrandingModel;
import opc.models.innovator.UpdateOkayModel;
import opc.models.innovator.UpdateOwtProfileModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.shared.GetInnovatorUserModel;
import opc.models.shared.LoginModel;
import opc.models.shared.UpdateSpendRulesModel;

import java.util.Optional;

public class InnovatorService extends BaseService {

    public static Response registerInnovator(
            final InnovatorRegistrationModel innovatorRegistrationModel) {
        return getBodyRequest(innovatorRegistrationModel)
                .when()
                .post("/innovator/api/gateway/register");
    }

    public static Response loginInnovator(final LoginModel loginModel) {
        return getBodyRequest(loginModel)
                .when()
                .post("/innovator/api/gateway/login");
    }

    public static Response createProgramme(final CreateProgrammeModel createProgrammeModel,
                                           final String token) {
        return getBodyAuthenticatedRequest(createProgrammeModel, token)
                .when()
                .post("/innovator/api/programmes/_/create");
    }

    public static Response createCorporateProfile(
            final CreateCorporateProfileModel createCorporateProfileModel,
            final String token,
            final String programmeId) {
        return getBodyAuthenticatedRequest(createCorporateProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/corporates/programmes/{programme_id}/profiles/_/create");
    }

    public static Response createConsumerProfile(
            final CreateConsumerProfileModel createConsumerProfileModel,
            final String token,
            final String programmeId) {
        return getBodyAuthenticatedRequest(createConsumerProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/consumers_v2/programmes/{programme_id}/profiles/_/create");
    }

    public static Response createManagedAccountProfile(
            final CreateManagedAccountProfileModel createManagedAccountProfileModel,
            final String token,
            final String programmeId) {
        return getBodyAuthenticatedRequest(createManagedAccountProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/managed_accounts_v2/programmes/{programme_id}/profiles/_/create");
    }

    public static Response getManagedAccountStatement(final String managedAccountId,
                                                      final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedAccountId)
                .when()
                .post("/innovator/api/managed_accounts_v2/{id}/statement/get");
    }

    public static Response createTransfersProfileV2(
            final CreateTransfersProfileModel createTransfersProfileModel,
            final String token,
            final String programmeId) {
        return getBodyAuthenticatedRequest(createTransfersProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/transfers_v2/programmes/{programme_id}/profiles/_/create");
    }

    public static Response createSendsProfileV2(final CreateSendProfileModel createSendProfileModel,
                                                final String token,
                                                final String programmeId) {
        return getBodyAuthenticatedRequest(createSendProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/send_v2/programmes/{programme_id}/profiles/_/create");
    }

    public static Response createOwtProfile(final CreateOwtProfileModel createOwtProfileModel,
                                            final String token,
                                            final String programmeId) {
        return getBodyAuthenticatedRequest(createOwtProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/outgoing_wire_transfers/programmes/{programme_id}/profiles/_/create");
    }

    public static Response updateOwtProfile(final UpdateOwtProfileModel updateOwtProfileModel,
                                            final String profileId,
                                            final String token,
                                            final String programmeId) {
        return getBodyAuthenticatedRequest(updateOwtProfileModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post("/innovator/api/outgoing_wire_transfers/programmes/{programme_id}/profiles/{profile_id}/update");
    }

    public static Response createPasswordProfile(
            final CreatePasswordProfileModel createPasswordProfileModel,
            final String token,
            final String programmeId,
            final String identityProfileId) {
        return getBodyAuthenticatedRequest(createPasswordProfileModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("identity_profile_id", identityProfileId)
                .when()
                .post("/innovator/api/passwords/programmes/{programme_id}/profiles/{identity_profile_id}/create");
    }

    public static Response getPasswordProfile(final String token,
                                              final String programmeId,
                                              final String identityProfileId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .pathParam("identity_profile_id", identityProfileId)
                .when()
                .post("/innovator/api/passwords/programmes/{programme_id}/profiles/{identity_profile_id}/get");
    }

    public static Response getProgrammeDetails(final String token, final String programmeId) {
        return getBodyAuthenticatedRequest(GetProgrammeKeysModel.getActiveKeysModel(), token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/programmes/{programme_id}/keys/get");
    }

    public static Response deactivateInnovator(final String token, final String innovatorId) {
        return getAuthenticatedRequest(token)
                .pathParam("id", innovatorId)
                .when()
                .post("/innovator/api/innovator/innovators/{id}/deactivate");
    }

    public static Response replenishCardPool(
            final CreateUnassignedCardBatchModel createUnassignedCardBatchModel,
            final String token) {
        return getBodyAuthenticatedRequest(createUnassignedCardBatchModel, token)
                .when()
                .post("/innovator/api/managed_cards_v2/card_pool/replenish");
    }

    public static Response getConsumers(final ConsumersFilterModel consumersFilterModel,
                                        final String token) {
        return getBodyAuthenticatedRequest(consumersFilterModel, token)
                .when()
                .post("/innovator/api/consumers_v2/get");
    }

    public static Response getCorporates(final CorporatesFilterModel corporatesFilterModel,
                                         final String token) {
        return getBodyAuthenticatedRequest(corporatesFilterModel, token)
                .when()
                .post("/innovator/api/corporates/get");
    }

    public static Response updateManagedCardsProfileV2(
            final UpdateManagedCardsProfileV2Model updateManagedCardsProfileV2Model,
            final String token,
            final String programmeId,
            final String profileId) {
        return getBodyAuthenticatedRequest(updateManagedCardsProfileV2Model, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/managed_cards_v2/programmes/{programme_id}/profiles/{profile_id}/update");
    }

    public static Response updateProgramme(final UpdateProgrammeModel updateProgrammeModel,
                                           final String programmeId,
                                           final String token) {
        return getBodyAuthenticatedRequest(updateProgrammeModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/programmes/{programme_id}/update");
    }

    public static Response updateProfileConstraint(
            final PasswordConstraintsModel passwordConstraintsModel,
            final String programmeId,
            final String token) {
        return getBodyAuthenticatedRequest(passwordConstraintsModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/passwords/programmes/{programme_id}/profiles/constraints/update");
    }

    public static Response getProfileConstraint(final String programmeId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/passwords/programmes/{programme_id}/profiles/constraints/get");
    }

    public static Response setProfileSpendRules(final SpendRulesModel spendRulesModel,
                                                final String token,
                                                final String programmeId,
                                                final String profileId) {
        return getBodyAuthenticatedRequest(spendRulesModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/managed_cards_v2/programmes/{programme_id}/profiles/{profile_id}/spend_rule/set");
    }

    public static Response getCardSpendRules(final String token,
                                             final String managedCardId) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post("/innovator/api/managed_cards_v2/{id}/spend_rule/get");
    }

    public static Response getProfileSpendRules(final String token,
                                                final String programmeId,
                                                final String profileId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/managed_cards_v2/programmes/{programme_id}/profiles/{profile_id}/spend_rule/get");
    }

    public static Response updateCorporateUser(
            final UpdateCorporateUserModel updateCorporateUserModel,
            final String token,
            final String corporateId,
            final String userId) {
        return getBodyAuthenticatedRequest(updateCorporateUserModel, token)
                .pathParam("id", corporateId)
                .pathParam("user_id", userId)
                .when()
                .post("/innovator/api/corporates/{id}/users/{user_id}/update");
    }

    public static Response getReports(final GetReportsModel getReportsModel,
                                      final String token) {
        return getBodyAuthenticatedRequest(getReportsModel, token)
                .when()
                .post("/innovator/api/innovator/reports/get");
    }

    public static Response getReport(final String reportId,
                                     final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("report_id", reportId)
                .when()
                .post("/innovator/api/innovator/reports/{report_id}/get");
    }

    public static Response unblockPhysicalCardPin(final String managedCardId,
                                                  final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post("/innovator/api/managed_cards_v2/{id}/physical/pin/unblock");
    }

    public static Response updateManagedAccountProfile(
            final UpdateManagedAccountProfileModel updateManagedAccountProfileModel,
            final String profileId,
            final String token,
            final String programmeId) {
        return getBodyAuthenticatedRequest(updateManagedAccountProfileModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/managed_accounts_v2/programmes/{programme_id}/profiles/{profile_id}/update");
    }

    public static Response getManagedAccountProfile(final String profileId,
                                                    final String token,
                                                    final String programmeId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/managed_accounts_v2/programmes/{programme_id}/profiles/{profile_id}/get");
    }

    public static Response getInnovatorRevenue(final String innovatorId,
                                               final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", innovatorId)
                .when()
                .post("/innovator/api/innovator/innovators/{id}/revenue/get");
    }

    public static Response getInnovatorRevenueStatement(final String innovatorId,
                                                        final String token,
                                                        final RevenueStatementRequestModel revenueStatementRequestModel) {

        return getBodyAuthenticatedRequest(revenueStatementRequestModel, token)
                .pathParam("id", innovatorId)
                .when()
                .post("/innovator/api/innovator/innovators/{id}/revenue/statements/get");
    }

    public static Response setCorporateProfileTemplate(final EmailTemplateModel emailTemplateModel,
                                                       final String programmeId,
                                                       final String profileId,
                                                       final String token) {

        return getBodyAuthenticatedRequest(emailTemplateModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/corporates/programmes/{programme_id}/profiles/{profile_id}/email_templates/set");
    }

    public static Response getCorporateProfileTemplate(
            final EmailTemplateContextModel emailTemplateContextModel,
            final String programmeId,
            final String profileId,
            final String token) {

        return getBodyAuthenticatedRequest(emailTemplateContextModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/corporates/programmes/{programme_id}/profiles/{profile_id}/email_templates/get");
    }

    public static Response setConsumerProfileTemplate(final EmailTemplateModel emailTemplateModel,
                                                      final String programmeId,
                                                      final String profileId,
                                                      final String token) {

        return getBodyAuthenticatedRequest(emailTemplateModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/consumers_v2/programmes/{programme_id}/profiles/{profile_id}/email_templates/set");
    }

    public static Response getConsumerProfileTemplate(
            final EmailTemplateContextModel emailTemplateContextModel,
            final String programmeId,
            final String profileId,
            final String token) {
        return getBodyAuthenticatedRequest(emailTemplateContextModel, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/consumers_v2/programmes/{programme_id}/profiles/{profile_id}/email_templates/get");
    }

    public static Response createPaymentModel(
            final CreateApplicationInitModel createApplicationInitModel,
            final String token) {
        return getBodyAuthenticatedRequest(createApplicationInitModel, token)
                .when()
                .post("/innovator/api/gateway/payment_models/init");
    }

    public static Response deactivateCorporate(final DeactivateIdentityModel deactivateIdentityModel,
                                               final String corporateId,
                                               final String token) {
        return getBodyAuthenticatedRequest(deactivateIdentityModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/innovator/api/corporates/{id}/deactivate");
    }

    public static Response deactivateConsumer(final DeactivateIdentityModel deactivateIdentityModel,
                                              final String consumerId,
                                              final String token) {
        return getBodyAuthenticatedRequest(deactivateIdentityModel, token)
                .pathParam("id", consumerId)
                .when()
                .post("/innovator/api/consumers_v2/{id}/deactivate");
    }

    public static Response getManagedCardInnovatorConfig(final String programmeId,
                                                         final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/gateway/programmes/{programme_id}/managed_cards_innovator_config/get");
    }

    public static Response deactivateCorporateUser(final String corporateId,
                                                   final String corporateUserId,
                                                   final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("user_id", corporateUserId)
                .when()
                .post("/innovator/api/corporates/{id}/users/{user_id}/deactivate");
    }

    public static Response deactivateConsumerUser(final String consumerId,
                                                  final String consumerUserId,
                                                  final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .pathParam("user_id", consumerUserId)
                .when()
                .post("/innovator/api/consumers_v2/{id}/users/{user_id}/deactivate");
    }

    public static Response getConsumerAllUser(final Optional<GetUserFilterModel> model,
                                              final String consumerId,
                                              final String token) {
        return getBodyAuthenticatedRequest(model.orElse(GetUserFilterModel.builder().build()), token)
                .pathParam("id", consumerId)
                .when()
                .post("/innovator/api/consumers_v2/{id}/users/get");
    }

    public static Response getCorporateAllUser(final Optional<GetUserFilterModel> model,
                                               final String corporateId,
                                               final String token) {
        return getBodyAuthenticatedRequest(model.orElse(GetUserFilterModel.builder().build()), token)
                .pathParam("id", corporateId)
                .when()
                .post("/innovator/api/corporates/{id}/users/get");
    }

    public static Response enableAuthy(final String programmeId,
                                       final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("innovator/api/authy/programmes/{programme_id}/enable");
    }

    public static Response disableAuthy(final String programmeId,
                                        final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("innovator/api/authy/programmes/{programme_id}/disable");
    }

    public static Response getManagedCardProfile(final String profileId,
                                                 final String token,
                                                 final String programmeId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .post(
                        "/innovator/api/managed_cards_v2/programmes/{programme_id}/profiles/{profile_id}/get");
    }

    public static Response downloadKybScreeningReport(final String corporateId,
                                                      final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("corporate_id", corporateId)
                .when()
                .post("/innovator/api/corporates/{corporate_id}/kyb/screening_report");
    }

    public static Response downloadKycScreeningReport(final String consumerId,
                                                      final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("consumer_id", consumerId)
                .when()
                .post("/innovator/api/consumers_v2/{consumer_id}/kyc/screening_report");
    }

    public static Response createOddProfile(
            final CreateOutgoingDirectDebitProfileModel createOutgoingDirectDebitProfileModel,
            final String token,
            final String programmeId) {
        return getBodyAuthenticatedRequest(createOutgoingDirectDebitProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/outgoing_direct_debits/programmes/{programme_id}/profiles/_/create");
    }

    public static Response enableOkay(final String programmeId,
                                      final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("innovator/api/okay/programmes/{programme_id}/enable");
    }

    public static Response disableOkay(final String programmeId,
                                       final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("innovator/api/okay/programmes/{programme_id}/disable");
    }

    public static Response updateOkay(final UpdateOkayModel updateOkayModel,
                                      final String programmeId,
                                      final String token) {
        return getBodyAuthenticatedRequest(updateOkayModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/okay/programmes/{programme_id}/update");
    }

    public static Response addCardSpendRules(final SpendRulesModel spendRulesModel,
                                             final String managedCardId,
                                             final String token) {
        return getBodyAuthenticatedRequest(spendRulesModel, token)
                .pathParam("id", managedCardId)
                .when()
                .post("/innovator/api/managed_cards_v2/{id}/spend_rule/add");
    }

    public static Response updateCardSpendRules(final UpdateSpendRulesModel spendRulesModel,
                                                final String managedCardId,
                                                final String token) {
        return getBodyAuthenticatedRequest(spendRulesModel, token)
                .pathParam("id", managedCardId)
                .when()
                .post("/innovator/api/managed_cards_v2/{id}/spend_rule/update");
    }

    public static Response updateCorporateProfile(
            final UpdateCorporateProfileModel updateCorporateProfileModel,
            final String token,
            final String programme_id,
            final String profile_id) {
        return getBodyAuthenticatedRequest(updateCorporateProfileModel, token)
                .pathParam("programme_id", programme_id)
                .pathParam("profile_id", profile_id)
                .when()
                .post("/innovator/api/corporates/programmes/{programme_id}/profiles/{profile_id}/update");
    }

    public static Response updateConsumerProfile(
            final UpdateConsumerProfileModel updateConsumerProfileModel,
            final String token,
            final String programme_id,
            final String profile_id) {
        return getBodyAuthenticatedRequest(updateConsumerProfileModel, token)
                .pathParam("programme_id", programme_id)
                .pathParam("profile_id", profile_id)
                .when()
                .post("/innovator/api/consumers_v2/programmes/{programme_id}/profiles/{profile_id}/update");
    }

    public static Response deleteCardSpendRules(final String managedCardId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post("/innovator/api/managed_cards_v2/{id}/spend_rule/delete");
    }

    public static Response getManagedCard(final String managedCardId,
                                          final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedCardId)
                .when()
                .post("/innovator/api/managed_cards_v2/{id}/get");
    }

    public static Response getManagedAccount(final String managedAccountId,
                                             final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", managedAccountId)
                .when()
                .post("/innovator/api/managed_accounts_v2/{id}/get");
    }

    public static Response updateOkayBranding(final UpdateOkayBrandingModel updateOkayBrandingModel,
                                              final String token,
                                              final String programmeId) {
        return getBodyAuthenticatedRequest(updateOkayBrandingModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/okay/programmes/{programme_id}/branding/update");
    }

    public static Response getOkayBranding(final String token,
                                           final String programmeId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/okay/programmes/{programme_id}/branding/get");
    }

    public static Response getCorporateUser(final String token,
                                            final String corporateId,
                                            final String userId) {
        return getAuthenticatedRequest(token)
                .pathParam("corporateId", corporateId)
                .pathParam("userId", userId)
                .when()
                .post("/innovator/api/corporates/{corporateId}/users/{userId}/get");
    }

    public static Response getConsumerUser(final String token,
                                           final String consumerId,
                                           final String userId) {

        return getAuthenticatedRequest(token)
                .pathParam("consumerId", consumerId)
                .pathParam("userId", userId)
                .when()
                .post("/innovator/api/consumers_v2/{consumerId}/users/{userId}/get");
    }

    public static Response getCorporatesInformation(final GetCorporateInformationModel getCorporateInformationModel,
                                                    final String token) {
        return getBodyAuthenticatedRequest(getCorporateInformationModel, token)
                .when()
                .post("/innovator/api/corporates/get");
    }

    public static Response getCorporateBeneficiaries(final String token,
                                                     final String corporateId) {

        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .when()
                .post("/innovator/api/corporates/{id}/kyb/beneficiaries/get");
    }

    public static Response resendBeneficiaryKycEmail(final String token,
                                                     final String corporateId,
                                                     final String beneficiaryId) {

        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("beneficiary_id", beneficiaryId)
                .when()
                .post("/innovator/api/corporates/{id}/beneficiaries/{beneficiary_id}/kyc/resend");
    }

    public static Response getBeneficiaryKycUrl(final String token,
                                                final String corporateId,
                                                final String beneficiaryId) {

        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("beneficiary_id", beneficiaryId)
                .when()
                .post("/innovator/api/corporates/{id}/beneficiaries/{beneficiary_id}/kyc/get_url");
    }

    public static Response enableAuth0(final String token,
                                       final String programmeId) {

        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/iam/programmes/{programme_id}/enable");
    }

    public static Response updateAuth0(final String token,
                                       final String programmeId) {

        return getBodyAuthenticatedRequest(UpdateAuth0Model.defaultUpdateAuth0Model(), token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/iam/programmes/{programme_id}/update");
    }

    public static Response startCorporateUserKyc(final String corporateId,
                                                 final String corporateUserId,
                                                 final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("user_id", corporateUserId)
                .when()
                .post("/innovator/api/corporates/{id}/users/{user_id}/kyc");
    }

    public static Response getUserChallenges(final GetUserChallengesModel getUserChallengesModel,
                                             final String token,
                                             final String userId) {
        return getBodyAuthenticatedRequest(getUserChallengesModel, token)
                .pathParam("credential_id", userId)
                .when()
                .post("innovator/api/auth_sessions_challenges/{credential_id}/challenges/get");
    }

    public static Response inviteNewUser(final InviteInnovatorUserModel inviteInnovatorUserModel,
                                         final String token) {

        return getAuthenticatedRequest(token)
                .body(inviteInnovatorUserModel)
                .when()
                .post("/innovator/api/gateway/invites/create");
    }

    public static Response getInnovatorUsers(final GetInnovatorUserModel getInnovatorUserModel,
                                             final String token) {
        return getBodyAuthenticatedRequest(getInnovatorUserModel, token)
                .when()
                .post("/innovator/api/gateway/users/get");
    }

    public static Response getConsumer(final String consumerId,
                                       final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .when()
                .post("/innovator/api/consumers_v2/{id}/get");
    }

    public static Response updateConsumer(final UpdateConsumerModel updateConsumerModel,
                                          final String token,
                                          final String consumerId) {
        return getBodyAuthenticatedRequest(updateConsumerModel, token)
                .pathParam("id", consumerId)
                .when()
                .post("/innovator/api/consumers_v2/{id}/update");
    }

    public static Response activateConsumer(final String consumerId,
                                            final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .when()
                .post("/innovator/api/consumers_v2/{id}/activate");
    }

    public static Response activateConsumerUser(final String consumerId,
                                                final String consumerUserId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .pathParam("user_id", consumerUserId)
                .when()
                .post("/innovator/api/consumers_v2/{id}/users/{user_id}/activate");
    }

    public static Response getCorporate(final String token,
                                        final String corporateId) {

        return getAuthenticatedRequest(token)
                .pathParam("corporateId", corporateId)
                .when()
                .post("/innovator/api/corporates/{corporateId}/get");
    }

    public static Response updateCorporate(final UpdateCorporateModel updateCorporateModel,
                                           final String token,
                                           final String corporateId) {
        return getBodyAuthenticatedRequest(updateCorporateModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/innovator/api/corporates/{id}/update");
    }

    public static Response activateCorporate(final ActivateIdentityModel activateIdentityModel,
                                             final String corporateId,
                                             final String token) {
        return getBodyAuthenticatedRequest(activateIdentityModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/innovator/api/corporates/{id}/activate");
    }

    public static Response activateCorporateUser(final String corporateId,
                                                 final String corporateUserId,
                                                 final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("user_id", corporateUserId)
                .when()
                .post("/innovator/api/corporates/{id}/users/{user_id}/activate");
    }

    public static Response getProgrammes(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .post("/innovator/api/programmes/get");
    }

    public static Response startLostPassword(final LostPasswordStartModel lostPasswordStartModel){
        return getBodyRequest(lostPasswordStartModel)
                .when()
                .post("/innovator/api/passwords/lost_password/start");
    }

    public static Response createLinkedAccountProfile(
            final CreateLinkedAccountProfileModel createLinkedAccountProfileModel,
            final String token,
            final String programmeId) {
        return getBodyAuthenticatedRequest(createLinkedAccountProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator/api/linked_accounts/programmes/{programme_id}/profiles/_/create");
    }

    public static Response getCorporatesDetails(final String token,
                                                    final String corporateId) {
        return getBodyAuthenticatedRequest(Optional.empty(), token)
                .pathParam("id", corporateId)
                .when()
                .post("innovator/api/corporates/{id}/users/get");
    }


    public static Response getPluginsOnProgramme (final String token,
                                                  final String programmeId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .post("/innovator/api/programmes/{programme_id}/plugins/get");
    }

    public static Response linkPluginToProgramme (final String token,
                                                  final String programmeId,
                                                  final String pluginId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .pathParam("plugin_id", pluginId)
                .post("/innovator/api/programmes/{programme_id}/plugins/{plugin_id}/install");
    }
    public static Response getConsumerKyc(final String consumerId,
                                          final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .when()
                .post("/innovator/api/consumers_v2/{id}/kyc/get");
    }
    public static Response installPlugin(final String token,
                                         final String programmeId,
                                         final String pluginCode) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .pathParam("plugin_code", pluginCode)
                .when()
                .post("/innovator/api/programmes/{programme_id}/plugins/{plugin_code}/install");
    }

    public static Response getConsumerAllUsers(final String token,
                                               final String consumerId,
                                               final Optional<GetUsersFiltersModel> model) {

        return getBodyAuthenticatedRequest(model.orElse(GetUsersFiltersModel.builder().build()), token)
                .pathParam("consumerId", consumerId)
                .when()
                .post("/innovator/api/consumers_v2/{consumerId}/users/get");
    }

    public static Response getCorporateAllUsers(final String token,
                                                final String corporateId,
                                                final Optional<GetUsersFiltersModel> model) {

        return getBodyAuthenticatedRequest(model.orElse(GetUsersFiltersModel.builder().build()), token)
                .pathParam("corporateId", corporateId)
                .when()
                .post("/innovator/api/corporates/{corporateId}/users/get");
    }

}
