package opc.junit.multi.consumers;

import opc.enums.opc.IdentityType;
import opc.enums.opc.KycLevel;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.StartKycModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ConsumersService;
import opc.services.multi.PasswordsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class StartConsumersKycTests extends BaseConsumersSetup {

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
    public void StartConsumersKyc_Success(final KycLevel kycLevel){
        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(kycLevel), secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue())
                .body("kycLevel", equalTo(kycLevel.name()));
    }

    @Test
    public void StartConsumersKyc_NoKycLevel_Success(){
        ConsumersService.startConsumerKyc(StartKycModel.builder().build(), secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue())
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @Test
    public void StartConsumersKyc_MissingDateOfBirth_Success() throws SQLException {
        ConsumersDatabaseHelper.setDateOfBirthNull(consumerId);

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue())
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @Test
    public void StartConsumersKyc_MissingAddress_Success() throws SQLException {
        ConsumersDatabaseHelper.deleteConsumerAddress(consumerId);

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue())
                .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @Test
    public void StartConsumersKyc_InvalidApiKey_Unauthorised(){

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), "abc", authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartConsumersKyc_NoApiKey_BadRequest(){

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), "", authenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartConsumersKyc_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartConsumersKyc_RootUserLoggedOut_Unauthorised(){

        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @ParameterizedTest
    @EnumSource(KycLevel.class)
    public void StartConsumersKyc_Approved_KycAlreadyApproved(final KycLevel kycLevel){
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startConsumerKyc(StartKycModel.startKycModel(kycLevel), secretKey, authenticationToken),
                SC_OK)
                .then()
                .body("reference", notNullValue());

        SimulatorService.simulateKycApproval(secretKey, consumerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(kycLevel), secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_ALREADY_APPROVED"));
    }

    @Test
    public void StartConsumersKyc_Pending_KycPendingReview() throws SQLException {
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken),
                SC_OK)
                .then()
                .body("reference", notNullValue());

        ConsumersDatabaseHelper.updateConsumerKyc("PENDING_REVIEW", consumerId);

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_PENDING_REVIEW"));
    }

    @Test
    public void StartConsumersKyc_Rejected_KycRejected() throws SQLException {
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken),
                SC_OK)
                .then()
                .body("reference", notNullValue());

        ConsumersDatabaseHelper.updateConsumerKyc("REJECTED", consumerId);

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_REJECTED"));
    }

    @Test
    public void StartConsumersKyc_BackofficeImpersonator_Forbidden(){
        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartConsumersKyc_SourceOfFundMissing_Success(){
        final CreateConsumerModel createConsumerModel= CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
            .setSourceOfFunds(null)
            .build();

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(KycLevel.KYC_LEVEL_2), secretKey, consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("reference", notNullValue())
            .body("kycLevel", equalTo(KycLevel.KYC_LEVEL_2.name()));
    }

    @ParameterizedTest
    @EnumSource(value = KycLevel.class)
    public void StartConsumersKyc_EmailNotVerified_NotAllowed(final KycLevel kycLevel){
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String consEmailNonVerified = ConsumersHelper.createConsumer(createConsumerModel, secretKey);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        final String token = PasswordsService.createPassword(createPasswordModel, consEmailNonVerified, secretKey)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("token");

        ConsumersService.startConsumerKyc(StartKycModel.startKycModel(kycLevel), secretKey, token)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_UNVERIFIED"));
    }
}
