package opc.junit.multi.consumers;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.IdentityType;
import opc.enums.opc.KycLevel;
import opc.enums.opc.KycState;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.StartKycModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ConsumersService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetConsumersKycTests extends BaseConsumersSetup {

    private String consumerId;
    private String authenticationToken;

    @BeforeEach
    public void Setup(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        authenticationToken = authenticatedConsumer.getRight();
    }

    @Test
    public void GetConsumersKyc_NotStarted_Success(){
        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.NOT_STARTED.name()))
                .body("ongoingFullDueDiligence", is(KycState.NOT_STARTED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

    }

    @ParameterizedTest
    @EnumSource(value = KycLevel.class)
    public void GetConsumersKyc_Initiated_Success(final KycLevel kycLevel){
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startConsumerKyc(StartKycModel.startKycModel(kycLevel), secretKey, authenticationToken),
                SC_OK);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.INITIATED.name()))
                .body("ongoingFullDueDiligence", is(KycState.INITIATED.name()))
                .body("kycLevel", equalTo(kycLevel.name()))
                .body("ongoingKycLevel", equalTo(kycLevel.name()));

    }

    @Test
    public void GetConsumersKyc_InitiatedNoKycLevel_Success(){
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startConsumerKyc(StartKycModel.builder().build(), secretKey, authenticationToken),
                SC_OK);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.INITIATED.name()))
                .body("ongoingFullDueDiligence", is(KycState.INITIATED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

    }

    @ParameterizedTest
    @EnumSource(value = KycLevel.class)
    public void GetConsumersKyc_InitiatedByKycSumsub_Success(final KycLevel kycLevel) {

        final ValidatableResponse response =
                ConsumersService.startKycMobile(StartKycModel.startKycModel(kycLevel), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK);

        response
                .body("verificationFlow", equalTo("consumer-kyc-flow-mobile"))
                .body("accessToken", notNullValue())
                .body("identityType", equalTo("consumers"))
                .body("externalUserId", equalTo(String.format("cons%s", consumerId)))
                .body("kycProviderKey", equalTo("sumsub"))
                .body("kycLevel", equalTo(kycLevel.name()));

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.INITIATED.name()))
                .body("ongoingFullDueDiligence", is(KycState.INITIATED.name()))
                .body("kycLevel", equalTo(kycLevel.name()))
                .body("ongoingKycLevel", equalTo(kycLevel.name()));

    }

    @Test
    public void GetConsumersKyc_InitiatedByKycSumsubNoKycLevel_Success() {

        final ValidatableResponse response =
                ConsumersService.startKycMobile(StartKycModel.builder().build(), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK);

        response
                .body("verificationFlow", equalTo("consumer-kyc-flow-mobile"))
                .body("accessToken", notNullValue())
                .body("identityType", equalTo("consumers"))
                .body("externalUserId", equalTo(String.format("cons%s", consumerId)))
                .body("kycProviderKey", equalTo("sumsub"))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.INITIATED.name()))
                .body("ongoingFullDueDiligence", is(KycState.INITIATED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

    }

    @Test
    public void GetConsumersKyc_PendingReview_Success() throws SQLException {

        ConsumersDatabaseHelper.updateConsumerKyc("PENDING_REVIEW", consumerId);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("ongoingFullDueDiligence", is(KycState.PENDING_REVIEW.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

    }

    @Test
    public void GetConsumersKyc_Rejected_Success() throws SQLException {

        ConsumersDatabaseHelper.updateConsumerKyc("REJECTED", consumerId);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.REJECTED.name()))
                .body("ongoingFullDueDiligence", is(KycState.REJECTED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

    }

    @Test
    public void GetConsumersKyc_ApprovedKycLevel1WithStepup_Success(){
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_1), secretKey, authenticationToken),
                SC_OK);

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken),
                SC_OK);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.INITIATED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @Test
    public void GetConsumersKyc_ApprovedKycLevel2_Success(){
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken),
                SC_OK);

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @Test
    public void GetConsumersKyc_ApprovedNoKycLevel_Success(){
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startConsumerKyc(StartKycModel.builder().build(), secretKey, authenticationToken),
                SC_OK);

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @Test
    public void GetConsumersKyc_InitiatedByKycSumsubApprovedKycLevel1WithStepup_Success() {

        final ValidatableResponse response =
                ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_1), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK);

        response
                .body("verificationFlow", equalTo("consumer-kyc-flow-mobile"))
                .body("accessToken", notNullValue())
                .body("identityType", equalTo("consumers"))
                .body("externalUserId", equalTo(String.format("cons%s", consumerId)))
                .body("kycProviderKey", equalTo("sumsub"))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()));

        final ValidatableResponse response1 =
                ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK);

        response1
                .body("verificationFlow", equalTo("consumer-kyc-flow-mobile"))
                .body("accessToken", notNullValue())
                .body("identityType", equalTo("consumers"))
                .body("externalUserId", equalTo(String.format("cons%s", consumerId)))
                .body("kycProviderKey", equalTo("sumsub"))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.INITIATED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_1.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @Test
    public void GetConsumersKyc_InitiatedByKycSumsubApprovedKycLevel2_Success() {

        final ValidatableResponse response =
                ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK);

        response
                .body("verificationFlow", equalTo("consumer-kyc-flow-mobile"))
                .body("accessToken", notNullValue())
                .body("identityType", equalTo("consumers"))
                .body("externalUserId", equalTo(String.format("cons%s", consumerId)))
                .body("kycProviderKey", equalTo("sumsub"))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @Test
    public void GetConsumersKyc_InitiatedByKycSumsubApprovedNoKycLevel_Success() {

        final ValidatableResponse response =
                ConsumersService.startKycMobile(StartKycModel.builder().build(), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK);

        response
                .body("verificationFlow", equalTo("consumer-kyc-flow-mobile"))
                .body("accessToken", notNullValue())
                .body("identityType", equalTo("consumers"))
                .body("externalUserId", equalTo(String.format("cons%s", consumerId)))
                .body("kycProviderKey", equalTo("sumsub"))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("fullDueDiligence", is(KycState.APPROVED.name()))
                .body("ongoingFullDueDiligence", is(KycState.APPROVED.name()))
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()))
                .body("ongoingKycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @Test
    public void GetConsumersKyc_InvalidApiKey_Unauthorised(){

        ConsumersService.getConsumerKyc("abc", authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetConsumersKyc_NoApiKey_Unauthorised(){

        ConsumersService.getConsumerKyc("", authenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetConsumersKyc_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ConsumersService.getConsumerKyc(secretKey, authenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetConsumersKyc_RootUserLoggedOut_Unauthorised(){

        final String unauthenticatedToken = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        ConsumersService.getConsumerKyc(secretKey, unauthenticatedToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetConsumersKyc_BackofficeImpersonator_Forbidden(){
        ConsumersService.getConsumerKyc(secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetConsumersKyc_BackofficeAccessToken_Forbidden(){
        ConsumersService.getConsumerKyc(secretKey, getBackofficeAccessToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}