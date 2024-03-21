package opc.junit.multi.users;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.*;


import io.restassured.response.Response;
import opc.junit.helpers.TestHelper;
import opc.models.multi.users.UsersModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public abstract class AbstractUserSendEmailTests extends AbstractUserTests{

  @Test
  public void SendVerificationEmailByRootUserSuccess(){
    final User user=createNewUser();
    UsersService.sendEmailVerification(new SendEmailVerificationModel(user.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void SendMultipleVerificationEmailSuccess(){
    final User user=createNewUser();
    UsersService.sendEmailVerification(new SendEmailVerificationModel(user.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);
    UsersService.sendEmailVerification(new SendEmailVerificationModel(user.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);
  }
  @Test
  public void SendVerificationEmailUpdatedBeforeSendingMailSuccess(){
    final User newUser = createNewUser();

    final UsersModel patchUserDetails = new UsersModel.Builder()
        .setEmail(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io")
        .build();

    UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty());

    UsersService.sendEmailVerification(new SendEmailVerificationModel(patchUserDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);

  }
  @Test
  public void SendEmailUpdatedBeforeVerificationSuccess(){
    final User newUser = createNewUser();
    UsersService.sendEmailVerification(new SendEmailVerificationModel(newUser.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);

    final UsersModel patchUserDetails = new UsersModel.Builder()
        .setEmail(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io")
        .build();

    UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty());

    UsersService.sendEmailVerification(new SendEmailVerificationModel(patchUserDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);
  }
  @Test
  public void SendEmailToOldAddressAfterUpdate(){
    final User newUser = createNewUser();
    final UsersModel patchUserDetails = new UsersModel.Builder()
        .setEmail(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io")
        .build();

    UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty());

    UsersService.sendEmailVerification(new SendEmailVerificationModel(newUser.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
  }

  @Test
  public void SendVerificationEmailInvalidAddressConflict() {
    UsersService.sendEmailVerification(new SendEmailVerificationModel(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io"),getSecretKey())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
  }
  @Test
  public void SendVerificationEmailWithoutAddressBadRequest(){
    Response response = UsersService.sendEmailVerification(new SendEmailVerificationModel(""), getSecretKey());
    Assertions.assertEquals(SC_BAD_REQUEST, response.statusCode());
    Assertions.assertTrue(response.body().asString().contains("request.email: must not be blank"));
  }
  @Test
  public void SendVerificationEmailWithWrongFormatAddressBadRequest(){
    final Response response=UsersService.sendEmailVerification(new SendEmailVerificationModel(RandomStringUtils.randomAlphabetic(10)),getSecretKey());
    Assertions.assertEquals(SC_BAD_REQUEST, response.statusCode());
    Assertions.assertTrue(response.body().asString().contains("request.email: must be a well-formed email address"));
  }
  @Test
  public void SendVerificationEmailWithoutSecretKeyBadRequest(){
    final User user=createNewUser();
    UsersService.sendEmailVerification(new SendEmailVerificationModel(user.userDetails.getEmail()),"")
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("invalidFields[0].error",equalTo("REQUIRED"))
        .body("invalidFields[0].fieldName",equalTo("api-key"));

  }
  @Test
  public void SendVerificationEmailWithInvalidSecretKeyUnauthorized(){
    final User user=createNewUser();
    UsersService.sendEmailVerification(new SendEmailVerificationModel(user.userDetails.getEmail()),"123")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
  @Test
  public void SendVerificationEmailWithAnotherInnovatorSecretKeyUnauthorized(){
    final User user=createNewUser();

    final Triple<String, String, String> innovator = TestHelper.registerLoggedInInnovatorWithProgramme();

    UsersService.sendEmailVerification(new SendEmailVerificationModel(user.userDetails.getEmail()),innovator.getRight())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}
