package opc.junit.sumsub;

import commons.config.ConfigHelper;
import opc.enums.opc.CountryCode;
import opc.enums.opc.KycLevel;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.admin.SetApplicantLevelModel;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.StartKycModel;
import opc.models.shared.AddressModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.services.admin.AdminService;
import opc.services.multi.ConsumersService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.CoreMatchers.equalTo;

public class SumSubConsumerConfigurationTests extends BaseSumSubSetup {

    final private static List<SetApplicantLevelModel> applicantLevelModels = new ArrayList<>();

    @AfterEach
    public void deleteConfiguration(){

        for (SetApplicantLevelModel applicantLevelModel : applicantLevelModels) {
            AdminService.deleteConsumerLevelConfiguration(applicantLevelModel, adminToken)
                    .then()
                    .statusCode(SC_OK);
        }
    }

    /**
     * With new changes, we can configure a Sumsub applicant level for a specific corporate or consumer type
     * on tenant or programme level.This test configures a valid(exist in SumSub) applicant level for
     * KYC_Level_2 consumer and checks applicant level on SumSub.
     */

    @ParameterizedTest
    @ValueSource(strings = {"consumer", "consumer-level-1"})
    public void Consumer_DynamicMappingSumSubApplicantLevelWithoutConfiguration_Success(final String applicantLevel) {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final String kycReferenceId;

        if (applicantLevel.equals("consumer")) {
            kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());
        } else {
            kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumer.getRight());
        }

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-%s",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment(), applicantLevel)));
    }

    @Test
    public void Consumer_DynamicMappingSumSubApplicantLevelForProgramme_Success() {

        final String applicantLevel = "consumer-no-POA";

        //We set configuration for application two
        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setConsumerApplicantLevelProgramme(
                applicantLevel, applicationTwo.getProgrammeId(), "CONSUMER_KYC_LEVEL_2");

        AdminService.createConsumerLevelConfiguration(setApplicantLevelModel, adminToken)
                .then()
                .statusCode(SC_OK);

        applicantLevelModels.add(setApplicantLevelModel);

        final Pair<String, String> consumerAppTwo = ConsumersHelper
                .createAuthenticatedConsumer(applicationTwo.getConsumersProfileId(), applicationTwo.getSecretKey());

        final String kycReferenceIdAppTwo = ConsumersHelper.startKyc(applicationTwo.getSecretKey(), consumerAppTwo.getRight());

        final IdentityDetailsModel weavrIdentityAppTwo =
                SumSubHelper.getWeavrIdentityDetails(applicationTwo.getSharedKey(), consumerAppTwo.getRight(), kycReferenceIdAppTwo).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppTwo.getAccessToken(), weavrIdentityAppTwo.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-" + applicantLevel,
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));

        // Creating a consumer with application One

        final Pair<String, String> consumerAppOne = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        final String kycReferenceIdAppOne = ConsumersHelper.startKyc(secretKey, consumerAppOne.getRight());

        final IdentityDetailsModel weavrIdentityAppOne =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumerAppOne.getRight(), kycReferenceIdAppOne).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppOne.getAccessToken(), weavrIdentityAppOne.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-consumer",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));
    }

    @Test
    public void Consumer_DynamicMappingSumSubApplicantLevelKycLevel1ForProgramme_Success() {

        final String applicantLevel = "consumer-no-POA";
        //We set configuration for application two
        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setConsumerApplicantLevelProgramme(
                applicantLevel, applicationTwo.getProgrammeId(), "CONSUMER_KYC_LEVEL_1");

        AdminService.createConsumerLevelConfiguration(setApplicantLevelModel, adminToken)
                .then()
                .statusCode(SC_OK);

        applicantLevelModels.add(setApplicantLevelModel);

        final Pair<String, String> consumerAppTwo = ConsumersHelper
                .createAuthenticatedConsumer(applicationTwo.getConsumersProfileId(), applicationTwo.getSecretKey());

        final String kycReferenceIdAppTwo = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, applicationTwo.getSecretKey(), consumerAppTwo.getRight());

        final IdentityDetailsModel weavrIdentityAppTwo =
                SumSubHelper.getWeavrIdentityDetails(applicationTwo.getSharedKey(), consumerAppTwo.getRight(), kycReferenceIdAppTwo).getParams();


        SumSubHelper.getApplicantData(weavrIdentityAppTwo.getAccessToken(), weavrIdentityAppTwo.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-" + applicantLevel,
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));

        // Creating a consumer with application One

        final Pair<String, String> consumerAppOne = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        final String kycReferenceIdAppOne = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, secretKey, consumerAppOne.getRight());

        final IdentityDetailsModel weavrIdentityAppOne =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumerAppOne.getRight(), kycReferenceIdAppOne).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppOne.getAccessToken(), weavrIdentityAppOne.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-consumer-level-1",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));
    }

    @Test
    public void Consumer_DynamicMappingSumSubApplicantLevelForIdentity_Success() {

        final String applicantLevel = "consumer-1POA-residency";

        //We set configuration for a specific consumer
        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel();
        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedConsumer(firstCreateConsumerModel, secretKey);

        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setConsumerApplicantLevelForIdentity(
                applicantLevel, programmeId, firstConsumer.getLeft(), "CONSUMER_KYC_LEVEL_2");

        AdminService.createConsumerLevelConfiguration(setApplicantLevelModel, adminToken)
                .then()
                .statusCode(SC_OK);

        applicantLevelModels.add(setApplicantLevelModel);

        final String firstKycReferenceId = ConsumersHelper.startKyc(secretKey, firstConsumer.getRight());

        final IdentityDetailsModel firstWeavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, firstConsumer.getRight(), firstKycReferenceId).getParams();

        SumSubHelper.getApplicantData(firstWeavrIdentity.getAccessToken(), firstWeavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-" + applicantLevel,
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));

        // Creating a consumer without configuration

        final CreateConsumerModel secondCreateConsumerModel = createDefaultConsumerModel();
        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerModel, secretKey);

        final String secondKycReferenceId = ConsumersHelper.startKyc(secretKey, secondConsumer.getRight());

        final IdentityDetailsModel secondWeavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, secondConsumer.getRight(), secondKycReferenceId).getParams();

        SumSubHelper.getApplicantData(secondWeavrIdentity.getAccessToken(), secondWeavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-consumer",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));
    }

    @Test
    public void Consumer_DynamicMappingSumSubApplicantLevelForTenant_Success() {

        final String applicantLevel = "consumer-1POA-residency";

        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setConsumerApplicantLevelForTenant(
                applicantLevel, nonFpsEnabledTenant.getInnovatorId(), "CONSUMER_KYC_LEVEL_2");

        AdminService.createConsumerLevelConfiguration(setApplicantLevelModel, adminToken)
                .then()
                .statusCode(SC_OK);

        applicantLevelModels.add(setApplicantLevelModel);

        final Pair<String, String> consumerNonFps = ConsumersHelper
                .createAuthenticatedConsumer(nonFpsEnabledTenant.getConsumersProfileId(), nonFpsEnabledTenant.getSecretKey());

        final String kycReferenceIdNonFps = ConsumersHelper.startKyc(nonFpsEnabledTenant.getSecretKey(), consumerNonFps.getRight());

        final IdentityDetailsModel weavrIdentityNonFps =
                SumSubHelper.getWeavrIdentityDetails(nonFpsEnabledTenant.getSharedKey(), consumerNonFps.getRight(), kycReferenceIdNonFps).getParams();

        SumSubHelper.getApplicantData(weavrIdentityNonFps.getAccessToken(), weavrIdentityNonFps.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-" + applicantLevel,
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));

        final CreateConsumerModel secondCreateConsumerAppOne = createDefaultConsumerModel();
        final Pair<String, String> consumerAppOne = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerAppOne, secretKey);

        final String kycReferenceIdAppOne = ConsumersHelper.startKyc(secretKey, consumerAppOne.getRight());

        final IdentityDetailsModel weavrIdentityAppOne =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumerAppOne.getRight(), kycReferenceIdAppOne).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppOne.getAccessToken(), weavrIdentityAppOne.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-consumer",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));
    }

    @Test
    public void Consumer_DynamicMappingSumSubApplicantLevelMultipleConfigurations_Success() {

        final String levelForTenantSemi = "consumer-1POA-residency";
        final String levelForAppTwo = "consumer-1POA-residency";
        final String levelForAppOne = "consumer-no-POA";
        final String levelForConsumer = "consumer-level-1";

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(applicationOne.getConsumersProfileId(), secretKey);

        // Set config for SemiTenant
        final SetApplicantLevelModel setApplicantLevelModelTenantSemi = SetApplicantLevelModel.setConsumerApplicantLevelForTenant(
                levelForTenantSemi, semiPasscodeApp.getInnovatorId(), "CONSUMER_KYC_LEVEL_2");
        AdminService.createConsumerLevelConfiguration(setApplicantLevelModelTenantSemi, adminToken)
                .then()
                .statusCode(SC_OK);

        // Set config for AppTwo
        final SetApplicantLevelModel setApplicantLevelModelAppTwo = SetApplicantLevelModel.setConsumerApplicantLevelProgramme(
                levelForAppTwo, applicationTwo.getProgrammeId(), "CONSUMER_KYC_LEVEL_2");
        AdminService.createConsumerLevelConfiguration(setApplicantLevelModelAppTwo, adminToken)
                .then()
                .statusCode(SC_OK);

        // Set config for AppOne
        final SetApplicantLevelModel setApplicantLevelModelAppOne = SetApplicantLevelModel.setConsumerApplicantLevelProgramme(
                levelForAppOne, programmeId, "CONSUMER_KYC_LEVEL_2");
        AdminService.createConsumerLevelConfiguration(setApplicantLevelModelAppOne, adminToken)
                .then()
                .statusCode(SC_OK);

        // Set config for consumer under AppOne
        final SetApplicantLevelModel setApplicantLevelModelIdentity = SetApplicantLevelModel.setConsumerApplicantLevelForIdentity(
                levelForConsumer, programmeId, consumer.getLeft(), "CONSUMER_KYC_LEVEL_2");
        AdminService.createConsumerLevelConfiguration(setApplicantLevelModelIdentity, adminToken)
                .then()
                .statusCode(SC_OK);

        applicantLevelModels.addAll(List.of(setApplicantLevelModelTenantSemi, setApplicantLevelModelAppTwo, setApplicantLevelModelAppOne, setApplicantLevelModelIdentity));

        // Create a consumer under nonFpsTenant (no configuration, default value)

        final Pair<String, String> consumerAppNonFps = ConsumersHelper
                .createAuthenticatedConsumer(nonFpsEnabledTenant.getConsumersProfileId(), nonFpsEnabledTenant.getSecretKey());

        final String kycReferenceIdNonFps = ConsumersHelper.startKyc(nonFpsEnabledTenant.getSecretKey(), consumerAppNonFps.getRight());

        final IdentityDetailsModel weavrIdentityAppNonFps =
                SumSubHelper.getWeavrIdentityDetails(nonFpsEnabledTenant.getSharedKey(), consumerAppNonFps.getRight(), kycReferenceIdNonFps).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppNonFps.getAccessToken(), weavrIdentityAppNonFps.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-consumer",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));


        // create an identity under semiPasscodeApp

        final Pair<String, String> consumerSemiPassCode = ConsumersHelper
                .createAuthenticatedConsumer(semiPasscodeApp.getConsumersProfileId(), semiPasscodeApp.getSecretKey());

        final String kycReferenceIdSemiPasscode = ConsumersHelper.startKyc(semiPasscodeApp.getSecretKey(), consumerSemiPassCode.getRight());

        final IdentityDetailsModel weavrIdentityAppSemiPasscode =
                SumSubHelper.getWeavrIdentityDetails(semiPasscodeApp.getSharedKey(), consumerSemiPassCode.getRight(), kycReferenceIdSemiPasscode).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppSemiPasscode.getAccessToken(), weavrIdentityAppSemiPasscode.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-%s",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment(), levelForTenantSemi)));

        // create an identity under semiScaSendsApp

        final Pair<String, String> consumerSemiScaSends = ConsumersHelper
                .createAuthenticatedConsumer(semiScaSendsApp.getConsumersProfileId(), semiScaSendsApp.getSecretKey());

        final String kycReferenceIdSemiScaSends = ConsumersHelper.startKyc(semiScaSendsApp.getSecretKey(), consumerSemiScaSends.getRight());

        final IdentityDetailsModel weavrIdentityAppSemiScaSends =
                SumSubHelper.getWeavrIdentityDetails(semiScaSendsApp.getSharedKey(), consumerSemiScaSends.getRight(), kycReferenceIdSemiScaSends).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppSemiScaSends.getAccessToken(), weavrIdentityAppSemiScaSends.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-%s",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment(), levelForTenantSemi)));

        // create an identity under appThree of tenant 10

        final Pair<String, String> consumerAppThree = ConsumersHelper
                .createAuthenticatedConsumer(applicationThree.getConsumersProfileId(), applicationThree.getSecretKey());

        final String kycReferenceIdAppThree = ConsumersHelper.startKyc(applicationThree.getSecretKey(), consumerAppThree.getRight());

        final IdentityDetailsModel weavrIdentityAppThree =
                SumSubHelper.getWeavrIdentityDetails(applicationThree.getSharedKey(), consumerAppThree.getRight(), kycReferenceIdAppThree).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppThree.getAccessToken(), weavrIdentityAppThree.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-consumer",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment())));

        // create an identity under appOne of tenant 10 ( should use levelAppOne)

        final Pair<String, String> consumerAppOne = ConsumersHelper
                .createAuthenticatedConsumer(applicationOne.getConsumersProfileId(), applicationOne.getSecretKey());

        final String kycReferenceIdAppOne = ConsumersHelper.startKyc(applicationOne.getSecretKey(), consumerAppOne.getRight());

        final IdentityDetailsModel weavrIdentityAppOne =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), consumerAppOne.getRight(), kycReferenceIdAppOne).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppOne.getAccessToken(), weavrIdentityAppOne.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-%s",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment(), levelForAppOne)));


        // create an identity under appTwo of tenant 10 ( should use levelAppTwo)

        final Pair<String, String> consumerAppTwo = ConsumersHelper
                .createAuthenticatedConsumer(applicationTwo.getConsumersProfileId(), applicationTwo.getSecretKey());

        final String kycReferenceIdAppTwo = ConsumersHelper.startKyc(applicationTwo.getSecretKey(), consumerAppTwo.getRight());

        final IdentityDetailsModel weavrIdentityAppTwo =
                SumSubHelper.getWeavrIdentityDetails(applicationTwo.getSharedKey(), consumerAppTwo.getRight(), kycReferenceIdAppTwo).getParams();

        SumSubHelper.getApplicantData(weavrIdentityAppTwo.getAccessToken(), weavrIdentityAppTwo.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-%s",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment(), levelForAppTwo)));


        // check consumer ( should use levelAppTwo)

        final String kycReferenceIdConsumer = ConsumersHelper.startKyc(applicationOne.getSecretKey(), consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), consumer.getRight(), kycReferenceIdConsumer).getParams();

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(String.format("%s-%s",
                        ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment(), levelForConsumer)));
    }

    /**
     * This test configures an invalid(non-exist on SumSub) applicant level for a valid corporation type. Sumsub returns 404,
     * but internally we get 503 and following message in the logs
     * "Failed to create Sum Sub applicant for externalUserId: {} and level {} may it be a configuration mismatch"
     */
    @Test
    public void Consumer_InvalidSumSubApplicantLevel_ServiceUnavailable() {

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setConsumerApplicantLevelForIdentity(
                "consumer-random", programmeId, consumer.getLeft(), "CONSUMER_KYC_LEVEL_2");

        AdminService.createConsumerLevelConfiguration(setApplicantLevelModel, adminToken)
                .then()
                .statusCode(SC_OK);

        applicantLevelModels.add(setApplicantLevelModel);

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_SERVICE_UNAVAILABLE);
    }

    private CreateConsumerModel createDefaultConsumerModel() {

        return CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
    }
}
