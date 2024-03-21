package opc.junit.multi.directdebits;

import io.restassured.path.json.JsonPath;
import commons.enums.Currency;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.multi.directdebit.GetCollectionModel;
import opc.models.multi.directdebit.GetDirectDebitMandatesResponse;
import opc.models.multi.directdebit.GetMandateCollectionsResponse;
import opc.models.multi.directdebit.GetMandateModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.simulator.SimulateCreateCollectionModel;
import opc.models.simulator.SimulateCreateMandateModel;
import opc.services.multi.DirectDebitsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_OK;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.DIRECT_DEBITS)
public class BaseDirectDebitsSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static final Long DEPOSIT_AMOUNT = 10000L;

    protected static ProgrammeDetailsModel oddApp;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateModulrManagedAccountProfileId;
    protected static String consumerModulrManagedAccountProfileId;
    protected static String corporateOddProfileId;
    protected static String consumerOddProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String secretKey;

    @BeforeAll
    public static void GlobalSetup() throws InterruptedException {

        oddApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.ODD_APP);

        innovatorId = oddApp.getInnovatorId();
        innovatorEmail = oddApp.getInnovatorEmail();
        innovatorPassword = oddApp.getInnovatorPassword();

        corporateProfileId = oddApp.getCorporatesProfileId();
        consumerProfileId = oddApp.getConsumersProfileId();

        corporateModulrManagedAccountProfileId = oddApp.getCorporateModulrManagedAccountsProfileId();
        consumerModulrManagedAccountProfileId = oddApp.getConsumerModulrManagedAccountsProfileId();
        corporateOddProfileId = oddApp.getCorporateOddProfileId();
        consumerOddProfileId = oddApp.getConsumerOddProfileId();
        corporatePrepaidManagedCardsProfileId = oddApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = oddApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();

        secretKey = oddApp.getSecretKey();
    }

    protected static Pair<String, FasterPaymentsBankDetailsModel> createFundedManagedAccount(final String profileId,
                                                                                             final String currency,
                                                                                             final String authenticationToken) {
        final String managedAccountId =
                ManagedAccountsHelper
                        .createManagedAccount(profileId, currency, secretKey, authenticationToken);

        final JsonPath managedAccountDetails =
                ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, authenticationToken).jsonPath();

        final FasterPaymentsBankDetailsModel bankDetails =
                new FasterPaymentsBankDetailsModel(managedAccountDetails.getString("bankAccountDetails[0].details.accountNumber"),
                        managedAccountDetails.getString("bankAccountDetails[0].details.sortCode"));

        TestHelper.simulateDepositWithSpecificBalanceCheck(managedAccountId, new CurrencyAmount(currency, DEPOSIT_AMOUNT),
                secretKey, authenticationToken, DEPOSIT_AMOUNT.intValue());

        return Pair.of(managedAccountId, bankDetails);
    }

    protected static Pair<String, FasterPaymentsBankDetailsModel> createManagedAccount(final String profileId,
                                                                                       final String currency,
                                                                                       final String authenticationToken) {
        final String managedAccountId =
                ManagedAccountsHelper
                        .createManagedAccount(profileId, currency, secretKey, authenticationToken);

        final JsonPath managedAccountDetails =
                ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, authenticationToken).jsonPath();

        final FasterPaymentsBankDetailsModel bankDetails =
                new FasterPaymentsBankDetailsModel(managedAccountDetails.getString("bankAccountDetails[0].details.accountNumber"),
                        managedAccountDetails.getString("bankAccountDetails[0].details.sortCode"));

        return Pair.of(managedAccountId, bankDetails);
    }

    protected static Pair<String, SimulateCreateMandateModel> createMandate(final Pair<String, FasterPaymentsBankDetailsModel> managedAccount,
                                                                            final String authenticationToken) {

        final SimulateCreateMandateModel.Builder simulateCreateMandateModelBuilder =
                SimulateCreateMandateModel.createMandateByAccountId(managedAccount);
        final String ddiId =
                SimulatorHelper.createMandate(simulateCreateMandateModelBuilder.build(), secretKey, authenticationToken)
                .jsonPath().getString("ddiId");
        final SimulateCreateMandateModel simulateCreateMandateModel = simulateCreateMandateModelBuilder.setDdiId(ddiId).build();

        final GetDirectDebitMandatesResponse response =
                TestHelper.ensureAsExpected(120,
                        () -> DirectDebitsService.getDirectDebitMandates(secretKey, Optional.empty(), authenticationToken),
                        x -> x.statusCode() == SC_OK &&
                        x.as(GetDirectDebitMandatesResponse.class).getMandate().stream()
                                .anyMatch(y -> y.getMerchantName().equals(simulateCreateMandateModel.getMerchantName())),
                                Optional.of(String.format("Expecting 200 with a mandate that matches merchant name %s, check logged payload", simulateCreateMandateModel.getMerchantName())))
                        .as(GetDirectDebitMandatesResponse.class);

        return Pair.of(response.getMandate().get(0).getId(), simulateCreateMandateModel);
    }

    protected static Map<String, SimulateCreateMandateModel> createMandates(final Pair<String, FasterPaymentsBankDetailsModel> managedAccount,
                                                                            final String authenticationToken,
                                                                            final int count) {

        final List<SimulateCreateMandateModel> simulateCreateMandateModels = new ArrayList<>();
        final Map<String, SimulateCreateMandateModel> mandates = new HashMap<>();

        IntStream.range(0, count)
                .forEach(i -> {
                    final SimulateCreateMandateModel.Builder simulateCreateMandateModel =
                            SimulateCreateMandateModel.createMandateByAccountId(managedAccount);
                    final String ddiId =
                            SimulatorHelper.createMandate(simulateCreateMandateModel.build(), secretKey, authenticationToken)
                            .jsonPath().getString("ddiId");
                    simulateCreateMandateModels.add(simulateCreateMandateModel.setDdiId(ddiId).build());
                });

        final GetDirectDebitMandatesResponse response =
                TestHelper.ensureAsExpected(120,
                                () -> DirectDebitsService.getDirectDebitMandates(secretKey, Optional.empty(), authenticationToken),
                                x -> x.statusCode() == SC_OK &&
                                        x.as(GetDirectDebitMandatesResponse.class).getCount() == count,
                                Optional.of(String.format("Expecting 200 with %s number of mandates, check logged payload", count)))
                        .as(GetDirectDebitMandatesResponse.class);

        IntStream.range(0, simulateCreateMandateModels.size()).forEach(i -> {

            final SimulateCreateMandateModel simulateCreateMandateModel = simulateCreateMandateModels.get(i);
            final List<GetMandateModel> mandateModel =
                    response.getMandate().stream().filter(x -> x.getMerchantName().equals(simulateCreateMandateModel.getMerchantName())).collect(Collectors.toList());
            mandates.put(mandateModel.get(0).getId(), simulateCreateMandateModel);
        });

        return mandates;
    }

    protected static List<Pair<String, String>> createMandateCollections(final Pair<String, String> mandateIds,
                                                                         final Currency currency,
                                                                         final List<Long> amounts,
                                                                         final String authenticationToken) {

        final List<Pair<String, String>> collections = new ArrayList<>();
        final Map<String, Long> providerIdAmountPair = new HashMap<>();

        amounts.forEach(amount -> {

            final SimulateCreateCollectionModel simulateCreateCollectionModel =
                    SimulateCreateCollectionModel.createCollection(currency, amount).build();

            final String simulatedProviderId = SimulatorHelper.createMandateCollection(simulateCreateCollectionModel, mandateIds.getRight(),
                    secretKey, authenticationToken)
                    .jsonPath().get("cid");

            providerIdAmountPair.put(simulatedProviderId, amount);
        });

        final GetMandateCollectionsResponse response =
                TestHelper.ensureAsExpected(120,
                                () -> DirectDebitsService.getDirectDebitMandateCollections(secretKey, mandateIds.getLeft(), Optional.empty(), authenticationToken),
                                x -> x.statusCode() == SC_OK &&
                                        x.as(GetMandateCollectionsResponse.class).getResponseCount() == amounts.size(),
                                Optional.of(String.format("Expecting 200 with a response count of %s, check logged payload", amounts.size())))
                        .as(GetMandateCollectionsResponse.class);

        new TreeMap<>(providerIdAmountPair).forEach((key, value) -> {

            final GetCollectionModel mandateCollection =
                    response.getCollection().stream().filter(x -> x.getAmount().getAmount().equals(value)).collect(Collectors.toList()).get(0);
            collections.add(Pair.of(mandateCollection.getId(), key));
        });

        return collections;
    }

    protected static Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> createMandateWithCollection(final Pair<String, FasterPaymentsBankDetailsModel> managedAccount,
                                                                                                                           final String currency,
                                                                                                                           final Long amount,
                                                                                                                           final String authenticationToken) {

        final Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandateCollections = new HashMap<>();

        final Pair<String, SimulateCreateMandateModel> mandateIds = createMandate(managedAccount, authenticationToken);
        mandateCollections.put(Pair.of(mandateIds.getLeft(), mandateIds.getRight()),
                createMandateCollections(Pair.of(mandateIds.getKey(), mandateIds.getRight().getDdiId()), Currency.valueOf(currency), Collections.singletonList(amount), authenticationToken));

        return mandateCollections;
    }

    protected static Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> createMandatesWithCollections(final Pair<String, FasterPaymentsBankDetailsModel> managedAccount,
                                                                                                                             final String currency,
                                                                                                                             final List<Long> amounts,
                                                                                                                             final String authenticationToken,
                                                                                                                             final int mandateCount) {
        final Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandateCollections = new HashMap<>();

        final Map<String, SimulateCreateMandateModel> mandateIds = createMandates(managedAccount, authenticationToken, mandateCount);
        mandateIds.forEach((key, value) -> mandateCollections.put(Pair.of(key, value),
                createMandateCollections(Pair.of(key, value.getDdiId()), Currency.valueOf(currency), amounts, authenticationToken)));

        return mandateCollections;
    }
}
