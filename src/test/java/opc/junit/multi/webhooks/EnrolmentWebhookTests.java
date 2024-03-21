package opc.junit.multi.webhooks;

import opc.enums.opc.ApiSchemaDefinition;
import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.FeeType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.UserType;
import opc.enums.opc.WebhookType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.webhook.WebhookAuthenticationFactorsEventModel;
import opc.models.webhook.WebhookOwtEventModel;
import opc.services.admin.AdminService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(MultiTags.IDENTITY_WEBHOOKS)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public class EnrolmentWebhookTests extends BaseWebhooksSetup {

    @BeforeAll
    public static void Setup() {
        InnovatorHelper.enableAuthy(programmeId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        AdminHelper.setProgrammeAuthyChallengeLimit(programmeId, LimitInterval.DAILY, 5000,
                AdminService.loginAdmin());
    }

    @Test
    public void Webhooks_OtpEnrolmentAccepted_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        final WebhookAuthenticationFactorsEventModel enrolmentEvent = getEnrolmentWebhookResponse(timestamp, corporate.getLeft());

        assertEnrolmentEvent(corporate.getLeft(), UserType.ROOT.name(), "ACTIVE", "SMS_OTP", enrolmentEvent);
    }

    @Test
    public void Webhooks_AuthyEnrolmentAccepted_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());

        final WebhookAuthenticationFactorsEventModel enrolmentEvent = getEnrolmentWebhookResponse(timestamp, corporate.getLeft());

        assertEnrolmentEvent(corporate.getLeft(), UserType.ROOT.name(), "ACTIVE", "AUTHY_PUSH", enrolmentEvent);
    }

    @Test
    public void Webhooks_OkayEnrolmentAccepted_Success() {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, passcodeAppSecretKey);

        final long timestamp = Instant.now().toEpochMilli();
        SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey, corporate.getRight());

        final WebhookAuthenticationFactorsEventModel enrolmentEvent = getEnrolmentWebhookResponse(timestamp, corporate.getLeft());

        assertEnrolmentEvent(corporate.getLeft(), UserType.ROOT.name(), "ACTIVE", "BIOMETRIC", enrolmentEvent);
    }

    @Test
    public void Webhooks_AuthyEnrolmentRejected_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final long timestamp = Instant.now().toEpochMilli();
        AuthenticationFactorsHelper.enrolAndRejectAuthyPush(corporate.getLeft(), secretKey, corporate.getRight());

        final WebhookAuthenticationFactorsEventModel enrolmentEvent = getEnrolmentWebhookResponse(timestamp, corporate.getLeft());

        assertEnrolmentEvent(corporate.getLeft(), UserType.ROOT.name(), "INACTIVE", "AUTHY_PUSH", enrolmentEvent);
    }

    @Test
    public void Webhooks_OkayEnrolmentRejected_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, passcodeAppSecretKey);

        final long timestamp = Instant.now().toEpochMilli();
        SecureHelper.enrolAndRejectOkayPush(corporate.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey, corporate.getRight());

        final WebhookAuthenticationFactorsEventModel enrolmentEvent = getEnrolmentWebhookResponse(timestamp, corporate.getLeft());

        assertEnrolmentEvent(corporate.getLeft(), UserType.ROOT.name(), "INACTIVE", "BIOMETRIC", enrolmentEvent);
    }

    @Test
    public void Webhooks_OutgoingWireTransferOtp_OwtCompleted() {

        final EnrolmentChannel enrolmentChannel =
                EnrolmentChannel.SMS;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, enrolmentChannel.name(), secretKey, corporate.getRight());

        final long depositAmount = 10000L;
        final long owtAmount = 200L;

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporate.getRight());

        TestHelper
                .simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                        depositAmount, secretKey, corporate.getRight(), 1);

        final long timestamp = Instant.now().toEpochMilli();

        final Pair<String, OutgoingWireTransfersModel> owt =
                OutgoingWireTransfersHelper.sendSuccessfulOwtOtp(outgoingWireTransfersProfileId, managedAccountId,
                        new CurrencyAmount(createCorporateModel.getBaseCurrency(), owtAmount), secretKey, corporate.getRight());

        final List<WebhookOwtEventModel> owtEvents = getOwtWebhookResponses(timestamp, owt.getLeft(), 4);

        assertOwtEvent(owt, "PENDING_CHALLENGE", owtEvents);
        assertOwtEvent(owt, "SUBMITTED", owtEvents);
        assertOwtEvent(owt, "APPROVED", owtEvents);
        assertOwtEvent(owt, "COMPLETED", owtEvents);
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = {"AUTHY", "BIOMETRIC"})
    public void Webhooks_OutgoingWireTransferPush_OwtCompleted(final EnrolmentChannel enrolmentChannel) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
                        .setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, passcodeAppSecretKey);
        CorporatesHelper.verifyKyb(passcodeAppSecretKey, corporate.getLeft());

        if (enrolmentChannel.name().equals("AUTHY")) {
            AuthenticationFactorsHelper.enrolAndVerifyPush(corporate.getLeft(), passcodeAppSecretKey, corporate.getRight());
        } else {
            SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey, corporate.getRight());
        }

        final long depositAmount = 10000L;
        final long owtAmount = 200L;

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(passcodeAppCorporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, passcodeAppSecretKey, corporate.getRight());

        TestHelper
                .simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                        depositAmount, passcodeAppSecretKey, corporate.getRight(), 1);

        final long timestamp = Instant.now().toEpochMilli();

        final Pair<String, OutgoingWireTransfersModel> owt =
                OutgoingWireTransfersHelper.sendSuccessfulOwtPush(enrolmentChannel, passcodeAppOutgoingWireTransfersProfileId, managedAccountId,
                        new CurrencyAmount(createCorporateModel.getBaseCurrency(), owtAmount), passcodeAppSecretKey, corporate.getRight());

        final List<WebhookOwtEventModel> owtEvents = getOwtWebhookResponses(timestamp, owt.getLeft(), 4);

        assertOwtEvent(owt, "PENDING_CHALLENGE", owtEvents);
        assertOwtEvent(owt, "SUBMITTED", owtEvents);
        assertOwtEvent(owt, "APPROVED", owtEvents);
        assertOwtEvent(owt, "COMPLETED", owtEvents);
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = {"AUTHY", "BIOMETRIC"})
    public void Webhooks_OutgoingWireTransferPush_OwtRejected(final EnrolmentChannel enrolmentChannel) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
                        .setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, passcodeAppSecretKey);
        CorporatesHelper.verifyKyb(passcodeAppSecretKey, corporate.getLeft());

        if (enrolmentChannel.name().equals("AUTHY")) {
            AuthenticationFactorsHelper.enrolAndVerifyPush(corporate.getLeft(), passcodeAppSecretKey, corporate.getRight());
        } else {
            SecureHelper.enrolAndVerifyBiometric(corporate.getLeft(), passcodeAppSharedKey, passcodeAppSecretKey, corporate.getRight());
        }

        final long depositAmount = 10000L;
        final long owtAmount = 200L;

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(passcodeAppCorporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, passcodeAppSecretKey, corporate.getRight());

        TestHelper
                .simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                        depositAmount, passcodeAppSecretKey, corporate.getRight(), 1);

        final long timestamp = Instant.now().toEpochMilli();
        final Pair<String, OutgoingWireTransfersModel> owt =
                OutgoingWireTransfersHelper.sendRejectedOwtPush(enrolmentChannel, passcodeAppOutgoingWireTransfersProfileId, managedAccountId,
                        new CurrencyAmount(createCorporateModel.getBaseCurrency(), owtAmount), passcodeAppSecretKey, corporate.getRight());

        final List<WebhookOwtEventModel> owtEvents = getOwtWebhookResponses(timestamp, owt.getLeft(), 2);

        assertOwtEvent(owt, "PENDING_CHALLENGE", owtEvents);
        assertOwtEvent(owt, "REJECTED", owtEvents);
    }

    private void assertOwtEvent(final Pair<String, OutgoingWireTransfersModel> owt,
                                final String state,
                                final List<WebhookOwtEventModel> owtEvents) {

        final WebhookOwtEventModel owtEvent =
                owtEvents.stream().filter(x -> x.getEventType().equals(state)).collect(Collectors.toList()).get(0);

        final SepaBankDetailsModel sepaBankDetails =
                (SepaBankDetailsModel) owt.getRight().getDestinationBeneficiary().getBankAccountDetails();

        assertEquals(state, owtEvent.getEventType());
        assertNotNull(owtEvent.getPublishedTimestamp());
        assertNotNull(owtEvent.getTransfer().getCreationTimestamp());
        assertEquals(owt.getRight().getDescription(), owtEvent.getTransfer().getDescription());
        assertEquals(owt.getRight().getDestinationBeneficiary().getAddress(), owtEvent.getTransfer().getDestination().getBeneficiaryAddress());
        assertEquals(owt.getRight().getDestinationBeneficiary().getBankAddress(), owtEvent.getTransfer().getDestination().getBeneficiaryBankAddress());
        assertEquals(owt.getRight().getDestinationBeneficiary().getBankCountry(), owtEvent.getTransfer().getDestination().getBeneficiaryBankCountry());
        assertEquals(owt.getRight().getDestinationBeneficiary().getBankName(), owtEvent.getTransfer().getDestination().getBeneficiaryBankName());
        assertEquals(owt.getRight().getDestinationBeneficiary().getName(), owtEvent.getTransfer().getDestination().getBeneficiaryName());
        assertEquals(sepaBankDetails.getBankIdentifierCode(), owtEvent.getTransfer().getDestination().getSepa().get("bankIdentifierCode"));
        assertEquals(sepaBankDetails.getIban(), owtEvent.getTransfer().getDestination().getSepa().get("iban"));
        assertEquals(owt.getLeft(), owtEvent.getTransfer().getId().get("id"));
        assertEquals("outgoing_wire_transfers", owtEvent.getTransfer().getId().get("type"));
        assertEquals(owt.getRight().getProfileId(), owtEvent.getTransfer().getProfileId());
        assertEquals(owt.getRight().getSourceInstrument().getId(), owtEvent.getTransfer().getSource().get("id"));
        assertEquals(owt.getRight().getSourceInstrument().getType(), owtEvent.getTransfer().getSource().get("type"));
        assertEquals(state, owtEvent.getTransfer().getState());
        assertEquals(owt.getRight().getTag(), owtEvent.getTransfer().getTag());
        assertEquals(owt.getRight().getTransferAmount().getCurrency(), owtEvent.getTransfer().getTransactionAmount().get("currency"));
        assertEquals(owt.getRight().getTransferAmount().getAmount(), Long.parseLong(owtEvent.getTransfer().getTransactionAmount().get("amount")));
        assertEquals(owt.getRight().getTransferAmount().getCurrency(), owtEvent.getTransfer().getTransactionFee().get("currency"));
        assertEquals(TestHelper.getFees(owt.getRight().getTransferAmount().getCurrency()).get(FeeType.SEPA_OWT_FEE).getAmount(),
                Long.parseLong(owtEvent.getTransfer().getTransactionFee().get("amount")));
        assertEquals(owt.getRight().getTransferAmount().getCurrency(), owtEvent.getTransfer().getTransferAmount().get("currency"));
        assertEquals(owt.getRight().getTransferAmount().getAmount(), Long.parseLong(owtEvent.getTransfer().getTransferAmount().get("amount")));
        assertEquals("SEPA", owtEvent.getTransfer().getType());
    }

    private void assertEnrolmentEvent(final String userId,
                                      final String userType,
                                      final String status,
                                      final String eventType,
                                      final WebhookAuthenticationFactorsEventModel authenticationEvent) {

        assertEquals(userId, authenticationEvent.getCredentialId().get("id"));
        assertEquals(userType, authenticationEvent.getCredentialId().get("type"));
        assertNotNull(authenticationEvent.getPublishedTimestamp());
        assertEquals(status, authenticationEvent.getStatus());
        assertEquals(eventType, authenticationEvent.getType());
    }

    private WebhookAuthenticationFactorsEventModel getEnrolmentWebhookResponse(final long timestamp,
                                                                               final String identityId) {
        return (WebhookAuthenticationFactorsEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.AUTHENTICATION_FACTORS,
                Pair.of("credentialId.id", identityId),
                WebhookAuthenticationFactorsEventModel.class,
                ApiSchemaDefinition.EnrolmentEvent);
    }

    private List<WebhookOwtEventModel> getOwtWebhookResponses(final long timestamp,
                                                              final String transferId,
                                                              final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.OWT,
                Pair.of("transfer.id.id", transferId),
                WebhookOwtEventModel.class,
                ApiSchemaDefinition.OutgoingWireTransferEventV3,
                expectedEventCount);
    }

    @AfterAll
    public static void DisableWebhooks(){
        InnovatorHelper.disableWebhook(UpdateProgrammeModel.WebHookUrlSetup(passcodeAppProgrammeId, true, webhookServiceDetails.getRight()),
                passcodeAppProgrammeId, passcodeAppInnovatorToken);
    }
}
