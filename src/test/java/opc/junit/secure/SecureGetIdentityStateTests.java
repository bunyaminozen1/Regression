package opc.junit.secure;

import io.restassured.response.Response;
import opc.enums.opc.KycLevel;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static opc.enums.sumsub.IdDocType.ID_CARD_BACK;
import static opc.enums.sumsub.IdDocType.ID_CARD_FRONT;
import static opc.enums.sumsub.IdDocType.PASSPORT;
import static opc.enums.sumsub.IdDocType.SELFIE;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL2;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SecureGetIdentityStateTests extends BaseSecureSetup {
    @Test
    public void SecureGetIdentityTests_DocumentsNotUploaded_True() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        SecureService.getIdentityState(sharedKey, consumer.getRight(), kycReferenceId)
                .then()
                .statusCode(SC_OK)
                .body("kycInputRequired", equalTo(true))
                .body("askIdDocumentHasAddress", equalTo(true));
    }

    @Test
    public void SecureGetIdentityTests_DocumentsUploadedPendingState_False() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);
        final List<IdDocType> requiredDocuments = Arrays.asList(PASSPORT, SELFIE, UTILITY_BILL);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

        final Response response = getIdentityResponse(consumer, kycReferenceId, false);
        assertEquals(false, response.jsonPath().get("kycInputRequired"));
        assertEquals(true, response.jsonPath().get("askIdDocumentHasAddress"));
    }

    @Test
    public void SecureGetIdentityTests_KycLevel1_False() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
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


        final Response response = getIdentityResponse(consumer, kycReferenceId, false);
        assertEquals(false, response.jsonPath().get("kycInputRequired"));
        assertEquals(false, response.jsonPath().get("askIdDocumentHasAddress"));
    }

    @Test
    public void SecureGetIdentityTests_RejectedRetry_TrueFalse() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);
        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);
        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL, UTILITY_BILL2);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

        final Response response = getIdentityResponse(consumer, kycReferenceId, false);
        assertEquals(false, response.jsonPath().get("kycInputRequired"));
        assertEquals(true, response.jsonPath().get("askIdDocumentHasAddress"));

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.RETRY,
                        Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());

        final Response responseAfterRejection = getIdentityResponse(consumer, kycReferenceId, true);
        assertEquals(true, responseAfterRejection.jsonPath().get("kycInputRequired"));
        assertEquals(true, responseAfterRejection.jsonPath().get("askIdDocumentHasAddress"));
    }

    private Response getIdentityResponse(Pair<String, String> consumer, String kycReferenceId, boolean expectedValue) {

        return TestHelper.ensureAsExpected(30,
                () -> SecureService.getIdentityState(sharedKey, consumer.getRight(), kycReferenceId),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("kycInputRequired").equals(expectedValue),
                Optional.of(String.format("Expecting 200 with Kyc Input Required set to %s for identity %s, check logged payload", expectedValue, consumer.getLeft())));
    }
}
