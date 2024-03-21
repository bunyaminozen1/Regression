package opc.junit.adminnew.consumers;

import opc.enums.opc.KycState;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.admin.UpdateKycModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.services.adminnew.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class PatchConsumersTests extends BaseConsumersSetup{
    private CreateConsumerModel createConsumerModel;
    private String consumerId;

    @BeforeEach
    public void Setup() {
        createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
    }

    @Test
    public void PatchConsumerKyc_EmailNotVerified_Success() {

        final UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(false)
                .setMobileVerified(true)
                .setFullDueDiligence(KycState.APPROVED.name())
                .build();

       AdminService.updateConsumerKyc(updateKycModel, adminToken, consumerId)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("emailVerified", equalTo(false))
                .body("mobileVerified", equalTo(true));
    }

    @Test
    public void PatchConsumerKyc_MobileAndEmailNotVerified_Success() {

        final UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(false)
                .setMobileVerified(false)
                .setFullDueDiligence(KycState.APPROVED.name())
                .build();

       AdminService.updateConsumerKyc(updateKycModel, adminToken, consumerId)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("emailVerified", equalTo(false))
                .body("mobileVerified", equalTo(false));
    }

    @Test
    public void PatchConsumerKyc_MobileAndEmailVerifiedToPendingState_Success() {

        final UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(true)
                .setFullDueDiligence(KycState.PENDING_REVIEW.name())
                .build();

        AdminService.updateConsumerKyc(updateKycModel, adminToken, consumerId)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("emailVerified", equalTo(true))
                .body("mobileVerified", equalTo(true));
    }

    @Test
    public void PatchConsumerKyc_MobileAndEmailVerifiedToInitiatedState_Success() {

        final UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setEmailVerified(true)
                .setMobileVerified(true)
                .setFullDueDiligence(KycState.INITIATED.name())
                .build();

        AdminService.updateConsumerKyc(updateKycModel, adminToken, consumerId)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.INITIATED.name()))
                .body("emailVerified", equalTo(true))
                .body("mobileVerified", equalTo(true));
    }

    @Test
    public void PatchConsumerKyc_KycNotStarted_Success() {

        final UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setFullDueDiligence(KycState.NOT_STARTED.name())
                .build();

        AdminService.updateConsumerKyc(updateKycModel, adminToken, consumerId)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.NOT_STARTED.name()));
    }

    @Test
    public void PatchConsumerKyc_NoToken_Unauthorised() {

        final UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setFullDueDiligence(KycState.NOT_STARTED.name())
                .build();

        AdminService.updateConsumerKyc(updateKycModel, "", consumerId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PatchConsumerKyc_IncorrectState_BadRequest() {

        final UpdateKycModel updateKycModel = UpdateKycModel.builder()
                .setFullDueDiligence(RandomStringUtils.randomAlphabetic(10))
                .build();

        AdminService.updateConsumerKyc(updateKycModel, adminToken, consumerId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }
}
