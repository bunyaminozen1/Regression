package opc.junit.helpers.adminnew;

import commons.enums.Currency;
import io.restassured.response.Response;
import opc.enums.opc.IdentityType;
import opc.enums.opc.PermissionType;
import opc.enums.opc.RetryType;
import opc.enums.opc.ServiceType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.models.admin.AcceptInviteModel;
import opc.models.admin.AssignAdminsToRoleModel;
import opc.models.admin.AuthorisationsRequestModel;
import opc.models.admin.ContextPropertiesV2Model;
import opc.models.admin.CreateRoleModel;
import opc.models.admin.CreateServiceTypeModel;
import opc.models.admin.CreateThirdPartyRegistryModel;
import opc.models.admin.DeletePropertiesModel;
import opc.models.admin.GetPermissionListResponseModel;
import opc.models.admin.InviteUserModel;
import opc.models.admin.LimitsApiContextWithCurrencyModel;
import opc.models.admin.LimitsApiLowValueExemptionModel;
import opc.models.admin.LimitsApiLowValueModel;
import opc.models.admin.PagingLimitModel;
import opc.models.admin.PermissionsResponseBody;
import opc.models.admin.RejectDepositModel;
import opc.models.admin.ReviewDecisionModel;
import opc.models.admin.SettlementRetryModel;
import opc.models.admin.SubscriptionStatusPayneticsModel;
import opc.models.admin.UpdateKybModel;
import opc.models.admin.UpdateKycModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.adminnew.AdminRoleBasedAccessService;
import opc.services.adminnew.AdminService;
import opc.services.adminnew.ThirdPartyRegistryService;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class AdminHelper {

    public static String login() {
        return TestHelper.ensureAsExpected(15,
                        AdminService::login,
                        SC_OK)
                .jsonPath()
                .get("token");
    }

    public static String impersonateTenant(final String tenantId) {
        return TestHelper.ensureAsExpected(15,
                        () -> AdminService.impersonateTenant(tenantId, login()),
                        SC_OK)
                .jsonPath()
                .get("token");
    }

    public static Response deactivateCorporate(final DeactivateIdentityModel deactivateIdentityModel,
                                               final String corporateId,
                                               final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.deactivateCorporate(deactivateIdentityModel, corporateId, token), SC_NO_CONTENT);
    }

    public static Response deactivateConsumer(final DeactivateIdentityModel deactivateIdentityModel,
                                              final String consumerId,
                                              final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.deactivateConsumer(deactivateIdentityModel, consumerId, token), SC_NO_CONTENT);
    }

    public static Response deactivateConsumerUser(final String consumerId,
                                                  final String userId,
                                                  final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.deactivateConsumerUser(consumerId, userId, token), SC_NO_CONTENT);
    }

    public static Response deactivateCorporateUser(final String corporateId,
                                                   final String userId,
                                                   final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.deactivateCorporateUser(corporateId, userId, token), SC_NO_CONTENT);
    }

    public static void setConsumerLowValueLimit(final String token,
                                                final String programmeId,
                                                final String currency) {

        final LimitsApiLowValueExemptionModel limitsApiModel =
                LimitsApiLowValueExemptionModel.builder()
                        .setContext(LimitsApiContextWithCurrencyModel.builder()
                                .setProgrammeIdContext(programmeId)
                                .setCurrencyContext(currency)
                                .build())
                        .setValue(LimitsApiLowValueModel.builder()
                                .setMaxCount(5)
                                .setMaxSum(100)
                                .setMaxAmount(100000)
                                .build())
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setConsumerLimits(token, limitsApiModel),
                SC_NO_CONTENT);
    }

    public static void setCorporateLowValueLimit(final String token,
                                                 final String programmeId,
                                                 final String currency) {

        final LimitsApiLowValueExemptionModel limitsApiModel =
                LimitsApiLowValueExemptionModel.builder()
                        .setContext(LimitsApiContextWithCurrencyModel.builder()
                                .setProgrammeIdContext(programmeId)
                                .setCurrencyContext(currency)
                                .build())
                        .setValue(LimitsApiLowValueModel.builder()
                                .setMaxCount(5)
                                .setMaxSum(100)
                                .setMaxAmount(100000)
                                .build())
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setCorporateLimits(token, limitsApiModel),
                SC_NO_CONTENT);
    }

    public static void setCorporateLowValueLimitWithCurrency(final String programmeId,
                                                             final Currency currency,
                                                             final String token) {

        final LimitsApiLowValueExemptionModel limitsApiLowValueExemptionModel =
                LimitsApiLowValueExemptionModel.builder()
                        .setContext(LimitsApiContextWithCurrencyModel.builder().setProgrammeIdContext(programmeId).setCurrencyContext(String.valueOf(currency)).build())
                        .setValue(LimitsApiLowValueModel.builder()
                                .setMaxCount(5)
                                .setMaxSum(100)
                                .setMaxAmount(30)
                                .build())
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setCorporateLimits(token, limitsApiLowValueExemptionModel),
                SC_OK);
    }

    public static void setConsumerLowValueLimitWithCurrency(final String programmeId,
                                                            final Currency currency,
                                                            final String token) {

        final LimitsApiLowValueExemptionModel limitsApiLowValueExemptionModel =
                LimitsApiLowValueExemptionModel.builder()
                        .setContext(LimitsApiContextWithCurrencyModel.builder().setProgrammeIdContext(programmeId).setCurrencyContext(String.valueOf(currency)).build())
                        .setValue(LimitsApiLowValueModel.builder()
                                .setMaxCount(5)
                                .setMaxSum(100)
                                .setMaxAmount(30)
                                .build())
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setConsumerLimits(token, limitsApiLowValueExemptionModel),
                SC_OK);
    }

    public static String getProviderLinkId(final AuthorisationsRequestModel authorisationsRequestModel,
                                           final String token) {
        return TestHelper.ensureAsExpected(30,
                        () -> AdminService.authorisationExpiryFind(authorisationsRequestModel, token), SC_OK)
                .jsonPath()
                .get("systemExpiredAuthorisationResponseEntries[0].providerLinkId");
    }

    public static Response createThirdPartyRegistry() {
        final CreateThirdPartyRegistryModel createThirdPartyRegistryModel = CreateThirdPartyRegistryModel.DefaultCreateThirdPartyRegistryModel();

        return TestHelper.ensureAsExpected(15,
                () -> ThirdPartyRegistryService.createThirdPartyRegistry(createThirdPartyRegistryModel, AdminService.loginAdmin()), SC_CREATED);
    }

    public static Response createThirdPartyRegistry(final CreateThirdPartyRegistryModel createThirdPartyRegistryModel) {
        return TestHelper.ensureAsExpected(15,
                () -> ThirdPartyRegistryService.createThirdPartyRegistry(createThirdPartyRegistryModel, AdminService.loginAdmin()), SC_CREATED);
    }

    public static Response createServiceType(final String providerKey, final ServiceType serviceType) {
        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType)
                .build();

        return TestHelper.ensureAsExpected(15,
                () -> ThirdPartyRegistryService.createServiceType(createServiceTypeModel, AdminService.loginAdmin(), providerKey), SC_CREATED);
    }

    public static Response createThirdPartyRegistryWithService() {
        final CreateThirdPartyRegistryModel createThirdPartyRegistryModel = CreateThirdPartyRegistryModel.DefaultCreateThirdPartyRegistryModel();

        final String providerKey = ThirdPartyRegistryService.createThirdPartyRegistry(createThirdPartyRegistryModel, AdminService.loginAdmin())
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.getRandomServiceType())
                .build();

        return TestHelper.ensureAsExpected(15,
                () -> ThirdPartyRegistryService.createServiceType(createServiceTypeModel, AdminService.loginAdmin(), providerKey), SC_CREATED);
    }

    public static Long createRole(final CreateRoleModel roleModel,
                                  final String token) {
        return Long.valueOf(TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.createRole(roleModel, token), SC_CREATED).jsonPath().getString("reviewId"));
    }

    public static Response createRoleResponse(final CreateRoleModel roleModel,
                                              final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.createRole(roleModel, token), SC_CREATED);
    }

    public static String updateRole(final CreateRoleModel roleModel,
                                    final String token,
                                    final Long roleId) {
        return TestHelper.ensureAsExpected(15,
                        () -> AdminRoleBasedAccessService.updateRole(roleModel, token, roleId), SC_OK)
                .jsonPath()
                .get("resourceId");
    }

    public static Response deleteRole(final Long roleId,
                                      final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.deleteRole(roleId, token), SC_OK);
    }

    public static Response getRoleById(final Long roleId,
                                       final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.getRoleById(token, roleId), SC_OK);
    }

    public static Response getAllRoles(final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.getAllRoles(token), SC_OK);
    }

    public static Response unassignRole(final String token,
                                        final Long adminId,
                                        final Long roleId) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.unassignRole(token, adminId, roleId), SC_OK);
    }

    public static Response addPermissionToRole(final String token,
                                               final Long permission,
                                               final Long roleId) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.addPermissionToRole(roleId, permission, token), SC_OK);
    }

    public static Response deletePermissionToRole(final String token,
                                                  final Long permission,
                                                  final Long roleId) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.deletePermissionToRole(roleId, permission, token), SC_OK);
    }

    public static Response retrieveAllRoles(final String token,
                                            final Long adminId) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.retrieveAllRoles(token, adminId), SC_OK);
    }

    public static Response inviteUser(final String token,
                                      final InviteUserModel inviteUserModel) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.inviteUser(token, inviteUserModel), SC_NO_CONTENT);
    }

    public static Response forceBootstrap(final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.forceBootstrap(token), SC_NO_CONTENT);
    }

    public static String getRoleManagerId(final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> AdminRoleBasedAccessService.getAllRoles(token), SC_OK)
                .path("roles.findAll{role -> role.name=='Role manager'}.id").toString();
    }

    public static Response acceptInvite(final AcceptInviteModel acceptInviteModel,
                                        final String inviteId) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminRoleBasedAccessService.acceptInvite(acceptInviteModel, inviteId), SC_NO_CONTENT);
    }

    public static Long getAdminId(final String token, final String email) {
        return Long.valueOf(TestHelper.ensureAsExpected(15,
                        () -> AdminRoleBasedAccessService.getAllAdmins(token), SC_OK)
                .path("adminDetails.findAll{adminDetail -> adminDetail.email=='" + email + "'}.id").toString().replace("[", "").replace("]", ""));
    }

    public static void updateConsumerKyc(final UpdateKycModel updateKycModel,
                                         final String consumerId,
                                         final String adminToken) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.updateConsumerKyc(updateKycModel, consumerId, adminToken),
                SC_OK);
    }

    public static void updateCorporateKyb(final UpdateKybModel updateKybModel,
                                          final String corporateId,
                                          final String adminToken) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.updateCorporateKyb(updateKybModel, corporateId, adminToken),
                SC_OK);
    }

    public static SubscriptionStatusPayneticsModel getConsumerSubscriptionStatus(final String identityId,
                                                                                 final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> AdminService.getConsumerSubscriptionStatus(token, identityId, Optional.of("{}")), SC_OK)
                .then()
                .extract()
                .as(SubscriptionStatusPayneticsModel.class);
    }

    public static SubscriptionStatusPayneticsModel getCorporateSubscriptionStatus(final String identityId,
                                                                                  final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> AdminService.getCorporateSubscriptionStatus(token, identityId, Optional.of("{}")), SC_OK)
                .then()
                .extract()
                .as(SubscriptionStatusPayneticsModel.class);
    }

    public static void rejectDeposit(final String token,
                                     final String depositId) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.rejectDeposit(RejectDepositModel.builder().note("note").build(), depositId, token), SC_NO_CONTENT);
    }

    public static void retrySettlement(final RetryType retryType, final String innovatorToken, final String settlementId) {
        // TODO Remove after simulator improvements
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TestHelper.ensureAsExpected(30,
                () -> AdminService.settlementRetry(SettlementRetryModel.DefaultSettlementRetryModel(retryType, "Retry."), innovatorToken, settlementId),
                SC_NO_CONTENT);
    }

    public static Response getPermissions(final String adminToken) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getPermissions(adminToken, new String[]{PermissionType.SYSTEM.toString(), PermissionType.PLATFORM.toString(), PermissionType.FIN_PLUGIN.toString()}, 0, 100), SC_OK);
    }

    public static Response createRole(final String adminToken) {
        final CreateRoleModel createRoleModel = CreateRoleModel.DefaultCreateRoleModel();

        return AdminService.createRole(createRoleModel, adminToken);

    }

    public static String createRole(final String adminToken,
                                    final String adminCheckerToken, final String... permissions) {
        final Response roleResponse = createRoleResponse(CreateRoleModel.DefaultCreateRoleModel(permissions), adminToken);

        final String reviewId = roleResponse.jsonPath().get("reviewId");
        final String category = roleResponse.jsonPath().get("category");

        return approveReview(adminCheckerToken, category, reviewId);
    }

    public static Response assignAdminsToRole(final String adminToken, final String roleId, final String... adminId) {

        AssignAdminsToRoleModel assignAdminsToRoleModel = AssignAdminsToRoleModel.CreateAssignAdminsToRoleModelBuilder(adminId);

        return TestHelper.ensureAsExpected(15,
                () -> AdminService.assignAdminsToRole(assignAdminsToRoleModel, adminToken, roleId), SC_CREATED);
    }

    public static String approveRejectReview(final ReviewDecisionModel reviewDecisionModel,
                                             final String adminToken,
                                             final String category,
                                             final String reviewId) {
        return TestHelper.ensureAsExpected(15,
                        () -> AdminService.reviewDecision(reviewDecisionModel, adminToken, category, reviewId), SC_OK)
                .jsonPath().get("resourceId");
    }

    public static String approveReview(final String adminToken,
                                       final String category,
                                       final String reviewId) {
        return approveRejectReview(new ReviewDecisionModel("APPROVE"), adminToken, category, reviewId);
    }

    public static String rejectReview(final String adminToken,
                                       final String category,
                                       final String reviewId) {
        return approveRejectReview(new ReviewDecisionModel("REJECT"), adminToken, category, reviewId);
    }

    public static String getAdminEmail(final String token, final String id) {
        return TestHelper.ensureAsExpected(15,
                        () -> AdminRoleBasedAccessService.getAllAdmins(token), SC_OK)
                .path("adminDetails.findAll{adminDetail -> adminDetail.id=='" + id + "'}.email").toString().replace("[", "").replace("]", "");
    }

    public static String retrievePermissionId(final String adminToken) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getPermissions(adminToken, new String[] {PermissionType.getRandomPermissionType().toString()}, 0, 10), SC_OK).jsonPath().get("permissions[0].permissionId");
    }

    public static Response getRoleById(final String adminToken,
                                       final String roleId) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getRoleById(adminToken, roleId), SC_OK);
    }

    public static String getModelIdProgramme (final String adminToken) {

        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getProgrammes(new PagingLimitModel().setLimit(10), adminToken), SC_OK)
                .jsonPath()
                .get("programme[0].modelId");
    }

    public static Response setEddCountriesProperty(final String adminToken,
                                                   final String tenantId,
                                                   final String eddCountry,
                                                   final IdentityType identityType) {

        final ContextPropertiesV2Model setEddCountriesModel = ContextPropertiesV2Model.setEddCountriesProperty(tenantId, eddCountry);


        return TestHelper.ensureAsExpected(15,
                () -> AdminService.setEddCountries(adminToken, identityType, setEddCountriesModel), SC_NO_CONTENT);
    }

    public static Response deleteEddCountriesProperty(final String adminToken,
                                                      final String tenantId,
                                                      final IdentityType identityType) {

        final DeletePropertiesModel deletePropertiesModel = DeletePropertiesModel.builder().additionalProperty(tenantId).build();


        return TestHelper.ensureAsExpected(15,
                () -> AdminService.deleteEddCountries(adminToken, identityType, deletePropertiesModel), SC_NO_CONTENT);
    }

    public static String getPermissionId(final String adminToken, final PermissionType permissionType, final String permissionName) {
        final var permissions = AdminService.getPermissions(adminToken, new String[]{permissionType.toString()}, 0, 100)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetPermissionListResponseModel.class);

        return permissions.getPermissions().stream()
                .filter(permission -> permission.getPermissionName().equals(permissionName))
                .map(PermissionsResponseBody::getPermissionId)
                .findFirst().orElseThrow();
    }

    public static String createAdminWithRole(final String token, final String password, final long roleId) {
        final var adminInvite = InviteUserModel.DefaultCreateInviteUserModel().setRoleId(roleId).build();
        final var adminEmail = adminInvite.getEmail();

        AdminHelper.inviteUser(token, adminInvite);

        final var inviteEmail = MailhogHelper.getUserInviteNonceAndInviteId(adminEmail);
        final var nonce = inviteEmail.getLeft();
        final var inviteId = inviteEmail.getRight();

        final var acceptInvite = new AcceptInviteModel(nonce, adminEmail, new PasswordModel(password));
        AdminHelper.acceptInvite(acceptInvite, inviteId);

        return AdminService.loginAdmin(new LoginModel(adminEmail, new PasswordModel(password)));
    }
}
