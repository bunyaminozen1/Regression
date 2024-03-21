package opc.junit.multi.consumers;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.IdentityType;
import opc.enums.opc.KycLevel;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class StartConsumersKycMobileTests extends BaseConsumersSetup {

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

    @ParameterizedTest
    @EnumSource(value = KycLevel.class)
    public void StartKycSumsub_Success(final KycLevel kycLevel) {

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
    }

    @Test
    public void StartKycSumsub_NoKycLevel_Success() {

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
    }

    @Test
    public void StartKycSumsub_MissingDateOfBirth_Success() throws SQLException {
        ConsumersDatabaseHelper.setDateOfBirthNull(consumerId);

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
    }

    @Test
    public void StartKycSumsub_MissingAddress_Success() throws SQLException {
        ConsumersDatabaseHelper.deleteConsumerAddress(consumerId);

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
    }

    @Test
    public void StartKycSumsub_InvalidApiKey_Unauthorised(){

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), "abc", authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartKycSumsub_NoApiKey_BadRequest(){

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), "", authenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartKycSumsub_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartKycSumsub_RootUserLoggedOut_Unauthorised(){

        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartKycSumsub_Approved_KycAlreadyApproved(){
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken),
                SC_OK)
                .then()
                .body("verificationFlow", equalTo("consumer-kyc-flow-mobile"))
                .body("accessToken", notNullValue())
                .body("identityType", equalTo("consumers"))
                .body("externalUserId", equalTo(String.format("cons%s", consumerId)))
                .body("kycProviderKey", equalTo("sumsub"));

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_ALREADY_APPROVED"));
    }

    @Test
    public void StartKycSumsub_Pending_KycPendingReview() throws SQLException {
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken),
                SC_OK)
                .then()
                .body("verificationFlow", equalTo("consumer-kyc-flow-mobile"))
                .body("accessToken", notNullValue())
                .body("identityType", equalTo("consumers"))
                .body("externalUserId", equalTo(String.format("cons%s", consumerId)))
                .body("kycProviderKey", equalTo("sumsub"));

        ConsumersDatabaseHelper.updateConsumerKyc("PENDING_REVIEW", consumerId);

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_PENDING_REVIEW"));
    }

    @Test
    public void StartKycSumsub_Rejected_KycAlreadyRejected() throws SQLException {
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken),
                SC_OK)
                .then()
                .body("verificationFlow", equalTo("consumer-kyc-flow-mobile"))
                .body("accessToken", notNullValue())
                .body("identityType", equalTo("consumers"))
                .body("externalUserId", equalTo(String.format("cons%s", consumerId)))
                .body("kycProviderKey", equalTo("sumsub"));

        ConsumersDatabaseHelper.updateConsumerKyc("REJECTED", consumerId);

        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_ALREADY_REJECTED"));
    }

    @Test
    public void StartKycSumsub_BackofficeImpersonator_Forbidden(){
        ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartKycSumsub_RegenerateTokenSameLevel_Success() {

        final String accessToken =
                ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_1), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK).extract().jsonPath().getString("accessToken");

        final String accessToken1 =
                ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_1), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK).extract().jsonPath().getString("accessToken");

        Assertions.assertNotEquals(accessToken, accessToken1);
    }

    @Test
    public void StartKycSumsub_RegenerateTokenDifferentLevel_Success() {

        final String accessToken =
                ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_1), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK).extract().jsonPath().getString("accessToken");

        final String accessToken1 =
                ConsumersService.startKycMobile(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                        .then()
                        .statusCode(SC_OK).extract().jsonPath().getString("accessToken");

        Assertions.assertNotEquals(accessToken, accessToken1);
    }
}
