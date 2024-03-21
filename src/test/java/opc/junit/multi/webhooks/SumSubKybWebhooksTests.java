package opc.junit.multi.webhooks;

import commons.models.CompanyModel;
import commons.models.MobileNumberModel;
import io.restassured.path.json.JsonPath;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.CompanyType;
import opc.enums.opc.KybState;
import opc.enums.opc.KycState;
import opc.enums.opc.WebhookType;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.enums.sumsub.SumSubApplicantState;
import opc.junit.database.SumsubDatabaseHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.admin.UpdateCorporateProfileModel;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.CompanyInfoModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.models.sumsub.SumSubAuthenticatedUserDataModel;
import opc.models.sumsub.SumSubCompanyInfoModel;
import opc.models.sumsub.questionnaire.custom.QuestionnaireRequest;
import opc.models.webhook.WebhookKybBeneficiaryEventModel;
import opc.models.webhook.WebhookKybEventModel;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static opc.enums.opc.CountryCode.getAllEeaCountries;
import static opc.enums.sumsub.IdDocType.ARTICLES_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.CERTIFICATE_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.COMPANY_POA;
import static opc.enums.sumsub.IdDocType.INFORMATION_STATEMENT;
import static opc.enums.sumsub.IdDocType.SHAREHOLDER_REGISTRY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(MultiTags.SUMSUB_CORPORATE_WEBHOOKS)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public class SumSubKybWebhooksTests extends BaseWebhooksSetup {

    @Test
    public void Corporate_CorporatePendingReview_CorporateStatePendingReview() {

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
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath().get("applicantId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final WebhookKybEventModel event = getWebhookResponse(pendingStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                event, KybState.PENDING_REVIEW,
                Optional.empty(), Optional.empty());
    }

    @Test
    public void Corporate_CorporateApproved_CorporateStateApproved() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
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

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        final JsonPath beneficiary = SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final WebhookKybEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventPendingState, KybState.PENDING_REVIEW,
                Optional.empty(), Optional.empty());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

        final WebhookKybEventModel eventApprovedState = getWebhookResponse(approvedStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventApprovedState, KybState.APPROVED,
                Optional.empty(), Optional.empty());
    }

    @Test
    public void Corporate_CorporateRejectedRetry_CorporateStateInitiated() {

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
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath().get("applicantId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        SumSubHelper.addAndApproveDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final WebhookKybEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventPendingState, KybState.PENDING_REVIEW,
                Optional.empty(), Optional.empty());

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.RETRY,
                        Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());
        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.INITIATED.name());

        final WebhookKybEventModel eventRejectedState = getWebhookResponse(rejectedStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventRejectedState, KybState.INITIATED,
                Optional.of(List.of("REPRESENTATIVE_DETAILS_UNSATISFACTORY", "DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Issue with verification."));
    }

    @ParameterizedTest
    @EnumSource(value = ReviewRejectType.class, names = {"EXTERNAL", "FINAL"})
    public void Corporate_CorporateRejectedExternalAndFinal_CorporateStateRejected(final ReviewRejectType rejectType) {

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
        final JsonPath beneficiary = SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();
        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(companyType, representativeId, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final WebhookKybEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventPendingState, KybState.PENDING_REVIEW,
                Optional.empty(), Optional.empty());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, rejectType, Optional.of("Issue with verification."),
                        applicantData.getId(), weavrIdentity.getExternalUserId());
        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.REJECTED.name());

        AdminService.getCorporate(AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()), corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true));

        final WebhookKybEventModel eventRejectedState = getWebhookResponse(rejectedStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventRejectedState, KybState.REJECTED,
                Optional.of(List.of("REPRESENTATIVE_DETAILS_UNSATISFACTORY", "DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Issue with verification."));
    }

    @Test
    public void Corporate_CorporateRejected_UnsupportedCountry() {

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

        final CompanyInfoModel companyInfoModel =
                CompanyInfoModel.defaultCompanyInfoModel(applicantData.getInfo().getCompanyInfo())
                        .setCountry("MSR")
                        .build();

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), companyInfoModel, applicantData.getId());

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        final JsonPath beneficiary = SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();
        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(companyType, representativeId, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final WebhookKybEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventPendingState, KybState.PENDING_REVIEW,
                Optional.empty(), Optional.empty());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.REJECTED.name());

        final WebhookKybEventModel eventRejectedState = getWebhookResponse(rejectedStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventRejectedState, KybState.REJECTED,
                Optional.of(List.of("UNSUPPORTED_COUNTRY")), Optional.empty());
    }

    @Test
    public void Corporate_WithoutRegisteredCountryInProgrammeLevel_Rejected(){
        final CompanyType companyType = CompanyType.PUBLIC_LIMITED_COMPANY;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name())
                        .setRegistrationCountry("DE")
                        .build())
                .build();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        setRegistrationCountriesOnProgrammeLevel(UpdateProgrammeModel.builder().setCountry(List.of("MT", "IT", "BE")).build());

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
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.REJECTED.name());

        final WebhookKybEventModel eventRejectedState = getWebhookResponse(rejectedStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventRejectedState, KybState.REJECTED,
                Optional.of(List.of("UNSUPPORTED_COUNTRY")), Optional.empty());
    }

    @Test
    public void Corporate_WithoutRegisteredCountryInProfileLevel_Rejected(){
        final CompanyType companyType = CompanyType.PUBLIC_LIMITED_COMPANY;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name())
                        .setRegistrationCountry("DE")
                        .build())
                .build();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        setRegistrationCountriesOnProfileLevel(UpdateCorporateProfileModel.builder().setAllowedCountries(List.of("MT", "IT", "BE")).build());

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
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.REJECTED.name());

        final WebhookKybEventModel eventRejectedState = getWebhookResponse(rejectedStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventRejectedState, KybState.REJECTED,
                Optional.of(List.of("UNSUPPORTED_COUNTRY")), Optional.empty());
    }

    @Test
    public void Corporate_CorporateRootUserDoesNotMatchRepresentative_CorporateStatePendingReview() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
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

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        final JsonPath beneficiary = SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.defaultAddRepresentativeModel().build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(companyType, representativeId, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final WebhookKybEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventPendingState, KybState.PENDING_REVIEW,
                Optional.of(List.of("ROOT_USER_UNMATCHED_TO_DIRECTOR_OR_REPRESENTATIVE")),
                Optional.empty());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final WebhookKybEventModel eventPendingStateAfterApproval = getWebhookResponse(approvedStateTimestamp,
                corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(),
                eventPendingStateAfterApproval, KybState.PENDING_REVIEW,
                Optional.of(List.of("ROOT_USER_UNMATCHED_TO_DIRECTOR_OR_REPRESENTATIVE")),
                Optional.empty());
    }

    @Test
    public void Corporate_VerifyAuthenticatedUserKybCorporateApproved_Success() {

        final CompanyType companyType = CompanyType.LLC;

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
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

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("info.companyInfo.companyName", equalTo(companyInfoModel.getCompanyName()))
                .body("info.companyInfo.registrationNumber", equalTo(companyInfoModel.getRegistrationNumber()))
                .body("info.companyInfo.country", equalTo(companyInfoModel.getCountry()))
                .body("info.companyInfo.legalAddress", equalTo(companyInfoModel.getLegalAddress()))
                .body("info.companyInfo.phone", equalTo(companyInfoModel.getPhone()))
                .body("info.companyInfo.address.town", equalTo(companyInfoModel.getAddress().getTown()))
                .body("review.reviewStatus", equalTo("init"));

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();

        final JsonPath beneficiary =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("info.companyInfo.beneficiaries[0].applicantId", equalTo(beneficiaryId))
                .body("info.companyInfo.beneficiaries[0].positions[0]", equalTo(addBeneficiaryModel.getPositions().get(0)))
                .body("info.companyInfo.beneficiaries[0].type", equalTo(addBeneficiaryModel.getType()))
                .body("info.companyInfo.beneficiaries[1].applicantId", equalTo(representativeId))
                .body("info.companyInfo.beneficiaries[1].positions", nullValue())
                .body("info.companyInfo.beneficiaries[1].type", equalTo(addRepresentativeModel.getType()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final String benAccessToken = SumSubHelper.generateCorporateAccessToken(beneficiaryExternalUserId, companyType.name());
        SumSubHelper.uploadQuestionnaire(benAccessToken, beneficiaryId, QuestionnaireRequest.builder()
                .id(beneficiaryId)
                .questionnaires(SumSubHelper.addQuestionnaire(UBO_DIRECTOR_QUESTIONNAIRE_ID, Map.ofEntries(
                        SumSubHelper.addSection("section1", SumSubHelper.addItems(Map.ofEntries(
                                SumSubHelper.addItem("industry", "ACCOUNTING_AUDIT_FINANCE")
                        ))),
                        SumSubHelper.addSection("pep", SumSubHelper.addItems(Map.ofEntries(
                                SumSubHelper.addItem("pepcategory", "NOT_A_PEP"),
                                SumSubHelper.addItem("nolongerpep", null),
                                SumSubHelper.addItem("rcacategory", "NOT_AN_RCA"),
                                SumSubHelper.addItem("nolongerrca", null)
                        ))),
                        SumSubHelper.addSection("declarationsection", SumSubHelper.addItems(Map.ofEntries(
                                SumSubHelper.addItem("declaration", "true")
                        )))
                )))
                .build());
        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

        final long timestamp = Instant.now().toEpochMilli();
        final UsersModel userInfo = startAndApproveAuthenticatedUserKyc(corporate.getRight(), companyType);

        final List<WebhookKybBeneficiaryEventModel> beneficiaryEvents = getWebhookResponses(timestamp, corporate.getLeft(), 2);
        final WebhookKybBeneficiaryEventModel beneficiaryPendingEvent =
                beneficiaryEvents.stream().filter(x -> x.getAdditionalInformation().getBeneficiary().getOngoingKybStatus().equals("PENDING_REVIEW")).collect(Collectors.toList()).get(0);
        final WebhookKybBeneficiaryEventModel beneficiaryApprovedEvent =
                beneficiaryEvents.stream().filter(x -> x.getAdditionalInformation().getBeneficiary().getOngoingKybStatus().equals("APPROVED")).collect(Collectors.toList()).get(0);

        assertBeneficiaryEvent(beneficiaryPendingEvent, userInfo, corporate.getLeft(), createCorporateModel, KycState.PENDING_REVIEW, KybState.APPROVED);
        assertBeneficiaryEvent(beneficiaryApprovedEvent, userInfo, corporate.getLeft(), createCorporateModel, KycState.APPROVED, KybState.APPROVED);

    }
    @Test
    public void Corporate_BeneficiaryWorkflowIgnoreApprovalCallbackWithoutWorkflowCompleted_Success() throws SQLException {
        final CompanyType companyType = CompanyType.SOLE_TRADER;

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
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
        final CompanyInfoModel companyInfoModel = CompanyInfoModel.defaultCompanyInfoModel(sumSubCompanyInfo)
                .setTaxId(RandomStringUtils.randomNumeric(10)).build();

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), companyInfoModel,
                applicantData.getId());

        SumSubHelper.uploadRequiredDocuments(Collections.singletonList(INFORMATION_STATEMENT),
                weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateSoleTraderQuestionnaire
                        (applicantData.getId(), "industry", "dividends", RandomStringUtils.randomAlphanumeric(10)));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.setRepresentativeInPendingState(companyType, representativeId, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final Map<String, String> beneficiaryBeforeApproval = SumsubDatabaseHelper.getBeneficiary(representativeId).get(0);
        assertEquals("1", beneficiaryBeforeApproval.get("workflow_enable"));
        assertEquals(KycState.PENDING_REVIEW.name(), beneficiaryBeforeApproval.get("status"));

        SumSubHelper.setApplicantInApprovedState(representativeExternalUserId, representativeId);

        //We ignore the callback because workflow is not completed yet
        final Map<String, String> beneficiaryAfterApproval = SumsubDatabaseHelper.getBeneficiary(representativeId).get(0);
        assertEquals(KycState.PENDING_REVIEW.name(), beneficiaryAfterApproval.get("status"));

        final long timestamp = Instant.now().toEpochMilli();

        //Simulate callback with type "applicantWorkflowCompleted", status on DB is updated and a webhook sent to embedder
        SumSubHelper.setWorkflowApplicantInApprovedState(representativeExternalUserId);

        final Map<String, String> beneficiaryAfterWorkflowCompleted = SumsubDatabaseHelper.getBeneficiary(representativeId).get(0);
        assertEquals("0", beneficiaryAfterWorkflowCompleted.get("workflow_enable"));
        assertEquals(KycState.APPROVED.name(), beneficiaryAfterWorkflowCompleted.get("status"));

        final List<WebhookKybBeneficiaryEventModel> beneficiaryEvents = getWebhookResponses(timestamp, corporate.getLeft(), 1);
        final WebhookKybBeneficiaryEventModel beneficiaryApprovedEvent =
                beneficiaryEvents.stream().filter(x -> x.getAdditionalInformation().getBeneficiary().getOngoingKybStatus().equals("APPROVED")).collect(Collectors.toList()).get(0);

        final UsersModel beneficiary = UsersModel.builder()
                .setName(addRepresentativeModel.getApplicant().getInfo().getFirstName())
                .setSurname(addRepresentativeModel.getApplicant().getInfo().getLastName())
                .setEmail(addRepresentativeModel.getApplicant().getEmail())
                .build();

        //Beneficiary state should be APPROVED, corporate state should be PENDING_REVIEW since it is not approved yet
        assertBeneficiaryEvent(beneficiaryApprovedEvent, beneficiary, corporate.getLeft(), createCorporateModel, KycState.APPROVED, KybState.PENDING_REVIEW);
    }

    @Test
    public void Corporate_BeneficiaryWorkflowIgnoreRejectionCallbackWithoutWorkflowCompleted_Success() throws SQLException {
        final CompanyType companyType = CompanyType.SOLE_TRADER;

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
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
        final CompanyInfoModel companyInfoModel = CompanyInfoModel.defaultCompanyInfoModel(sumSubCompanyInfo)
                .setTaxId(RandomStringUtils.randomNumeric(10)).build();

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), companyInfoModel,
                applicantData.getId());

        SumSubHelper.uploadRequiredDocuments(Collections.singletonList(INFORMATION_STATEMENT),
                weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateSoleTraderQuestionnaire
                        (applicantData.getId(), "industry", "dividends", RandomStringUtils.randomAlphanumeric(10)));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.setRepresentativeInPendingState(companyType, representativeId, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        final Map<String, String> beneficiaryBeforeApproval = SumsubDatabaseHelper.getBeneficiary(representativeId).get(0);
        assertEquals("1", beneficiaryBeforeApproval.get("workflow_enable"));
        assertEquals(KycState.PENDING_REVIEW.name(), beneficiaryBeforeApproval.get("status"));

        final List<String> rejectLabels = List.of(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL,
                        Optional.of("Issue with verification."), representativeId, representativeExternalUserId);

        //We ignore the callback because workflow is not completed yet
        final Map<String, String> beneficiaryAfterApproval = SumsubDatabaseHelper.getBeneficiary(representativeId).get(0);
        assertEquals(KycState.PENDING_REVIEW.name(), beneficiaryAfterApproval.get("status"));

        final long timestamp = Instant.now().toEpochMilli();

        //Simulate callback with type "applicantWorkflowCompleted", status on DB is updated and a webhook sent to embedder
        SumSubHelper.setApplicantInApprovedState(representativeExternalUserId, representativeId);
        SumSubHelper.setWorkflowApplicantInApprovedState(representativeExternalUserId);

        final Map<String, String> beneficiaryAfterWorkflowCompleted = SumsubDatabaseHelper.getBeneficiary(representativeId).get(0);
        assertEquals("0", beneficiaryAfterWorkflowCompleted.get("workflow_enable"));
        assertEquals(KycState.APPROVED.name(), beneficiaryAfterWorkflowCompleted.get("status"));

        final List<WebhookKybBeneficiaryEventModel> beneficiaryEvents = getWebhookResponses(timestamp, corporate.getLeft(), 1);
        final WebhookKybBeneficiaryEventModel beneficiaryApprovedEvent =
                beneficiaryEvents.stream().filter(x -> x.getAdditionalInformation().getBeneficiary().getOngoingKybStatus().equals("APPROVED")).collect(Collectors.toList()).get(0);

        final UsersModel beneficiary = UsersModel.builder()
                .setName(addRepresentativeModel.getApplicant().getInfo().getFirstName())
                .setSurname(addRepresentativeModel.getApplicant().getInfo().getLastName())
                .setEmail(addRepresentativeModel.getApplicant().getEmail())
                .build();

        //Beneficiary state should be APPROVED, corporate state should be PENDING_REVIEW since it is not approved yet
        assertBeneficiaryEvent(beneficiaryApprovedEvent, beneficiary, corporate.getLeft(), createCorporateModel, KycState.APPROVED, KybState.PENDING_REVIEW);
    }

    private UsersModel startAndApproveAuthenticatedUserKyc(final String corporateToken,
                                                           final CompanyType companyType) {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user =
                UsersHelper.createEnrolledUser(usersModel, secretKey, corporateToken);

        final String kycReferenceId = UsersHelper.startUserKyc(secretKey, user.getRight());

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, user.getRight(), kycReferenceId).getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(), weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.approveRepresentative(companyType, applicantData.getId(), weavrUserIdentity.getExternalUserId(), UBO_DIRECTOR_QUESTIONNAIRE_ID);

        return usersModel;
    }

    private void assertKybEvent(final String corporateId,
                                final String corporateEmail,
                                final WebhookKybEventModel event,
                                final KybState kybState,
                                final Optional<List<String>> details,
                                final Optional<String> rejectionComment) {

        assertEquals(corporateId, event.getCorporateId());
        assertEquals(corporateEmail, event.getCorporateEmail());
        assertEquals(details.orElse(new ArrayList<>()).size(),
                Arrays.asList(event.getDetails()).size());
        assertEquals(rejectionComment.orElse(""), event.getRejectionComment());
        assertEquals(kybState.name(), event.getStatus());
        assertEquals(kybState.name(), event.getOngoingStatus());

        if (details.isPresent() && details.get().size() > 0) {
            details.get().forEach(eventDetail ->
                    assertEquals(eventDetail,
                            Arrays.stream(event.getDetails())
                                    .filter(x -> x.equals(eventDetail))
                                    .findFirst()
                                    .orElse(String.format("Details did not match. Expected %s Actual %s",
                                            eventDetail, Arrays.asList(event.getDetails()).size() == 0 ? "[No details returned from sumsub]" :
                                                    String.join(", ", event.getDetails())))));
        }
    }

    private void assertBeneficiaryEvent(final WebhookKybBeneficiaryEventModel beneficiaryEvent,
                                        final UsersModel userInfo,
                                        final String corporateId,
                                        final CreateCorporateModel createCorporateModel,
                                        final KycState state,
                                        final KybState corporateState) {

        assertEquals(userInfo.getEmail(), beneficiaryEvent.getAdditionalInformation().getBeneficiary().getEmail());
        assertEquals(userInfo.getName(), beneficiaryEvent.getAdditionalInformation().getBeneficiary().getFirstName());
        assertEquals(userInfo.getSurname(), beneficiaryEvent.getAdditionalInformation().getBeneficiary().getLastName());
        assertEquals("", beneficiaryEvent.getAdditionalInformation().getBeneficiary().getMiddleName());
        assertEquals(state.name(), beneficiaryEvent.getAdditionalInformation().getBeneficiary().getOngoingKybStatus());
        assertEquals(state.name(), beneficiaryEvent.getAdditionalInformation().getBeneficiary().getStatus());
        assertEquals("DIRECTOR", beneficiaryEvent.getAdditionalInformation().getBeneficiary().getType());
        assertEquals(corporateId, beneficiaryEvent.getAdditionalInformation().getCorporateId());
        assertEquals(createCorporateModel.getCompany().getName(), beneficiaryEvent.getAdditionalInformation().getCorporateName());
        assertEquals(corporateState.name(), beneficiaryEvent.getAdditionalInformation().getKybStatus());
        assertEquals(createCorporateModel.getRootUser().getEmail(), beneficiaryEvent.getAdditionalInformation().getRootUserEmail());
        assertEquals("STATUS_UPDATED", beneficiaryEvent.getEvent()[0]);
        assertEquals("", beneficiaryEvent.getRejectionComment());
        assertEquals(0, beneficiaryEvent.getEventDetails().length);
    }

    private WebhookKybEventModel getWebhookResponse(final long timestamp,
                                                    final String identityId) {
        return (WebhookKybEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.CORPORATE_KYB,
                Pair.of("corporateId", identityId),
                WebhookKybEventModel.class,
                ApiSchemaDefinition.KybEvent);
    }

    private List<WebhookKybBeneficiaryEventModel> getWebhookResponses(final long timestamp,
                                                                      final String identityId,
                                                                      final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.CORPORATE_BENEFICIARY_KYB,
                Pair.of("additionalInformation.corporateId", identityId),
                WebhookKybBeneficiaryEventModel.class,
                ApiSchemaDefinition.BeneficiaryVerifiedEvent,
                expectedEventCount);
    }

    private static void setRegistrationCountriesOnProgrammeLevel(final UpdateProgrammeModel updateProgrammeModel) {

        AdminService.updateProgramme(updateProgrammeModel, programmeId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);
    }

    private static void setRegistrationCountriesOnProfileLevel(final UpdateCorporateProfileModel updateCorporateProfileModel) {

        AdminService.updateCorporateProfile(updateCorporateProfileModel, adminImpersonatedTenantToken, programmeId, corporateProfileId)
                .then()
                .statusCode(SC_OK);
    }

    @AfterEach
    private void resetResidentialCountries() {
        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder()
                .setCountry(getAllEeaCountries())
                .setHasCountry(true)
                .build();

        setRegistrationCountriesOnProgrammeLevel(updateProgrammeModel);

        final UpdateCorporateProfileModel updateProfileModel = UpdateCorporateProfileModel.builder()
                .setAllowedCountries(getAllEeaCountries())
                .setHasAllowedCountries(true)
                .build();

        setRegistrationCountriesOnProfileLevel(updateProfileModel);
    }
}
