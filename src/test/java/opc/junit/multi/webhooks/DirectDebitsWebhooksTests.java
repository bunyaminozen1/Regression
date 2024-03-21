//package opc.junit.multi.webhooks;
//
//import io.cucumber.messages.internal.com.google.gson.Gson;
//import io.restassured.path.json.JsonPath;
//import opc.enums.opc.CollectionRejectionReason;
//import opc.enums.opc.CompanyType;
//import commons.enums.Currency;
//import opc.enums.opc.InnovatorSetup;
//import opc.enums.opc.ManagedInstrumentType;
//import opc.enums.opc.OwnerType;
//import opc.enums.opc.WebhookType;
//import opc.junit.helpers.TestHelper;
//import opc.junit.helpers.innovator.InnovatorHelper;
//import opc.junit.helpers.multi.ConsumersHelper;
//import opc.junit.helpers.multi.CorporatesHelper;
//import opc.junit.helpers.multi.DirectDebitsHelper;
//import opc.junit.helpers.multi.ManagedAccountsHelper;
//import opc.junit.helpers.webhook.WebhookHelper;
//import opc.junit.multi.BaseSetupExtension;
//import opc.models.innovator.UpdateProgrammeModel;
//import opc.models.multi.consumers.CreateConsumerModel;
//import commons.models.CompanyModel;
//import opc.models.multi.corporates.CreateCorporateModel;
//import opc.models.multi.directdebit.RejectCollectionModel;
//import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
//import opc.models.shared.CurrencyAmount;
//import opc.models.shared.ProgrammeDetailsModel;
//import opc.models.simulator.SimulateCreateMandateModel;
//import opc.models.webhook.WebhookCollectionEventModel;
//import opc.models.webhook.WebhookDataResponse;
//import opc.models.webhook.WebhookMandateEventModel;
//import org.apache.commons.lang3.tuple.Pair;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.RegisterExtension;
//import org.junit.jupiter.api.parallel.Execution;
//import org.junit.jupiter.api.parallel.ExecutionMode;
//
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static opc.enums.opc.MandateCollectionState.PAID;
//import static opc.enums.opc.MandateCollectionState.REJECTED;
//import static opc.enums.opc.MandateCollectionState.UNPAID;
//import static opc.enums.opc.MandateState.CANCELLED;
//import static opc.enums.opc.MandateState.EXPIRED;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//@Execution(ExecutionMode.SAME_THREAD)
//public class DirectDebitsWebhooksTests {
//
//    @RegisterExtension
//    static BaseSetupExtension setupExtension = new BaseSetupExtension();
//
//    private static final Long DEPOSIT_AMOUNT = 10000L;
//
//    private static String programmeId;
//    private static String innovatorEmail;
//    private static String innovatorPassword;
//    private static String corporateProfileId;
//    private static String consumerProfileId;
//    private static String corporateOddProfileId;
//    private static String consumerOddProfileId;
//    private static String secretKey;
//
//    private static String corporateId;
//    private static String consumerId;
//    private static String corporateAuthenticationToken;
//    private static String consumerAuthenticationToken;
//    private static String corporateCurrency;
//    private static String consumerCurrency;
//    private static Pair<String, FasterPaymentsBankDetailsModel> corporateManagedAccount;
//    private static Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount;
//
//    private static Pair<String, String> webhookServiceDetails;
//
//    @BeforeAll
//    public static void Setup(){
//
//        final ProgrammeDetailsModel webhooksOddApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.WEBHOOKS_ODD_APP);
//
//        programmeId = webhooksOddApp.getProgrammeId();
//        innovatorEmail = webhooksOddApp.getInnovatorEmail();
//        innovatorPassword = webhooksOddApp.getInnovatorPassword();
//
//        corporateProfileId = webhooksOddApp.getCorporatesProfileId();
//        consumerProfileId = webhooksOddApp.getConsumersProfileId();
//
//        final String corporateModulrManagedAccountProfileId = webhooksOddApp.getCorporateModulrManagedAccountsProfileId();
//        final String consumerModulrManagedAccountProfileId = webhooksOddApp.getConsumerModulrManagedAccountsProfileId();
//        corporateOddProfileId = webhooksOddApp.getCorporateOddProfileId();
//        consumerOddProfileId = webhooksOddApp.getConsumerOddProfileId();
//
//        secretKey = webhooksOddApp.getSecretKey();
//
//        corporateSetup();
//        consumerSetup();
//
//        corporateManagedAccount = createFundedManagedAccount(corporateModulrManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
//        consumerManagedAccount = createManagedAccount(consumerModulrManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);
//
//        webhookServiceDetails = WebhookHelper.generateWebhookUrl();
//
//        InnovatorHelper.enableWebhook(UpdateProgrammeModel.WebHookUrlSetup(programmeId, false, webhookServiceDetails.getRight()),
//                programmeId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));
//    }
//
//    @Test
//    public void Webhooks_CorporateCreateMandate_Success() {
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(corporateManagedAccount, secretKey, corporateAuthenticationToken);
//
//        final WebhookDataResponse webhookResponse = WebhookHelper.getWebhookServiceEvent(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_MANDATE);
//
//        final WebhookMandateEventModel mandateEvent =
//                new Gson().fromJson(webhookResponse.getContent(), WebhookMandateEventModel.class);
//
//        assertCorporateMandateEvent(mandate, mandateEvent, "CREATED", "ACTIVE");
//    }
//
//    @Test
//    public void Webhooks_ConsumerCreateMandate_Success() {
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(consumerManagedAccount, secretKey, consumerAuthenticationToken);
//
//        final WebhookDataResponse webhookResponse = WebhookHelper.getWebhookServiceEvent(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_MANDATE);
//
//        final WebhookMandateEventModel mandateEvent =
//                new Gson().fromJson(webhookResponse.getContent(), WebhookMandateEventModel.class);
//
//        assertConsumerMandateEvent(mandate, mandateEvent);
//    }
//
//    @Test
//    public void Webhooks_CancelMandateInternally_Success() {
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(corporateManagedAccount, secretKey, corporateAuthenticationToken);
//
//        DirectDebitsHelper.cancelMandateInternally(mandate.getLeft(), secretKey, corporateAuthenticationToken);
//
//        final List<WebhookDataResponse> webhookResponse =
//                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_MANDATE, 2);
//
//        final List<WebhookMandateEventModel> mandateEvents = new ArrayList<>();
//        webhookResponse.forEach(event -> mandateEvents.add(new Gson().fromJson(event.getContent(), WebhookMandateEventModel.class)));
//
//        final WebhookMandateEventModel cancelledMandateEvent =
//                mandateEvents.stream().filter(x -> x.getEventType().equals(CANCELLED.name())).collect(Collectors.toList()).get(0);
//
//        assertCorporateMandateEvent(mandate, cancelledMandateEvent, "CANCELLED", "CANCELLED");
//    }
//
//    @Test
//    public void Webhooks_CancelMandateExternally_Success() {
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(corporateManagedAccount, secretKey, corporateAuthenticationToken);
//
//        DirectDebitsHelper.cancelMandateExternally(mandate.getLeft(), mandate.getRight().getDdiId(), secretKey, corporateAuthenticationToken);
//
//        final List<WebhookDataResponse> webhookResponse =
//                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_MANDATE, 2);
//
//        final List<WebhookMandateEventModel> mandateEvents = new ArrayList<>();
//        webhookResponse.forEach(event -> mandateEvents.add(new Gson().fromJson(event.getContent(), WebhookMandateEventModel.class)));
//
//        final WebhookMandateEventModel cancelledMandateEvent =
//                mandateEvents.stream().filter(x -> x.getEventType().equals(CANCELLED.name())).collect(Collectors.toList()).get(0);
//
//        assertCorporateMandateEvent(mandate, cancelledMandateEvent, "CANCELLED", "CANCELLED");
//    }
//
//    @Test
//    public void Webhooks_ExpireMandate_Success() {
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(corporateManagedAccount, secretKey, corporateAuthenticationToken);
//
//        DirectDebitsHelper.expireMandate(mandate.getLeft(), mandate.getRight().getDdiId(), secretKey, corporateAuthenticationToken);
//
//        final List<WebhookDataResponse> webhookResponse =
//                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_MANDATE, 2);
//
//        final List<WebhookMandateEventModel> mandateEvents = new ArrayList<>();
//        webhookResponse.forEach(event -> mandateEvents.add(new Gson().fromJson(event.getContent(), WebhookMandateEventModel.class)));
//
//        final WebhookMandateEventModel expiredMandateEvent =
//                mandateEvents.stream().filter(x -> x.getEventType().equals(EXPIRED.name())).collect(Collectors.toList()).get(0);
//
//        assertCorporateMandateEvent(mandate, expiredMandateEvent, "EXPIRED", "EXPIRED");
//    }
//
//    @Test
//    public void Webhooks_CreateCollection_Success() {
//
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(corporateManagedAccount, secretKey, corporateAuthenticationToken);
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, String> collection =
//                DirectDebitsHelper.createMandateCollections(Pair.of(mandate.getLeft(), mandate.getRight().getDdiId()),
//                        Currency.valueOf(corporateCurrency), Collections.singletonList(100L), secretKey, corporateAuthenticationToken).get(0);
//
//        final WebhookDataResponse webhookResponse = WebhookHelper.getWebhookServiceEvent(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_COLLECTION);
//
//        final WebhookCollectionEventModel collectionEvent =
//                new Gson().fromJson(webhookResponse.getContent(), WebhookCollectionEventModel.class);
//
//        assertCollectionEvent(collection, collectionEvent, mandate, "NEW", "PENDING", 100);
//    }
//
//    @Test
//    public void Webhooks_RejectCollection_Success() {
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(corporateManagedAccount, secretKey, corporateAuthenticationToken);
//
//        final Pair<String, String> collection =
//                DirectDebitsHelper.createMandateCollections(Pair.of(mandate.getLeft(), mandate.getRight().getDdiId()),
//                        Currency.valueOf(corporateCurrency), Collections.singletonList(101L), secretKey, corporateAuthenticationToken).get(0);
//
//        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
//                collection.getLeft(), mandate.getLeft(), secretKey, corporateAuthenticationToken);
//
//        final List<WebhookDataResponse> webhookResponse =
//                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_COLLECTION, 2);
//
//        final List<WebhookCollectionEventModel> collectionEvents = new ArrayList<>();
//        webhookResponse.forEach(event -> collectionEvents.add(new Gson().fromJson(event.getContent(), WebhookCollectionEventModel.class)));
//
//        final WebhookCollectionEventModel rejectedCollectionEvent =
//                collectionEvents.stream().filter(x -> x.getEventType().equals(REJECTED.name())).collect(Collectors.toList()).get(0);
//
//        assertCollectionEvent(collection, rejectedCollectionEvent, mandate, "REJECTED", "REJECTED", 101);
//    }
//
//    @Test
//    public void Webhooks_PaidCollection_Success() {
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(corporateManagedAccount, secretKey, corporateAuthenticationToken);
//
//        final Pair<String, String> collection =
//                DirectDebitsHelper.createMandateCollections(Pair.of(mandate.getLeft(), mandate.getRight().getDdiId()),
//                        Currency.valueOf(corporateCurrency), Collections.singletonList(101L), secretKey, corporateAuthenticationToken).get(0);
//
//        DirectDebitsHelper.collectCollection(collection.getRight(), mandate.getRight().getDdiId(),
//                collection.getLeft(), mandate.getLeft(), secretKey, corporateAuthenticationToken);
//
//        final List<WebhookDataResponse> webhookResponse =
//                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_COLLECTION, 2);
//
//        final List<WebhookCollectionEventModel> collectionEvents = new ArrayList<>();
//        webhookResponse.forEach(event -> collectionEvents.add(new Gson().fromJson(event.getContent(), WebhookCollectionEventModel.class)));
//
//        final WebhookCollectionEventModel paidCollectionEvent =
//                collectionEvents.stream().filter(x -> x.getEventType().equals(PAID.name())).collect(Collectors.toList()).get(0);
//
//        assertCollectionEvent(collection, paidCollectionEvent, mandate, "PAID", "PAID", 101);
//    }
//
//    @Test
//    public void Webhooks_UnpaidCollection_Success() {
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(corporateManagedAccount, secretKey, corporateAuthenticationToken);
//
//        final Pair<String, String> collection =
//                DirectDebitsHelper.createMandateCollections(Pair.of(mandate.getLeft(), mandate.getRight().getDdiId()),
//                        Currency.valueOf(corporateCurrency), Collections.singletonList(11000L), secretKey, corporateAuthenticationToken).get(0);
//
//        DirectDebitsHelper.collectCollection(collection.getRight(), mandate.getRight().getDdiId(),
//                collection.getLeft(), mandate.getLeft(), UNPAID, secretKey, corporateAuthenticationToken);
//
//        final List<WebhookDataResponse> webhookResponse =
//                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_COLLECTION, 2);
//
//        final List<WebhookCollectionEventModel> collectionEvents = new ArrayList<>();
//        webhookResponse.forEach(event -> collectionEvents.add(new Gson().fromJson(event.getContent(), WebhookCollectionEventModel.class)));
//
//        final WebhookCollectionEventModel rejectedCollectionEvent =
//                collectionEvents.stream().filter(x -> x.getEventType().equals(UNPAID.name())).collect(Collectors.toList()).get(0);
//
//        assertCollectionEvent(collection, rejectedCollectionEvent, mandate, "UNPAID", "UNPAID", 11000);
//    }
//
//    @Test
//    public void Webhooks_RefundedCollection_Success() {
//
//        final long timestamp = Instant.now().toEpochMilli();
//        final Pair<String, SimulateCreateMandateModel> mandate =
//                DirectDebitsHelper.createMandate(corporateManagedAccount, secretKey, corporateAuthenticationToken);
//
//        final Pair<String, String> collection =
//                DirectDebitsHelper.createMandateCollections(Pair.of(mandate.getLeft(), mandate.getRight().getDdiId()),
//                        Currency.valueOf(corporateCurrency), Collections.singletonList(101L), secretKey, corporateAuthenticationToken).get(0);
//
//        DirectDebitsHelper.collectCollection(collection.getRight(), mandate.getRight().getDdiId(),
//                collection.getLeft(), mandate.getLeft(), secretKey, corporateAuthenticationToken);
//
//        DirectDebitsHelper.rejectCollection(RejectCollectionModel.rejectCollectionModel(CollectionRejectionReason.random()),
//                collection.getLeft(), mandate.getLeft(), secretKey, corporateAuthenticationToken);
//
//        final List<WebhookDataResponse> webhookResponse =
//                WebhookHelper.getWebhookServiceEvents(webhookServiceDetails.getLeft(), timestamp, WebhookType.ODD_COLLECTION, 3);
//
//        final List<WebhookCollectionEventModel> collectionEvents = new ArrayList<>();
//        webhookResponse.forEach(event -> collectionEvents.add(new Gson().fromJson(event.getContent(), WebhookCollectionEventModel.class)));
//
//        final WebhookCollectionEventModel paidCollectionEvent =
//                collectionEvents.stream().filter(x -> x.getEventType().equals(PAID.name())).collect(Collectors.toList()).get(0);
//
//        final WebhookCollectionEventModel rejectedCollectionEvent =
//                collectionEvents.stream().filter(x -> x.getEventType().equals(REJECTED.name())).collect(Collectors.toList()).get(0);
//
//        assertCollectionEvent(collection, paidCollectionEvent, mandate, "PAID", "PAID", 101);
//        assertCollectionEvent(collection, rejectedCollectionEvent, mandate, "REJECTED", "REJECTED", 101);
//    }
//
//    private void assertCollectionEvent(final Pair<String, String> collection,
//                                       final WebhookCollectionEventModel collectionEvent,
//                                       final Pair<String, SimulateCreateMandateModel> mandate,
//                                       final String eventType,
//                                       final String collectionState,
//                                       final int collectionAmount){
//        assertEquals(eventType, collectionEvent.getEventType());
//        assertNotNull(collectionEvent.getPublishedTimestamp());
//        assertEquals(collection.getLeft(), collectionEvent.getCollection().getId());
//        assertEquals(mandate.getLeft(), collectionEvent.getCollection().getMandateId());
//        assertEquals(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue(), collectionEvent.getCollection().getInstrumentId().getType());
//        assertEquals(corporateManagedAccount.getLeft(), collectionEvent.getCollection().getInstrumentId().getId());
//        assertEquals(corporateCurrency, collectionEvent.getCollection().getAmount().getCurrency());
//        assertEquals(collectionAmount, collectionEvent.getCollection().getAmount().getAmount());
//        assertEquals(collectionState, collectionEvent.getCollection().getStatus());
//        assertEquals(mandate.getRight().getMerchantName(), collectionEvent.getCollection().getMerchantName());
//        assertEquals(mandate.getRight().getMerchantAccountNumber(), collectionEvent.getCollection().getMerchantNumber());
//        assertEquals(mandate.getRight().getMerchantReference(), collectionEvent.getCollection().getMerchantReference());
//        assertNotNull(collectionEvent.getCollection().getSettlementTimestamp());
//    }
//
//    private void assertCorporateMandateEvent(final Pair<String, SimulateCreateMandateModel> mandate,
//                                             final WebhookMandateEventModel mandateEvent,
//                                             final String eventType,
//                                             final String mandateState){
//        assertEquals(corporateOddProfileId, mandateEvent.getMandate().getProfileId());
//        assertEquals(corporateManagedAccount.getLeft(), mandateEvent.getMandate().getInstrumentId().getId());
//        assertEquals(OwnerType.CORPORATE.getValue(), mandateEvent.getMandate().getOwnerId().getType());
//        assertEquals(corporateId, mandateEvent.getMandate().getOwnerId().getId());
//
//        assertCommonMandateEvent(mandate, mandateEvent, eventType, mandateState);
//    }
//
//    private void assertConsumerMandateEvent(final Pair<String, SimulateCreateMandateModel> mandate,
//                                            final WebhookMandateEventModel mandateEvent){
//        assertEquals(consumerOddProfileId, mandateEvent.getMandate().getProfileId());
//        assertEquals(consumerManagedAccount.getLeft(), mandateEvent.getMandate().getInstrumentId().getId());
//        assertEquals(OwnerType.CONSUMER.getValue(), mandateEvent.getMandate().getOwnerId().getType());
//        assertEquals(consumerId, mandateEvent.getMandate().getOwnerId().getId());
//
//        assertCommonMandateEvent(mandate, mandateEvent, "CREATED", "ACTIVE");
//    }
//
//    private void assertCommonMandateEvent(final Pair<String, SimulateCreateMandateModel> mandate,
//                                          final WebhookMandateEventModel mandateEvent,
//                                          final String eventType,
//                                          final String mandateState){
//        assertEquals(eventType, mandateEvent.getEventType());
//        assertNotNull(mandateEvent.getPublishedTimestamp());
//        assertEquals(mandate.getLeft(), mandateEvent.getMandate().getId());
//        assertEquals(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue(), mandateEvent.getMandate().getInstrumentId().getType());
//        assertEquals(mandateState, mandateEvent.getMandate().getStatus());
//        assertNotNull(mandateEvent.getMandate().getSetupDate());
//        assertEquals("ELECTRONIC", mandateEvent.getMandate().getType());
//        assertEquals(0, mandateEvent.getMandate().getPaymentAmount());
//        assertEquals(0, mandateEvent.getMandate().getNumberOfPayments());
//        assertEquals("UNDEFINED_FREQUENCY", mandateEvent.getMandate().getCollectionFrequency());
//        assertEquals(0, mandateEvent.getMandate().getCollectionDueDate());
//        assertEquals(mandate.getRight().getMerchantName(), mandateEvent.getMandate().getMerchantName());
//        assertEquals(mandate.getRight().getMerchantAccountNumber(), mandateEvent.getMandate().getMerchantAccountNumber());
//        assertEquals(mandate.getRight().getMerchantReference(), mandateEvent.getMandate().getMerchantReference());
//        assertEquals(mandate.getRight().getMerchantSortCode(), mandateEvent.getMandate().getMerchantSortCode());
//    }
//
//    private static void consumerSetup() {
//        final CreateConsumerModel createConsumerModel =
//                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();
//
//        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
//        consumerId = authenticatedConsumer.getLeft();
//        consumerAuthenticationToken = authenticatedConsumer.getRight();
//        consumerCurrency = createConsumerModel.getBaseCurrency();
//    }
//
//    private static void corporateSetup() {
//        final CreateCorporateModel createCorporateModel =
//                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.GBP.name())
//                        .setCompany(CompanyModel.DefaultCompanyModel()
//                                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
//                                .build())
//                        .build();
//
//        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
//        corporateId = authenticatedCorporate.getLeft();
//        corporateAuthenticationToken = authenticatedCorporate.getRight();
//        corporateCurrency = createCorporateModel.getBaseCurrency();
//    }
//
//    protected static Pair<String, FasterPaymentsBankDetailsModel> createFundedManagedAccount(final String profileId,
//                                                                                             final String currency,
//                                                                                             final String authenticationToken) {
//        final String managedAccountId =
//                ManagedAccountsHelper
//                        .createManagedAccount(profileId, currency, secretKey, authenticationToken);
//
//        final JsonPath managedAccountDetails =
//                ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, authenticationToken).jsonPath();
//
//        final FasterPaymentsBankDetailsModel bankDetails =
//                new FasterPaymentsBankDetailsModel(managedAccountDetails.getString("bankAccountDetails[0].details.accountNumber"),
//                        managedAccountDetails.getString("bankAccountDetails[0].details.sortCode"));
//
//        TestHelper.simulateDepositWithSpecificBalanceCheck(managedAccountId, new CurrencyAmount(currency, DEPOSIT_AMOUNT),
//                secretKey, authenticationToken, DEPOSIT_AMOUNT.intValue());
//
//        return Pair.of(managedAccountId, bankDetails);
//    }
//
//    protected static Pair<String, FasterPaymentsBankDetailsModel> createManagedAccount(final String profileId,
//                                                                                       final String currency,
//                                                                                       final String authenticationToken) {
//        final String managedAccountId =
//                ManagedAccountsHelper
//                        .createManagedAccount(profileId, currency, secretKey, authenticationToken);
//
//        final JsonPath managedAccountDetails =
//                ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, authenticationToken).jsonPath();
//
//        final FasterPaymentsBankDetailsModel bankDetails =
//                new FasterPaymentsBankDetailsModel(managedAccountDetails.getString("bankAccountDetails[0].details.accountNumber"),
//                        managedAccountDetails.getString("bankAccountDetails[0].details.sortCode"));
//
//        return Pair.of(managedAccountId, bankDetails);
//    }
//
//    @AfterEach
//    public void DeleteEvents(){
//        WebhookHelper.deleteEvents(webhookServiceDetails.getLeft());
//    }
//
//    @AfterAll
//    public static void DisableWebhooks(){
//        InnovatorHelper.disableWebhook(UpdateProgrammeModel.WebHookUrlSetup(programmeId, true, webhookServiceDetails.getRight()),
//                programmeId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));
//    }
//}
