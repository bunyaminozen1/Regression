package opc.junit.multi.authenticationfactors;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.innovator.CreateCorporateProfileModel;
import commons.models.innovator.IdentityProfileAuthenticationModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

public class EnrolDeviceByPushTests extends BaseAuthenticationFactorsSetup {

  final private static EnrolmentChannel enrolmentChannel=EnrolmentChannel.AUTHY;

  @BeforeAll
  public static void testSetup() {
    final String adminToken = AdminService.loginAdmin();
    final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 100000);

    AdminHelper.resetProgrammeAuthyLimitsCounter(passcodeApp.getProgrammeId(), adminToken);
    AdminHelper.setProgrammeAuthyChallengeLimit(passcodeApp.getProgrammeId(), resetCount, adminToken);
  }


  @Test
  public void EnrolUser_CorporateRoot_Success() {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
        passcodeAppCorporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, passcodeAppSecretKey);

    assertSuccessfulEnrolment(corporate.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
  }

  @Test
  public void EnrolUser_ConsumerRoot_Success() {

    final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(
        passcodeAppConsumerProfileId).build();
    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
        createConsumerModel, passcodeAppSecretKey);

    assertSuccessfulEnrolment(consumer.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            consumer.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
  }

  @Test
  public void EnrolUser_AuthenticatedUser_Success() {

    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
        passcodeAppConsumerProfileId, passcodeAppSecretKey);

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, passcodeAppSecretKey,
        consumer.getRight());

    final UsersModel updateUser = UsersModel.builder().setEmail(usersModel.getEmail())
        .setMobile(MobileNumberModel.random()).build();
    UsersHelper.updateUser(updateUser, passcodeAppSecretKey, user.getLeft(), user.getRight());

    assertSuccessfulEnrolment(user.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            user.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
  }

  @Test
  public void EnrolUser_AuthenticatedUserNoMobileNumber_MobileNumberNotAvailable() {

    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
        passcodeAppConsumerProfileId, passcodeAppSecretKey);

    final UsersModel usersModel =
        UsersModel.DefaultUsersModel()
            .setMobile(null)
            .build();
    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, passcodeAppSecretKey,
        consumer.getRight());

    AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), passcodeAppSecretKey, user.getRight())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("MOBILE_NUMBER_NOT_AVAILABLE"));
  }

  @Test
  public void EnrolUser_AlreadyEnrolledNotVerified_Success() {

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        passcodeAppCorporateProfileId, passcodeAppSecretKey);

    assertSuccessfulEnrolment(corporate.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

    assertSuccessfulEnrolment(corporate.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
  }

  @Test
  public void EnrolUser_AlreadyEnrolledAndVerified_ChannelAlreadyRegistered() {

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        passcodeAppCorporateProfileId, passcodeAppSecretKey);

    assertSuccessfulEnrolment(corporate.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

    SimulatorHelper.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(),
        State.ACTIVE);

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("ACTIVE"));

    AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), passcodeAppSecretKey, corporate.getRight())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_ALREADY_REGISTERED"));
  }

  @Test
  public void EnrolUser_AlreadyEnrolledAndRejected_Success() {

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        passcodeAppCorporateProfileId, passcodeAppSecretKey);

    assertSuccessfulEnrolment(corporate.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

    SimulatorHelper.rejectAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(),
        State.INACTIVE);

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("INACTIVE"));

    assertSuccessfulEnrolment(corporate.getRight());

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factors[0].type", equalTo("PUSH"))
        .body("factors[0].channel", equalTo(enrolmentChannel.name()))
        .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
  }

  @Test
  public void EnrolUser_DifferentInnovatorApiKey_Forbidden() {

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        passcodeAppCorporateProfileId, passcodeAppSecretKey);

    final Triple<String, String, String> innovator =
        TestHelper.registerLoggedInInnovatorWithProgramme();

    final String otherInnovatorSecretKey =
        InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
            .get("secretKey");

    AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), otherInnovatorSecretKey,
            corporate.getRight())
        .then()
        .statusCode(SC_FORBIDDEN);
  }

  @Test
  public void EnrolUser_UserLoggedOut_Unauthorised() {

    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
        passcodeAppConsumerProfileId, passcodeAppSecretKey);

    AuthenticationService.logout(passcodeAppSecretKey, consumer.getRight());

    AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), passcodeAppSecretKey, consumer.getRight())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void EnrolUser_InvalidApiKey_Unauthorised() {

    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
        passcodeAppConsumerProfileId, passcodeAppSecretKey);

    AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), "abc", consumer.getRight())
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void EnrolUser_NoApiKey_BadRequest() {

    final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
        passcodeAppConsumerProfileId, passcodeAppSecretKey);

    AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), "", consumer.getRight())
        .then()
        .statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void EnrolUser_BackofficeImpersonator_Forbidden() {

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        passcodeAppCorporateProfileId, passcodeAppSecretKey);

    AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), passcodeAppSecretKey,
            getBackofficeImpersonateToken(corporate.getLeft(), IdentityType.CORPORATE))
        .then()
        .statusCode(SC_FORBIDDEN);
  }

  @Test
  public void EnrolUser_AuthyNotSupportedByProfile_ChannelNotSupported() {

    final CreateCorporateProfileModel createCorporateProfileModel =
        CreateCorporateProfileModel.DefaultCreateCorporateProfileModel()
            .setAccountInformationFactors(
                IdentityProfileAuthenticationModel.DefaultAccountInfoIdentityProfileAuthenticationScheme())
            .setPaymentInitiationFactors(
                IdentityProfileAuthenticationModel.PaymentInitIdentityProfileAuthenticationScheme(
                    "SMS_OTP"))
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.BeneficiaryManagementIdentityProfileAuthenticationScheme("SMS_OTP"))
            .build();

    final Pair<String, String> innovatorDetails =
        InnovatorHelper.createNewInnovatorWithCorporateProfile(createCorporateProfileModel);

    final Pair<String, String> corporate =
        CorporatesHelper.createAuthenticatedCorporate(innovatorDetails.getRight(),
            innovatorDetails.getLeft(), TestHelper.DEFAULT_PASSWORD);

    AuthenticationFactorsService.enrolPush(EnrolmentChannel.AUTHY.name(),
            innovatorDetails.getLeft(), corporate.getRight())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));
  }

  @ParameterizedTest
  @EnumSource(value = EnrolmentChannel.class, names = {"EMAIL", "UNKNOWN", "SMS"})
  public void EnrolUser_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
        passcodeAppCorporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, passcodeAppSecretKey);

    AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), passcodeAppSecretKey, corporate.getRight())
        .then()
        .statusCode(SC_BAD_REQUEST);

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factor", nullValue());
  }

  @Test
  public void EnrolUser_NoChannel_NotFound() {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
        passcodeAppCorporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, passcodeAppSecretKey);

    AuthenticationFactorsService.enrolPush("", passcodeAppSecretKey, corporate.getRight())
        .then()
        .statusCode(SC_NOT_FOUND);

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factor", nullValue());
  }

  @Test
  public void EnrolUser_EmptyChannelValue_NotFound() {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
        passcodeAppCorporateProfileId).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, passcodeAppSecretKey);

    AuthenticationFactorsService.enrolPush(" ", passcodeAppSecretKey, corporate.getRight())
        .then()
        .statusCode(SC_NOT_FOUND);

    AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
            corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("factor", nullValue());
  }

  private void assertSuccessfulEnrolment(final String authenticationToken) {

    AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), passcodeAppSecretKey,
            authenticationToken)
        .then()
        .statusCode(SC_NO_CONTENT);
  }
}
