package opc.junit.multi.beneficiaries;

import io.restassured.response.Response;
import opc.enums.opc.BeneficiariesBatchState;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import opc.helpers.ModelHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.models.multi.beneficiaries.CreateBeneficiariesBatchModel;
import opc.models.multi.beneficiaries.RemoveBeneficiariesModel;
import opc.models.shared.VerificationModel;
import opc.services.multi.BeneficiariesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public abstract class AbstractRemoveBeneficiariesTests extends BaseBeneficiariesSetup {
    protected abstract String getToken();

    protected abstract String getIdentityId();

    protected abstract String getDestinationToken();

    protected abstract String getDestinationCurrency();

    protected abstract String getDestinationIdentityName();

    protected abstract String getDestinationPrepaidManagedCardProfileId();

    protected abstract String getDestinationDebitManagedCardProfileId();

    protected abstract String getDestinationManagedAccountProfileId();

    protected abstract IdentityType getIdentityType();

    protected static final String VERIFICATION_CODE = "123456";

    /**
     * Test cases for Removing Trusted Beneficiaries
     * Documentation: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2215510082/Trusted+Beneficiaries+for+Sends+and+OWTs
     * Test Plan: TBA
     * Main ticket: https://weavr-payments.atlassian.net/browse/ROADMAP-507
     * <p>
     * The main cases:
     * 1. remove beneficiaries for Instruments (Managed Accounts, Managed Cards)
     * 2. remove beneficiaries for Bank Details (SEPA and Faster Payments)
     * 3. removing multiple beneficiaries (Valid and Invalid)
     * 4. failing to remove beneficiaries based on predefined logic
     */

    @Test
    public void RemoveBeneficiaries_RemoveActiveInstrumentBeneficiary_Success() {
        // Create destination managed account to be used as beneficiary instrument
        final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

        // Create beneficiary in ACTIVE state
        final String activeBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

        final Response response = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                .Remove(List.of(activeBeneficiary)).build(), secretKey, getToken());
        response.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.PENDING_CHALLENGE.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("REMOVE"));

        //Beneficiary is still ACTIVE, new batch is in state PENDING_CHALLENGE
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(response), secretKey, getToken());
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE,
                activeBeneficiary, secretKey, getToken());

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
                getBeneficiaryBatchId(response), EnrolmentChannel.SMS.name(), secretKey, getToken());

        //Batch is COMPLETED, Beneficiary is in state REMOVED
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
                getBeneficiaryBatchId(response), secretKey, getToken());
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.REMOVED,
                activeBeneficiary, secretKey, getToken());
    }

    @Test
    public void RemoveBeneficiaries_RemoveActiveBankDetailsBeneficiary_Success() {
        // Create beneficiary with valid Faster Payments bank details
        final Pair<String, String> fasterPaymentsBankDetails = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
                CreateBeneficiariesBatchModel.FasterPaymentsBeneficiaryBatch(getIdentityType(),
                        getDestinationIdentityName(), List.of(fasterPaymentsBankDetails)).build();

        final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
        beneficiariesBatchResponse.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("CREATE"));

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
                getBeneficiaryBatchId(beneficiariesBatchResponse), EnrolmentChannel.SMS.name(), secretKey, getToken());

        final String beneficiary = BeneficiariesHelper.getBeneficiaryIdByAccountNumberAndBatchId(fasterPaymentsBankDetails.getLeft(), getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, beneficiary, secretKey, getToken());
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        final Response response = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                .Remove(List.of(beneficiary)).build(), secretKey, getToken());
        response.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.PENDING_CHALLENGE.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("REMOVE"));

        //Beneficiary is still ACTIVE, new batch is in state PENDING_CHALLENGE
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(response), secretKey, getToken());
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, beneficiary, secretKey, getToken());

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
                getBeneficiaryBatchId(response), EnrolmentChannel.SMS.name(), secretKey, getToken());

        //Batch is COMPLETED, Beneficiary is in state REMOVED
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
                getBeneficiaryBatchId(response), secretKey, getToken());
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.REMOVED, beneficiary, secretKey, getToken());
    }

    @Test
    public void RemoveBeneficiaries_RemovePendingChallengeBeneficiary_NotFound() {
        // Create destination managed account to be used as beneficiary instrument
        final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

        // Create beneficiary in PENDING_CHALLENGE state
        final String activeBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

        final Response response = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                .Remove(List.of(activeBeneficiary)).build(), secretKey, getToken());
        response.then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void RemoveBeneficiaries_RemoveInvalidBeneficiary_NotFound() {
        // Create destination managed account to be used as beneficiary instrument
        final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

        // Create beneficiary in INVALID state
        final String activeBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.INVALID, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

        final Response response = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                .Remove(List.of(activeBeneficiary)).build(), secretKey, getToken());
        response.then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void RemoveBeneficiaries_RemoveChallengeFailedBeneficiary_NotFound() {
        //enroll identity for authy
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

        // Create destination managed account to be used as beneficiary instrument
        final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

        // Create beneficiary in CHALLENGE_FAILED state
        final String activeBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.CHALLENGE_FAILED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

        BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                        .Remove(List.of(activeBeneficiary)).build(), secretKey, getToken())
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void RemoveBeneficiaries_RemoveRemovedBeneficiary_NotFound() {
        // Create destination managed account to be used as beneficiary instrument
        final String managedAccountId = createManagedAccount(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken()).getLeft();

        // Create beneficiary in CHALLENGE_FAILED state
        final String activeBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.REMOVED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccountId, secretKey, getToken()).getRight();

        final Response response = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                .Remove(List.of(activeBeneficiary)).build(), secretKey, getToken());
        response.then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void RemoveBeneficiaries_RemoveMultipleActiveInstrumentBeneficiaries_Success() {
        // Create destination managed accounts to be used as beneficiary instruments
        final List<String> managedAccountIds = createManagedAccounts(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken(), 3);

        final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
                CreateBeneficiariesBatchModel.InstrumentsBeneficiaryBatch(getIdentityType(),
                        ManagedInstrumentType.MANAGED_ACCOUNTS, getDestinationIdentityName(), managedAccountIds).build();

        final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
        beneficiariesBatchResponse.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("CREATE"));

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
                getBeneficiaryBatchId(beneficiariesBatchResponse), EnrolmentChannel.SMS.name(), secretKey, getToken());

        final List<String> beneficiaries = BeneficiariesHelper.getBeneficiariesIdsByBatchId(getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());
        beneficiaries.forEach(beneficiary ->
                BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, beneficiary, secretKey, getToken()));
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        final Response response = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                .Remove(beneficiaries).build(), secretKey, getToken());
        response.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.PENDING_CHALLENGE.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("REMOVE"));

        //Beneficiaries are still ACTIVE, new batch is in state PENDING_CHALLENGE
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(response), secretKey, getToken());
        beneficiaries.forEach(beneficiary ->
                BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, beneficiary, secretKey, getToken()));

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
                getBeneficiaryBatchId(response), EnrolmentChannel.SMS.name(), secretKey, getToken());

        //Batch is COMPLETED, Beneficiaries are in state REMOVED
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
                getBeneficiaryBatchId(response), secretKey, getToken());
        beneficiaries.forEach(beneficiary ->
                BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.REMOVED, beneficiary, secretKey, getToken()));
    }

    @Test
    public void RemoveBeneficiaries_RemoveMultipleActiveBankDetailsBeneficiaries_Success() {
        // Create multiple beneficiaries with valid SEPA bank details
        final CreateBeneficiariesBatchModel createBeneficiariesBatchModel =
                CreateBeneficiariesBatchModel.SEPABeneficiaryBatch(getIdentityType(),
                        getDestinationIdentityName(), ModelHelper.createMultipleValidSEPABankDetails(3)).build();

        final Response beneficiariesBatchResponse = BeneficiariesService.createBeneficiariesBatch(createBeneficiariesBatchModel, secretKey, getToken());
        beneficiariesBatchResponse.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.INITIALISED.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("CREATE"));

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
                getBeneficiaryBatchId(beneficiariesBatchResponse), EnrolmentChannel.SMS.name(), secretKey, getToken());

        final List<String> beneficiaries = BeneficiariesHelper.getBeneficiariesIdsByBatchId(getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());
        beneficiaries.forEach(beneficiary ->
                BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, beneficiary, secretKey, getToken()));
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED, getBeneficiaryBatchId(beneficiariesBatchResponse), secretKey, getToken());

        final Response response = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                .Remove(beneficiaries).build(), secretKey, getToken());
        response.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.PENDING_CHALLENGE.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("REMOVE"));

        //Beneficiaries are still ACTIVE, new batch is in state PENDING_CHALLENGE
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(response), secretKey, getToken());
        beneficiaries.forEach(beneficiary ->
                BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.ACTIVE, beneficiary, secretKey, getToken()));

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
                getBeneficiaryBatchId(response), EnrolmentChannel.SMS.name(), secretKey, getToken());

        //Batch is COMPLETED, Beneficiaries are in state REMOVED
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
                getBeneficiaryBatchId(response), secretKey, getToken());
        beneficiaries.forEach(beneficiary ->
                BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.REMOVED, beneficiary, secretKey, getToken()));
    }

    @Test
    public void RemoveBeneficiaries_RemoveOneActiveBeneficiaryInMultiple_Success() {
        //enroll identity for authy
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

        final List<String> managedAccounts = createManagedAccounts(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken(), 4);

        // Create beneficiary in ACTIVE state
        final String activeBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.ACTIVE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccounts.get(0), secretKey, getToken()).getRight();

        // Create beneficiary in PENDING_CHALLENGE state
        final String pendingChallengeBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccounts.get(1), secretKey, getToken()).getRight();

        // Create beneficiary in INVALID state
        final String invalidBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.INVALID, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccounts.get(2), secretKey, getToken()).getRight();

        // Create beneficiary in CHALLENGE_FAILED state
        final String challengedFailedBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.CHALLENGE_FAILED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccounts.get(3), secretKey, getToken()).getRight();

        final Response response = BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                .Remove(List.of(activeBeneficiary, pendingChallengeBeneficiary, invalidBeneficiary, challengedFailedBeneficiary))
                .build(), secretKey, getToken());
        response.then()
                .statusCode(SC_OK)
                .body("state", equalTo(BeneficiariesBatchState.PENDING_CHALLENGE.name()))
                .body("operationBatchId.batchId", notNullValue())
                .body("operationBatchId.operation", equalTo("REMOVE"));

        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.PENDING_CHALLENGE,
                getBeneficiaryBatchId(response), secretKey, getToken());

        BeneficiariesHelper.startAndVerifyBeneficiaryBatchOtp(new VerificationModel(VERIFICATION_CODE),
                getBeneficiaryBatchId(response), EnrolmentChannel.SMS.name(), secretKey, getToken());

        //Batch is COMPLETED, only ACTIVE beneficiary is REMOVED
        BeneficiariesHelper.ensureBeneficiaryBatchState(BeneficiariesBatchState.COMPLETED,
                getBeneficiaryBatchId(response), secretKey, getToken());

        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.REMOVED, activeBeneficiary, secretKey, getToken());
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.PENDING_CHALLENGE, pendingChallengeBeneficiary, secretKey, getToken());
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.INVALID, invalidBeneficiary, secretKey, getToken());
        BeneficiariesHelper.ensureBeneficiaryState(BeneficiaryState.CHALLENGE_FAILED, challengedFailedBeneficiary, secretKey, getToken());
    }

    @Test
    public void RemoveBeneficiaries_RemoveMultipleBeneficiariesDifferentThanActive_NotFound() {
        //enroll identity for authy
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(getIdentityId(), secretKey, getToken());

        final List<String> managedAccounts = createManagedAccounts(getDestinationManagedAccountProfileId(),
                getDestinationCurrency(), destinationSecretKey, getDestinationToken(), 4);

        // Create beneficiary in REMOVED state
        final String removedBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.REMOVED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccounts.get(0), secretKey, getToken()).getRight();

        // Create beneficiary in PENDING_CHALLENGE state
        final String pendingChallengeBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.PENDING_CHALLENGE, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccounts.get(1), secretKey, getToken()).getRight();

        // Create beneficiary in INVALID state
        final String invalidBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.INVALID, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccounts.get(2), secretKey, getToken()).getRight();

        // Create beneficiary in CHALLENGE_FAILED state
        final String challengedFailedBeneficiary = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                BeneficiaryState.CHALLENGE_FAILED, getIdentityType(), ManagedInstrumentType.MANAGED_ACCOUNTS,
                getDestinationIdentityName(), managedAccounts.get(3), secretKey, getToken()).getRight();

        BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                        .Remove(List.of(removedBeneficiary, pendingChallengeBeneficiary,
                                invalidBeneficiary, challengedFailedBeneficiary)).build(), secretKey, getToken())
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void RemoveBeneficiaries_BeneficiaryIdTooShortOrTooLong_BadRequest() {
        BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                        .Remove(List.of(RandomStringUtils.randomNumeric(17), RandomStringUtils.randomNumeric(19))).build(), secretKey, getToken())
                .then().statusCode(SC_NOT_FOUND);
    }

    @Disabled
    @Test
    @DisplayName("RemoveBeneficiaries_NullValueInBeneficiariesIdList_BadRequest - will be fixed by DEV-5314")
    public void RemoveBeneficiaries_NullValueInBeneficiariesIdList_BadRequest() {
        BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                        .Remove(Stream.of("123", null).collect(Collectors.toList())).build(), secretKey, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.beneficiaryIds: must match \"^[0-9]+$\""));
    }

    @Test
    public void RemoveBeneficiaries_InvalidBeneficiaryId_BadRequest() {
        BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                        .Remove(Stream.of("abc").collect(Collectors.toList())).build(), secretKey, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.beneficiaryIds: must match \"^[0-9]+$\""));
    }

    @Test
    public void RemoveBeneficiaries_EmptyBeneficiaryId_BadRequest() {
        BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                        .Remove(Stream.of("").collect(Collectors.toList())).build(), secretKey, getToken())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.beneficiaryIds: must match \"^[0-9]+$\""));
    }

    @Test
    public void RemoveBeneficiaries_NoApiKey_BadRequest() {
        BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                        .Remove(List.of(RandomStringUtils.randomNumeric(18))).build(), "", getToken())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void RemoveBeneficiaries_InvalidApiKey_Unauthorised() {
        BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                        .Remove(List.of(RandomStringUtils.randomNumeric(18))).build(), "abc", getToken())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void RemoveBeneficiaries_InvalidToken_Unauthorised() {
        BeneficiariesService.removeBeneficiaries(RemoveBeneficiariesModel
                        .Remove(List.of(RandomStringUtils.randomNumeric(18))).build(), secretKey, "")
                .then().statusCode(SC_UNAUTHORIZED);
    }
}