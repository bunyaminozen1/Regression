package opc.junit.sumsub;

import io.cucumber.messages.internal.com.google.gson.Gson;
import io.cucumber.messages.internal.com.google.gson.JsonObject;
import opc.enums.opc.ConsumerApplicantLevel;
import opc.enums.opc.ConsumerSourceOfFunds;
import opc.enums.opc.CountryCode;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.KycLevel;
import opc.enums.opc.KycState;
import opc.enums.opc.Occupation;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.database.SumsubDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.adminnew.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.admin.ActivateConsumerLevelCheckModel;
import opc.models.admin.DeactivateConsumerLevelCheckModel;
import opc.models.admin.RegisteredCountriesContextModel;
import opc.models.admin.RegisteredCountriesDimension;
import opc.models.admin.RegisteredCountriesSetCountriesModel;
import opc.models.admin.RegisteredCountriesSetModel;
import opc.models.admin.RegisteredCountriesValue;
import opc.models.admin.SetApplicantLevelModel;
import opc.models.admin.SubscriptionStatusPayneticsModel;
import opc.models.innovator.ConsumersFilterModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.PrefillDetailsModel;
import opc.models.multi.consumers.StartKycModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.secure.SetIdentityDetailsModel;
import opc.models.shared.AddressModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.sumsub.FixedInfoModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubAddressModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.admin.AdminService;
import opc.services.multi.ConsumersService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.PasswordsService;
import opc.services.secure.SecureService;
import opc.services.sumsub.SumSubService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static opc.enums.sumsub.IdDocType.GERMAN_DRIVER_LICENCE;
import static opc.enums.sumsub.IdDocType.ID_CARD_BACK;
import static opc.enums.sumsub.IdDocType.ID_CARD_BACK_USER_IP;
import static opc.enums.sumsub.IdDocType.ID_CARD_FRONT;
import static opc.enums.sumsub.IdDocType.ID_CARD_FRONT_USER_IP;
import static opc.enums.sumsub.IdDocType.PASSPORT;
import static opc.enums.sumsub.IdDocType.SELFIE;
import static opc.enums.sumsub.IdDocType.SELFIE_USER_IP;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL_USER_IP;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Tag(MultiTags.SUMSUB_CONSUMER)
public class SumSubConsumerFlowTests extends BaseSumSubSetup {

    @BeforeAll
    public static void enableEdd() {
        opc.junit.helpers.adminnew.AdminHelper.setEddCountriesProperty(adminToken, nonFpsEnabledTenant.getInnovatorId(), CountryCode.DE.name(), IdentityType.CONSUMER);
    }

    @AfterAll
    public static void disableEdd() {
        opc.junit.helpers.adminnew.AdminHelper.deleteEddCountriesProperty(adminToken, nonFpsEnabledTenant.getInnovatorId(), IdentityType.CONSUMER);
    }

    @Test
    public void Consumer_VerifyConsumerWithId_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(createConsumerModel, secretKey);

        //Verify email
        ConsumersHelper.verifyEmail(createConsumerModel.getRootUser().getEmail(), secretKey);

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        //create managed account model
        final CreateManagedAccountModel createManagedAccountModel = CreateManagedAccountModel
                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, createConsumerModel.getBaseCurrency()).build();

        //try to create managed account before approval
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("OWNER_IDENTITY_NOT_VERIFIED"));

        final long startTime = System.currentTimeMillis();

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());

        //try again to create managed account after approval
        ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, consumer.getRight());

        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time > startTime);
        });

        //This part checks if request that is sent to Paynetics matches with provided address.
        final String userCreateLog = getPayneticsUserCreateLog(consumer.getLeft());
        JsonObject requestPayload = new Gson().fromJson(userCreateLog, JsonObject.class);
        final AddressModel address = createConsumerModel.getRootUser().getAddress();

        assertEquals(requestPayload.get("address1").getAsString(), address.getAddressLine1());
        assertEquals(requestPayload.get("address2").getAsString(), address.getAddressLine2());
        assertEquals(requestPayload.get("country").getAsString(), address.getCountry());
        assertEquals(requestPayload.get("city").getAsString(), address.getCity());
        assertEquals(requestPayload.get("zip").getAsString(), address.getPostCode());

        assertTrue(ensureIdentitySubscribed(consumer.getLeft()));

        //This part checks the paynetics subscription status of identity
        final SubscriptionStatusPayneticsModel subscriptionStatus =
                AdminHelper.getConsumerSubscriptionStatus(consumer.getLeft(), AdminService.loginAdmin());

        assertEquals(subscriptionStatus.getMessage(), "subscriptions are active");
        assertEquals(subscriptionStatus.getStatus(), "COMPLETED");

    }

    @Test
    public void Consumer_VerifyConsumerWithId_QuestionnairePEP_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        verifyPepStatus(consumer.getLeft(), "CHECK_NOT_STARTED", "PENDING");

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildPEPQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), Assertions::assertNull);

        verifyPepStatus(consumer.getLeft(), "YES", "YES");
    }

    @Test
    public void Consumer_VerifyConsumerWithId_QuestionnaireSourceOfFundsAndOccupation_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .setOccupation(Occupation.ACCOUNTING)
                                .build())
                        .setSourceOfFunds(ConsumerSourceOfFunds.INHERITANCE)
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOccupation(consumer.getLeft(), "AUTO_AVIATION");
        ConsumersHelper.verifyConsumerSourceOfFunds(consumer.getLeft(), "DIVIDENDS");
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time > startTime);
        });
    }


    @ParameterizedTest
    @MethodSource("merchantTokenProvider")
    public void Consumer_QuestionnaireLevel1SourceOfFundsAndOccupationWithMerchantTokenCheck_Success(final CountryCode countryCode,
                                                                                                     final String merchantToken,
                                                                                                     final ProgrammeDetailsModel programme) {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId())
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setNationality(countryCode.name())
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(countryCode)
                                        .build())
                                .setOccupation(Occupation.ACCOUNTING)
                                .build())
                        .setSourceOfFunds(ConsumerSourceOfFunds.RENT)
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, programme.getSecretKey());
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, programme.getSecretKey(), consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(programme.getSharedKey(), consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildNoOriginOfFundsQuestionnaire(applicantData, "AUTO_AVIATION")
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOccupation(consumer.getLeft(), "AUTO_AVIATION");
        ConsumersHelper.verifyConsumerSourceOfFunds(consumer.getLeft(), "RENT");
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time > startTime);
        });

        ConsumersService.getConsumers(programme.getSecretKey(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.occupation", equalTo("AUTO_AVIATION"))
                .body("sourceOfFunds", equalTo("RENT"))
                .body("sourceOfFundsOther", equalTo(null));


        //We are checking if the identity use the proper merchant token based on country
        final String consumerSubscriptionRequest = getNaturalPersonsApiRequest(consumer.getLeft());
        final JsonObject requestPayload = new Gson().fromJson(consumerSubscriptionRequest, JsonObject.class);

        assertEquals(requestPayload.get("merchant").getAsString(), merchantToken);
        //Check if we send correct email to paynetics
        assertEquals(requestPayload.get("email").getAsString(), createConsumerModel.getRootUser().getEmail());
        ensureIdentitySubscribed(consumer.getLeft());
    }

    @Test
    public void Consumer_VerifyConsumerWithId_QuestionnaireLevel1OccupationIsNotMappedInEnum_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationOneUk.getConsumersProfileId())
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setNationality("GB")
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.GB)
                                        .build())
                                .setOccupation(Occupation.ACCOUNTING)
                                .build())
                        .setSourceOfFunds(ConsumerSourceOfFunds.CIVIL_CONTRACT)
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, applicationOneUk.getSecretKey());
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, applicationOneUk.getSecretKey(), consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOneUk.getSharedKey(), consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildNoOriginOfFundsQuestionnaire(applicantData, "CRYPTOCURRENCIES")
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOccupation(consumer.getLeft(), "CRYPTOCURRENCIES");
        ConsumersHelper.verifyConsumerSourceOfFunds(consumer.getLeft(), "CIVIL_CONTRACT");
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time > startTime);
        });

        ConsumersService.getConsumers(applicationOneUk.getSecretKey(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.occupation", equalTo("OTHER"))
                .body("sourceOfFunds", equalTo("CIVIL_CONTRACT"))
                .body("sourceOfFundsOther", equalTo(null));
    }

    @Test
    public void Consumer_VerifyConsumerWithPassport_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(PASSPORT, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(PASSPORT.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(PASSPORT.getType()).get(0)))
                .body("SELFIE.idDocType", equalTo(SELFIE.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE.getType()).get(0)))
                .body("PROOF_OF_RESIDENCE.idDocType", equalTo(UTILITY_BILL_USER_IP.getType()))
                .body("PROOF_OF_RESIDENCE.imageIds[0]", equalTo(documentImageMap.get(UTILITY_BILL_USER_IP.getType()).get(0)));

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time > startTime);
        });

        final List<String> rejectLabels = List.of(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.RETRY,
                        Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());

        final ConsumersFilterModel filterModel = ConsumersFilterModel.builder()
                .setEmail(createConsumerModel.getRootUser().getEmail())
                .build();

        AdminService.getConsumers(filterModel, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("consumerWithKyc[0].consumer.id.id", equalTo(consumer.getLeft()))
                .body("consumerWithKyc[0].kyc.fullDueDiligence", equalTo("APPROVED"))
                .body("consumerWithKyc[0].kyc.kycLevel", equalTo("KYC_LEVEL_2"))
                .body("consumerWithKyc[0].kyc.ongoingFullDueDiligence", equalTo("APPROVED"))
                .body("consumerWithKyc[0].kyc.ongoingKycLevel", equalTo("KYC_LEVEL_2"));

        //create managed account model
        final CreateManagedAccountModel createManagedAccountModel = CreateManagedAccountModel
                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, createConsumerModel.getBaseCurrency()).build();

        //try to create managed account
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, consumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void Consumer_ConsumerApprovedDifferentLevelsFlow_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER));
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        verifySumsubState("INITIATED", consumer.getLeft());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        verifySumsubState("PENDING_REVIEW", consumer.getLeft());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        verifySumsubState("APPROVED", consumer.getLeft());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }


    @Test
    public void Consumer_VerifyConsumerDetailsUpdate_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.randomFixedInfoModel()
                        .setAddresses(Collections.singletonList(SumSubAddressModel.randomAddressModelBuilder()
                                .setCountry("DEU")
                                .build()))
                        .build();
        SumSubHelper.submitApplicantInformation(fixedInfoModel, weavrIdentity.getAccessToken(), applicantData.getId());

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final Map<String, String> updatedConsumerRootDetails = ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0);
        final Map<String, String> updatedConsumerUserDetails = ConsumersDatabaseHelper.getConsumerUser(consumer.getLeft()).get(0);
        final Map<String, String> updatedConsumerAddress = ConsumersDatabaseHelper.getConsumerAddress(consumer.getLeft()).get(0);
        assertEquals(String.format("Id - %s", applicantData.getId()), updatedConsumerRootDetails.get("identification_number"));
        assertEquals(fixedInfoModel.getFirstName(), updatedConsumerUserDetails.get("name"));
        assertEquals(fixedInfoModel.getLastName(), updatedConsumerUserDetails.get("surname"));
        assertEquals(fixedInfoModel.getDob(), updatedConsumerUserDetails.get("date_of_birth"));
        assertEquals(fixedInfoModel.getDob(), updatedConsumerRootDetails.get("date_of_birth"));
        assertEquals(fixedInfoModel.getPlaceOfBirth(), updatedConsumerRootDetails.get("place_of_birth"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getSubStreet(), updatedConsumerAddress.get("address_line2"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getStreet(), updatedConsumerAddress.get("address_line1"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getState(), updatedConsumerAddress.get("state"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getTown(), updatedConsumerAddress.get("city"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getPostCode(), updatedConsumerAddress.get("post_code"));
        assertEquals(updatedConsumerAddress.get("country"), "DE");
        assertEquals(updatedConsumerRootDetails.get("nationality"), "MT");

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_VerifyConsumerNoPlaceOfBirthAndNationalityDetailsUpdate_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setPlaceOfBirth(null)
                                .setNationality(null)
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.randomFixedInfoModel()
                        .setAddresses(Collections.singletonList(SumSubAddressModel.randomAddressModelBuilder()
                                .setCountry("DEU")
                                .build()))
                        .build();
        SumSubHelper.submitApplicantInformation(fixedInfoModel, weavrIdentity.getAccessToken(), applicantData.getId());

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final Map<String, String> updatedConsumerRootDetails = ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0);
        final Map<String, String> updatedConsumerUserDetails = ConsumersDatabaseHelper.getConsumerUser(consumer.getLeft()).get(0);
        final Map<String, String> updatedConsumerAddress = ConsumersDatabaseHelper.getConsumerAddress(consumer.getLeft()).get(0);
        assertEquals(String.format("Id - %s", applicantData.getId()), updatedConsumerRootDetails.get("identification_number"));
        assertEquals(fixedInfoModel.getFirstName(), updatedConsumerUserDetails.get("name"));
        assertEquals(fixedInfoModel.getLastName(), updatedConsumerUserDetails.get("surname"));
        assertEquals(fixedInfoModel.getDob(), updatedConsumerUserDetails.get("date_of_birth"));
        assertEquals(fixedInfoModel.getDob(), updatedConsumerRootDetails.get("date_of_birth"));
        assertEquals(fixedInfoModel.getPlaceOfBirth(), updatedConsumerRootDetails.get("place_of_birth"));
        assertEquals("MALTA", updatedConsumerRootDetails.get("place_of_birth"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getSubStreet(), updatedConsumerAddress.get("address_line2"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getStreet(), updatedConsumerAddress.get("address_line1"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getState(), updatedConsumerAddress.get("state"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getTown(), updatedConsumerAddress.get("city"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getPostCode(), updatedConsumerAddress.get("post_code"));
        assertEquals(updatedConsumerAddress.get("country"), "DE");
        assertEquals(updatedConsumerRootDetails.get("nationality"), "MT");

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_VerifyConsumerKycLevel1NoPlaceOfBirth_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setPlaceOfBirth(null)
                                .build()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.defaultFixedInfoModel(applicantData.getFixedInfo())
                        .setPlaceOfBirth(null)
                        .build();
        SumSubHelper.submitApplicantInformation(fixedInfoModel, weavrIdentity.getAccessToken(), applicantData.getId());

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_VerifyConsumerKycLevel1NationalityRequired_NotAllDocumentsSubmitted() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setPlaceOfBirth(null)
                                .setNationality(null)
                                .build()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.defaultFixedInfoModel(applicantData.getFixedInfo())
                        .setPlaceOfBirth(null)
                        .setNationality(null)
                        .build();
        SumSubHelper.submitApplicantInformation(fixedInfoModel, weavrIdentity.getAccessToken(), applicantData.getId());

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubService.setApplicantInPendingState(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_CONFLICT)
                .body("description", equalTo("Not all required documents are submitted. Make sure to upload all the documents beforehand."));

        //This part checks the paynetics subscription status of identity
        opc.services.adminnew.AdminService.getConsumerSubscriptionStatus(AdminService.loginAdmin(), consumer.getLeft(), Optional.of("{}"))
                .then()
                .statusCode(SC_OK)
                .body("status", equalTo("NO_STATUS_IN_TIME_FRAME"));
    }

    @Test
    public void Consumer_VerifyConsumerPlaceOfBirthAndNationalityDetailsUpdate_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setPlaceOfBirth("Spain")
                                .setNationality("PT")
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .build()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final Map<String, String> updatedConsumerRootDetails = ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0);
        final Map<String, String> updatedConsumerUserDetails = ConsumersDatabaseHelper.getConsumerUser(consumer.getLeft()).get(0);
        final Map<String, String> updatedConsumerAddress = ConsumersDatabaseHelper.getConsumerAddress(consumer.getLeft()).get(0);
        assertEquals(String.format("Id - %s", applicantData.getId()), updatedConsumerRootDetails.get("identification_number"));
        assertEquals(fixedInfoModel.getFirstName(), updatedConsumerUserDetails.get("name"));
        assertEquals(fixedInfoModel.getLastName(), updatedConsumerUserDetails.get("surname"));
        assertEquals(fixedInfoModel.getDob(), updatedConsumerUserDetails.get("date_of_birth"));
        assertEquals(fixedInfoModel.getDob(), updatedConsumerRootDetails.get("date_of_birth"));
        assertEquals(fixedInfoModel.getPlaceOfBirth(), updatedConsumerRootDetails.get("place_of_birth"));
        assertEquals("Spain", updatedConsumerRootDetails.get("place_of_birth"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getSubStreet(), updatedConsumerAddress.get("address_line2"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getStreet(), updatedConsumerAddress.get("address_line1"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getState(), updatedConsumerAddress.get("state"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getTown(), updatedConsumerAddress.get("city"));
        assertEquals(fixedInfoModel.getAddresses().get(0).getPostCode(), updatedConsumerAddress.get("post_code"));
        assertEquals(createConsumerModel.getRootUser().getAddress().getCountry(), updatedConsumerAddress.get("country"), "MT");
        assertEquals(updatedConsumerRootDetails.get("nationality"), "PT");

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time > -startTime);
        });
    }

    @Test
    public void Consumer_VerifyConsumerNoAddressRequired_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId, new SetIdentityDetailsModel(true));

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER_NO_POA));
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE);
        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(ID_CARD_FRONT_USER_IP.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(ID_CARD_FRONT_USER_IP.getType()).get(0)))
                .body("IDENTITY.imageIds[1]", equalTo(documentImageMap.get(ID_CARD_BACK_USER_IP.getType()).get(1)))
                .body("SELFIE.idDocType", equalTo(SELFIE.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE.getType()).get(0)));

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_VerifyConsumerNoAddressRequiredCountryDoesNotMatchIp_KycRejected() throws SQLException {

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

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId, new SetIdentityDetailsModel(true));

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER_NO_POA));
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE);
        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(ID_CARD_FRONT_USER_IP.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(ID_CARD_FRONT_USER_IP.getType()).get(0)))
                .body("IDENTITY.imageIds[1]", equalTo(documentImageMap.get(ID_CARD_BACK_USER_IP.getType()).get(1)))
                .body("SELFIE.idDocType", equalTo(SELFIE.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE.getType()).get(0)));

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), Assertions::assertNull);

        assertEquals("1", ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0).get("active"));

        // Rejection comment should be populated in the response of /kyc/get APIs
        checkRejectionCommentsGetKycApiResponses(consumer.getLeft(), "Additional documents required due to geolocation mismatch");
    }

    @Test
    public void Consumer_VerifyConsumerNoAddressRequiredIdCountryDoesNotMatchIp_KycRejected() throws SQLException {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId, new SetIdentityDetailsModel(true));

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER_NO_POA));
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE);
        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(ID_CARD_FRONT.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(ID_CARD_FRONT.getType()).get(0)))
                .body("IDENTITY.imageIds[1]", equalTo(documentImageMap.get(ID_CARD_BACK.getType()).get(1)))
                .body("SELFIE.idDocType", equalTo(SELFIE.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE.getType()).get(0)));

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), Assertions::assertNull);

        assertEquals("1", ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0).get("active"));
    }

    @Test
    public void Consumer_VerifyConsumerAddressRequiredNotPassed_NotAllDocumentsSubmitted() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId, new SetIdentityDetailsModel(false));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(ID_CARD_FRONT.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(ID_CARD_FRONT.getType()).get(0)))
                .body("IDENTITY.imageIds[1]", equalTo(documentImageMap.get(ID_CARD_BACK.getType()).get(1)))
                .body("SELFIE.idDocType", equalTo(SELFIE.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE.getType()).get(0)));

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubService.setApplicantInPendingState(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_CONFLICT)
                .body("description", equalTo("Not all required documents are submitted. Make sure to upload all the documents beforehand."));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.INITIATED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.INITIATED.name());
    }

    @Test
    public void Consumer_VerifyConsumerAddressRequired_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId, new SetIdentityDetailsModel(false));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(ID_CARD_FRONT.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(ID_CARD_FRONT.getType()).get(0)))
                .body("IDENTITY.imageIds[1]", equalTo(documentImageMap.get(ID_CARD_BACK.getType()).get(1)))
                .body("SELFIE.idDocType", equalTo(SELFIE.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE.getType()).get(0)));

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_IdDifferentCountry_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE_USER_IP, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_SelfieDifferentCountry_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_UtilityBillDifferentCountry_KycRejected() throws SQLException {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE_USER_IP, UTILITY_BILL);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), Assertions::assertNull);

        assertEquals("1", ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0).get("active"));
    }

    @Test
    public void Consumer_ConsumerAddressDifferentCountry_KycRejected() throws SQLException {

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

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE_USER_IP, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), Assertions::assertNull);

        assertEquals("1", ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0).get("active"));
    }

    @Test
    public void Consumer_PassportDifferentCountry_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(PASSPORT, SELFIE_USER_IP, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(PASSPORT.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(PASSPORT.getType()).get(0)))
                .body("SELFIE.idDocType", equalTo(SELFIE_USER_IP.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE_USER_IP.getType()).get(0)))
                .body("PROOF_OF_RESIDENCE.idDocType", equalTo(UTILITY_BILL_USER_IP.getType()))
                .body("PROOF_OF_RESIDENCE.imageIds[0]", equalTo(documentImageMap.get(UTILITY_BILL_USER_IP.getType()).get(0)));


        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_DocsDifferentCountryDifferentLevelsFlow_KycRejected() throws SQLException {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        final long level1StartTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        final long level2StartTime = System.currentTimeMillis();
        ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER));
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE_USER_IP, UTILITY_BILL);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        verifySumsubState("INITIATED", consumer.getLeft());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(ID_CARD_FRONT_USER_IP.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(ID_CARD_FRONT_USER_IP.getType()).get(0)))
                .body("IDENTITY.imageIds[1]", equalTo(documentImageMap.get(ID_CARD_BACK_USER_IP.getType()).get(1)))
                .body("SELFIE.idDocType", equalTo(SELFIE_USER_IP.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE_USER_IP.getType()).get(0)))
                .body("PROOF_OF_RESIDENCE.idDocType", equalTo(UTILITY_BILL.getType()))
                .body("PROOF_OF_RESIDENCE.imageIds[0]", equalTo(documentImageMap.get(UTILITY_BILL.getType()).get(0)));

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        verifySumsubState("PENDING_REVIEW", consumer.getLeft());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        verifySumsubState("APPROVED", consumer.getLeft());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.REJECTED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= level1StartTime);
            assertTrue(time < level2StartTime);
        });

        assertEquals("1", ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0).get("active"));
    }

    @Test
    public void Consumer_AddressDifferentCountryDifferentLevelsFlow_KycRejected() throws SQLException {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.FR)
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

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        final long level1StartTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        final long level2StartTime = System.currentTimeMillis();
        ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER));
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE_USER_IP, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        verifySumsubState("INITIATED", consumer.getLeft());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(ID_CARD_FRONT_USER_IP.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(ID_CARD_FRONT_USER_IP.getType()).get(0)))
                .body("IDENTITY.imageIds[1]", equalTo(documentImageMap.get(ID_CARD_BACK_USER_IP.getType()).get(1)))
                .body("SELFIE.idDocType", equalTo(SELFIE_USER_IP.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE_USER_IP.getType()).get(0)))
                .body("PROOF_OF_RESIDENCE.idDocType", equalTo(UTILITY_BILL_USER_IP.getType()))
                .body("PROOF_OF_RESIDENCE.imageIds[0]", equalTo(documentImageMap.get(UTILITY_BILL_USER_IP.getType()).get(0)));

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        verifySumsubState("PENDING_REVIEW", consumer.getLeft());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        verifySumsubState("APPROVED", consumer.getLeft());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= level1StartTime);
            assertTrue(time <= level2StartTime);
        });

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.REJECTED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        assertEquals("1", ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0).get("active"));
    }

    @Test
    public void Consumer_KycMobile_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_KycMobileDifferentLevelsFlow_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        verifyPepStatus(consumer.getLeft(), "CHECK_NOT_STARTED", "PENDING");

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        verifyPepStatus(consumer.getLeft(), "NO", "NO");

        ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());

        verifyPepStatus(consumer.getLeft(), "NO", "PENDING");

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER));
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        verifySumsubState("INITIATED", consumer.getLeft());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        verifySumsubState("PENDING_REVIEW", consumer.getLeft());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        verifySumsubState("APPROVED", consumer.getLeft());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        verifyPepStatus(consumer.getLeft(), "NO", "NO");
    }

    @Test
    public void Consumer_KycMobilePendingUpdateLevel_Conflict() throws SQLException {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        verifySumsubState("PENDING_REVIEW", consumer.getLeft());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_PENDING_REVIEW"));
    }

    @Test
    public void Consumer_KycMobileInitiatedUpdateLevel_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.INITIATED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.INITIATED.name());

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void Consumer_KycMobileFinalRejectedUpdateLevel_AlreadyRejected() throws SQLException {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        verifySumsubState("PENDING_REVIEW", consumer.getLeft());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL,
                        Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), Assertions::assertNull);
        verifySumsubState("REJECTED", consumer.getLeft());

        checkRejectionCommentsGetKycApiResponses(consumer.getLeft(), "Issue with verification.");

        AdminService.getConsumer(adminToken, consumer.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true));

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_1), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_ALREADY_REJECTED"));
    }

    @Test
    public void Consumer_KycMobileRetryRejectedUpdateLevel_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKycMobile(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        verifySumsubState("PENDING_REVIEW", consumer.getLeft());

        ConsumersService.getConsumerKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.RETRY,
                        Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.INITIATED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.INITIATED.name());
        verifySumsubState("INITIATED", consumer.getLeft());

        // Rejection comment should be populated in the response of /kyc/get APIs
        checkRejectionCommentsGetKycApiResponses(consumer.getLeft(), "Issue with verification.");

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void ConsumerWithApprovedNationalitiesStatusApproved() {

        RegisteredCountriesSetCountriesModel setCountriesModel = RegisteredCountriesSetCountriesModel.builder().context(
                        RegisteredCountriesContextModel.builder().dimension(
                                RegisteredCountriesDimension.defaultDimensionModel(innovatorId, programmeId)).build())
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel("MT", "DE")).build()).build();

        AdminService.setApprovedNationalitiesConsumers(adminToken, setCountriesModel).then()
                .statusCode(SC_NO_CONTENT);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .setNationality("DE")
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
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE_USER_IP, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void ConsumerWithoutApprovedNationalitiesStatusRejected() throws SQLException {

        RegisteredCountriesSetCountriesModel setCountriesModel = RegisteredCountriesSetCountriesModel.builder().context(
                        RegisteredCountriesContextModel.builder().dimension(
                                RegisteredCountriesDimension.defaultDimensionModel(innovatorId, programmeId)).build())
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel("MT", "IT")).build()).build();

        AdminService.setApprovedNationalitiesConsumers(adminToken, setCountriesModel).then()
                .statusCode(SC_NO_CONTENT);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .setNationality("DE")
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
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE_USER_IP, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), Assertions::assertNull);

        // Rejection comment should be populated in the response of /kyc/get APIs
        checkRejectionCommentsGetKycApiResponses(consumer.getLeft(),
                "Nationality of individual requires additional documents (proof of residency)to be provided.");

        assertEquals("1", ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0).get("active"));
    }

    @Test
    public void ConsumerWithoutSettingApprovedNationalitiesStatusApproved() {

        RegisteredCountriesSetCountriesModel setCountriesModel = RegisteredCountriesSetCountriesModel.builder().context(
                RegisteredCountriesContextModel.builder().dimension(
                        RegisteredCountriesDimension.defaultDimensionModel(innovatorId, programmeId)).build()).build();

        AdminService.setApprovedNationalitiesConsumers(adminToken, setCountriesModel).then()
                .statusCode(SC_NO_CONTENT);

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE_USER_IP, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test//Allowed POI and not allowed nationality
    public void ConsumerMultipleNationalitiesAllowedPOI_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setNationality("TR")
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
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test//Not Allowed POI and allowed nationality
    public void ConsumerMultipleNationalitiesNotAllowedPOI_Rejected() {
        RegisteredCountriesSetCountriesModel setCountriesModel = RegisteredCountriesSetCountriesModel.builder().context(
                        RegisteredCountriesContextModel.builder().dimension(
                                RegisteredCountriesDimension.defaultDimensionModel(innovatorId, programmeId)).build())
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel("MT", "IT")).build()).build();

        AdminService.setApprovedNationalitiesConsumers(adminToken, setCountriesModel).then()
                .statusCode(SC_NO_CONTENT);

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(ID_CARD_FRONT_USER_IP.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(ID_CARD_FRONT_USER_IP.getType()).get(0)))
                .body("IDENTITY.imageIds[1]", equalTo(documentImageMap.get(ID_CARD_BACK_USER_IP.getType()).get(1)))
                .body("SELFIE.idDocType", equalTo(SELFIE.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE.getType()).get(0)))
                .body("PROOF_OF_RESIDENCE.idDocType", equalTo(UTILITY_BILL_USER_IP.getType()))
                .body("PROOF_OF_RESIDENCE.imageIds[0]", equalTo(documentImageMap.get(UTILITY_BILL_USER_IP.getType()).get(0)));

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), Assertions::assertNull);
    }

    //Accepting driving licence as POI for UK individual applicants
    @Test
    public void ConsumerUkCitizenship_AllowedDrivingLicenceAsPOI_Success() {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationOneUk.getConsumersProfileId())
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setNationality("GB")
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.GB)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, applicationOneUk.getSecretKey());

        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setConsumerApplicantLevelForIdentity(
                CONSUMER_PUK, applicationOneUk.getProgrammeId(), consumer.getLeft(), "CONSUMER_KYC_LEVEL_2");

        opc.services.admin.AdminService.createConsumerLevelConfiguration(setApplicantLevelModel, adminToken)
                .then()
                .statusCode(SC_OK);

        final String kycReferenceId = ConsumersHelper.startKyc(applicationOneUk.getSecretKey(), consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOneUk.getSharedKey(), consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(GERMAN_DRIVER_LICENCE, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatusConsumerUk(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );
        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();
        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerLastApprovalTime(consumer.getLeft(), time -> {
            assertNotNull(time);
            assertTrue(time >= startTime);
        });
    }

    @Test
    public void Consumer_ActivateNationalityCheckForConsumer1PoaResidencyLevel_Rejected() {

        final ActivateConsumerLevelCheckModel activateNationalityCheck = ActivateConsumerLevelCheckModel
                .activateNationalityCheckModel("consumer-1POA-residency");

        AdminService.activateConsumerLevelCheck(activateNationalityCheck, AdminService.loginAdmin());

        RegisteredCountriesSetCountriesModel setCountriesModel = RegisteredCountriesSetCountriesModel.builder()
                .context(
                        RegisteredCountriesContextModel.builder().dimension(
                                        RegisteredCountriesDimension.defaultDimensionModel(innovatorId, programmeId))
                                .build())
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel("MT", "IT")).build()).build();

        AdminService.setApprovedNationalitiesConsumers(adminToken, setCountriesModel).then()
                .statusCode(SC_NO_CONTENT);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .setNationality("DE")
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
                createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId)
                        .getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                                weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        SumSubHelper.changeApplicantLevel(applicantData.getId(), CONSUMER_1_POA_RESIDENCY_LEVEL);

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER_1_POA));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP,
                ID_CARD_BACK_USER_IP, SELFIE_USER_IP, UTILITY_BILL_USER_IP, GERMAN_DRIVER_LICENCE);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
                applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
                applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());

        final DeactivateConsumerLevelCheckModel deactivateNationalityCheck = DeactivateConsumerLevelCheckModel
                .deactivateNationalityCheck("consumer-1POA-residency");
        AdminService.deactivateConsumerLevelCheck(deactivateNationalityCheck, AdminService.loginAdmin());

    }

    @Test
    public void Consumer_ActivateGeolocationCheckForConsumer1PoaResidencyLevel_Rejected() {

        final ActivateConsumerLevelCheckModel geolocationCheckModel = ActivateConsumerLevelCheckModel
                .activateGeolocationCheckModel("consumer-1POA-residency");

        AdminService.activateConsumerLevelCheck(geolocationCheckModel, AdminService.loginAdmin());

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.FR)
                                        .build())
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
                createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId)
                        .getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                                weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        SecureService.setIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId,
                new SetIdentityDetailsModel(true));

        SumSubHelper.changeApplicantLevel(applicantData.getId(), CONSUMER_1_POA_RESIDENCY_LEVEL);

        weavrIdentity.setAccessToken(SumSubHelper.generateConsumerAccessToken(weavrIdentity.getExternalUserId(), ConsumerApplicantLevel.CONSUMER_1_POA));

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP,
                ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP, GERMAN_DRIVER_LICENCE);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
                applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());

        final DeactivateConsumerLevelCheckModel deactivateGeolocationCheck = DeactivateConsumerLevelCheckModel
                .deactivateGeolocationCheck("consumer-1POA-residency");
        AdminService.deactivateConsumerLevelCheck(deactivateGeolocationCheck, AdminService.loginAdmin());
    }

    /**
     * If a consumer's KYC, email and mobile are verified, when a second consumer is created with the same name, surname and
     * date of birth it will be rejected even if approved on sumsub. It doesn't matter whether
     * the first consumer is deactivated or not
     */

    @Test
    public void Consumer_SameProgrammeDuplicateIdentitiesFirstConsumerApproved_ConsumerStateRejected() throws SQLException {

        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, firstConsumer.getRight());

        final CreateConsumerModel secondCreateConsumerModel = createDuplicateConsumerModel(firstCreateConsumerModel, consumerProfileId);

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

        //Second consumer verifies mobile number, first consumer KYC status should not change
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, secondConsumer.getRight());

        ConsumersHelper.verifyConsumerState(firstConsumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(firstConsumer.getLeft(), KycState.APPROVED.name());

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

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(secondConsumer.getLeft(), KycState.REJECTED.name());

        // Rejection comment should be populated in the response of /kyc/get APIs
        checkRejectionCommentsGetKycApiResponses(secondConsumer.getLeft(),
                String.format("%s %s with date of birth 1990-01-01 already exists on your programme.",
                        secondCreateConsumerModel.getRootUser().getName(), secondCreateConsumerModel.getRootUser().getSurname()));

        final Map<String, String> duplicateIdentity = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);

        assertEquals(secondConsumer.getLeft(), duplicateIdentity.get("consumer_id"));
        assertEquals(firstConsumer.getLeft(), duplicateIdentity.get("duplicate_identity_id"));
        assertEquals(secondCreateConsumerModel.getRootUser().getName(), duplicateIdentity.get("name"));
        assertEquals(secondCreateConsumerModel.getRootUser().getSurname(), duplicateIdentity.get("surname"));
        assertEquals("1990-01-01", duplicateIdentity.get("date_of_birth"));
    }

    @Test
    public void Consumer_DifferentProgrammeDuplicateIdentities_ConsumerStateApproved() {

        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel(applicationTwo.getConsumersProfileId());

        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, applicationTwo.getSecretKey());
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), applicationTwo.getSecretKey(), firstConsumer.getRight());

        final CreateConsumerModel secondCreateConsumerModel = createDuplicateConsumerModel(firstCreateConsumerModel, consumerProfileId);
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

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(secondConsumer.getLeft(), KycState.APPROVED.name());
    }

    @Test
    public void Consumer_SameProgrammeDuplicateIdentitiesFirstConsumerRejected_ConsumerStateApproved() throws SQLException {

        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedConsumer(firstCreateConsumerModel, secretKey);
        ConsumersHelper.startKyc(secretKey, firstConsumer.getRight());
        ConsumersDatabaseHelper.updateConsumerKyc(KycState.REJECTED.name(), firstConsumer.getLeft());

        final CreateConsumerModel secondCreateConsumerModel = createDuplicateConsumerModel(firstCreateConsumerModel, consumerProfileId);

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

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(secondConsumer.getLeft(), KycState.APPROVED.name());
    }

    @Test
    public void Consumer_SameProgrammeDuplicateIdentitiesFirstConsumerPendingReview_ConsumerStateApproved() throws SQLException {

        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedConsumer(firstCreateConsumerModel, secretKey);

        ConsumersHelper.startKyc(secretKey, firstConsumer.getRight());
        ConsumersDatabaseHelper.updateConsumerKyc(KycState.PENDING_REVIEW.name(), firstConsumer.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, firstConsumer.getRight());

        final CreateConsumerModel secondCreateConsumerModel = createDuplicateConsumerModel(firstCreateConsumerModel, consumerProfileId);

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

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(secondConsumer.getLeft(), KycState.APPROVED.name());
    }

    @Test
    public void Consumer_DuplicateIdentitiesFirstConsumerEmailNotVerified_NewConsumerApproved() throws SQLException {

        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final String firstConsumerId = ConsumersHelper.createConsumer(firstCreateConsumerModel, secretKey);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        final String firstConsumerToken =
                TestHelper.ensureAsExpected(15,
                                () -> PasswordsService.createPassword(createPasswordModel, firstConsumerId, secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, firstConsumerToken);

        // Simulating verify KYC is not used because this action also verify email
        ConsumersDatabaseHelper.updateConsumerKyc(KycState.APPROVED.name(), firstConsumerId);

        final CreateConsumerModel secondCreateConsumerModel = createDuplicateConsumerModel(firstCreateConsumerModel, consumerProfileId);

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

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(secondConsumer.getLeft(), KycState.APPROVED.name());
    }

    @Test
    public void Consumer_DuplicateIdentitiesFirstConsumerMobileNotVerified_NewConsumerApproved() {

        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel(consumerProfileId);
        ConsumersHelper.createConsumer(firstCreateConsumerModel, secretKey);
        ConsumersHelper.verifyEmail(firstCreateConsumerModel.getRootUser().getEmail(), secretKey);

        final CreateConsumerModel secondCreateConsumerModel = createDuplicateConsumerModel(firstCreateConsumerModel, consumerProfileId);

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

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(secondConsumer.getLeft(), KycState.APPROVED.name());
    }

    @Test
    public void Consumer_DuplicateIdentitiesFirstConsumerDeactivated_NewConsumerRejected() {

        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(),
                secretKey, firstConsumer.getRight());

        InnovatorHelper.deactivateConsumer(new DeactivateIdentityModel(false, "TEMPORARY"),
                firstConsumer.getLeft(), InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        final CreateConsumerModel secondCreateConsumerModel = createDuplicateConsumerModel(firstCreateConsumerModel, consumerProfileId);

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

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(secondConsumer.getLeft(), KycState.REJECTED.name());
    }

    @Test
    public void Consumer_RetrieveUserIpFromSumSubApproved_Success() throws SQLException {

        final String ipAddressMultiApi = "127.0.0.1";

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setIpAddress(ipAddressMultiApi)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        assertEquals(createConsumerModel.getIpAddress(), getConsumerIpAddress(consumer.getRight()));

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        assertEquals(createConsumerModel.getIpAddress(), getConsumerIpAddress(consumer.getRight()));

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        final String ipFromSumSub = SumSubService.getApplicantTimeline(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("items[4].ipInfo.ip");

        final Map<String, String> consumerInfo = ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0);

        //Check if ip_address field in DB is populated according to Multi Api
        assertEquals(ipAddressMultiApi, consumerInfo.get("ip_address"));

        //Check if kyc_ip_address field in DB is populated according to sumsub callback
        assertEquals(ipFromSumSub, consumerInfo.get("kyc_ip_address"));

        //Consider to retrieve from get API
        assertEquals(ipAddressMultiApi, getConsumerIpAddress(consumer.getRight()));
    }

    @Test
    public void Consumer_SetEddCountriesEddRequiredFromSumsub_Rejected() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(nonFpsEnabledTenant.getConsumersProfileId());
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(createConsumerModel, nonFpsEnabledTenant.getSecretKey());

        //Verify email
        ConsumersHelper.verifyEmail(createConsumerModel.getRootUser().getEmail(), nonFpsEnabledTenant.getSecretKey());

        final String kycReferenceId = ConsumersHelper.startKyc(nonFpsEnabledTenant.getSecretKey(), consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(nonFpsEnabledTenant.getSharedKey(), consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
    }

    @Test
    public void Consumer_SetEddCountriesEddRequiredThenApprovedFromSumsub_Approved() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(nonFpsEnabledTenant.getConsumersProfileId());
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(createConsumerModel, nonFpsEnabledTenant.getSecretKey());

        //Verify email
        ConsumersHelper.verifyEmail(createConsumerModel.getRootUser().getEmail(), nonFpsEnabledTenant.getSecretKey());

        final String kycReferenceId = ConsumersHelper.startKyc(nonFpsEnabledTenant.getSecretKey(), consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(nonFpsEnabledTenant.getSharedKey(), consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        //add applicant tag as an EDD_Approved
        SumSubService.addApplicantTag(applicantData.getId(), "EDD_Approved")
                .then()
                .statusCode(SC_OK);

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
    }

    @Test
    public void Consumer_EddCountriesNotSetEddApprovedFromSumsub_Approved() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(createConsumerModel, secretKey);

        //Verify email
        ConsumersHelper.verifyEmail(createConsumerModel.getRootUser().getEmail(), secretKey);

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        final Map<String, List<Integer>> documentImageMap =
                SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        assertRequiredDocsStatus(weavrIdentity, documentImageMap, applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        //add applicant tag as an EDD_Approved
        SumSubService.addApplicantTag(applicantData.getId(), "EDD_Approved")
                .then()
                .statusCode(SC_OK);

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());
    }

    @Test
    public void Consumer_RetrieveUserIpFromSumSubRejected_Success() throws SQLException {

        final String ipAddressMultiApi = "127.0.0.1";

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setIpAddress(ipAddressMultiApi)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.IT)
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

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());

        final String ipFromSumSub = SumSubService.getApplicantTimeline(weavrIdentity.getAccessToken(), applicantData.getId())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("items[4].ipInfo.ip");

        final Map<String, String> consumerInfo = ConsumersDatabaseHelper.getConsumer(consumer.getLeft()).get(0);

        //Check if ip_address field in DB is populated according to Multi Api
        assertEquals(ipAddressMultiApi, consumerInfo.get("ip_address"));

        //Check if kyc_ip_address field in DB is populated according to sumsub callback
        assertEquals(ipFromSumSub, consumerInfo.get("kyc_ip_address"));

        //Consider to retrieve from get API
        assertEquals(ipAddressMultiApi, getConsumerIpAddress(consumer.getRight()));
    }

    @Test
    public void Consumer_StartKycWithPredefinedQuestionnaire_Success() throws SQLException {

        RegisteredCountriesSetCountriesModel setCountriesModel = RegisteredCountriesSetCountriesModel.builder().context(
                        RegisteredCountriesContextModel.builder().dimension(
                                RegisteredCountriesDimension.defaultDimensionModel(innovatorId, programmeId)).build())
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel("MT", "DE")).build()).build();

        AdminService.setApprovedNationalitiesConsumers(adminToken, setCountriesModel).then()
                .statusCode(SC_NO_CONTENT);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setAddress(AddressModel.DefaultAddressModel()
                                        .setCountry(CountryCode.DE)
                                        .build())
                                .setNationality("DE")
                                .build())
                        .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        List<PrefillDetailsModel> prefillDetailsList = Arrays.asList(
                new PrefillDetailsModel.Builder().setName("employmentStatus").setValue("SELF_EMPLOYED").build(),
                new PrefillDetailsModel.Builder().setName("industry").setValue("INFORMATIONAL_TECHNOLOGIES").build(),
                new PrefillDetailsModel.Builder().setName("incomingFundsFrom").setValue("INTRAEU").build(),
                new PrefillDetailsModel.Builder().setName("originOfFunds").setValue("ORDINARY_BUSINESS_ACTIVITY").build(),
                new PrefillDetailsModel.Builder().setName("otherOriginOfFunds").setValue("").build(),
                new PrefillDetailsModel.Builder().setName("expectedValueOfDeposits").setValue("morethan15k").build(),
                new PrefillDetailsModel.Builder().setName("expectedFrequencyOfIncomingTxs").setValue("lessthan2").build()
        );

        final String kycReferenceId = ConsumersHelper.startKycWithQuestionnaireValues(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight(), prefillDetailsList);

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("questionnaires.sections.section1.items.empstatus.value[0]", equalTo("SELF_EMPLOYED"))
                .body("questionnaires.sections.section1.items.industry.value[0]", equalTo("INFORMATIONAL_TECHNOLOGIES"))
                .body("questionnaires.sections.section1.items.receivefundsfrom.value[0]", equalTo("INTRAEU"))
                .body("questionnaires.sections.section1.items.originoffunds.value[0]", equalTo("ORDINARY_BUSINESS_ACTIVITY"))
                .body("questionnaires.sections.section1.items.otheroriginoffunds.value[0]", nullValue())
                .body("questionnaires.sections.section1.items.expectedvalueofdeposits.value[0]", equalTo("morethan15k"))
                .body("questionnaires.sections.section1.items.expectedfrequencyofincomingtxs.value[0]", equalTo("lessthan2"));
        verifySumsubState("INITIATED", consumer.getLeft());

    }

    @Test
    public void Consumers_CheckSipStatusOfIdentity_Success() {
        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);
        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        verifySipStatus(consumer.getLeft(), "NO", "NO");

        final List<String> rejectLabels = List.of(RejectLabels.COMPROMISED_PERSONS.name());
        SumSubHelper.setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL,
                Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());

        verifySipStatus(consumer.getLeft(), "NO", "NO");
    }

    @Test
    public void Consumer_UpdateNaturePersonV3Api_Success() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(consumerProfileId);
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(createConsumerModel, secretKey);

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final FixedInfoModel fixedInfoModel =
                SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        assertApplicantData(weavrIdentity, fixedInfoModel);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL_USER_IP);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        final long startTime = System.currentTimeMillis();

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());

        assertTrue(ensureIdentitySubscribed(consumer.getLeft()));

        final FixedInfoModel updateInfoModel =
                FixedInfoModel.defaultFixedInfoModel(applicantData.getFixedInfo())
                        .setPlaceOfBirth(RandomStringUtils.randomAlphabetic(5))
                        .setFirstName(RandomStringUtils.randomAlphabetic(5))
                        .setLastName(RandomStringUtils.randomAlphabetic(5))
                        .setDob("1980-12-12").build();

        SumSubHelper.submitApplicantInformation(updateInfoModel, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());

        final String consumerSubscriptionRequest = getUpdateNaturalPersonsApiRequest(consumer.getLeft());
        final JsonObject requestPayload = new Gson().fromJson(consumerSubscriptionRequest, JsonObject.class);

        assertEquals(requestPayload.get("first_name").getAsString(), updateInfoModel.getFirstName());
        assertEquals(requestPayload.get("last_name").getAsString(), updateInfoModel.getLastName());
        assertEquals(requestPayload.get("place_of_birth").getAsString(), updateInfoModel.getPlaceOfBirth());
        assertEquals(requestPayload.get("birthday").getAsString(), updateInfoModel.getDob());
    }

    @AfterEach
    public void setApprovedNationalitiesDefault() {

        final List<String> countries =
                Arrays.stream(CountryCode.values())
                        .filter(x -> !x.equals(CountryCode.AF))
                        .map(CountryCode::name)
                        .collect(Collectors.toList());

        final RegisteredCountriesSetCountriesModel setCountriesModel = RegisteredCountriesSetCountriesModel.builder().context(
                        RegisteredCountriesContextModel.builder().dimension(
                                RegisteredCountriesDimension.defaultDimensionModel(innovatorId, programmeId)).build())
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel(countries)).build()).build();

        final RegisteredCountriesSetCountriesModel setCountriesModel1 = RegisteredCountriesSetCountriesModel.builder().context(
                        RegisteredCountriesContextModel.builder().dimension(
                                RegisteredCountriesDimension.defaultDimensionModel(nonFpsEnabledTenant.getInnovatorId(), nonFpsEnabledTenant.getProgrammeId())).build())
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel(countries)).build()).build();

        AdminService.setApprovedNationalitiesConsumers(adminToken, setCountriesModel).then()
                .statusCode(SC_NO_CONTENT);

        AdminService.setApprovedNationalitiesConsumers(adminToken, setCountriesModel1).then()
                .statusCode(SC_NO_CONTENT);
    }

    private CreateConsumerModel createDefaultConsumerModel(final String consumerProfileId) {

        return CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
    }

    private void assertApplicantData(final IdentityDetailsModel weavrIdentity, final FixedInfoModel fixedInfoModel) {
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("fixedInfo.firstName", equalTo(fixedInfoModel.getFirstName()))
                .body("fixedInfo.lastName", equalTo(fixedInfoModel.getLastName()))
                .body("fixedInfo.dob", equalTo(fixedInfoModel.getDob()))
                .body("fixedInfo.nationality", equalTo(fixedInfoModel.getNationality()))
                .body("fixedInfo.country", equalTo(fixedInfoModel.getCountry()))
                .body("fixedInfo.placeOfBirth", equalTo(fixedInfoModel.getPlaceOfBirth()))
                .body("fixedInfo.phone", equalTo(fixedInfoModel.getPhone()))
                .body("fixedInfo.addresses[0].subStreet", equalTo(fixedInfoModel.getAddresses().get(0).getSubStreet()))
                .body("fixedInfo.addresses[0].street", equalTo(fixedInfoModel.getAddresses().get(0).getStreet()))
                .body("fixedInfo.addresses[0].state", equalTo(fixedInfoModel.getAddresses().get(0).getState()))
                .body("fixedInfo.addresses[0].town", equalTo(fixedInfoModel.getAddresses().get(0).getTown()))
                .body("fixedInfo.addresses[0].postCode", equalTo(fixedInfoModel.getAddresses().get(0).getPostCode()))
                .body("fixedInfo.addresses[0].country", equalTo(fixedInfoModel.getAddresses().get(0).getCountry()))
                .body("review.reviewStatus", equalTo("init"));
    }

    private void assertRequiredDocsStatus(final IdentityDetailsModel weavrIdentity,
                                          final Map<String, List<Integer>> documentImageMap,
                                          final String applicantId) {

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantId)
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo(ID_CARD_FRONT.getType()))
                .body("IDENTITY.imageIds[0]", equalTo(documentImageMap.get(ID_CARD_FRONT.getType()).get(0)))
                .body("IDENTITY.imageIds[1]", equalTo(documentImageMap.get(ID_CARD_BACK.getType()).get(1)))
                .body("SELFIE.idDocType", equalTo(SELFIE.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE.getType()).get(0)))
                .body("PROOF_OF_RESIDENCE.idDocType", equalTo(UTILITY_BILL_USER_IP.getType()))
                .body("PROOF_OF_RESIDENCE.imageIds[0]", equalTo(documentImageMap.get(UTILITY_BILL_USER_IP.getType()).get(0)));
    }

    private void assertRequiredDocsStatusConsumerUk(final IdentityDetailsModel weavrIdentity,
                                                    final Map<String, List<Integer>> documentImageMap,
                                                    final String applicantId) {

        SumSubHelper.getApplicantRequiredDocStatus(weavrIdentity.getAccessToken(), applicantId)
                .then()
                .statusCode(SC_OK)
                .body("IDENTITY.idDocType", equalTo("DRIVERS"))
                .body("SELFIE.idDocType", equalTo(SELFIE.getType()))
                .body("SELFIE.imageIds[0]", equalTo(documentImageMap.get(SELFIE.getType()).get(0)))
                .body("PROOF_OF_RESIDENCE.idDocType", equalTo(UTILITY_BILL_USER_IP.getType()))
                .body("PROOF_OF_RESIDENCE.imageIds[0]", equalTo(documentImageMap.get(UTILITY_BILL_USER_IP.getType()).get(0)));
    }

    public void verifySumsubState(final String expectedState,
                                  final String consumerId) throws SQLException {
        final String sumsubStatus = SumsubDatabaseHelper.getConsumerTenant(consumerId).get(0).get("status");
        assertEquals(expectedState, sumsubStatus);
    }

    public String getConsumerIpAddress(final String consumerToken) {
        return ConsumersService.getConsumers(secretKey, consumerToken)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("ipAddress");
    }

    public void verifyPepStatus(final String consumerId,
                                final String pepEffective,
                                final String pepOngoing) {

        TestHelper.ensureDatabaseResultAsExpected(30,
                () -> ConsumersDatabaseHelper.getConsumer(consumerId),
                x -> x.size() > 0 && x.get(0).get("pep_effective").equals(pepEffective) && x.get(0).get("pep_ongoing").equals(pepOngoing),
                Optional.of(String.format("Subscription for identity with id %s not '%s'", consumerId, pepEffective)));

    }

    public void verifySipStatus(final String consumerId,
                                final String sipEffective,
                                final String sipOngoing) {

        TestHelper.ensureDatabaseResultAsExpected(30,
                () -> ConsumersDatabaseHelper.getConsumer(consumerId),
                x -> !x.isEmpty() && x.get(0).get("sip_effective").equals(sipEffective) && x.get(0).get("sip_ongoing").equals(sipOngoing),
                Optional.of(String.format("Subscription for identity with id %s not '%s'", consumerId, sipEffective)));
    }

    public CreateConsumerModel createDuplicateConsumerModel(final CreateConsumerModel createConsumerModel, final String profileId) {
        return CreateConsumerModel.DefaultCreateConsumerModel(profileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(createConsumerModel.getRootUser().getName())
                        .setSurname(createConsumerModel.getRootUser().getSurname())
                        .setDateOfBirth(createConsumerModel.getRootUser().getDateOfBirth())
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
    }

    private static Stream<Arguments> merchantTokenProvider() {
        return Stream.of(
                arguments(CountryCode.DE, PAYNETICS_EEA_TOKEN, applicationOne),
                arguments(CountryCode.GB, PAYNETICS_UK_TOKEN, applicationOneUk),
                arguments(CountryCode.IM, PAYNETICS_UK_TOKEN, applicationOneUk)
        );
    }

    private static void checkRejectionCommentsGetKycApiResponses(final String consumerId, final String rejectionComment){
        opc.services.admin.AdminService.getConsumerKyc(consumerId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("rejectionComment", equalTo(rejectionComment));

        opc.services.innovator.InnovatorService.getConsumerKyc(consumerId, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("rejectionComment", equalTo(rejectionComment));

        opc.services.adminnew.AdminService.getConsumerKyc(adminToken, consumerId)
                .then()
                .statusCode(SC_OK)
                .body("rejectionComment", equalTo(rejectionComment));

        opc.services.innovatornew.InnovatorService.getConsumerKyc(innovatorToken, consumerId)
                .then()
                .statusCode(SC_OK)
                .body("rejectionComment", equalTo(rejectionComment));
    }
}
