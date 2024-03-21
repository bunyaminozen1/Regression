package opc.junit.admin.corporates;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import opc.enums.opc.CompanyType;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import commons.models.CompanyModel;
import opc.models.backoffice.ImpersonateIdentityModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubAuthenticatedUserDataModel;
import opc.services.admin.AdminService;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

/**
 * these tests check the function of elevating a new authorized user to root user after its KYC is
 * completed. requirements= email, mobile and KYC should be verified
 */

public class ElevateRootUserTests extends BaseCorporatesSetup {

  @Test
  public void Corporate_KycVerifiedUserElevateToRoot_Success() {

    final CompanyType companyType = CompanyType.LLC;

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
            corporateProfileId)
        .setCompany(CompanyModel.defaultCompanyModel()
            .setType(companyType.name()).build())
        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
            .setMobile(new MobileNumberModel("+356",
                String.format("79%s", RandomStringUtils.randomNumeric(6))))
            .build())
        .build();
    final Pair<String, String> corporate = CorporatesHelper.createKybVerifiedCorporate(
        createCorporateModel, secretKey);

    final CorporateRootUserModel rootUser = createCorporateModel.getRootUser();

    CorporatesService.getCorporates(secretKey, corporate.getRight())
        .then()
        .body("rootUser.dateOfBirth.day", equalTo(rootUser.getDateOfBirth().getDay()))
        .body("rootUser.dateOfBirth.month", equalTo(rootUser.getDateOfBirth().getMonth()))
        .body("rootUser.dateOfBirth.year", equalTo(rootUser.getDateOfBirth().getYear()))
        .body("rootUser.email", equalTo(rootUser.getEmail()))
        .body("rootUser.id.id", equalTo(corporate.getLeft()))
        .body("rootUser.mobile.countryCode", equalTo(rootUser.getMobile().getCountryCode()))
        .body("rootUser.mobile.number", equalTo(rootUser.getMobile().getNumber()))
        .body("rootUser.name", equalTo(rootUser.getName()))
        .body("rootUser.surname", equalTo(rootUser.getSurname()));

    final Pair<String, UsersModel> user = startAndApproveAuthenticatedUserKyc(createCorporateModel,
            corporate.getRight(), companyType);
    final UsersModel authorizedUserInfo = user.getRight();

    AdminService.elevateRootUser(AdminService.loginAdmin(), corporate.getLeft(), user.getLeft())
        .then()
        .statusCode(SC_OK);

    CorporatesService.getCorporates(secretKey, corporate.getRight())
        .then()
        .body("rootUser.dateOfBirth.day", equalTo(authorizedUserInfo.getDateOfBirth().getDay()))
        .body("rootUser.dateOfBirth.month", equalTo(authorizedUserInfo.getDateOfBirth().getMonth()))
        .body("rootUser.dateOfBirth.year", equalTo(authorizedUserInfo.getDateOfBirth().getYear()))
        .body("rootUser.email", equalTo(authorizedUserInfo.getEmail()))
        .body("rootUser.id.id", equalTo(corporate.getLeft()))
        .body("rootUser.mobile.countryCode",
            equalTo(authorizedUserInfo.getMobile().getCountryCode()))
        .body("rootUser.mobile.number", equalTo(authorizedUserInfo.getMobile().getNumber()))
        .body("rootUser.name", equalTo(authorizedUserInfo.getName()))
        .body("rootUser.surname", equalTo(authorizedUserInfo.getSurname()));
  }
  /**
   * Covered issue from production https://weavr-payments.atlassian.net/browse/SAS-5671
   * The embedder started getting error: 404 on endpoint - /multi/backoffice/impersonate_identity_login after change the root user
   */
  @Test
  public void Corporate_KycVerifiedUserElevateToRoot_ImpersonateIdentitySuccess() {

    final CompanyType companyType = CompanyType.LLC;

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
                    corporateProfileId)
            .setCompany(CompanyModel.defaultCompanyModel()
                    .setType(companyType.name()).build())
            .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                    .setMobile(new MobileNumberModel("+356",
                            String.format("79%s", RandomStringUtils.randomNumeric(6))))
                    .build())
            .build();
    final Pair<String, String> corporate = CorporatesHelper.createKybVerifiedCorporate(
            createCorporateModel, secretKey);

    final CorporateRootUserModel rootUser = createCorporateModel.getRootUser();

    CorporatesService.getCorporates(secretKey, corporate.getRight())
            .then()
            .body("rootUser.dateOfBirth.day", equalTo(rootUser.getDateOfBirth().getDay()))
            .body("rootUser.dateOfBirth.month", equalTo(rootUser.getDateOfBirth().getMonth()))
            .body("rootUser.dateOfBirth.year", equalTo(rootUser.getDateOfBirth().getYear()))
            .body("rootUser.email", equalTo(rootUser.getEmail()))
            .body("rootUser.id.id", equalTo(corporate.getLeft()))
            .body("rootUser.mobile.countryCode", equalTo(rootUser.getMobile().getCountryCode()))
            .body("rootUser.mobile.number", equalTo(rootUser.getMobile().getNumber()))
            .body("rootUser.name", equalTo(rootUser.getName()))
            .body("rootUser.surname", equalTo(rootUser.getSurname()));

    final Pair<String, UsersModel> user = startAndApproveAuthenticatedUserKyc(createCorporateModel,
            corporate.getRight(), companyType);
    final UsersModel authorizedUserInfo = user.getRight();

    AdminService.elevateRootUser(AdminService.loginAdmin(), corporate.getLeft(), user.getLeft())
            .then()
            .statusCode(SC_OK);

    CorporatesService.getCorporates(secretKey, corporate.getRight())
            .then()
            .body("rootUser.dateOfBirth.day", equalTo(authorizedUserInfo.getDateOfBirth().getDay()))
            .body("rootUser.dateOfBirth.month", equalTo(authorizedUserInfo.getDateOfBirth().getMonth()))
            .body("rootUser.dateOfBirth.year", equalTo(authorizedUserInfo.getDateOfBirth().getYear()))
            .body("rootUser.email", equalTo(authorizedUserInfo.getEmail()))
            .body("rootUser.id.id", equalTo(corporate.getLeft()))
            .body("rootUser.mobile.countryCode",
                    equalTo(authorizedUserInfo.getMobile().getCountryCode()))
            .body("rootUser.mobile.number", equalTo(authorizedUserInfo.getMobile().getNumber()))
            .body("rootUser.name", equalTo(authorizedUserInfo.getName()))
            .body("rootUser.surname", equalTo(authorizedUserInfo.getSurname()));

    BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(corporate.getLeft(), IdentityType.CORPORATE), secretKey)
            .then()
            .statusCode(SC_OK)
            .body("token.token", notNullValue())
            .body("identity.type", equalTo("CORPORATE"))
            .body("identity.id", equalTo(corporate.getLeft()))
            .body("credentials.type", equalTo("USER"))
            .body("credentials.id", equalTo(corporate.getLeft()));
  }

  @Test
  public void Corporate_KycInitiatedUserElevateToRoot_KycNotVerified() {
    final Pair<String, String> corporate = createVerifiedCorporate(CompanyType.LLC);

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
        corporate.getRight());

    final String kycReferenceId = UsersHelper.startUserKyc(secretKey, user.getRight());

    final IdentityDetailsModel weavrUserIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, user.getRight(), kycReferenceId)
            .getParams();

    final SumSubAuthenticatedUserDataModel applicantData =
        SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(),
                weavrUserIdentity.getExternalUserId())
            .as(SumSubAuthenticatedUserDataModel.class);

    assertEquals("init", applicantData.getReview().getReviewStatus());

    AdminService.elevateRootUser(AdminService.loginAdmin(), corporate.getLeft(), user.getLeft())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("KYC_NOT_VERIFIED"));
  }

  @Test
  public void Corporate_KycPendingUserElevateToRoot_KycNotVerified() {
    final CompanyType companyType = CompanyType.LLC;
    final Pair<String, String> corporate = createVerifiedCorporate(companyType);

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
        corporate.getRight());

    final String kycReferenceId = UsersHelper.startUserKyc(secretKey, user.getRight());

    final IdentityDetailsModel weavrUserIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, user.getRight(), kycReferenceId)
            .getParams();

    final SumSubAuthenticatedUserDataModel applicantData =
        SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(),
                weavrUserIdentity.getExternalUserId())
            .as(SumSubAuthenticatedUserDataModel.class);

    SumSubHelper.setRepresentativeInInitiatedState(companyType, applicantData.getId(),
        applicantData.getExternalUserId(), UBO_DIRECTOR_QUESTIONNAIRE_ID);

    SumSubHelper.setInitiatedRepresentativeInPendingState(companyType, applicantData.getId(),
        applicantData.getExternalUserId());

    SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(),
            applicantData.getExternalUserId())
        .then()
        .statusCode(SC_OK)
        .body("review.reviewStatus", equalTo("pending"));

    AdminService.elevateRootUser(AdminService.loginAdmin(), corporate.getLeft(), user.getLeft())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("KYC_NOT_VERIFIED"));
  }

  @Test
  public void Corporate_KycRejectedUserElevateToRoot_KycNotVerified() {
    final CompanyType companyType = CompanyType.LLC;
    final Pair<String, String> corporate = createVerifiedCorporate(companyType);

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey,
        corporate.getRight());

    final String kycReferenceId = UsersHelper.startUserKyc(secretKey, user.getRight());

    final IdentityDetailsModel weavrUserIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, user.getRight(), kycReferenceId)
            .getParams();

    final SumSubAuthenticatedUserDataModel applicantData =
        SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(),
                weavrUserIdentity.getExternalUserId())
            .as(SumSubAuthenticatedUserDataModel.class);

    SumSubHelper.rejectRepresentative(companyType, applicantData.getId(),
        applicantData.getExternalUserId(), UBO_DIRECTOR_QUESTIONNAIRE_ID);

    SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(),
            applicantData.getExternalUserId())
        .then()
        .statusCode(SC_OK)
        .body("review.reviewResult.reviewAnswer", equalTo("RED"));

    AdminService.elevateRootUser(AdminService.loginAdmin(), corporate.getLeft(), user.getLeft())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("KYC_NOT_VERIFIED"));
  }

  @Test
  public void Corporate_EmailNotVerifiedUser_ElevateToRoot_EmailNotVerified() {
    final Pair<String, String> corporate = createVerifiedCorporate(CompanyType.LLC);

    final String userId = UsersHelper.createUser(secretKey, corporate.getRight());

    AdminService.elevateRootUser(AdminService.loginAdmin(), corporate.getLeft(), userId)
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("EMAIL_NOT_VERIFIED"));
  }

  @Test
  public void Corporate_MobileNotVerifiedUser_ElevateToRoot_MobileNotVerified() {
    final Pair<String, String> corporate = createVerifiedCorporate(CompanyType.LLC);

    final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey,
            corporate.getRight());

    AdminService.elevateRootUser(AdminService.loginAdmin(), corporate.getLeft(), user.getLeft())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("MOBILE_NOT_VERIFIED"));
  }

  /**
   * This method creates an authenticated and verified corporate
   */
  private Pair<String, String> createVerifiedCorporate(final CompanyType companyType) {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
            corporateProfileId)
        .setCompany(CompanyModel.defaultCompanyModel()
            .setType(companyType.name()).build())
        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
            .setMobile(new MobileNumberModel("+356",
                String.format("79%s", RandomStringUtils.randomNumeric(6))))
            .build())
        .build();

    return CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);
  }
}
