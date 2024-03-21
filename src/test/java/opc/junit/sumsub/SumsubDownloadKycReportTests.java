package opc.junit.sumsub;

import opc.enums.opc.CountryCode;
import opc.enums.opc.KycLevel;
import opc.enums.opc.KycState;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.AddressModel;
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
import java.util.List;
import java.util.Optional;

import static opc.enums.sumsub.IdDocType.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.SUMSUB_CONSUMER)
public class SumsubDownloadKycReportTests extends BaseSumSubSetup {

    private static String adminToken;
    private static String innovatorToken;

    @BeforeAll
    public static void Setup(){

        adminToken = AdminService.loginAdmin();
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    }

    @Test
    public void DownloadReport_ConsumerInitiatedRetry_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.RETRY,
                        Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.INITIATED.name());

        AdminService.downloadKycScreeningReport(consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));

        InnovatorService.downloadKycScreeningReport(consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));
    }

    @Test
    public void DownloadReport_ConsumerNotStarted_KycStatusNotInitiatedRetry() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        AdminService.downloadKycScreeningReport(consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYC_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));

        InnovatorService.downloadKycScreeningReport(consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYC_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));
    }

    @Test
    public void DownloadReport_ConsumerInitiatedLevel1_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        AdminService.downloadKycScreeningReport(consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));

        InnovatorService.downloadKycScreeningReport(consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));
    }

    @Test
    public void DownloadReport_ConsumerInitiatedLevel2_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        ConsumersHelper.startKyc(secretKey, consumer.getRight());

        AdminService.downloadKycScreeningReport(consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));

        InnovatorService.downloadKycScreeningReport(consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body(containsString("PDF"));
    }

    @Test
    public void DownloadReport_ConsumerPendingReview_KycStatusNotInitiatedRetry() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        AdminService.downloadKycScreeningReport(consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYC_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));

        InnovatorService.downloadKycScreeningReport(consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYC_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));
    }

    @Test
    public void DownloadReport_ConsumerApproved_KycStatusNotInitiatedRetry() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        AdminService.downloadKycScreeningReport(consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYC_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));

        InnovatorService.downloadKycScreeningReport(consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYC_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));
    }

    @Test
    public void DownloadReport_ConsumerRejectedExternalAndFinal_ConsumerStateRejected() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL, Optional.of("Issue with verification."),
                        applicantData.getId(), weavrIdentity.getExternalUserId());

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        AdminService.downloadKycScreeningReport(consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYC_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));

        InnovatorService.downloadKycScreeningReport(consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body(equalTo("\"KYC_STATUS_NOT_INITIATED_OR_INITIATED_RETRY\""));
    }

    @Test
    public void DownloadReport_ConsumerGetKybReport_NotFound() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        AdminService.downloadKybScreeningReport(consumer.getLeft(), adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        InnovatorService.downloadKybScreeningReport(consumer.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DownloadReport_UnknownConsumer_NotFound() {

        AdminService.downloadKycScreeningReport(RandomStringUtils.randomNumeric(18), adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);

        InnovatorService.downloadKycScreeningReport(RandomStringUtils.randomNumeric(18), innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }
}
