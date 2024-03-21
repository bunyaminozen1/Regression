package opc.junit.multi.authforwarding;

import io.cucumber.messages.internal.com.google.gson.Gson;
import opc.enums.opc.ApiDocument;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.CardholderPresent;
import opc.enums.opc.DefaultTimeoutDecision;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.WebhookType;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.CurrencyAmount;
import opc.models.webhook.WebhookAuthForwardingDebitEventModel;
import opc.models.webhook.WebhookAuthForwardingPrepaidEventModel;
import opc.models.webhook.WebhookAuthorisationEventModel;
import opc.models.webhook.WebhookDataResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthForwardingDeniedTests extends BaseAuthForwardingSetup {

    @BeforeAll
    public static void AuthForwardingApprovedSetup(){
        //Enable Auth Forwarding For Innovator
        AdminHelper.enableAuthForwarding(true, innovatorId, adminImpersonatedTenantToken);

        //Enable Auth Forwarding (Denied Webhook) for Programme
        final Map<String, String> deniedWebhookResponse =
                Map.of("default_status", "200",
                        "default_content", new Gson().toJson(Map.of("result", "DENIED")),
                        "default_content_type", "application/json");

        authForwardingWebhookServiceDetails = WebhookHelper.generateWebhookUrl(deniedWebhookResponse);
        InnovatorHelper.enableAuthForwarding(UpdateProgrammeModel.AuthForwardingUrlSetup(programmeId, true, authForwardingWebhookServiceDetails.getRight()),
                programmeId, innovatorToken);
        
        //Set and enable Auth Forwarding on card profiles
        setupManagedCardProfiles(true, DefaultTimeoutDecision.APPROVE.name());
    }

    @AfterAll
    public static void DisableAuthForwarding() {
        //Disable Auth Forwarding For Innovator
        AdminHelper.enableAuthForwarding(false, innovatorId, adminImpersonatedTenantToken);
    }

    @Test
    public void AuthForwardingDenied_PrepaidCardPurchase_DeniedSuccess() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long depositAmount = getRandomDepositAmount();
        final long purchaseAmount = getRandomPurchaseAmount();

        final String managedCardId =
                ManagedCardsHelper
                        .createManagedCard(CreateManagedCardModel
                                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardProfileId,
                                        createCorporateModel.getBaseCurrency()).build(), secretKey, corporate.getRight());

        TestHelper.simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountProfileId,
                transfersProfileId, managedCardId, createCorporateModel.getBaseCurrency(),
                depositAmount, secretKey, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        ManagedCardsHelper.simulatePurchase(secretKey,
                managedCardId,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount));

        final Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse =
                WebhookHelper.getWebhookServiceEvents(authForwardingWebhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTH_FORWARDING));

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTHORISATION));

        final WebhookAuthForwardingPrepaidEventModel authForwardingEvent =
                new Gson().fromJson(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent(), WebhookAuthForwardingPrepaidEventModel.class);

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        assertAll("Auth Forwarding Event",
                // AUTH FORWARDING BODY
                () -> assertEquals("AUTHORISED", authForwardingEvent.getAuthorisationType()),
                () -> assertEquals(depositAmount, (int) Double.parseDouble(authForwardingEvent.getAvailableBalance().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getAvailableBalance().get("currency")),
                () -> assertEquals(managedCardId, authForwardingEvent.getCardId()),
                () -> assertFalse(authForwardingEvent.isCardPresent()),
                () -> assertEquals(CardholderPresent.PRESENT.name(), authForwardingEvent.getCardholderPresent()),
                () -> assertEquals(0, (int) Double.parseDouble(authForwardingEvent.getForexFee().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getForexFee().get("currency")),
                () -> assertEquals(0, (int) Double.parseDouble(authForwardingEvent.getForexPadding().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getForexPadding().get("currency")),
                () -> assertEquals("5399", authForwardingEvent.getMerchantData().get("merchantCategoryCode")),
                () -> assertEquals("MT", authForwardingEvent.getMerchantData().get("merchantCountry")),
                () -> assertEquals("Misc. General Merchandise", authForwardingEvent.getMerchantData().get("merchantDescription")),
                () -> assertEquals("Amazon IT", authForwardingEvent.getMerchantData().get("merchantName")),
                () -> assertEquals("PREPAID_MODE", authForwardingEvent.getMode()),
                () -> assertEquals(corporate.getLeft(), authForwardingEvent.getOwner().get("id")),
                () -> assertEquals(IdentityType.CORPORATE.name(), authForwardingEvent.getOwner().get("type")),
                () -> assertEquals(Math.negateExact(purchaseAmount), (int) Double.parseDouble(authForwardingEvent.getSourceAmount().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getSourceAmount().get("currency")),
                () -> assertEquals(Math.negateExact(purchaseAmount), (int) Double.parseDouble(authForwardingEvent.getTotalTransactionCost().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getTotalTransactionCost().get("currency")),
                () -> assertEquals(Math.negateExact(purchaseAmount), (int) Double.parseDouble(authForwardingEvent.getTransactionAmount().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getTransactionAmount().get("currency")),
                () -> assertNotNull(authForwardingEvent.getTransactionTimestamp()),
                // DATABASE
                () -> assertEquals(authorisation.get("id"), authForwardingEvent.getTransactionId()),
                () -> assertEquals(authorisation.get("auth_code"), authForwardingEvent.getAuthCode())
        );

        assertHeadersAuthForwardingEvent(authForwardingWebhookResponse);

        assertAll("Authorisation Event",
                ()-> assertFalse(authorisationEvent.isApproved()),
                ()-> assertEquals("DECLINE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
                ()-> assertEquals("AUTH_FORWARDING", authorisationEvent.getAuthRuleFailedReason()),
                ()-> assertEquals("DECLINED", authorisationEvent.getAuthorisationType()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals((int) depositAmount, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent())),
                ()-> assertEquals("PRESENT", authorisationEvent.getCardholderPresent()),
                ()-> assertEquals("AUTH_RULE_CHECKS_FAILED", authorisationEvent.getDeclineReason()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency()),
                ()-> assertEquals((int) depositAmount, Integer.parseInt(authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount())),
                ()-> assertEquals(managedCardId, authorisationEvent.getId().get("id")),
                ()-> assertEquals("managed_cards", authorisationEvent.getId().get("type")),
                ()-> assertEquals("5399", authorisationEvent.getMerchantCategoryCode()),
                ()-> assertEquals("Amazon IT", authorisationEvent.getMerchantName()),
                ()-> assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id")),
                ()-> assertEquals("corporates", authorisationEvent.getOwner().get("type")),
                ()-> assertEquals("0", authorisationEvent.getRelatedAuthorisationId()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount"))),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount"))),
                ()-> assertEquals(authorisation.get("id"), authorisationEvent.getTransactionId()),
                ()-> assertEquals(authorisation.get("auth_code"), authorisationEvent.getAuthCode()),
                ()-> assertNotNull(authorisationEvent.getTransactionTimestamp())
        );

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.AuthForwardingEvent, authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
    }

    @Test
    public void AuthForwardingDenied_DebitCardPurchase_DeniedSuccess() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 1000L;
        final long depositAmount = getRandomDepositAmount();
        final long purchaseAmount = getRandomPurchaseAmount();

        final Long depositFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.DEPOSIT_FEE).getAmount();

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporate.getRight());

        final String managedCardId =
                ManagedCardsHelper
                        .createManagedCard(CreateManagedCardModel
                                .DefaultCreateDebitManagedCardModel(corporateDebitManagedCardProfileId,
                                        managedAccountId).build(), secretKey, corporate.getRight());

        TestHelper
                .simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                        depositAmount + depositFee, secretKey, corporate.getRight(), 1);

        ManagedCardsHelper.setDefaultDebitSpendLimit(new CurrencyAmount(createCorporateModel.getBaseCurrency(), availableToSpend), secretKey,
                managedCardId, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        ManagedCardsHelper.simulatePurchase(secretKey,
                managedCardId,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount));

        final Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse =
                WebhookHelper.getWebhookServiceEvents(authForwardingWebhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTH_FORWARDING));

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTHORISATION));

        final WebhookAuthForwardingDebitEventModel authForwardingEvent =
                new Gson().fromJson(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent(), WebhookAuthForwardingDebitEventModel.class);

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        assertAll("Auth Forwarding Event",
                // AUTH FORWARDING BODY
                () -> assertEquals("AUTHORISED", authForwardingEvent.getAuthorisationType()),
                () -> assertEquals(availableToSpend, (int) Double.parseDouble(authForwardingEvent.getAvailableToSpend().get(0).getValue().getAmount())),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getAvailableToSpend().get(0).getValue().getCurrency()),
                () -> assertEquals(LimitInterval.ALWAYS.name(), authForwardingEvent.getAvailableToSpend().get(0).getInterval()),
                () -> assertEquals(managedCardId, authForwardingEvent.getCardId()),
                () -> assertFalse(authForwardingEvent.isCardPresent()),
                () -> assertEquals(CardholderPresent.PRESENT.name(), authForwardingEvent.getCardholderPresent()),
                () -> assertEquals(0, (int) Double.parseDouble(authForwardingEvent.getForexFee().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getForexFee().get("currency")),
                () -> assertEquals(0, (int) Double.parseDouble(authForwardingEvent.getForexPadding().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getForexPadding().get("currency")),
                () -> assertEquals("5399", authForwardingEvent.getMerchantData().get("merchantCategoryCode")),
                () -> assertEquals("MT", authForwardingEvent.getMerchantData().get("merchantCountry")),
                () -> assertEquals("Misc. General Merchandise", authForwardingEvent.getMerchantData().get("merchantDescription")),
                () -> assertEquals("Amazon IT", authForwardingEvent.getMerchantData().get("merchantName")),
                () -> assertEquals("DEBIT_MODE", authForwardingEvent.getMode()),
                () -> assertEquals(corporate.getLeft(), authForwardingEvent.getOwner().get("id")),
                () -> assertEquals(IdentityType.CORPORATE.name(), authForwardingEvent.getOwner().get("type")),
                () -> assertEquals(Math.negateExact(purchaseAmount), (int) Double.parseDouble(authForwardingEvent.getSourceAmount().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getSourceAmount().get("currency")),
                () -> assertEquals(Math.negateExact(purchaseAmount), (int) Double.parseDouble(authForwardingEvent.getTotalTransactionCost().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getTotalTransactionCost().get("currency")),
                () -> assertEquals(Math.negateExact(purchaseAmount), (int) Double.parseDouble(authForwardingEvent.getTransactionAmount().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getTransactionAmount().get("currency")),
                () -> assertEquals(managedAccountId, authForwardingEvent.getParentManagedAccountId()),
                () -> assertNotNull(authForwardingEvent.getTransactionTimestamp()),
                // DATABASE
                () -> assertEquals(authorisation.get("id"), authForwardingEvent.getTransactionId()),
                () -> assertEquals(authorisation.get("auth_code"), authForwardingEvent.getAuthCode())
        );

        assertHeadersAuthForwardingEvent(authForwardingWebhookResponse);

        assertAll("Authorisation Event",
                ()-> assertFalse(authorisationEvent.isApproved()),
                ()-> assertEquals("DECLINE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
                ()-> assertEquals("AUTH_FORWARDING", authorisationEvent.getAuthRuleFailedReason()),
                ()-> assertEquals("DECLINED", authorisationEvent.getAuthorisationType()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent())),
                ()-> assertEquals("PRESENT", authorisationEvent.getCardholderPresent()),
                ()-> assertEquals("AUTH_RULE_CHECKS_FAILED", authorisationEvent.getDeclineReason()),
                ()-> assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId()),
                ()-> assertEquals((int) (availableToSpend), Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount"))),
                ()-> assertEquals("ALWAYS", authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval")),
                ()-> assertEquals(managedCardId, authorisationEvent.getId().get("id")),
                ()-> assertEquals("managed_cards", authorisationEvent.getId().get("type")),
                ()-> assertEquals("5399", authorisationEvent.getMerchantCategoryCode()),
                ()-> assertEquals("Amazon IT", authorisationEvent.getMerchantName()),
                ()-> assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id")),
                ()-> assertEquals("corporates", authorisationEvent.getOwner().get("type")),
                ()-> assertEquals("0", authorisationEvent.getRelatedAuthorisationId()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount"))),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount"))),
                ()-> assertEquals(authorisation.get("id"), authorisationEvent.getTransactionId()),
                ()-> assertEquals(authorisation.get("auth_code"), authorisationEvent.getAuthCode()),
                ()-> assertNotNull(authorisationEvent.getTransactionTimestamp())
        );

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.AuthForwardingEvent, authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
    }

    private void assertHeadersAuthForwardingEvent(Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse) {
        assertAll("Headers",
                () -> assertNotNull(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getHeaders().getWebhooksKey().get(0)),
                () -> assertEquals("application/json", authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getHeaders().getContentType().get(0)),
                () -> assertNotNull(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getHeaders().getSignature().get(0)),
                () -> assertNotNull(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getHeaders().getCallRef().get(0))
        );
    }


    private static void setupManagedCardProfiles(final boolean authForwardingEnabled, final String defaultTimeoutDecision) {
        corporateDebitManagedCardProfileId = getCorporateDebitManagedCardProfileId(authForwardingEnabled, defaultTimeoutDecision);
        corporatePrepaidManagedCardProfileId = getCorporatePrepaidManagedCardProfileId(authForwardingEnabled, defaultTimeoutDecision);
        consumerDebitManagedCardProfileId = getConsumerDebitManagedCardProfileId(authForwardingEnabled, defaultTimeoutDecision);
        consumerPrepaidManagedCardProfileId = getConsumerPrepaidManagedCardProfileId(authForwardingEnabled, defaultTimeoutDecision);
    }
}