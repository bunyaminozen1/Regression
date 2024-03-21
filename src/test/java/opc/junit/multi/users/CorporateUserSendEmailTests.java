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
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CorporateUserSendEmailTests extends AbstractUserSendEmailTests{
  private String corporateId;
  private String authenticationToken;
  private String corporateEmail;

  @BeforeEach
  public void BeforeEach() {
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
    corporateId = authenticatedCorporate.getLeft();
    authenticationToken = authenticatedCorporate.getRight();
    corporateEmail = createCorporateModel.getRootUser().getEmail();
  }
  @Test
  public void CheckVerificationEmailContent(){
    final User user=createNewUser();
    UsersService.sendEmailVerification(new SendEmailVerificationModel(user.userDetails.getEmail()),getSecretKey())
        .then()
        .statusCode(SC_NO_CONTENT);

    final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(user.userDetails.getEmail());
    assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getFrom(), email.getFrom());
    assertEquals(MailHogEmail.CORPORATE_EMAIL_VERIFICATION.getSubject(), email.getSubject());
    assertEquals(user.userDetails.getEmail(), email.getTo());
  }

  @Test
  public void SendEmailVerification_SameEmailDifferentProgrammes_Success() {

    final String userEmail = String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10));

    //Create user and send email verification code under applicationOne
    final UsersModel userModel = UsersModel.DefaultUsersModel().setEmail(userEmail).build();
    UsersHelper.createUser(userModel, getSecretKey(), getAuthToken());

    UsersService.sendEmailVerification(new SendEmailVerificationModel(userEmail), getSecretKey())
            .then()
            .statusCode(SC_NO_CONTENT);

    UsersService.verifyUserEmail(UserVerifyEmailModel.builder()
                    .email(userEmail)
                    .verificationCode(TestHelper.VERIFICATION_CODE).build(), secretKey)
            .then()
            .statusCode(SC_NO_CONTENT);

    //Create user and send email verification code under applicationThree
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(applicationThree.getCorporatesProfileId()).build();
    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationThree.getSecretKey());

    final UsersModel userModelUnderAnotherProgramme = UsersModel.DefaultUsersModel().setEmail(userEmail).build();
    UsersHelper.createUser(userModelUnderAnotherProgramme, applicationThree.getSecretKey(), authenticatedCorporate.getRight());

    UsersService.sendEmailVerification(new SendEmailVerificationModel(userEmail), applicationThree.getSecretKey())
            .then()
            .statusCode(SC_NO_CONTENT);

    UsersService.verifyUserEmail(UserVerifyEmailModel.builder()
                    .email(userEmail)
                    .verificationCode(TestHelper.VERIFICATION_CODE).build(), applicationThree.getSecretKey())
            .then()
            .statusCode(SC_NO_CONTENT);
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
    return corporateEmail;
  }
}

