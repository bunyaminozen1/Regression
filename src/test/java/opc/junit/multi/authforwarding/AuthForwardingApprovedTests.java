package opc.junit.multi.authforwarding;

import io.cucumber.messages.internal.com.google.gson.Gson;
import opc.enums.opc.ApiDocument;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.CardMode;
import opc.enums.opc.CardholderPresent;
import commons.enums.Currency;
import opc.enums.opc.DefaultTimeoutDecision;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.WebhookType;
import opc.junit.database.GpsSimulatorDatabaseHelper;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CardDetailsModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.DebitCardModeDetailsModel;
import opc.models.multi.managedcards.PrepaidCardModeDetailsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.AdditionalMerchantDataModel;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.models.webhook.WebhookAuthForwardingDebitEventModel;
import opc.models.webhook.WebhookAuthForwardingPrepaidEventModel;
import opc.models.webhook.WebhookAuthorisationEventModel;
import opc.models.webhook.WebhookDataResponse;
import opc.models.webhook.WebhookSettlementEventModel;
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

public class AuthForwardingApprovedTests extends BaseAuthForwardingSetup {

    @BeforeAll
    public static void AuthForwardingApprovedSetup(){
        //Enable Auth Forwarding For Innovator
        AdminHelper.enableAuthForwarding(true, innovatorId, adminImpersonatedTenantToken);

        //Enable Auth Forwarding (Approved Webhook) for Programme
        final Map<String, String> approvedWebhookResponse =
                Map.of("default_status", "200",
                        "default_content", new Gson().toJson(Map.of("result", "APPROVED")),
                        "default_content_type", "application/json");

        authForwardingWebhookServiceDetails = WebhookHelper.generateWebhookUrl(approvedWebhookResponse);
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
    public void AuthForwardingApproved_PrepaidCardPurchase_Success() throws SQLException {

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

        final Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse =
                WebhookHelper.getWebhookServiceEvents(authForwardingWebhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTH_FORWARDING));

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        Arrays.asList(WebhookType.AUTHORISATION, WebhookType.SETTLEMENT));

        final WebhookAuthForwardingPrepaidEventModel authForwardingEvent =
                new Gson().fromJson(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent(), WebhookAuthForwardingPrepaidEventModel.class);

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final WebhookSettlementEventModel settlementEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.SETTLEMENT).getContent(), WebhookSettlementEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);

        assertAll("Auth Forwarding Event",
                // AUTH FORWARDING BODY
                () -> assertEquals("AUTHORISED", authForwardingEvent.getAuthorisationType()),
                () -> assertEquals(depositAmount,(int) Double.parseDouble(authForwardingEvent.getAvailableBalance().get("amount"))),
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
                ()-> assertTrue(authorisationEvent.isApproved()),
                ()-> assertEquals("APPROVE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
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

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.AuthForwardingEvent, authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsSettlementEvent, webhookResponse.get(WebhookType.SETTLEMENT).getContent());
    }

    @Test
    public void AuthForwardingApproved_DebitCardPurchase_Success() throws SQLException {

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

        final Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse =
                WebhookHelper.getWebhookServiceEvents(authForwardingWebhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTH_FORWARDING));

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        Arrays.asList(WebhookType.AUTHORISATION, WebhookType.SETTLEMENT));

        final WebhookAuthForwardingDebitEventModel authForwardingEvent =
                new Gson().fromJson(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent(), WebhookAuthForwardingDebitEventModel.class);

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final WebhookSettlementEventModel settlementEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.SETTLEMENT).getContent(), WebhookSettlementEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);

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
                ()-> assertTrue(authorisationEvent.isApproved()),
                ()-> assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason()),
                ()-> assertEquals("APPROVE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
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

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.AuthForwardingEvent, authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsSettlementEvent, webhookResponse.get(WebhookType.SETTLEMENT).getContent());
    }

    @Test
    public void AuthForwardingApproved_PurchaseExceedingAvailableToSpendLimit_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 10L;
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

        final WebhookDataResponse webhookResponse =
                WebhookHelper.getWebhookServiceEvent(webhookServiceDetails.getLeft(),
                        timestamp,
                        WebhookType.AUTHORISATION);

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.getContent(), WebhookAuthorisationEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        assertAll("Authorisation Event",
                () -> assertAuthForwardingNotTriggered(authorisation),
                () -> assertFalse(authorisationEvent.isApproved()),
                () -> assertEquals("SPEND_LIMIT_EXCEEDED", authorisationEvent.getAuthRuleFailedReason()),
                () -> assertEquals("DECLINED", authorisationEvent.getAuthorisationType()),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency")),
                () -> assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount"))),
                () -> assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent())),
                () -> assertEquals(CardholderPresent.PRESENT.name(), authorisationEvent.getCardholderPresent()),
                () -> assertEquals("AUTH_RULE_CHECKS_FAILED", authorisationEvent.getDeclineReason()),
                () -> assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId()),
                () -> assertEquals((int) availableToSpend, Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount"))),
                () -> assertEquals("ALWAYS", authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval")),
                () -> assertEquals(managedCardId, authorisationEvent.getId().get("id")),
                () -> assertEquals("managed_cards", authorisationEvent.getId().get("type")),
                () -> assertEquals("5399", authorisationEvent.getMerchantCategoryCode()),
                () -> assertEquals("Amazon IT", authorisationEvent.getMerchantName()),
                () -> assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id")),
                () -> assertEquals("corporates", authorisationEvent.getOwner().get("type")),
                () -> assertEquals("0", authorisationEvent.getRelatedAuthorisationId()),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency")),
                () -> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency")),
                () -> assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount"))),
                () -> assertNotNull(authorisationEvent.getTransactionId()),
                () -> assertNotNull(authorisationEvent.getTransactionTimestamp()),
                () -> assertNotNull(authorisationEvent.getAuthCode())
        );

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.getContent());
    }

    @Test
    public void AuthForwardingApproved_PrepaidCardForexPurchase_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        final long depositAmount = getRandomDepositAmount();
        final long purchaseAmount = getRandomPurchaseAmount();

        final Long purchaseFee = TestHelper.getFees(createConsumerModel.getBaseCurrency()).get(FeeType.PURCHASE_FEE).getAmount();

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(consumerPrepaidManagedCardProfileId, createConsumerModel.getBaseCurrency(),
                        secretKey, consumer.getRight());

        TestHelper.simulateManagedAccountDepositAndTransferToCard(consumerManagedAccountProfileId,
                transfersProfileId, managedCardId, createConsumerModel.getBaseCurrency(), depositAmount, secretKey, consumer.getRight());

        ManagedCardsHelper.setDefaultPrepaidSpendLimit(secretKey, managedCardId, consumer.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        final Currency forexCurrency = Currency.getRandomWithExcludedCurrency(Currency.valueOf(createConsumerModel.getBaseCurrency()));

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(forexCurrency.name(), purchaseAmount))
                        .setForexPadding(20L)
                        .setForexFee(15L)
                        .build();

        SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                simulateCardPurchaseModel);

        final long actualPurchaseAmount = Math.abs(Long.parseLong(GpsSimulatorDatabaseHelper.getLatestSettlement().get(0).get("card_amount")));

        final Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse =
                WebhookHelper.getWebhookServiceEvents(authForwardingWebhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTH_FORWARDING));

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        Arrays.asList(WebhookType.AUTHORISATION, WebhookType.SETTLEMENT));

        final WebhookAuthForwardingPrepaidEventModel authForwardingEvent =
                new Gson().fromJson(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent(), WebhookAuthForwardingPrepaidEventModel.class);

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final WebhookSettlementEventModel settlementEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.SETTLEMENT).getContent(), WebhookSettlementEventModel.class);

        final PrepaidCardModeDetailsModel.Builder prepaidDetails =
                PrepaidCardModeDetailsModel.builder().setCardMode(CardMode.PREPAID)
                        .setAvailableBalance(depositAmount)
                        .setCurrency(createConsumerModel.getBaseCurrency());

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        assertAll("Auth Forwarding Event",
                // AUTH FORWARDING BODY
                () -> assertEquals("AUTHORISED", authForwardingEvent.getAuthorisationType()),
                () -> assertEquals(depositAmount,(int) Double.parseDouble(authForwardingEvent.getAvailableBalance().get("amount"))),
                () -> assertEquals(createConsumerModel.getBaseCurrency(), authForwardingEvent.getAvailableBalance().get("currency")),
                () -> assertEquals(managedCardId, authForwardingEvent.getCardId()),
                () -> assertFalse(authForwardingEvent.isCardPresent()),
                () -> assertEquals(CardholderPresent.PRESENT.name(), authForwardingEvent.getCardholderPresent()),
                () -> assertEquals(simulateCardPurchaseModel.getForexFee(), (int) Double.parseDouble(authForwardingEvent.getForexFee().get("amount"))),
                () -> assertEquals(createConsumerModel.getBaseCurrency(), authForwardingEvent.getForexFee().get("currency")),
                () -> assertEquals(simulateCardPurchaseModel.getForexPadding(), (int) Double.parseDouble(authForwardingEvent.getForexPadding().get("amount"))),
                () -> assertEquals(createConsumerModel.getBaseCurrency(), authForwardingEvent.getForexPadding().get("currency")),
                () -> assertEquals("5399", authForwardingEvent.getMerchantData().get("merchantCategoryCode")),
                () -> assertEquals("MT", authForwardingEvent.getMerchantData().get("merchantCountry")),
                () -> assertEquals("Misc. General Merchandise", authForwardingEvent.getMerchantData().get("merchantDescription")),
                () -> assertEquals("Amazon IT", authForwardingEvent.getMerchantData().get("merchantName")),
                () -> assertEquals("PREPAID_MODE", authForwardingEvent.getMode()),
                () -> assertEquals(consumer.getLeft(), authForwardingEvent.getOwner().get("id")),
                () -> assertEquals(IdentityType.CONSUMER.name(), authForwardingEvent.getOwner().get("type")),
                () -> assertEquals(Math.negateExact(purchaseAmount), (int) Double.parseDouble(authForwardingEvent.getSourceAmount().get("amount"))),
                () -> assertEquals(forexCurrency.name() , authForwardingEvent.getSourceAmount().get("currency")),
                () -> assertEquals(Math.negateExact(actualPurchaseAmount + simulateCardPurchaseModel.getForexFee() +
                        simulateCardPurchaseModel.getForexPadding()), (int) Double.parseDouble(authForwardingEvent.getTotalTransactionCost().get("amount"))),
                () -> assertEquals(createConsumerModel.getBaseCurrency(), authForwardingEvent.getTotalTransactionCost().get("currency")),
                () -> assertEquals(Math.negateExact(actualPurchaseAmount), (int) Double.parseDouble(authForwardingEvent.getTransactionAmount().get("amount"))),
                () -> assertEquals(createConsumerModel.getBaseCurrency(), authForwardingEvent.getTransactionAmount().get("currency")),
                () -> assertNotNull(authForwardingEvent.getTransactionTimestamp()),
                // DATABASE
                () -> assertEquals(authorisation.get("id"), authForwardingEvent.getTransactionId()),
                () -> assertEquals(authorisation.get("auth_code"), authForwardingEvent.getAuthCode())
        );

        assertHeadersAuthForwardingEvent(authForwardingWebhookResponse);

        assertAuthorisationEvent(authorisationEvent, simulateCardPurchaseModel,
                new CurrencyAmount(createConsumerModel.getBaseCurrency(), actualPurchaseAmount + simulateCardPurchaseModel.getForexFee() +
                        simulateCardPurchaseModel.getForexPadding()), managedCardId, consumer.getLeft(), "consumers",
                prepaidDetails.setAvailableBalance(depositAmount - actualPurchaseAmount - simulateCardPurchaseModel.getForexFee() - simulateCardPurchaseModel.getForexPadding()).build());

        assertSettlementEvent(settlementEvent, authorisationEvent,
                simulateCardPurchaseModel,
                new CurrencyAmount(createConsumerModel.getBaseCurrency(), actualPurchaseAmount + simulateCardPurchaseModel.getForexFee()),
                purchaseFee, managedCardId, consumer.getLeft(), "consumers",
                prepaidDetails.setAvailableBalance(depositAmount - actualPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee()).build());

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.AuthForwardingEvent, authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsSettlementEvent, webhookResponse.get(WebhookType.SETTLEMENT).getContent());
    }

    @Test
    public void AuthForwardingApproved_DebitCardForexPurchase_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

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
        final Currency forexCurrency = Currency.getRandomWithExcludedCurrency(Currency.valueOf(createCorporateModel.getBaseCurrency()));

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(forexCurrency.name(), purchaseAmount))
                        .setForexPadding(10L)
                        .setForexFee(5L)
                        .build();

        SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                simulateCardPurchaseModel);

        final long actualPurchaseAmount = Math.abs(Long.parseLong(GpsSimulatorDatabaseHelper.getLatestSettlement().get(0).get("card_amount")));

        final Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse =
                WebhookHelper.getWebhookServiceEvents(authForwardingWebhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTH_FORWARDING));

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        Arrays.asList(WebhookType.AUTHORISATION, WebhookType.SETTLEMENT));

        final WebhookAuthForwardingDebitEventModel authForwardingEvent =
                new Gson().fromJson(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent(), WebhookAuthForwardingDebitEventModel.class);

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final WebhookSettlementEventModel settlementEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.SETTLEMENT).getContent(), WebhookSettlementEventModel.class);

        final DebitCardModeDetailsModel.Builder debitDetails =
                DebitCardModeDetailsModel.builder().setCardMode(CardMode.DEBIT)
                        .setInterval("ALWAYS")
                        .setManagedAccountId(managedAccountId);

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
                () -> assertEquals(simulateCardPurchaseModel.getForexFee(), (int) Double.parseDouble(authForwardingEvent.getForexFee().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getForexFee().get("currency")),
                () -> assertEquals(simulateCardPurchaseModel.getForexPadding(), (int) Double.parseDouble(authForwardingEvent.getForexPadding().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getForexPadding().get("currency")),
                () -> assertEquals("5399", authForwardingEvent.getMerchantData().get("merchantCategoryCode")),
                () -> assertEquals("MT", authForwardingEvent.getMerchantData().get("merchantCountry")),
                () -> assertEquals("Misc. General Merchandise", authForwardingEvent.getMerchantData().get("merchantDescription")),
                () -> assertEquals("Amazon IT", authForwardingEvent.getMerchantData().get("merchantName")),
                () -> assertEquals("DEBIT_MODE", authForwardingEvent.getMode()),
                () -> assertEquals(corporate.getLeft(), authForwardingEvent.getOwner().get("id")),
                () -> assertEquals(IdentityType.CORPORATE.name(), authForwardingEvent.getOwner().get("type")),
                () -> assertEquals(Math.negateExact(purchaseAmount), (int) Double.parseDouble(authForwardingEvent.getSourceAmount().get("amount"))),
                () -> assertEquals(forexCurrency.name() , authForwardingEvent.getSourceAmount().get("currency")),
                () -> assertEquals(Math.negateExact(actualPurchaseAmount + simulateCardPurchaseModel.getForexFee() +
                        simulateCardPurchaseModel.getForexPadding()), (int) Double.parseDouble(authForwardingEvent.getTotalTransactionCost().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getTotalTransactionCost().get("currency")),
                () -> assertEquals(Math.negateExact(actualPurchaseAmount), (int) Double.parseDouble(authForwardingEvent.getTransactionAmount().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getTransactionAmount().get("currency")),
                () -> assertEquals(managedAccountId, authForwardingEvent.getParentManagedAccountId()),
                () -> assertNotNull(authForwardingEvent.getTransactionTimestamp()),
                // DATABASE
                () -> assertEquals(authorisation.get("id"), authForwardingEvent.getTransactionId()),
                () -> assertEquals(authorisation.get("auth_code"), authForwardingEvent.getAuthCode())
        );

        assertHeadersAuthForwardingEvent(authForwardingWebhookResponse);

        assertAuthorisationEvent(authorisationEvent, simulateCardPurchaseModel,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), actualPurchaseAmount + simulateCardPurchaseModel.getForexFee() +
                        simulateCardPurchaseModel.getForexPadding()), managedCardId, corporate.getLeft(), "corporates",
                debitDetails.setAvailableToSpend(availableToSpend - actualPurchaseAmount - simulateCardPurchaseModel.getForexFee()).build());

        assertSettlementEvent(settlementEvent, authorisationEvent,
                simulateCardPurchaseModel,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), actualPurchaseAmount + simulateCardPurchaseModel.getForexFee()),
                purchaseFee, managedCardId, corporate.getLeft(), "corporates",
                debitDetails.setAvailableToSpend(availableToSpend - actualPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee()).build());

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.AuthForwardingEvent, authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsSettlementEvent, webhookResponse.get(WebhookType.SETTLEMENT).getContent());
    }

    @Test
    public void AuthForwardingApproved_PrepaidCardPurchase_AdditionalData_Success() throws SQLException {

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

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount))
                        .setMerchantId("123456")
                        .setCardPresent(false)
                        .setCardHolderPresent(CardholderPresent.NOT_PRESENT.name())
                        .setAdditionalMerchantData(AdditionalMerchantDataModel
                                .DefaultAdditionalDataModel().build())
                        .build();

        SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                simulateCardPurchaseModel);

        final Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse =
                WebhookHelper.getWebhookServiceEvents(authForwardingWebhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTH_FORWARDING));

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        Arrays.asList(WebhookType.AUTHORISATION, WebhookType.SETTLEMENT));

        final WebhookAuthForwardingPrepaidEventModel authForwardingEvent =
                new Gson().fromJson(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent(), WebhookAuthForwardingPrepaidEventModel.class);

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final WebhookSettlementEventModel settlementEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.SETTLEMENT).getContent(), WebhookSettlementEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);

        assertAll("Auth Forwarding Event",
                // AUTH FORWARDING BODY
                () -> assertEquals("AUTHORISED", authForwardingEvent.getAuthorisationType()),
                () -> assertEquals(depositAmount,(int) Double.parseDouble(authForwardingEvent.getAvailableBalance().get("amount"))),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getAvailableBalance().get("currency")),
                () -> assertEquals(managedCardId, authForwardingEvent.getCardId()),
                () -> assertFalse(authForwardingEvent.isCardPresent()),
                () -> assertEquals(CardholderPresent.NOT_PRESENT.name(), authForwardingEvent.getCardholderPresent()),
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
        assertAdditionalMerchantDataAuthForwardingEvent(authForwardingEvent);

        assertAll("Authorisation Event",
                ()-> assertTrue(authorisationEvent.isApproved()),
                ()-> assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason()),
                ()-> assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType()),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals((int) remainingBalanceAfterAuth, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent())),
                ()-> assertEquals("NOT_PRESENT", authorisationEvent.getCardholderPresent()),
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

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.AuthForwardingEvent, authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsSettlementEvent, webhookResponse.get(WebhookType.SETTLEMENT).getContent());
    }

    @Test
    public void AuthForwardingApproved_DebitCardPurchase_AdditionalData_Success() throws SQLException {

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

        final SimulateCardPurchaseByIdModel simulateCardPurchaseModel =
                SimulateCardPurchaseByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount))
                        .setMerchantId("123456")
                        .setCardPresent(true)
                        .setCardHolderPresent(CardholderPresent.PRESENT.name())
                        .setAdditionalMerchantData(AdditionalMerchantDataModel
                                .DefaultAdditionalDataModel().build())
                        .build();

        SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                simulateCardPurchaseModel);

        final Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse =
                WebhookHelper.getWebhookServiceEvents(authForwardingWebhookServiceDetails.getLeft(),
                        timestamp,
                        List.of(WebhookType.AUTH_FORWARDING));

        final Map<WebhookType, WebhookDataResponse> webhookResponse =
                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(),
                        timestamp,
                        Arrays.asList(WebhookType.AUTHORISATION, WebhookType.SETTLEMENT));

        final WebhookAuthForwardingDebitEventModel authForwardingEvent =
                new Gson().fromJson(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent(), WebhookAuthForwardingDebitEventModel.class);

        final WebhookAuthorisationEventModel authorisationEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.AUTHORISATION).getContent(), WebhookAuthorisationEventModel.class);

        final WebhookSettlementEventModel settlementEvent =
                new Gson().fromJson(webhookResponse.get(WebhookType.SETTLEMENT).getContent(), WebhookSettlementEventModel.class);

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);

        assertAll("Auth Forwarding Event",
                // AUTH FORWARDING BODY
                () -> assertEquals("AUTHORISED", authForwardingEvent.getAuthorisationType()),
                () -> assertEquals(availableToSpend, (int) Double.parseDouble(authForwardingEvent.getAvailableToSpend().get(0).getValue().getAmount())),
                () -> assertEquals(createCorporateModel.getBaseCurrency(), authForwardingEvent.getAvailableToSpend().get(0).getValue().getCurrency()),
                () -> assertEquals(LimitInterval.ALWAYS.name(), authForwardingEvent.getAvailableToSpend().get(0).getInterval()),
                () -> assertEquals(managedCardId, authForwardingEvent.getCardId()),
                () -> assertTrue(authForwardingEvent.isCardPresent()),
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
        assertAdditionalMerchantDataAuthForwardingEvent(authForwardingEvent);

        assertAll("Authorisation Event",
                ()-> assertTrue(authorisationEvent.isApproved()),
                ()-> assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason()),
                ()-> assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType()),
                ()-> assertEquals("APPROVE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
                ()-> assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount"))),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getCardPresent())),
                ()-> assertEquals(CardholderPresent.PRESENT.name(), authorisationEvent.getCardholderPresent()),
                ()-> assertEquals("NO_REASON", authorisationEvent.getDeclineReason()),
                ()-> assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId()),
                ()-> assertEquals((int) (availableToSpend - purchaseAmount), Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount"))),
                ()-> assertEquals("ALWAYS", authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval")),
                ()->  assertEquals(managedCardId, authorisationEvent.getId().get("id")),
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

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.AuthForwardingEvent, authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsAuthorisationEvent, webhookResponse.get(WebhookType.AUTHORISATION).getContent());
        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, ApiSchemaDefinition.ManagedCardsSettlementEvent, webhookResponse.get(WebhookType.SETTLEMENT).getContent());
    }

    private void assertAuthForwardingNotTriggered(final Map<String, String> authorisation) {
        assertAll("Auth Forwarding not triggered",
                () -> assertEquals("0",authorisation.get("is_auth_forwarding_triggered")),
                () -> assertEquals("0",authorisation.get("is_auth_forwarding_approved")),
                () -> assertEquals("0",authorisation.get("is_auth_forwarding_timeout_triggered"))
        );
    }

    private void assertHeadersAuthForwardingEvent(Map<WebhookType, WebhookDataResponse> authForwardingWebhookResponse) {
        assertAll("Headers",
                () -> assertNotNull(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getHeaders().getWebhooksKey().get(0)),
                () -> assertEquals("application/json", authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getHeaders().getContentType().get(0)),
                () -> assertNotNull(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getHeaders().getSignature().get(0)),
                () -> assertNotNull(authForwardingWebhookResponse.get(WebhookType.AUTH_FORWARDING).getHeaders().getCallRef().get(0))
        );
    }

    private void assertAdditionalMerchantDataAuthForwardingEvent(WebhookAuthForwardingDebitEventModel authForwardingEvent) {
        assertAll("Additional Merchant Data",
                () -> assertEquals("Merchant City", authForwardingEvent.getMerchantData().get("merchantCity")),
                () -> assertEquals("Merchant Contact", authForwardingEvent.getMerchantData().get("merchantContact")),
                () -> assertEquals("123456", authForwardingEvent.getMerchantData().get("merchantId")),
                () -> assertEquals("Merchant Name Other", authForwardingEvent.getMerchantData().get("merchantNameOther")),
                () -> assertEquals("Merchant Network Id", authForwardingEvent.getMerchantData().get("merchantNetworkId")),
                () -> assertEquals("Merchant Postal Code", authForwardingEvent.getMerchantData().get("merchantPostalCode")),
                () -> assertEquals("MT", authForwardingEvent.getMerchantData().get("merchantState")),
                () -> assertEquals("Merchant Street", authForwardingEvent.getMerchantData().get("merchantStreet")),
                () -> assertEquals("+35621565369", authForwardingEvent.getMerchantData().get("merchantTelephone")),
                () -> assertEquals("https://amazon.com", authForwardingEvent.getMerchantData().get("merchantURL"))
        );
    }

    private void assertAdditionalMerchantDataAuthForwardingEvent(WebhookAuthForwardingPrepaidEventModel authForwardingEvent) {
        assertAll("Additional Merchant Data",
                () -> assertEquals("Merchant City", authForwardingEvent.getMerchantData().get("merchantCity")),
                () -> assertEquals("Merchant Contact", authForwardingEvent.getMerchantData().get("merchantContact")),
                () -> assertEquals("123456", authForwardingEvent.getMerchantData().get("merchantId")),
                () -> assertEquals("Merchant Name Other", authForwardingEvent.getMerchantData().get("merchantNameOther")),
                () -> assertEquals("Merchant Network Id", authForwardingEvent.getMerchantData().get("merchantNetworkId")),
                () -> assertEquals("Merchant Postal Code", authForwardingEvent.getMerchantData().get("merchantPostalCode")),
                () -> assertEquals("MT", authForwardingEvent.getMerchantData().get("merchantState")),
                () -> assertEquals("Merchant Street", authForwardingEvent.getMerchantData().get("merchantStreet")),
                () -> assertEquals("+35621565369", authForwardingEvent.getMerchantData().get("merchantTelephone")),
                () -> assertEquals("https://amazon.com", authForwardingEvent.getMerchantData().get("merchantURL"))
        );
    }

    private void assertAuthorisationEvent(final WebhookAuthorisationEventModel authorisationEvent,
                                          final SimulateCardPurchaseByIdModel cardPurchase,
                                          final CurrencyAmount transactionAmount,
                                          final String managedCardId,
                                          final String ownerId,
                                          final String ownerType,
                                          final CardDetailsModel cardDetailsModel) throws SQLException {

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        assertAll("Authorisation Event",
                ()-> assertTrue(authorisationEvent.isApproved()),
                ()-> assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason()),
                ()-> assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType()),
                ()-> assertEquals("APPROVE", authorisationEvent.getAuthForwardingDetails().get("authForwardingDecisionOutcome")),
                ()-> assertTrue(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingTriggered"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getAuthForwardingDetails().get("authForwardingInnovatorTimedOut"))),
                ()-> assertEquals(transactionAmount.getCurrency(), authorisationEvent.getAvailableBalance().get("currency")),
                ()-> assertEquals(cardDetailsModel.getCardMode().equals(CardMode.DEBIT) ? 0 : ((PrepaidCardModeDetailsModel) cardDetailsModel).getAvailableBalance(),
                    Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount"))),
                ()-> assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent())),
                ()-> assertEquals(CardholderPresent.PRESENT.name(), authorisationEvent.getCardholderPresent()),
                ()-> assertEquals("NO_REASON", authorisationEvent.getDeclineReason()),
                ()-> assertEquals(managedCardId, authorisationEvent.getId().get("id")),
                ()-> assertEquals("managed_cards", authorisationEvent.getId().get("type")),
                ()-> assertEquals("5399", authorisationEvent.getMerchantCategoryCode()),
                ()-> assertEquals("Amazon IT", authorisationEvent.getMerchantName()),
                ()-> assertEquals(ownerId, authorisationEvent.getOwner().get("id")),
                ()-> assertEquals(ownerType, authorisationEvent.getOwner().get("type")),
                ()-> assertEquals("0", authorisationEvent.getRelatedAuthorisationId()),
                ()-> assertEquals(cardPurchase.getTransactionAmount().getCurrency(), authorisationEvent.getSourceAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(cardPurchase.getTransactionAmount().getAmount()), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount"))),
                ()-> assertEquals(transactionAmount.getCurrency(), authorisationEvent.getTransactionAmount().get("currency")),
                ()-> assertEquals(Math.negateExact(transactionAmount.getAmount()), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount"))),
                ()-> assertEquals(authorisation.get("id"), authorisationEvent.getTransactionId()),
                ()-> assertEquals(authorisation.get("auth_code"), authorisationEvent.getAuthCode()),
                ()-> assertNotNull(authorisationEvent.getTransactionTimestamp()),
                ()-> assertEquals(transactionAmount.getCurrency(), authorisationEvent.getForexFee().get("currency")),
                ()-> assertEquals(cardPurchase.getForexFee(), Integer.parseInt(authorisationEvent.getForexFee().get("amount"))),
                ()-> assertEquals(transactionAmount.getCurrency(), authorisationEvent.getForexPadding().get("currency")),
                ()-> assertEquals(cardPurchase.getForexPadding(), Integer.parseInt(authorisationEvent.getForexPadding().get("amount")))
        );
        if (cardDetailsModel.getCardMode().equals(CardMode.DEBIT)){

            final DebitCardModeDetailsModel debitCard = (DebitCardModeDetailsModel) cardDetailsModel;

            assertAll("Authorisation Event - Debit Mode",
                    ()-> assertEquals(debitCard.getManagedAccountId(), authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId()),
                    ()-> assertEquals((int) debitCard.getAvailableToSpend(), Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount"))),
                    ()-> assertEquals(debitCard.getInterval(), authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"))
            );
        } else {
            final PrepaidCardModeDetailsModel prepaidCard = (PrepaidCardModeDetailsModel) cardDetailsModel;

            assertAll("Authorisation Event - Prepaid Mode",
                    ()-> assertEquals(prepaidCard.getCurrency(), authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency()),
                    ()-> assertEquals((int) prepaidCard.getAvailableBalance(), Integer.parseInt(authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()))
            );
        }
    }

    private void assertSettlementEvent(final WebhookSettlementEventModel settlementEvent,
                                       final WebhookAuthorisationEventModel authorisationEvent,
                                       final SimulateCardPurchaseByIdModel cardPurchase,
                                       final CurrencyAmount transactionAmount,
                                       final Long purchaseFee,
                                       final String managedCardId,
                                       final String ownerId,
                                       final String ownerType,
                                       final CardDetailsModel cardDetailsModel) throws SQLException {

        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);

        assertAll("Settlement Event",
                () -> assertEquals(transactionAmount.getCurrency(), settlementEvent.getAvailableBalance().get("currency")),
                () -> assertEquals(cardDetailsModel.getCardMode().equals(CardMode.DEBIT) ? 0 : ((PrepaidCardModeDetailsModel) cardDetailsModel).getAvailableBalance(),
                        Integer.parseInt(settlementEvent.getAvailableBalance().get("amount"))),
                () -> assertEquals(transactionAmount.getCurrency(), settlementEvent.getFeeAmount().get("currency")),
                () -> assertEquals(purchaseFee.intValue(), Integer.parseInt(settlementEvent.getFeeAmount().get("amount"))),
                () -> assertEquals(managedCardId, settlementEvent.getId().get("id")),
                () -> assertEquals("managed_cards", settlementEvent.getId().get("type")),
                () -> assertEquals("5399", settlementEvent.getMerchantCategoryCode()),
                () -> assertEquals("Amazon IT", settlementEvent.getMerchantName()),
                () -> assertEquals(ownerId, settlementEvent.getOwner().get("id")),
                () -> assertEquals(ownerType, settlementEvent.getOwner().get("type")),
                () -> assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId()),
                () -> assertEquals("SALE_PURCHASE", settlementEvent.getSettlementType()),
                () -> assertEquals(cardPurchase.getTransactionAmount().getCurrency(), settlementEvent.getSourceAmount().get("currency")),
                () -> assertEquals(Math.negateExact(cardPurchase.getTransactionAmount().getAmount()), Integer.parseInt(settlementEvent.getSourceAmount().get("amount"))),
                () -> assertEquals(transactionAmount.getCurrency(), settlementEvent.getTransactionAmount().get("currency")),
                () -> assertEquals(Math.negateExact(transactionAmount.getAmount()), Integer.parseInt(settlementEvent.getTransactionAmount().get("amount"))),
                () -> assertEquals(settlement.get("id"), settlementEvent.getTransactionId()),
                () -> assertNotNull(settlementEvent.getTransactionTimestamp()),
                () -> assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode()),
                () -> assertEquals(transactionAmount.getCurrency(), settlementEvent.getForexFee().get("currency")),
                () -> assertEquals(cardPurchase.getForexFee(), Integer.parseInt(settlementEvent.getForexFee().get("amount")))
        );
        if (cardDetailsModel.getCardMode() == CardMode.DEBIT) {
            final DebitCardModeDetailsModel debitCard = (DebitCardModeDetailsModel) cardDetailsModel;

            assertAll("Settlement Event - Debit Mode",
                    () -> assertEquals(debitCard.getManagedAccountId(), settlementEvent.getDetails().getDebitModeDetails().getParentManagedAccountId()),
                    () -> assertEquals((int) debitCard.getAvailableToSpend(), Integer.parseInt(settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount"))),
                    () -> assertEquals(debitCard.getInterval(), settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"))
            );
        } else {
            final PrepaidCardModeDetailsModel prepaidCard = (PrepaidCardModeDetailsModel) cardDetailsModel;

            assertAll("Settlement Event - Prepaid Mode",
                    () -> assertEquals(prepaidCard.getCurrency(), settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency()),
                    () -> assertEquals((int) prepaidCard.getAvailableBalance(), Integer.parseInt(settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()))
            );
        }
    }

    private static void setupManagedCardProfiles(final boolean authForwardingEnabled, final String defaultTimeoutDecision) {
        corporateDebitManagedCardProfileId = getCorporateDebitManagedCardProfileId(authForwardingEnabled, defaultTimeoutDecision);
        corporatePrepaidManagedCardProfileId = getCorporatePrepaidManagedCardProfileId(authForwardingEnabled, defaultTimeoutDecision);
        consumerDebitManagedCardProfileId = getConsumerDebitManagedCardProfileId(authForwardingEnabled, defaultTimeoutDecision);
        consumerPrepaidManagedCardProfileId = getConsumerPrepaidManagedCardProfileId(authForwardingEnabled, defaultTimeoutDecision);
    }
}