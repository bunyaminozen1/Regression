package fpi.paymentrun.webhooks;

import commons.models.MobileNumberModel;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.AdminUserModel;
import fpi.paymentrun.models.CompanyModel;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.webhook.BuyerBeneficiaryStatusEventModel;
import fpi.paymentrun.models.webhook.PluginWebhookKybEventModel;
import io.restassured.path.json.JsonPath;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.CompanyType;
import opc.enums.opc.KybState;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.CompanyInfoModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static opc.enums.sumsub.IdDocType.ARTICLES_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.CERTIFICATE_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.COMPANY_POA;
import static opc.enums.sumsub.IdDocType.INFORMATION_STATEMENT;
import static opc.enums.sumsub.IdDocType.SHAREHOLDER_REGISTRY;
import static opc.junit.helpers.webhook.WebhookHelper.getPluginWebhookServiceEvent;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SumSubKybWebhooksTests extends BaseWebhooksSetup {

    @Test
    public void SumSubWebhooks_BuyerApproved_StateApproved() {
        final CompanyType companyType = CompanyType.LLC;
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .type(companyType.name()).build())
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        final String kybReference = BuyersHelper.startKyb(secretKey, buyer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, buyer.getRight(), kybReference).getParams();
        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());
        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId()));

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        final JsonPath beneficiary = SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createBuyerModel).build();
        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");
        SumSubHelper.approveRepresentative(companyType, representativeId, createBuyerModel, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());
        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        createBuyerModel.getCompany().getType()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.PENDING_REVIEW.name());

        final PluginWebhookKybEventModel eventPendingState = getKybWebhookResponse(pendingStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventPendingState,
                KybState.PENDING_REVIEW, Optional.empty(), Optional.empty());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.APPROVED.name());

        final PluginWebhookKybEventModel eventApprovedState = getKybWebhookResponse(approvedStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventApprovedState,
                KybState.APPROVED, Optional.empty(), Optional.empty());
    }

    @Test
    public void SumSubWebhooks_BeneficiaryDirector_PendingState() {
        final CompanyType companyType = CompanyType.LLC;
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .type(companyType.name()).build())
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        final String kybReference = BuyersHelper.startKyb(secretKey, buyer.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, buyer.getRight(), kybReference).getParams();
        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);
        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());
        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId()));

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createBuyerModel).build();
        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");
        SumSubHelper.approveRepresentative(companyType, representativeId, createBuyerModel, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addDirectorModel =
                AddBeneficiaryModel.defaultAddDirectorModel().build();
        final JsonPath director =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addDirectorModel).jsonPath();
        final String directorId = director.get("applicantId");
        final String directorExternalUserId = director.get("applicant.externalUserId");
        final String directorAccessToken = SumSubHelper.generateCorporateAccessToken(directorExternalUserId, companyType.name());
        SumSubHelper.submitConsent(directorAccessToken, directorId);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                createBuyerModel.getCompany().getType()));

//        Set applicant in Pending state
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.PENDING_REVIEW.name());

        final PluginWebhookKybEventModel kybEventPendingState = getKybWebhookResponse(pendingStateTimestamp, buyer.getLeft());
        final BuyerBeneficiaryStatusEventModel eventBeneficiaryState = getBeneficiaryStateWebhookResponse(pendingStateTimestamp, buyer.getLeft());

        assertKybEvent("buyerKYBWatch", buyer.getLeft(), kybEventPendingState,
                KybState.PENDING_REVIEW, Optional.empty(), Optional.empty());
        assertBeneficiaryStateEvent("buyerBeneficiaryStatusWatch", buyer.getLeft(), director, createBuyerModel,
                eventBeneficiaryState, KybState.PENDING_REVIEW, KybState.PENDING_REVIEW, "OTHER_DIRECTOR", addBeneficiaryModel.getApplicant().getEmail(), Optional.empty(), Optional.empty());
    }

    @Test
    public void SumSubWebhooks_BeneficiaryDirector_StateApproved() {
        final CompanyType companyType = CompanyType.LLC;
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .type(companyType.name()).build())
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        final String kybReference = BuyersHelper.startKyb(secretKey, buyer.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, buyer.getRight(), kybReference).getParams();
        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);
        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());
        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId()));

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createBuyerModel).build();
        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");
        SumSubHelper.approveRepresentative(companyType, representativeId, createBuyerModel, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addDirectorModel =
                AddBeneficiaryModel.defaultAddDirectorModel().build();
        final JsonPath director =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addDirectorModel).jsonPath();
        final String directorId = director.get("applicantId");
        final String directorExternalUserId = director.get("applicant.externalUserId");
        final String directorAccessToken = SumSubHelper.generateCorporateAccessToken(directorExternalUserId,
                companyType.name());
        SumSubHelper.submitConsent(directorAccessToken, directorId);

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        createBuyerModel.getCompany().getType()));

//        Set applicant in Pending state
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.PENDING_REVIEW.name());

//        Approve director
        SumSubHelper.approveDirector(directorId, directorExternalUserId);
        final long approvedStateTimestamp = Instant.now().toEpochMilli();

        final BuyerBeneficiaryStatusEventModel eventApprovedState = getBeneficiaryStateWebhookResponse(approvedStateTimestamp, buyer.getLeft());
        assertBeneficiaryStateEvent("buyerBeneficiaryStatusWatch", buyer.getLeft(), director, createBuyerModel,
                eventApprovedState, KybState.PENDING_REVIEW, KybState.APPROVED, "OTHER_DIRECTOR", addBeneficiaryModel.getApplicant().getEmail(), Optional.empty(), Optional.empty());
    }

    @Test
    public void SumSubWebhooks_BeneficiaryUBO_StateApproved() {
        final CompanyType companyType = CompanyType.LLC;
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .type(companyType.name()).build())
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        final String kybReference = BuyersHelper.startKyb(secretKey, buyer.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, buyer.getRight(), kybReference).getParams();
        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);
        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());
        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId()));

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        final JsonPath beneficiary = SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createBuyerModel).build();
        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");
        SumSubHelper.approveRepresentative(companyType, representativeId, createBuyerModel, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addDirectorModel =
                AddBeneficiaryModel.defaultAddDirectorModel().build();
        final JsonPath director =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addDirectorModel).jsonPath();
        final String directorId = director.get("applicantId");
        final String directorExternalUserId = director.get("applicant.externalUserId");
        final String directorAccessToken = SumSubHelper.generateCorporateAccessToken(directorExternalUserId,
                companyType.name());
        SumSubHelper.submitConsent(directorAccessToken, directorId);

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        createBuyerModel.getCompany().getType()));
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

//        Approve Beneficiary
        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        final long approvedBeneficiaryStateTimestamp = Instant.now().toEpochMilli();

        final BuyerBeneficiaryStatusEventModel eventApprovedBeneficiaryState = getBeneficiaryStateWebhookResponse(approvedBeneficiaryStateTimestamp, buyer.getLeft());
        assertBeneficiaryStateEvent("buyerBeneficiaryStatusWatch", buyer.getLeft(), beneficiary, createBuyerModel,
                eventApprovedBeneficiaryState, KybState.PENDING_REVIEW, KybState.APPROVED, "UBO", addBeneficiaryModel.getApplicant().getEmail(), Optional.empty(), Optional.empty());
    }

    @Test
    public void SumSubWebhooks_BuyerPendingReview_StatePendingReview() {
        final CompanyType companyType = CompanyType.LLC;
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .type(companyType.name()).build())
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        final String kybReference = BuyersHelper.startKyb(secretKey, buyer.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, buyer.getRight(), kybReference).getParams();
        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);
        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());
        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId()));

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createBuyerModel).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        createBuyerModel.getCompany().getType()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.PENDING_REVIEW.name());

        final PluginWebhookKybEventModel eventPendingState = getKybWebhookResponse(pendingStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventPendingState,
                KybState.PENDING_REVIEW, Optional.empty(), Optional.empty());
    }

    @Test
    public void SumSubWebhooks_BuyerRejectedRetry_StateInitiated() {
        final CompanyType companyType = CompanyType.LLC;
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .type(companyType.name()).build())
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        final String kybReference = BuyersHelper.startKyb(secretKey, buyer.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, buyer.getRight(), kybReference).getParams();
        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);
        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());
        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId()));

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createBuyerModel).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        SumSubHelper.addAndApproveDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        createBuyerModel.getCompany().getType()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.PENDING_REVIEW.name());

        final PluginWebhookKybEventModel eventPendingState = getKybWebhookResponse(pendingStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventPendingState,
                KybState.PENDING_REVIEW, Optional.empty(), Optional.empty());

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper.setApplicantInRejectState(rejectLabels, ReviewRejectType.RETRY,
                Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());
        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.INITIATED.name());

        final PluginWebhookKybEventModel eventRejectedState = getKybWebhookResponse(rejectedStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventRejectedState,
                KybState.INITIATED, Optional.of(List.of("REPRESENTATIVE_DETAILS_UNSATISFACTORY", "DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Issue with verification."));
    }

    @ParameterizedTest
    @EnumSource(value = ReviewRejectType.class, names = {"EXTERNAL", "FINAL"})
    public void SumSubWebhooks_BuyerRejectedExternalAndFinal_StateRejected(final ReviewRejectType rejectType) {
        final CompanyType companyType = CompanyType.LLC;
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .type(companyType.name()).build())
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        final String kybReference = BuyersHelper.startKyb(secretKey, buyer.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, buyer.getRight(), kybReference).getParams();
        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);
        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());
        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId()));

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        final JsonPath beneficiary = SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createBuyerModel).build();
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
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.PENDING_REVIEW.name());

        final PluginWebhookKybEventModel eventPendingState = getKybWebhookResponse(pendingStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventPendingState,
                KybState.PENDING_REVIEW, Optional.empty(), Optional.empty());

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
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.REJECTED.name());

        AdminService.getCorporate(AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()), buyer.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true));

        final PluginWebhookKybEventModel eventRejectedState = getKybWebhookResponse(rejectedStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventRejectedState,
                KybState.REJECTED, Optional.of(List.of("REPRESENTATIVE_DETAILS_UNSATISFACTORY", "DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Issue with verification."));
    }

    @Test
    public void SumSubWebhooks_BuyerRejected_UnsupportedCountry() {
        final CompanyType companyType = CompanyType.LLC;
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .type(companyType.name()).build())
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        final String kybReference = BuyersHelper.startKyb(secretKey, buyer.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, buyer.getRight(), kybReference).getParams();
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
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId()));

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        final JsonPath beneficiary = SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createBuyerModel).build();
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
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.PENDING_REVIEW.name());

        final PluginWebhookKybEventModel eventPendingState = getKybWebhookResponse(pendingStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventPendingState,
                KybState.PENDING_REVIEW, Optional.empty(), Optional.empty());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.REJECTED.name());

        final PluginWebhookKybEventModel eventRejectedState = getKybWebhookResponse(rejectedStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventRejectedState,
                KybState.REJECTED, Optional.of(List.of("UNSUPPORTED_COUNTRY")),
                Optional.empty());
    }

    @Test
    public void SumSubWebhooks_BuyerRootUserDoesNotMatchRepresentative_StatePendingReview() {
        final CompanyType companyType = CompanyType.LLC;
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel()
                        .company(CompanyModel.gbCompanyModel()
                                .type(companyType.name()).build())
                        .adminUser(AdminUserModel.defaultAdminUserModel()
                                .mobile(new MobileNumberModel("+356", String.format("79%s", RandomStringUtils.randomNumeric(6))))
                                .build())
                        .build();
        final Pair<String, String> buyer = BuyersHelper.createEnrolledBuyer(createBuyerModel, secretKey);
        final String kybReference = BuyersHelper.startKyb(secretKey, buyer.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, buyer.getRight(), kybReference).getParams();
        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);
        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());
        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId()));

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
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.PENDING_REVIEW.name());

        final PluginWebhookKybEventModel eventPendingState = getKybWebhookResponse(pendingStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventPendingState,
                KybState.PENDING_REVIEW, Optional.of(List.of("ADMIN_USER_UNMATCHED_TO_DIRECTOR_OR_REPRESENTATIVE")),
                Optional.empty());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));
        BuyersHelper.verifyKybStatus(secretKey, buyer.getRight(), KybState.PENDING_REVIEW.name());

        final PluginWebhookKybEventModel eventPendingStateAfterApproval = getKybWebhookResponse(approvedStateTimestamp, buyer.getLeft());
        assertKybEvent("buyerKYBWatch", buyer.getLeft(), eventPendingStateAfterApproval,
                KybState.PENDING_REVIEW, Optional.of(List.of("ADMIN_USER_UNMATCHED_TO_DIRECTOR_OR_REPRESENTATIVE")),
                Optional.empty());
    }

    private PluginWebhookKybEventModel getKybWebhookResponse(final long timestamp,
                                                             final String identityId) {
        return (PluginWebhookKybEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "buyerKYBWatch",
                Pair.of("data.buyerId", identityId),
                PluginWebhookKybEventModel.class,
                ApiSchemaDefinition.KybEvent);
    }

    private BuyerBeneficiaryStatusEventModel getBeneficiaryStateWebhookResponse(final long timestamp,
                                                                                final String identityId) {
        return (BuyerBeneficiaryStatusEventModel) getPluginWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                "buyerBeneficiaryStatusWatch",
                Pair.of("data.additionalInformation.buyerId", identityId),
                BuyerBeneficiaryStatusEventModel.class,
                ApiSchemaDefinition.BeneficiariesStateEvent);
    }

    private void assertKybEvent(final String type,
                                final String identityId,
                                final PluginWebhookKybEventModel kybEvent,
                                final KybState kybState,
                                final Optional<List<String>> details,
                                final Optional<String> rejectionComment) {

        assertEquals(type, kybEvent.getType());
        assertEquals(identityId, kybEvent.getData().getBuyerId());
        assertEquals(details.orElse(new ArrayList<>()).size(),
                Arrays.asList(kybEvent.getData().getDetails()).size());
        assertEquals(rejectionComment.orElse(""), kybEvent.getData().getRejectionComment());
        assertEquals(kybState.name(), kybEvent.getData().getStatus());
        assertEquals(kybState.name(), kybEvent.getData().getOngoingStatus());

        if (details.isPresent() && details.get().size() > 0) {
            details.get().forEach(eventDetail ->
                    assertEquals(eventDetail,
                            Arrays.stream(kybEvent.getData().getDetails())
                                    .filter(x -> x.equals(eventDetail))
                                    .findFirst()
                                    .orElse(String.format("Details did not match. Expected %s Actual %s",
                                            eventDetail, Arrays.asList(kybEvent.getData().getDetails()).size() == 0 ? "[No details returned from sumsub]" :
                                                    String.join(", ", kybEvent.getData().getDetails())))));
        }
    }

    private void assertBeneficiaryStateEvent(final String type,
                                             final String buyerId,
                                             final JsonPath director,
                                             final CreateBuyerModel buyerModel,
                                             final BuyerBeneficiaryStatusEventModel beneficiaryEvent,
                                             final KybState kybState,
                                             final KybState beneficiaryKybState,
                                             final String beneficiaryType,
                                             final String beneficiaryEmail,
                                             final Optional<List<String>> details,
                                             final Optional<String> rejectionComment) {

        assertEquals(type, beneficiaryEvent.getType());
        assertEquals(buyerModel.getAdminUser().getEmail(), beneficiaryEvent.getData().getAdditionalInformation().getAdminUserEmail());
        assertEquals(buyerId, beneficiaryEvent.getData().getAdditionalInformation().getBuyerId());
        assertEquals(buyerModel.getCompany().getName(), beneficiaryEvent.getData().getAdditionalInformation().getBuyerName());
        assertEquals(kybState.name(), beneficiaryEvent.getData().getAdditionalInformation().getKybStatus());
        assertEquals(director.get("applicant.info.firstName"), beneficiaryEvent.getData().getAdditionalInformation().getBeneficiary().getFirstName());
        assertEquals(director.get("applicant.info.lastName"), beneficiaryEvent.getData().getAdditionalInformation().getBeneficiary().getLastName());
        assertEquals(beneficiaryKybState.name(), beneficiaryEvent.getData().getAdditionalInformation().getBeneficiary().getStatus());
        assertEquals(beneficiaryKybState.name(), beneficiaryEvent.getData().getAdditionalInformation().getBeneficiary().getOngoingKybStatus());
        assertEquals(beneficiaryType, beneficiaryEvent.getData().getAdditionalInformation().getBeneficiary().getType());
        assertEquals(details.orElse(new ArrayList<>()).size(),
                Arrays.asList(beneficiaryEvent.getData().getEventDetails()).size());
        assertEquals(rejectionComment.orElse(null), beneficiaryEvent.getData().getRejectionComment());
        assertEquals("STATUS_UPDATED", beneficiaryEvent.getData().getEvent().get(0));

        if (details.isPresent() && details.get().size() > 0) {
            details.get().forEach(eventDetail ->
                    assertEquals(eventDetail,
                            Arrays.stream(beneficiaryEvent.getData().getEventDetails())
                                    .filter(x -> x.equals(eventDetail))
                                    .findFirst()
                                    .orElse(String.format("Details did not match. Expected %s Actual %s",
                                            eventDetail, Arrays.asList(beneficiaryEvent.getData().getEventDetails()).size() == 0 ? "[No details returned from sumsub]" :
                                                    String.join(", ", beneficiaryEvent.getData().getEventDetails())))));
        }
    }
}
