package opc.junit.backoffice.fees;

import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.IdentityType;
import opc.enums.opc.WebhookType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.shared.FeeSourceModel;
import opc.models.shared.FeesChargeModel;
import opc.models.webhook.WebhookChargeFeesEventModel;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public class WebhookTests extends BaseIdentitySetup {

    @BeforeAll
    public static void BeforeAll() {
        InnovatorHelper.enableWebhook(UpdateProgrammeModel.WebHookUrlSetup(programmeId, false, webhookServiceDetails.getRight()),
                programmeId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));
    }

    @Test
    public void Webhooks_ChargeConsumerFee_Success() {

        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);
        final String consumerId = consumer.getLeft();
        final String consumerCurrency = consumerDetails.getBaseCurrency();
        final String consumerImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(consumerId, IdentityType.CONSUMER, secretKey);

        ConsumersHelper.verifyKyc(secretKey, consumerId);
        final String managedAccountId = ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, consumerCurrency).build(),
                        secretKey, consumer.getRight());

        TestHelper.simulateManagedAccountDeposit(managedAccountId, consumerCurrency,
                5000L, secretKey, consumer.getRight());

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final long timestamp = Instant.now().toEpochMilli();
        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final WebhookChargeFeesEventModel event = getWebhookResponse(timestamp, managedAccountId);

        assertChargeFeeEvent(managedAccountId, "COMPLETED", "CUSTOM", event);
    }

    @Test
    public void Webhooks_ChargeCorporateFee_Success() {

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
        final String corporateId = corporate.getLeft();
        final String corporateCurrency = corporateDetails.getBaseCurrency();
        final String corporateImpersonateToken = BackofficeHelper.impersonateIdentityAccessToken(corporateId, IdentityType.CORPORATE, secretKey);

        CorporatesHelper.verifyKyb(secretKey, corporateId);
        final String managedAccountId = ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, corporateCurrency).build(),
                        secretKey, corporate.getRight());

        TestHelper.simulateManagedAccountDeposit(managedAccountId, corporateCurrency,
                5000L, secretKey, corporate.getRight());

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final long timestamp = Instant.now().toEpochMilli();
        BackofficeMultiService.chargeFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final WebhookChargeFeesEventModel event = getWebhookResponse(timestamp, managedAccountId);

        assertChargeFeeEvent(managedAccountId, "COMPLETED", "CUSTOM", event);
    }

    @Test
    public void Webhooks_ChargeConsumerFee_DeprecatedEndpoint_Success() {

        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);
        final String consumerId = consumer.getLeft();
        final String consumerCurrency = consumerDetails.getBaseCurrency();
        final String consumerImpersonateToken = BackofficeHelper.impersonateIdentity(consumerId, IdentityType.CONSUMER, secretKey);

        ConsumersHelper.verifyKyc(secretKey, consumerId);
        final String managedAccountId = ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(consumerManagedAccountProfileId, consumerCurrency).build(),
                        secretKey, consumer.getRight());

        TestHelper.simulateManagedAccountDeposit(managedAccountId, consumerCurrency,
                5000L, secretKey, consumer.getRight());

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final long timestamp = Instant.now().toEpochMilli();
        BackofficeMultiService.chargeConsumerFee(feesChargeModel, secretKey, consumerImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final WebhookChargeFeesEventModel event = getWebhookResponse(timestamp, managedAccountId);

        assertChargeFeeEvent(managedAccountId, "COMPLETED", "CUSTOM", event);
    }

    @Test
    public void Webhooks_ChargeCorporateFee_DeprecatedEndpoint_Success() {

        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
        final String corporateId = corporate.getLeft();
        final String corporateCurrency = corporateDetails.getBaseCurrency();
        final String corporateImpersonateToken = BackofficeHelper.impersonateIdentity(corporateId, IdentityType.CORPORATE, secretKey);


        CorporatesHelper.verifyKyb(secretKey, corporateId);
        final String managedAccountId = ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, corporateCurrency).build(),
                        secretKey, corporate.getRight());

        TestHelper.simulateManagedAccountDeposit(managedAccountId, corporateCurrency,
                5000L, secretKey, corporate.getRight());

        final FeesChargeModel feesChargeModel =
                new FeesChargeModel("PRINTED_CARD_ACCOUNT_STATEMENT",
                        new FeeSourceModel("managed_accounts", managedAccountId));

        final long timestamp = Instant.now().toEpochMilli();
        BackofficeMultiService.chargeCorporateFee(feesChargeModel, secretKey, corporateImpersonateToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        final WebhookChargeFeesEventModel event = getWebhookResponse(timestamp, managedAccountId);

        assertChargeFeeEvent(managedAccountId, "COMPLETED", "CUSTOM", event);
    }

    private void assertChargeFeeEvent(final String managedAccountId,
                                      final String status,
                                      final String feeType,
                                      final WebhookChargeFeesEventModel event) {

        assertEquals(managedAccountId, event.getChargeFee().getSource().get("id"));
        assertEquals("managed_accounts", event.getChargeFee().getSource().get("type"));
        assertEquals(status, event.getType());
        assertNotNull(event.getPublishedTimestamp());
        assertNotNull(event.getChargeFee().getFeeSpec());
        assertNotNull(event.getChargeFee().getId());
        assertEquals(feeType, event.getChargeFee().getFeeType());
    }

    private WebhookChargeFeesEventModel getWebhookResponse(final long timestamp,
                                                           final String instrumentId) {
        return (WebhookChargeFeesEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.CHARGE_FEES,
                Pair.of("chargeFee.source.id", instrumentId),
                WebhookChargeFeesEventModel.class,
                ApiSchemaDefinition.ChargeFeeEvent);
    }
}
