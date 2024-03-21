package opc.junit.multi.users;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import io.restassured.response.Response;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.users.UserVerifyEmailModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public abstract class AbstractVerifyUserEmailTests extends AbstractUserTests{

  private static final String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;

  @Test
  public void VerifyUserEmail_ByRootUser_Success(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    UsersService.verifyUserEmail(userVerifyEmailModel(user.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void VerifyUserEmail_UserWithTagField_Success(){
    final UsersModel userModel = UsersModel.DefaultUsersModel().setTag(RandomStringUtils.randomAlphanumeric(5)).build();
    UsersHelper.createAuthenticatedUser(userModel, secretKey, getAuthToken());

    sendSuccessfulVerificationEmail(userModel.getEmail());
    UsersService.verifyUserEmail(userVerifyEmailModel(userModel.getEmail()),getSecretKey())
            .then()
            .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void VerifyUserEmail_WithUpdatedEmail_Success(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    final UsersModel patchUserDetails = new UsersModel.Builder()
        .setEmail(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io")
        .build();
    UsersService.patchUser(patchUserDetails, getSecretKey(), user.id, getAuthToken(), Optional.empty());

    sendSuccessfulVerificationEmail(patchUserDetails.getEmail());

    UsersService.verifyUserEmail(userVerifyEmailModel(patchUserDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void VerifyUserEmail_WithoutSendingEmailVerification_Conflict(){
    final User user=createNewUser();
    UsersService.verifyUserEmail(userVerifyEmailModel(user.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
  }
  @Test
  public void VerifyUserEmail_AlreadyVerified_Conflict(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    UsersService.verifyUserEmail(userVerifyEmailModel(user.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);

    UsersService.verifyUserEmail(userVerifyEmailModel(user.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
  }

  @Test
  public void VerifyUserEmail_InvalidSecretKey_Unauthorized(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    UsersService.verifyUserEmail(userVerifyEmailModel(user.userDetails.getEmail()),"abc")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
  @Test
  public void VerifyUserEmail_NoSecretKey_BadRequest(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    UsersService.verifyUserEmail(userVerifyEmailModel(user.userDetails.getEmail()),"")
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("invalidFields[0].error",equalTo("REQUIRED"))
        .body("invalidFields[0].fieldName",equalTo("api-key"));
  }
  @Test
  public void VerifyUserEmail_DifferentInnovatorSecretKey_Unauthorized(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    Triple<String,String,String> innovator=TestHelper.registerLoggedInInnovatorWithProgramme();

    UsersService.verifyUserEmail(userVerifyEmailModel(user.userDetails.getEmail()),innovator.getRight())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
  @Test
  public void VerifyUserEmail_UnknownVerificationCode_Conflict(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    final UserVerifyEmailModel userVerifyEmailModel=UserVerifyEmailModel.builder()
            .email(user.userDetails.getEmail())
            .verificationCode(RandomStringUtils.randomNumeric(6)).build();

    UsersService.verifyUserEmail(userVerifyEmailModel,getSecretKey())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
  }
  @Test
  public void VerifyUserEmail_MinVerificationCode_BadRequest(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    final UserVerifyEmailModel userVerifyEmailModel=UserVerifyEmailModel.builder()
        .email(user.userDetails.getEmail())
        .verificationCode(RandomStringUtils.randomNumeric(5)).build();

    UsersService.verifyUserEmail(userVerifyEmailModel,getSecretKey())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message", containsString("request.verificationCode: size must be between 6 and 6"));
  }
  @Test
  public void VerifyUserEmail_MaxVerificationCode_BadRequest(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    final UserVerifyEmailModel userVerifyEmailModel=UserVerifyEmailModel.builder()
        .email(user.userDetails.getEmail())
        .verificationCode(RandomStringUtils.randomNumeric(7)).build();

    UsersService.verifyUserEmail(userVerifyEmailModel,getSecretKey())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message", containsString("request.verificationCode: size must be between 6 and 6"));
  }
  @Test
  public void VerifyUserEmail_InvalidVerificationCode_BadRequest(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    final UserVerifyEmailModel userVerifyEmailModel=UserVerifyEmailModel.builder()
        .email(user.userDetails.getEmail())
        .verificationCode(RandomStringUtils.randomAlphanumeric(6)).build();

    UsersService.verifyUserEmail(userVerifyEmailModel,getSecretKey())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message", containsString("request.verificationCode: must match \"^[0-9]+$\""));
  }
  @Test
  public void VerifyUserEmail_NoVerificationCode_BadRequest(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    final UserVerifyEmailModel userVerifyEmailModel=UserVerifyEmailModel.builder()
        .email(user.userDetails.getEmail()).build();

    UsersService.verifyUserEmail(userVerifyEmailModel,getSecretKey())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message", equalTo("request.verificationCode: must not be blank"));
  }
  @Test
  public void VerifyUserEmail_NoEmail_BadRequest(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    final Response response=UsersService.verifyUserEmail(userVerifyEmailModel(""),getSecretKey());
    Assertions.assertEquals(SC_BAD_REQUEST, response.statusCode());
    Assertions.assertTrue(response.body().asString().contains("request.email: must not be blank"));
  }

  @Test
  public void VerifyUserEmail_InvalidEmailFormat_BadRequest(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    final Response response=UsersService.verifyUserEmail(userVerifyEmailModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6))),getSecretKey());
    Assertions.assertEquals(SC_BAD_REQUEST, response.statusCode());
    Assertions.assertTrue(response.body().asString().contains("request.email: must be a well-formed email address"));
  }
  @Test
  public void VerifyUser_EmailUnknownEmail_Conflict(){
    final User user=createNewUser();
    sendSuccessfulVerificationEmail(user.userDetails.getEmail());

    UsersService.verifyUserEmail(userVerifyEmailModel(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6))),getSecretKey())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
  }

  private void sendSuccessfulVerificationEmail(String userEmail){
    UsersService.sendEmailVerification(new SendEmailVerificationModel(userEmail),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);

  }
  private UserVerifyEmailModel userVerifyEmailModel(String userEmail){
    return UserVerifyEmailModel.builder().email(userEmail).verificationCode(VERIFICATION_CODE).build();
  }
}
