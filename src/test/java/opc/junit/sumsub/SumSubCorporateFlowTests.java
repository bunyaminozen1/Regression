package opc.junit.sumsub;

import commons.models.CompanyModel;
import commons.models.MobileNumberModel;
import io.cucumber.messages.internal.com.google.gson.Gson;
import io.cucumber.messages.internal.com.google.gson.JsonObject;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CompanyType;
import opc.enums.opc.CountryCode;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.KybState;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.enums.sumsub.SumSubApplicantState;
import opc.junit.database.BeneficiaryDatabaseHelper;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.database.DocumentRepositoryDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.admin.SetApplicantLevelModel;
import opc.models.admin.SubscriptionStatusPayneticsModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CorporateRootUserModel.Builder;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.GetCorporateBeneficiariesModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.AddLegalEntityDirectorModel;
import opc.models.sumsub.CompanyAddressModel;
import opc.models.sumsub.CompanyInfoModel;
import opc.models.sumsub.FixedInfoModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.LegalEntityDirectorInfoModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.models.sumsub.SumSubAuthenticatedUserDataModel;
import opc.models.sumsub.SumSubCompanyInfoModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.CorporatesService;
import opc.services.multi.ManagedAccountsService;
import opc.services.sumsub.SumSubService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static opc.enums.sumsub.IdDocType.ARTICLES_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.CERTIFICATE_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.COMPANY_POA;
import static opc.enums.sumsub.IdDocType.INFORMATION_STATEMENT;
import static opc.enums.sumsub.IdDocType.SELFIE;
import static opc.enums.sumsub.IdDocType.SHAREHOLDER_REGISTRY;
import static opc.junit.helpers.TestHelper.OTP_VERIFICATION_CODE;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Tag(MultiTags.SUMSUB_CORPORATE)
public class SumSubCorporateFlowTests extends BaseSumSubSetup {

  @BeforeAll
  public static void enableEdd() {
    opc.junit.helpers.adminnew.AdminHelper.setEddCountriesProperty(adminToken, nonFpsEnabledTenant.getInnovatorId(), "MT", IdentityType.CORPORATE);
  }

  @AfterAll
  public static void disableEdd() {
    opc.junit.helpers.adminnew.AdminHelper.deleteEddCountriesProperty(adminToken, nonFpsEnabledTenant.getInnovatorId(), IdentityType.CORPORATE);
  }

  @ParameterizedTest
  @EnumSource(value = CompanyType.class, names = {"LLC", "LIMITED_LIABILITY_PARTNERSHIP"})
  public void Corporate_VerifyCorporateLevel_Success(final CompanyType companyType) {

    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
        weavrIdentity.getExternalUserId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
        CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
        SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
        AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
            .setShareSize(50).build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
        applicantData.getId(),
        companyType.name(),
        addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel)
                        .setShareSize(50).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId()),
        beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    final long startTime = System.currentTimeMillis();

    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());
    CorporatesHelper.verifyCorporateLastApprovalTime(corporate.getLeft(), time -> {
      assertNotNull(time);
      assertTrue(time > startTime);
    });
    CorporatesHelper.verifyBeneficiaryLastApprovalTime(beneficiaryId, time -> {
      assertNotNull(time);
      assertTrue(time > startTime);
    });
    CorporatesHelper.verifyBeneficiaryLastApprovalTime(representativeId, time -> {
      assertNotNull(time);
      assertTrue(time < startTime);
    });

    BeneficiariesHelper.verifyBeneficiariesShareSize(corporate.getLeft(),beneficiaryExternalUserId);

    assertTrue(ensureIdentitySubscribed(corporate.getLeft()));

    //This part checks the paynetics subscription status of identity
    final SubscriptionStatusPayneticsModel subscriptionStatus = opc.junit.helpers.adminnew.AdminHelper.getCorporateSubscriptionStatus(corporate.getLeft(),AdminService.loginAdmin());

    assertEquals(subscriptionStatus.getEntity().getIdentityId(), corporate.getLeft());
    assertEquals(subscriptionStatus.getEntity().getPhase(), "COMPLETED_EVENT_PAYNETICS_EEA");
    assertEquals(subscriptionStatus.getEntity().getStatus(), "COMPLETED");
  }

  /**
   *   Root user is creating Corporate 1, passing KYC
   *   Root user starts creating Corporate 2
   *   we retrieve via Sumsub webhook the documents instead of asking once again to perform KYC
   */
  @Test
  public void Corporate_VerifyCorporateLevelDirectorWithKycReady_Success() {
    //create first corporate and KYCed director
    final Builder rootUserModel = createKycVerifiedDirector();

    final String rootUserEmail = rootUserModel.getEmail();

    final CompanyType companyType = CompanyType.LLC;

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
        .setCompany(CompanyModel.defaultCompanyModel()
            .setType(companyType.name()).build())
        .setRootUser(rootUserModel
            //TODO remove after SEMI
            .setEmail(String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                RandomStringUtils.randomAlphabetic(5)))
            .setMobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
            .build())
        .build();

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId).getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
        SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
        SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
        AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
        applicantData.getId(),
        companyType.name(),
        addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

    final CreateCorporateModel createCorporateModel2 = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
        .setCompany(CompanyModel.defaultCompanyModel()
            .setType(companyType.name()).build())
        .setRootUser(rootUserModel
            .setEmail(rootUserEmail)
            .setMobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
            .build())
        .build();
    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel2)
            .build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId()),
        beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());
  }

    @ParameterizedTest
    @EnumSource(value = CompanyType.class, names = { "PUBLIC_LIMITED_COMPANY" })
    public void Corporate_VerifyCorporatePlcLevel_Success(final CompanyType companyType) {

    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK)
        .body("info.companyInfo.companyName", equalTo(companyInfoModel.getCompanyName()))
        .body("info.companyInfo.registrationNumber",
            equalTo(companyInfoModel.getRegistrationNumber()))
        .body("info.companyInfo.country", equalTo(companyInfoModel.getCountry()))
        .body("info.companyInfo.legalAddress", equalTo(companyInfoModel.getLegalAddress()))
        .body("info.companyInfo.phone", equalTo(companyInfoModel.getPhone()))
        .body("info.companyInfo.address.town", equalTo(companyInfoModel.getAddress().getTown()))
        .body("review.reviewStatus", equalTo("init"));

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
        CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantData.getId());

      SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
              SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
      );

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, CORPORATE_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK)
        .body("info.companyInfo.beneficiaries[0].applicantId", equalTo(representativeId))
        .body("info.companyInfo.beneficiaries[0].positions", nullValue())
        .body("info.companyInfo.beneficiaries[0].type", equalTo(addRepresentativeModel.getType()));

      final Pair<String, String> director =
              SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

      weavrIdentity.setAccessToken(
              SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                      companyType.name()));

      SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
      SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

      CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

      //Verify email
      CorporatesHelper.verifyEmail(createCorporateModel.getRootUser().getEmail(), secretKey);
      //verify mobile
      AuthenticationFactorsHelper.enrolAndVerifyOtp(
          OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

      //create managed account model
      final CreateManagedAccountModel createManagedAccountModel = CreateManagedAccountModel
          .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();

      //try to create managed account before approval
      ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, corporate.getRight(), Optional.empty())
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

      SumSubHelper.approveDirector(director.getLeft(), director.getRight());

      SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
      SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

      CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

      assertTrue(ensureIdentitySubscribed(corporate.getLeft()));

      //try again to create managed account after approval
      ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporate.getRight());
  }

  /**
   * This test checks if documents with special characters or different alphabet letters are handled correctly
   * @throws SQLException
   */
  @Test
  public void Corporate_VerifyDocumentWithSpecialCharacters_Success() throws SQLException {

    final CompanyType companyType = CompanyType.PUBLIC_LIMITED_COMPANY;
    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
            SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                    .getParams();

    final SumSubApplicantDataModel applicantData =
            SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                            weavrIdentity.getExternalUserId())
                    .as(SumSubApplicantDataModel.class);

    SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
            CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
            applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addRepresentativeModel =
            AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
            SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                    addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
            representativeExternalUserId, CORPORATE_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    final AddBeneficiaryModel addDirectorModel = AddBeneficiaryModel.addDirectorWithSpecialCharactersModel().build();

    final Pair<String, String> director =
            SumSubHelper.addDirectorWithSpecialCharacters(addDirectorModel, companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    final String corporateId = corporate.getLeft();
    final String companyName = createCorporateModel.getCompany().getName().toUpperCase();
    final String directorFirstName = addDirectorModel.getApplicant().getInfo().getFirstName();
    final String directorLastName = addDirectorModel.getApplicant().getInfo().getLastName();

    final Map<Integer, Map<String, String>> otherDirector = DocumentRepositoryDatabaseHelper.getFile("OTHER_DIRECTOR", corporate.getLeft());
    assertEquals(otherDirector.get(0).get("filepath"), String.format("kyb/corporates/%s %s/%s %s/OTHER_DIRECTOR_SCREENING_REPORT.pdf",
            corporateId, companyName, directorFirstName, directorLastName));

    assertEquals(otherDirector.get(1).get("filepath"), String.format("kyb/corporates/%s %s/%s %s/OTHER_DIRECTOR_WATCHLIST_REPORT.pdf",
            corporateId, companyName, directorFirstName, directorLastName));
  }

  @ParameterizedTest
  @MethodSource("merchantTokenProvider")
  @Disabled
  //TODO to fix this test on QA
  public void Corporate_VerifyCorporatePvLevelAndMerchantTokenCheck_Success(final CountryCode countryCode,
                                                                            final String merchantToken,
                                                                            final ProgrammeDetailsModel programme) {

    final CompanyType companyType = CompanyType.PUBLIC_LIMITED_COMPANY;
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId())
            .setCompany(CompanyModel.defaultCompanyModel()
                    .setRegistrationCountry(countryCode.name())
                    .setType(companyType.name()).build())
            .build();

    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel, programme.getSecretKey());
    final String kybReferenceId = CorporatesHelper.startKyb(programme.getSecretKey(), corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(programme.getSharedKey(), corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
        CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK)
        .body("info.companyInfo.beneficiaries[0].applicantId", equalTo(representativeId))
        .body("info.companyInfo.beneficiaries[0].positions", nullValue())
        .body("info.companyInfo.beneficiaries[0].type", equalTo(addRepresentativeModel.getType()));

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    // An alternative endpoint to get the address, but since there is no check on sandbox, it returns empty response.
    final List<Object> checks = SumSubHelper.getApplicantLatestCheck(applicantData.getId())
            .then()
            .statusCode(SC_OK)
            .extract()
            .jsonPath().get("checks");

    assertEquals(0, checks.size());

   //We are checking if the identity use the proper merchant token based on country
   final String corporateSubscriptionRequest = getCorporatesV3ApiRequest(createCorporateModel.getCompany().getName());
   final JsonObject requestPayload = new Gson().fromJson(corporateSubscriptionRequest, JsonObject.class);

   assertEquals(requestPayload.get("merchant").getAsString(), merchantToken);
   ensureIdentitySubscribed(corporate.getLeft());
  }

  @Test
  public void Corporate_VerifyCorporateSoleTraderLevel_Success() throws SQLException {

    final CompanyType companyType = CompanyType.SOLE_TRADER;
    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    final SumSubCompanyInfoModel sumSubCompanyInfo = applicantData.getInfo().getCompanyInfo();
    final CompanyInfoModel companyInfoModel =
        CompanyInfoModel.builder()
            .setCompanyName(sumSubCompanyInfo.getCompanyName())
            .setRegistrationNumber(sumSubCompanyInfo.getRegistrationNumber())
            .setCountry(sumSubCompanyInfo.getCountry())
            .setLegalAddress("MLT")
            .setPhone(sumSubCompanyInfo.getPhone())
            .setType("TestType")
            .setTaxId("Tax1234")
            .setWebsite("https://test.io")
            .setAddress(new CompanyAddressModel("Test", "MLT001"))
            .build();

    SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), companyInfoModel,
        applicantData.getId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK)
        .body("info.companyInfo.type", equalTo(companyInfoModel.getType()))
        .body("info.companyInfo.taxId", equalTo(companyInfoModel.getTaxId()))
        .body("info.companyInfo.website", equalTo(companyInfoModel.getWebsite()));

    assertCompanyInfo(response, companyInfoModel);

    SumSubHelper.uploadRequiredDocuments(Collections.singletonList(INFORMATION_STATEMENT),
        weavrIdentity.getAccessToken(), applicantData.getId());

    final String industry = "art-galleries";
    final String sourceOfFunds = "dividends";
    final String regNumQuestionnaires = RandomStringUtils.randomAlphanumeric(10);
    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateSoleTraderQuestionnaire
                    (applicantData.getId(), industry, sourceOfFunds, regNumQuestionnaires));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
        applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    final Map<String, String> corporateInfo = CorporatesDatabaseHelper.getCorporate(corporate.getLeft()).get(0);

    assertEquals(industry, corporateInfo.get("industry"));
    assertEquals(sourceOfFunds.toUpperCase(), corporateInfo.get("source_of_funds"));
    assertEquals(regNumQuestionnaires, corporateInfo.get("registration_number"));
  }

  /**
   *   Create applicants of type company_zero (shareholders) and check beneficiary mapping
   *   Main ticket: https://weavr-payments.atlassian.net/browse/DEV-5139
   */

  //Accepting driving licence as POI for UK corporate applicants
  @Test
  public void CorporateUkCitizenship_AllowedDrivingLicenceAsPOI_Success() {
    final CompanyType companyType = CompanyType.LLC;
    final String applicantLevel = "corporate-puk";

    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setCorporateApplicantLevelForIdentity(
            applicantLevel, programmeId, corporate.getLeft(), companyType.name());

    AdminService.createCorporateLevelConfiguration(setApplicantLevelModel, adminToken)
            .then()
            .statusCode(SC_OK);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
            SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                    .getParams();

    final SumSubApplicantDataModel applicantData =
            SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                            weavrIdentity.getExternalUserId())
                    .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
            SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
                    weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .then()
            .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
            CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
            SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
            applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
            AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
                    .setShareSize(50).build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
            applicantData.getId(),
            companyType.name(),
            addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    final AddBeneficiaryModel addRepresentativeModel =
            AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel)
                    .setShareSize(50).build();

    final JsonPath representative =
            SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                    addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentativePukLevel(companyType, representativeId, createCorporateModel,
            representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                    weavrIdentity.getExternalUserId()),
            beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveBeneficiaryPukLevel(beneficiaryId, beneficiaryExternalUserId, companyType);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());
  }

  @Test
  public void Corporate_VerifyCorporateDetailsUpdated_Success() throws SQLException {

    final CompanyType companyType = CompanyType.LLC;
    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
            corporateProfileId)
        .setCompany(CompanyModel.defaultCompanyModel()
            .setType(companyType.name()).build()).build();
    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
        createCorporateModel, secretKey);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final String applicantId =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class).getId();

    final CompanyInfoModel companyInfoModel =
        CompanyInfoModel.randomCompanyInfoModel().build();
    SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), companyInfoModel,
        applicantId);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
        CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
        SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantId);

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantId,
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantId)
    );

    final AddBeneficiaryModel addBeneficiaryModel =
        AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
            .build();
    SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantId, addBeneficiaryModel)
        .jsonPath().get("applicantId");

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.defaultAddRepresentativeModel().build();
    SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantId, addRepresentativeModel)
        .jsonPath().get("applicantId");

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    SumSubHelper.addAndApproveDirector(companyType, weavrIdentity.getAccessToken(), applicantId);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantId);
    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    final Map<String, String> updatedCorporateDetails = CorporatesDatabaseHelper.getCorporate(
        corporate.getLeft()).get(0);
    final Map<String, String> updatedCorporateAddress = CorporatesDatabaseHelper.getCorporateRegisteredAddress(
        corporate.getLeft()).get(0);
    assertEquals(companyInfoModel.getCompanyName(), updatedCorporateDetails.get("name"));
    assertEquals(companyInfoModel.getRegistrationNumber(),
        updatedCorporateDetails.get("registration_number"));
    assertEquals(companyInfoModel.getIncorporatedOn(),
        updatedCorporateDetails.get("incorporated_on"));
    assertEquals(companyInfoModel.getAddress().getTown(), updatedCorporateAddress.get("city"));
    assertEquals(companyInfoModel.getCountry(), updatedCorporateAddress.get("address_line1"));
    assertEquals(companyInfoModel.getLegalAddress(), updatedCorporateAddress.get("address_line1"));
  }

  @Test
  public void Corporate_VerifyCorporateSoleTraderLevelRepresentativeDoesNotMatchRootUser_PendingReview()
      throws InterruptedException {

    final CompanyType companyType = CompanyType.SOLE_TRADER;
    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);

    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final SumSubCompanyInfoModel sumSubCompanyInfo = applicantData.getInfo().getCompanyInfo();
    final CompanyInfoModel companyInfoModel =
        CompanyInfoModel.builder()
            .setCompanyName(sumSubCompanyInfo.getCompanyName())
            .setRegistrationNumber(sumSubCompanyInfo.getRegistrationNumber())
            .setCountry(sumSubCompanyInfo.getCountry())
            .setLegalAddress("MLT")
            .setPhone(sumSubCompanyInfo.getPhone())
            .setType("TestType")
            .setTaxId("Tax1234")
            .setWebsite("https://test.io")
            .setAddress(new CompanyAddressModel("Test", "MLT001"))
            .build();

    SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), companyInfoModel,
        applicantData.getId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK)
        .body("info.companyInfo.type", equalTo(companyInfoModel.getType()))
        .body("info.companyInfo.taxId", equalTo(companyInfoModel.getTaxId()))
        .body("info.companyInfo.website", equalTo(companyInfoModel.getWebsite()));

    assertCompanyInfo(response, companyInfoModel);

    SumSubHelper.uploadRequiredDocuments(Collections.singletonList(INFORMATION_STATEMENT),
        weavrIdentity.getAccessToken(), applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateSoleTraderQuestionnaire
                    (applicantData.getId(), "art-galleries", "dividends", "123456"));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.defaultAddRepresentativeModel().build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
        applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    TimeUnit.SECONDS.sleep(10);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    //This part checks the paynetics subscription status of identity
    final SubscriptionStatusPayneticsModel subscriptionStatus =
            opc.junit.helpers.adminnew.AdminHelper.getCorporateSubscriptionStatus(corporate.getLeft(),AdminService.loginAdmin());

    assertEquals(subscriptionStatus.getEntity().getIdentityId(), corporate.getLeft());
    assertEquals(subscriptionStatus.getEntity().getStatus(), "ON_GOING");
  }

  @Test
  public void Corporate_CityTooLongFromSumSub_Success() {

    final CompanyType companyType = CompanyType.SOLE_TRADER;
    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);

    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final SumSubCompanyInfoModel sumSubCompanyInfo = applicantData.getInfo().getCompanyInfo();
    final CompanyInfoModel companyInfoModel =
        CompanyInfoModel.builder()
            .setCompanyName(sumSubCompanyInfo.getCompanyName())
            .setRegistrationNumber(sumSubCompanyInfo.getRegistrationNumber())
            .setCountry(sumSubCompanyInfo.getCountry())
            .setLegalAddress("MLT")
            .setPhone(sumSubCompanyInfo.getPhone())
            .setType("TestType")
            .setTaxId("Tax1234")
            .setWebsite("https://test.io")
            .setAddress(new CompanyAddressModel(RandomStringUtils.randomAlphabetic(30), "MLT001"))
            .build();

    SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), companyInfoModel,
        applicantData.getId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK)
        .body("info.companyInfo.type", equalTo(companyInfoModel.getType()))
        .body("info.companyInfo.taxId", equalTo(companyInfoModel.getTaxId()))
        .body("info.companyInfo.website", equalTo(companyInfoModel.getWebsite()));

    assertCompanyInfo(response, companyInfoModel);

    SumSubHelper.uploadRequiredDocuments(Collections.singletonList(INFORMATION_STATEMENT),
        weavrIdentity.getAccessToken(), applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateSoleTraderQuestionnaire
                    (applicantData.getId(), "art-galleries", "dividends", "123456"));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
        applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    CorporatesService.getCorporates(secretKey, corporate.getRight())
        .then()
        .statusCode(SC_OK);
  }

  @Test
  public void Corporate_ResendDirectorKycEmail_Success() {

    final CreateCorporateModel createCorporateModel = createCorporateModel(CompanyType.LLC);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);

    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
        CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
        SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
        AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
            .build();


        final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
                applicantData.getId(),
                CompanyType.LLC.name(),
                addBeneficiaryModel);
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            CompanyType.LLC.name()));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.setRepresentativeInInitiatedState(CompanyType.LLC, representativeId,
        createCorporateModel, representativeExternalUserId);
    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            CompanyType.LLC.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId()),
        beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(CompanyType.LLC, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    CompanyType.LLC.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    final GetCorporateBeneficiariesModel getCorporateBeneficiariesModel =
        InnovatorHelper.getCorporateBeneficiaries(innovatorToken, corporate.getLeft());

    InnovatorService.getBeneficiaryKycUrl(innovatorToken, corporate.getLeft(),
            getCorporateBeneficiariesModel.getUbo().get(0).getId())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("BENEFICIARY_NOT_DIRECTOR"));

    InnovatorService.resendBeneficiaryKycEmail(innovatorToken, corporate.getLeft(),
            getCorporateBeneficiariesModel.getUbo().get(0).getId())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("BENEFICIARY_NOT_DIRECTOR"));

    InnovatorService.getBeneficiaryKycUrl(innovatorToken, corporate.getLeft(),
            getCorporateBeneficiariesModel.getDirector().get(0).getId())
        .then()
        .statusCode(SC_OK)
        .body("kycUrl", notNullValue());

    InnovatorService.resendBeneficiaryKycEmail(innovatorToken, corporate.getLeft(),
            getCorporateBeneficiariesModel.getDirector().get(0).getId())
        .then()
        .statusCode(SC_NO_CONTENT);
    Assertions.assertNotNull(
        MailhogHelper.getMailHogEmail(createCorporateModel.getRootUser().getEmail()).getBody());

    SumSubHelper.setInitiatedRepresentativeInPendingState(CompanyType.LLC, representativeId,
        representativeExternalUserId);

    InnovatorService.getBeneficiaryKycUrl(innovatorToken, corporate.getLeft(),
            getCorporateBeneficiariesModel.getDirector().get(0).getId())
        .then()
        .statusCode(SC_OK)
        .body("kycUrl", notNullValue());

    InnovatorService.resendBeneficiaryKycEmail(innovatorToken, corporate.getLeft(),
            getCorporateBeneficiariesModel.getDirector().get(0).getId())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("KYC_STATUS_INVALID"));

    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, CompanyType.LLC);
    SumSubHelper.approvePendingRepresentative(representativeId, representativeExternalUserId);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    InnovatorService.getBeneficiaryKycUrl(innovatorToken, corporate.getLeft(),
            getCorporateBeneficiariesModel.getDirector().get(0).getId())
        .then()
        .statusCode(SC_OK)
        .body("kycUrl", notNullValue());

    InnovatorService.resendBeneficiaryKycEmail(innovatorToken, corporate.getLeft(),
            getCorporateBeneficiariesModel.getDirector().get(0).getId())
        .then()
        .statusCode(SC_CONFLICT)
        .body("errorCode", equalTo("KYC_STATUS_INVALID"));

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
        applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());
  }

  @Test
  public void Corporate_VerifyAuthenticatedUserKycCorporateApproved_Success() {

    final CompanyType companyType = CompanyType.LLC;
    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);

    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
        CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
        SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
        AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
            .build();

        final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
                applicantData.getId(),
                companyType.name(),
                addBeneficiaryModel);
        final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId()),
        beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
        applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    startAndApproveAuthenticatedUserKyc(createCorporateModel, corporate.getRight(), companyType);
  }

  @Test
  public void Corporate_VerifyAdminInitiatedAuthenticatedUserKycCorporateApproved_Success() {

    final CompanyType companyType = CompanyType.LLC;
    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);

    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
        CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
        SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
        AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
            .build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
            applicantData.getId(),
            companyType.name(),
            addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId()),
        beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
        applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    startByAdminAndApproveAuthenticatedUserKyc(corporate.getRight(), corporate.getLeft(),
        companyType);
  }

  /**
   * This test checks if updated information is retrieved from sumsub when beneficiary information
   * is updated between pending and approval callbacks. Assertions in the lines 1156-1161 check if
   * the information from sumsub matches with information on the database
   */
  @Test
  public void Corporate_UpdateInformationBeforeApproval_Success() throws SQLException {

    final CompanyType companyType = CompanyType.LLC;

    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
        weavrIdentity.getExternalUserId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
        CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
        SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
        AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
            .build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
        applicantData.getId(),
        companyType.name(),
        addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId()),
        beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    final FixedInfoModel updateInfoModel =
        FixedInfoModel.builder()
            .setFirstName(RandomStringUtils.randomAlphabetic(5))
            .setLastName(RandomStringUtils.randomAlphabetic(5))
            .build();

    final String accessToken = SumSubHelper.generateCorporateAccessToken(
        representativeExternalUserId, companyType.name());
    SumSubHelper.updateApplicantInformation(updateInfoModel, accessToken, representativeId);

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
        applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    Map<String, String> directorInfo = CorporatesDatabaseHelper.getBeneficiary(representativeExternalUserId).get(0);

    assertEquals(updateInfoModel.getFirstName(), directorInfo.get("first_name"));
    assertEquals(updateInfoModel.getLastName(), directorInfo.get("last_name"));
    assertEquals(addRepresentativeModel.getApplicant().getInfo().getPlaceOfBirth(),
        directorInfo.get("place_of_birth"));
    assertEquals(
        addRepresentativeModel.getApplicant().getInfo().getAddresses().get(0).getPostCode(),
        directorInfo.get("address_post_code"));
    assertEquals(addRepresentativeModel.getApplicant().getInfo().getAddresses().get(0).getState(),
        directorInfo.get("address_state"));
    assertEquals(addRepresentativeModel.getApplicant().getInfo().getAddresses().get(0).getTown(),
        directorInfo.get("address_city"));
  }

  @Test
  public void Corporate_BeneficiaryQuestionnaire_Success() throws SQLException {

    final CompanyType companyType=CompanyType.LLC;

    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = CorporatesHelper.createKybVerifiedCorporate(createCorporateModel, secretKey);

    startAndApproveAuthenticatedUserKyc(createCorporateModel, corporate.getRight(), companyType);

    final String beneficiaryId = InnovatorHelper.getCorporateBeneficiaries(
            InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), corporate.getLeft())
        .getDirector().get(0).getId();

    final String industry = CorporatesDatabaseHelper.getBeneficiaryAdditionalInfo("industry",
        beneficiaryId).get(0).get("property_value");
    final String pepcategory = CorporatesDatabaseHelper.getBeneficiaryAdditionalInfo("pepcategory",
        beneficiaryId).get(0).get("property_value");
    final String rcacategory = CorporatesDatabaseHelper.getBeneficiaryAdditionalInfo("rcacategory",
        beneficiaryId).get(0).get("property_value");
    final String declaration = CorporatesDatabaseHelper.getBeneficiaryAdditionalInfo("declaration",
        beneficiaryId).get(0).get("property_value");

    assertEquals("AUTO_AVIATION", industry);
    assertEquals("NOT_A_PEP", pepcategory);
    assertEquals("NOT_AN_RCA", rcacategory);
    assertEquals("true", declaration);
  }

  /**
   * With new changes, a company(legal entity) can be added as other director for a corporate
   * This test adds a company as director to a corporate and checks the information
   */
  @Test
  public void Corporate_AddLegalEntityDirectorAndPerformAmlCheck_Success() throws SQLException {

    final CompanyType companyType = CompanyType.LLC;
    final CreateCorporateModel createCorporateModel = createCorporateModel(CompanyType.LLC);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
        weavrIdentity.getExternalUserId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
            weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
        .then()
        .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
        SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
        SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
        AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
            .build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
        applicantData.getId(),
        companyType.name(),
        addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId()),
        beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    //_______________________________________
    // add a company as director
    final AddLegalEntityDirectorModel addLegalEntityDirectorModel =
        AddLegalEntityDirectorModel.defaultAddLegalEntityDirectorModel();

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    final JsonPath beneficiaryOtherDirector = SumSubHelper.addLegalEntityDirector(
        weavrIdentity.getAccessToken(),
        applicantData.getId(),
        addLegalEntityDirectorModel).jsonPath();

    final String otherDirectorBeneficiaryId = beneficiaryOtherDirector.get("applicantId");
    final String otherDirectorExternalUserId = beneficiaryOtherDirector.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    SumSubHelper.setApplicantInApprovedState(otherDirectorExternalUserId, otherDirectorBeneficiaryId);
    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
        applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    // We retrieve director's data from database and check if matches with provided info

    final Map<String, String> legalEntityDirector = BeneficiaryDatabaseHelper.findByCorporateId(
        corporate.getLeft()).get(2);

    final LegalEntityDirectorInfoModel companyDirectorInfo = addLegalEntityDirectorModel.getApplicant()
        .getInfo().getCompanyInfo();

    assertEquals(companyDirectorInfo.getCompanyName(), legalEntityDirector.get("company_name"));
    assertEquals(companyDirectorInfo.getCountry(), legalEntityDirector.get("country"));
    assertEquals(companyDirectorInfo.getRegistrationNumber(), legalEntityDirector.get("registration_number"));
    assertEquals("APPROVED", legalEntityDirector.get("status"));
    assertEquals("OTHER_DIRECTOR", legalEntityDirector.get("beneficiary_type"));
  }

  /**
   * With new improvements of onboarding process corporates the subscription documents have to be checked based on type of company
   * This test checks the behavior if not all documents were uploaded.
   */

  @ParameterizedTest
  @EnumSource(value = CompanyType.class, names = {"LLC", "PUBLIC_LIMITED_COMPANY", "LIMITED_LIABILITY_PARTNERSHIP"})
  public void CorporateOnboarding_CheckSubscriptionDocsDifferentCorporateType_Conflict(final CompanyType companyType) {

    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
            SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                    .getParams();

    final SumSubApplicantDataModel applicantData =
            SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                            weavrIdentity.getExternalUserId())
                    .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
            SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    final ValidatableResponse response = SumSubHelper.getApplicantData(
                    weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .then()
            .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = List.of(SELFIE);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
            applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addRepresentativeModel =
            AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
            SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                    addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
            representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .then()
            .statusCode(SC_OK)
            .body("info.companyInfo.beneficiaries[0].applicantId", equalTo(representativeId))
            .body("info.companyInfo.beneficiaries[0].positions", nullValue())
            .body("info.companyInfo.beneficiaries[0].type", equalTo(addRepresentativeModel.getType()));

    SumSubHelper.addAndApproveDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubService.setApplicantInPendingState(weavrIdentity.getAccessToken(), applicantData.getId())
            .then()
            .statusCode(SC_CONFLICT)
            .body("description", equalTo("Not all required documents are submitted. Make sure to upload all the documents beforehand."));

  }

  @Test
  public void CorporateOnboarding_CheckSubscriptionDocsSoleTraderCompanyType_Conflict() {

    final CompanyType companyType = CompanyType.SOLE_TRADER;
    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);

    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
            SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                    .getParams();

    final SumSubApplicantDataModel applicantData =
            SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                            weavrIdentity.getExternalUserId())
                    .as(SumSubApplicantDataModel.class);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    final SumSubCompanyInfoModel sumSubCompanyInfo = applicantData.getInfo().getCompanyInfo();
    final CompanyInfoModel companyInfoModel =
            CompanyInfoModel.builder()
                    .setCompanyName(sumSubCompanyInfo.getCompanyName())
                    .setRegistrationNumber(sumSubCompanyInfo.getRegistrationNumber())
                    .setCountry(sumSubCompanyInfo.getCountry())
                    .setLegalAddress("MLT")
                    .setPhone(sumSubCompanyInfo.getPhone())
                    .setType("TestType")
                    .setTaxId("Tax1234")
                    .setWebsite("https://test.io")
                    .setAddress(new CompanyAddressModel("Test", "MLT001"))
                    .build();

    SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), companyInfoModel,
            applicantData.getId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
                    weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .then()
            .statusCode(SC_OK)
            .body("info.companyInfo.type", equalTo(companyInfoModel.getType()))
            .body("info.companyInfo.taxId", equalTo(companyInfoModel.getTaxId()))
            .body("info.companyInfo.website", equalTo(companyInfoModel.getWebsite()));

    assertCompanyInfo(response, companyInfoModel);

    SumSubHelper.uploadRequiredDocuments(Collections.singletonList(SELFIE),
            weavrIdentity.getAccessToken(), applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addRepresentativeModel =
            AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
            SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                    addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
            representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubService.setApplicantInPendingState(weavrIdentity.getAccessToken(), applicantData.getId())
            .then()
            .statusCode(SC_CONFLICT)
            .body("description", equalTo("Not all required documents are submitted. Make sure to upload all the documents beforehand."));
  }

  @Test
  public void CorporateOnboarding_CheckSubscriptionDocsNonProfitOrganisation_Conflict() {

    final CompanyType companyType = CompanyType.NON_PROFIT_ORGANISATION;

    final CreateCorporateModel createCorporateModel =
            CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                    .setCompany(CompanyModel
                            .defaultCompanyModel()
                            .setRegistrationCountry((CountryCode.DE).name())
                            .setType(CompanyType.NON_PROFIT_ORGANISATION.name())
                            .build())
                    .build();
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
            SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                    .getParams();

    final SumSubApplicantDataModel applicantData =
            SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                            weavrIdentity.getExternalUserId())
                    .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
            SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    final ValidatableResponse response = SumSubHelper.getApplicantData(
                    weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .then()
            .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = List.of(COMPANY_POA);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
            applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addRepresentativeModel =
            AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
            SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                    addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
            representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .then()
            .statusCode(SC_OK)
            .body("info.companyInfo.beneficiaries[0].applicantId", equalTo(representativeId))
            .body("info.companyInfo.beneficiaries[0].positions", nullValue())
            .body("info.companyInfo.beneficiaries[0].type", equalTo(addRepresentativeModel.getType()));

    SumSubHelper.addAndApproveDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubService.setApplicantInPendingState(weavrIdentity.getAccessToken(), applicantData.getId())
            .then()
            .statusCode(SC_CONFLICT)
            .body("description", equalTo("Not all required documents are submitted. Make sure to upload all the documents beforehand."));

  }

  @Test
  public void Corporate_ChecksSipStatusOfBeneficiaries_Success() {

    final CompanyType companyType = CompanyType.LLC;
    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
            SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                    .getParams();

    final SumSubApplicantDataModel applicantData =
            SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                            weavrIdentity.getExternalUserId())
                    .as(SumSubApplicantDataModel.class);

    SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId());

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
            CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
            SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
            applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
            AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
                    .setShareSize(50).build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
            applicantData.getId(),
            companyType.name(),
            addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    final AddBeneficiaryModel addRepresentativeModel =
            AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel)
                    .setShareSize(50).build();

    final JsonPath representative =
            SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                    addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.setRepresentativeInPendingState(companyType, representativeId, createCorporateModel,
            representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                    weavrIdentity.getExternalUserId()),
            beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.setApplicantInApprovedState(representativeExternalUserId, representativeId);
    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    // Verify if the sip status of beneficiaries is null on DB
    verifySipStatus(representativeExternalUserId, "NO", "NO");
    verifySipStatus(beneficiaryExternalUserId, "NO", "NO");
    verifySipStatus(director.getRight(), "NO", "NO");

    // Retrieve rejection callback from sumsub with COMPROMISED_PERSONS label for all beneficiaries

    final List<String> rejectLabels = Arrays.asList(RejectLabels.COMPROMISED_PERSONS.name());
    SumSubHelper
            .setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL,
                    Optional.of("Issue with verification."), representativeId, representativeExternalUserId);

    SumSubHelper
            .setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL,
                    Optional.of("Issue with verification."), beneficiaryId, beneficiaryExternalUserId);

    SumSubHelper
            .setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL,
                    Optional.of("Issue with verification."), director.getLeft(), director.getRight());

    // Verify if the sip status of beneficiaries is null on DB
    verifySipStatus(representativeExternalUserId, "YES", "YES");
    verifySipStatus(beneficiaryExternalUserId, "YES", "YES");
    verifySipStatus(director.getRight(), "YES", "YES");
  }

  @Test
  public void Corporate_SetEddCountries_Rejected() {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(nonFpsEnabledTenant.getCorporatesProfileId())
            .setCompany(CompanyModel.defaultCompanyModel()
                    .setType(CompanyType.LLC.name()).build())
            .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                    .setMobile(new MobileNumberModel("+356",
                            String.format("79%s", RandomStringUtils.randomNumeric(6))))
                    .build())
            .build();

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, nonFpsEnabledTenant.getSecretKey());
    final String kybReferenceId = CorporatesHelper.startKyb(nonFpsEnabledTenant.getSecretKey(), corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
            SumSubHelper.getWeavrIdentityDetails(nonFpsEnabledTenant.getSharedKey(), corporate.getRight(), kybReferenceId)
                    .getParams();

    final SumSubApplicantDataModel applicantData =
            SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                            weavrIdentity.getExternalUserId())
                    .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
            SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
                    weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .then()
            .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
            CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
            SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
            applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
            AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
                    .setShareSize(50).build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
            applicantData.getId(),
            CompanyType.LLC.name(),
            addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    CompanyType.LLC.name()));

    final AddBeneficiaryModel addRepresentativeModel =
            AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel)
                    .setShareSize(50).build();

    final JsonPath representative =
            SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                    addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(CompanyType.LLC, representativeId, createCorporateModel,
            representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    CompanyType.LLC.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                    weavrIdentity.getExternalUserId()),
            beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(CompanyType.LLC, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    CompanyType.LLC.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, CompanyType.LLC);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.REJECTED.name());
  }

  @Test
  public void Corporate_SetEddCountriesEddRequiredThenApprovedFromSumsub_Approved() {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(nonFpsEnabledTenant.getCorporatesProfileId())
            .setCompany(CompanyModel.defaultCompanyModel()
                    .setType(CompanyType.LLC.name()).build())
            .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                    .setMobile(new MobileNumberModel("+356",
                            String.format("79%s", RandomStringUtils.randomNumeric(6))))
                    .build())
            .build();

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, nonFpsEnabledTenant.getSecretKey());
    final String kybReferenceId = CorporatesHelper.startKyb(nonFpsEnabledTenant.getSecretKey(), corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
            SumSubHelper.getWeavrIdentityDetails(nonFpsEnabledTenant.getSharedKey(), corporate.getRight(), kybReferenceId)
                    .getParams();

    final SumSubApplicantDataModel applicantData =
            SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                            weavrIdentity.getExternalUserId())
                    .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
            SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
                    weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .then()
            .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
            CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
            SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
            applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
            AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
                    .setShareSize(50).build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
            applicantData.getId(),
            CompanyType.LLC.name(),
            addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    CompanyType.LLC.name()));

    final AddBeneficiaryModel addRepresentativeModel =
            AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel)
                    .setShareSize(50).build();

    final JsonPath representative =
            SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                    addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(CompanyType.LLC, representativeId, createCorporateModel,
            representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    CompanyType.LLC.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                    weavrIdentity.getExternalUserId()),
            beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(CompanyType.LLC, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    CompanyType.LLC.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, CompanyType.LLC);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.REJECTED.name());

    //add applicant tag as an EDD_Approved
    SumSubService.addApplicantTag(applicantData.getId(), "EDD_Approved")
            .then()
            .statusCode(SC_OK);

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());
  }

  @Test
  public void Corporate_EddCountriesNotSetEddApprovedFromSumsub_Approved() {

    final CompanyType companyType = CompanyType.LLC;
    final CreateCorporateModel createCorporateModel = createCorporateModel(companyType);
    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
            SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                    .getParams();

    final SumSubApplicantDataModel applicantData =
            SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                            weavrIdentity.getExternalUserId())
                    .as(SumSubApplicantDataModel.class);

    final CompanyInfoModel companyInfoModel =
            SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
            weavrIdentity.getExternalUserId());

    final ValidatableResponse response = SumSubHelper.getApplicantData(
                    weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
            .then()
            .statusCode(SC_OK);

    assertCompanyInfo(response, companyInfoModel);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
            CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
            SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
            applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
            SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addBeneficiaryModel =
            AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
                    .setShareSize(50).build();

    final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
            applicantData.getId(),
            companyType.name(),
            addBeneficiaryModel);
    final String beneficiaryId = beneficiary.get("applicantId");
    final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    final AddBeneficiaryModel addRepresentativeModel =
            AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel)
                    .setShareSize(50).build();

    final JsonPath representative =
            SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                    addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
            representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    assertCompanyBeneficiaries(SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                    weavrIdentity.getExternalUserId()),
            beneficiaryId, addBeneficiaryModel, representativeId, addRepresentativeModel);

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
    SumSubHelper.approveDirector(director.getLeft(), director.getRight());

    //add applicant tag as an EDD_Approved
    SumSubService.addApplicantTag(applicantData.getId(), "EDD_Approved")
            .then()
            .statusCode(SC_OK);

    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
    SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    assertTrue(ensureIdentitySubscribed(corporate.getLeft()));
  }

  private void startAndApproveAuthenticatedUserKyc(
      final CreateCorporateModel createCorporateModel,
      final String corporateToken,
      final CompanyType companyType) {

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    final Pair<String, String> user =
        UsersHelper.createEnrolledUser(usersModel, secretKey, corporateToken);

    final String kycReferenceId = UsersHelper.startUserKyc(secretKey, user.getRight());

    final IdentityDetailsModel weavrUserIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, user.getRight(), kycReferenceId)
            .getParams();

    final SumSubAuthenticatedUserDataModel applicantData =
        SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(),
                weavrUserIdentity.getExternalUserId())
            .as(SumSubAuthenticatedUserDataModel.class);

    SumSubHelper.approveRepresentative(companyType, applicantData.getId(), createCorporateModel,
        weavrUserIdentity.getExternalUserId(), UBO_DIRECTOR_QUESTIONNAIRE_ID);

  }

  private void startByAdminAndApproveAuthenticatedUserKyc(final String corporateToken,
      final String corporateId,
      final CompanyType companyType) {

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    final Pair<String, String> user =
        UsersHelper.createEnrolledUser(usersModel, secretKey, corporateToken);

    final String kycReferenceId = AdminHelper.startCorporateUserKyc(corporateId, user.getLeft(),
        AdminService.loginAdmin());

    final IdentityDetailsModel weavrUserIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, user.getRight(), kycReferenceId)
            .getParams();

    final SumSubAuthenticatedUserDataModel applicantData =
        SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(),
                weavrUserIdentity.getExternalUserId())
            .as(SumSubAuthenticatedUserDataModel.class);

    SumSubHelper.approveRepresentative(companyType, applicantData.getId(),
        weavrUserIdentity.getExternalUserId(), CORPORATE_QUESTIONNAIRE_ID);
  }

  private Pair<String, String> createAuthenticatedCorporate(
      final CreateCorporateModel createCorporateModel) {
    return CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
  }

  private Pair<String, String> createAuthenticatedCorporate(final CreateCorporateModel createCorporateModel,
                                                            final String secretKey) {
    return CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
  }

  private CreateCorporateModel createCorporateModel(final CompanyType companyType) {

    return CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
        .setCompany(CompanyModel.defaultCompanyModel()
            .setType(companyType.name()).build())
        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
            .setMobile(new MobileNumberModel("+356",
                String.format("79%s", RandomStringUtils.randomNumeric(6))))
            .build())
        .build();

  }

  /**
   * This method checks if company info from sumsub matches with provided info when creating
   * corporate
   */
  private void assertCompanyInfo(final ValidatableResponse response,
      final CompanyInfoModel companyInfoModel) {

    response
        .body("info.companyInfo.companyName", equalTo(companyInfoModel.getCompanyName()))
        .body("info.companyInfo.registrationNumber",
            equalTo(companyInfoModel.getRegistrationNumber()))
        .body("info.companyInfo.country", equalTo(companyInfoModel.getCountry()))
        .body("info.companyInfo.legalAddress", equalTo(companyInfoModel.getLegalAddress()))
        .body("info.companyInfo.phone", equalTo(companyInfoModel.getPhone()))
        .body("info.companyInfo.address.town", equalTo(companyInfoModel.getAddress().getTown()))
        .body("review.reviewStatus", equalTo("init"));
  }

  /**
   * This method checks if company beneficiaries info from sumsub matches with provided info when
   * creating beneficiary
   */
  private void assertCompanyBeneficiaries(final Response response,
      final String beneficiaryId,
      final AddBeneficiaryModel addBeneficiaryModel,
      final String representativeId,
      final AddBeneficiaryModel addRepresentativeModel) {

    response
        .then()
        .statusCode(SC_OK)
        .body("info.companyInfo.beneficiaries[0].applicantId", equalTo(beneficiaryId))
        .body("info.companyInfo.beneficiaries[0].positions[0]",
            equalTo(addBeneficiaryModel.getPositions().get(0)))
        .body("info.companyInfo.beneficiaries[0].type", equalTo(addBeneficiaryModel.getType()))
        .body("info.companyInfo.beneficiaries[1].applicantId", equalTo(representativeId))
        .body("info.companyInfo.beneficiaries[1].positions", nullValue())
        .body("info.companyInfo.beneficiaries[1].type", equalTo(addRepresentativeModel.getType()));
  }

  /**
   * This method creates a corporate with a director and perform KYB/KYC process
   * @return director info as rootUserModel in order to be used for new created corporates
   */
  private Builder createKycVerifiedDirector(){

    final CompanyType companyType = CompanyType.PUBLIC_LIMITED_COMPANY;

    final Builder rootUserModel = CorporateRootUserModel.DefaultRootUserModel();

    final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
        .setCompany(CompanyModel.defaultCompanyModel()
            .setType(companyType.name()).build())
        .setRootUser(rootUserModel
            .setMobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
            .build())
        .build();

    final Pair<String, String> corporate = createAuthenticatedCorporate(createCorporateModel);
    final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

    final IdentityDetailsModel weavrIdentity =
        SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
            .getParams();

    final SumSubApplicantDataModel applicantData =
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId())
            .as(SumSubApplicantDataModel.class);

    SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

    final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
        CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION);
    SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
        applicantData.getId());

    SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
        SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
    );

    final AddBeneficiaryModel addRepresentativeModel =
        AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

    final JsonPath representative =
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
            addRepresentativeModel).jsonPath();
    final String representativeId = representative.get("applicantId");
    final String representativeExternalUserId = representative.get("applicant.externalUserId");

    SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
        representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

    weavrIdentity.setAccessToken(
        SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
            companyType.name()));

    final Pair<String, String> director =
            SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

    weavrIdentity.setAccessToken(
            SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                    companyType.name()));

    SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

    SumSubHelper.approveDirector(director.getLeft(), director.getRight());
    SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());

    CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

    return rootUserModel;
  }

  public void verifySipStatus(final String externalReferenceId,
                              final String sipEffective,
                              final String sipOngoing) {

    TestHelper.ensureDatabaseResultAsExpected(30,
            () -> CorporatesDatabaseHelper.getBeneficiary(externalReferenceId),
            x -> x.size() > 0 && x.get(0).get("sip_effective").equals(sipEffective) && x.get(0).get("sip_ongoing").equals(sipOngoing),
            Optional.of(String.format("Subscription for identity with id %s not '%s'", externalReferenceId, sipEffective)));
  }

  private static Stream<Arguments> merchantTokenProvider() {
    return Stream.of(
            arguments(CountryCode.DE, PAYNETICS_EEA_TOKEN, applicationOne),
            arguments(CountryCode.GB, PAYNETICS_UK_TOKEN, applicationOneUk),
            arguments(CountryCode.IM, PAYNETICS_UK_TOKEN, applicationOneUk)
    );
  }
}

