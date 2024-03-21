package opc.services.adminnew;

import commons.services.BaseService;
import io.restassured.response.Response;
import opc.enums.opc.IdentityType;
import opc.models.GetUsersFiltersModel;
import opc.models.admin.AcceptInviteModel;
import opc.models.admin.AddPermissionsToRoleModel;
import opc.models.admin.AssignAdminsToRoleModel;
import opc.models.admin.AuthorisationsRequestModel;
import opc.models.admin.ContextPropertiesV2Model;
import opc.models.admin.CreateRoleModel;
import opc.models.admin.DeletePropertiesModel;
import opc.models.admin.GetInnovatorsModel;
import opc.models.admin.ImpersonateTenantModel;
import opc.models.admin.InviteUserModel;
import opc.models.admin.LimitsApiContextWithCurrencyModel;
import opc.models.admin.LimitsApiLowValueExemptionModel;
import opc.models.admin.LimitsApiModel;
import opc.models.admin.OwtProgrammes;
import opc.models.admin.PagingLimitModel;
import opc.models.admin.RegisteredCountriesSetCountriesModel;
import opc.models.admin.RejectDepositModel;
import opc.models.admin.RemoveDuplicateIdentityFlagModel;
import opc.models.admin.ReviewDecisionModel;
import opc.models.admin.SettlementRetryModel;
import opc.models.admin.UpdateAllowedCountriesModel;
import opc.models.admin.UpdateConsumerProfileModel;
import opc.models.admin.UpdateCorporateProfileModel;
import opc.models.admin.UpdateCorporateUserModel;
import opc.models.admin.UpdateEmailValidationProviderModel;
import opc.models.admin.UpdateKybModel;
import opc.models.admin.UpdateKycModel;
import opc.models.admin.UpdateRoleModel;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.ConsumersFilterModel;
import opc.models.innovator.CorporatesFilterModel;
import opc.models.innovator.CreateProgrammeModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.innovator.GetReportsModel;
import opc.models.innovator.UpdateConsumerModel;
import opc.models.innovator.UpdateCorporateModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminService extends BaseService {

    public static Response login() {
        return getBodyRequest(new LoginModel("admin@weavr.io", new PasswordModel("!Password123!10")))
                .when()
                .post("/admin_new/gateway/login");
    }

    public static String loginAdmin() {
        return getBodyRequest(new LoginModel("admin@weavr.io", new PasswordModel("!Password123!10")))
                .when()
                .post("/admin_new/gateway/login")
                .jsonPath()
                .get("token");
    }

    public static String loginAdmin(final LoginModel loginModel) {
        return getBodyRequest(loginModel)
                .when()
                .post("/admin_new/gateway/login")
                .jsonPath()
                .get("token");
    }

    public static Response impersonateTenant(final String tenantId,
                                             final String token) {
        return getBodyAuthenticatedRequest(new ImpersonateTenantModel(tenantId), token)
                .when()
                .post("/admin_new/gateway/current/tenant");
    }

    public static Response removeDuplicateIdentityFlag(final RemoveDuplicateIdentityFlagModel removeDuplicateIdentityFlagModel,
                                                       final String consumerId,
                                                       final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .body(removeDuplicateIdentityFlagModel)
                .pathParam("consumer_id", consumerId)
                .when()
                .post("/admin_new/consumers/{consumer_id}/kyc/duplicate_remove");
    }

    public static Response consumerIdentityClosure(final String consumerId,
                                                   final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("consumerId", consumerId)
                .when()
                .post("/admin_new/consumers/{consumerId}/close");
    }

    public static Response corporateIdentityClosure(final String corporateId, final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .pathParam("id", corporateId)
                .when()
                .post("/admin_new/corporates/{id}/close");
    }

    public static Response deactivateCorporate(final DeactivateIdentityModel deactivateIdentityModel,
                                               final String corporateId,
                                               final String token) {
        return getBodyAuthenticatedRequest(deactivateIdentityModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/admin_new/corporates/{id}/deactivate");
    }

    public static Response deactivateCorporateUser(final String corporateId,
                                                   final String userId,
                                                   final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("corporateId", corporateId)
                .pathParam("userId", userId)
                .when()
                .post("/admin_new/corporates/{corporateId}/users/{userId}/deactivate");
    }

    public static Response deactivateConsumer(final DeactivateIdentityModel deactivateIdentityModel,
                                              final String consumerId,
                                              final String token) {
        return getBodyAuthenticatedRequest(deactivateIdentityModel, token)
                .pathParam("id", consumerId)
                .when()
                .post("/admin_new/consumers/{id}/deactivate");
    }

    public static Response deactivateConsumerUser(final String consumerId,
                                                  final String userId,
                                                  final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("consumerId", consumerId)
                .pathParam("userId", userId)
                .when()
                .post("/admin_new/consumers/{consumerId}/users/{userId}/deactivate");
    }

    public static Response setConsumerLimits(final String token,
                                             final LimitsApiLowValueExemptionModel limitsApiModel) {
        return getBodyAuthenticatedRequest(limitsApiModel, token)
                .when()
                .post("/admin_new/consumers/low_value_exemption/set");
    }

    public static Response setCorporateLimits(final String token,
                                              final LimitsApiLowValueExemptionModel limitsApiModel) {
        return getBodyAuthenticatedRequest(limitsApiModel, token)
                .when()
                .post("/admin_new/corporates/low_value_exemption/set");
    }

    public static Response getConsumerLimits(final String token,
                                             final LimitsApiContextWithCurrencyModel limitsApiContextModel) {
        return getBodyAuthenticatedRequest(limitsApiContextModel, token)
                .when()
                .post("/admin_new/consumers/low_value_exemption/get");
    }

    public static Response getCorporateLimits(final String token,
                                              final LimitsApiContextWithCurrencyModel limitsApiContextModel) {
        return getBodyAuthenticatedRequest(limitsApiContextModel, token)
                .when()
                .post("/admin_new/corporates/low_value_exemption/set");
    }

    public static Response getConsumers(final ConsumersFilterModel consumersFilterModel,
                                        final String token) {
        return getBodyAuthenticatedRequest(consumersFilterModel, token)
                .when()
                .post("/admin_new/consumers/find");
    }

    public static Response getConsumer(final String consumerId,
                                       final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .when()
                .get("/admin_new/consumers/{id}");
    }

    public static Response updateConsumer(final UpdateConsumerModel updateConsumerModel,
                                          final String token,
                                          final String consumerId) {
        return getBodyAuthenticatedRequest(updateConsumerModel, token)
                .pathParam("id", consumerId)
                .when()
                .patch("/admin_new/consumers/{id}");
    }

    public static Response activateConsumer(final ActivateIdentityModel activateIdentityModel,
                                            final String consumerId,
                                            final String token) {
        return getBodyAuthenticatedRequest(activateIdentityModel, token)
                .pathParam("id", consumerId)
                .when()
                .post("/admin_new/consumers/{id}/activate");
    }

    public static Response activateConsumerUser(final String consumerId,
                                                final String consumerUserId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .pathParam("user_id", consumerUserId)
                .when()
                .post("/admin_new/consumers/{id}/users/{user_id}/activate");
    }

    public static Response getCorporates(final CorporatesFilterModel corporatesFilterModel,
                                         final String token) {

        return getBodyAuthenticatedRequest(corporatesFilterModel, token)
                .when()
                .post("/admin_new/corporates/find");
    }

    public static Response getCorporate(final String token,
                                        final String corporateId) {

        return getAuthenticatedRequest(token)
                .pathParam("corporateId", corporateId)
                .when()
                .get("/admin_new/corporates/{corporateId}");
    }

    public static Response updateCorporate(final UpdateCorporateModel updateCorporateModel,
                                           final String token,
                                           final String corporateId) {
        return getBodyAuthenticatedRequest(updateCorporateModel, token)
                .pathParam("id", corporateId)
                .when()
                .patch("/admin_new/corporates/{id}");
    }

    public static Response activateCorporate(final ActivateIdentityModel activateIdentityModel,
                                             final String corporateId,
                                             final String token) {
        return getBodyAuthenticatedRequest(activateIdentityModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/admin_new/corporates/{id}/activate");
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
                .patch("/admin_new/corporates/{id}/users/{user_id}");
    }

    public static Response activateCorporateUser(final String corporateId,
                                                 final String userId,
                                                 final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("userId", userId)
                .when()
                .post("/admin_new/corporates/{id}/users/{userId}/activate");
    }

    public static Response authorisationExpiryFind(final AuthorisationsRequestModel authorisationsRequestModel,
                                                   final String token) {
        return getBodyAuthenticatedRequest(authorisationsRequestModel, token)
                .when()
                .post("/admin_new/managed_cards/authorisations/expiry/find");
    }

    public static Response createProgramme(final String token,
                                           final CreateProgrammeModel programmeModel) {
        return getAuthenticatedRequest(token)
                .body(programmeModel)
                .when()
                .post("/admin_new/programmes");
    }

    public static Response rejectDeposit(final RejectDepositModel rejectDepositModel,
                                         final String depositId,
                                         final String adminToken) {
        return getBodyAuthenticatedRequest(rejectDepositModel, adminToken)
                .pathParam("id", depositId)
                .when()
                .post("/admin_new/managed_accounts/deposits/{id}/reject");
    }

    public static Response updateCompanyRegistrationCountries(final String token,
                                                              final RegisteredCountriesSetCountriesModel setCountriesModel) {
        return getAuthenticatedRequest(token)
                .body(setCountriesModel)
                .when()
                .put("/admin_new/configs/corporates/sets/COMPANY_REGISTRATION_COUNTRIES");
    }

    public static Response getConsumerSubscriptionStatus(final String token,
                                                         final String consumerId,
                                                         final Optional<String> optionalBody) {
        return getBodyAuthenticatedRequest(optionalBody.orElse(""), token)
                .when()
                .pathParam("consumer_id", consumerId)
                .post("/admin_new/consumers/kyc_logs/{consumer_id}/refresh_status");
    }

    public static Response getCorporateSubscriptionStatus(final String token,
                                                          final String corporateId,
                                                          final Optional<String> optionalBody) {
        return getBodyAuthenticatedRequest(optionalBody.orElse(""), token)
                .when()
                .pathParam("corporate_id", corporateId)
                .post("/admin_new/corporates/kyb_logs/{corporate_id}/refresh_status");
    }

    public static Response inviteUser(final String token,
                                      final InviteUserModel inviteUserModel) {
        return getBodyAuthenticatedRequest(inviteUserModel, token)
                .when()
                .post("/admin_new/gateway/invites");
    }

    public static Response consumeInvite(final String token,
                                        final AcceptInviteModel acceptInviteModel,
                                        final String inviteId) {

        return getBodyAuthenticatedRequest(acceptInviteModel, token)
                .when()
                .pathParam("invite_id", inviteId)
                .post("/admin_new/gateway/invites/{invite_id}/consume");
    }


    public static Response updateCorporateKyb(final UpdateKybModel updateKybModel,
                                              final String token,
                                              final String corporateId) {
        return getBodyAuthenticatedRequest(updateKybModel, token)
                .pathParam("id", corporateId)
                .when()
                .patch("/admin_new/corporates/{id}/kyb");
    }

    public static Response updateConsumerKyc(final UpdateKycModel updateKycModel,
                                     final String token,
                                     final String consumerId) {
        return getBodyAuthenticatedRequest(updateKycModel, token)
                .pathParam("id", consumerId)
                .when()
                .patch("/admin_new/consumers/{id}/kyc");
    }

    public static Response getConsumerKyc(final String token,
                                          final String consumerId) {

        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .when()
                .get("/admin_new/consumers/{id}/kyc");
    }

    public static Response getCorporateKyb(final String corporateId,
                                           final String token) {

        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .when()
                .get("/admin_new/corporates/{id}/kyb");
    }

    public static Response getOWTProfileId(final String token,
                                           final String programmeId,
                                           final String owtProfileId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", owtProfileId)
                .get("/admin_new/outgoing_wire_transfers/programmes/{programme_id}/profiles/{profile_id}");
    }

    public static Response createOWTProfileId(final String token,
                                              final OwtProgrammes owtProgrammes,
                                              final String programmeId) {
        return getBodyAuthenticatedRequest(owtProgrammes, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/admin_new/outgoing_wire_transfers/programmes/{programme_id}/profiles");
    }

    public static Response updateOWTProfileId(final String token,
                                              final OwtProgrammes owtProgrammes,
                                              final String programmeId,
                                              final String profileId) {
        return getBodyAuthenticatedRequest(owtProgrammes, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .patch("/admin_new/outgoing_wire_transfers/programmes/{programme_id}/profiles/{profile_id}");
    }

    public static Response startLostPassword(final LostPasswordStartModel lostPasswordStartModel){
        return getBodyRequest(lostPasswordStartModel)
                .when()
                .post("/admin_new/passwords/lost_password/start");
    }

    public static Response getConsumerProfile(final String adminToken,
                                              final String programmeId,
                                              final String profileId) {
        return getAuthenticatedRequest(adminToken)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .get("/admin_new/consumers/programmes/{programme_id}/profiles/{profile_id}");
    }

    public static Response updateEmailValidationProvider(final UpdateEmailValidationProviderModel model,
                                                         final String adminToken,
                                                         final String programmeId,
                                                         final String profileId) {
        return getBodyAuthenticatedRequest(model, adminToken)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .when()
                .patch("/admin_new/consumers/programmes/{programme_id}/profiles/{profile_id}");
    }

    public static Response getConsumerAllUsers(final String token,
                                               final String consumerId,
                                               final Optional<GetUsersFiltersModel> model) {

        return getBodyAuthenticatedRequest(model.orElse(GetUsersFiltersModel.builder().build()), token)
                .pathParam("consumerId", consumerId)
                .when()
                .post("/admin_new/consumers/{consumerId}/users/find");
    }

    public static Response getCorporateAllUsers(final String token,
                                                final String corporateId,
                                                final Optional<GetUsersFiltersModel> model) {

        return getBodyAuthenticatedRequest(model.orElse(GetUsersFiltersModel.builder().build()), token)
                .pathParam("corporateId", corporateId)
                .when()
                .post("/admin_new/corporates/{corporateId}/users/find");
    }

    public static Response updateProgramme(final UpdateAllowedCountriesModel updateAllowedCountriesModel,
                                           final String programmeId,
                                           final String token) {
        return getBodyAuthenticatedRequest(updateAllowedCountriesModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .patch("/admin_new/programmes/{programme_id}");
    }

    public static Response updateConsumerProfile(final UpdateConsumerProfileModel updateConsumerProfileModel,
                                                 final String token,
                                                 final String programme_id,
                                                 final String profile_id) {
        return getBodyAuthenticatedRequest(updateConsumerProfileModel, token)
                .pathParam("programme_id", programme_id)
                .pathParam("profile_id", profile_id)
                .when()
                .patch("/admin_new/consumers/programmes/{programme_id}/profiles/{profile_id}");
    }

    public static Response updateCorporateProfile(final UpdateCorporateProfileModel updateCorporateProfileModel,
                                                  final String token,
                                                  final String programme_id,
                                                  final String profile_id) {
        return getBodyAuthenticatedRequest(updateCorporateProfileModel, token)
                .pathParam("programme_id", programme_id)
                .pathParam("profile_id", profile_id)
                .when()
                .patch("/admin_new/corporates/programmes/{programme_id}/profiles/{profile_id}");
    }

    public static Response setSepaInstantLimit(final LimitsApiModel limitsApiModel, final String token) {
        return getBodyAuthenticatedRequest(limitsApiModel, token)
            .when()
            .put("/admin_new/limits/SEPA_INSTANT_DEPOSIT_LIMIT");
    }

    public static Response setSepaInstantIncomingWireTransferLimit(final LimitsApiModel limitsApiModel, final String token) {
        return getBodyAuthenticatedRequest(limitsApiModel, token)
            .when()
            .put("/admin_new/limits/SEPA_INSTANT_INCOMING_WIRE_TRANSFER_LIMIT");
    }

    public static Response settlementRetry(final SettlementRetryModel settlementRetryModel,
                                           final String token,
                                           final String settlementId) {
        return getBodyAuthenticatedRequest(settlementRetryModel, token)
                .pathParam("settlement_id", settlementId)
                .when()
                .post("/admin_new/managed_cards/settlements/{settlement_id}/retry");
    }

    public static Response getSettlement(final String token,
                                        final String settlementId) {
        // TODO Remove after simulator improvements
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return getAuthenticatedRequest(token)
                .pathParam("settlement_id", settlementId)
                .when()
                .get("/admin_new/managed_cards/settlements/{settlement_id}");
    }

    public static Response getPermissions (final String adminToken, String [] types, Integer offset, Integer limit){
        return getAuthenticatedRequest(adminToken)
                .param("types", types)
                .param("offset", offset)
                .param("limit", limit)
                .when()
                .get("/admin_new/access_control/permissions");
    }

    public static Response getRoleById (final String adminToken,
                                        final String roleId){
        return getAuthenticatedRequest(adminToken)
                .pathParam("role_id", roleId)
                .when()
                .get("/admin_new/access_control/roles/{role_id}");
    }

    public static Response deleteRole (final String adminToken,
                                       final String roleId){
        return getAuthenticatedRequest(adminToken)
                .pathParam("role_id", roleId)
                .when()
                .delete("/admin_new/access_control/roles/{role_id}");
    }

    public static Response updateRole (final UpdateRoleModel updateRoleModel,
                                       final String adminToken,
                                       final String roleId){
        return getBodyAuthenticatedRequest(updateRoleModel, adminToken)
                .pathParam("role_id", roleId)
                .when()
                .patch("/admin_new/access_control/roles/{role_id}");
    }

    public static Response getRoles (final String adminToken){
        return getAuthenticatedRequest(adminToken)
                .when()
                .get("/admin_new/access_control/roles");
    }

    public static Response getRoles(final String adminToken, final String name) {
        return getAuthenticatedRequest(adminToken)
                .queryParam("role_name", name)
                .when()
                .get("/admin_new/access_control/roles");
    }

    public static Response createRole (final CreateRoleModel createRoleModel,
                                       final String adminToken){
        return getBodyAuthenticatedRequest(createRoleModel, adminToken)
                .when()
                .post("/admin_new/access_control/roles");
    }

    public static Response editRolePermissions(final AddPermissionsToRoleModel addPermissionsToRoleModel,
                                               final String adminToken,
                                               final String roleId) {
        return getBodyAuthenticatedRequest(addPermissionsToRoleModel, adminToken)
            .pathParam("role_id", roleId)
            .put("/admin_new/access_control/roles/{role_id}/permissions");
    }

    public static Response addPermissionToRole(final String adminToken,
                                               final String roleId,
                                               final String permissionId) {
        final var request = AddPermissionsToRoleModel.builder().permissions(List.of(permissionId)).build();
        return editRolePermissions(request, adminToken, roleId);
    }

    public static Response deletePermissionFromRole(final String adminToken,
                                                    final String roleId,
                                                    final String permissionId) {
        return getAuthenticatedRequest(adminToken)
                .pathParam("role_id", roleId)
                .pathParam("permission_id", permissionId)
                .put("/admin_new/access_control/roles/{role_id}/permission/{permission_id}");
    }

    public static Response getAdminsAssignedToRole(final String adminToken,
                                                   final String roleId) {
        return getAuthenticatedRequest(adminToken)
                .when()
                .pathParam("role_id", roleId)
                .get("/admin_new/access_control/roles/{role_id}/assignees");
    }

    public static Response assignAdminsToRole(final AssignAdminsToRoleModel assignAdminsToRoleModel,
                                             final String adminToken,
                                             final String roleId) {
        return getBodyAuthenticatedRequest(assignAdminsToRoleModel, adminToken)
                .when()
                .pathParam("role_id", roleId)
                .post("/admin_new/access_control/roles/{role_id}/assignees");
    }

    public static Response deleteAdminFromRole(final String adminToken,
                                               final String roleId,
                                               final String adminId) {
        return getAuthenticatedRequest(adminToken)
                .when()
                .pathParam("role_id", roleId)
                .pathParam("admin_id", adminId)
                .delete("/admin_new/access_control/roles/{role_id}/assignees/{admin_id}");
    }

    public static Response getReviews(final String adminToken) {
        return getAuthenticatedRequest(adminToken)
                .when()
                .get("/admin_new/reviews");
    }

    public static Response getReviewsByCategory(final String adminToken,
                                                final String category) {
        return getAuthenticatedRequest(adminToken)
                .when()
                .pathParam("category", category)
                .get("/admin_new/reviews/{category}");
    }

    public static Response getReview(final String adminToken,
                                     final String category,
                                     final String reviewId) {
        return getAuthenticatedRequest(adminToken)
                .when()
                .pathParam("category", category)
                .pathParam("review_id", reviewId)
                .get("/admin_new/reviews/{category}/{review_id}");
    }

    public static Response reviewDecision(final ReviewDecisionModel reviewDecisionModel,
                                          final String adminToken,
                                          final String category,
                                          final String reviewId) {
        return getBodyAuthenticatedRequest(reviewDecisionModel, adminToken)
                .when()
                .pathParam("category", category)
                .pathParam("review_id", reviewId)
                .post("/admin_new/reviews/{category}/{review_id}/decision");
    }

    public static Response setContextPropertyV2(final ContextPropertiesV2Model model,
                                                final String token,
                                                final String propertyName){
        return getBodyAuthenticatedRequest(model, token)
                .pathParam("propertyName", propertyName)
                .when()
                .post("/admin_new/properties/{propertyName}");
    }

    public static Response getContextPropertyV2(final List<Map<String, String>> model,
                                                final String token,
                                                final String propertyName){
        return getBodyAuthenticatedRequest(model, token)
                .pathParam("propertyName", propertyName)
                .when()
                .post("/admin_new/properties/{propertyName}/exact");
    }

    public static Response getInnovators(final GetInnovatorsModel model,
                                         final String token){
        return getBodyAuthenticatedRequest(model, token)
                .when()
                .post("/admin_new/innovator/innovators/find");
    }

    public static Response getProgrammes(final PagingLimitModel model,
                                         final String token){
        return getBodyAuthenticatedRequest(model, token)
                .when()
                .post("/admin_new/programmes/find");
    }

    public static Response getModels(final PagingLimitModel model,
                                     final String token){
        return getBodyAuthenticatedRequest(model, token)
                .when()
                .post("/admin_new/programmes/models/find");
    }

    public static Response getModel(final String token,
                                    final String modelId){
        return getAuthenticatedRequest(token)
                .pathParam("modelId", modelId)
                .when()
                .get("/admin_new/programmes/models/{modelId}");
    }

    public static Response setEddCountries(final String token,
                                           final IdentityType identityType,
                                           final ContextPropertiesV2Model setEddCountriesModel){
        return getBodyAuthenticatedRequest(setEddCountriesModel, token)
                .when()
                .post(String.format("/admin_new/properties/%s_EDD_COUNTRIES", identityType.name()));
    }

    public static Response deleteEddCountries(final String token,
                                              final IdentityType identityType,
                                              final DeletePropertiesModel deletePropertiesModel){
        return getBodyAuthenticatedRequest(deletePropertiesModel, token)
                .when()
                .delete(String.format("/admin_new/properties/%s_EDD_COUNTRIES", identityType.name()));
    }

    public static String loginChecker() {
        return getBodyRequest(new LoginModel("admin_checker@weavr.io", new PasswordModel("Pass1234!")))
                .when()
                .post("/admin/api/gateway/login")
                .jsonPath()
                .get("token");
    }

    public static Response getReports(final GetReportsModel getReportsModel,
                                      final String token) {
        return getBodyAuthenticatedRequest(getReportsModel, token)
                .when()
                .post("/admin_new/innovator/reports/find");
    }
}
