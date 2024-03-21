package opc.junit.multiprivate;

import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.AssignRoleModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.multiprivate.MultiPrivateService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;

@Execution(ExecutionMode.CONCURRENT)
public class RoleBasedAccessControlTests extends BaseMultiPrivateSetup {
    private static String corporateId;
    private static String corporateAuthenticationToken;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
    }

    @Test
    //This is the test which will cover whole e2e flow related to RBAC using available endpoints on multi-private
    //When new endpoint for RBAC is added, please update steps here
    public void RBAC_CorporateHappyPath_Success() {
        MultiPrivateService.getPluginRoles(secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("description", notNullValue())
                .body("id", notNullValue())
                .body("name", notNullValue())
                .body("permissions.size()", greaterThanOrEqualTo(0));

        MultiPrivateService.getUserPermissions(secretKey, corporateId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        final String roleId = getRoleId(corporateAuthenticationToken);

        MultiPrivateService.assignRole(secretKey, corporateAuthenticationToken, corporateId, new AssignRoleModel(Long.valueOf(roleId)))
                .then()
                .statusCode(SC_OK)
                .body("id[0]", equalTo(roleId))
                .body("name", notNullValue());

        MultiPrivateService.getRoleAssignees(secretKey, corporateAuthenticationToken, Long.valueOf(roleId))
                .then()
                .statusCode(SC_OK)
                .body("email[0]", notNullValue())
                .body("id", hasItem(corporateId));

        MultiPrivateService.unassignRole(secretKey, corporateAuthenticationToken, corporateId, Long.valueOf(roleId))
                .then()
                .statusCode(SC_OK);

        MultiPrivateService.getRoleAssignees(secretKey, corporateAuthenticationToken, Long.valueOf(roleId))
                .then()
                .statusCode(SC_OK)
                .body("id", not(hasItem(corporateId)));

        MultiPrivateService.getUserPermissions(secretKey, corporateId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetPluginRoles_NoSecretKey_BadRequest() {
        MultiPrivateService.getPluginRoles("", corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void GetPluginRoles_NoAuthToken_Unauthorized() {
        MultiPrivateService.getPluginRoles(secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetRoleAssignees_NoSecretKey_BadRequest() {
        MultiPrivateService.getRoleAssignees("", corporateAuthenticationToken, Long.valueOf(getRoleId(corporateAuthenticationToken)))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void GetRoleAssignees_NoAuthToken_Unauthorized() {
        MultiPrivateService.getRoleAssignees(secretKey, "", Long.valueOf(getRoleId(corporateAuthenticationToken)))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetRoleAssignees_WrongId_NotFound() {
        MultiPrivateService.getRoleAssignees(secretKey, corporateAuthenticationToken, RandomUtils.nextLong())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetUserPermissions_NoSecretKey_BadRequest() {
        MultiPrivateService.getUserPermissions("", corporateId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void GetUserPermissions_NoCorporateId_NotFound() {
        MultiPrivateService.getUserPermissions(secretKey, "", corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetUserPermissions_NoAuthToken_Unauthorized() {
        MultiPrivateService.getUserPermissions(secretKey, corporateId, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUserPermissions_WrongId_NotFound() {
        MultiPrivateService.getUserPermissions(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetUserRoles_Success() {
        MultiPrivateService.getUserRoles(secretKey, corporateAuthenticationToken, corporateId)
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void GetUserRoles_NoSecretKey_BadRequest() {
        MultiPrivateService.getUserRoles("", corporateAuthenticationToken, corporateId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }
    @Test
    public void GetUserRoles_NoAuthToken_UnAuthorized() {
        MultiPrivateService.getUserRoles(secretKey, "", corporateId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUserRoles_NoCorporateId_NotFound() {
        MultiPrivateService.getUserRoles(secretKey, corporateAuthenticationToken, "")
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetUserRoles_RandomAuthToken_UnAuthorized() {
        MultiPrivateService.getUserRoles(secretKey, RandomStringUtils.randomNumeric(18), corporateId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUserRoles_CrossIdentity_NotFound() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporateCrossIdentity = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateAuthenticationTokenCrossIdentity = authenticatedCorporateCrossIdentity.getRight();

        MultiPrivateService.getUserRoles(secretKey, corporateAuthenticationTokenCrossIdentity, corporateId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void AssignRole_NoSecretKey_BadRequest() {
        MultiPrivateService.assignRole("", corporateAuthenticationToken, corporateId, new AssignRoleModel(Long.valueOf(getRoleId(corporateAuthenticationToken))))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void AssignRole_NoAuthToken_Unauthorized() {
        MultiPrivateService.assignRole(secretKey, "", corporateId, new AssignRoleModel(Long.valueOf(getRoleId(corporateAuthenticationToken))))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void AssignRole_WrongId_NotFound() {
        MultiPrivateService.assignRole(secretKey, corporateAuthenticationToken, RandomStringUtils.randomNumeric(18), new AssignRoleModel(Long.valueOf(getRoleId(corporateAuthenticationToken))))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void AssignRole_AssignRoleNotExist_NotFound() {
        MultiPrivateService.assignRole(secretKey, corporateAuthenticationToken, RandomStringUtils.randomNumeric(18), new AssignRoleModel(RandomUtils.nextLong()))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void Unassigned_NoSecretKey_BadRequest() {
        MultiPrivateService.unassignRole("", corporateAuthenticationToken, corporateId, Long.valueOf(getRoleId(corporateAuthenticationToken)))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("api-key"));
    }

    @Test
    public void Unassigned_NoAuthToken_Unauthorized() {
        MultiPrivateService.unassignRole(secretKey, "", corporateId, Long.valueOf(getRoleId(corporateAuthenticationToken)))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Unassigned_WrongIdentityId_Conflict() {
        final Long roleId =Long.valueOf(getRoleId(corporateAuthenticationToken));
        MultiPrivateService.unassignRole(secretKey, corporateAuthenticationToken, RandomStringUtils.randomNumeric(18), roleId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("USER_NOT_FOUND"));
    }

    @Test
    public void Unassigned_WrongRoleId_Conflict() {
        MultiPrivateService.unassignRole(secretKey, corporateAuthenticationToken, corporateId, RandomUtils.nextLong())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ROLE_NOT_ASSIGNED"));
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }

    private static String getRoleId(final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> MultiPrivateService.getPluginRoles(secretKey, token),
                        SC_OK)
                .jsonPath()
                .getList("findAll { it.name == 'Admin' }.id").toString().replace("[", "").replace("]", "");
    }
}
