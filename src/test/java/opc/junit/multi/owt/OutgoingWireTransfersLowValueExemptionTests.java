package opc.junit.multi.owt;

import commons.enums.Currency;
import commons.enums.State;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwtType;
import opc.helpers.OwtModelHelper;
import opc.junit.database.OwtDatabaseHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.Beneficiary;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransferResponseModel;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersResponseModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.VerificationModel;
import opc.services.admin.AdminService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

@Tag(MultiTags.OWT)
public class OutgoingWireTransfersLowValueExemptionTests extends BaseOutgoingWireTransfersSetup {
    private static final String CHANNEL_OTP = EnrolmentChannel.SMS.name();
    private static final String CHANNEL_OKAY_PUSH = EnrolmentChannel.BIOMETRIC.name();
    private static final String CHANNEL_AUTHY = EnrolmentChannel.AUTHY.name();
    private static final String VERIFICATION_CODE = "123456";

    @BeforeAll
    public static void Setup() {

        final String adminToken = AdminService.loginAdmin();
        AdminHelper.resetProgrammeAuthyLimitsCounter(lowValueExemptionAppProgrammeId, adminToken);
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 10000);
        AdminHelper.setProgrammeAuthyChallengeLimit(lowValueExemptionAppProgrammeId, resetCount, adminToken);

        AdminHelper.setConsumerLowValueLimit(adminToken);
        AdminHelper.setCorporateLowValueLimit(adminToken);

        AdminHelper.setConsumerLowValueLimitWithCurrency(lowValueExemptionAppProgrammeId, Currency.EUR, AdminService.loginAdmin());
        AdminHelper.setCorporateLowValueLimitWithCurrency(lowValueExemptionAppProgrammeId, Currency.EUR, AdminService.loginAdmin());
    }

    /**
     * Low Value Exemption Limits for transactions: maxSum = 100, maxCount = 5.
     */

    //Tests for OTP verification
    @Test
    public void OwtLowValueExemption_ConsumerOtpExceedAmountLimit_Success() throws SQLException {
        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 30L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 3);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_ConsumerOtpExceedCountLimit_Success() throws SQLException {
        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_ConsumerOtpExceedCountAndAmountLimits_Success() throws SQLException {
        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateOtpExceedAmountLimit_Success() throws SQLException {
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 25L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 4);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateOtpExceedCountLimit_Success() throws SQLException {
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateOtpExceedCountAndAmountLimits_Success()
            throws SQLException {
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    /**
     * Low Value Exemption Limits for transactions: maxSum = 100, maxCount = 5, maxAmount = 30.
     */

    @Test
    public void OwtLowValueExemption_ConsumerOtpNotExceedMaxAmountLimit_Success() throws SQLException {
        //Set low value limit for the programme wit specific currency and maximum amount
        AdminHelper.setConsumerLowValueLimitWithCurrency(lowValueExemptionAppProgrammeId, Currency.EUR, AdminService.loginAdmin());

        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.CurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId, Currency.EUR).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 32L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 3);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_ConsumerOtpExceedMaxAmountLimit_Success() throws SQLException {
        //Set low value limit for the programme wit specific currency and maximum amount
        AdminHelper.setConsumerLowValueLimitWithCurrency(lowValueExemptionAppProgrammeId, Currency.EUR, AdminService.loginAdmin());

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.CurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId, Currency.EUR).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Mac Value Limits -> PENDING_CHALLENGE state
        final Long destinationAmount = 33L;
        assertOutgoingWireTransferInLowValueLimitsPendingState(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId);
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateOtpNotExceedAmountLimits_Success() throws SQLException {
        
        //Set low value limit for the programme wit specific currency and maximum amount 25
        AdminHelper.setCorporateLowValueLimitWithCurrency(lowValueExemptionAppProgrammeId, Currency.EUR, AdminService.loginAdmin());
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 25L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 4);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateOtpExceedAmountLimits_Success() throws SQLException {
        
        //Set low value limit for the programme wit specific currency and maximum amount 25
        AdminHelper.setCorporateLowValueLimitWithCurrency(lowValueExemptionAppProgrammeId, Currency.EUR, AdminService.loginAdmin());
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 26L;
        assertOutgoingWireTransferInLowValueLimitsPendingState(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OTP verification for the pending transaction
        startOtpVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        OutgoingWireTransfersService.verifyOutgoingWireTransfer(
                        new VerificationModel(VERIFICATION_CODE), pendingOutgoingWireTransferId,
                        CHANNEL_OTP, lowValueExemptionAppSecretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    //Tests for OkayPush verification
    @Test
    public void OwtLowValueExemption_ConsumerOkayPushExceedAmountLimit_Success() throws SQLException {
        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createBiometricEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey, lowValueExemptionAppSharedKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 30L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 3);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OkayPush verification for the pending transaction
        startOkayPushVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        SimulatorService.acceptOkayOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_ConsumerOkayPushExceedCountLimit_Success() throws SQLException {
        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createBiometricEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey, lowValueExemptionAppSharedKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OkayPush verification for the pending transaction
        startOkayPushVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        SimulatorService.acceptOkayOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_ConsumerOkayPushExceedCountAndAmountLimits_Success() throws SQLException {
        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createBiometricEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey, lowValueExemptionAppSharedKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OkayPush verification for the pending transaction
        startOkayPushVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        SimulatorService.acceptOkayOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateOkayPushExceedAmountLimit_Success() throws SQLException {
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createBiometricEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey, lowValueExemptionAppSharedKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 25L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 4);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OkayPush verification for the pending transaction
        startOkayPushVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        SimulatorService.acceptOkayOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateOkayPushExceedCountLimit_Success() throws SQLException {
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createBiometricEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey, lowValueExemptionAppSharedKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OkayPush verification for the pending transaction
        startOkayPushVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        SimulatorService.acceptOkayOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateOkayPushExceedCountAndAmountLimits_Success()
            throws SQLException {
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createBiometricEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey, lowValueExemptionAppSharedKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform OkayPush verification for the pending transaction
        startOkayPushVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        SimulatorService.acceptOkayOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    // Tests for AuthyPush verification
    @Test
    public void OwtLowValueExemption_ConsumerAuthyPushExceedAmountLimit_Success() throws SQLException {
        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createAuthyEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 30L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 3);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform AuthyPush verification for the pending transaction
        startAuthyPushVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        SimulatorService.acceptAuthyOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_ConsumerAuthyPushExceedCountLimit_Success() throws SQLException {
        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createAuthyEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform AuthyPush verification for the pending transaction
        startAuthyPushVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        SimulatorService.acceptAuthyOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_ConsumerAuthyPushExceedCountAndAmountLimits_Success()
            throws SQLException {
        
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createAuthyEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, consumerAuthenticationToken,
                consumerManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(consumerManagedAccountId,
                consumerAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform AuthyPush verification for the pending transaction
        startAuthyPushVerification(pendingOutgoingWireTransferId, consumerAuthenticationToken);
        SimulatorService.acceptAuthyOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, consumerAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateAuthyPushExceedAmountLimit_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createAuthyEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 25L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 4);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform AuthyPush verification for the pending transaction
        startAuthyPushVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        SimulatorService.acceptAuthyOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateAuthyPushExceedCountLimit_Success() throws SQLException {
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createAuthyEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 10L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform AuthyPush verification for the pending transaction
        startAuthyPushVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        SimulatorService.acceptAuthyOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

    @Test
    public void OwtLowValueExemption_CorporateAuthyPushExceedCountAndAmountLimits_Success() throws SQLException {
        
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(lowValueExemptionAppCorporateProfileId).build();

        final String corporateAuthenticationToken =
                CorporatesHelper.createAuthyEnrolledVerifiedCorporate(createCorporateModel, lowValueExemptionAppSecretKey).getRight();

        final String corporateManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppCorporateManagedAccountProfileId,
                corporateAuthenticationToken);

        // Transaction within Low Value Limits -> COMPLETED state
        final Long destinationAmount = 20L;
        assertOutgoingWireTransferInLowValueLimits(destinationAmount, corporateAuthenticationToken,
                corporateManagedAccountId, 5);

        // Transaction exceeding the amount limit -> PENDING state
        final String pendingOutgoingWireTransferId = sendOutgoingWireTransfer(corporateManagedAccountId,
                corporateAuthenticationToken, destinationAmount);
        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.PENDING_CHALLENGE);
        assertExemptionState(pendingOutgoingWireTransferId, "NO_EXEMPTION");

        // Perform AuthyPush verification for the pending transaction
        startAuthyPushVerification(pendingOutgoingWireTransferId, corporateAuthenticationToken);
        SimulatorService.acceptAuthyOwt(lowValueExemptionAppSecretKey, pendingOutgoingWireTransferId)
                .then()
                .statusCode(SC_NO_CONTENT);

        assertOutgoingWireTransferState(pendingOutgoingWireTransferId, corporateAuthenticationToken,
                State.COMPLETED);
    }

  @Test
  public void OwtLowValueExemption_LowValueExemptionOwtBulk_Success()  {

    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

      final String consumerAuthenticationToken =
              ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

    final String consumerManagedAccountId = createFundedManagedAccount(
        lowValueExemptionAppConsumerManagedAccountProfileId,
        consumerAuthenticationToken);

    final OutgoingWireTransfersModel outgoingWireTransfersModelLowValue =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(lowValueExemptionAppOutgoingWireTransfersProfileId,
            consumerManagedAccountId,
            Currency.EUR.name(), 30L, OwtType.SEPA).build();

    final BulkOutgoingWireTransfersModel bulkModel = BulkOutgoingWireTransfersModel.builder()
        .outgoingWireTransfers(
            List.of(outgoingWireTransfersModelLowValue, outgoingWireTransfersModelLowValue))
        .build();

      final List<BulkOutgoingWireTransferResponseModel> bulkTransfer =
              OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(bulkModel, lowValueExemptionAppSecretKey, consumerAuthenticationToken, Optional.empty())
                      .then()
                      .statusCode(SC_OK)
                      .extract()
                      .as(BulkOutgoingWireTransfersResponseModel.class).getResponse();

      bulkTransfer
              .forEach(owt -> OutgoingWireTransfersHelper.ensureOwtState(lowValueExemptionAppSecretKey, owt.getId(), consumerAuthenticationToken, State.COMPLETED));
  }

  @Test
  public void OwtLowValueExemption_BeneficiaryAndLowValueExemptionOwtBulk_Success()  {
    
    final CreateConsumerModel createConsumerModel =
        CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

      final String consumerAuthenticationToken =
              ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

    final String consumerManagedAccountId = createFundedManagedAccount(
        lowValueExemptionAppConsumerManagedAccountProfileId,
        consumerAuthenticationToken);

    final String beneficiaryId = BeneficiariesHelper.createSEPABeneficiaryInState
        (BeneficiaryState.ACTIVE, IdentityType.CONSUMER, "Test", lowValueExemptionAppSecretKey, consumerAuthenticationToken).getRight();

    final OutgoingWireTransfersModel outgoingWireTransfersModelLowValue =
        OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(lowValueExemptionAppOutgoingWireTransfersProfileId,
            consumerManagedAccountId,
            Currency.EUR.name(), 30L, OwtType.SEPA).build();

    final OutgoingWireTransfersModel outgoingWireTransfersModelBeneficiary =
        OutgoingWireTransfersModel.newBuilder()
            .setProfileId(lowValueExemptionAppOutgoingWireTransfersProfileId)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDescription(RandomStringUtils.randomAlphabetic(5))
            .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
            .setSourceInstrument(new ManagedInstrumentTypeId(consumerManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
            .setTransferAmount(new CurrencyAmount(Currency.EUR.name(), 1000L))
            .build();

    final BulkOutgoingWireTransfersModel bulkModel = BulkOutgoingWireTransfersModel.builder()
        .outgoingWireTransfers(
            List.of(outgoingWireTransfersModelLowValue, outgoingWireTransfersModelBeneficiary))
        .build();

      final List<BulkOutgoingWireTransferResponseModel> bulkTransfer =
              OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(bulkModel, lowValueExemptionAppSecretKey, consumerAuthenticationToken, Optional.empty())
                      .then()
                      .statusCode(SC_OK)
                      .extract()
                      .as(BulkOutgoingWireTransfersResponseModel.class).getResponse();

      bulkTransfer
              .forEach(owt -> OutgoingWireTransfersHelper.ensureOwtState(lowValueExemptionAppSecretKey, owt.getId(), consumerAuthenticationToken, State.COMPLETED));
  }

    @Test
    public void OwtLowValueExemption_LowValueExemptionMultipleOwtBulkExceededAmount_SuccessPendingChallenge()  {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(lowValueExemptionAppConsumerManagedAccountProfileId, consumerAuthenticationToken);

        final List<BulkOutgoingWireTransferResponseModel> bulkTransfer =
                OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(OwtModelHelper
                                .createOwtBulkPayments(5, lowValueExemptionAppOutgoingWireTransfersProfileId, consumerManagedAccountId,
                                        Currency.EUR.name(), 25L, OwtType.SEPA), lowValueExemptionAppSecretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .as(BulkOutgoingWireTransfersResponseModel.class).getResponse();

        bulkTransfer
                .forEach(owt -> OutgoingWireTransfersHelper.ensureOwtState(lowValueExemptionAppSecretKey, owt.getId(), consumerAuthenticationToken, State.PENDING_CHALLENGE));
    }

    @Test
    public void OwtLowValueExemption_BeneficiaryAndLowValueExemptionMultipleSingleOwtBulk_Success()  {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(
                lowValueExemptionAppConsumerManagedAccountProfileId,
                consumerAuthenticationToken);

        final Map<String, State> owtIdStateMap = new HashMap<>();

        IntStream.range(0, 4).forEach(i -> {

            final String owtId =
                    OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(OwtModelHelper
                                    .createOwtBulkPayments(1, lowValueExemptionAppOutgoingWireTransfersProfileId, consumerManagedAccountId,
                                            Currency.EUR.name(), 30L, OwtType.SEPA), lowValueExemptionAppSecretKey, consumerAuthenticationToken, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .as(BulkOutgoingWireTransfersResponseModel.class).getResponse().stream().findFirst().orElseThrow().getId();

            owtIdStateMap.put(owtId, i < 3 ? State.COMPLETED : State.PENDING_CHALLENGE);
        });

        owtIdStateMap
                .forEach((key, value) ->
                        OutgoingWireTransfersHelper.ensureOwtState(lowValueExemptionAppSecretKey, key, consumerAuthenticationToken, value));
    }

    @Test
    public void OwtLowValueExemption_LowValueExemptionMultipleOwtBulkExceededAmount_LowValueSuccessfulAfterPendingChallenge()  {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(lowValueExemptionAppConsumerProfileId).build();

        final String consumerAuthenticationToken =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, lowValueExemptionAppSecretKey).getRight();

        final String consumerManagedAccountId = createFundedManagedAccount(lowValueExemptionAppConsumerManagedAccountProfileId, consumerAuthenticationToken);

        final List<BulkOutgoingWireTransferResponseModel> bulkTransfer =
                OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(OwtModelHelper
                                .createOwtBulkPayments(5, lowValueExemptionAppOutgoingWireTransfersProfileId, consumerManagedAccountId,
                                        Currency.EUR.name(), 25L, OwtType.SEPA), lowValueExemptionAppSecretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .as(BulkOutgoingWireTransfersResponseModel.class).getResponse();

        bulkTransfer
                .forEach(owt ->
                        OutgoingWireTransfersHelper.ensureOwtState(lowValueExemptionAppSecretKey, owt.getId(), consumerAuthenticationToken, State.PENDING_CHALLENGE));

        final List<BulkOutgoingWireTransferResponseModel> bulkTransferLowValueExemption =
                OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(OwtModelHelper
                                .createOwtBulkPayments(4, lowValueExemptionAppOutgoingWireTransfersProfileId, consumerManagedAccountId,
                                        Currency.EUR.name(), 25L, OwtType.SEPA), lowValueExemptionAppSecretKey, consumerAuthenticationToken, Optional.empty())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .as(BulkOutgoingWireTransfersResponseModel.class).getResponse();

        bulkTransferLowValueExemption
                .forEach(owt ->
                        OutgoingWireTransfersHelper.ensureOwtState(lowValueExemptionAppSecretKey, owt.getId(), consumerAuthenticationToken, State.COMPLETED));
    }

    private static String createFundedManagedAccount(final String profile,
                                                     final String token) {
        final String managedAccountId =
                createManagedAccount(profile, Currency.EUR.name(), token, lowValueExemptionAppSecretKey)
                        .getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

        return managedAccountId;
    }

    private static void assertOutgoingWireTransferInLowValueLimits(final Long destinationAmount,
                                                                   final String token,
                                                                   final String identityManagedAccountId,
                                                                   final int expectedCount) throws SQLException {
        int count = 0;
        long leftSum = 100L;
        while (count < 5) {
            final String owtId = sendOutgoingWireTransfer(identityManagedAccountId, token,
                    destinationAmount);
            assertOutgoingWireTransferState(owtId, token, State.COMPLETED);
            assertExemptionState(owtId, "LOW_VALUE");

            count++;
            leftSum = leftSum - destinationAmount;
            if (leftSum < destinationAmount) {
                break;
            }
        }
        Assertions.assertEquals(expectedCount, count);
    }

    private static void assertOutgoingWireTransferInLowValueLimitsPendingState(final Long destinationAmount,
                                                                               final String token,
                                                                               final String identityManagedAccountId) throws SQLException {

        final String owtId = sendOutgoingWireTransfer(identityManagedAccountId, token, destinationAmount);
        assertOutgoingWireTransferState(owtId, token, State.PENDING_CHALLENGE);
        assertExemptionState(owtId, "NO_EXEMPTION");
    }

    private static void assertExemptionState(final String owtId,
                                             final String expectedState) throws SQLException {
        final String exemptionState = OwtDatabaseHelper.getOwtById(owtId).get(0).get("exemption");
        Assertions.assertEquals(expectedState, exemptionState);
    }

    private static String sendOutgoingWireTransfer(final String managedAccountId,
                                                   final String token,
                                                   final Long amount) {

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(lowValueExemptionAppOutgoingWireTransfersProfileId,
                        managedAccountId,
                        Currency.EUR.name(), amount, OwtType.SEPA).build();

        return OutgoingWireTransfersService
                .sendOutgoingWireTransfer(outgoingWireTransfersModel, lowValueExemptionAppSecretKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id");
    }

    private static void assertOutgoingWireTransferState(final String id, final String token, final State state) {

        OutgoingWireTransfersHelper.ensureOwtState(lowValueExemptionAppSecretKey, id, token, state);
    }

    private void startOtpVerification(final String id, final String token) {

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL_OTP,
                        lowValueExemptionAppSecretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private void startOkayPushVerification(final String id,
                                           final String token) {

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL_OKAY_PUSH,
                        lowValueExemptionAppSecretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private void startAuthyPushVerification(final String id,
                                            final String token) {

        OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, CHANNEL_AUTHY,
                        lowValueExemptionAppSecretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
