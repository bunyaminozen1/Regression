package opc.junit.sumsub;

import opc.enums.opc.CompanyType;
import opc.enums.opc.KybState;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static opc.enums.sumsub.IdDocType.ARTICLES_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.CERTIFICATE_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.COMPANY_POA;
import static opc.enums.sumsub.IdDocType.INFORMATION_STATEMENT;
import static opc.enums.sumsub.IdDocType.SHAREHOLDER_REGISTRY;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.SUMSUB_CORPORATE)
public class SumsubDownloadKybReportTests extends BaseSumSubSetup {

    private static String adminToken;
    private static String innovatorToken;

    @BeforeAll
    public static void Setup(){

        adminToken = AdminService.loginAdmin();
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    }

    @Test
    public void DownloadReport_CorporateInitiatedRetry_Success() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath().get("applicantId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.defaultAddRepresentativeModel().build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath().get("applicantId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        SumSubHelper.addAndApproveDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.RETRY,
                        Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.INITIATED.name());

        AdminService.downloadKybScreeningReport(corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));

        InnovatorService.downloadKybScreeningReport(corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));
    }

    @Test
    public void DownloadReport_CorporateNotStarted_KybStatusNotInitiatedRetry() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        AdminService.downloadKybScreeningReport(corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYB_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));

        InnovatorService.downloadKybScreeningReport(corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYB_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));
    }

    @Test
    public void DownloadReport_CorporateInitiated_Success() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.startKyb(secretKey, corporate.getRight());

        AdminService.downloadKybScreeningReport(corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));

        InnovatorService.downloadKybScreeningReport(corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));
    }

    @Test
    public void DownloadReport_CorporatePendingReview_KybStatusNotInitiatedRetry() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath().get("applicantId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.defaultAddRepresentativeModel().build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath().get("applicantId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        SumSubHelper.addAndApproveDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        AdminService.downloadKybScreeningReport(corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYB_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));

        InnovatorService.downloadKybScreeningReport(corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYB_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));
    }

    @Test
    public void DownloadReport_CorporateApproved_KybStatusNotInitiatedRetry() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        AdminService.downloadKybScreeningReport(corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYB_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));

        InnovatorService.downloadKybScreeningReport(corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYB_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));
    }

    @Test
    public void DownloadReport_CorporateRejected_KybStatusNotInitiatedRetry() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath().get("applicantId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.defaultAddRepresentativeModel().build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath().get("applicantId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        SumSubHelper.addAndApproveDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.EXTERNAL, Optional.of("Issue with verification."),
                        applicantData.getId(), weavrIdentity.getExternalUserId());

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.REJECTED.name());

        AdminService.downloadKybScreeningReport(corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYB_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));

        InnovatorService.downloadKybScreeningReport(corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYB_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));
    }

    @Test
    public void DownloadReport_CorporateGetKycReport_NotFound() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        AdminService.downloadKycScreeningReport(corporate.getLeft(), adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        InnovatorService.downloadKycScreeningReport(corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DownloadReport_UnknownCorporate_NotFound() {

        AdminService.downloadKybScreeningReport(RandomStringUtils.randomNumeric(18), adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        InnovatorService.downloadKybScreeningReport(RandomStringUtils.randomNumeric(18), innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }
}
