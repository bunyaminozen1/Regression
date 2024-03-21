package opc.junit.helpers.multi;

import opc.enums.opc.BeneficiariesBatchState;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.helpers.ModelHelper;
import opc.junit.database.BeneficiaryDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.beneficiaries.BeneficiaryResponseModel;
import opc.models.multi.beneficiaries.CreateBeneficiariesBatchModel;
import opc.models.multi.beneficiaries.RemoveBeneficiariesModel;
import opc.models.shared.VerificationModel;
import opc.services.multi.BeneficiariesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.platform.commons.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class BeneficiariesHelper {

    protected static final String VERIFICATION_CODE = "123456";

    public static void verifyBeneficiariesShareSize(final String corporateId, final String externalId) {

        TestHelper.ensureDatabaseResultAsExpected(45,
                () -> BeneficiaryDatabaseHelper.findByCorporateId(corporateId),
                x -> x.size() > 0 &&
                x.values().stream()
                        .filter(stringStringMap -> stringStringMap.containsKey(BeneficiaryDatabaseHelper.provider_reference))
                        .filter(stringStringMap -> stringStringMap.get(BeneficiaryDatabaseHelper.provider_reference).equals(externalId))
                        .findFirst()
                        .filter(map -> map.get("share_size") != null).isPresent(),
                Optional.of("Beneficiary share size not found"));
    }

    public static void verifyShareholder(String corporateId, String providerReference) {

            try{
                final Map<Integer, Map<String, String>> result = BeneficiaryDatabaseHelper.findByCorporateId(corporateId);
                if (result.size() > 0){
                    Map<String, String> record = result.values().stream()
                            .filter(stringStringMap -> stringStringMap.containsKey(BeneficiaryDatabaseHelper.provider_reference))
                            .filter(stringStringMap -> stringStringMap.get(BeneficiaryDatabaseHelper.provider_reference).equals(providerReference))
                            .findFirst()
                            .get();
                    Assert.assertTrue(StringUtils.isNotBlank(record.get("legal_address")));
                    Assert.assertTrue(StringUtils.isNotBlank(record.get("company_name")));
                    Assert.assertTrue(StringUtils.isNotBlank(record.get("registration_number")));
                    Assert.assertTrue(StringUtils.isNotBlank(record.get("country")));
                }
            } catch (Exception e){
                throw new RuntimeException(e);
            }
    }

    public static void ensureBeneficiaryBatchState(final BeneficiariesBatchState batchState,
                                                   final String batchId,
                                                   final String secretKey,
                                                   final String token) {

        TestHelper.ensureAsExpected(30,
                () -> BeneficiariesService.getBeneficiaryBatch(batchId, secretKey, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(batchState.name()),
                Optional.of(String.format("Expecting 200 with batch in state %s, check logged payloads", batchState.name())));
    }

    public static void ensureBeneficiaryState(final BeneficiaryState beneficiaryState,
                                              final String beneficiaryId,
                                              final String secretKey,
                                              final String token) {

        TestHelper.ensureAsExpected(30,
                () -> BeneficiariesService.getBeneficiary(beneficiaryId, secretKey, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals(beneficiaryState.name()),
                Optional.of(String.format("Expecting 200 with beneficiary in state %s, check logged payloads", beneficiaryState.name())));
    }

    public static void startBeneficiaryBatchPushVerification(final String batchId,
                                                             final String channel,
                                                             final String secretKey,
                                                             final String token) {
        TestHelper.ensureAsExpected(15,
            () -> BeneficiariesService.startBeneficiaryBatchPushVerification(batchId, channel, secretKey, token),
            SC_NO_CONTENT);
    }
    public static void startBeneficiaryBatchOtpVerification(final String batchId,
                                                             final String channel,
                                                             final String secretKey,
                                                             final String token) {
        TestHelper.ensureAsExpected(15,
            () -> BeneficiariesService.startBeneficiaryBatchOtpVerification(batchId, channel, secretKey, token),
            SC_NO_CONTENT);
    }

    public static void verifyBeneficiaryBatchOtp(final VerificationModel verificationModel,
                                                 final String batchId,
                                                 final String channel,
                                                 final String secretKey,
                                                 final String token) {
        TestHelper.ensureAsExpected(15,
            () -> BeneficiariesService.verifyBeneficiaryBatchOtp(verificationModel, batchId, channel, secretKey, token),
            SC_NO_CONTENT);
    }

    public static void startAndVerifyBeneficiaryBatchOtp(final VerificationModel verificationModel,
                                                         final String batchId,
                                                         final String channel,
                                                         final String secretKey,
                                                         final String token) {
        startBeneficiaryBatchOtpVerification(batchId, channel, secretKey, token);
        verifyBeneficiaryBatchOtp(verificationModel, batchId, channel, secretKey, token);
    }

    public static String getBeneficiaryIdByInstrumentIdAndBatchId(final String instrumentId,
                                                                  final String batchId,
                                                                  final String secretKey,
                                                                  final String token) {

//        final List<String> beneficiaryIds = BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), token)
//            .path("beneficiaries.findAll { beneficiary -> beneficiary.beneficiaryDetails.instrument.id == '" + instrumentId +
//                "' && beneficiary.relatedOperationBatches.findAll { batch -> batch.batchId == '" + batchId + "'}}.id");
//
//        if (beneficiaryIds.isEmpty()) {
//            throw new RuntimeException("No beneficiary found for instrument id " + instrumentId + " and batch id " + batchId);
//        }
//
//        return beneficiaryIds.get(0);

        List<String> beneficiaryIds = null;
        final long startTime = System.currentTimeMillis();
        final long timeoutMillis = 15000;

        do {
            beneficiaryIds = BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), token)
                .path("beneficiaries.findAll { beneficiary -> beneficiary.beneficiaryDetails.instrument.id == '" + instrumentId +
                    "' && beneficiary.relatedOperationBatches.findAll { batch -> batch.batchId == '" + batchId + "'}}.id");

            if (beneficiaryIds != null && !beneficiaryIds.isEmpty()) {
                return beneficiaryIds.get(0);
            }

        } while (System.currentTimeMillis() - startTime < timeoutMillis);

        throw new RuntimeException("No beneficiary found for instrument id " + instrumentId + " and batch id " + batchId + " within " + timeoutMillis + " milliseconds.");
    }

    public static List<String> getBeneficiariesIdsByBatchId(final String batchId,
                                                            final String secretKey,
                                                            final String token) {

//        final List<String> beneficiaryIds = BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), token)
//            .path("beneficiaries.findAll { beneficiary -> beneficiary.relatedOperationBatches.findAll { batch -> batch.batchId == '" + batchId + "'}}.id");
//
//        if (beneficiaryIds.isEmpty()) {
//            throw new RuntimeException("No beneficiary found for batch id " + batchId);
//        }
//
//        return beneficiaryIds;

        List<String> beneficiaryIds = null;
        final long startTime = System.currentTimeMillis();
        final long timeoutMillis = 15000;

        do {
            beneficiaryIds = BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), token)
           .path("beneficiaries.findAll { beneficiary -> beneficiary.relatedOperationBatches.findAll { batch -> batch.batchId == '" + batchId + "'}}.id");

            if (beneficiaryIds != null && !beneficiaryIds.isEmpty()) {
                return beneficiaryIds;
            }

        } while (System.currentTimeMillis() - startTime < timeoutMillis);

        throw new RuntimeException("No beneficiary found for batch id " + batchId + " within " + timeoutMillis + " milliseconds.");
    }

    public static String getBeneficiaryIdByIbanAndBatchId(final String iban,
                                                          final String batchId,
                                                          final String secretKey,
                                                          final String token) {

//        final List<String> beneficiaryIds = BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), token)
//            .path("beneficiaries.findAll { beneficiary -> beneficiary.beneficiaryDetails.bankAccountDetails.iban == '" + iban +
//                "' && beneficiary.relatedOperationBatches.findAll { batch -> batch.batchId == '" + batchId + "'}}.id");
//
//        if (beneficiaryIds.isEmpty()) {
//            throw new RuntimeException("No beneficiary found for iban " + iban + " and batch id " + batchId);
//        }
//
//        return beneficiaryIds.get(0);

        List<String> beneficiaryIds = null;
        final long startTime = System.currentTimeMillis();
        final long timeoutMillis = 15000;

        do {
            beneficiaryIds = BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), token)
            .path("beneficiaries.findAll { beneficiary -> beneficiary.beneficiaryDetails.bankAccountDetails.iban == '" + iban +
                "' && beneficiary.relatedOperationBatches.findAll { batch -> batch.batchId == '" + batchId + "'}}.id");

            if (beneficiaryIds != null && !beneficiaryIds.isEmpty()) {
                return beneficiaryIds.get(0);
            }

        } while (System.currentTimeMillis() - startTime < timeoutMillis);

        throw new RuntimeException("No beneficiary found for iban " + iban + " and batch id " + batchId + " within " + timeoutMillis + " milliseconds.");
    }

    public static String getBeneficiaryIdByAccountNumberAndBatchId(final String accountNumber,
                                                                   final String batchId,
                                                                   final String secretKey,
                                                                   final String token) {

//        final List<String> beneficiaryIds = BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), token)
//            .path("beneficiaries.findAll { beneficiary -> beneficiary.beneficiaryDetails.bankAccountDetails.accountNumber == '" + accountNumber +
//                "' && beneficiary.relatedOperationBatches.findAll { batch -> batch.batchId == '" + batchId + "'}}.id");
//
//        if (beneficiaryIds.isEmpty()) {
//            throw new RuntimeException("No beneficiary found for accountNumber " + accountNumber + " and batch id " + batchId);
//        }
//
//        return beneficiaryIds.get(0);

        List<String> beneficiaryIds = null;
        final long startTime = System.currentTimeMillis();
        final long timeoutMillis = 15000;

        do {
            beneficiaryIds = BeneficiariesService.getBeneficiaries(secretKey, Optional.empty(), token)
            .path("beneficiaries.findAll { beneficiary -> beneficiary.beneficiaryDetails.bankAccountDetails.accountNumber == '" + accountNumber +
                "' && beneficiary.relatedOperationBatches.findAll { batch -> batch.batchId == '" + batchId + "'}}.id");

            if (beneficiaryIds != null && !beneficiaryIds.isEmpty()) {
                return beneficiaryIds.get(0);
            }

        } while (System.currentTimeMillis() - startTime < timeoutMillis);

        throw new RuntimeException("No beneficiary found for accountNumber " + accountNumber + " and batch id " + batchId + " within " + timeoutMillis + " milliseconds.");
    }

    public static String getBeneficiaryBatchId(final CreateBeneficiariesBatchModel createBeneficiariesBatchModel,
                                               final String secretKey,
                                               final String token) {
        return BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, token)
            .jsonPath().get("operationBatchId.batchId");
    }

    public static Pair<String, String> createInstrumentBeneficiaryInState(final BeneficiaryState expectedState,
                                                                          final IdentityType identityType,
                                                                          final ManagedInstrumentType instrumentType,
                                                                          final String identityName,
                                                                          final String instrumentId,
                                                                          final String secretKey,
                                                                          final String token) {

        switch (expectedState) {
            case PENDING_CHALLENGE:
                return createInstrumentBeneficiaryInPendingChallengeState(instrumentType, identityType,
                    identityName, instrumentId, secretKey, token);
            case INVALID:
                return createInstrumentBeneficiaryInInvalidState(instrumentType, identityType,
                    instrumentId, secretKey, token);
            case CHALLENGE_FAILED:
                return createInstrumentBeneficiaryInChallengeFailedState(instrumentType, identityType,
                    identityName, instrumentId, secretKey, token);
            case ACTIVE:
                return createInstrumentBeneficiaryInActiveState(instrumentType, identityType,
                    identityName, instrumentId, secretKey, token);
            case REMOVED:
                return createInstrumentBeneficiaryInRemovedState(instrumentType, identityType,
                    identityName, instrumentId, secretKey, token);
            default:
                throw new IllegalStateException("Unexpected value: " + expectedState);
        }
    }

    public static Pair<String, String> createSEPABeneficiaryInState(final BeneficiaryState expectedState,
                                                                    final IdentityType identityType,
                                                                    final String identityName,
                                                                    final String secretKey,
                                                                    final String token) {

        switch (expectedState) {
            case PENDING_CHALLENGE:
                return createSepaBeneficiaryInPendingChallengeState(identityType, identityName, secretKey, token);
            case CHALLENGE_FAILED:
                return createSepaBeneficiaryInChallengeFailedState(identityType, identityName, secretKey, token);
            case ACTIVE:
                return createSepaBeneficiaryInActiveState(identityType, identityName, secretKey, token);
            case REMOVED:
                return createSepaBeneficiaryInRemovedState(identityType, identityName, secretKey, token);
            default:
                throw new IllegalStateException("Unexpected value: " + expectedState);
        }
    }

    public static Pair<String, String> createFasterPaymentsBeneficiaryInState(final BeneficiaryState expectedState,
                                                                              final IdentityType identityType,
                                                                              final String identityName,
                                                                              final String secretKey,
                                                                              final String token) {

        switch (expectedState) {
            case PENDING_CHALLENGE:
                return createFasterPaymentsBeneficiaryInPendingChallengeState(identityType, identityName, secretKey, token);
            case CHALLENGE_FAILED:
                return createFasterPaymentsBeneficiaryInChallengeFailedState(identityType, identityName, secretKey, token);
            case ACTIVE:
                return createFasterPaymentsBeneficiaryInActiveState(identityType, identityName, secretKey, token);
            case REMOVED:
                return createFasterPaymentsBeneficiaryInRemovedState(identityType, identityName, secretKey, token);
            default:
                throw new IllegalStateException("Unexpected value: " + expectedState);
        }
    }

    private static Pair<String, String> createInstrumentBeneficiaryInPendingChallengeState(final ManagedInstrumentType instrumentType,
                                                                                           final IdentityType identityType,
                                                                                           final String identityName,
                                                                                           final String instrumentId,
                                                                                           final String secretKey,
                                                                                           final String token) {
        final CreateBeneficiariesBatchModel createBeneficiaryBatchModel =
            CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(identityType,
                instrumentType, identityName, List.of(instrumentId)).build();

        final String batchId = BeneficiariesHelper.getBeneficiaryBatchId(createBeneficiaryBatchModel, secretKey, token);
        final String beneficiaryId = BeneficiariesHelper.getBeneficiaryIdByInstrumentIdAndBatchId(instrumentId, batchId, secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE, batchId, secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.PENDING_CHALLENGE, beneficiaryId, secretKey, token);

        return Pair.of(batchId, beneficiaryId);
    }

    private static Pair<String, String> createSepaBeneficiaryInPendingChallengeState(final IdentityType identityType,
                                                                                     final String identityName,
                                                                                     final String secretKey,
                                                                                     final String token) {
        final Pair<String, String> IbanAndBankIdentifierNumber = ModelHelper.generateRandomValidSEPABankDetails();
        final CreateBeneficiariesBatchModel createBeneficiaryBatchModel =
            CreateBeneficiariesBatchModel.SEPABeneficiaryBatch(identityType, identityName,
                List.of(IbanAndBankIdentifierNumber)).build();

        final String batchId = BeneficiariesHelper.getBeneficiaryBatchId(createBeneficiaryBatchModel, secretKey, token);
        final String beneficiaryId = BeneficiariesHelper.getBeneficiaryIdByIbanAndBatchId(IbanAndBankIdentifierNumber.getLeft(), batchId, secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE, batchId, secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.PENDING_CHALLENGE, beneficiaryId, secretKey, token);

        return Pair.of(batchId, beneficiaryId);
    }

    private static Pair<String, String> createFasterPaymentsBeneficiaryInPendingChallengeState(final IdentityType identityType,
                                                                                               final String identityName,
                                                                                               final String secretKey,
                                                                                               final String token) {
        final Pair<String, String> AccountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreateBeneficiariesBatchModel createBeneficiaryBatchModel =
            CreateBeneficiariesBatchModel.FasterPaymentsBeneficiaryBatch(identityType, identityName,
                List.of(AccountNumberAndSortCode)).build();

        final String batchId = BeneficiariesHelper.getBeneficiaryBatchId(createBeneficiaryBatchModel, secretKey, token);
        final String beneficiaryId = BeneficiariesHelper.getBeneficiaryIdByAccountNumberAndBatchId(AccountNumberAndSortCode.getLeft(), batchId, secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE, batchId, secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.PENDING_CHALLENGE, beneficiaryId, secretKey, token);

        return Pair.of(batchId, beneficiaryId);
    }

    private static Pair<String, String> createInstrumentBeneficiaryInInvalidState(final ManagedInstrumentType instrumentType,
                                                                                  final IdentityType identityType,
                                                                                  final String instrumentId,
                                                                                  final String secretKey,
                                                                                  final String token) {
        final CreateBeneficiariesBatchModel createBeneficiaryBatchModel =
            CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(identityType,
                instrumentType, RandomStringUtils.randomAlphabetic(8), List.of(instrumentId)).build();

        final String batchId = BeneficiariesHelper.getBeneficiaryBatchId(createBeneficiaryBatchModel, secretKey, token);
        final String beneficiaryId = BeneficiariesHelper.getBeneficiaryIdByInstrumentIdAndBatchId(instrumentId, batchId, secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.FAILED, batchId, secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.INVALID, beneficiaryId, secretKey, token);

        return Pair.of(batchId, beneficiaryId);
    }

    private static Pair<String, String> createInstrumentBeneficiaryInActiveState(final ManagedInstrumentType instrumentType,
                                                                                 final IdentityType identityType,
                                                                                 final String identityName,
                                                                                 final String instrumentId,
                                                                                 final String secretKey,
                                                                                 final String token) {
        final Pair<String, String> batchAndBeneficiary = createInstrumentBeneficiaryInPendingChallengeState
            (instrumentType, identityType, identityName, instrumentId, secretKey, token);

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            batchAndBeneficiary.getLeft(), EnrolmentChannel.SMS.name(), secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, batchAndBeneficiary.getLeft(), secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, batchAndBeneficiary.getRight(), secretKey, token);

        return batchAndBeneficiary;
    }

    private static Pair<String, String> createSepaBeneficiaryInActiveState(final IdentityType identityType,
                                                                           final String identityName,
                                                                           final String secretKey,
                                                                           final String token) {
        final Pair<String, String> batchAndBeneficiary =
            createSepaBeneficiaryInPendingChallengeState(identityType, identityName, secretKey, token);

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            batchAndBeneficiary.getLeft(), EnrolmentChannel.SMS.name(), secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, batchAndBeneficiary.getLeft(), secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, batchAndBeneficiary.getRight(), secretKey, token);

        return batchAndBeneficiary;
    }

    private static Pair<String, String> createFasterPaymentsBeneficiaryInActiveState(final IdentityType identityType,
                                                                                     final String identityName,
                                                                                     final String secretKey,
                                                                                     final String token) {
        final Pair<String, String> batchAndBeneficiary =
            createFasterPaymentsBeneficiaryInPendingChallengeState(identityType, identityName, secretKey, token);

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            batchAndBeneficiary.getLeft(), EnrolmentChannel.SMS.name(), secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, batchAndBeneficiary.getLeft(), secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, batchAndBeneficiary.getRight(), secretKey, token);

        return batchAndBeneficiary;
    }

    private static Pair<String, String> createInstrumentBeneficiaryInRemovedState(final ManagedInstrumentType instrumentType,
                                                                                  final IdentityType identityType,
                                                                                  final String identityName,
                                                                                  final String instrumentId,
                                                                                  final String secretKey,
                                                                                  final String token) {
        final String beneficiary = createInstrumentBeneficiaryInActiveState
            (instrumentType, identityType, identityName, instrumentId, secretKey, token).getRight();

        final String removeBatchId = BeneficiariesService.removeBeneficiaries(
                RemoveBeneficiariesModel.Remove(List.of(beneficiary)).build(), secretKey, token)
            .jsonPath().get("operationBatchId.batchId");

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            removeBatchId, EnrolmentChannel.SMS.name(), secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, removeBatchId, secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.REMOVED, beneficiary, secretKey, token);

        return Pair.of(removeBatchId, beneficiary);
    }

    private static Pair<String, String> createSepaBeneficiaryInRemovedState(final IdentityType identityType,
                                                                            final String identityName,
                                                                            final String secretKey,
                                                                            final String token) {
        final String beneficiary = createSepaBeneficiaryInActiveState
            (identityType, identityName, secretKey, token).getRight();

        final String removeBatchId = BeneficiariesService.removeBeneficiaries(
                RemoveBeneficiariesModel.Remove(List.of(beneficiary)).build(), secretKey, token)
            .jsonPath().get("operationBatchId.batchId");

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            removeBatchId, EnrolmentChannel.SMS.name(), secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, removeBatchId, secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.REMOVED, beneficiary, secretKey, token);

        return Pair.of(removeBatchId, beneficiary);
    }

    private static Pair<String, String> createFasterPaymentsBeneficiaryInRemovedState(final IdentityType identityType,
                                                                                      final String identityName,
                                                                                      final String secretKey,
                                                                                      final String token) {
        final String beneficiary = createFasterPaymentsBeneficiaryInActiveState
            (identityType, identityName, secretKey, token).getRight();

        final String removeBatchId = BeneficiariesService.removeBeneficiaries(
                RemoveBeneficiariesModel.Remove(List.of(beneficiary)).build(), secretKey, token)
            .jsonPath().get("operationBatchId.batchId");

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
            removeBatchId, EnrolmentChannel.SMS.name(), secretKey, token);

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, removeBatchId, secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.REMOVED, beneficiary, secretKey, token);

        return Pair.of(removeBatchId, beneficiary);
    }

    // Requires Identity to be enrolled for Authy
    private static Pair<String, String> createInstrumentBeneficiaryInChallengeFailedState(final ManagedInstrumentType instrumentType,
                                                                                          final IdentityType identityType,
                                                                                          final String identityName,
                                                                                          final String instrumentId,
                                                                                          final String secretKey,
                                                                                          final String token) {
        final Pair<String, String> batchAndBeneficiary = createInstrumentBeneficiaryInPendingChallengeState(instrumentType,
            identityType, identityName, instrumentId, secretKey, token);

        BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchAndBeneficiary.getLeft(), EnrolmentChannel.AUTHY.name(), secretKey, token);
        SimulatorHelper.rejectAuthyBeneficiaryBatch(secretKey, batchAndBeneficiary.getLeft());

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.CHALLENGE_FAILED, batchAndBeneficiary.getLeft(), secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.CHALLENGE_FAILED, batchAndBeneficiary.getRight(), secretKey, token);

        return batchAndBeneficiary;
    }

    // Requires Identity to be enrolled for Authy
    private static Pair<String, String> createSepaBeneficiaryInChallengeFailedState(final IdentityType identityType,
                                                                                    final String identityName,
                                                                                    final String secretKey,
                                                                                    final String token) {
        final Pair<String, String> batchAndBeneficiary =
            createSepaBeneficiaryInPendingChallengeState(identityType, identityName, secretKey, token);

        BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchAndBeneficiary.getLeft(), EnrolmentChannel.AUTHY.name(), secretKey, token);
        SimulatorHelper.rejectAuthyBeneficiaryBatch(secretKey, batchAndBeneficiary.getLeft());

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.CHALLENGE_FAILED, batchAndBeneficiary.getLeft(), secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.CHALLENGE_FAILED, batchAndBeneficiary.getRight(), secretKey, token);

        return batchAndBeneficiary;
    }

    // Requires Identity to be enrolled for Authy
    private static Pair<String, String> createFasterPaymentsBeneficiaryInChallengeFailedState(final IdentityType identityType,
                                                                                              final String identityName,
                                                                                              final String secretKey,
                                                                                              final String token) {
        final Pair<String, String> batchAndBeneficiary =
            createFasterPaymentsBeneficiaryInPendingChallengeState(identityType, identityName, secretKey, token);

        BeneficiariesHelper.startBeneficiaryBatchPushVerification(batchAndBeneficiary.getLeft(), EnrolmentChannel.AUTHY.name(), secretKey, token);
        SimulatorHelper.rejectAuthyBeneficiaryBatch(secretKey, batchAndBeneficiary.getLeft());

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.CHALLENGE_FAILED, batchAndBeneficiary.getLeft(), secretKey, token);
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.CHALLENGE_FAILED, batchAndBeneficiary.getRight(), secretKey, token);

        return batchAndBeneficiary;
    }

    public static BeneficiaryResponseModel getBeneficiary(final String beneficiaryId,
                                                       final String secretKey,
                                                       final String token) {

        return TestHelper.ensureAsExpected(15,
                () -> BeneficiariesService.getBeneficiary(beneficiaryId, secretKey, token),
                SC_OK)
                .as(BeneficiaryResponseModel.class);
    }
}