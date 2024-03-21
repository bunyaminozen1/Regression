package opc.junit.multi.webhooks;

import commons.enums.Currency;
import commons.enums.State;
import opc.enums.opc.AdminFeeType;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.BeneficiaryState;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.KybState;
import opc.enums.opc.KycLevel;
import opc.enums.opc.KycState;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwtType;
import opc.enums.opc.WebhookType;
import opc.helpers.ModelHelper;
import opc.junit.database.ManagedAccountsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.BeneficiariesHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.admin.ChargeFeeModel;
import opc.models.admin.FeeSpecModel;
import opc.models.admin.ScaConfigModel;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.beneficiaries.BeneficiaryResponseModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.outgoingwiretransfers.Beneficiary;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.multi.sends.BulkSendFundsModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.simulator.SimulateDepositModel;
import opc.models.webhook.WebhookChargeFeesEventModel;
import opc.models.webhook.WebhookIdentityActivationModel;
import opc.models.webhook.WebhookIdentityDeactivationModel;
import opc.models.webhook.WebhookKybEventModel;
import opc.models.webhook.WebhookKycEventModel;
import opc.models.webhook.WebhookManagedAccountDepositEventModel;
import opc.models.webhook.WebhookManualTransactionEventModel;
import opc.models.webhook.WebhookOwtEventModel;
import opc.models.webhook.WebhookSendsEventModel;
import opc.models.webhook.WebhookSettlementEventModel;
import opc.models.webhook.WebhookTransfersEventModel;
import opc.services.admin.AdminFeesService;
import opc.services.admin.AdminService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.SendsService;
import opc.services.multi.TransfersService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(MultiTags.WEBHOOKS)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public class WebhookTests extends BaseWebhooksSetup {

    @Test
    public void Webhooks_CorporateDeposit_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                                .build(), secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        final Long depositAmount = 1000L;
        final long timestamp = Instant.now().toEpochMilli();
        final SimulateDepositModel simulateDepositModel =
                SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(createCorporateModel.getBaseCurrency(), depositAmount));

        SimulatorService
                .simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        final WebhookManagedAccountDepositEventModel event = getManagedAccountsDepositWebhookResponse(timestamp, managedAccountId);

        assertManagedAccountsDepositEvent(corporate.getLeft(), IdentityType.CORPORATE.getValue(), createCorporateModel.getRootUser().getEmail(),
                managedAccountId, createCorporateModel.getBaseCurrency(), depositAmount.intValue(), event, State.COMPLETED.name(), false);
    }

    @Test
    public void Webhooks_ConsumerDeposit_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        ConsumersHelper.verifyKyc(secretKey, consumer.getLeft());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, createConsumerModel.getBaseCurrency())
                                .build(), secretKey, consumer.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumer.getRight());

        final Long depositAmount = 1000L;
        final long timestamp = Instant.now().toEpochMilli();
        final SimulateDepositModel simulateDepositModel = SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(createConsumerModel.getBaseCurrency(), depositAmount));
        SimulatorService
                .simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        final WebhookManagedAccountDepositEventModel event = getManagedAccountsDepositWebhookResponse(timestamp, managedAccountId);

        assertManagedAccountsDepositEvent(consumer.getLeft(), IdentityType.CONSUMER.getValue(), createConsumerModel.getRootUser().getEmail(),
                managedAccountId, createConsumerModel.getBaseCurrency(), depositAmount.intValue(), event, State.COMPLETED.name(), false);
    }

    @Test
    public void Webhooks_CorporatePendingDeposit_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

         final String managedAccountId =
            ManagedAccountsHelper.createManagedAccount(
                CreateManagedAccountModel
                    .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                    .build(), secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        final String iban = getManagedAccountIbanId(managedAccountId);

        final long depositAmount = 1000L;
        final long timestamp = Instant.now().toEpochMilli();

        TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
            ModelHelper.generateRandomValidIban(), createCorporateModel.getBaseCurrency(), depositAmount,true, false, "RefTest123", true, innovatorId,  secretKey, corporate.getRight());

        final WebhookManagedAccountDepositEventModel event = getManagedAccountsDepositWebhookResponse(timestamp, managedAccountId);

        assertManagedAccountsDepositEvent(corporate.getLeft(), IdentityType.CORPORATE.getValue(), createCorporateModel.getRootUser().getEmail(),
            managedAccountId, createCorporateModel.getBaseCurrency(), (int) depositAmount, event, State.COMPLETED.name(), true);
    }

    @Test
    public void Webhooks_ConsumerPendingDeposit_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        ConsumersHelper.verifyKyc(secretKey, consumer.getLeft());

        final String managedAccountId =
            ManagedAccountsHelper.createManagedAccount(
                CreateManagedAccountModel
                    .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, createConsumerModel.getBaseCurrency())
                    .build(), secretKey, consumer.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, consumer.getRight());

        final String iban = getManagedAccountIbanId(managedAccountId);

        final long depositAmount = 1000L;
        final long timestamp = Instant.now().toEpochMilli();

        TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
            ModelHelper.generateRandomValidIban(), createConsumerModel.getBaseCurrency(), depositAmount,true, false, "RefTest123", true, innovatorId,  secretKey, consumer.getRight());

        final WebhookManagedAccountDepositEventModel event = getManagedAccountsDepositWebhookResponse(timestamp, managedAccountId);

        assertManagedAccountsDepositEvent(consumer.getLeft(), IdentityType.CONSUMER.getValue(), createConsumerModel.getRootUser().getEmail(),
            managedAccountId, createConsumerModel.getBaseCurrency(), (int) depositAmount, event, State.COMPLETED.name(), true);
    }

    @Test
    public void Webhooks_SepaInstantPendingDeposit_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final String managedAccountId =
            ManagedAccountsHelper.createManagedAccount(
                CreateManagedAccountModel
                    .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                    .build(), secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        final long depositAmount = 1000L;
        final long timestamp = Instant.now().toEpochMilli();

        final String iban = getManagedAccountIbanId(managedAccountId);

        TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
            ModelHelper.generateRandomValidIban(), createCorporateModel.getBaseCurrency(), depositAmount,true, true, "RefTest123", true, innovatorId,  secretKey, corporate.getRight());

        final WebhookManagedAccountDepositEventModel event = getManagedAccountsDepositWebhookResponse(timestamp, managedAccountId);

        assertManagedAccountsDepositEvent(corporate.getLeft(), IdentityType.CORPORATE.getValue(), createCorporateModel.getRootUser().getEmail(),
            managedAccountId, createCorporateModel.getBaseCurrency(), (int) depositAmount, event, State.COMPLETED.name(), true);
    }

    @Test
    public void Webhooks_SepaInstantPendingDeposit_Rejected() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final String managedAccountId =
            ManagedAccountsHelper.createManagedAccount(
                CreateManagedAccountModel
                    .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                    .build(), secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        final long depositAmount =  (corporateMaxSum + 10000);
        final long timestamp = Instant.now().toEpochMilli();

        final String iban = getManagedAccountIbanId(managedAccountId);

        TestHelper.simulateMaPendingDepositByIbanId(iban, managedAccountId, "SenderTest",
            ModelHelper.generateRandomValidIban(), createCorporateModel.getBaseCurrency(), depositAmount,true, true, "RefTest123", false, innovatorId,  secretKey, corporate.getRight());

        final WebhookManagedAccountDepositEventModel event = getManagedAccountsDepositWebhookResponse(timestamp, managedAccountId);

        assertManagedAccountsDepositEvent(corporate.getLeft(), IdentityType.CORPORATE.getValue(), createCorporateModel.getRootUser().getEmail(),
            managedAccountId, createCorporateModel.getBaseCurrency(), (int) depositAmount, event, State.REJECTED.name(), true);
    }

    @Test
    public void Webhooks_CorporateKyb_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final long timestamp = Instant.now().toEpochMilli();
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final WebhookKybEventModel kybEvent = getKybWebhookResponse(timestamp, corporate.getLeft());

        assertKybEvent(corporate.getLeft(), createCorporateModel.getRootUser().getEmail(), kybEvent,
                KybState.APPROVED, Optional.empty(), Optional.empty());
    }

    @Test
    public void Webhooks_ConsumerKyc_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final long timestamp = Instant.now().toEpochMilli();
        ConsumersHelper.verifyKyc(secretKey, consumer.getLeft());

        final WebhookKycEventModel event = getKycWebhookResponse(timestamp, consumer.getLeft());

        assertKycEvent(consumer.getLeft(), createConsumerModel.getRootUser().getEmail(),
                event, KycState.APPROVED, KycState.APPROVED,
                KycLevel.KYC_LEVEL_2, KycLevel.KYC_LEVEL_2, Optional.empty(), Optional.empty());
    }

    @Test
    public void Webhooks_TransferTransaction_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        ConsumersHelper.verifyKyc(secretKey, consumer.getLeft());

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, createConsumerModel.getBaseCurrency()).build(),
                        secretKey, consumer.getRight());

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, createConsumerModel.getBaseCurrency()).build(),
                        secretKey, consumer.getRight());

        TestHelper
                .simulateManagedAccountDeposit(sourceManagedAccountId, createConsumerModel.getBaseCurrency(), 10000L, secretKey, consumer.getRight());

        final Long depositAmount = 500L;

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createConsumerModel.getBaseCurrency(), depositAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .build();

        final long timestamp = Instant.now().toEpochMilli();
        TransfersService.transferFunds(transferFundsModel, secretKey, consumer.getRight(), Optional.empty()).then().statusCode(SC_OK);

        final WebhookTransfersEventModel event = getTransfersWebhookResponse(timestamp, sourceManagedAccountId);

        assertTransfersEvent(transferFundsModel, sourceManagedAccountId, MANAGED_ACCOUNTS, destinationManagedAccountId,
                MANAGED_ACCOUNTS, createConsumerModel.getBaseCurrency(), depositAmount, State.COMPLETED,
                "UNDEFINED", "COMPLETED", event);
    }

    @Test
    public void Webhooks_SendTransaction_Success() {

        final CreateConsumerModel sourceConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> sourceConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(sourceConsumerModel, secretKey);

        final CreateConsumerModel destinationConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(sourceConsumerModel.getBaseCurrency()).build();
        final Pair<String, String> destinationConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(destinationConsumerModel, secretKey);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, sourceConsumer.getRight());

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, destinationConsumer.getRight());

        TestHelper
                .simulateManagedAccountDeposit(sourceManagedAccountId, sourceConsumerModel.getBaseCurrency(), 10000L, secretKey, sourceConsumer.getRight());

        final Long depositAmount = 500L;

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(destinationConsumerModel.getBaseCurrency(), depositAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .build();

        final long timestamp = Instant.now().toEpochMilli();
        SendsService.sendFunds(sendFundsModel, secretKey, sourceConsumer.getRight(), Optional.empty()).then().statusCode(SC_OK);

        final WebhookSendsEventModel sendEvent = getSendWebhookResponse(timestamp, sourceManagedAccountId);

        assertSendEvent(sendFundsModel, sendEvent, "COMPLETED", "COMPLETED", "NOT_ENABLED", "NO_ERROR");
    }

    @Test
    public void Webhooks_SendBulkTransaction_Success() {

        enableSendsSca();

        final CreateConsumerModel sourceConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> sourceConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(sourceConsumerModel, secretKey);

        final CreateConsumerModel destinationConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(sourceConsumerModel.getBaseCurrency()).build();
        final Pair<String, String> destinationConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(destinationConsumerModel, secretKey);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, sourceConsumer.getRight());

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, destinationConsumer.getRight());

        TestHelper
                .simulateManagedAccountDeposit(sourceManagedAccountId, sourceConsumerModel.getBaseCurrency(), 10000L, secretKey, sourceConsumer.getRight());

        final Long depositAmount = 500L;

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(destinationConsumerModel.getBaseCurrency(), depositAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .build();

        final long timestamp = Instant.now().toEpochMilli();

        SendsService.bulkSendFunds(BulkSendFundsModel.builder().sends(List.of(sendFundsModel, sendFundsModel)).build(), secretKey, sourceConsumer.getRight(), Optional.empty());

        final List<WebhookSendsEventModel> sendEvents = getSendWebhookResponses(timestamp, sourceManagedAccountId, 2);
        final WebhookSendsEventModel firstSendEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);
        final WebhookSendsEventModel secondSendEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);

        assertSendEvent(sendFundsModel, firstSendEvent, "PENDING_CHALLENGE", "PENDING", "NO_EXEMPTION", "NO_ERROR");
        assertSendEvent(sendFundsModel, secondSendEvent, "PENDING_CHALLENGE", "PENDING", "NO_EXEMPTION", "NO_ERROR");

        disableSendsSca();
    }

    @Test
    public void Webhooks_SendBulkWithValidAndInvalidTransaction_Success() {

        enableSendsSca();

        final CreateConsumerModel sourceConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> sourceConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(sourceConsumerModel, secretKey);

        final CreateConsumerModel destinationConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(sourceConsumerModel.getBaseCurrency()).build();
        final Pair<String, String> destinationConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(destinationConsumerModel, secretKey);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, sourceConsumer.getRight());

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, destinationConsumer.getRight());

        TestHelper
                .simulateManagedAccountDeposit(sourceManagedAccountId, sourceConsumerModel.getBaseCurrency(), 10000L, secretKey, sourceConsumer.getRight());

        final Long depositAmount = 500L;

        final SendFundsModel invalidSendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(destinationConsumerModel.getBaseCurrency(), depositAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(String.format("1%s", RandomStringUtils.randomNumeric(17)), MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .build();

        final SendFundsModel validSendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(destinationConsumerModel.getBaseCurrency(), depositAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .build();

        final long timestamp = Instant.now().toEpochMilli();

        SendsService.bulkSendFunds(
                BulkSendFundsModel.builder().sends(List.of(invalidSendFundsModel, validSendFundsModel)).build(), secretKey, sourceConsumer.getRight(), Optional.empty());

        final List<WebhookSendsEventModel> sendEvents = getSendWebhookResponses(timestamp, sourceManagedAccountId, 2);

        final WebhookSendsEventModel invalidEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("INVALID")).collect(Collectors.toList()).get(0);
        final WebhookSendsEventModel validEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);

        assertSendEvent(invalidSendFundsModel, invalidEvent, "INVALID", "INVALID", "NOT_ENABLED", "DESTINATION_NOT_FOUND");
        assertSendEvent(validSendFundsModel, validEvent, "PENDING_CHALLENGE", "PENDING", "NO_EXEMPTION", "NO_ERROR");

        disableSendsSca();
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void Webhooks_SendTransactionOtpVerified_SendCompleted() {
        enableSendsSca();

        final CreateConsumerModel sourceConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> sourceConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(sourceConsumerModel, secretKey);

        final CreateConsumerModel destinationConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(sourceConsumerModel.getBaseCurrency()).build();
        final Pair<String, String> destinationConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(destinationConsumerModel, secretKey);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, sourceConsumerModel.getBaseCurrency()).build(),
                        secretKey, sourceConsumer.getRight());

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, destinationConsumer.getRight());

        TestHelper
                .simulateManagedAccountDeposit(sourceManagedAccountId, sourceConsumerModel.getBaseCurrency(), 10000L, secretKey, sourceConsumer.getRight());

        final long timestamp = Instant.now().toEpochMilli();

        final Pair<String, SendFundsModel> send =
            SendsHelper.sendFundsSuccessfulOtpVerified(sendsProfileId,
                new CurrencyAmount(destinationConsumerModel.getBaseCurrency(), 500L), sourceManagedAccountId, destinationManagedAccountId,
                secretKey, sourceConsumer.getRight());

        final List<WebhookSendsEventModel> sendEvents = getSendWebhookResponses(timestamp, sourceManagedAccountId, 2);

        final WebhookSendsEventModel pendingEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);
        final WebhookSendsEventModel completedEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("COMPLETED")).collect(Collectors.toList()).get(0);

        assertSendEvent(send.getRight(), pendingEvent, "PENDING_CHALLENGE", "PENDING", "NO_EXEMPTION", "NO_ERROR");
        assertSendEvent(send.getRight(), completedEvent, "COMPLETED", "COMPLETED", "NO_EXEMPTION", "NO_ERROR");

        disableSendsSca();
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void Webhooks_SendTransactionOtp_ChallengeExpired() throws InterruptedException {
        enableSendsSca();

        final CreateConsumerModel sourceConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> sourceConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(sourceConsumerModel, secretKey);

        final CreateConsumerModel destinationConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(sourceConsumerModel.getBaseCurrency()).build();
        final Pair<String, String> destinationConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(destinationConsumerModel, secretKey);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, sourceConsumerModel.getBaseCurrency()).build(),
                        secretKey, sourceConsumer.getRight());

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, destinationConsumer.getRight());

        TestHelper
                .simulateManagedAccountDeposit(sourceManagedAccountId, sourceConsumerModel.getBaseCurrency(), 10000L, secretKey, sourceConsumer.getRight());

        final long timestamp = Instant.now().toEpochMilli();

        final Pair<String, SendFundsModel> send =
                SendsHelper.sendFundsAndStartOtpChallenge(sendsProfileId,
                        new CurrencyAmount(destinationConsumerModel.getBaseCurrency(), 500L), sourceManagedAccountId, destinationManagedAccountId,
                        secretKey, sourceConsumer.getRight());

        TimeUnit.SECONDS.sleep(140);

        final List<WebhookSendsEventModel> sendEvents = getSendWebhookResponses(timestamp, sourceManagedAccountId, 2);

        final WebhookSendsEventModel pendingEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);
        final WebhookSendsEventModel failedEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("FAILED")).collect(Collectors.toList()).get(0);

        assertSendEvent(send.getRight(), pendingEvent, "PENDING_CHALLENGE", "PENDING", "NO_EXEMPTION", "NO_ERROR");
        assertSendEvent(send.getRight(), failedEvent, "FAILED", "REJECTED", "NO_EXEMPTION", "NO_ERROR");

        disableSendsSca();
    }

    @Test
    public void Webhooks_ManualTransactionToManagedAccount_Success() {
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        ConsumersHelper.verifyKyc(secretKey, consumer.getLeft());

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, createConsumerModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, consumer.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, createConsumerModel.getBaseCurrency(), 500L);

        final WebhookManualTransactionEventModel event = getManualTransactionWebhookResponse(timestamp, managedAccountId);

        assertManualTransactionEvent(managedAccountId, event);
    }

    @Test
    public void Webhooks_ManualTransactionToManagedCard_Success() {
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String consumerCurrency = createConsumerModel.getBaseCurrency();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        ConsumersHelper.verifyKyc(secretKey, consumer.getLeft());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel.DefaultCreatePrepaidManagedCardModel(consumerPrepaidManagedCardProfileId, consumerCurrency).build();
        final String managedCardId = ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, consumer.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        AdminHelper.fundManagedCard(innovatorId, managedCardId, consumerCurrency, 500L);

        final WebhookManualTransactionEventModel event = getManualTransactionWebhookResponse(timestamp, managedCardId);

        assertManualTransactionEvent(managedCardId, event);
    }

    @Test
    public void Webhooks_BeneficiaryIdSendTransaction_Success() {

        enableSendsSca();

        final CreateConsumerModel sourceConsumerModel = CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> sourceConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(sourceConsumerModel, secretKey);

        final CreateConsumerModel destinationConsumerModel = CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> destinationConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(destinationConsumerModel, secretKey);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, sourceConsumer.getRight());

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, destinationConsumer.getRight());

        final String destinationIdentityName =
                String.format("%s %s", destinationConsumerModel.getRootUser().getName(),
                        destinationConsumerModel.getRootUser().getSurname());

        final String beneficiaryId = BeneficiariesHelper.createInstrumentBeneficiaryInState(
                        BeneficiaryState.ACTIVE, IdentityType.CONSUMER, ManagedInstrumentType.MANAGED_ACCOUNTS,
                        destinationIdentityName, destinationManagedAccountId, secretKey, sourceConsumer.getRight())
                .getRight();

        TestHelper
                .simulateManagedAccountDeposit(sourceManagedAccountId, sourceConsumerModel.getBaseCurrency(), 10000L, secretKey, sourceConsumer.getRight());

        final Long depositAmount = 500L;

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(destinationConsumerModel.getBaseCurrency(), depositAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(beneficiaryId))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .build();

        final long timestamp = Instant.now().toEpochMilli();
        SendsService.sendFunds(sendFundsModel, secretKey, sourceConsumer.getRight(), Optional.empty()).then().statusCode(SC_OK);

        final BeneficiaryResponseModel beneficiary =
                BeneficiariesHelper.getBeneficiary(beneficiaryId, secretKey, sourceConsumer.getRight());

        final WebhookSendsEventModel sendEvent = getSendWebhookResponse(timestamp, sourceManagedAccountId);

        assertBeneficiaryIdSendEvent(beneficiary, sendFundsModel, sendEvent, destinationManagedAccountId, "COMPLETED", "COMPLETED", "TRUSTED_BENEFICIARY", "NO_ERROR");

        disableSendsSca();
    }

    @Test
    @DisplayName("Webhooks_BeneficiaryIdOutgoingWireTransfer_OwtSent - DEV-6378 opened to return return the correct address")
    public void Webhooks_BeneficiaryIdOutgoingWireTransfer_OwtSent() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        final long depositAmount = 10000L;

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporate.getRight());

        TestHelper
                .simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                        depositAmount, secretKey, corporate.getRight(), 1);

        final long timestamp = Instant.now().toEpochMilli();

        final String beneficiaryId = BeneficiariesHelper.createSEPABeneficiaryInState
                (BeneficiaryState.ACTIVE, IdentityType.CORPORATE, "Test", secretKey, corporate.getRight()).getRight();

        final BeneficiaryResponseModel beneficiary =
                BeneficiariesHelper.getBeneficiary(beneficiaryId, secretKey, corporate.getRight());

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.newBuilder()
                        .setProfileId(outgoingWireTransfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDescription(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
                        .setSourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setTransferAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), 100L))
                        .build();

        final Pair<String, OutgoingWireTransfersModel> owt =
                OutgoingWireTransfersHelper.sendOwt(outgoingWireTransfersModel, secretKey, corporate.getRight());

        final WebhookOwtEventModel event = getOwtWebhookResponse(timestamp, owt.getLeft());

        assertBeneficiaryIdOwtEvent(beneficiary, owt, event, "SUBMITTED", "SEPA");
    }

    @Test
    public void Webhooks_OutgoingWireTransfer_OwtSent() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

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
                OutgoingWireTransfersHelper.sendOwt(outgoingWireTransfersProfileId, managedAccountId,
                        new CurrencyAmount(createCorporateModel.getBaseCurrency(), owtAmount), secretKey, corporate.getRight());

        final WebhookOwtEventModel event = getOwtWebhookResponse(timestamp, owt.getLeft());

        assertOwtEvent(owt, event, "PENDING_CHALLENGE", "SEPA");
    }

    @Test
    public void Webhooks_OutgoingWireTransferFasterPayments_OwtSent() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.GBP.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

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
                OutgoingWireTransfersHelper.sendOwtFasterPayments(outgoingWireTransfersProfileId, managedAccountId,
                        new CurrencyAmount(createCorporateModel.getBaseCurrency(), owtAmount), secretKey, corporate.getRight());

        final WebhookOwtEventModel event = getOwtWebhookResponse(timestamp, owt.getLeft());

        assertOwtEvent(owt, event, "PENDING_CHALLENGE", "FASTER_PAYMENTS");
    }

    @Test
    public void Webhooks_OutgoingWireTransferOtp_OwtCompleted() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

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

        final List<WebhookOwtEventModel> events = getOwtWebhookResponses(timestamp, managedAccountId, 4);

        assertOwtEvent(owt, events, "PENDING_CHALLENGE", "SEPA");
        assertOwtEvent(owt, events, "SUBMITTED", "SEPA");
        assertOwtEvent(owt, events, "APPROVED", "SEPA");
        assertOwtEvent(owt, events, "COMPLETED", "SEPA");
    }

    @Test
    public void Webhooks_OutgoingWireTransferOtp_OwtChallengeExpired() throws InterruptedException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

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
                OutgoingWireTransfersHelper.sendOwtAndStartOtpChallenge(outgoingWireTransfersProfileId, managedAccountId,
                        new CurrencyAmount(createCorporateModel.getBaseCurrency(), owtAmount), secretKey, corporate.getRight());

        TimeUnit.SECONDS.sleep(70);

        final List<WebhookOwtEventModel> events = getOwtWebhookResponses(timestamp, managedAccountId, 2);

        assertOwtEvent(owt, events, "PENDING_CHALLENGE", "SEPA");
        assertOwtEvent(owt, events, "FAILED", "SEPA");
    }

    @Test
    public void Webhooks_BulkOutgoingWireTransfer_OwtSent() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        final long depositAmount = 10000L;
        final long owtAmount = 200L;

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporate.getRight());

        TestHelper
                .simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                        depositAmount, secretKey, corporate.getRight(), 1);

        final long timestamp = Instant.now().toEpochMilli();


        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId, createCorporateModel.getBaseCurrency(), owtAmount, OwtType.SEPA).build();

        OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
                        .outgoingWireTransfers(List.of(outgoingWireTransfersModel, outgoingWireTransfersModel))
                        .build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(200);

        final List<WebhookOwtEventModel> owtEvents = getOwtWebhookResponses(timestamp, managedAccountId, 2);

        final WebhookOwtEventModel pendingChallengeFirstOwtEvent =
                owtEvents.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);

        final WebhookOwtEventModel pendingChallengeSecondOwtEvent =
                owtEvents.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(1);

        final Pair<String, OutgoingWireTransfersModel> firstOwt = Pair.of(pendingChallengeFirstOwtEvent.getTransfer().getId().get("id"), outgoingWireTransfersModel);
        final Pair<String, OutgoingWireTransfersModel> secondOwt = Pair.of(pendingChallengeSecondOwtEvent.getTransfer().getId().get("id"), outgoingWireTransfersModel);

        assertOwtEvent(firstOwt, pendingChallengeFirstOwtEvent, "PENDING_CHALLENGE", "SEPA");
        assertOwtEvent(secondOwt, pendingChallengeSecondOwtEvent, "PENDING_CHALLENGE", "SEPA");
    }

    @Test
    public void Webhooks_BulkOutgoingWireTransferWithValidAndInvalid_OwtSent() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());
        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

        final long depositAmount = 10000L;
        final long owtAmount = 200L;

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporate.getRight());

        TestHelper
                .simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                        depositAmount, secretKey, corporate.getRight(), 1);

        final long timestamp = Instant.now().toEpochMilli();

        final OutgoingWireTransfersModel validOutgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId, createCorporateModel.getBaseCurrency(), owtAmount, OwtType.SEPA).build();

        final OutgoingWireTransfersModel invalidOutgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel("123",
                        managedAccountId, createCorporateModel.getBaseCurrency(), owtAmount, OwtType.SEPA).build();

        OutgoingWireTransfersService.sendBulkOutgoingWireTransfers(BulkOutgoingWireTransfersModel.builder()
                        .outgoingWireTransfers(List.of(validOutgoingWireTransfersModel, invalidOutgoingWireTransfersModel))
                        .build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(200);

        final List<WebhookOwtEventModel> owtEvents = getOwtWebhookResponses(timestamp, managedAccountId, 2);

        final WebhookOwtEventModel pendingChallengeOwtEvent =
                owtEvents.stream().filter(x -> x.getEventType().equals("PENDING_CHALLENGE")).collect(Collectors.toList()).get(0);

        final WebhookOwtEventModel invalidOwtEvent =
                owtEvents.stream().filter(x -> x.getEventType().equals("INVALID")).collect(Collectors.toList()).get(0);

        final Pair<String, OutgoingWireTransfersModel> validOwt = Pair.of(pendingChallengeOwtEvent.getTransfer().getId().get("id"), validOutgoingWireTransfersModel);
        final Pair<String, OutgoingWireTransfersModel> invalidOwt = Pair.of(invalidOwtEvent.getTransfer().getId().get("id"), invalidOutgoingWireTransfersModel);

        assertOwtEvent(validOwt, pendingChallengeOwtEvent, "PENDING_CHALLENGE", "SEPA");
        assertOwtEvent(invalidOwt, invalidOwtEvent, "INVALID", "SEPA");
    }

    @Test
    public void Webhooks_SendTransactionPendingAndResumed_Success() {

        final CreateConsumerModel sourceConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(Currency.GBP.name())
                .build();
        final Pair<String, String> sourceConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(sourceConsumerModel, secretKey);

        final CreateConsumerModel destinationConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(sourceConsumerModel.getBaseCurrency())
                .build();
        final Pair<String, String> destinationConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(destinationConsumerModel, secretKey);

        final String sourceManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, sourceConsumerModel.getBaseCurrency()).build(),
                        secretKey, sourceConsumer.getRight());

        final String destinationManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency()).build(),
                        secretKey, destinationConsumer.getRight());

        TestHelper
                .simulateManagedAccountDeposit(sourceManagedAccountId, sourceConsumerModel.getBaseCurrency(), 10000L, secretKey, sourceConsumer.getRight());

        AdminHelper.setConsumerVelocityLimit(new CurrencyAmount(destinationConsumerModel.getBaseCurrency(), 100L),
                Arrays.asList(new CurrencyAmount(Currency.EUR.name(), 2010L),
                        new CurrencyAmount(Currency.USD.name(), 2020L)),
                AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()), destinationConsumer.getLeft());

        final Long depositAmount = 500L;

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(sourceConsumerModel.getBaseCurrency(), depositAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, MANAGED_ACCOUNTS))
                        .build();

        final long timestamp = Instant.now().toEpochMilli();
        final String sendId =
                SendsService
                        .sendFunds(sendFundsModel, secretKey, sourceConsumer.getRight(), Optional.empty()).then()
                        .statusCode(SC_OK)
                        .body("state", equalTo("PENDING"))
                        .extract()
                        .jsonPath()
                        .get("id");

        AdminHelper.resumeSend(AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()), sendId);

        final List<WebhookSendsEventModel> sendEvents = getSendWebhookResponses(timestamp, sourceManagedAccountId, 2);
        final WebhookSendsEventModel pendingEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("PENDING")).collect(Collectors.toList()).get(0);
        final WebhookSendsEventModel completedEvent =
                sendEvents.stream().filter(x -> x.getEventType().equals("COMPLETED")).collect(Collectors.toList()).get(0);

        assertSendEvent(sendFundsModel, pendingEvent, "PENDING", "PENDING", "NOT_ENABLED", "NO_ERROR");
        assertSendEvent(sendFundsModel, completedEvent, "COMPLETED", "COMPLETED", "NOT_ENABLED", "NO_ERROR");
    }

    @Test
    public void Webhooks_DepositTransactionPendingAndResumed_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setBaseCurrency(Currency.USD.name())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(createCorporateModel.getBaseCurrency(), 100L),
                Arrays.asList(new CurrencyAmount(Currency.EUR.name(), 2010L),
                        new CurrencyAmount(Currency.GBP.name(), 2020L)),
                AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()), corporate.getLeft());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(
                        CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency())
                                .build(), secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

        final Long depositAmount = 1000L;
        final long timestamp = Instant.now().toEpochMilli();
        final SimulateDepositModel simulateDepositModel =
                SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(createCorporateModel.getBaseCurrency(), depositAmount));

        SimulatorService
                .simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        final WebhookManagedAccountDepositEventModel preResumeEvent = getManagedAccountsDepositWebhookResponse(timestamp, managedAccountId);

        final String adminTenantImpersonationToken = AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin());

        final String depositId =
                AdminHelper.getDeposits(adminTenantImpersonationToken, managedAccountId).jsonPath().get("entry[0].id");
        final long preResumeTimestamp = Instant.now().toEpochMilli();
        AdminHelper.resumeDeposit(adminTenantImpersonationToken, depositId);

        final WebhookManagedAccountDepositEventModel postResumeEvent = getManagedAccountsDepositWebhookResponse(preResumeTimestamp, managedAccountId);

        assertManagedAccountsDepositEvent(corporate.getLeft(), IdentityType.CORPORATE.getValue(), createCorporateModel.getRootUser().getEmail(),
                managedAccountId, createCorporateModel.getBaseCurrency(), simulateDepositModel.getDepositAmount().getAmount().intValue(),
                preResumeEvent, State.PENDING.name(), false);

        assertManagedAccountsDepositEvent(corporate.getLeft(), IdentityType.CORPORATE.getValue(), createCorporateModel.getRootUser().getEmail(),
                managedAccountId, createCorporateModel.getBaseCurrency(), simulateDepositModel.getDepositAmount().getAmount().intValue(),
                postResumeEvent, State.COMPLETED.name(), false);
    }

    @Test
    public void Webhooks_OctTransactionPendingAndResumed_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setBaseCurrency(Currency.USD.name())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        AdminHelper.setCorporateVelocityLimit(new CurrencyAmount(createCorporateModel.getBaseCurrency(), 100L),
                Arrays.asList(new CurrencyAmount(Currency.EUR.name(), 2010L),
                        new CurrencyAmount(Currency.GBP.name(), 2020L)),
                AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()), corporate.getLeft());

        final long octAmount = 4000L;

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(corporatePrepaidManagedCardProfileId, createCorporateModel.getBaseCurrency(),
                        secretKey, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        final String octId = SimulatorHelper.simulateOct(managedCardId, octAmount, corporate.getRight(),
                secretKey, createCorporateModel.getBaseCurrency());

        AdminHelper.resumeOct(AdminService.impersonateTenant(innovatorId, AdminService.loginAdmin()), octId);

        final List<WebhookSettlementEventModel> octEvents = getOctWebhookResponses(timestamp, octId, 2);
        final WebhookSettlementEventModel pendingEvent =
                octEvents.stream().filter(x -> x.getState().equals("PENDING")).collect(Collectors.toList()).get(0);
        final WebhookSettlementEventModel completedEvent =
                octEvents.stream().filter(x -> x.getState().equals("COMPLETED")).collect(Collectors.toList()).get(0);

        assertOctEvent(createCorporateModel.getBaseCurrency(), pendingEvent,
                0, (int) octAmount, octId, managedCardId, corporate.getLeft(), "PENDING");

        assertOctEvent(createCorporateModel.getBaseCurrency(), completedEvent,
                (int) octAmount, (int) octAmount, octId, managedCardId, corporate.getLeft(), "COMPLETED");
    }

    @Test
    public void Webhooks_ChargeFee_Success() {

        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);
        final String consumerId = consumer.getLeft();
        final String consumerCurrency = consumerDetails.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
        final String managedAccountId = ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, consumerCurrency).build(),
                        secretKey, consumer.getRight());

        TestHelper.simulateManagedAccountDeposit(managedAccountId, consumerCurrency,
                5000L, secretKey, consumer.getRight());

        final ChargeFeeModel chargeFeeModel =
                ChargeFeeModel.builder()
                        .setFeeType(AdminFeeType.DEPOSIT)
                        .setFeeSubType("PRINTED_STATEMENT")
                        .setNote(RandomStringUtils.randomAlphabetic(5))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setFeeSpec(FeeSpecModel.defaultFeeSpecModel(Collections.singletonList(new CurrencyAmount(consumerCurrency, 100L)))).build();

        final long timestamp = Instant.now().toEpochMilli();
        AdminFeesService.chargeFee(chargeFeeModel, adminImpersonatedTenantToken)
                .then()
                .statusCode(SC_OK);

        final WebhookChargeFeesEventModel event = getChargeFeesWebhookResponse(timestamp, managedAccountId);

        assertChargeFeesEvent(managedAccountId, "COMPLETED", "DEPOSIT", event);
    }

    @Test
    public void Webhooks_DeactivateActivateCorporate_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final long deactivateTimestamp = Instant.now().toEpochMilli();
        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(true, "TEMPORARY"),
                corporate.getLeft(), innovatorToken);

        final long activateTimestamp = Instant.now().toEpochMilli();
        AdminHelper.activateCorporate(new ActivateIdentityModel(true),
                corporate.getLeft(), adminImpersonatedTenantToken);

        final WebhookIdentityDeactivationModel deactivatedEvent = getIdentityDeactivatedWebhookResponse(deactivateTimestamp, createCorporateModel.getRootUser().getEmail(),
                ApiSchemaDefinition.CorporateDeactivatedEvent, WebhookType.CORPORATES_DEACTIVATED);
        final WebhookIdentityActivationModel activatedEvent = getIdentityActivatedWebhookResponse(activateTimestamp, createCorporateModel.getRootUser().getEmail(),
                ApiSchemaDefinition.CorporateActivatedEvent, WebhookType.CORPORATES_ACTIVATED);

        assertIdentityDeactivatedEvent("TEMPORARY", createCorporateModel.getRootUser().getEmail(), deactivatedEvent);
        assertIdentityActivatedEvent(createCorporateModel.getRootUser().getEmail(), activatedEvent);
    }

    @Test
    public void Webhooks_DeactivateActivateConsumer_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final long deactivateTimestamp = Instant.now().toEpochMilli();
        InnovatorHelper.deactivateConsumer(new DeactivateIdentityModel(true, "TEMPORARY"),
                consumer.getLeft(), innovatorToken);

        final long activateTimestamp = Instant.now().toEpochMilli();
        AdminHelper.activateConsumer(new ActivateIdentityModel(true),
                consumer.getLeft(), adminImpersonatedTenantToken);

        final WebhookIdentityDeactivationModel deactivatedEvent = getIdentityDeactivatedWebhookResponse(deactivateTimestamp, createConsumerModel.getRootUser().getEmail(),
                ApiSchemaDefinition.ConsumerDeactivatedEvent, WebhookType.CONSUMERS_DEACTIVATED);
        final WebhookIdentityActivationModel activatedEvent = getIdentityActivatedWebhookResponse(activateTimestamp, createConsumerModel.getRootUser().getEmail(),
                ApiSchemaDefinition.ConsumerActivatedEvent, WebhookType.CONSUMERS_ACTIVATED);

        assertIdentityDeactivatedEvent("TEMPORARY", createConsumerModel.getRootUser().getEmail(), deactivatedEvent);
        assertIdentityActivatedEvent(createConsumerModel.getRootUser().getEmail(), activatedEvent);
    }

    private void assertChargeFeesEvent(final String managedAccountId,
                                       final String status,
                                       final String feeType,
                                       final WebhookChargeFeesEventModel chargeFeesEvent) {

        assertEquals(managedAccountId, chargeFeesEvent.getChargeFee().getSource().get("id"));
        assertEquals(status, chargeFeesEvent.getType());
        assertNotNull(chargeFeesEvent.getPublishedTimestamp());
        assertNotNull(chargeFeesEvent.getChargeFee().getFeeSpec());
        assertNotNull(chargeFeesEvent.getChargeFee().getId());
        assertEquals(feeType, chargeFeesEvent.getChargeFee().getFeeType());
    }

    private WebhookChargeFeesEventModel getChargeFeesWebhookResponse(final long timestamp,
                                                                     final String managedAccountId) {
        return (WebhookChargeFeesEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.CHARGE_FEES,
                Pair.of("chargeFee.source.id", managedAccountId),
                WebhookChargeFeesEventModel.class,
                ApiSchemaDefinition.ChargeFeeEvent);
    }

    private void assertOwtEvent(final Pair<String, OutgoingWireTransfersModel> owt,
                                final List<WebhookOwtEventModel> owtEvents,
                                final String state,
                                final String payment) {

        final WebhookOwtEventModel owtEvent =
                owtEvents.stream().filter(x -> x.getEventType().equals(state)).collect(Collectors.toList()).get(0);

        assertOwtEvent(owt, owtEvent, state, payment);
    }

    private WebhookOwtEventModel getOwtWebhookResponse(final long timestamp,
                                                       final String transferId) {
        return (WebhookOwtEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.OWT,
                Pair.of("transfer.id.id", transferId),
                WebhookOwtEventModel.class,
                ApiSchemaDefinition.OutgoingWireTransferEventV3);
    }

    //    filter by source instrument because we have bulk OWTs and can't have owtId
    private List<WebhookOwtEventModel> getOwtWebhookResponses(final long timestamp,
                                                              final String sourceInstrumentId,
                                                              final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.OWT,
                Pair.of("transfer.source.id", sourceInstrumentId),
                WebhookOwtEventModel.class,
                ApiSchemaDefinition.OutgoingWireTransferEventV3,
                expectedEventCount);
    }

    private void assertOwtEvent(final Pair<String, OutgoingWireTransfersModel> owt,
                                final WebhookOwtEventModel owtEvent,
                                final String state,
                                final String payment) {

        assertEquals(state, owtEvent.getEventType());
        assertNotNull(owtEvent.getPublishedTimestamp());
        assertNotNull(owtEvent.getTransfer().getCreationTimestamp());
        assertEquals(owt.getRight().getDescription(), owtEvent.getTransfer().getDescription());
        assertEquals(owt.getRight().getDestinationBeneficiary().getAddress(), owtEvent.getTransfer().getDestination().getBeneficiaryAddress());
        assertEquals(owt.getRight().getDestinationBeneficiary().getBankAddress(), owtEvent.getTransfer().getDestination().getBeneficiaryBankAddress());
        assertEquals(owt.getRight().getDestinationBeneficiary().getBankCountry(), owtEvent.getTransfer().getDestination().getBeneficiaryBankCountry());
        assertEquals(owt.getRight().getDestinationBeneficiary().getBankName(), owtEvent.getTransfer().getDestination().getBeneficiaryBankName());
        assertEquals(owt.getRight().getDestinationBeneficiary().getName(), owtEvent.getTransfer().getDestination().getBeneficiaryName());
        assertEquals(owt.getLeft(), owtEvent.getTransfer().getId().get("id"));
        assertEquals("outgoing_wire_transfers", owtEvent.getTransfer().getId().get("type"));
        assertEquals(owt.getRight().getProfileId(), owtEvent.getTransfer().getProfileId());
        assertEquals(owt.getRight().getSourceInstrument().getId(), owtEvent.getTransfer().getSource().get("id"));
        assertEquals(owt.getRight().getSourceInstrument().getType(), owtEvent.getTransfer().getSource().get("type"));
        assertEquals(state, owtEvent.getTransfer().getState());
        assertEquals(owt.getRight().getTag(), owtEvent.getTransfer().getTag());
        assertEquals(owt.getRight().getTransferAmount().getCurrency(), owtEvent.getTransfer().getTransactionAmount().get("currency"));
        assertEquals(owt.getRight().getTransferAmount().getAmount(), Long.parseLong(owtEvent.getTransfer().getTransactionAmount().get("amount")));
        if (!state.equals("INVALID")) {
            assertEquals(owt.getRight().getTransferAmount().getCurrency(), owtEvent.getTransfer().getTransactionFee().get("currency"));
        }
        assertEquals(owt.getRight().getTransferAmount().getCurrency(), owtEvent.getTransfer().getTransferAmount().get("currency"));
        assertEquals(owt.getRight().getTransferAmount().getAmount(), Long.parseLong(owtEvent.getTransfer().getTransferAmount().get("amount")));
        assertEquals(payment, owtEvent.getTransfer().getType());
        assertEquals("", owtEvent.getTransfer().getBeneficiaryId());
        assertEquals("NO_EXEMPTION", owtEvent.getTransfer().getChallengeExemptionReason());

        if (payment.equals("SEPA")) {
            final SepaBankDetailsModel sepaBankDetails =
                    (SepaBankDetailsModel) owt.getRight().getDestinationBeneficiary().getBankAccountDetails();

            assertEquals(sepaBankDetails.getIban(), owtEvent.getTransfer().getDestination().getSepa().get("iban"));
            assertEquals(sepaBankDetails.getBankIdentifierCode(), owtEvent.getTransfer().getDestination().getSepa().get("bankIdentifierCode"));
            if (!state.equals("INVALID")) {
                assertEquals(TestHelper.getFees(owt.getRight().getTransferAmount().getCurrency()).get(FeeType.SEPA_OWT_FEE).getAmount(),
                        Long.parseLong(owtEvent.getTransfer().getTransactionFee().get("amount")));
            }

        } else {
            final FasterPaymentsBankDetailsModel fasterPaymentsBankDetails =
                    (FasterPaymentsBankDetailsModel) owt.getRight().getDestinationBeneficiary().getBankAccountDetails();

            assertEquals(fasterPaymentsBankDetails.getAccountNumber(), owtEvent.getTransfer().getDestination().getFasterPayments().get("accountNumber"));
            assertEquals(fasterPaymentsBankDetails.getSortCode(), owtEvent.getTransfer().getDestination().getFasterPayments().get("sortCode"));
            if (!state.equals("INVALID")) {
                assertEquals(TestHelper.getFees(owt.getRight().getTransferAmount().getCurrency()).get(FeeType.FASTER_PAYMENTS_OWT_FEE).getAmount(),
                        Long.parseLong(owtEvent.getTransfer().getTransactionFee().get("amount")));
            }
        }
    }

    private void assertBeneficiaryIdOwtEvent(final BeneficiaryResponseModel beneficiary,
                                             final Pair<String, OutgoingWireTransfersModel> owt,
                                             final WebhookOwtEventModel owtEvent,
                                             final String state,
                                             final String payment) {

        assertEquals(state, owtEvent.getEventType());
        assertNotNull(owtEvent.getPublishedTimestamp());
        assertNotNull(owtEvent.getTransfer().getCreationTimestamp());
        assertEquals(owt.getRight().getDescription(), owtEvent.getTransfer().getDescription());
        assertEquals(beneficiary.getBeneficiaryDetails().getAddress(), owtEvent.getTransfer().getDestination().getBeneficiaryAddress());
        assertEquals(beneficiary.getBeneficiaryDetails().getAddress(), owtEvent.getTransfer().getDestination().getBeneficiaryBankAddress());
        assertEquals(beneficiary.getBeneficiaryDetails().getBankCountry(), owtEvent.getTransfer().getDestination().getBeneficiaryBankCountry());
        assertEquals(beneficiary.getBeneficiaryDetails().getBankName(), owtEvent.getTransfer().getDestination().getBeneficiaryBankName());
        assertEquals(beneficiary.getBeneficiaryInformation().getBusinessName(), owtEvent.getTransfer().getDestination().getBeneficiaryName());
        assertEquals(owt.getLeft(), owtEvent.getTransfer().getId().get("id"));
        assertEquals("outgoing_wire_transfers", owtEvent.getTransfer().getId().get("type"));
        assertEquals(owt.getRight().getProfileId(), owtEvent.getTransfer().getProfileId());
        assertEquals(owt.getRight().getSourceInstrument().getId(), owtEvent.getTransfer().getSource().get("id"));
        assertEquals(owt.getRight().getSourceInstrument().getType(), owtEvent.getTransfer().getSource().get("type"));
        assertEquals(state, owtEvent.getTransfer().getState());
        assertEquals(owt.getRight().getTag(), owtEvent.getTransfer().getTag());
        assertEquals(owt.getRight().getTransferAmount().getCurrency(), owtEvent.getTransfer().getTransactionAmount().get("currency"));
        assertEquals(owt.getRight().getTransferAmount().getAmount(), Long.parseLong(owtEvent.getTransfer().getTransactionAmount().get("amount")));
        if (!state.equals("INVALID")) {
            assertEquals(owt.getRight().getTransferAmount().getCurrency(), owtEvent.getTransfer().getTransactionFee().get("currency"));
        }
        assertEquals(owt.getRight().getTransferAmount().getCurrency(), owtEvent.getTransfer().getTransferAmount().get("currency"));
        assertEquals(owt.getRight().getTransferAmount().getAmount(), Long.parseLong(owtEvent.getTransfer().getTransferAmount().get("amount")));
        assertEquals(payment, owtEvent.getTransfer().getType());
        assertEquals(beneficiary.getId(), owtEvent.getTransfer().getBeneficiaryId());
        assertEquals("TRUSTED_BENEFICIARY", owtEvent.getTransfer().getChallengeExemptionReason());


            assertEquals(beneficiary.getBeneficiaryDetails().getBankAccountDetails().getIban(), owtEvent.getTransfer().getDestination().getSepa().get("iban"));
            assertEquals(beneficiary.getBeneficiaryDetails().getBankAccountDetails().getBankIdentifierCode(), owtEvent.getTransfer().getDestination().getSepa().get("bankIdentifierCode"));
            if (!state.equals("INVALID")) {
                assertEquals(TestHelper.getFees(owt.getRight().getTransferAmount().getCurrency()).get(FeeType.SEPA_OWT_FEE).getAmount(),
                        Long.parseLong(owtEvent.getTransfer().getTransactionFee().get("amount")));
            }
    }

    private void assertSendEvent(final SendFundsModel sendFundsModel,
                                 final WebhookSendsEventModel sendEvent,
                                 final String eventType,
                                 final String state,
                                 final String exemptionReason,
                                 final String conflict) {

        assertEquals(eventType, sendEvent.getEventType());
        assertNotNull(sendEvent.getPublishedTimestamp());
        assertNotNull(sendEvent.getSend().getId().get("id"));
        assertEquals("send", sendEvent.getSend().getId().get("type"));
        assertEquals(sendFundsModel.getProfileId(), sendEvent.getSend().getProfileId());
        assertEquals(sendFundsModel.getTag(), sendEvent.getSend().getTag());
        assertEquals(sendFundsModel.getSource().getId(), sendEvent.getSend().getSource().get("id"));
        assertEquals("managed_accounts", sendEvent.getSend().getSource().get("type"));
        assertEquals(sendFundsModel.getDestination().getId(), sendEvent.getSend().getDestination().get("id"));
        assertEquals("managed_accounts", sendEvent.getSend().getDestination().get("type"));
        assertEquals(sendFundsModel.getDestinationAmount().getCurrency(), sendEvent.getSend().getDestinationAmount().get("currency"));
        assertEquals(sendFundsModel.getDestinationAmount().getAmount(), Long.parseLong(sendEvent.getSend().getDestinationAmount().get("amount")));
        assertEquals(state, sendEvent.getSend().getState());
        assertNotNull(sendEvent.getSend().getCreationTimestamp());
        assertEquals(conflict, sendEvent.getSend().getConflict());
        assertEquals(exemptionReason, sendEvent.getSend().getChallengeExemptionReason());
        assertEquals("", sendEvent.getSend().getBeneficiaryId());
    }

    private void assertBeneficiaryIdSendEvent(final BeneficiaryResponseModel beneficiary,
                                              final SendFundsModel sendFundsModel,
                                              final WebhookSendsEventModel sendEvent,
                                              final String destinationInstrumentId,
                                              final String eventType,
                                              final String state,
                                              final String exemptionReason,
                                              final String conflict) {

        assertEquals(eventType, sendEvent.getEventType());
        assertNotNull(sendEvent.getPublishedTimestamp());
        assertNotNull(sendEvent.getSend().getId().get("id"));
        assertEquals("send", sendEvent.getSend().getId().get("type"));
        assertEquals(sendFundsModel.getProfileId(), sendEvent.getSend().getProfileId());
        assertEquals(sendFundsModel.getTag(), sendEvent.getSend().getTag());
        assertEquals(sendFundsModel.getSource().getId(), sendEvent.getSend().getSource().get("id"));
        assertEquals("managed_accounts", sendEvent.getSend().getSource().get("type"));
        assertEquals(destinationInstrumentId, sendEvent.getSend().getDestination().get("id"));
        assertEquals("managed_accounts", sendEvent.getSend().getDestination().get("type"));
        assertEquals(sendFundsModel.getDestinationAmount().getCurrency(), sendEvent.getSend().getDestinationAmount().get("currency"));
        assertEquals(sendFundsModel.getDestinationAmount().getAmount(), Long.parseLong(sendEvent.getSend().getDestinationAmount().get("amount")));
        assertEquals(state, sendEvent.getSend().getState());
        assertNotNull(sendEvent.getSend().getCreationTimestamp());
        assertEquals(conflict, sendEvent.getSend().getConflict());
        assertEquals(exemptionReason, sendEvent.getSend().getChallengeExemptionReason());
        assertEquals(beneficiary.getId(), sendEvent.getSend().getBeneficiaryId());
    }

    private WebhookSendsEventModel getSendWebhookResponse(final long timestamp,
                                                          final String sourceId) {
        return (WebhookSendsEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.SENDS,
                Pair.of("send.source.id", sourceId),
                WebhookSendsEventModel.class,
                ApiSchemaDefinition.SendEvent);
    }

    private List<WebhookSendsEventModel> getSendWebhookResponses(final long timestamp,
                                                                 final String sourceId,
                                                                 final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.SENDS,
                Pair.of("send.source.id", sourceId),
                WebhookSendsEventModel.class,
                ApiSchemaDefinition.SendEvent,
                expectedEventCount);
    }

    private WebhookManagedAccountDepositEventModel getManagedAccountsDepositWebhookResponse(final long timestamp,
                                                                                            final String managedAccountId) {
        return (WebhookManagedAccountDepositEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.MANAGED_ACCOUNT_DEPOSIT,
                Pair.of("id.id", managedAccountId),
                WebhookManagedAccountDepositEventModel.class,
                ApiSchemaDefinition.ManagedAccountsDepositEvent);
    }

    private void assertManagedAccountsDepositEvent(final String identityId,
                                                   final String identityType,
                                                   final String emailAddress,
                                                   final String managedAccountId,
                                                   final String currency,
                                                   final int depositAmount,
                                                   final WebhookManagedAccountDepositEventModel depositEvent,
                                                   final String state,
                                                   final boolean isPendingDeposit ) {

        assertEquals(managedAccountId, depositEvent.getId().get("id"));
        assertEquals("managed_accounts", depositEvent.getId().get("type"));
        assertEquals(identityId, depositEvent.getOwner().get("id"));
        assertEquals(identityType, depositEvent.getOwner().get("type"));
        assertNotNull(depositEvent.getTransactionId());
        assertEquals(currency, depositEvent.getTransactionAmount().get("currency"));
        assertEquals(depositAmount, Integer.parseInt(depositEvent.getTransactionAmount().get("amount")));
        assertNotNull(depositEvent.getTransactionTimestamp());
        assertEquals(emailAddress, depositEvent.getEmailAddress());
        assertEquals(isPendingDeposit? "SenderTest" : "Sender Test", depositEvent.getSenderName());
        assertEquals("RefTest123", depositEvent.getSenderReference());
        assertTrue(StringUtils.isNotEmpty(depositEvent.getSenderIban()));
        assertEquals(state, depositEvent.getState());
    }

    private WebhookKybEventModel getKybWebhookResponse(final long timestamp,
                                                       final String identityId) {
        return (WebhookKybEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.CORPORATE_KYB,
                Pair.of("corporateId", identityId),
                WebhookKybEventModel.class,
                ApiSchemaDefinition.KybEvent);
    }

    private void assertKybEvent(final String identityId,
                                final String identityEmail,
                                final WebhookKybEventModel kybEvent,
                                final KybState kybState,
                                final Optional<List<String>> details,
                                final Optional<String> rejectionComment) {

        assertEquals(identityEmail, kybEvent.getCorporateEmail());
        assertEquals(identityId, kybEvent.getCorporateId());
        assertEquals(details.orElse(new ArrayList<>()).size(),
                Arrays.asList(kybEvent.getDetails()).size());
        assertEquals(rejectionComment.orElse(""), kybEvent.getRejectionComment());
        assertEquals(kybState.name(), kybEvent.getStatus());
        assertEquals(kybState.name(), kybEvent.getOngoingStatus());

        if (details.isPresent() && details.get().size() > 0) {
            details.get().forEach(eventDetail ->
                    assertEquals(eventDetail,
                            Arrays.stream(kybEvent.getDetails())
                                    .filter(x -> x.equals(eventDetail))
                                    .findFirst()
                                    .orElse(String.format("Details did not match. Expected %s Actual %s",
                                            eventDetail, Arrays.asList(kybEvent.getDetails()).size() == 0 ? "[No details returned from sumsub]" :
                                                    String.join(", ", kybEvent.getDetails())))));
        }
    }

    private WebhookKycEventModel getKycWebhookResponse(final long timestamp,
                                                       final String identityId) {
        return (WebhookKycEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.CONSUMER_KYC,
                Pair.of("consumerId", identityId),
                WebhookKycEventModel.class,
                ApiSchemaDefinition.KycEvent);
    }

    private void assertKycEvent(final String consumerId,
                                final String consumerEmail,
                                final WebhookKycEventModel event,
                                final KycState kycState,
                                final KycState ongoingKycState,
                                final KycLevel kycLevel,
                                final KycLevel ongoingKycLevel,
                                final Optional<List<String>> details,
                                final Optional<String> rejectionComment) {

        assertEquals(consumerId, event.getConsumerId());
        assertEquals(consumerEmail, event.getConsumerEmail());
        assertEquals(details.orElse(new ArrayList<>()).size(),
                Arrays.asList(event.getDetails()).size());
        assertEquals(rejectionComment.orElse(""), event.getRejectionComment());
        assertEquals(kycLevel.name(), event.getKycLevel());
        assertEquals(ongoingKycLevel.name(), event.getOngoingKycLevel());
        assertEquals(kycState.name(), event.getStatus());
        assertEquals(ongoingKycState.name(), event.getOngoingStatus());
        assertNotNull(event.getEventTimestamp());

        if (details.isPresent() && details.get().size() > 0) {
            details.get().forEach(eventDetail ->
                    assertEquals(eventDetail,
                            Arrays.stream(event.getDetails())
                                    .filter(x -> x.equals(eventDetail))
                                    .findFirst()
                                    .orElse(String.format("Details did not match. Expected %s Actual %s",
                                            eventDetail, Arrays.asList(event.getDetails()).size() == 0 ? "[No details returned from sumsub]" :
                                                    String.join(", ", event.getDetails())))));
        }
    }

    private WebhookTransfersEventModel getTransfersWebhookResponse(final long timestamp,
                                                                   final String sourceManagedAccountId) {
        return (WebhookTransfersEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.TRANSFERS,
                Pair.of("transfer.source.id", sourceManagedAccountId),
                WebhookTransfersEventModel.class,
                ApiSchemaDefinition.TransferEvent);
    }

    private void assertTransfersEvent(final TransferFundsModel transferFundsModel,
                                      final String sourceManagedAccountId,
                                      final ManagedInstrumentType sourceInstrumentType,
                                      final String destinationManagedAccountId,
                                      final ManagedInstrumentType destinationInstrumentType,
                                      final String currency,
                                      final Long depositAmount,
                                      final State state,
                                      final String conflict,
                                      final String eventType,
                                      final WebhookTransfersEventModel transfersEvent) {

        assertEquals(eventType, transfersEvent.getEventType());
        assertNotNull(transfersEvent.getPublishedTimestamp());
        assertNotNull(transfersEvent.getTransfer().getId().get("id"));
        assertEquals("transfers", transfersEvent.getTransfer().getId().get("type"));
        assertEquals(transferFundsModel.getProfileId(), transfersEvent.getTransfer().getProfileId());
        assertEquals(transferFundsModel.getTag(), transfersEvent.getTransfer().getTag());
        assertEquals(sourceManagedAccountId, transfersEvent.getTransfer().getSource().get("id"));
        assertEquals(sourceInstrumentType.getValue(), transfersEvent.getTransfer().getSource().get("type"));
        assertEquals(destinationManagedAccountId, transfersEvent.getTransfer().getDestination().get("id"));
        assertEquals(destinationInstrumentType.getValue(), transfersEvent.getTransfer().getDestination().get("type"));
        assertEquals(currency, transfersEvent.getTransfer().getDestinationAmount().get("currency"));
        assertEquals(depositAmount, Long.parseLong(transfersEvent.getTransfer().getDestinationAmount().get("amount")));
        assertEquals(state.name(), transfersEvent.getTransfer().getState());
        assertNotNull(transfersEvent.getTransfer().getCreationTimestamp());
        assertEquals(conflict, transfersEvent.getTransfer().getConflict());
    }

    private WebhookManualTransactionEventModel getManualTransactionWebhookResponse(final long timestamp,
                                                                                   final String targetInstrumentId) {
        return (WebhookManualTransactionEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.MANUAL_TRANSACTION,
                Pair.of("targetInstrument.id", targetInstrumentId),
                WebhookManualTransactionEventModel.class,
                ApiSchemaDefinition.ManualTransactionEvent);
    }

    private void assertManualTransactionEvent(final String instrumentId,
                                              final WebhookManualTransactionEventModel manualTransactionEvent) {

        assertNotNull(manualTransactionEvent.getTransactionTimestamp());
        assertNotNull(manualTransactionEvent.getTransactionId());
        assertEquals(instrumentId, manualTransactionEvent.getTargetInstrument().get("id"));
        assertNotNull(manualTransactionEvent.getTargetInstrument().get("type"));
        assertNotNull(manualTransactionEvent.getActualBalanceAdjustment());
        assertNotNull(manualTransactionEvent.getAvailableBalanceAdjustment());
    }

    private List<WebhookSettlementEventModel> getOctWebhookResponses(final long timestamp,
                                                                     final String octId,
                                                                     final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.SETTLEMENT,
                Pair.of("transactionId", octId),
                WebhookSettlementEventModel.class,
                ApiSchemaDefinition.ManagedCardsSettlementEvent,
                expectedEventCount);
    }

    private void assertOctEvent(final String currency,
                                final WebhookSettlementEventModel octEvent,
                                final int availableBalance,
                                final int octAmount,
                                final String octId,
                                final String managedCardId,
                                final String identityId,
                                final String state) {

        assertEquals(currency, octEvent.getAvailableBalance().get("currency"));
        assertEquals(availableBalance, Integer.parseInt(octEvent.getAvailableBalance().get("amount")));
        assertEquals(currency, octEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals(availableBalance, Integer.parseInt(octEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(currency, octEvent.getFeeAmount().get("currency"));
        assertEquals(0, Integer.parseInt(octEvent.getFeeAmount().get("amount")));
        assertEquals(managedCardId, octEvent.getId().get("id"));
        assertEquals("managed_cards", octEvent.getId().get("type"));
        assertEquals("5399", octEvent.getMerchantCategoryCode());
        assertEquals("BetFair Winnings Division", octEvent.getMerchantName());
        assertEquals("MCHT1234", octEvent.getMerchantId());
        assertEquals(identityId, octEvent.getOwner().get("id"));
        assertEquals("corporates", octEvent.getOwner().get("type"));
        assertEquals("0", octEvent.getRelatedAuthorisationId());
        assertEquals("ORIGINAL_CREDIT_TRANSACTION", octEvent.getSettlementType());
        assertEquals(currency, octEvent.getSourceAmount().get("currency"));
        assertEquals(octAmount, Integer.parseInt(octEvent.getSourceAmount().get("amount")));
        assertEquals(currency, octEvent.getTransactionAmount().get("currency"));
        assertEquals(octAmount, Integer.parseInt(octEvent.getTransactionAmount().get("amount")));
        assertEquals(octId, octEvent.getTransactionId());
        assertNotNull(octEvent.getTransactionTimestamp());
        assertEquals(state, octEvent.getState());
    }

    private WebhookIdentityDeactivationModel getIdentityDeactivatedWebhookResponse(final long timestamp,
                                                                                   final String email,
                                                                                   final ApiSchemaDefinition definition,
                                                                                   final WebhookType webhookType) {
        return (WebhookIdentityDeactivationModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                webhookType,
                Pair.of("emailAddress", email),
                WebhookIdentityDeactivationModel.class,
                definition);
    }

    private WebhookIdentityActivationModel getIdentityActivatedWebhookResponse(final long timestamp,
                                                                               final String email,
                                                                               final ApiSchemaDefinition definition,
                                                                               final WebhookType webhookType) {
        return (WebhookIdentityActivationModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                webhookType,
                Pair.of("emailAddress", email),
                WebhookIdentityActivationModel.class,
                definition);
    }

    private void assertIdentityDeactivatedEvent(final String reasonCode,
                                                final String email,
                                                final WebhookIdentityDeactivationModel deactivatedEvent) {

        assertEquals("INNOVATOR", deactivatedEvent.getActionDoneBy());
        assertEquals(email, deactivatedEvent.getEmailAddress());
        assertEquals(reasonCode, deactivatedEvent.getReasonCode());
    }

    private void assertIdentityActivatedEvent(final String email,
                                              final WebhookIdentityActivationModel activatedEvent) {

        assertEquals("ADMIN", activatedEvent.getActionDoneBy());
        assertEquals(email, activatedEvent.getEmailAddress());
    }

    private static void enableSendsSca() {
        final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setScaConfig(new ScaConfigModel(true, false))
                        .build();
        InnovatorHelper.enableSendsSca(updateProgrammeModel, programmeId, innovatorToken);
    }

    private static void disableSendsSca() {
        final String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setScaConfig(new ScaConfigModel(false, false))
                        .build();
        InnovatorHelper.disableSendsSca(updateProgrammeModel, programmeId, innovatorToken);
    }

    protected String getManagedAccountIbanId(final String managedAccountId) throws SQLException {
        return  ManagedAccountsDatabaseHelper.getIbanByManagedAccount(managedAccountId).get(0).get("iban_id");
    }
}
