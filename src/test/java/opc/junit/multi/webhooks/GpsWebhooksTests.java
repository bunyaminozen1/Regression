package opc.junit.multi.webhooks;

import commons.enums.Currency;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.CardMode;
import opc.enums.opc.FeeType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.WebhookType;
import opc.junit.database.GpsSimulatorDatabaseHelper;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CardDetailsModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.DebitCardModeDetailsModel;
import opc.models.multi.managedcards.PrepaidCardModeDetailsModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.CurrencyAmount;
import opc.models.simulator.SimulateCardAuthModel;
import opc.models.simulator.SimulateCardMerchantRefundByIdModel;
import opc.models.simulator.SimulateCardPurchaseByIdModel;
import opc.models.simulator.SimulateOctMerchantRefundModel;
import opc.models.webhook.WebhookAuthorisationEventModel;
import opc.models.webhook.WebhookDataResponse;
import opc.models.webhook.WebhookSettlementEventModel;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(MultiTags.GPS_WEBHOOKS)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public class GpsWebhooksTests extends BaseWebhooksSetup {

    @Test
    public void Webhooks_PrepaidCardPurchase_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long depositAmount = 500L;
        final long purchaseAmount = 10L;
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

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        final WebhookSettlementEventModel settlementEvent =
                getSettlementWebhookResponse(timestamp, corporate.getLeft());

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);

        assertTrue(authorisationEvent.isApproved());
        assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals((int) remainingBalanceAfterAuth, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("NO_REASON", authorisationEvent.getDeclineReason());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals((int) remainingBalanceAfterAuth, Integer.parseInt(authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals("Amazon IT", authorisationEvent.getMerchantName());
        assertEquals("123456789", authorisationEvent.getMerchantId());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), authorisationEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), authorisationEvent.getAuthCode());
        assertNotNull(authorisationEvent.getTransactionTimestamp());

        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getAvailableBalance().get("currency"));
        assertEquals((int) remainingBalanceAfterSettlement, Integer.parseInt(settlementEvent.getAvailableBalance().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals((int) remainingBalanceAfterSettlement, Integer.parseInt(settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getFeeAmount().get("currency"));
        assertEquals(purchaseFee.intValue(), Integer.parseInt(settlementEvent.getFeeAmount().get("amount")));
        assertEquals(managedCardId, settlementEvent.getId().get("id"));
        assertEquals("managed_cards", settlementEvent.getId().get("type"));
        assertEquals("5399", settlementEvent.getMerchantCategoryCode());
        assertEquals("Amazon IT", settlementEvent.getMerchantName());
        assertEquals("123456789", settlementEvent.getMerchantId());
        assertEquals("COMPLETED", settlementEvent.getState());
        assertEquals(corporate.getLeft(), settlementEvent.getOwner().get("id"));
        assertEquals("corporates", settlementEvent.getOwner().get("type"));
        assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId());
        assertEquals("SALE_PURCHASE", settlementEvent.getSettlementType());
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getTransactionAmount().get("amount")));
        assertEquals(settlement.get("id"), settlementEvent.getTransactionId());
        assertNotNull(settlementEvent.getTransactionTimestamp());
        assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode());
    }

    @Test
    public void Webhooks_DebitCardPurchase_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 1000L;
        final long depositAmount = 500L;
        final long purchaseAmount = 10L;
        final Long depositFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long purchaseFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.PURCHASE_FEE).getAmount();

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency()).build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, corporate.getRight());

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
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        final WebhookSettlementEventModel settlementEvent =
                getSettlementWebhookResponse(timestamp, corporate.getLeft());

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<String, String> settlement = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId).get(0);

        assertTrue(authorisationEvent.isApproved());
        assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("NO_REASON", authorisationEvent.getDeclineReason());
        assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
        assertEquals((int) (availableToSpend - purchaseAmount), Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
        assertEquals("ALWAYS", authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals("Amazon IT", authorisationEvent.getMerchantName());
        assertEquals("123456789", authorisationEvent.getMerchantId());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), authorisationEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), authorisationEvent.getAuthCode());
        assertNotNull(authorisationEvent.getTransactionTimestamp());

        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getAvailableBalance().get("currency"));
        assertEquals(0, Integer.parseInt(settlementEvent.getAvailableBalance().get("amount")));
        assertEquals(managedAccountId, settlementEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
        assertEquals((int) (availableToSpend - purchaseAmount - purchaseFee), Integer.parseInt(settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
        assertEquals("ALWAYS", settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getFeeAmount().get("currency"));
        assertEquals(purchaseFee.intValue(), Integer.parseInt(settlementEvent.getFeeAmount().get("amount")));
        assertEquals(managedCardId, settlementEvent.getId().get("id"));
        assertEquals("managed_cards", settlementEvent.getId().get("type"));
        assertEquals("5399", settlementEvent.getMerchantCategoryCode());
        assertEquals("Amazon IT", settlementEvent.getMerchantName());
        assertEquals("123456789", settlementEvent.getMerchantId());
        assertEquals("COMPLETED", settlementEvent.getState());
        assertEquals(corporate.getLeft(), settlementEvent.getOwner().get("id"));
        assertEquals("corporates", settlementEvent.getOwner().get("type"));
        assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId());
        assertEquals("SALE_PURCHASE", settlementEvent.getSettlementType());
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getTransactionAmount().get("amount")));
        assertEquals(settlement.get("id"), settlementEvent.getTransactionId());
        assertNotNull(settlementEvent.getTransactionTimestamp());
        assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode());
    }

    @Test
    public void Webhooks_PurchaseExceedingAvailableToSpendLimit_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 10L;
        final long depositAmount = 500L;
        final long purchaseAmount = 100L;
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

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        assertFalse(authorisationEvent.isApproved());
        assertEquals("SPEND_LIMIT_EXCEEDED", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("DECLINED", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("AUTH_RULE_CHECKS_FAILED", authorisationEvent.getDeclineReason());
        assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
        assertEquals((int) availableToSpend, Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
        assertEquals("ALWAYS", authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals("Amazon IT", authorisationEvent.getMerchantName());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertNotNull(authorisationEvent.getTransactionId());
        assertNotNull(authorisationEvent.getTransactionTimestamp());
        assertNotNull(authorisationEvent.getAuthCode());
    }

    @Test
    public void Webhooks_MerchantCategoryBlocked_DeniedSpendControl() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 1000L;
        final long depositAmount = 500L;
        final long purchaseAmount = 100L;
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

        final SpendRulesModel spendRulesModel =
                getDefaultSpendRulesModel(Collections.singletonList(
                        new SpendLimitModel(new CurrencyAmount(createCorporateModel.getBaseCurrency(), availableToSpend), LimitInterval.ALWAYS)))
                        .setBlockedMerchantCategories(Collections.singletonList("5399")).build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        ManagedCardsHelper.simulatePurchase(secretKey,
                managedCardId,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount));

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        assertFalse(authorisationEvent.isApproved());
        assertEquals("MERCHANT_CATEGORY_BLOCKED", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("DECLINED", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("AUTH_RULE_CHECKS_FAILED", authorisationEvent.getDeclineReason());
        assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
        assertEquals((int) availableToSpend, Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
        assertEquals("ALWAYS", authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals("Amazon IT", authorisationEvent.getMerchantName());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertNotNull(authorisationEvent.getTransactionId());
        assertNotNull(authorisationEvent.getTransactionTimestamp());
        assertNotNull(authorisationEvent.getAuthCode());
    }

    @Test
    public void Webhooks_MerchantCategoryNotAllowed_DeniedSpendControl() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long depositAmount = 500L;
        final long purchaseAmount = 100L;

        final String managedCardId =
                ManagedCardsHelper
                        .createManagedCard(CreateManagedCardModel
                                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardProfileId,
                                        createCorporateModel.getBaseCurrency()).build(), secretKey, corporate.getRight());

        TestHelper.simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountProfileId,
                transfersProfileId, managedCardId, createCorporateModel.getBaseCurrency(),
                depositAmount, secretKey, corporate.getRight());

        final SpendRulesModel spendRulesModel =
                SpendRulesModel.builder()
                        .setAllowedMerchantCategories(Collections.singletonList("9999"))
                        .setBlockedMerchantCategories(new ArrayList<>())
                        .setAllowedMerchantIds(new ArrayList<>())
                        .setBlockedMerchantIds(new ArrayList<>())
                        .setSpendLimit(new ArrayList<>())
                        .setAllowAtm(false)
                        .setAllowContactless(false)
                        .setAllowCashback(false)
                        .setAllowCreditAuthorisations(false)
                        .setAllowECommerce(false)
                        .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        ManagedCardsHelper.simulatePurchase(secretKey,
                managedCardId,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount));

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        assertFalse(authorisationEvent.isApproved());
        assertEquals("MERCHANT_CATEGORY_NOT_IN_ALLOWED_LIST", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("DECLINED", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals((int) depositAmount, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("AUTH_RULE_CHECKS_FAILED", authorisationEvent.getDeclineReason());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals((int) depositAmount, Integer.parseInt(authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals("Amazon IT", authorisationEvent.getMerchantName());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertNotNull(authorisationEvent.getTransactionId());
        assertNotNull(authorisationEvent.getTransactionTimestamp());
        assertNotNull(authorisationEvent.getAuthCode());
    }

    @Test
    public void Webhooks_DebitOctThroughMerchantRefund_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 10000L;
        final long depositAmount = 5000L;
        final long purchaseAmount = 1000L;
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
        final SimulateOctMerchantRefundModel simulateOctMerchantRefundModel =
                new SimulateOctMerchantRefundModel("OCT Merchant", new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount));
        SimulatorHelper.simulateOctThroughMerchantRefund(simulateOctMerchantRefundModel, innovatorId, managedCardId);

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        final WebhookSettlementEventModel settlementEvent =
                getSettlementWebhookResponse(timestamp, corporate.getLeft());

        assertTrue(authorisationEvent.isApproved());
        assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("AUTHORISED_CREDIT", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("NOT_PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("NO_REASON", authorisationEvent.getDeclineReason());
        assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
        assertEquals((int) availableToSpend, Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
        assertEquals("ALWAYS", authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals(simulateOctMerchantRefundModel.getMerchantName(), authorisationEvent.getMerchantName());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertNotNull(authorisationEvent.getTransactionId());
        assertNotNull(authorisationEvent.getTransactionTimestamp());
        assertNotNull(authorisationEvent.getAuthCode());

        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getAvailableBalance().get("currency"));
        assertEquals(0, Integer.parseInt(settlementEvent.getAvailableBalance().get("amount")));
        assertEquals(managedAccountId, settlementEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
        assertEquals((int) availableToSpend, Integer.parseInt(settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
        assertEquals("ALWAYS", settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getFeeAmount().get("currency"));
        assertEquals(0, Integer.parseInt(settlementEvent.getFeeAmount().get("amount")));
        assertEquals(managedCardId, settlementEvent.getId().get("id"));
        assertEquals("managed_cards", settlementEvent.getId().get("type"));
        assertEquals("5399", settlementEvent.getMerchantCategoryCode());
        assertEquals(simulateOctMerchantRefundModel.getMerchantName(), settlementEvent.getMerchantName());
        assertEquals(corporate.getLeft(), settlementEvent.getOwner().get("id"));
        assertEquals("corporates", settlementEvent.getOwner().get("type"));
        assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId());
        assertEquals("ORIGINAL_CREDIT_TRANSACTION", settlementEvent.getSettlementType());
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getSourceAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(settlementEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getTransactionAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(settlementEvent.getTransactionAmount().get("amount")));
        assertNotNull(settlementEvent.getTransactionId());
        assertNotNull(settlementEvent.getTransactionTimestamp());
        assertNotNull(settlementEvent.getAuthCode());
        assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode());
    }

    @Test
    public void Webhooks_PrepaidOctThroughMerchantRefund_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long depositAmount = 500L;
        final long purchaseAmount = 100L;

        final String managedCardId =
                ManagedCardsHelper
                        .createManagedCard(CreateManagedCardModel
                                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardProfileId,
                                        createCorporateModel.getBaseCurrency()).build(), secretKey, corporate.getRight());

        TestHelper
                .simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountProfileId, transfersProfileId, managedCardId,
                        createCorporateModel.getBaseCurrency(), depositAmount, secretKey, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        final SimulateOctMerchantRefundModel simulateOctMerchantRefundModel =
                new SimulateOctMerchantRefundModel("OCT Merchant", new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount));
        SimulatorHelper.simulateOctThroughMerchantRefund(simulateOctMerchantRefundModel, innovatorId, managedCardId);

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        final WebhookSettlementEventModel settlementEvent =
                getSettlementWebhookResponse(timestamp, corporate.getLeft());

        assertTrue(authorisationEvent.isApproved());
        assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("AUTHORISED_CREDIT", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals((int) (depositAmount), Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("NOT_PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("NO_REASON", authorisationEvent.getDeclineReason());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals((int) (depositAmount), Integer.parseInt(authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals(simulateOctMerchantRefundModel.getMerchantName(), authorisationEvent.getMerchantName());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertNotNull(authorisationEvent.getTransactionId());
        assertNotNull(authorisationEvent.getTransactionTimestamp());
        assertNotNull(authorisationEvent.getAuthCode());

        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getAvailableBalance().get("currency"));
        assertEquals((int) (depositAmount + purchaseAmount), Integer.parseInt(settlementEvent.getAvailableBalance().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals((int) (depositAmount + purchaseAmount), Integer.parseInt(settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getFeeAmount().get("currency"));
        assertEquals(0, Integer.parseInt(settlementEvent.getFeeAmount().get("amount")));
        assertEquals(managedCardId, settlementEvent.getId().get("id"));
        assertEquals("managed_cards", settlementEvent.getId().get("type"));
        assertEquals("5399", settlementEvent.getMerchantCategoryCode());
        assertEquals(simulateOctMerchantRefundModel.getMerchantName(), settlementEvent.getMerchantName());
        assertEquals(corporate.getLeft(), settlementEvent.getOwner().get("id"));
        assertEquals("corporates", settlementEvent.getOwner().get("type"));
        assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId());
        assertEquals("ORIGINAL_CREDIT_TRANSACTION", settlementEvent.getSettlementType());
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getSourceAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(settlementEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getTransactionAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(settlementEvent.getTransactionAmount().get("amount")));
        assertNotNull(settlementEvent.getTransactionId());
        assertNotNull(settlementEvent.getTransactionTimestamp());
        assertNotNull(settlementEvent.getAuthCode());
        assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode());
    }

    @Test
    public void Webhooks_DebitAuthorisationDeclinedThroughOverruling_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 1000L;
        final long depositAmount = 500L;
        final long purchaseAmount = 10L;
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
        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(createManagedAccountModel.getCurrency(), purchaseAmount))
                        .setIsOverruled(true)
                        .setOverrulingNotificationDelaySeconds(0)
                        .build();

        SimulatorHelper.simulateCancelledAuthorisation(innovatorId, simulateCardAuthModel, managedCardId);

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        assertFalse(authorisationEvent.isApproved());
        assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("DECLINED", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals(0, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("TIMEOUT", authorisationEvent.getDeclineReason());
        assertEquals(managedAccountId, authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
        assertEquals(availableToSpend, Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
        assertEquals("ALWAYS", authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals(String.format("old_simulator_%s", createManagedAccountModel.getCurrency().toLowerCase()), authorisationEvent.getMerchantName());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), authorisationEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), authorisationEvent.getAuthCode());
        assertNotNull(authorisationEvent.getTransactionTimestamp());
    }

    @Test
    public void Webhooks_PrepaidAuthorisationDeclinedThroughOverruling_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long depositAmount = 500L;
        final long purchaseAmount = 10L;
        final String managedCardId =
                ManagedCardsHelper
                        .createManagedCard(CreateManagedCardModel
                                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardProfileId,
                                        createCorporateModel.getBaseCurrency()).build(), secretKey, corporate.getRight());

        TestHelper.simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountProfileId,
                transfersProfileId, managedCardId, createCorporateModel.getBaseCurrency(),
                depositAmount, secretKey, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount))
                        .setIsOverruled(true)
                        .setOverrulingNotificationDelaySeconds(0)
                        .build();

        SimulatorHelper.simulateCancelledAuthorisation(innovatorId, simulateCardAuthModel, managedCardId);

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        assertFalse(authorisationEvent.isApproved());
        assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("DECLINED", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals(depositAmount, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("TIMEOUT", authorisationEvent.getDeclineReason());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals(depositAmount, Integer.parseInt(authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals(String.format("old_simulator_%s", createCorporateModel.getBaseCurrency().toLowerCase()), authorisationEvent.getMerchantName());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), authorisationEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), authorisationEvent.getAuthCode());
        assertNotNull(authorisationEvent.getTransactionTimestamp());
    }

    @Test
    public void Webhooks_DebitAuthorisationCancelledThroughDelayedOverruling_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long availableToSpend = 1000L;
        final long depositAmount = 500L;
        final long purchaseAmount = 10L;
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
        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(createManagedAccountModel.getCurrency(), purchaseAmount))
                        .setIsOverruled(true)
                        .setOverrulingNotificationDelaySeconds(20)
                        .build();

        SimulatorHelper.simulateCancelledAuthorisation(innovatorId, simulateCardAuthModel, managedCardId);

        final List<WebhookAuthorisationEventModel> authorisationEvents =
                getAuthorisationWebhookResponses(timestamp, corporate.getLeft(), 2);

        authorisationEvents.sort(Comparator.comparing(WebhookAuthorisationEventModel::getTransactionTimestamp));

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        final WebhookAuthorisationEventModel authorisedEvent = authorisationEvents.get(0);

        final WebhookAuthorisationEventModel cancelledEvent = authorisationEvents.get(1);

        assertTrue(authorisedEvent.isApproved());
        assertEquals("NONE", authorisedEvent.getAuthRuleFailedReason());
        assertEquals("AUTHORISED", authorisedEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisedEvent.getAvailableBalance().get("currency"));
        assertEquals(0, Integer.parseInt(authorisedEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisedEvent.getCardPresent()));
        assertEquals("PRESENT", authorisedEvent.getCardholderPresent());
        assertEquals("NO_REASON", authorisedEvent.getDeclineReason());
        assertEquals(managedAccountId, authorisedEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
        assertEquals((int) (availableToSpend - purchaseAmount), Integer.parseInt(authorisedEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
        assertEquals("ALWAYS", authorisedEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        assertEquals(managedCardId, authorisedEvent.getId().get("id"));
        assertEquals("managed_cards", authorisedEvent.getId().get("type"));
        assertEquals("5399", authorisedEvent.getMerchantCategoryCode());
        assertEquals(String.format("old_simulator_%s", createManagedAccountModel.getCurrency().toLowerCase()), authorisedEvent.getMerchantName());
        assertEquals(corporate.getLeft(), authorisedEvent.getOwner().get("id"));
        assertEquals("corporates", authorisedEvent.getOwner().get("type"));
        assertEquals("0", authorisedEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisedEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisedEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisedEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisedEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), authorisedEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), authorisedEvent.getAuthCode());
        assertNotNull(authorisedEvent.getTransactionTimestamp());

        assertFalse(cancelledEvent.isApproved());
        assertEquals("NONE", cancelledEvent.getAuthRuleFailedReason());
        assertEquals("CANCELLED", cancelledEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), cancelledEvent.getAvailableBalance().get("currency"));
        assertEquals(0, Integer.parseInt(cancelledEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(cancelledEvent.getCardPresent()));
        assertEquals("PRESENT", cancelledEvent.getCardholderPresent());
        assertEquals("TIMEOUT", cancelledEvent.getDeclineReason());
        assertEquals(managedAccountId, cancelledEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
        assertEquals(availableToSpend, Integer.parseInt(cancelledEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
        assertEquals("ALWAYS", cancelledEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        assertEquals(managedCardId, cancelledEvent.getId().get("id"));
        assertEquals("managed_cards", cancelledEvent.getId().get("type"));
        assertEquals("5399", cancelledEvent.getMerchantCategoryCode());
        assertEquals(String.format("old_simulator_%s", createManagedAccountModel.getCurrency().toLowerCase()), cancelledEvent.getMerchantName());
        assertEquals(corporate.getLeft(), cancelledEvent.getOwner().get("id"));
        assertEquals("corporates", cancelledEvent.getOwner().get("type"));
        assertEquals("0", cancelledEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), cancelledEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(cancelledEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), cancelledEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(cancelledEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), cancelledEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), cancelledEvent.getAuthCode());
        assertNotNull(cancelledEvent.getTransactionTimestamp());
    }

    @Test
    public void Webhooks_PrepaidAuthorisationCancelledThroughDelayedOverruling_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long depositAmount = 500L;
        final long purchaseAmount = 10L;
        final String managedCardId =
                ManagedCardsHelper
                        .createManagedCard(CreateManagedCardModel
                                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardProfileId,
                                        createCorporateModel.getBaseCurrency()).build(), secretKey, corporate.getRight());

        TestHelper.simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountProfileId,
                transfersProfileId, managedCardId, createCorporateModel.getBaseCurrency(),
                depositAmount, secretKey, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();
        final SimulateCardAuthModel simulateCardAuthModel =
                SimulateCardAuthModel.DefaultCardAuthModel(new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount))
                        .setIsOverruled(true)
                        .setOverrulingNotificationDelaySeconds(20)
                        .build();

        SimulatorHelper.simulateCancelledAuthorisation(innovatorId, simulateCardAuthModel, managedCardId);

        final List<WebhookAuthorisationEventModel> authorisationEvents =
                getAuthorisationWebhookResponses(timestamp, corporate.getLeft(), 2);

        authorisationEvents.sort(Comparator.comparing(WebhookAuthorisationEventModel::getTransactionTimestamp));

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        final WebhookAuthorisationEventModel authorisedEvent = authorisationEvents.get(0);

        final WebhookAuthorisationEventModel cancelledEvent = authorisationEvents.get(1);

        assertTrue(authorisedEvent.isApproved());
        assertEquals("NONE", authorisedEvent.getAuthRuleFailedReason());
        assertEquals("AUTHORISED", authorisedEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisedEvent.getAvailableBalance().get("currency"));
        assertEquals((int) (depositAmount - purchaseAmount), Integer.parseInt(authorisedEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisedEvent.getCardPresent()));
        assertEquals("PRESENT", authorisedEvent.getCardholderPresent());
        assertEquals("NO_REASON", authorisedEvent.getDeclineReason());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisedEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals((int) (depositAmount - purchaseAmount), Integer.parseInt(authorisedEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(managedCardId, authorisedEvent.getId().get("id"));
        assertEquals("managed_cards", authorisedEvent.getId().get("type"));
        assertEquals("5399", authorisedEvent.getMerchantCategoryCode());
        assertEquals(String.format("old_simulator_%s", createCorporateModel.getBaseCurrency().toLowerCase()), authorisedEvent.getMerchantName());
        assertEquals(corporate.getLeft(), authorisedEvent.getOwner().get("id"));
        assertEquals("corporates", authorisedEvent.getOwner().get("type"));
        assertEquals("0", authorisedEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisedEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisedEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisedEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisedEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), authorisedEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), authorisedEvent.getAuthCode());
        assertNotNull(authorisedEvent.getTransactionTimestamp());

        assertFalse(cancelledEvent.isApproved());
        assertEquals("NONE", cancelledEvent.getAuthRuleFailedReason());
        assertEquals("CANCELLED", cancelledEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), cancelledEvent.getAvailableBalance().get("currency"));
        assertEquals(depositAmount, Integer.parseInt(cancelledEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(cancelledEvent.getCardPresent()));
        assertEquals("PRESENT", cancelledEvent.getCardholderPresent());
        assertEquals("TIMEOUT", cancelledEvent.getDeclineReason());
        assertEquals(createCorporateModel.getBaseCurrency(), cancelledEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals(depositAmount, Integer.parseInt(cancelledEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(managedCardId, cancelledEvent.getId().get("id"));
        assertEquals("managed_cards", cancelledEvent.getId().get("type"));
        assertEquals("5399", cancelledEvent.getMerchantCategoryCode());
        assertEquals(String.format("old_simulator_%s", createCorporateModel.getBaseCurrency().toLowerCase()), cancelledEvent.getMerchantName());
        assertEquals(corporate.getLeft(), cancelledEvent.getOwner().get("id"));
        assertEquals("corporates", cancelledEvent.getOwner().get("type"));
        assertEquals("0", cancelledEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), cancelledEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(cancelledEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), cancelledEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(cancelledEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), cancelledEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), cancelledEvent.getAuthCode());
        assertNotNull(cancelledEvent.getTransactionTimestamp());
    }

    @Test
    public void Webhooks_DebitCardForexPurchase_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final long availableToSpend = 10000L;
        final long depositAmount = 500L;
        final long purchaseAmount = 10L;
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

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        final WebhookSettlementEventModel settlementEvent =
                getSettlementWebhookResponse(timestamp, corporate.getLeft());

        final DebitCardModeDetailsModel.Builder debitDetails =
                DebitCardModeDetailsModel.builder().setCardMode(CardMode.DEBIT)
                        .setInterval("ALWAYS")
                        .setManagedAccountId(managedAccountId);

        assertAuthorisationEvent(authorisationEvent, simulateCardPurchaseModel,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), actualPurchaseAmount + simulateCardPurchaseModel.getForexFee() +
                        simulateCardPurchaseModel.getForexPadding()), managedCardId, corporate.getLeft(), "corporates",
                debitDetails.setAvailableToSpend(availableToSpend - actualPurchaseAmount - simulateCardPurchaseModel.getForexFee()).build());

        assertSettlementEvent(settlementEvent, authorisationEvent,
                simulateCardPurchaseModel,
                new CurrencyAmount(createCorporateModel.getBaseCurrency(), actualPurchaseAmount + simulateCardPurchaseModel.getForexFee()),
                purchaseFee, managedCardId, corporate.getLeft(), "corporates",
                debitDetails.setAvailableToSpend(availableToSpend - actualPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee()).build());
    }

    @Test
    public void Webhooks_PrepaidCardForexPurchase_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        final long depositAmount = 500L;
        final long purchaseAmount = 10L;
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
                        .setForexPadding(10L)
                        .setForexFee(5L)
                        .build();

        SimulatorHelper.simulateCardPurchaseById(secretKey,
                managedCardId,
                simulateCardPurchaseModel);

        final long actualPurchaseAmount = Math.abs(Long.parseLong(GpsSimulatorDatabaseHelper.getLatestSettlement().get(0).get("card_amount")));

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, consumer.getLeft());

        final WebhookSettlementEventModel settlementEvent =
                getSettlementWebhookResponse(timestamp, consumer.getLeft());

        final PrepaidCardModeDetailsModel.Builder prepaidDetails =
                PrepaidCardModeDetailsModel.builder().setCardMode(CardMode.PREPAID)
                        .setAvailableBalance(depositAmount)
                        .setCurrency(createConsumerModel.getBaseCurrency());

        assertAuthorisationEvent(authorisationEvent, simulateCardPurchaseModel,
                new CurrencyAmount(createConsumerModel.getBaseCurrency(), actualPurchaseAmount + simulateCardPurchaseModel.getForexFee() +
                        simulateCardPurchaseModel.getForexPadding()), managedCardId, consumer.getLeft(), "consumers",
                prepaidDetails.setAvailableBalance(depositAmount - actualPurchaseAmount - simulateCardPurchaseModel.getForexFee() - simulateCardPurchaseModel.getForexPadding()).build());

        assertSettlementEvent(settlementEvent, authorisationEvent,
                simulateCardPurchaseModel,
                new CurrencyAmount(createConsumerModel.getBaseCurrency(), actualPurchaseAmount + simulateCardPurchaseModel.getForexFee()),
                purchaseFee, managedCardId, consumer.getLeft(), "consumers",
                prepaidDetails.setAvailableBalance(depositAmount - actualPurchaseAmount - purchaseFee - simulateCardPurchaseModel.getForexFee()).build());
    }

    @Test
    public void Webhooks_MerchantRefund_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        final long depositAmount = 500L;
        final long purchaseAmount = 10L;
        final Long purchaseFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.PURCHASE_FEE).getAmount();
        final Long refundFee = TestHelper.getFees(createCorporateModel.getBaseCurrency()).get(FeeType.REFUND_FEE).getAmount();
        final long remainingBalanceAfterAuth = depositAmount - purchaseAmount;
        final long remainingBalanceAfterSettlement = depositAmount - purchaseAmount - purchaseFee;
        final long remainingBalanceAfterRefund = depositAmount - purchaseAmount - purchaseFee + purchaseAmount - refundFee;
        final String managedCardId =
                ManagedCardsHelper
                        .createManagedCard(CreateManagedCardModel
                                .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardProfileId,
                                        createCorporateModel.getBaseCurrency()).build(), secretKey, corporate.getRight());

        TestHelper.simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountProfileId,
                transfersProfileId, managedCardId, createCorporateModel.getBaseCurrency(),
                depositAmount, secretKey, corporate.getRight());

        final long timestamp = Instant.now().toEpochMilli();

        final SimulateCardMerchantRefundByIdModel simulateCardMerchantRefundModel =
                SimulateCardMerchantRefundByIdModel.builder()
                        .setTransactionAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), purchaseAmount))
                        .setMerchantId("123456789")
                        .build();

        SimulatorHelper.simulateMerchantRefundById(secretKey,
                managedCardId,
                simulateCardMerchantRefundModel);

        final WebhookAuthorisationEventModel authorisationEvent =
                getAuthorisationWebhookResponse(timestamp, corporate.getLeft());

        final List<WebhookSettlementEventModel> settlementEvents =
                getSettlementWebhookResponses(timestamp, corporate.getLeft(), 2);

        final WebhookSettlementEventModel settlementEvent =
                settlementEvents.stream().filter(x -> x.getSettlementType().equals("SALE_PURCHASE"))
                        .collect(Collectors.toList()).stream().findFirst().orElseThrow();

        final WebhookSettlementEventModel refundEvent =
                settlementEvents.stream().filter(x -> x.getSettlementType().equals("PURCHASE_REFUND"))
                        .collect(Collectors.toList()).stream().findFirst().orElseThrow();

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);
        final Map<Integer, Map<String, String>> settlements = ManagedCardsDatabaseHelper.getSettlementByCardId(managedCardId);
        final Map<String, String> settlement = settlements.get(0);
        final Map<String, String> refund = settlements.get(1);

        assertTrue(authorisationEvent.isApproved());
        assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals((int) remainingBalanceAfterAuth, Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("NO_REASON", authorisationEvent.getDeclineReason());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals((int) remainingBalanceAfterAuth, Integer.parseInt(authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals("Refundable.com", authorisationEvent.getMerchantName());
        assertEquals("123456789", authorisationEvent.getMerchantId());
        assertEquals(corporate.getLeft(), authorisationEvent.getOwner().get("id"));
        assertEquals("corporates", authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), authorisationEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), authorisationEvent.getAuthCode());
        assertNotNull(authorisationEvent.getTransactionTimestamp());

        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getAvailableBalance().get("currency"));
        assertEquals((int) remainingBalanceAfterSettlement, Integer.parseInt(settlementEvent.getAvailableBalance().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals((int) remainingBalanceAfterSettlement, Integer.parseInt(settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getFeeAmount().get("currency"));
        assertEquals(purchaseFee.intValue(), Integer.parseInt(settlementEvent.getFeeAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getForexFee().get("currency"));
        assertEquals(0, Integer.parseInt(settlementEvent.getForexFee().get("amount")));
        assertEquals(managedCardId, settlementEvent.getId().get("id"));
        assertEquals("managed_cards", settlementEvent.getId().get("type"));
        assertEquals("5399", settlementEvent.getMerchantCategoryCode());
        assertEquals("Refundable.com", settlementEvent.getMerchantName());
        assertEquals("123456789", settlementEvent.getMerchantId());
        assertEquals("COMPLETED", settlementEvent.getState());
        assertEquals(corporate.getLeft(), settlementEvent.getOwner().get("id"));
        assertEquals("corporates", settlementEvent.getOwner().get("type"));
        assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId());
        assertEquals("SALE_PURCHASE", settlementEvent.getSettlementType());
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), settlementEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(purchaseAmount), Integer.parseInt(settlementEvent.getTransactionAmount().get("amount")));
        assertEquals(settlement.get("id"), settlementEvent.getTransactionId());
        assertNotNull(settlementEvent.getTransactionTimestamp());
        assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode());

        assertEquals(createCorporateModel.getBaseCurrency(), refundEvent.getAvailableBalance().get("currency"));
        assertEquals((int) remainingBalanceAfterRefund, Integer.parseInt(refundEvent.getAvailableBalance().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), refundEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
        assertEquals((int) remainingBalanceAfterRefund, Integer.parseInt(refundEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        assertEquals(createCorporateModel.getBaseCurrency(), refundEvent.getFeeAmount().get("currency"));
        assertEquals(refundFee.intValue(), Integer.parseInt(refundEvent.getFeeAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), refundEvent.getForexFee().get("currency"));
        assertEquals(0, Integer.parseInt(refundEvent.getForexFee().get("amount")));
        assertEquals(managedCardId, refundEvent.getId().get("id"));
        assertEquals("managed_cards", refundEvent.getId().get("type"));
        assertEquals("5399", refundEvent.getMerchantCategoryCode());
        assertEquals("Refundable.com", refundEvent.getMerchantName());
        assertEquals("123456789", refundEvent.getMerchantId());
        assertEquals("COMPLETED", refundEvent.getState());
        assertEquals(corporate.getLeft(), refundEvent.getOwner().get("id"));
        assertEquals("corporates", refundEvent.getOwner().get("type"));
        assertEquals("0", refundEvent.getRelatedAuthorisationId());
        assertEquals("PURCHASE_REFUND", refundEvent.getSettlementType());
        assertEquals(createCorporateModel.getBaseCurrency(), refundEvent.getSourceAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(refundEvent.getSourceAmount().get("amount")));
        assertEquals(createCorporateModel.getBaseCurrency(), refundEvent.getTransactionAmount().get("currency"));
        assertEquals(purchaseAmount, Integer.parseInt(refundEvent.getTransactionAmount().get("amount")));
        assertEquals(refund.get("id"), refundEvent.getTransactionId());
        assertNotNull(refundEvent.getTransactionTimestamp());
        assertEquals(settlement.get("id"), refundEvent.getRelatedSettlementId());
    }

    private void assertAuthorisationEvent(final WebhookAuthorisationEventModel authorisationEvent,
                                          final SimulateCardPurchaseByIdModel cardPurchase,
                                          final CurrencyAmount transactionAmount,
                                          final String managedCardId,
                                          final String ownerId,
                                          final String ownerType,
                                          final CardDetailsModel cardDetailsModel) throws SQLException {

        final Map<String, String> authorisation = ManagedCardsDatabaseHelper.getAuthorisationByCardId(managedCardId).get(0);

        assertTrue(authorisationEvent.isApproved());
        assertEquals("NONE", authorisationEvent.getAuthRuleFailedReason());
        assertEquals("AUTHORISED", authorisationEvent.getAuthorisationType());
        assertEquals(transactionAmount.getCurrency(), authorisationEvent.getAvailableBalance().get("currency"));
        assertEquals(cardDetailsModel.getCardMode().equals(CardMode.DEBIT) ? 0 : ((PrepaidCardModeDetailsModel) cardDetailsModel).getAvailableBalance(),
                Integer.parseInt(authorisationEvent.getAvailableBalance().get("amount")));
        assertFalse(Boolean.parseBoolean(authorisationEvent.getCardPresent()));
        assertEquals("PRESENT", authorisationEvent.getCardholderPresent());
        assertEquals("NO_REASON", authorisationEvent.getDeclineReason());
        assertEquals(managedCardId, authorisationEvent.getId().get("id"));
        assertEquals("managed_cards", authorisationEvent.getId().get("type"));
        assertEquals("5399", authorisationEvent.getMerchantCategoryCode());
        assertEquals("Amazon IT", authorisationEvent.getMerchantName());
        assertEquals(ownerId, authorisationEvent.getOwner().get("id"));
        assertEquals(ownerType, authorisationEvent.getOwner().get("type"));
        assertEquals("0", authorisationEvent.getRelatedAuthorisationId());
        assertEquals(cardPurchase.getTransactionAmount().getCurrency(), authorisationEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(cardPurchase.getTransactionAmount().getAmount()), Integer.parseInt(authorisationEvent.getSourceAmount().get("amount")));
        assertEquals(transactionAmount.getCurrency(), authorisationEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(transactionAmount.getAmount()), Integer.parseInt(authorisationEvent.getTransactionAmount().get("amount")));
        assertEquals(authorisation.get("id"), authorisationEvent.getTransactionId());
        assertEquals(authorisation.get("auth_code"), authorisationEvent.getAuthCode());
        assertNotNull(authorisationEvent.getTransactionTimestamp());
        assertEquals(transactionAmount.getCurrency(), authorisationEvent.getForexFee().get("currency"));
        assertEquals(cardPurchase.getForexFee(), Integer.parseInt(authorisationEvent.getForexFee().get("amount")));
        assertEquals(transactionAmount.getCurrency(), authorisationEvent.getForexPadding().get("currency"));
        assertEquals(cardPurchase.getForexPadding(), Integer.parseInt(authorisationEvent.getForexPadding().get("amount")));

        if (cardDetailsModel.getCardMode().equals(CardMode.DEBIT)) {

            final DebitCardModeDetailsModel debitCard = (DebitCardModeDetailsModel) cardDetailsModel;

            assertEquals(debitCard.getManagedAccountId(), authorisationEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
            assertEquals((int) debitCard.getAvailableToSpend(), Integer.parseInt(authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
            assertEquals(debitCard.getInterval(), authorisationEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        } else {
            final PrepaidCardModeDetailsModel prepaidCard = (PrepaidCardModeDetailsModel) cardDetailsModel;

            assertEquals(prepaidCard.getCurrency(), authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
            assertEquals((int) prepaidCard.getAvailableBalance(), Integer.parseInt(authorisationEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
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

        assertEquals(transactionAmount.getCurrency(), settlementEvent.getAvailableBalance().get("currency"));
        assertEquals(cardDetailsModel.getCardMode().equals(CardMode.DEBIT) ? 0 : ((PrepaidCardModeDetailsModel) cardDetailsModel).getAvailableBalance(),
                Integer.parseInt(settlementEvent.getAvailableBalance().get("amount")));
        assertEquals(transactionAmount.getCurrency(), settlementEvent.getFeeAmount().get("currency"));
        assertEquals(purchaseFee.intValue(), Integer.parseInt(settlementEvent.getFeeAmount().get("amount")));
        assertEquals(managedCardId, settlementEvent.getId().get("id"));
        assertEquals("managed_cards", settlementEvent.getId().get("type"));
        assertEquals("5399", settlementEvent.getMerchantCategoryCode());
        assertEquals("Amazon IT", settlementEvent.getMerchantName());
        assertEquals("123456789", settlementEvent.getMerchantId());
        assertEquals("COMPLETED", settlementEvent.getState());
        assertEquals(ownerId, settlementEvent.getOwner().get("id"));
        assertEquals(ownerType, settlementEvent.getOwner().get("type"));
        assertEquals(authorisationEvent.getTransactionId(), settlementEvent.getRelatedAuthorisationId());
        assertEquals("SALE_PURCHASE", settlementEvent.getSettlementType());
        assertEquals(cardPurchase.getTransactionAmount().getCurrency(), settlementEvent.getSourceAmount().get("currency"));
        assertEquals(Math.negateExact(cardPurchase.getTransactionAmount().getAmount()), Integer.parseInt(settlementEvent.getSourceAmount().get("amount")));
        assertEquals(transactionAmount.getCurrency(), settlementEvent.getTransactionAmount().get("currency"));
        assertEquals(Math.negateExact(transactionAmount.getAmount()), Integer.parseInt(settlementEvent.getTransactionAmount().get("amount")));
        assertEquals(settlement.get("id"), settlementEvent.getTransactionId());
        assertNotNull(settlementEvent.getTransactionTimestamp());
        assertEquals(authorisationEvent.getAuthCode(), settlementEvent.getAuthCode());
        assertEquals(transactionAmount.getCurrency(), settlementEvent.getForexFee().get("currency"));
        assertEquals(cardPurchase.getForexFee(), Integer.parseInt(settlementEvent.getForexFee().get("amount")));

        if (cardDetailsModel.getCardMode().equals(CardMode.DEBIT)) {

            final DebitCardModeDetailsModel debitCard = (DebitCardModeDetailsModel) cardDetailsModel;

            assertEquals(debitCard.getManagedAccountId(), settlementEvent.getDetails().getDebitModeDetails().getParentManagedAccountId());
            assertEquals((int) debitCard.getAvailableToSpend(), Integer.parseInt(settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("availableAmount")));
            assertEquals(debitCard.getInterval(), settlementEvent.getDetails().getDebitModeDetails().getAvailableToSpend().get(0).get("interval"));
        } else {
            final PrepaidCardModeDetailsModel prepaidCard = (PrepaidCardModeDetailsModel) cardDetailsModel;

            assertEquals(prepaidCard.getCurrency(), settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getCurrency());
            assertEquals((int) prepaidCard.getAvailableBalance(), Integer.parseInt(settlementEvent.getDetails().getPrepaidModeDetails().get("availableBalance").getAmount()));
        }
    }

    private WebhookAuthorisationEventModel getAuthorisationWebhookResponse(final long timestamp,
                                                                           final String identityId) {
        return (WebhookAuthorisationEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.AUTHORISATION,
                Pair.of("owner.id", identityId),
                WebhookAuthorisationEventModel.class,
                ApiSchemaDefinition.ManagedCardsAuthorisationEvent);
    }

    private List<WebhookAuthorisationEventModel> getAuthorisationWebhookResponses(final long timestamp,
                                                                                  final String identityId,
                                                                                  final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.AUTHORISATION,
                Pair.of("owner.id", identityId),
                WebhookAuthorisationEventModel.class,
                ApiSchemaDefinition.ManagedCardsAuthorisationEvent,
                expectedEventCount);
    }

    private WebhookSettlementEventModel getSettlementWebhookResponse(final long timestamp,
                                                                     final String identityId) {
        return (WebhookSettlementEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.SETTLEMENT,
                Pair.of("owner.id", identityId),
                WebhookSettlementEventModel.class,
                ApiSchemaDefinition.ManagedCardsSettlementEvent);
    }

    private List<WebhookSettlementEventModel> getSettlementWebhookResponses(final long timestamp,
                                                                            final String identityId,
                                                                            final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.SETTLEMENT,
                Pair.of("owner.id", identityId),
                WebhookSettlementEventModel.class,
                ApiSchemaDefinition.ManagedCardsSettlementEvent,
                expectedEventCount);
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(final List<SpendLimitModel> spendLimit) {
        return SpendRulesModel
                .builder()
                .setAllowedMerchantCategories(new ArrayList<>())
                .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                .setAllowedMerchantIds(new ArrayList<>())
                .setBlockedMerchantIds(new ArrayList<>())
                .setAllowContactless(true)
                .setAllowAtm(true)
                .setAllowECommerce(true)
                .setAllowCashback(true)
                .setAllowCreditAuthorisations(true)
                .setSpendLimit(spendLimit);
    }
}
