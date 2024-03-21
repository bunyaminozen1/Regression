package opc.junit.multi.authforwarding;

import io.cucumber.messages.internal.com.google.gson.Gson;
import opc.enums.opc.ApiDocument;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.CardholderPresent;
import opc.enums.opc.DefaultTimeoutDecision;
import opc.enums.opc.FeeType;
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
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.models.shared.CurrencyAmount;
import opc.models.webhook.WebhookAuthorisationEventModel;
import opc.models.webhook.WebhookDataResponse;
import opc.models.webhook.WebhookSettlementEventModel;
import opc.services.multi.ManagedCardsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthForwardingTimeoutTests extends BaseAuthForwardingSetup {

    @BeforeAll
    public static void AuthForwardingApprovedSetup(){
        // Enable Auth Forwarding For Innovator
        AdminHelper.enableAuthForwarding(true, innovatorId, adminImpersonatedTenantToken);

        // Enable Auth Forwarding (Timeout Webhook) for Programme
        final Map<String, String> deniedWebhookResponse =
                Map.of("default_status", "200",
                        "default_content", new Gson().toJson(Map.of("result", "DENIED")),
                        "default_content_type", "application/json",
                        "timeout", "5");

        authForwardingWebhookServiceDetails = WebhookHelper.generateWebhookUrl(deniedWebhookResponse);

        InnovatorHelper.enableAuthForwarding(UpdateProgrammeModel.AuthForwardingUrlSetup(programmeId, true, authForwardingWebhookServiceDetails.getRight()),
                programmeId, innovatorToken);
    }

    @AfterAll
    public static void DisableAuthForwarding() {
        // Disable Auth Forwarding For Innovator
        AdminHelper.enableAuthForwarding(false, innovatorId, adminImpersonatedTenantToken);
    }

    @Test
    public void AuthForwardingTimeout_PrepaidCardPurchase_CardProfileLevelDefaultDecision_Decline_Success() throws SQLException {

        // Set Profile level default timeout decision - Decline
        corporatePrepaidManagedCardProfileId = getCorporatePrepaidManagedCardProfileId(true, DefaultTimeoutDecision.DECLINE.name());

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

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTHORISATION));

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);


        assertAll("Authorisation Event",
                ()-> assertFalse(authorisationEvent.isApproved()),
                ()-> assertEquals("DECLINE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
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

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
    }

    @Test
    public void AuthForwardingTimeout_PrepaidCardPurchase_CardProfileLevelDefaultDecision_Approve_Success() throws SQLException {

        // Set Profile level default timeout decision - Approve
        corporatePrepaidManagedCardProfileId = getCorporatePrepaidManagedCardProfileId(true, DefaultTimeoutDecision.APPROVE.name());

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long depositAmount = getRandomDepositAmount();
        final long purchaseAmount = getRandomPurchaseAmount();

        final Long purchaseFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.PURCHASE_FEE).getAmount();
        final long remainingBalanceAfterAuth = depositAmount - purchaseAmount;
        final long remainingBalanceAfterSettlement = depositAmount - purchaseAmount - purchaseFee;

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

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        Arrays.asList(WebhookType.AUTHORISATION, WebhookType.SETTLEMENT));

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final WebhookSettlementEventModel settlementEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.SETTLEMENT).getContent(), WebhookSettlementEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);


        assertAll("Authorisation Event",
                ()-> assertTrue(authorisationEvent.isApproved()),
                ()-> assertEquals("APPROVE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
                ()-> assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason()),
                ()-> assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals((int) remainingBalanceAfterAuth, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent())),
                ()-> assertEquals(CardholderPresent.PRESENT.name(), authorisationEvent.getCardholderPresent()),
                ()-> assertEquals("NO_REASON", authorisationEvent.getDeclineReason()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency()),
                ()-> assertEquals((int) remainingBalanceAfterAuth, Integer.parseInt(authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount())),
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

        assertAll("Settlement Event",
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals((int) remainingBalanceAfterSettlement, Integer.parseInt(settlementEvent.getAvailableBalance().get("amount"))),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency()),
                ()-> assertEquals((int) remainingBalanceAfterSettlement, Integer.parseInt(settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount())),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getFeeAmount().get("currency")),
                ()-> assertEquals(purchaseFee.intValue(), Integer.parseInt(settlementEvent.getFeeAmount().get("amount"))),
                ()-> assertEquals(managedCardId, settlementEvent.getId().get("id")),
                ()-> assertEquals("managed_cards", settlementEvent.getId().get("type")),
                ()-> assertEquals("5399", settlementEvent.getMerchantCategoryCode()),
                ()-> assertEquals("Amazon IT", settlementEvent.getMerchantName()),
                ()-> assertEquals(corporate.getLeft(), settlementEvent.getOwner().get("id")),
                ()-> assertEquals("corporates", settlementEvent.getOwner().get("type")),
                ()-> assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId()),
                ()-> assertEquals("SALE_PURCHASE", settlementEvent.getSettlementType()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getSourceAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getSourceAmount().get("amount"))),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getTransactionAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getTransactionAmount().get("amount"))),
                ()-> assertEquals(settlement.get("id"), settlementEvent.getTransactionId()),
                ()-> assertNotNull(settlementEvent.getTransactionTimestamp()),
                ()-> assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode())
        );

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsSettlementEvent, webhookResponse.get(WebhookType.SETTLEMENT).getContent());
    }

    @Test
    public void AuthForwardingTimeout_DebitCardPurchase_CardProfileLevelDefaultDecision_Decline_Success() throws SQLException {

        // Set Profile level default timeout decision - Decline
        corporateDebitManagedCardProfileId = getCorporateDebitManagedCardProfileId(true, DefaultTimeoutDecision.DECLINE.name());

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


        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTHORISATION));


        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);


        assertAll("Authorisation Event",
                ()-> assertFalse(authorisationEvent.isApproved()),
                ()-> assertEquals("DECLINE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
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

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
    }

    @Test
    public void AuthForwardingTimeout_DebitCardPurchase_CardProfileLevelDefaultDecision_Approve_Success() throws SQLException {

        // Set Profile level default timeout decision - Approve
        corporateDebitManagedCardProfileId = getCorporateDebitManagedCardProfileId(true, DefaultTimeoutDecision.APPROVE.name());

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 1000L;
        final long depositAmount = getRandomDepositAmount();
        final long purchaseAmount = getRandomPurchaseAmount();

        final Long depositFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long purchaseFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.PURCHASE_FEE).getAmount();

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


        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        Arrays.asList(WebhookType.AUTHORISATION, WebhookType.SETTLEMENT));


        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final WebhookSettlementEventModel settlementEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.SETTLEMENT).getContent(), WebhookSettlementEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);


        assertAll("Authorisation Event",
                ()-> assertTrue(authorisationEvent.isApproved()),
                ()-> assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason()),
                ()-> assertEquals("APPROVE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
                ()-> assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent())),
                ()-> assertEquals(CardholderPresent.PRESENT.name(), authorisationEvent.getCardholderPresent()),
                ()-> assertEquals("NO_REASON", authorisationEvent.getDeclineReason()),
                ()-> assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId()),
                ()-> assertEquals((int) (availableToSpend - purchaseAmount), Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount"))),
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

        assertAll("Settlement Event",
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals(0, Integer.parseInt(settlementEvent.getAvailableBalance().get("amount"))),
                ()-> assertEquals(managedAccountId, settlementEvent.getDetails().getDebitModeDetails().getParentManagedAccountId()),
                ()-> assertEquals((int) (availableToSpend - purchaseAmount - purchaseFee), Integer.parseInt(settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount"))),
                ()-> assertEquals("ALWAYS", settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval")),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getFeeAmount().get("currency")),
                ()-> assertEquals(purchaseFee.intValue(), Integer.parseInt(settlementEvent.getFeeAmount().get("amount"))),
                ()-> assertEquals(managedCardId, settlementEvent.getId().get("id")),
                ()-> assertEquals("managed_cards", settlementEvent.getId().get("type")),
                ()-> assertEquals("5399", settlementEvent.getMerchantCategoryCode()),
                ()-> assertEquals("Amazon IT", settlementEvent.getMerchantName()),
                ()-> assertEquals(corporate.getLeft(), settlementEvent.getOwner().get("id")),
                ()-> assertEquals("corporates", settlementEvent.getOwner().get("type")),
                ()-> assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId()),
                ()-> assertEquals("SALE_PURCHASE", settlementEvent.getSettlementType()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getSourceAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getSourceAmount().get("amount"))),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getTransactionAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getTransactionAmount().get("amount"))),
                ()-> assertEquals(settlement.get("id"), settlementEvent.getTransactionId()),
                ()-> assertNotNull(settlementEvent.getTransactionTimestamp()),
                ()-> assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode())
        );

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsSettlementEvent, webhookResponse.get(WebhookType.SETTLEMENT).getContent());
    }

    @Test
    public void AuthForwardingTimeout_DebitCardPurchase_CardLevelDefaultDecision_Approve_Success() throws SQLException {

        // Set Profile level default timeout decision - Decline
        corporateDebitManagedCardProfileId = getCorporateDebitManagedCardProfileId(true, DefaultTimeoutDecision.DECLINE.name());

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 1000L;
        final long depositAmount = getRandomDepositAmount();
        final long purchaseAmount = getRandomPurchaseAmount();

        final Long depositFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long purchaseFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.PURCHASE_FEE).getAmount();

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporate.getRight());

        // Create Managed Card with Default Decision - Approve (Card level decision takes priority over profile level decision)
        final String managedCardId =
                ManagedCardsHelper
                        .createManagedCard(CreateManagedCardModel
                                .DefaultCreateDebitManagedCardAuthForwardingModel(corporateDebitManagedCardProfileId,
                                        managedAccountId, DefaultTimeoutDecision.APPROVE.name()).build(), secretKey, corporate.getRight());

        TestHelper
                .simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                        depositAmount + depositFee, secretKey, corporate.getRight(), 1);

        ManagedCardsHelper.setDefaultDebitSpendLimit(new CurrencyAmount(createCorporateModel.getBaseCurrency(), availableToSpend), secretKey,
                managedCardId, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        ManagedCardsHelper.simulatePurchase(secretKey,
                managedCardId,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount));


        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        Arrays.asList(WebhookType.AUTHORISATION, WebhookType.SETTLEMENT));


        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final WebhookSettlementEventModel settlementEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.SETTLEMENT).getContent(), WebhookSettlementEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);


        assertAll("Authorisation Event",
                ()-> assertTrue(authorisationEvent.isApproved()),
                ()-> assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason()),
                ()-> assertEquals("APPROVE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
                ()-> assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent())),
                ()-> assertEquals(CardholderPresent.PRESENT.name(), authorisationEvent.getCardholderPresent()),
                ()-> assertEquals("NO_REASON", authorisationEvent.getDeclineReason()),
                ()-> assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId()),
                ()-> assertEquals((int) (availableToSpend - purchaseAmount), Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount"))),
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

        assertAll("Settlement Event",
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals(0, Integer.parseInt(settlementEvent.getAvailableBalance().get("amount"))),
                ()-> assertEquals(managedAccountId, settlementEvent.getDetails().getDebitModeDetails().getParentManagedAccountId()),
                ()-> assertEquals((int) (availableToSpend - purchaseAmount - purchaseFee), Integer.parseInt(settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount"))),
                ()-> assertEquals("ALWAYS", settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval")),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getFeeAmount().get("currency")),
                ()-> assertEquals(purchaseFee.intValue(), Integer.parseInt(settlementEvent.getFeeAmount().get("amount"))),
                ()-> assertEquals(managedCardId, settlementEvent.getId().get("id")),
                ()-> assertEquals("managed_cards", settlementEvent.getId().get("type")),
                ()-> assertEquals("5399", settlementEvent.getMerchantCategoryCode()),
                ()-> assertEquals("Amazon IT", settlementEvent.getMerchantName()),
                ()-> assertEquals(corporate.getLeft(), settlementEvent.getOwner().get("id")),
                ()-> assertEquals("corporates", settlementEvent.getOwner().get("type")),
                ()-> assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId()),
                ()-> assertEquals("SALE_PURCHASE", settlementEvent.getSettlementType()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getSourceAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getSourceAmount().get("amount"))),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getTransactionAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getTransactionAmount().get("amount"))),
                ()-> assertEquals(settlement.get("id"), settlementEvent.getTransactionId()),
                ()-> assertNotNull(settlementEvent.getTransactionTimestamp()),
                ()-> assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode())
        );

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsSettlementEvent, webhookResponse.get(WebhookType.SETTLEMENT).getContent());
    }

    @Test
    public void AuthForwardingTimeout_DebitCardPurchase_CardLevelDefaultDecision_Decline_Success() throws SQLException {

        // Set Profile level default timeout decision - Approve
        corporateDebitManagedCardProfileId = getCorporateDebitManagedCardProfileId(true, DefaultTimeoutDecision.APPROVE.name());

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
        // Patch Managed Card with Default Decision - Decline (Card level decision takes priority over profile level decision)
        final PatchManagedCardModel patchManagedCardModel =
                PatchManagedCardModel.DefaultPatchManagedCardModel()
                        .setAuthForwardingDefaultTimeoutDecision(DefaultTimeoutDecision.DECLINE.name())
                        .build();

        ManagedCardsService.patchManagedCard(patchManagedCardModel,secretKey,managedCardId,corporate.getRight());

        TestHelper
                .simulateManagedAccountDeposit(managedAccountId, createManagedAccountModel.getCurrency(),
                        depositAmount + depositFee, secretKey, corporate.getRight(), 1);

        ManagedCardsHelper.setDefaultDebitSpendLimit(new CurrencyAmount(createCorporateModel.getBaseCurrency(), availableToSpend), secretKey,
                managedCardId, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        ManagedCardsHelper.simulatePurchase(secretKey,
                managedCardId,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount));


        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTHORISATION));


        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        assertAll("Authorisation Event",
                ()-> assertFalse(authorisationEvent.isApproved()),
                ()-> assertEquals("DECLINE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
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

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
    }
}