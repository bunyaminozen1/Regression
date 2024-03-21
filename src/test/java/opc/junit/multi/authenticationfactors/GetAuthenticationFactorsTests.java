package opc.junit.multi.authenticationfactors;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

public class GetAuthenticationFactorsTests extends BaseAuthenticationFactorsSetup {

  private final static String VERIFICATION_CODE = "123456";

  private static String corporateToken;
  private static String consumerToken;
  private static String authenticatedUserToken;

  @BeforeAll
  public static void Setup() {

    corporateToken = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey)
        .getRight();
    consumerToken = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey)
        .getRight();

    final Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(secretKey,
        corporateToken);
    authenticatedUserToken = authenticatedUser.getRight();

    final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random())
        .build();
    UsersHelper.updateUser(updateUser, secretKey, authenticatedUser.getLeft(),
        authenticatedUserToken);

    AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
        secretKey, corporateToken);
    AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
        secretKey, consumerToken);
    AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
        secretKey, authenticatedUserToken);
  }

  @Test
  public void GetAuthFactors_CorporateOtp_Success() {
    AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(),
            corporateToken)
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("OTP"))
        .body("factors[0].channel", equalTo("SMS"))
        .body("factors[0].status", equalTo("ACTIVE"));
  }

  @Test
  public void GetAuthFactors_ConsumerOtp_Success() {
    AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(),
            consumerToken)
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("OTP"))
        .body("factors[0].channel", equalTo("SMS"))
        .body("factors[0].status", equalTo("ACTIVE"));
  }

  @Test
  public void GetAuthFactors_AuthenticatedUserOtp_Success() {
    AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(),
            authenticatedUserToken)
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("OTP"))
        .body("factors[0].channel", equalTo("SMS"))
        .body("factors[0].status", equalTo("ACTIVE"));
  }

  @Test
  public void GetAuthFactors_PendingVerification_Success() {

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        corporateProfileId, secretKey);
    AuthenticationFactorsHelper.enrolOtpUser(EnrolmentChannel.SMS.name(), secretKey,
        corporate.getRight());
    AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("OTP"))
        .body("factors[0].channel", equalTo("SMS"))
        .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
  }

  @Test
  public void GetAuthFactors_NoEnrolments_NoEntries() {

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        corporateProfileId, secretKey);

    AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors", nullValue());
  }

  @Test
  public void GetAuthFactors_DifferentInnovatorApiKey_Forbidden() {

    final Triple<String, String, String> innovator =
        TestHelper.registerLoggedInInnovatorWithProgramme();

    final String otherInnovatorSecretKey =
        InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
            .get("secretKey");

    AuthenticationFactorsService.getAuthenticationFactors(otherInnovatorSecretKey, Optional.empty(),
            corporateToken)
        .then()
        .statusCode(SC_FORBIDDEN);
  }

  @Test
  public void GetAuthFactors_UserLoggedOut_Unauthorised() {

    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
        consumerProfileId, secretKey);
    AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
        secretKey, consumer.getRight());

    AuthenticationService.logout(secretKey, consumer.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(),
            consumer.getRight())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void GetAuthFactors_InvalidApiKey_Unauthorised() {

    AuthenticationFactorsService.getAuthenticationFactors("abc", Optional.empty(), consumerToken)
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void GetAuthFactors_NoApiKey_BadRequest() {

    AuthenticationFactorsService.getAuthenticationFactors("", Optional.empty(), consumerToken)
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void GetAuthFactors_BackofficeImpersonator_Forbidden() {

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        corporateProfileId, secretKey);
    AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
        secretKey, corporate.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(),
            getBackofficeImpersonateToken(corporate.getLeft(), IdentityType.CORPORATE))
        .then()
        .statusCode(SC_FORBIDDEN);
  }
}
