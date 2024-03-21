package opc.junit.innovator.authenticatorfactors;

import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GetAuthenticationFactorsTests extends BaseAuthenticationFactorsSetup {

  private final static String VERIFICATION_CODE = "123456";
  private static String innovatortoken;
  private static Pair<String, String> corporate;
  private static Pair<String, String> consumer;
  private static Pair<String, String> corporateAuthenticatedUser;
  private static Pair<String, String> consumerAuthenticatedUser;


  @BeforeAll
  public static void Setup() {

    innovatortoken= InnovatorHelper.loginInnovator(applicationOne.getInnovatorEmail(),applicationOne.getInnovatorPassword());

    corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
    consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

    InnovatorHelper.enableAuthy(applicationOne.getProgrammeId(), innovatortoken);

    AdminHelper.setProgrammeAuthyChallengeLimit(applicationOne.getProgrammeId(), LimitInterval.DAILY,50,
        AdminService.loginAdmin());

    corporateAuthenticatedUser = UsersHelper.createAuthenticatedUser(secretKey,
        corporate.getRight());
    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, secretKey, corporateAuthenticatedUser.getLeft(),
        corporateAuthenticatedUser.getRight());

    consumerAuthenticatedUser = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());
    final UsersModel updateUser1 = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser1, secretKey, consumerAuthenticatedUser.getLeft(),
        consumerAuthenticatedUser.getRight());

    AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
        secretKey, corporate.getRight());
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());

    AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
        secretKey, consumer.getRight());
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumer.getLeft(), secretKey, consumer.getRight());

    AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
        secretKey, corporateAuthenticatedUser.getRight());
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporateAuthenticatedUser.getLeft(), secretKey, corporateAuthenticatedUser.getRight());

    AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
        secretKey, consumerAuthenticatedUser.getRight());
    AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(consumerAuthenticatedUser.getLeft(), secretKey, consumerAuthenticatedUser.getRight());

  }

  @Test
  public void GetAuthFactorsCorporateAuthenticatedUserSuccess() {
    InnovatorService.getCorporateUser(innovatortoken, corporate.getLeft(),
            corporateAuthenticatedUser.getLeft())
        .then()
        .statusCode(SC_OK)
        .body("id", equalTo(corporateAuthenticatedUser.getLeft()))
        .body("type", equalTo("USER"))
        .body("identity.type", equalTo("corporates"))
        .body("identity.id", equalTo(corporate.getLeft()))
        .body("credentials.credential[0].id", equalTo(corporate.getLeft()))
        .body("credentials.credential[0].type", equalTo("ROOT"))
        .body("credentials.credential[0].active", equalTo(true))
        .body("credentials.credential[0].factors[0].type", equalTo("AUTHY_PUSH"))
        .body("credentials.credential[0].factors[0].providerKey", equalTo("authy"))
        .body("credentials.credential[0].factors[0].status", equalTo("ACTIVE"))
        .body("credentials.credential[0].factors[1].type", equalTo("PASSWORD"))
        .body("credentials.credential[0].factors[1].providerKey", equalTo("passwords"))
        .body("credentials.credential[0].factors[1].status", equalTo("ACTIVE"))
        .body("credentials.credential[0].factors[2].type", equalTo("SMS_OTP"))
        .body("credentials.credential[0].factors[2].providerKey", equalTo("weavr_authfactors"))
        .body("credentials.credential[0].factors[2].status", equalTo("ACTIVE"))
        .body("credentials.credential[1].id", equalTo(corporateAuthenticatedUser.getLeft()))
        .body("credentials.credential[1].type", equalTo("USER"))
        .body("credentials.credential[1].active", equalTo(true))
        .body("credentials.credential[1].factors[0].type", equalTo("AUTHY_PUSH"))
        .body("credentials.credential[1].factors[0].providerKey", equalTo("authy"))
        .body("credentials.credential[1].factors[0].status", equalTo("ACTIVE"))
        .body("credentials.credential[1].factors[1].type", equalTo("PASSWORD"))
        .body("credentials.credential[1].factors[1].providerKey", equalTo("passwords"))
        .body("credentials.credential[1].factors[1].status", equalTo("ACTIVE"))
        .body("credentials.credential[1].factors[2].type", equalTo("SMS_OTP"))
        .body("credentials.credential[1].factors[2].providerKey", equalTo("weavr_authfactors"))
        .body("credentials.credential[1].factors[2].status", equalTo("ACTIVE"));

  }

  @Test
  public void GetAuthFactorsConsumerAuthenticatedUserSuccess() {
    InnovatorService.getConsumerUser(innovatortoken, consumer.getLeft(),
            consumerAuthenticatedUser.getLeft())
        .then()
        .statusCode(SC_OK)
        .body("id", equalTo(consumerAuthenticatedUser.getLeft()))
        .body("type", equalTo("USER"))
        .body("identity.type", equalTo("consumers"))
        .body("identity.id", equalTo(consumer.getLeft()))
        .body("credentials.credential[0].id", equalTo(consumer.getLeft()))
        .body("credentials.credential[0].type", equalTo("ROOT"))
        .body("credentials.credential[0].active", equalTo(true))
        .body("credentials.credential[0].factors[0].type", equalTo("AUTHY_PUSH"))
        .body("credentials.credential[0].factors[0].providerKey", equalTo("authy"))
        .body("credentials.credential[0].factors[0].status", equalTo("ACTIVE"))
        .body("credentials.credential[0].factors[1].type", equalTo("PASSWORD"))
        .body("credentials.credential[0].factors[1].providerKey", equalTo("passwords"))
        .body("credentials.credential[0].factors[1].status", equalTo("ACTIVE"))
        .body("credentials.credential[0].factors[2].type", equalTo("SMS_OTP"))
        .body("credentials.credential[0].factors[2].providerKey", equalTo("weavr_authfactors"))
        .body("credentials.credential[0].factors[2].status", equalTo("ACTIVE"))
        .body("credentials.credential[1].id", equalTo(consumerAuthenticatedUser.getLeft()))
        .body("credentials.credential[1].type", equalTo("USER"))
        .body("credentials.credential[1].active", equalTo(true))
        .body("credentials.credential[1].factors[0].type", equalTo("AUTHY_PUSH"))
        .body("credentials.credential[1].factors[0].providerKey", equalTo("authy"))
        .body("credentials.credential[1].factors[0].status", equalTo("ACTIVE"))
        .body("credentials.credential[1].factors[1].type", equalTo("PASSWORD"))
        .body("credentials.credential[1].factors[1].providerKey", equalTo("passwords"))
        .body("credentials.credential[1].factors[1].status", equalTo("ACTIVE"))
        .body("credentials.credential[1].factors[2].type", equalTo("SMS_OTP"))
        .body("credentials.credential[1].factors[2].providerKey", equalTo("weavr_authfactors"))
        .body("credentials.credential[1].factors[2].status", equalTo("ACTIVE"));

  }

  @Test//*****
  public void GetAuthFactorsCorporateWithInvalidIdentityIdNotFound() {
    InnovatorService.getCorporateUser(innovatortoken, RandomStringUtils.randomNumeric(18),
            corporateAuthenticatedUser.getLeft())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void GetAuthFactorsCorporateWithInvalidUserIdNotFound() {
    InnovatorService.getCorporateUser(innovatortoken, corporate.getLeft(),
            RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void GetAuthFactorsConsumerWithInvalidIdentityIdForbidden() {
    InnovatorService.getCorporateUser(innovatortoken, RandomStringUtils.randomNumeric(18),
            consumerAuthenticatedUser.getLeft())
        .then()
        .statusCode(SC_NOT_FOUND);
  }

  @Test
  public void GetAuthFactorsConsumerWithInvalidUserIdNotFound() {
    InnovatorService.getCorporateUser(innovatortoken, consumer.getLeft(),
            RandomStringUtils.randomNumeric(18))
        .then()
        .statusCode(SC_NOT_FOUND);
  }
  @Test//***
  public void GetAuthFactorsCrossIdentityCorporateNotFound() {
    InnovatorService.getConsumerUser(innovatortoken, corporate.getLeft(),
            consumerAuthenticatedUser.getLeft())
        .then()
        .statusCode(SC_NOT_FOUND);
  }
  @Test//***
  public void GetAuthFactorsCrossIdentityConsumerNotFound() {
    InnovatorService.getConsumerUser(innovatortoken, consumer.getLeft(),
            corporateAuthenticatedUser.getLeft())
        .then()
        .statusCode(SC_NOT_FOUND);
  }
  @Test
  public void GetAuthFactorsWithoutIdentityIdMethodNotAllowed() {
    InnovatorService.getCorporateUser(innovatortoken, "", consumerAuthenticatedUser.getLeft())
        .then()
        .statusCode(SC_METHOD_NOT_ALLOWED);
  }

  @Test
  public void GetAuthFactorsWithoutUserIdMethodNotAllowed() {
    InnovatorService.getCorporateUser(innovatortoken, corporate.getLeft(), "")
        .then()
        .statusCode(SC_METHOD_NOT_ALLOWED);
  }

}