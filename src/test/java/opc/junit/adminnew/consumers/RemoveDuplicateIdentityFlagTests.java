package opc.junit.adminnew.consumers;

import opc.enums.opc.CountryCode;
import opc.enums.opc.KycState;
import opc.enums.sumsub.IdDocType;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.admin.RemoveDuplicateIdentityFlagModel;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import commons.models.DateOfBirthModel;
import opc.models.shared.AddressModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.adminnew.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static opc.enums.sumsub.IdDocType.ID_CARD_BACK_USER_IP;
import static opc.enums.sumsub.IdDocType.ID_CARD_FRONT_USER_IP;
import static opc.enums.sumsub.IdDocType.SELFIE;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL_USER_IP;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RemoveDuplicateIdentityFlagTests extends BaseConsumersSetup {

    final static RemoveDuplicateIdentityFlagModel removeComment = RemoveDuplicateIdentityFlagModel.removeComment();

    @Test
    public void Consumers_RejectedDuplicateIdentityRemoveFlag_Success() throws SQLException {
        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel();
        final String duplicateName = firstCreateConsumerModel.getRootUser().getName();
        final String duplicateSurname = firstCreateConsumerModel.getRootUser().getSurname();
        final DateOfBirthModel duplicateDob = firstCreateConsumerModel.getRootUser().getDateOfBirth();

        final Pair<String, String> firstConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final CreateConsumerModel secondCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(duplicateName)
                        .setSurname(duplicateSurname)
                        .setDateOfBirth(duplicateDob)
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerModel, secretKey);
        final String kycReferenceId = ConsumersHelper.startKyc(secretKey, secondConsumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, secondConsumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT_USER_IP, ID_CARD_BACK_USER_IP, SELFIE, UTILITY_BILL_USER_IP);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultQuestionnaire(applicantData));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());

        ConsumersHelper.verifyConsumerState(secondConsumer.getLeft(), KycState.REJECTED.name());
        ConsumersHelper.verifyConsumerOngoingState(secondConsumer.getLeft(), KycState.REJECTED.name());

        final Map<String, String> duplicateIdentityBeforeRemove = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);

        assertEquals(secondConsumer.getLeft(), duplicateIdentityBeforeRemove.get("consumer_id"));
        assertEquals(firstConsumer.getLeft(), duplicateIdentityBeforeRemove.get("duplicate_identity_id"));
        assertEquals(duplicateName, duplicateIdentityBeforeRemove.get("name"));
        assertEquals(duplicateSurname, duplicateIdentityBeforeRemove.get("surname"));
        assertEquals("1990-01-01", duplicateIdentityBeforeRemove.get("date_of_birth"));
        assertNull(duplicateIdentityBeforeRemove.get("approval_reason"));
        assertNull(duplicateIdentityBeforeRemove.get("approval_timestamp"));

        //remove after fix
        //ConsumersService.getConsumerKyc(secretKey, secondConsumer.getRight());

        AdminService.removeDuplicateIdentityFlag(removeComment, secondConsumer.getLeft(), impersonatedAdminToken)
                .then()
                .statusCode(SC_NO_CONTENT);


        final Map<String, String> duplicateIdentityAfterRemove = ConsumersDatabaseHelper.getDuplicateIdentity(secondConsumer.getLeft()).get(0);

        assertEquals(secondConsumer.getLeft(), duplicateIdentityAfterRemove.get("consumer_id"));
        assertEquals(firstConsumer.getLeft(), duplicateIdentityAfterRemove.get("duplicate_identity_id"));
        assertEquals(duplicateName, duplicateIdentityAfterRemove.get("name"));
        assertEquals(duplicateSurname, duplicateIdentityAfterRemove.get("surname"));
        assertEquals("1990-01-01", duplicateIdentityAfterRemove.get("date_of_birth"));
        assertEquals(removeComment.getComment(), duplicateIdentityAfterRemove.get("approval_reason"));
        assertNotNull(duplicateIdentityAfterRemove.get("approval_timestamp"));
    }

    @Test
    public void Consumers_ApprovedDuplicateIdentityRemoveFlag_KycStatusNotRejected() {

        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel();
        final String duplicateName = firstCreateConsumerModel.getRootUser().getName();
        final String duplicateSurname = firstCreateConsumerModel.getRootUser().getSurname();
        final DateOfBirthModel duplicateDob = firstCreateConsumerModel.getRootUser().getDateOfBirth();

        ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final CreateConsumerModel secondCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(duplicateName)
                        .setSurname(duplicateSurname)
                        .setDateOfBirth(duplicateDob)
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(secondCreateConsumerModel, secretKey);

        AdminService.removeDuplicateIdentityFlag(removeComment, secondConsumer.getLeft(), impersonatedAdminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_STATUS_NOT_REJECTED"));
    }

    @Test
    public void Consumers_PendingReviewDuplicateIdentityRemoveFlag_KycStatusNotRejected() throws SQLException {
        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel();
        final String duplicateName = firstCreateConsumerModel.getRootUser().getName();
        final String duplicateSurname = firstCreateConsumerModel.getRootUser().getSurname();
        final DateOfBirthModel duplicateDob = firstCreateConsumerModel.getRootUser().getDateOfBirth();

        ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final CreateConsumerModel secondCreateConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setName(duplicateName)
                        .setSurname(duplicateSurname)
                        .setDateOfBirth(duplicateDob)
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(CountryCode.DE)
                                .build())
                        .build())
                .build();

        final Pair<String, String> secondConsumer = ConsumersHelper.createAuthenticatedConsumer(secondCreateConsumerModel, secretKey);
        ConsumersHelper.startKyc(secretKey, secondConsumer.getRight());
        ConsumersDatabaseHelper.updateConsumerKyc(KycState.PENDING_REVIEW.name(), secondConsumer.getLeft());

        AdminService.removeDuplicateIdentityFlag(removeComment, secondConsumer.getLeft(), impersonatedAdminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_STATUS_NOT_REJECTED"));
    }

    @Test
    public void Consumers_NotDuplicateIdentityRemoveFlag_NotADuplicate() throws SQLException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);
        ConsumersDatabaseHelper.updateConsumerKyc(KycState.REJECTED.name(), consumer.getLeft());

        AdminService.removeDuplicateIdentityFlag(removeComment, consumer.getLeft(), impersonatedAdminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("NOT_A_DUPLICATE"));
    }

    @Test
    public void Consumers_RejectedDuplicateIdentityRemoveFlagWithoutComment_BadRequest() {

        final CreateConsumerModel firstCreateConsumerModel = createDefaultConsumerModel();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(firstCreateConsumerModel, secretKey);

        final RemoveDuplicateIdentityFlagModel removeDuplicateIdentityFlagModel =
                RemoveDuplicateIdentityFlagModel.builder()
                        .comment(null)
                        .build();

        AdminService.removeDuplicateIdentityFlag(removeDuplicateIdentityFlagModel, consumer.getLeft(), impersonatedAdminToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.comment: must not be blank"));
    }

    @Test
    public void Consumers_RemoveFlagWithoutToken_Unauthorized() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);

        AdminService.removeDuplicateIdentityFlag(removeComment, consumer.getLeft(), "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Consumers_RemoveFlagInvalidToken_Unauthorized() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);

        AdminService.removeDuplicateIdentityFlag(removeComment, consumer.getLeft(), RandomStringUtils.randomAlphanumeric(20))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Consumers_RemoveFlagDifferentIdentityToken_Forbidden() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);

        AdminService.removeDuplicateIdentityFlag(removeComment, consumer.getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void Consumers_RemoveFlagDifferentWithoutIdentityId_NotFound() {

        AdminService.removeDuplicateIdentityFlag(removeComment, "", impersonatedAdminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void Consumers_RemoveFlagDifferentInvalidIdentityId_NotFound() {

        AdminService.removeDuplicateIdentityFlag(removeComment, RandomStringUtils.randomNumeric(18), impersonatedAdminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
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
}
