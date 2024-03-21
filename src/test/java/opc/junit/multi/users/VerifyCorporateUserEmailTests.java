package opc.junit.multi.users;

import opc.enums.mailhog.MailHogEmail;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UserVerifyEmailModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VerifyCorporateUserEmailTests extends AbstractUserTests{
  private String corporateId;
  private String authenticationToken;
  private CreateCorporateModel createCorporateModel;
  final private static String verificationCode = TestHelper.VERIFICATION_CODE;

  @BeforeEach
  public void BeforeEach() {
    createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
    corporateId = authenticatedCorporate.getLeft();
    authenticationToken = authenticatedCorporate.getRight();
  }
  @Test
  public void verifyUserEmailSuccess(){
    final User user = createNewUser();
    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);

    final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(user.userDetails.getEmail());
    assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getFrom(), email.getFrom());
    assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getSubject(), email.getSubject());
    assertEquals(user.userDetails.getEmail(), email.getTo());

    UsersService.verifyUserEmail(verifyEmailModel(user.userDetails.getEmail()), getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);
  }
  @Test
  public void SendEmailVerificationInvalidApiKeyUnauthorised(){
    final User user = createNewUser();
    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), "abc")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
  @Test
  public void VerifyEmailInvalidApiKeyUnauthorised(){
    final User user = createNewUser();
    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), getSecretKey());
    UsersService.verifyUserEmail(verifyEmailModel(user.userDetails.getEmail()), "abc")
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
  @Test
  public void SendEmailVerificationNoApiKeyBadRequest(){
    final User user = createNewUser();
    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), "")
        .then()
        .statusCode(SC_BAD_REQUEST);
  }
  @Test
  public void VerifyEmailNoApiKeyBadRequest(){
    final User user = createNewUser();
    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), getSecretKey());
    UsersService.verifyUserEmail(verifyEmailModel(user.userDetails.getEmail()), "")
        .then()
        .statusCode(SC_BAD_REQUEST);
  }
  @Test
  public void VerifyInvalidEmailConflict(){
    final User user=createNewUser();
    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()),getSecretKey());
    UsersService.verifyUserEmail(verifyEmailModel("test@test.com"), getSecretKey())
        .then()
        .statusCode(SC_CONFLICT);
  }
  @Test
  public void SendEmailVerificationDifferentInnovatorApiKeyForbidden(){

    final User user=createNewUser();

    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), nonFpsApplication.getSecretKey())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
  }
  @Test
  public void SendEmailVerificationEmailAlreadySentSuccess() {
    final User user = createNewUser();
    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);

    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void SendEmailVerificationEmailAlreadyVerifiedSuccess() {
    final User user = createNewUser();
    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);

    UsersService.verifyUserEmail(verifyEmailModel(user.userDetails.getEmail()), getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);

    UsersService.sendEmailVerification(sendEmailModel(user.userDetails.getEmail()), getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @Test
  public void SendEmailVerificationNoEmailBadRequest() {
    UsersService.sendEmailVerification(sendEmailModel(""), getSecretKey())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void SendEmailVerificationInvalidEmailBadRequest() {
    UsersService.sendEmailVerification(sendEmailModel(RandomStringUtils.randomAlphanumeric(6)), getSecretKey())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void SendEmailVerification_InvalidEmailFormatBadRequest() {

    UsersService.sendEmailVerification(sendEmailModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6))), getSecretKey())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void SendEmailVerification_UnknownEmail_EmailNotFound() {
    UsersService.sendEmailVerification(sendEmailModel(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6))), getSecretKey())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("EMAIL_NOT_FOUND"));
  }

  @Override
  protected String getSecretKey() {
    return secretKey;
  }

  @Override
  protected String getAuthToken() {
    return authenticationToken;
  }

  @Override
  protected User createNewUser() {
    final UsersModel model = UsersModel.DefaultUsersModel().build();
    final String userId = UsersHelper.createUser(model, getSecretKey(), getAuthToken());
    return new User(userId, model, corporateId, IdentityType.CORPORATE, UserType.USER);
  }

  @Override
  protected String createPassword(String userId) {
    final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
        .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

    PasswordsService.createPassword(createPasswordModel, userId, secretKey);
    return createPasswordModel.getPassword().getValue();
  }

  @Override
  protected String getRootEmail() {
    return createCorporateModel.getRootUser().getEmail();
  }
  public SendEmailVerificationModel sendEmailModel(String userEmail){
        return new SendEmailVerificationModel(userEmail);
  }
  public UserVerifyEmailModel verifyEmailModel(String userEmail){
        return UserVerifyEmailModel.builder()
            .email(userEmail)
            .verificationCode(verificationCode).build();
  }

}
