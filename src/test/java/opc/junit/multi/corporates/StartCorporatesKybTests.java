package opc.junit.multi.corporates;

import opc.enums.opc.IdentityType;
import opc.enums.opc.KybLevel;
import opc.enums.opc.KycLevel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.StartKycModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ConsumersService;
import opc.services.multi.CorporatesService;
import opc.services.multi.PasswordsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class StartCorporatesKybTests extends BaseCorporatesSetup {

  private String corporateId;
  private String authenticationToken;

  @BeforeEach
  public void Setup() {
    final CreateCorporateModel createCorporateModel =
        opc.models.multi.corporates.CreateCorporateModel.DefaultCreateCorporateModel(
            corporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKey);
    corporateId = authenticatedCorporate.getLeft();
    authenticationToken = authenticatedCorporate.getRight();
  }

  @Test
  public void StartCorporatesKyb_Success() {
    CorporatesService.startCorporateKyb(secretKey, authenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("reference", notNullValue());
  }

  @Test
  public void StartCorporatesKyb_InvalidApiKey_Unauthorised() {

    CorporatesService.startCorporateKyb("abc", authenticationToken)
        .then().statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void StartCorporatesKyb_NoApiKey_BadRequest() {

    CorporatesService.startCorporateKyb("", authenticationToken)
        .then().statusCode(SC_BAD_REQUEST);
  }

  @Test
  public void StartCorporatesKyb_DifferentInnovatorApiKey_Forbidden() {

    final Triple<String, String, String> innovator =
        TestHelper.registerLoggedInInnovatorWithProgramme();
    final String secretKey =
        InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
            .get("secretKey");

    CorporatesService.startCorporateKyb(secretKey, authenticationToken)
        .then().statusCode(SC_FORBIDDEN);
  }

  @Test
  public void StartCorporatesKyb_RootUserLoggedOut_Unauthorised() {

    final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId,
        secretKey).getRight();

    CorporatesService.startCorporateKyb(secretKey, token)
        .then().statusCode(SC_UNAUTHORIZED);
  }

  @Test
  public void StartCorporatesKyb_Approved_KybAlreadyApproved() {
    CorporatesService.startCorporateKyb(secretKey, authenticationToken)
        .then()
        .statusCode(SC_OK)
        .body("reference", notNullValue());

    SimulatorService.simulateKybApproval(secretKey, corporateId)
        .then()
        .statusCode(SC_NO_CONTENT);

    CorporatesService.startCorporateKyb(secretKey, authenticationToken)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("KYB_ALREADY_APPROVED"));
  }

  @Test
  public void StartCorporatesKyb_BackofficeImpersonator_Forbidden() {
    CorporatesService.startCorporateKyb(secretKey,
            getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
        .then()
        .statusCode(SC_FORBIDDEN);
  }

  @Test
  public void StartCorporatesKyb_MissingIndustry_Success() {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
            corporateProfileId)
        .setIndustry(null)
        .build();

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKey);

    CorporatesService.startCorporateKyb(secretKey, corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("reference", notNullValue());
  }

  @Test
  public void StartCorporatesKyb_MissingSourceOfFunds_Success() {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
            corporateProfileId)
        .setSourceOfFunds(null)
        .build();

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKey);

    CorporatesService.startCorporateKyb(secretKey, corporate.getRight())
        .then()
        .statusCode(SC_OK)
        .body("reference", notNullValue());
  }

  @Test
  public void StartCorporatesKyb_EmailNotVerified_NotAllowed(){
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
    final String corpEmailNonVerified = CorporatesHelper.createCorporate(createCorporateModel, secretKey);

    final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
            .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

    final String token = PasswordsService.createPassword(createPasswordModel, corpEmailNonVerified, secretKey)
            .then()
            .statusCode(SC_OK)
            .extract().jsonPath().getString("token");

    CorporatesService.startCorporateKyb(secretKey, token)
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("EMAIL_UNVERIFIED"));
  }
}
