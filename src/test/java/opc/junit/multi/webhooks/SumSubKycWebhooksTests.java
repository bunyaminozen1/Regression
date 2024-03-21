package opc.junit.multi.webhooks;

import commons.models.DateOfBirthModel;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.ConsumerApplicantLevel;
import opc.enums.opc.CountryCode;
import opc.enums.opc.KycLevel;
import opc.enums.opc.KycState;
import opc.enums.opc.WebhookType;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.admin.RegisteredCountriesContextModel;
import opc.models.admin.RegisteredCountriesDimension;
import opc.models.admin.RegisteredCountriesSetCountriesModel;
import opc.models.admin.RegisteredCountriesSetModel;
import opc.models.admin.RegisteredCountriesValue;
import opc.models.admin.RemoveDuplicateIdentityFlagModel;
import opc.models.admin.UpdateConsumerProfileModel;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.secure.SetIdentityDetailsModel;
import opc.models.shared.AddressModel;
import opc.models.sumsub.FixedInfoModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubAddressModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.models.webhook.WebhookKycEventModel;
import opc.services.admin.AdminService;
import opc.services.multi.ConsumersService;
import opc.services.secure.SecureService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import static opc.enums.sumsub.IdDocType.ID_CARD_BACK;
import static opc.enums.sumsub.IdDocType.ID_CARD_BACK_USER_IP;
import static opc.enums.sumsub.IdDocType.ID_CARD_FRONT;
import static opc.enums.sumsub.IdDocType.ID_CARD_FRONT_USER_IP;
import static opc.enums.sumsub.IdDocType.SELFIE;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL_USER_IP;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(MultiTags.SUMSUB_CONSUMER_WEBHOOKS)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public class SumSubKycWebhooksTests extends BaseWebhooksSetup {

    @BeforeAll
    public static void Setup() {

        final List<String> countries =
                Arrays.stream(CountryCode.values())
                        .filter(x -> !x.equals(CountryCode.AF))
                        .map(CountryCode::name)
                        .collect(Collectors.toList());

        RegisteredCountriesSetCountriesModel setCountriesModel = RegisteredCountriesSetCountriesModel.builder().context(
                        RegisteredCountriesContextModel.builder().dimension(
                                RegisteredCountriesDimension.defaultDimensionModel(innovatorId, programmeId)).build())
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel(countries)).build()).build();

        AdminService.setApprovedNationalitiesConsumers(adminToken, setCountriesModel).then()
                .statusCode(SC_NO_CONTENT);

        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder()
                .setCountry(getAllEeaCountries())
                .setHasCountry(true)
                .build();

        setResidentialCountriesOnProgrammeLevel(updateProgrammeModel);

        final UpdateConsumerProfileModel updateProfileModel = UpdateConsumerProfileModel.builder()
                .allowedCountries(getAllEeaCountries())
                .hasAllowedCountries(true)
                .build();

        setResidentialCountriesOnProfileLevel(updateProfileModel);
    }

    @Test
    public void Consumer_ConsumerApprovedDifferentLevelsFlow_ConsumerStateApproved() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

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

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingLevel1StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingLevel1State = getWebhookResponse(pendingLevel1StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingLevel1State, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_1, Optional.empty(), Optional.empty());

        final long approvedLevel1StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        final WebhookKycEventModel eventApprovedLevel1State = getWebhookResponse(approvedLevel1StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventApprovedLevel1State, KycState.APPROVED, KycState.APPROVED,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_1, Optional.empty(), Optional.empty());

        ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER));
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        final long pendingLevel2StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingLevel2State = getWebhookResponse(pendingLevel2StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingLevel2State, KycState.APPROVED, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        final long approvedLevel2StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());

        final WebhookKycEventModel eventApprovedLevel2State = getWebhookResponse(approvedLevel2StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventApprovedLevel2State, KycState.APPROVED, KycState.APPROVED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());
    }

    @Test
    public void Consumer_ConsumerApproved_ConsumerStateApproved() {

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
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        final WebhookKycEventModel eventApprovedState = getWebhookResponse(approvedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventApprovedState, KycState.APPROVED, KycState.APPROVED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());
    }

    @Test
    public void Consumer_ConsumerRejectedRetry_ConsumerStateInitiated() {

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
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

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

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.INITIATED.name());

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(rejectedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.INITIATED, KycState.INITIATED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2,
                Optional.of(List.of("OTHER", "DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Issue with verification."));
    }

    @ParameterizedTest
    @EnumSource(value = ReviewRejectType.class, names = {"EXTERNAL", "FINAL"})
    public void Consumer_ConsumerRejectedExternalAndFinal_ConsumerStateRejected(final ReviewRejectType rejectType) {

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
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        final List<String> rejectLabels = Collections.singletonList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, rejectType, Optional.of("Issue with verification."),
                        applicantData.getId(), weavrIdentity.getExternalUserId());
        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        AdminService.getConsumer(AdminService.loginAdmin(), consumer.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true));

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(rejectedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2,
                Optional.of(List.of("DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Issue with verification."));
    }

    @ParameterizedTest
    @EnumSource(value = ReviewRejectType.class, names = {"FINAL"})
    public void Consumer_ConsumerRejectedDifferentLevelFlow_ConsumerStateRejected(final ReviewRejectType rejectType) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

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

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingLevel1StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingLevel1State = getWebhookResponse(pendingLevel1StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingLevel1State, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_1, Optional.empty(), Optional.empty());

        final long approvedLevel1StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        final WebhookKycEventModel eventApprovedLevel1State = getWebhookResponse(approvedLevel1StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventApprovedLevel1State, KycState.APPROVED, KycState.APPROVED,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_1, Optional.empty(), Optional.empty());

        ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER));

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
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.APPROVED, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

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

        /* Due to the changes as part of DEV-6007, it was decided with product to
           update this test to NOT reject the identity on FINAL reject state but leave
           the states as they were. L1 APPROVED, L2 PENDING_REVIEW
         */
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(rejectedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.APPROVED, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_2,
                Optional.of(List.of("OTHER", "DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Issue with verification."));
    }

    @Test
    public void Consumer_ConsumerRejected_UnsupportedCountry() {

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

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.defaultFixedInfoModel(applicantData.getFixedInfo())
                        .setCountry("MSR")
                        .setAddresses(Collections.singletonList(SumSubAddressModel.randomAddressModelBuilder()
                                .setCountry("MSR")
                                .build()))
                        .build();

        SumSubHelper.submitApplicantInformation(fixedInfoModel, weavrIdentity.getAccessToken(), applicantData.getId());
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
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(rejectedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.of(List.of("UNSUPPORTED_COUNTRY")),
                Optional.empty());
    }

    @Test
    public void Consumer_UtilityBillDifferentCountry_KycRejected() {

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

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(approvedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.of(List.of("DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Additional documents required due to geolocation mismatch"));
    }

    @Test
    public void Consumer_CountryAddressDifferentCountry_KycRejected() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.FR)
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

        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(approvedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.of(List.of("DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Additional documents required due to geolocation mismatch"));
    }

    @Test
    public void Consumer_UtilityBillAndAddressDifferentCountry_KycRejected() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.FR)
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

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(approvedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.of(List.of("DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Additional documents required due to geolocation mismatch"));
    }

    @Test
    public void Consumer_ConsumerCountryDoesNotMatchIpDifferentLevelsFlow_KycRejected() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

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

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        final long pendingLevel1StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingLevel1State = getWebhookResponse(pendingLevel1StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingLevel1State, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_1, Optional.empty(), Optional.empty());

        final long approvedLevel1StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        final WebhookKycEventModel eventApprovedLevel1State = getWebhookResponse(approvedLevel1StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventApprovedLevel1State, KycState.APPROVED, KycState.APPROVED,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_1, Optional.empty(), Optional.empty());

        ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER));
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        final long pendingLevel2StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingLevel2State = getWebhookResponse(pendingLevel2StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingLevel2State, KycState.APPROVED, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        final long rejectedLevel2StateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel eventRejectedLevel2State = getWebhookResponse(rejectedLevel2StateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedLevel2State, KycState.APPROVED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_1, KycLevel.KYC_LEVEL_2, Optional.of(List.of("DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Additional documents required due to geolocation mismatch"));
    }

    @Test
    public void Consumer_VerifyConsumerNoAddressRequiredCountryDoesNotMatchIp_KycRejected() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.FR)
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

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId, new SetIdentityDetailsModel(true));
        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER_NO_POA));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(approvedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.of(List.of("DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Additional documents required due to geolocation mismatch"));
    }

    @Test
    public void Consumer_VerifyConsumerNoAddressRequiredIdCountryDoesNotMatchIp_KycRejected() {

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

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId, new SetIdentityDetailsModel(true));
        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER_NO_POA));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(approvedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.of(List.of("DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Additional documents required due to geolocation mismatch"));
    }

    @Test
    public void Consumer_VerifyConsumerNoAddressRequiredAddressCountryAndIdCountryDoesNotMatchIp_KycRejected() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.FR)
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

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId, new SetIdentityDetailsModel(true));
        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER_NO_POA));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel eventRejectedState = getWebhookResponse(approvedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventRejectedState, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.of(List.of("DOCUMENTS_UNSATISFACTORY")),
                Optional.of("Additional documents required due to geolocation mismatch"));
    }

    @Test
    public void Consumer_WithoutRegisteredCountryInProgrammeLevel_Rejected() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        setResidentialCountriesOnProgrammeLevel(UpdateProgrammeModel.builder().setCountry(List.of("IT", "BE")).build());

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

        final long timestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final List<WebhookKycEventModel> kycEvents = getWebhookResponses(timestamp, consumer.getLeft(), 2);
        final WebhookKycEventModel kycRejectedEvent =
                kycEvents.stream().filter(x -> x.getStatus().equals("REJECTED")).findFirst().orElseThrow();

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                kycRejectedEvent, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.of(List.of("UNSUPPORTED_COUNTRY")),
                Optional.empty());

        resetResidentialCountries();
    }

    @Test
    public void Consumer_WithoutRegisteredCountryInProfileLevel_Rejected() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        setResidentialCountriesOnProfileLevel(UpdateConsumerProfileModel.builder().allowedCountries(List.of("IT", "BE")).build());

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

        final long timestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final List<WebhookKycEventModel> kycEvents = getWebhookResponses(timestamp, consumer.getLeft(), 2);
        final WebhookKycEventModel kycRejectedEvent =
                kycEvents.stream().filter(x -> x.getStatus().equals("REJECTED")).findFirst().orElseThrow();

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                kycRejectedEvent, KycState.REJECTED, KycState.REJECTED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.of(List.of("UNSUPPORTED_COUNTRY")),
                Optional.empty());

        resetResidentialCountries();
    }

    @Test
    public void Consumer_WithRegisteredCountryOnProgrammeLevel_Approved() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        setResidentialCountriesOnProgrammeLevel(UpdateProgrammeModel.builder().setCountry(List.of("MT", "DE")).build());

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

        final long timestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        final List<WebhookKycEventModel> kycEvents = getWebhookResponses(timestamp, consumer.getLeft(), 2);
        final WebhookKycEventModel kycApprovedEvent =
                kycEvents.stream().filter(x -> x.getStatus().equals("APPROVED")).findFirst().orElseThrow();

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                kycApprovedEvent, KycState.APPROVED, KycState.APPROVED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(),
                Optional.empty());

        resetResidentialCountries();
    }

    @Test
    public void Consumer_WithRegisteredCountryOnProfileLevel_Approved() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        setResidentialCountriesOnProfileLevel(UpdateConsumerProfileModel.builder().allowedCountries(List.of("MT", "DE")).build());

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

        final long timestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        final List<WebhookKycEventModel> kycEvents = getWebhookResponses(timestamp, consumer.getLeft(), 2);
        final WebhookKycEventModel kycApprovedEvent =
                kycEvents.stream().filter(x -> x.getStatus().equals("APPROVED")).findFirst().orElseThrow();

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                kycApprovedEvent, KycState.APPROVED, KycState.APPROVED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(),
                Optional.empty());

        resetResidentialCountries();
    }

    @Test
    public void Consumer_SameProgrammeDuplicateIdentitiesFirstConsumerApproved_ConsumerStateRejected() throws SQLException {

        final CreateConsumerModel firstCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
        final String duplicateName = firstCreateConsumerModel.getRootUser().getName();
        final String duplicateSurname = firstCreateConsumerModel.getRootUser().getSurname();
        final DateOfBirthModel duplicateDob = firstCreateConsumerModel.getRootUser().getDateOfBirth();

        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final CreateConsumerModel secondCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(duplicateName)
                        .setSurname(duplicateSurname)
                        .setDateOfBirth(duplicateDob)
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, secondConsumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, secondConsumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel rejectedStateEvent = getWebhookResponse(rejectedStateTimestamp, secondConsumer.getLeft());

        assertEquals(secondConsumer.getLeft(), rejectedStateEvent.getConsumerId());
        assertEquals(secondCreateConsumerModel.getRootUser().getEmail(), rejectedStateEvent.getConsumerEmail());
        assertEquals(1, Arrays.asList(rejectedStateEvent.getDetails()).size());
        assertEquals("REJECTED_DUPLICATE", rejectedStateEvent.getDetails()[0]);
        assertEquals(String.format("%s %s with date of birth 1990-01-01 already exists on your programme.",
                duplicateName, duplicateSurname), rejectedStateEvent.getRejectionComment());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getKycLevel());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getOngoingKycLevel());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getStatus());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getOngoingStatus());

        final Map<String, String> duplicateIdentity = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);

        assertEquals(secondConsumer.getLeft(), duplicateIdentity.get("consumer_id"));
        assertEquals(firstConsumer.getLeft(), duplicateIdentity.get("duplicate_identity_id"));
        assertEquals(duplicateName, duplicateIdentity.get("name"));
        assertEquals(duplicateSurname, duplicateIdentity.get("surname"));
        assertEquals("1990-01-01", duplicateIdentity.get("date_of_birth"));
    }

    @Test
    public void Consumer_DuplicateIdentitiesDifferentLetterCase_ConsumerStateRejected() throws SQLException {

        final CreateConsumerModel firstCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
        final String duplicateName = firstCreateConsumerModel.getRootUser().getName();
        final String duplicateSurname = firstCreateConsumerModel.getRootUser().getSurname();
        final DateOfBirthModel duplicateDob = firstCreateConsumerModel.getRootUser().getDateOfBirth();

        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final CreateConsumerModel secondCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(duplicateName.toUpperCase())
                        .setSurname(duplicateSurname.toUpperCase())
                        .setDateOfBirth(duplicateDob)
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, secondConsumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, secondConsumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel rejectedStateEvent = getWebhookResponse(rejectedStateTimestamp, secondConsumer.getLeft());

        assertEquals(secondConsumer.getLeft(), rejectedStateEvent.getConsumerId());
        assertEquals(secondCreateConsumerModel.getRootUser().getEmail(), rejectedStateEvent.getConsumerEmail());
        assertEquals(1, Arrays.asList(rejectedStateEvent.getDetails()).size());
        assertEquals("REJECTED_DUPLICATE", rejectedStateEvent.getDetails()[0]);
        assertEquals(String.format("%s %s with date of birth 1990-01-01 already exists on your programme.",
                duplicateName.toUpperCase(), duplicateSurname.toUpperCase()), rejectedStateEvent.getRejectionComment());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getKycLevel());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getOngoingKycLevel());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getStatus());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getOngoingStatus());

        final Map<String, String> duplicateIdentity = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);

        assertEquals(secondConsumer.getLeft(), duplicateIdentity.get("consumer_id"));
        assertEquals(firstConsumer.getLeft(), duplicateIdentity.get("duplicate_identity_id"));
        assertEquals(duplicateName, duplicateIdentity.get("name"));
        assertEquals(duplicateSurname, duplicateIdentity.get("surname"));
        assertEquals("1990-01-01", duplicateIdentity.get("date_of_birth"));
    }

    @Test
    public void Consumer_DuplicateIdentitiesRotateNameSurname_ConsumerStateRejected() throws SQLException {

        final CreateConsumerModel firstCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
        final String duplicateName = firstCreateConsumerModel.getRootUser().getName();
        final String duplicateSurname = firstCreateConsumerModel.getRootUser().getSurname();
        final DateOfBirthModel duplicateDob = firstCreateConsumerModel.getRootUser().getDateOfBirth();

        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final CreateConsumerModel secondCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(duplicateSurname)
                        .setSurname(duplicateName)
                        .setDateOfBirth(duplicateDob)
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, secondConsumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, secondConsumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel rejectedStateEvent = getWebhookResponse(rejectedStateTimestamp, secondConsumer.getLeft());

        assertEquals(secondConsumer.getLeft(), rejectedStateEvent.getConsumerId());
        assertEquals(secondCreateConsumerModel.getRootUser().getEmail(), rejectedStateEvent.getConsumerEmail());
        assertEquals(1, Arrays.asList(rejectedStateEvent.getDetails()).size());
        assertEquals("REJECTED_DUPLICATE", rejectedStateEvent.getDetails()[0]);
        assertEquals(String.format("%s %s with date of birth 1990-01-01 already exists on your programme.",
                duplicateSurname, duplicateName), rejectedStateEvent.getRejectionComment());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getKycLevel());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getOngoingKycLevel());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getStatus());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getOngoingStatus());

        final Map<String, String> duplicateIdentity = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);

        assertEquals(secondConsumer.getLeft(), duplicateIdentity.get("consumer_id"));
        assertEquals(firstConsumer.getLeft(), duplicateIdentity.get("duplicate_identity_id"));
        assertEquals(duplicateName, duplicateIdentity.get("name"));
        assertEquals(duplicateSurname, duplicateIdentity.get("surname"));
        assertEquals("1990-01-01", duplicateIdentity.get("date_of_birth"));
    }

    @Test
    public void Consumers_RejectedDuplicateIdentityRemoveFlag_Success() throws SQLException {
        final CreateConsumerModel firstCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
        final String duplicateName = firstCreateConsumerModel.getRootUser().getName();
        final String duplicateSurname = firstCreateConsumerModel.getRootUser().getSurname();
        final DateOfBirthModel duplicateDob = firstCreateConsumerModel.getRootUser().getDateOfBirth();

        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final CreateConsumerModel secondCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(duplicateName)
                        .setSurname(duplicateSurname)
                        .setDateOfBirth(duplicateDob)
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, secondConsumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, secondConsumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData));


        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.REJECTED.name());

        final List<WebhookKycEventModel> webhookEvents = getWebhookResponses(rejectedStateTimestamp, secondConsumer.getLeft(), 2);
        final WebhookKycEventModel rejectedStateEvent = webhookEvents.stream().filter(x -> x.getStatus().equals(KycState.REJECTED.name())).findFirst().orElseThrow();

        assertEquals(secondConsumer.getLeft(), rejectedStateEvent.getConsumerId());
        assertEquals(secondCreateConsumerModel.getRootUser().getEmail(), rejectedStateEvent.getConsumerEmail());
        assertEquals(1, Arrays.asList(rejectedStateEvent.getDetails()).size());
        assertEquals("REJECTED_DUPLICATE", rejectedStateEvent.getDetails()[0]);
        assertEquals(String.format("%s %s with date of birth 1990-01-01 already exists on your programme.",
                duplicateName, duplicateSurname), rejectedStateEvent.getRejectionComment());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getKycLevel());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getOngoingKycLevel());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getStatus());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getOngoingStatus());

        final Map<String, String> duplicateIdentityBeforeRemove = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);

        assertEquals(secondConsumer.getLeft(), duplicateIdentityBeforeRemove.get("consumer_id"));
        assertEquals(firstConsumer.getLeft(), duplicateIdentityBeforeRemove.get("duplicate_identity_id"));
        assertEquals(duplicateName, duplicateIdentityBeforeRemove.get("name"));
        assertEquals(duplicateSurname, duplicateIdentityBeforeRemove.get("surname"));
        assertEquals("1990-01-01", duplicateIdentityBeforeRemove.get("date_of_birth"));

        //remove after fix
        ConsumersService.getConsumerKyc(secretKey, secondConsumer.getRight());

        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        final RemoveDuplicateIdentityFlagModel removeComment = RemoveDuplicateIdentityFlagModel.removeComment();
        AdminService.removeDuplicateIdentityFlag(removeComment, secondConsumer.getLeft(), adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<String, String> duplicateIdentityAfterRemove = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);
        assertEquals(removeComment.getComment(), duplicateIdentityAfterRemove.get("approval_reason"));
        assertNotNull(duplicateIdentityAfterRemove.get("approval_timestamp"));

        final WebhookKycEventModel approvedStateEvent = getWebhookResponse(approvedStateTimestamp, secondConsumer.getLeft());

        assertEquals(secondConsumer.getLeft(), approvedStateEvent.getConsumerId());
        assertEquals(secondCreateConsumerModel.getRootUser().getEmail(), approvedStateEvent.getConsumerEmail());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), approvedStateEvent.getKycLevel());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), approvedStateEvent.getOngoingKycLevel());
        assertEquals(KycState.APPROVED.name(), approvedStateEvent.getStatus());
        assertEquals(KycState.APPROVED.name(), approvedStateEvent.getOngoingStatus());
    }

    @Test
    public void Consumers_RejectedDuplicateIdentityRemoveFlagNewAdminApi_Success() throws SQLException {
        final CreateConsumerModel firstCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
        final String duplicateName = firstCreateConsumerModel.getRootUser().getName();
        final String duplicateSurname = firstCreateConsumerModel.getRootUser().getSurname();
        final DateOfBirthModel duplicateDob = firstCreateConsumerModel.getRootUser().getDateOfBirth();

        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final CreateConsumerModel secondCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(duplicateName)
                        .setSurname(duplicateSurname)
                        .setDateOfBirth(duplicateDob)
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, secondConsumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, secondConsumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData));

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());


        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.REJECTED.name());

        final List<WebhookKycEventModel> webhookEvents = getWebhookResponses(rejectedStateTimestamp, secondConsumer.getLeft(), 2);
        final WebhookKycEventModel rejectedStateEvent = webhookEvents.stream().filter(x -> x.getStatus().equals(KycState.REJECTED.name())).findFirst().orElseThrow();


        assertEquals(secondConsumer.getLeft(), rejectedStateEvent.getConsumerId());
        assertEquals(secondCreateConsumerModel.getRootUser().getEmail(), rejectedStateEvent.getConsumerEmail());
        assertEquals(1, Arrays.asList(rejectedStateEvent.getDetails()).size());
        assertEquals("REJECTED_DUPLICATE", rejectedStateEvent.getDetails()[0]);
        assertEquals(String.format("%s %s with date of birth 1990-01-01 already exists on your programme.",
                duplicateName, duplicateSurname), rejectedStateEvent.getRejectionComment());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getKycLevel());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), rejectedStateEvent.getOngoingKycLevel());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getStatus());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getOngoingStatus());

        final Map<String, String> duplicateIdentityBeforeRemove = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);

        assertEquals(secondConsumer.getLeft(), duplicateIdentityBeforeRemove.get("consumer_id"));
        assertEquals(firstConsumer.getLeft(), duplicateIdentityBeforeRemove.get("duplicate_identity_id"));
        assertEquals(duplicateName, duplicateIdentityBeforeRemove.get("name"));
        assertEquals(duplicateSurname, duplicateIdentityBeforeRemove.get("surname"));
        assertEquals("1990-01-01", duplicateIdentityBeforeRemove.get("date_of_birth"));

        final long approvedStateTimestamp = Instant.now().toEpochMilli();

        final RemoveDuplicateIdentityFlagModel removeComment = RemoveDuplicateIdentityFlagModel.removeComment();
        AdminService.removeDuplicateIdentityFlag(removeComment, secondConsumer.getLeft(), adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<String, String> duplicateIdentityAfterRemove = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);
        assertEquals(removeComment.getComment(), duplicateIdentityAfterRemove.get("approval_reason"));
        assertNotNull(duplicateIdentityAfterRemove.get("approval_timestamp"));

        final WebhookKycEventModel approvedStateEvent = getWebhookResponse(approvedStateTimestamp, secondConsumer.getLeft());

        assertEquals(secondConsumer.getLeft(), approvedStateEvent.getConsumerId());
        assertEquals(secondCreateConsumerModel.getRootUser().getEmail(), approvedStateEvent.getConsumerEmail());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), approvedStateEvent.getKycLevel());
        assertEquals(KycLevel.KYC_LEVEL_2.name(), approvedStateEvent.getOngoingKycLevel());
        assertEquals(KycState.APPROVED.name(), approvedStateEvent.getStatus());
        assertEquals(KycState.APPROVED.name(), approvedStateEvent.getOngoingStatus());
    }

    @Test
    public void Consumers_RejectedDuplicateIdentityRemoveFlagKycLevel1_Success() throws SQLException {
        final CreateConsumerModel firstCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
        final String duplicateName = firstCreateConsumerModel.getRootUser().getName();
        final String duplicateSurname = firstCreateConsumerModel.getRootUser().getSurname();
        final DateOfBirthModel duplicateDob = firstCreateConsumerModel.getRootUser().getDateOfBirth();

        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final CreateConsumerModel secondCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(duplicateName)
                        .setSurname(duplicateSurname)
                        .setDateOfBirth(duplicateDob)
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, secondConsumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, secondConsumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());

        final long rejectedStateTimestamp = Instant.now().toEpochMilli();

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.REJECTED.name());

        final WebhookKycEventModel rejectedStateEvent = getWebhookResponse(rejectedStateTimestamp, secondConsumer.getLeft());

        assertEquals(secondConsumer.getLeft(), rejectedStateEvent.getConsumerId());
        assertEquals(secondCreateConsumerModel.getRootUser().getEmail(), rejectedStateEvent.getConsumerEmail());
        assertEquals(1, Arrays.asList(rejectedStateEvent.getDetails()).size());
        assertEquals("REJECTED_DUPLICATE", rejectedStateEvent.getDetails()[0]);
        assertEquals(String.format("%s %s with date of birth 1990-01-01 already exists on your programme.",
                duplicateName, duplicateSurname), rejectedStateEvent.getRejectionComment());
        assertEquals(KycLevel.KYC_LEVEL_1.name(), rejectedStateEvent.getKycLevel());
        assertEquals(KycLevel.KYC_LEVEL_1.name(), rejectedStateEvent.getOngoingKycLevel());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getStatus());
        assertEquals(KycState.REJECTED.name(), rejectedStateEvent.getOngoingStatus());

        final Map<String, String> duplicateIdentityBeforeRemove = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);

        assertEquals(secondConsumer.getLeft(), duplicateIdentityBeforeRemove.get("consumer_id"));
        assertEquals(firstConsumer.getLeft(), duplicateIdentityBeforeRemove.get("duplicate_identity_id"));
        assertEquals(duplicateName, duplicateIdentityBeforeRemove.get("name"));
        assertEquals(duplicateSurname, duplicateIdentityBeforeRemove.get("surname"));
        assertEquals("1990-01-01", duplicateIdentityBeforeRemove.get("date_of_birth"));

        //remove after fix
        ConsumersService.getConsumerKyc(secretKey, secondConsumer.getRight());

        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        final RemoveDuplicateIdentityFlagModel removeComment = RemoveDuplicateIdentityFlagModel.removeComment();
        AdminService.removeDuplicateIdentityFlag(removeComment, secondConsumer.getLeft(), adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<String, String> duplicateIdentityAfterRemove = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);
        assertEquals(removeComment.getComment(), duplicateIdentityAfterRemove.get("approval_reason"));
        assertNotNull(duplicateIdentityAfterRemove.get("approval_timestamp"));

        final WebhookKycEventModel approvedStateEvent = getWebhookResponse(approvedStateTimestamp, secondConsumer.getLeft());

        assertEquals(secondConsumer.getLeft(), approvedStateEvent.getConsumerId());
        assertEquals(secondCreateConsumerModel.getRootUser().getEmail(), approvedStateEvent.getConsumerEmail());
        assertEquals(KycLevel.KYC_LEVEL_1.name(), approvedStateEvent.getKycLevel());
        assertEquals(KycLevel.KYC_LEVEL_1.name(), approvedStateEvent.getOngoingKycLevel());
        assertEquals(KycState.APPROVED.name(), approvedStateEvent.getStatus());
        assertEquals(KycState.APPROVED.name(), approvedStateEvent.getOngoingStatus());
    }

    @Test
    public void Consumer_ConsumerApprovedThenRejectedFinal_ConsumerStateApproved() {

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
        final long pendingStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final WebhookKycEventModel eventPendingState = getWebhookResponse(pendingStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventPendingState, KycState.PENDING_REVIEW, KycState.PENDING_REVIEW,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        final long approvedStateTimestamp = Instant.now().toEpochMilli();
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        final WebhookKycEventModel eventApprovedState = getWebhookResponse(approvedStateTimestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                eventApprovedState, KycState.APPROVED, KycState.APPROVED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());

        final List<String> rejectLabels = Collections.singletonList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL, Optional.of("Issue with verification."),
                        applicantData.getId(), weavrIdentity.getExternalUserId());

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
    }

    private static void setResidentialCountriesOnProgrammeLevel(final UpdateProgrammeModel updateProgrammeModel) {

        AdminService.updateProgramme(updateProgrammeModel, programmeId, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);
    }

    private static void setResidentialCountriesOnProfileLevel(final UpdateConsumerProfileModel updateConsumerProfileModel) {

        AdminService.updateConsumerProfile(updateConsumerProfileModel, adminImpersonatedTenantToken, programmeId, consumerProfileId)
                .then()
                .statusCode(SC_OK);
    }

    @AfterEach
    private void resetResidentialCountries() {
        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder()
                .setCountry(getAllEeaCountries())
                .setHasCountry(true)
                .build();

        setResidentialCountriesOnProgrammeLevel(updateProgrammeModel);

        final UpdateConsumerProfileModel updateProfileModel = UpdateConsumerProfileModel.builder()
                .allowedCountries(getAllEeaCountries())
                .hasAllowedCountries(true)
                .build();

        setResidentialCountriesOnProfileLevel(updateProfileModel);
    }

    private void assertKycEvent(final String consumerId,
                                final String consumerEmail,
                                final WebhookKycEventModel event,
                                final KycState kycState,
                                final KycState ongoingKycState,
                                final KycLevel kycLevel,
                                final KycLevel ongoingKycLevel,
                                final Optional<List<String>> details,
                                final Optional<String> rejectionComment) {

        assertEquals(consumerId, event.getConsumerId());
        assertEquals(consumerEmail, event.getConsumerEmail());
        assertEquals(details.orElse(new ArrayList<>()).size(),
                Arrays.asList(event.getDetails()).size());
        assertEquals(rejectionComment.orElse(""), event.getRejectionComment());
        assertEquals(kycLevel.name(), event.getKycLevel());
        assertEquals(ongoingKycLevel.name(), event.getOngoingKycLevel());
        assertEquals(kycState.name(), event.getStatus());
        assertEquals(ongoingKycState.name(), event.getOngoingStatus());

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

    private WebhookKycEventModel getWebhookResponse(final long timestamp,
                                                    final String identityId) {
        return (WebhookKycEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.CONSUMER_KYC,
                Pair.of("consumerId", identityId),
                WebhookKycEventModel.class,
                ApiSchemaDefinition.KycEvent);
    }

    private List<WebhookKycEventModel> getWebhookResponses(final long timestamp,
                                                           final String identityId,
                                                           final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.CONSUMER_KYC,
                Pair.of("consumerId", identityId),
                WebhookKycEventModel.class,
                ApiSchemaDefinition.KycEvent,
                expectedEventCount);
    }
}
