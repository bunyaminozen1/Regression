package opc.junit.admin.innovator;

import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import opc.models.admin.UpdateInnovatorUserModel;
import opc.models.innovator.InviteInnovatorUserModel;
import opc.models.shared.GetInnovatorUserModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UpdateInnovatorTests extends BaseInnovatorSetup {
  private static String nonRootInnovatorUserId;

  @BeforeAll
  public static void createNonRootInnovatorUser(){
    final InviteInnovatorUserModel inviteInnovatorUserModel = InviteInnovatorUserModel.defaultInviteUserModel();

    InnovatorService.inviteNewUser(inviteInnovatorUserModel,innovatorToken)
            .then()
            .statusCode(SC_NO_CONTENT);

    nonRootInnovatorUserId = InnovatorService.getInnovatorUsers(new GetInnovatorUserModel(inviteInnovatorUserModel.getEmail()), innovatorToken)
            .then()
            .statusCode(SC_OK)
            .extract().jsonPath().get("userdetails[0].id");
  }

  @Test
  public void UpdateInnovator_RootAdminUpdateInnovatorRootUser_Success() {

    final UpdateInnovatorUserModel updateInnovatorUserModel = UpdateInnovatorUserModel.builder()
        .name(RandomStringUtils.randomAlphabetic(5))
        .surname(RandomStringUtils.randomAlphabetic(5))
        .email(String.format("%s@fakemail.com", RandomStringUtils.randomAlphanumeric(8)))
        .active("FALSE")
        .build();

    AdminService.updateInnovatorUser(updateInnovatorUserModel, innovatorId, adminRootUserToken)
        .then()
        .statusCode(SC_OK)
        .body("name", equalTo(updateInnovatorUserModel.getName()))
        .body("surname", equalTo(updateInnovatorUserModel.getSurname()))
        .body("email", not(updateInnovatorUserModel.getEmail()))
        .body("active", equalTo(false));
  }
  @Test
  public void UpdateInnovator_RootAdminUpdateInnovatorNonRootUser_Success() {

    final UpdateInnovatorUserModel updateInnovatorUserModel = UpdateInnovatorUserModel.builder()
        .name(RandomStringUtils.randomAlphabetic(5))
        .surname(RandomStringUtils.randomAlphabetic(5))
        .email(String.format("%s@fakemail.com", RandomStringUtils.randomAlphanumeric(8)))
        .active("FALSE")
        .build();

    AdminService.updateInnovatorUser(updateInnovatorUserModel, nonRootInnovatorUserId, adminRootUserToken)
        .then()
        .statusCode(SC_OK)
        .body("name", equalTo(updateInnovatorUserModel.getName()))
        .body("surname", equalTo(updateInnovatorUserModel.getSurname()))
        .body("email", not(updateInnovatorUserModel.getEmail()))
        .body("active", equalTo(false));
  }

  @Test
  public void UpdateInnovator_NonRootAdminUpdateInnovatorRootUser_Success() {

    final UpdateInnovatorUserModel updateInnovatorUserModel = UpdateInnovatorUserModel.builder()
        .name(RandomStringUtils.randomAlphabetic(5))
        .surname(RandomStringUtils.randomAlphabetic(5))
        .email(String.format("%s@fakemail.com", RandomStringUtils.randomAlphanumeric(8)))
        .active("TRUE")
        .build();

    AdminService.updateInnovatorUser(updateInnovatorUserModel, innovatorId, adminNonRootUserToken)
        .then()
        .statusCode(SC_OK)
        .body("name", equalTo(updateInnovatorUserModel.getName()))
        .body("surname", equalTo(updateInnovatorUserModel.getSurname()))
        .body("email", not(updateInnovatorUserModel.getEmail()))
        .body("active", equalTo(true));
  }

  @Test
  public void UpdateInnovator_NonRootAdminUpdateInnovatorNonRootUser_Success() {

    final UpdateInnovatorUserModel updateInnovatorUserModel = UpdateInnovatorUserModel.builder()
        .name(RandomStringUtils.randomAlphabetic(5))
        .surname(RandomStringUtils.randomAlphabetic(5))
        .email(String.format("%s@fakemail.com", RandomStringUtils.randomAlphanumeric(8)))
        .active("FALSE")
        .build();

    AdminService.updateInnovatorUser(updateInnovatorUserModel, nonRootInnovatorUserId, adminNonRootUserToken)
        .then()
        .statusCode(SC_OK)
        .body("name", equalTo(updateInnovatorUserModel.getName()))
        .body("surname", equalTo(updateInnovatorUserModel.getSurname()))
        .body("email", not(updateInnovatorUserModel.getEmail()))
        .body("active", equalTo(false));
  }

  @Test
  public void UpdateInnovator_WithoutToken_Unauthorized() {

    final UpdateInnovatorUserModel updateInnovatorUserModel = UpdateInnovatorUserModel.builder().build();

    AdminService.updateInnovatorUser(updateInnovatorUserModel, innovatorId, "")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void UpdateInnovator_InvalidToken_Unauthorized() {

    final UpdateInnovatorUserModel updateInnovatorUserModel = UpdateInnovatorUserModel.builder().build();

    AdminService.updateInnovatorUser(updateInnovatorUserModel, innovatorId, RandomStringUtils.randomAlphanumeric(40))
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void UpdateInnovator_WithoutInnovatorUserId_MethodNotAllowed() {
    final String adminToken = AdminService.loginAdmin();

    final UpdateInnovatorUserModel updateInnovatorUserModel = UpdateInnovatorUserModel.builder().build();

    AdminService.updateInnovatorUser(updateInnovatorUserModel, "", adminToken)
        .then()
        .statusCode(SC_METHOD_NOT_ALLOWED);
  }

  @Test
  public void UpdateInnovator_InvalidInnovatorUserId_NotFound() {
    final String adminToken = AdminService.loginAdmin();

    final UpdateInnovatorUserModel updateInnovatorUserModel = UpdateInnovatorUserModel.builder().build();

    AdminService.updateInnovatorUser(updateInnovatorUserModel, RandomStringUtils.randomNumeric(18), adminToken)
        .then()
        .statusCode(SC_NOT_FOUND);
  }
}
