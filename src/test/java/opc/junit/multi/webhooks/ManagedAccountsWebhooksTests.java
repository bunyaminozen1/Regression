package opc.junit.multi.webhooks;

import opc.enums.opc.ApiSchemaDefinition;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.ManagedAccountState;
import opc.enums.opc.OwnerType;
import opc.enums.opc.WebhookType;
import opc.junit.database.ManagedAccountsDatabaseHelper;
import opc.junit.database.PayneticsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.webhook.WebhookManagedAccountEventModel;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static opc.enums.opc.ManagedAccountState.BLOCKED;
import static opc.enums.opc.ManagedAccountState.CREATED;
import static opc.enums.opc.ManagedAccountState.DESTROYED;
import static opc.enums.opc.ManagedAccountState.REQUESTED;
import static opc.enums.opc.ManagedAccountState.UNBLOCKED;
import static opc.enums.opc.ManagedAccountState.UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(MultiTags.MANAGED_ACCOUNTS_WEBHOOKS)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public class ManagedAccountsWebhooksTests extends BaseWebhooksSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static CreateCorporateModel createCorporateModel;
    private static CreateConsumerModel createConsumerModel;
    private static String corporateId;
    private static String consumerId;
    private static String corporateCurrency;
    private static String consumerCurrency;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
        consumerSetup();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR", "USD"})
    public void Webhooks_ManagedAccountCreateEvent_Success(final Currency currency) throws SQLException {

        final long timestamp = Instant.now().toEpochMilli();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(currency.name(), corporateManagedAccountProfileId, corporateAuthenticationToken);

        final WebhookManagedAccountEventModel managedAccountEvent = getWebhookResponse(timestamp, managedAccount.getLeft());

        assertManagedAccountEvent(CREATED, managedAccount, managedAccountEvent, OwnerType.CORPORATE);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"GBP"})
    public void Webhooks_ManagedAccountRequestedEvent_Success(final Currency currency) throws SQLException {

        final long timestamp = Instant.now().toEpochMilli();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(currency.name(), corporateManagedAccountProfileId, corporateAuthenticationToken);

        final List<WebhookManagedAccountEventModel> managedAccountEvents = getWebhookResponses(timestamp, managedAccount.getLeft(), 2);

        assertManagedAccountEvent(REQUESTED, managedAccount, managedAccountEvents, OwnerType.CORPORATE);
        assertManagedAccountEvent(CREATED, managedAccount, managedAccountEvents, OwnerType.CORPORATE);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Webhooks_ManagedAccountNonFpsEnabledTenantCreateEvent_Success(final Currency currency) throws SQLException {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(nonFpsTenant.getConsumersProfileId())
                        .setBaseCurrency(currency.name())
                        .build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel,
                        nonFpsTenant.getSecretKey());

        final long timestamp = Instant.now().toEpochMilli();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(currency.name(), nonFpsTenant.getConsumerPayneticsEeaManagedAccountsProfileId(), nonFpsTenant.getSecretKey(), consumer.getRight());

        final WebhookManagedAccountEventModel managedAccountEvent = getWebhookResponse(timestamp, managedAccount.getLeft());

        assertManagedAccountEvent(CREATED, managedAccount, managedAccountEvent, OwnerType.CONSUMER, consumer.getLeft(), 0L, false, null, null, null);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Webhooks_ManagedAccountIbanUpgradeMAUpdateEvent_Success(final Currency currency) throws SQLException {

        final long timestamp = Instant.now().toEpochMilli();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(currency.name(), corporateManagedAccountProfileId, corporateAuthenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccount.getLeft(), secretKey, corporateAuthenticationToken);

        final List<WebhookManagedAccountEventModel> managedAccountEvents = getWebhookResponses(timestamp, managedAccount.getLeft(), currency.equals(Currency.GBP) ? 3 : 2);

        assertManagedAccountEvent(UPDATED, managedAccount, managedAccountEvents, OwnerType.CORPORATE, corporateId, 0L,
                true, currency.equals(Currency.USD) ? "USD SAXO - Paynetics AD" : createCorporateModel.getCompany().getName(), null, null);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void Webhooks_ManagedAccountIbanUpgradeNonFpsEnabledTenantMAUpdateEvent_Success(final Currency currency) throws SQLException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsTenant.getCorporatesProfileId())
                        .setBaseCurrency(currency.name())
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsTenant.getSecretKey());

        final long timestamp = Instant.now().toEpochMilli();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(currency.name(), nonFpsTenant.getCorporatePayneticsEeaManagedAccountsProfileId(), nonFpsTenant.getSecretKey(), corporate.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(managedAccount.getLeft(), nonFpsTenant.getSecretKey(), corporate.getRight());

        final List<WebhookManagedAccountEventModel> managedAccountEvents = getWebhookResponses(timestamp, managedAccount.getLeft(), 2);

        assertManagedAccountEvent(UPDATED, managedAccount, managedAccountEvents, OwnerType.CORPORATE, corporate.getLeft(), 0L,
                false, currency.equals(Currency.EUR) ? createCorporateModel.getCompany().getName() : String.format("%s SAXO - Paynetics AD", currency.name()), null, null);
    }

    @Test
    public void Webhooks_ManagedAccountBlockedEvent_Success() throws SQLException {

        final long timestamp = Instant.now().toEpochMilli();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerCurrency, consumerManagedAccountProfileId, consumerAuthenticationToken);

        ManagedAccountsHelper.blockManagedAccount(managedAccount.getLeft(), secretKey, consumerAuthenticationToken);

        final List<WebhookManagedAccountEventModel> managedAccountEvents = getWebhookResponses(timestamp, managedAccount.getLeft(), consumerCurrency.equals(Currency.GBP.name()) ? 3 : 2);

        assertManagedAccountEvent(BLOCKED, managedAccount, managedAccountEvents, OwnerType.CONSUMER, consumerId, 0L,
                true, null, "User", null);
    }

    @Test
    public void Webhooks_ManagedAccountUnblockedEvent_Success() throws SQLException {

        final long timestamp = Instant.now().toEpochMilli();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerCurrency, consumerManagedAccountProfileId, consumerAuthenticationToken);

        ManagedAccountsHelper.blockManagedAccount(managedAccount.getLeft(), secretKey, consumerAuthenticationToken);
        ManagedAccountsHelper.unblockManagedAccount(managedAccount.getLeft(), secretKey, consumerAuthenticationToken);

        final List<WebhookManagedAccountEventModel> managedAccountEvents = getWebhookResponses(timestamp, managedAccount.getLeft(), consumerCurrency.equals(Currency.GBP.name()) ? 4 : 3);

        assertManagedAccountEvent(UNBLOCKED, managedAccount, managedAccountEvents, OwnerType.CONSUMER);
    }

    @Test
    public void Webhooks_ManagedAccountDestroyedEvent_Success() throws SQLException {

        final long timestamp = Instant.now().toEpochMilli();
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateCurrency, corporateManagedAccountProfileId, corporateAuthenticationToken);

        ManagedAccountsHelper.removeManagedAccount(managedAccount.getLeft(), secretKey, corporateAuthenticationToken);

        final List<WebhookManagedAccountEventModel> managedAccountEvents = getWebhookResponses(timestamp, managedAccount.getLeft(), corporateCurrency.equals(Currency.GBP.name()) ? 3 : 2);

        assertManagedAccountEvent(DESTROYED, managedAccount, managedAccountEvents, OwnerType.CORPORATE, corporateId, 0L,
                true, null, null, "USER");
    }

    private Pair<String, CreateManagedAccountModel> createManagedAccount(final String currency,
                                                                         final String profileId,
                                                                         final String authenticationToken) {

        return createManagedAccount(currency, profileId, secretKey, authenticationToken);
    }

    private Pair<String, CreateManagedAccountModel> createManagedAccount(final String currency,
                                                                         final String profileId,
                                                                         final String secretKey,
                                                                         final String authenticationToken) {

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(profileId, currency).build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(managedAccountId, createManagedAccountModel);
    }

    private WebhookManagedAccountEventModel getWebhookResponse(final long timestamp,
                                                               final String managedAccountId) {
        return (WebhookManagedAccountEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.MANAGED_ACCOUNT,
                Pair.of("account.id.id", managedAccountId),
                WebhookManagedAccountEventModel.class,
                ApiSchemaDefinition.ManagedAccountEvent);
    }

    private List<WebhookManagedAccountEventModel> getWebhookResponses(final long timestamp,
                                                                      final String managedAccountId,
                                                                      final int expectedEventCount) {
        return WebhookHelper.getWebhookServiceEvents(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.MANAGED_ACCOUNT,
                Pair.of("account.id.id", managedAccountId),
                WebhookManagedAccountEventModel.class,
                ApiSchemaDefinition.ManagedAccountEvent,
                expectedEventCount);
    }

    private void assertManagedAccountEvent(final ManagedAccountState state,
                                           final Pair<String, CreateManagedAccountModel> managedAccount,
                                           final WebhookManagedAccountEventModel managedAccountEvent,
                                           final OwnerType ownerType) throws SQLException {
        assertManagedAccountEvent(state, managedAccount, managedAccountEvent, ownerType,
                ownerType.equals(OwnerType.CORPORATE) ? corporateId : consumerId, 0L, true, null, null, null);
    }

    private void assertManagedAccountEvent(final ManagedAccountState state,
                                           final Pair<String, CreateManagedAccountModel> managedAccount,
                                           final List<WebhookManagedAccountEventModel> managedAccountEvents,
                                           final OwnerType ownerType) throws SQLException {

        assertManagedAccountEvent(state, managedAccount, managedAccountEvents, ownerType,
                ownerType.equals(OwnerType.CORPORATE) ? corporateId : consumerId, 0L, true, null, null, null);
    }

    private void assertManagedAccountEvent(final ManagedAccountState state,
                                           final Pair<String, CreateManagedAccountModel> managedAccount,
                                           final WebhookManagedAccountEventModel managedAccountEvent,
                                           final OwnerType ownerType,
                                           final String ownerId,
                                           final Long balance,
                                           final boolean isFpsEnabled,
                                           final String identityName,
                                           final String blockType,
                                           final String destroyType) throws SQLException {

        assertEquals(state.name(), managedAccountEvent.getType());
        assertNotNull(managedAccountEvent.getPublishedTimestamp());
        assertEquals("managed_accounts", managedAccountEvent.getAccount().getId().getType());
        assertEquals(managedAccount.getLeft(), managedAccountEvent.getAccount().getId().getId());
        assertEquals(managedAccount.getRight().getProfileId(), managedAccountEvent.getAccount().getProfileId());
        assertEquals(managedAccount.getRight().getTag(), managedAccountEvent.getAccount().getTag());
        assertEquals(managedAccount.getRight().getFriendlyName(), managedAccountEvent.getAccount().getFriendlyName());
        assertEquals(managedAccount.getRight().getCurrency(), managedAccountEvent.getAccount().getCurrency());
        assertEquals((destroyType == null && !state.equals(REQUESTED)), managedAccountEvent.getAccount().isActive());
        assertEquals(balance, managedAccountEvent.getAccount().getBalances().getActualBalance());
        assertEquals(balance, managedAccountEvent.getAccount().getBalances().getAvailableBalance());
        assertEquals(ownerType.getValue(), managedAccountEvent.getAccount().getOwner().getType());
        assertEquals(ownerId, managedAccountEvent.getAccount().getOwner().getId());
        assertNotNull(managedAccountEvent.getAccount().getCreationTimestamp());
        assertEquals(blockType == null ? 0 : 1, managedAccountEvent.getAccount().getState().getBlockTypes().size());
        assertEquals(destroyType == null ? "" : destroyType, managedAccountEvent.getAccount().getState().getDestroyType());

        if (state.equals(UPDATED)) {

            final Currency currency = Currency.valueOf(managedAccount.getRight().getCurrency());
            String paymentReference = "";
            if (isFpsEnabled && currency.equals(Currency.USD)) {
                paymentReference =
                        PayneticsDatabaseHelper.getPayneticsAccount(managedAccount.getLeft()).get(0).get("processor_external_reference");
            } else if (!isFpsEnabled) {
                paymentReference =
                        PayneticsDatabaseHelper.getPayneticsAccount(managedAccount.getLeft()).get(0).get("processor_external_reference");
            }

            assertEquals(getBankAccountAddress(currency, isFpsEnabled),
                    managedAccountEvent.getAccount().getBankAccountDetails().getAddress());
            assertEquals(identityName, managedAccountEvent.getAccount().getBankAccountDetails().getBeneficiary());
            assertEquals(getBeneficiaryBank(currency, isFpsEnabled), managedAccountEvent.getAccount().getBankAccountDetails().getBeneficiaryBank());

            if (isFpsEnabled) {
                assertEquals(currency.equals(Currency.GBP) ? "" : paymentReference,
                        managedAccountEvent.getAccount().getBankAccountDetails().getPaymentReference());
            } else {
                assertEquals(currency.equals(Currency.EUR) ? "" : paymentReference,
                        managedAccountEvent.getAccount().getBankAccountDetails().getPaymentReference());
            }

            switch (currency) {
                case EUR:
                    final Map<String, String> eurBankDetails =
                            ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccount.getLeft()).get(0);

                    assertEquals(eurBankDetails.get("iban"), managedAccountEvent.getAccount().getBankAccountDetails().getIban());
                    assertEquals(eurBankDetails.get("bank_identifier_code"), managedAccountEvent.getAccount().getBankAccountDetails().getBankIdentifierCode());
                    assertNullOrEmpty(managedAccountEvent.getAccount().getBankAccountDetails().getAccountNumber());
                    assertNullOrEmpty(managedAccountEvent.getAccount().getBankAccountDetails().getSortCode());
                    break;

                case GBP:
                    final Map<String, String> gbpBankDetails =
                            ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccount.getLeft()).get(0);

                    if (isFpsEnabled) {
                        assertEquals(gbpBankDetails.get("account_number"), managedAccountEvent.getAccount().getBankAccountDetails().getAccountNumber());
                        assertEquals(gbpBankDetails.get("sort_code"), managedAccountEvent.getAccount().getBankAccountDetails().getSortCode());
                        assertEquals(gbpBankDetails.get("iban"), managedAccountEvent.getAccount().getBankAccountDetails().getIban());
                        assertEquals(gbpBankDetails.get("bank_identifier_code"), managedAccountEvent.getAccount().getBankAccountDetails().getBankIdentifierCode());
                    } else {
                        assertEquals(gbpBankDetails.get("account_number"), managedAccountEvent.getAccount().getBankAccountDetails().getAccountNumber());
                        assertEquals(gbpBankDetails.get("sort_code"), managedAccountEvent.getAccount().getBankAccountDetails().getSortCode());
                        assertEquals(gbpBankDetails.get("iban"), managedAccountEvent.getAccount().getBankAccountDetails().getIban());
                        assertEquals(gbpBankDetails.get("bank_identifier_code"), managedAccountEvent.getAccount().getBankAccountDetails().getBankIdentifierCode());
                    }
                    break;

                case USD:

                    final Map<String, String> usdBankDetails =
                            ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccount.getLeft()).get(0);

                    assertEquals(usdBankDetails.get("iban"), managedAccountEvent.getAccount().getBankAccountDetails().getIban());
                    assertEquals(usdBankDetails.get("bank_identifier_code"), managedAccountEvent.getAccount().getBankAccountDetails().getBankIdentifierCode());
                    assertEquals(usdBankDetails.get("sort_code"), managedAccountEvent.getAccount().getBankAccountDetails().getSortCode());
                    assertEquals("USD SAXO - 00010675", managedAccountEvent.getAccount().getBankAccountDetails().getAccountNumber());
                    break;

                default:
                    throw new IllegalArgumentException("Currency not supported.");
            }
        } else {
            assertNull(managedAccountEvent.getAccount().getBankAccountDetails());
        }
    }

    private void assertManagedAccountEvent(final ManagedAccountState state,
                                           final Pair<String, CreateManagedAccountModel> managedAccount,
                                           final List<WebhookManagedAccountEventModel> managedAccountEvents,
                                           final OwnerType ownerType,
                                           final String ownerId,
                                           final Long balance,
                                           final boolean isFpsEnabled,
                                           final String identityName,
                                           final String blockType,
                                           final String destroyType) throws SQLException {

        final WebhookManagedAccountEventModel managedAccountEvent =
                managedAccountEvents.stream().filter(x -> x.getType().equals(state.name())).collect(Collectors.toList()).get(0);

        assertEquals(state.name(), managedAccountEvent.getType());
        assertNotNull(managedAccountEvent.getPublishedTimestamp());
        assertEquals("managed_accounts", managedAccountEvent.getAccount().getId().getType());
        assertEquals(managedAccount.getLeft(), managedAccountEvent.getAccount().getId().getId());
        assertEquals(managedAccount.getRight().getProfileId(), managedAccountEvent.getAccount().getProfileId());
        assertEquals(managedAccount.getRight().getTag(), managedAccountEvent.getAccount().getTag());
        assertEquals(managedAccount.getRight().getFriendlyName(), managedAccountEvent.getAccount().getFriendlyName());
        assertEquals(managedAccount.getRight().getCurrency(), managedAccountEvent.getAccount().getCurrency());
        assertEquals((destroyType == null && !state.equals(REQUESTED)), managedAccountEvent.getAccount().isActive());
        assertEquals(balance, managedAccountEvent.getAccount().getBalances().getActualBalance());
        assertEquals(balance, managedAccountEvent.getAccount().getBalances().getAvailableBalance());
        assertEquals(ownerType.getValue(), managedAccountEvent.getAccount().getOwner().getType());
        assertEquals(ownerId, managedAccountEvent.getAccount().getOwner().getId());
        assertEquals(blockType == null ? 0 : 1, managedAccountEvent.getAccount().getState().getBlockTypes().size());
        assertEquals(destroyType == null ? "" : destroyType, managedAccountEvent.getAccount().getState().getDestroyType());
        assertNotNull(managedAccountEvent.getAccount().getCreationTimestamp());

        if (state.equals(UPDATED)) {

            final Currency currency = Currency.valueOf(managedAccount.getRight().getCurrency());
            String paymentReference = "";
            if (isFpsEnabled && currency.equals(Currency.USD)) {
                paymentReference =
                        PayneticsDatabaseHelper.getPayneticsAccount(managedAccount.getLeft()).get(0).get("processor_external_reference");
            } else if (!isFpsEnabled) {
                paymentReference =
                        PayneticsDatabaseHelper.getPayneticsAccount(managedAccount.getLeft()).get(0).get("processor_external_reference");
            }

            assertEquals(getBankAccountAddress(currency, isFpsEnabled),
                    managedAccountEvent.getAccount().getBankAccountDetails().getAddress());
            assertEquals(identityName, managedAccountEvent.getAccount().getBankAccountDetails().getBeneficiary());
            assertEquals(getBeneficiaryBank(currency, isFpsEnabled), managedAccountEvent.getAccount().getBankAccountDetails().getBeneficiaryBank());

            if (isFpsEnabled) {
                assertEquals(currency.equals(Currency.GBP) ? "" : paymentReference,
                        managedAccountEvent.getAccount().getBankAccountDetails().getPaymentReference());
            } else {
                assertEquals(currency.equals(Currency.EUR) ? "" : paymentReference,
                        managedAccountEvent.getAccount().getBankAccountDetails().getPaymentReference());
            }

            switch (currency) {
                case EUR:
                    final Map<String, String> eurBankDetails =
                            ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccount.getLeft()).get(0);

                    assertEquals(eurBankDetails.get("iban"), managedAccountEvent.getAccount().getBankAccountDetails().getIban());
                    assertEquals(eurBankDetails.get("bank_identifier_code"), managedAccountEvent.getAccount().getBankAccountDetails().getBankIdentifierCode());
                    assertNullOrEmpty(managedAccountEvent.getAccount().getBankAccountDetails().getAccountNumber());
                    assertNullOrEmpty(managedAccountEvent.getAccount().getBankAccountDetails().getSortCode());
                    break;

                case GBP:
                    final Map<String, String> gbpBankDetails =
                            ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccount.getLeft()).get(0);

                    if (isFpsEnabled) {
                        assertEquals(gbpBankDetails.get("account_number"), managedAccountEvent.getAccount().getBankAccountDetails().getAccountNumber());
                        assertEquals(gbpBankDetails.get("sort_code"), managedAccountEvent.getAccount().getBankAccountDetails().getSortCode());
                        assertEquals(gbpBankDetails.get("iban"), managedAccountEvent.getAccount().getBankAccountDetails().getIban());
                        assertEquals(gbpBankDetails.get("bank_identifier_code"), managedAccountEvent.getAccount().getBankAccountDetails().getBankIdentifierCode());
                    } else {
                        assertEquals(gbpBankDetails.get("account_number"), managedAccountEvent.getAccount().getBankAccountDetails().getAccountNumber());
                        assertEquals(gbpBankDetails.get("sort_code"), managedAccountEvent.getAccount().getBankAccountDetails().getSortCode());
                        assertEquals(gbpBankDetails.get("iban"), managedAccountEvent.getAccount().getBankAccountDetails().getIban());
                        assertEquals(gbpBankDetails.get("bank_identifier_code"), managedAccountEvent.getAccount().getBankAccountDetails().getBankIdentifierCode());
                    }
                    break;

                case USD:

                    final Map<String, String> usdBankDetails =
                            ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccount.getLeft()).get(0);

                    assertEquals(usdBankDetails.get("iban"), managedAccountEvent.getAccount().getBankAccountDetails().getIban());
                    assertEquals(usdBankDetails.get("bank_identifier_code"), managedAccountEvent.getAccount().getBankAccountDetails().getBankIdentifierCode());
                    assertEquals(usdBankDetails.get("sort_code"), managedAccountEvent.getAccount().getBankAccountDetails().getSortCode());
                    assertEquals("USD SAXO - 00010675", managedAccountEvent.getAccount().getBankAccountDetails().getAccountNumber());
                    break;

                default:
                    throw new IllegalArgumentException("Currency not supported.");
            }
        } else {
            assertNull(managedAccountEvent.getAccount().getBankAccountDetails());
        }
    }

    private static void assertNullOrEmpty(final String value) {
        assertTrue(value == null || value.isEmpty());
    }

    private static void consumerSetup() {
        createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerId = authenticatedConsumer.getLeft();
        consumerCurrency = createConsumerModel.getBaseCurrency();
    }

    private static void corporateSetup() {

        createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateId = authenticatedCorporate.getLeft();
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }

    private String getBankAccountAddress(final Currency currency, final boolean fpsEnabled) {

        final String address;
        switch (currency) {
            case EUR:
                address = "SEPA - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria";
                break;
            case GBP:
                address = fpsEnabled ? "FPS - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria" :
                        "GBP SAXO - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria";
                break;
            case USD:
                address = "USD SAXO - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria";
                break;
            default:
                throw new IllegalArgumentException("Currency not supported");
        }

        return address;
    }

    private String getBeneficiaryBank(final Currency currency, final boolean fpsEnabled) {

        final String beneficiaryBank;
        switch (currency) {
            case EUR:
                beneficiaryBank = "SEPA - Saxo Payments A/S";
                break;
            case GBP:
                beneficiaryBank = fpsEnabled ? "FPS - Clearbank A/S" :
                        "GBP SAXO - Saxo Payments A/S";
                break;
            case USD:
                beneficiaryBank = "USD SAXO - Saxo Payments A/S";
                break;
            default:
                throw new IllegalArgumentException("Currency not supported");
        }

        return beneficiaryBank;
    }

    @AfterAll
    public static void DisableWebhooks(){
        InnovatorHelper.disableWebhook(UpdateProgrammeModel.WebHookUrlSetup(nonFpsProgrammeId, true, webhookServiceDetails.getRight()),
                nonFpsProgrammeId, nonFpsInnovatorToken);
    }
}
