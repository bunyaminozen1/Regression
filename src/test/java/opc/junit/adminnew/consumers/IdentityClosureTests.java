package opc.junit.adminnew.consumers;

import opc.enums.opc.CountryCode;
import opc.enums.opc.KycState;
import opc.enums.sumsub.IdDocType;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.adminnew.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.AddressModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.adminnew.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static opc.enums.sumsub.IdDocType.ID_CARD_BACK_USER_IP;
import static opc.enums.sumsub.IdDocType.ID_CARD_FRONT_USER_IP;
import static opc.enums.sumsub.IdDocType.SELFIE;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL_USER_IP;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IdentityClosureTests extends BaseConsumersSetup {

    private static CreateConsumerModel createConsumerModel;
    private static Pair<String, String> consumer;

    @BeforeEach
    public void createConsumer(){
        createConsumerModel = createDefaultConsumerModel();
        consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
    }

    @Test
    public void ConsumerIdentityClosure_KycNotStartedStatus_Success() throws SQLException {

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        deactivateRootUser(consumer.getLeft());

        deactivateConsumerIdentity(consumer.getLeft());

        assertSuccessfulConsumerClosure(consumer.getLeft(), createConsumerModel);

        checkConsumerClosureStatus(consumer.getLeft(), "1");
    }

    @Test
    public void ConsumerIdentityClosure_KycInitiatedStatus_Success() throws SQLException {

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        deactivateRootUser(consumer.getLeft());

        deactivateConsumerIdentity(consumer.getLeft());

        assertSuccessfulConsumerClosure(consumer.getLeft(), createConsumerModel);

        checkConsumerSumSubStatus(weavrIdentity, createConsumerModel, "init");

        checkConsumerClosureStatus(consumer.getLeft(), "1");
    }

    @Test
    public void ConsumerIdentityClosure_KycPendingReviewStatus_Success() throws SQLException {

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        deactivateRootUser(consumer.getLeft());

        deactivateConsumerIdentity(consumer.getLeft());

        assertSuccessfulConsumerClosure(consumer.getLeft(), createConsumerModel);

        checkConsumerSumSubStatus(weavrIdentity, createConsumerModel, "pending");

        checkConsumerClosureStatus(consumer.getLeft(), "1");
    }

    @Test
    public void ConsumerIdentityClosure_KycApprovedStatus_Success() throws SQLException {

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.APPROVED.name());

        deactivateRootUser(consumer.getLeft());

        deactivateConsumerIdentity(consumer.getLeft());

        assertSuccessfulConsumerClosure(consumer.getLeft(), createConsumerModel);

        checkConsumerSumSubStatus(weavrIdentity, createConsumerModel, "completed");

        checkConsumerClosureStatus(consumer.getLeft(), "1");
    }

    @Test
    public void ConsumerIdentityClosure_KycRejectedStatus_Success() throws SQLException {

        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, consumer.getRight());

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(consumer.getLeft(), KycState.REJECTED.name());

        deactivateRootUser(consumer.getLeft());

        deactivateConsumerIdentity(consumer.getLeft());

        assertSuccessfulConsumerClosure(consumer.getLeft(), createConsumerModel);

        checkConsumerSumSubStatus(weavrIdentity, createConsumerModel, "completed");

        checkConsumerClosureStatus(consumer.getLeft(), "1");
    }

    @Test
    public void ConsumerIdentityClosure_IdentityNotDeactivated_ConditionsNotMet() throws SQLException {

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        deactivateRootUser(consumer.getLeft());

        assertFailedConsumerClosure(consumer.getLeft());

        checkConsumerClosureStatus(consumer.getLeft(), "0");
    }

    @Test
    public void ConsumerIdentityClosure_RootUserNotDeactivated_ConditionsNotMet() throws SQLException {

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        deactivateConsumerIdentity(consumer.getLeft());

        assertFailedConsumerClosure(consumer.getLeft());

        checkConsumerClosureStatus(consumer.getLeft(), "0");
    }

    @Test
    public void ConsumerIdentityClosure_AuthorizedUserNotDeactivated_ConditionsNotMet() throws SQLException {

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        //Create authorized user
        UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        deactivateRootUser(consumer.getLeft());

        deactivateConsumerIdentity(consumer.getLeft());

        assertFailedConsumerClosure(consumer.getLeft());

        checkConsumerClosureStatus(consumer.getLeft(), "0");
    }

    @Test
    public void ConsumerIdentityClosure_ManagedAccountNotDeactivated_ConditionsNotMet() throws SQLException {

        final CreateConsumerModel consumerModel = createDefaultConsumerModel();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerModel, secretKey);

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        //Create managed account
        ManagedAccountsHelper.createManagedAccount(managedAccountProfileId, createConsumerModel.getBaseCurrency(), secretKey, consumer.getRight());

        deactivateRootUser(consumer.getLeft());

        deactivateConsumerIdentity(consumer.getLeft());

        assertFailedConsumerClosure(consumer.getLeft());

        checkConsumerClosureStatus(consumer.getLeft(), "0");
    }

    @Test
    public void ConsumerIdentityClosure_ManagedCardNotDeactivated_ConditionsNotMet() throws SQLException {

        final CreateConsumerModel consumerModel = createDefaultConsumerModel();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerModel, secretKey);

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        //Create managed card
        ManagedCardsHelper.createManagedCard(prepaidCardProfileId, createConsumerModel.getBaseCurrency(), secretKey, consumer.getRight());

        deactivateRootUser(consumer.getLeft());

        deactivateConsumerIdentity(consumer.getLeft());

        assertFailedConsumerClosure(consumer.getLeft());

        checkConsumerClosureStatus(consumer.getLeft(), "0");
    }

    @Test
    public void ConsumerIdentityClosure_Multiple_ConditionsNotMet() throws SQLException {

        final CreateConsumerModel consumerModel = createDefaultConsumerModel();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerModel, secretKey);

        checkConsumerClosureStatus(consumer.getLeft(), "0");

        // Create authorized user
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());
        //Create managed account
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(managedAccountProfileId, consumerModel.getBaseCurrency(), secretKey, consumer.getRight());
        //Create managed card
        final String managedCardId = ManagedCardsHelper.createManagedCard(prepaidCardProfileId, consumerModel.getBaseCurrency(), secretKey, consumer.getRight());

        //Try to close identity, failing
        assertFailedConsumerClosure(consumer.getLeft());
        checkConsumerClosureStatus(consumer.getLeft(), "0");

        //Deactivate authorized user and try to close, failing
        AdminHelper.deactivateConsumerUser(consumer.getLeft(), user.getLeft(), adminToken);
        assertFailedConsumerClosure(consumer.getLeft());
        checkConsumerClosureStatus(consumer.getLeft(), "0");

        //Deactivate Managed Account and try to close, failing
        ManagedAccountsHelper.removeManagedAccount(managedAccountId, secretKey, consumer.getRight());
        assertFailedConsumerClosure(consumer.getLeft());
        checkConsumerClosureStatus(consumer.getLeft(), "0");

        //Deactivate Managed Card and try to close, failing
        ManagedCardsHelper.removeManagedCard(secretKey, managedCardId, consumer.getRight());
        assertFailedConsumerClosure(consumer.getLeft());
        checkConsumerClosureStatus(consumer.getLeft(), "0");

        //Deactivate root user and try to close, failing
        deactivateRootUser(consumer.getLeft());
        assertFailedConsumerClosure(consumer.getLeft());
        checkConsumerClosureStatus(consumer.getLeft(), "0");

        //Deactivate identity and try to close, successful
        deactivateConsumerIdentity(consumer.getLeft());
        assertSuccessfulConsumerClosure(consumer.getLeft(), consumerModel);
        checkConsumerClosureStatus(consumer.getLeft(), "1");
    }

    @Test
    public void ConsumerIdentityClosure_WithoutToken_Unauthorized() {

        AdminService.consumerIdentityClosure(consumer.getLeft(), "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ConsumerIdentityClosure_InvalidToken_Unauthorized() {

        AdminService.consumerIdentityClosure(consumer.getLeft(), RandomStringUtils.randomAlphanumeric(100))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ConsumerIdentityClosure_DifferentIdentityToken_Forbidden() {

        AdminService.consumerIdentityClosure(consumer.getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ConsumerIdentityClosure_DifferentImpersonatedTenantToken_NotFound() {

        final String impersonateTenant = AdminHelper.impersonateTenant("1");

        AdminService.consumerIdentityClosure(consumer.getLeft(), impersonateTenant)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ConsumerIdentityClosure_CrossIdentityId_NotFound() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AdminService.consumerIdentityClosure(corporate.getLeft(), impersonatedAdminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ConsumerIdentityClosure_UnknownIdentityId_NotFound() {

        AdminService.consumerIdentityClosure(RandomStringUtils.randomNumeric(18), impersonatedAdminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ConsumerIdentityClosure_WithoutIdentityId_MethodNotAllowed() {

        AdminService.consumerIdentityClosure("", impersonatedAdminToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    private CreateConsumerModel createDefaultConsumerModel(){

        return   CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();
    }

    private void deactivateRootUser(final String consumerId ){
        AdminHelper.deactivateConsumerUser(consumerId, consumerId, adminToken);
    }

    private void deactivateConsumerIdentity(final String consumerId){
        AdminHelper.deactivateConsumer(
                new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"), consumerId, impersonatedAdminToken);
    }

    private void assertSuccessfulConsumerClosure(final String consumerId, final CreateConsumerModel createConsumerModel){
        AdminService.consumerIdentityClosure(consumerId, impersonatedAdminToken)
                .then()
                .statusCode(SC_OK)
                .body("id.id", equalTo(consumerId))
                .body("rootUser.name", equalTo(createConsumerModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(createConsumerModel.getRootUser().getEmail()))
                .body("rootUser.active", equalTo(false))
                .body("active", equalTo(false));
    }

    private void assertFailedConsumerClosure (final String consumerId){

        AdminService.consumerIdentityClosure(consumerId, impersonatedAdminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CONDITIONS_NOT_MET"));
    }

    private void checkConsumerClosureStatus(final String consumerId, final String status) throws SQLException {

        final Map<String, String> consumer = ConsumersDatabaseHelper.getConsumer(consumerId).get(0);
        assertEquals(status, consumer.get("permanently_closed"));
        if (status.equals("1")) {
            assertNotNull(status, consumer.get("closed_timestamp"));
        }
    }

    private void checkConsumerSumSubStatus(final IdentityDetailsModel weavrIdentity,
                                           final CreateConsumerModel createConsumerModel,
                                           final String status){
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("fixedInfo.firstName", equalTo(createConsumerModel.getRootUser().getName()))
                .body("fixedInfo.lastName", equalTo(createConsumerModel.getRootUser().getSurname()))
                .body("review.reviewStatus", equalTo(status))
                .body("deleted", equalTo(true));
    }
}
