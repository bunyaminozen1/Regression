package opc.junit.adminnew.rolebasedaccesscontrol;

import io.restassured.response.Response;
import opc.enums.opc.Permission;
import opc.enums.opc.PermissionType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.adminnew.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.models.admin.AcceptInviteModel;
import opc.models.admin.AssignAdminsToRoleModel;
import opc.models.admin.CreateRoleModel;
import opc.models.admin.GetPermissionListResponseModel;
import opc.models.admin.GetRolesResponseModel;
import opc.models.admin.InviteUserModel;
import opc.models.admin.MakerCheckerResponseModel;
import opc.models.admin.ReviewDecisionModel;
import opc.models.admin.ReviewModel;
import opc.models.admin.ReviewResponseModel;
import opc.models.admin.RoleResponseModel;
import opc.models.admin.UpdateRoleModel;
import opc.models.innovator.CreateProgrammeModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.adminnew.AdminRoleBasedAccessService;
import opc.services.adminnew.AdminService;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static opc.junit.helpers.adminnew.AdminHelper.createRole;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoleBasedAccessControlTests extends BaseRoleBasedAccessControlSetup {
    private static String adminCheckerToken;

    @BeforeAll
    public static void BeforeAll() {
        adminCheckerToken = opc.services.admin.AdminService.loginNonRootAdmin("admin_checker@weavr.io", TestHelper.DEFAULT_INNOVATOR_PASSWORD);
    }

    @ParameterizedTest()
    @EnumSource(value = PermissionType.class, mode = EnumSource.Mode.EXCLUDE, names = {"UNKNOWN"})
    public void GetPermissions_HappyPath_Success(final PermissionType permissionType) {
        AdminService.getPermissions(adminToken, new String[] {permissionType.toString()}, 0, 10)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetPermissionListResponseModel.class);
    }

    @Test
    public void GetPermissions_AdminTokenMissing_Unauthorized() {
        AdminService.getPermissions(null, new String[] {PermissionType.getRandomPermissionType().toString()}, 0, 10)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateRole_HappyPathApprove_Success() {
        final CreateRoleModel roleModel = CreateRoleModel.DefaultCreateRoleModel();

        final MakerCheckerResponseModel updateRoleResponse = AdminService.createRole(roleModel, adminToken)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .as(MakerCheckerResponseModel.class);

        assertEquals(updateRoleResponse.getAction(), "CREATE");
        assertEquals(updateRoleResponse.getCategory(), "access_control");
        assertEquals(updateRoleResponse.getResourceType(), "role");
        assertEquals(updateRoleResponse.getStatus(), "PENDING");

        final String roleId = AdminHelper.approveReview(adminCheckerToken, updateRoleResponse.getCategory(), updateRoleResponse.getReviewId());

        AdminHelper.getRoleById(adminToken, roleId)
                .then()
                .statusCode(SC_OK)
                .body("name", equalTo(roleModel.getName()))
                .body("description", equalTo(roleModel.getDescription()));
    }

    @Test
    public void CreateRole_HappyPathReject_Success() {
        final CreateRoleModel roleModel = CreateRoleModel.DefaultCreateRoleModel();

        final MakerCheckerResponseModel updateRoleResponse = AdminService.createRole(roleModel, adminToken)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .as(MakerCheckerResponseModel.class);

        assertEquals(updateRoleResponse.getAction(), "CREATE");
        assertEquals(updateRoleResponse.getCategory(), "access_control");
        assertEquals(updateRoleResponse.getResourceType(), "role");
        assertEquals(updateRoleResponse.getStatus(), "PENDING");

        final String roleId = AdminHelper.rejectReview(adminCheckerToken, updateRoleResponse.getCategory(), updateRoleResponse.getReviewId());

        assertEquals(roleId, "0");
    }

    @Test
    public void CreateRole_AuthTokenMissing_Unauthorized() {
        final CreateRoleModel roleModel = CreateRoleModel.DefaultCreateRoleModel();
        AdminService.createRole(roleModel, null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateRole_SameRole_Conflict() {
        final CreateRoleModel createRoleModel = CreateRoleModel.DefaultCreateRoleModel();
        final Response roleResponse = AdminHelper.createRoleResponse(createRoleModel, adminToken);

        final String reviewId = roleResponse.jsonPath().get("reviewId");
        final String category = roleResponse.jsonPath().get("category");

        AdminHelper.approveReview(adminCheckerToken, category, reviewId);

        AdminService.createRole(createRoleModel, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROLE_NAME_ALREADY_EXISTS"));
    }

    @Test
    public void CreateRole_AdminWithoutPermission_Forbidden() {
        InviteUserModel inviteUserModel = InviteUserModel.DefaultCreateInviteUserModel().build();

        AdminHelper.inviteUser(adminToken, inviteUserModel);

        final Pair<String, String> response = MailhogHelper.getUserInviteNonceAndInviteId(inviteUserModel.getEmail());
        final String nonce = response.getLeft();
        final String inviteId = response.getRight();

        AcceptInviteModel acceptInviteModel = new AcceptInviteModel(nonce, inviteUserModel.getEmail(), new PasswordModel("Password1234!"));

        AdminHelper.acceptInvite(acceptInviteModel, inviteId);

        final String token = AdminService.loginAdmin(new LoginModel(inviteUserModel.getEmail(), new PasswordModel("Password1234!")));

        AdminService.createRole(CreateRoleModel.DefaultCreateRoleModel(), token)
                .then()
                .statusCode(SC_FORBIDDEN);

        final String roleId = AdminHelper.getRoleManagerId(adminToken).replace("[", "").replace("]", "");
        final String adminId = String.valueOf(AdminHelper.getAdminId(adminToken, inviteUserModel.getEmail()));

        final ReviewModel assignAdminResponse = AdminHelper.assignAdminsToRole(adminToken, roleId, adminId)
                .then()
                .statusCode(SC_CREATED)
                .extract().as(ReviewModel.class);

        AdminHelper.approveReview(adminCheckerToken, assignAdminResponse.getCategory(), assignAdminResponse.getReviewId());

        AdminService.createRole(CreateRoleModel.DefaultCreateRoleModel(), token)
                .then()
                .statusCode(SC_CREATED);

        final ReviewModel unassignAdminResponse = AdminService.deleteAdminFromRole(adminToken, roleId, adminId)
                .then()
                .statusCode(SC_CREATED)
                .extract().as(ReviewModel.class);

        AdminHelper.approveReview(adminCheckerToken, unassignAdminResponse.getCategory(), unassignAdminResponse.getReviewId());

        AdminService.createRole(CreateRoleModel.DefaultCreateRoleModel(), token)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetRoles_HappyPath_Success() {
        AdminHelper.createRole(adminToken, adminCheckerToken);

        AdminService.getRoles(adminToken)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetRolesResponseModel.class);
    }

    @Test
    public void GetRoles_AuthTokenMissing_Unauthorized() {
        AdminService.getRoles(null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    //check if admin has permissions





    /////////////////////////////////////get role by id

    @Test
    public void GetRoleById_HappyPath_Success() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        AdminService.getRoleById(adminToken, roleId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(RoleResponseModel.class);
    }

    @Test
    public void GetRoleById_AuthTokenMissing_Unauthorized() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        AdminService.getRoleById(null, roleId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetRoleById_RoleNonExisting_NotFound() {
        AdminService.getRoleById(adminToken, RandomStringUtils.randomNumeric(8))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    //check if admin has permissions




    /////////////////////////////////////////delete role - no review needed

    @Test
    public void DeleteRole_HappyPath_Success() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        AdminService.deleteRole(adminToken, roleId)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService.getRoleById(adminToken, roleId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeleteRole_AuthTokenMissing_Unauthorized() {
        final String roleId = createRole(adminToken, adminCheckerToken);
        AdminService.deleteRole(null, roleId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeleteRole_RoleNonExisting_NotFound() {
        AdminService.deleteRole(adminToken, RandomStringUtils.randomNumeric(8))
                .then()
                .statusCode(SC_NOT_FOUND);
    }


    /// check if admin has permissions

    ///////////////////////////////////////////edit role

    @Test
    public void UpdateRole_HappyPathApprove_Success() {
        final String roleId = AdminHelper.createRole(adminToken, adminCheckerToken);

        final String name = RandomStringUtils.randomAlphabetic(8);
        final String description = RandomStringUtils.randomAlphabetic(8);

        UpdateRoleModel updateRoleModel = UpdateRoleModel.builder().build();
        updateRoleModel.setName(name);
        updateRoleModel.setDescription(description);

        final MakerCheckerResponseModel updateRoleResponse = AdminService.updateRole(updateRoleModel, adminToken, roleId)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .as(MakerCheckerResponseModel.class);

        assertEquals(updateRoleResponse.getAction(), "UPDATE");
        assertEquals(updateRoleResponse.getCategory(), "access_control");
        assertEquals(updateRoleResponse.getResourceType(), "role");
        assertEquals(updateRoleResponse.getStatus(), "PENDING");

        AdminHelper.approveReview(adminCheckerToken, updateRoleResponse.getCategory(), updateRoleResponse.getReviewId());

        AdminHelper.getRoleById(adminToken, roleId)
                .then()
                .statusCode(SC_OK)
                .body("name", equalTo(name))
                .body("description", equalTo(description));
    }

    //bug - when update role is edited to a name that exists, error is not user-friendly

    @Test
    public void UpdateRole_HappyPathReject_Success() {
        final String roleId = AdminHelper.createRole(adminToken, adminCheckerToken);

        final String name = RandomStringUtils.randomAlphabetic(8);
        final String description = RandomStringUtils.randomAlphabetic(8);

        UpdateRoleModel updateRoleModel = UpdateRoleModel.builder().build();
        updateRoleModel.setName(name);
        updateRoleModel.setDescription(description);

        final MakerCheckerResponseModel updateRoleResponse = AdminService.updateRole(updateRoleModel, adminToken, roleId)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .as(MakerCheckerResponseModel.class);

        assertEquals(updateRoleResponse.getAction(), "UPDATE");
        assertEquals(updateRoleResponse.getCategory(), "access_control");
        assertEquals(updateRoleResponse.getResourceType(), "role");
        assertEquals(updateRoleResponse.getStatus(), "PENDING");

        AdminHelper.rejectReview(adminCheckerToken, updateRoleResponse.getCategory(), updateRoleResponse.getReviewId());

        AdminHelper.getRoleById(adminToken, roleId)
                .then()
                .statusCode(SC_OK)
                .body("name", not(equalTo(name)))
                .body("description", not(equalTo(description)));
    }

    @Test
    public void UpdateRole_AuthTokenMissing_Unauthorized() {
        final String resourceId = AdminHelper.createRole(adminToken, adminCheckerToken);

        UpdateRoleModel updateRoleModel = UpdateRoleModel.builder().build();
        updateRoleModel.setName("TestName");
        updateRoleModel.setDescription("TestDescription");

        AdminService.updateRole(updateRoleModel, null, resourceId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateRole_RoleNonExisting_NotFound() {
        UpdateRoleModel updateRoleModel = UpdateRoleModel.builder().build();
        updateRoleModel.setName("TestName");
        updateRoleModel.setDescription("TestDescription");

        AdminService.updateRole(updateRoleModel, adminToken, RandomStringUtils.randomNumeric(8))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROLE_NOT_FOUND"));
    }

    @ParameterizedTest
    @EnumSource(value = Permission.class)
    public void AddPermissionToRole_HappyPath_Success(Permission permission) {
        if (!permission.getPermissionType().equals("SYSTEM")) {
            return;
        }

        final String roleId = createRole(adminToken, adminCheckerToken);

        final GetPermissionListResponseModel responseBody = AdminHelper.getPermissions(adminToken)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetPermissionListResponseModel.class);

        final String permissionId = responseBody.getPermissions().stream()
                .filter(permissionList -> permissionList.getPermissionName().equals(permission.getPermission())).findFirst().orElseThrow().getPermissionId();

        final var reviewResponse = AdminService.addPermissionToRole(adminToken, roleId, permissionId)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .as(ReviewModel.class);

        AdminHelper.approveReview(adminCheckerToken, reviewResponse.getCategory(), reviewResponse.getReviewId());

        AdminHelper.getRoleById(adminToken, roleId)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(roleId))
                .body("permissions.permissionId[0]", equalTo(permissionId));
    }

    @Test
    public void AddPermissionToRole_AdminTokenMissing_UnAuthorized() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        final String permissionId = AdminHelper.getPermissions(adminToken).jsonPath().get("permissions[0].permissionId");

        AdminService.addPermissionToRole(null, roleId, permissionId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void AddPermissionToRole_UnknownPermission_Conflict() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        AdminService.addPermissionToRole(adminToken, roleId, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_CONFLICT);

    }

    @Test
    public void AddPermissionToRole_UnknownRole_Conflict() {
        final String permissionId = AdminHelper.getPermissions(adminToken).jsonPath().get("permissions[0].permissionId");

        AdminService.addPermissionToRole(adminToken, RandomStringUtils.randomNumeric(18), permissionId)
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void UnAssignPermissionFromRole_RoleUnAssigned_Success() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        AssignAdminsToRoleModel assignRole = AssignAdminsToRoleModel.CreateAssignAdminsToRoleModelBuilder("1");
        final var assignmentReview = AdminRoleBasedAccessService.assignRole(assignRole, adminToken, roleId)
                .then()
                .statusCode(SC_CREATED).extract().as(ReviewModel.class);

        AdminHelper.approveReview(adminCheckerToken, assignmentReview.getCategory(), assignmentReview.getReviewId());

        final var unassignmentReview = AdminRoleBasedAccessService.unassignRole(adminToken, 1L, Long.valueOf(roleId))
                .then()
                .statusCode(SC_CREATED).extract().as(ReviewModel.class);

        AdminHelper.approveReview(adminCheckerToken, unassignmentReview.getCategory(), unassignmentReview.getReviewId());
    }

    @Test
    public void AddAssigneeToRole_HappyPathApprove_Success() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        final AssignAdminsToRoleModel assignAdminsToRoleModel = AssignAdminsToRoleModel.CreateAssignAdminsToRoleModelBuilder("1");

        final MakerCheckerResponseModel addAssigneeToRoleResponse = AdminService.assignAdminsToRole(assignAdminsToRoleModel, adminToken, roleId)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .as(MakerCheckerResponseModel.class);

        assertEquals(addAssigneeToRoleResponse.getAction(), "UPDATE");
        assertEquals(addAssigneeToRoleResponse.getCategory(), "access_control");
        assertEquals(addAssigneeToRoleResponse.getResourceType(), "role");
        assertEquals(addAssigneeToRoleResponse.getStatus(), "PENDING");

        AdminHelper.approveReview(adminCheckerToken, addAssigneeToRoleResponse.getCategory(), addAssigneeToRoleResponse.getReviewId());

        AdminService.getAdminsAssignedToRole(adminToken, roleId)
                .then()
                .statusCode(SC_OK)
                .body("admins.id[0]", equalTo("1"));
    }

    @Test
    public void AddAssigneeToRole_HappyPathReject_Success() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        final AssignAdminsToRoleModel assignAdminsToRoleModel = AssignAdminsToRoleModel.CreateAssignAdminsToRoleModelBuilder("1");

        final MakerCheckerResponseModel addAssigneeToRoleResponse = AdminService.assignAdminsToRole(assignAdminsToRoleModel, adminToken, roleId)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .as(MakerCheckerResponseModel.class);

        assertEquals(addAssigneeToRoleResponse.getAction(), "UPDATE");
        assertEquals(addAssigneeToRoleResponse.getCategory(), "access_control");
        assertEquals(addAssigneeToRoleResponse.getResourceType(), "role");
        assertEquals(addAssigneeToRoleResponse.getStatus(), "PENDING");

        AdminHelper.rejectReview(adminCheckerToken, addAssigneeToRoleResponse.getCategory(), addAssigneeToRoleResponse.getReviewId());

        AdminService.getAdminsAssignedToRole(adminToken, roleId)
                .then()
                .statusCode(SC_OK)
                .body("admins.id[0]", equalTo(null));
    }

    @Test
    public void AddAssigneeToRole_AdminTokenMissing_Unauthorized() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        final AssignAdminsToRoleModel as = AssignAdminsToRoleModel.CreateAssignAdminsToRoleModelBuilder("1");

        AdminService.assignAdminsToRole(as, null, roleId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void AddAssigneeToRole_AddAdminTwice_Success() {
        final String roleId = createRole(adminToken, adminCheckerToken);

        final Response response = AdminHelper.assignAdminsToRole(adminToken, roleId, "1");

        AdminHelper.approveReview(adminCheckerToken, response.jsonPath().get("category"), response.jsonPath().get("reviewId"));

        Response response2 = AdminHelper.assignAdminsToRole(adminToken, roleId, "1", "1");

        AdminService.reviewDecision(new ReviewDecisionModel("APPROVE"), adminCheckerToken, response2.jsonPath().get("category"), response2.jsonPath().get("reviewId"))
                .then()
                .statusCode(SC_OK);

        AdminService.getAdminsAssignedToRole(adminToken, roleId)
                .then()
                .statusCode(SC_OK)
                .body("admins.id", Matchers.iterableWithSize(1))
                .body("admins.id[0]", equalTo("1"));
    }



    //////////////////////////////////////get assignee from role


    @Test
    public void GetUsersAssignedToRole_UserAssigned_Success() {

        final String roleId = createRole(adminToken, adminCheckerToken);

        AssignAdminsToRoleModel assignRole = AssignAdminsToRoleModel.CreateAssignAdminsToRoleModelBuilder("1");

        ReviewModel review = AdminRoleBasedAccessService.assignRole(assignRole, adminToken, roleId)
                .then()
                .statusCode(SC_CREATED)
                .extract().as(ReviewModel.class);

        AdminHelper.approveReview(adminCheckerToken, review.getCategory(), review.getReviewId());

        AdminRoleBasedAccessService.getUsersAssignedToRole(adminToken, Long.valueOf(roleId))
                .then()
                .statusCode(SC_OK)
                .body("admins[0].email", equalTo("admin@weavr.io"))
                .body("admins[0].friendlyName", equalTo("Root administrator"))
                .body("admins[0].id", equalTo("1"));
    }

    @Test
    public void GetReviews_HappyPath_Success() {
        AdminHelper.createRole(adminToken, adminCheckerToken);

        AdminService.getReviews(adminToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetReviews_AuthTokenMissing_Unauthorized() {
        AdminService.getReviews(null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetReviewDetails_HappyPathApproved_Success() {
        final Response roleResponse = createRole(adminToken);

        final String reviewId = roleResponse.jsonPath().get("reviewId");
        final String category = roleResponse.jsonPath().get("category");

        AdminHelper.approveReview(adminCheckerToken, category, reviewId);

        final ReviewResponseModel reviewResponse = AdminService.getReview(adminToken, category, reviewId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(ReviewResponseModel.class);

        assertEquals(reviewResponse.getDetails().getAction(), "CREATE");
        assertEquals(reviewResponse.getDetails().getCategory(), "access_control");
        assertEquals(reviewResponse.getDetails().getResourceType(), "role");
        assertEquals(reviewResponse.getDetails().getStatus(), "APPROVED");
    }

    @Test
    public void GetReviewDetails_HappyPathRejected_Success() {
        final Response roleResponse = createRole(adminToken);

        final String reviewId = roleResponse.jsonPath().get("reviewId");
        final String category = roleResponse.jsonPath().get("category");

        AdminHelper.rejectReview(adminCheckerToken, category, reviewId);

        final ReviewResponseModel reviewResponse = AdminService.getReview(adminToken, category, reviewId)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(ReviewResponseModel.class);

        assertEquals(reviewResponse.getDetails().getAction(), "CREATE");
        assertEquals(reviewResponse.getDetails().getCategory(), "access_control");
        assertEquals(reviewResponse.getDetails().getResourceType(), "role");
        assertEquals(reviewResponse.getDetails().getStatus(), "REJECTED");
    }

    @Test
    public void GetReviewDetails_AuthTokenMissing_Unauthorized() {
        final Response roleResponse = createRole(adminToken);

        final String reviewId = roleResponse.jsonPath().get("reviewId");
        final String category = roleResponse.jsonPath().get("category");

        AdminHelper.approveReview(adminCheckerToken, category, reviewId);

        AdminService.getReview(null, category, reviewId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetReviewDetails_MissingReview_Conflict() {
        final Response roleResponse = createRole(adminToken);

        final String reviewId = roleResponse.jsonPath().get("reviewId");
        final String category = roleResponse.jsonPath().get("category");

        AdminHelper.approveReview(adminCheckerToken, category, reviewId);

        AdminService.getReview(adminToken, category, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void ReviewDecision_ApproveTwice_Conflict() {
        final Response roleResponse = createRole(adminToken);

        final String reviewId = roleResponse.jsonPath().get("reviewId");
        final String category = roleResponse.jsonPath().get("category");

        AdminHelper.approveReview(adminCheckerToken, category, reviewId);

        AdminService.reviewDecision(new ReviewDecisionModel("APPROVE"), adminCheckerToken, category, reviewId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("REVIEW_NOT_PENDING"));
    }

    @Test
    public void ReviewDecision_ApproverIsCreator_Conflict() {
        final Response roleResponse = createRole(adminToken);

        final String reviewId = roleResponse.jsonPath().get("reviewId");
        final String category = roleResponse.jsonPath().get("category");

        AdminService.reviewDecision(new ReviewDecisionModel("APPROVE"), adminToken, category, reviewId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHECKER_NOT_ALLOWED"));
    }

    @Test
    public void InviteUser_ExistingUser_Conflict() {
        InviteUserModel inviteUserModel = InviteUserModel.DefaultCreateInviteUserModel()
                .setEmail("admin@weavr.io")
                .build();

        AdminRoleBasedAccessService.inviteUser(adminToken, inviteUserModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void InviteUser_UserAcceptInvitation_Success() {
        InviteUserModel inviteUserModel = InviteUserModel.DefaultCreateInviteUserModel().build();

        AdminRoleBasedAccessService.inviteUser(adminToken, inviteUserModel)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Pair<String, String> response = MailhogHelper.getUserInviteNonceAndInviteId(inviteUserModel.getEmail());
        final String nonce = response.getLeft();
        final String inviteId = response.getRight();

        AcceptInviteModel acceptInviteModel = new AcceptInviteModel(nonce, inviteUserModel.getEmail(), new PasswordModel("Password1234!"));

        AdminRoleBasedAccessService.acceptInvite(acceptInviteModel, inviteId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CreateProgrammeAdmin_ExternalIdProvided_Success() {
        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.ProgrammeWithExternalIdModel();
        final String impersonatedToken = AdminService.impersonateTenant(tenantId, AdminService.loginAdmin()).jsonPath().get("token");

        AdminService.createProgramme(impersonatedToken, createProgrammeModel)
                .then()
                .statusCode(SC_OK)
                .body("tenantExternalId", equalTo(createProgrammeModel.getTenantExternalId()));
    }

    @Test
    public void CreateProgrammeAdmin_DuplicatedExternalIdProvided_Conflict() {
        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.ProgrammeWithExternalIdModel();
        final String impersonatedToken = AdminService.impersonateTenant(tenantId, AdminService.loginAdmin()).jsonPath().get("token");

        AdminService.createProgramme(impersonatedToken, createProgrammeModel)
                .then()
                .statusCode(SC_OK)
                .body("tenantExternalId", equalTo(createProgrammeModel.getTenantExternalId()));

        CreateProgrammeModel programmeModelWithSameExternalId = CreateProgrammeModel.ProgrammeWithExternalIdModel(createProgrammeModel.getTenantExternalId());

        AdminService.createProgramme(impersonatedToken, programmeModelWithSameExternalId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TENANT_EXTERNAL_ID_ALREADY_EXISTS"));
    }

    @Test
    public void CreateProgrammeInnovator_ExternalIdProvided_Success() {
        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.ProgrammeWithExternalIdModel();
        final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);

        InnovatorService.createProgramme(createProgrammeModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("tenantExternalId", equalTo(createProgrammeModel.getTenantExternalId()));
    }

    @Test
    public void CreateProgrammeInnovator_DuplicatedExternalIdProvided_Conflict() {
        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.ProgrammeWithExternalIdModel();
        final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);

        InnovatorService.createProgramme(createProgrammeModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("tenantExternalId", equalTo(createProgrammeModel.getTenantExternalId()));

        final CreateProgrammeModel programmeModelWithSameExternalId = CreateProgrammeModel.ProgrammeWithExternalIdModel(createProgrammeModel.getTenantExternalId());

        InnovatorService.createProgramme(programmeModelWithSameExternalId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("TENANT_EXTERNAL_ID_ALREADY_EXISTS"));
    }

    @Test
    public void ViewRolesUsers_AdminUsers_Success() {
        final String adminId = AdminHelper.getRoleManagerId(adminToken).replace("[", "").replace("]", "");
        AdminRoleBasedAccessService.getUsersAssignedToRole(adminToken, Long.valueOf(adminId))
                .then()
                .statusCode(SC_OK)
                .body("admins[0].email", equalTo("admin@weavr.io"))
                .body("admins[0].friendlyName", equalTo("Root administrator"))
                .body("admins[0].id", equalTo("1"))
                .body("admins[1].email", equalTo("admin_checker@weavr.io"))
                .body("admins[1].friendlyName", equalTo("admin_checker@weavr.io"))
                .body("admins[1].id", notNullValue());
    }
}