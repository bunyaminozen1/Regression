package data;

import opc.enums.opc.CountryCode;
import opc.enums.opc.KycLevel;
import opc.enums.opc.Occupation;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.StartKycModel;
import opc.models.shared.AddressModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.multi.ConsumersService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.sumsub.IdDocType.ID_CARD_BACK;
import static opc.enums.sumsub.IdDocType.ID_CARD_FRONT;
import static opc.enums.sumsub.IdDocType.SELFIE;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL_USER_IP;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class ConsumerTests extends BaseTestSetup {

    @ParameterizedTest
    @EnumSource(value = Occupation.class, mode = EnumSource.Mode.EXCLUDE, names = { "UNKNOWN" })
    public void CreateConsumer_Success(final Occupation occupation){

        IntStream.range(0, new RandomDataGenerator().nextInt(1, 5)).forEach(i -> {
            final CreateConsumerModel createConsumerModel =
                    CreateConsumerModel.dataCreateConsumerModel(consumerProfileId)
                            .setRootUser(ConsumerRootUserModel.dataRootUserModel()
                                    .setOccupation(occupation).build())
                            .build();

            ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                    .then()
                    .statusCode(SC_OK);
        });
    }

    @ParameterizedTest
    @EnumSource(value = KycLevel.class)
    public void CreateConsumer_KycInitiated_Success(final KycLevel kycLevel){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.dataCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        ConsumersService.startConsumerKyc(StartKycModel.builder().setKycLevel(kycLevel).build(),
                secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @ParameterizedTest
    @EnumSource(value = KycLevel.class)
    public void CreateConsumer_KycApproved_Success(final KycLevel kycLevel){

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.dataCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        ConsumersService.startConsumerKyc(StartKycModel.builder().setKycLevel(kycLevel).build(),
                        secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK);

        verifyKyc(consumer.getLeft(), secretKey);
    }

    @Test
    public void Consumer_KycPending_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.dataCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.dataRootUserModel()
                        .setAddress(AddressModel.dataAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final Pair<IdentityDetailsModel, SumSubApplicantDataModel> applicantData =
                updateKycData(consumer.getRight(), kycReferenceId);

        SumSubHelper.setApplicantInPendingState(applicantData.getLeft(), applicantData.getRight().getId());

        SumSubHelper.getApplicantData(applicantData.getLeft().getAccessToken(), applicantData.getLeft().getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));
    }

    @Test
    public void Consumer_KycRejected_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.dataCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.dataRootUserModel()
                        .setAddress(AddressModel.dataAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final Pair<IdentityDetailsModel, SumSubApplicantDataModel> applicantData =
                updateKycData(consumer.getRight(), kycReferenceId);

        SumSubHelper.setApplicantInPendingState(applicantData.getLeft(), applicantData.getRight().getId());

        SumSubHelper.getApplicantData(applicantData.getLeft().getAccessToken(), applicantData.getLeft().getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        final List<String> rejectLabels = Collections.singletonList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL, Optional.of("Issue with verification."),
                        applicantData.getRight().getId(), applicantData.getLeft().getExternalUserId());

        SumSubHelper.getApplicantData(applicantData.getLeft().getAccessToken(), applicantData.getLeft().getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"))
                .body("review.reviewResult.reviewRejectType", equalTo("FINAL"));
    }

    private Pair<IdentityDetailsModel, SumSubApplicantDataModel> updateKycData(final String consumerAuthenticationToken, final String kycReferenceId){

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumerAuthenticationToken, kycReferenceId).getParams();

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

        return Pair.of(weavrIdentity, applicantData);
    }
}
