package opc.junit.helpers.multi;

import io.restassured.response.Response;
import commons.enums.Currency;
import opc.enums.opc.MandateCollectionState;
import opc.enums.opc.MandateState;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.directdebit.GetCollectionModel;
import opc.models.multi.directdebit.GetDirectDebitMandatesResponse;
import opc.models.multi.directdebit.GetMandateCollectionsResponse;
import opc.models.multi.directdebit.RejectCollectionModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.simulator.SimulateCreateCollectionModel;
import opc.models.simulator.SimulateCreateMandateModel;
import opc.services.multi.DirectDebitsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class DirectDebitsHelper {

    public static void cancelMandateInternally(final String mandateId,
                                               final String secretKey,
                                               final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> DirectDebitsService.cancelDirectDebitMandate(secretKey, mandateId, authenticationToken),
                SC_NO_CONTENT);

        checkMandateState(mandateId, MandateState.CANCELLED, secretKey, authenticationToken);
    }

    public static void cancelMandateExternally(final String mandateId,
                                               final String providerReference,
                                               final String secretKey,
                                               final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> SimulatorHelper.cancelMandate(providerReference, secretKey, authenticationToken),
                SC_NO_CONTENT);

        checkMandateState(mandateId, MandateState.CANCELLED, secretKey, authenticationToken);
    }

    public static void expireMandate(final String mandateId,
                                     final String providerReference,
                                     final String secretKey,
                                     final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> SimulatorHelper.expireMandate(providerReference, secretKey, authenticationToken),
                SC_NO_CONTENT);

        checkMandateState(mandateId, MandateState.EXPIRED, secretKey, authenticationToken);
    }

    public static void collectCollection(final String collectionProviderId,
                                         final String mandateProviderId,
                                         final String collectionId,
                                         final String mandateId,
                                         final String secretKey,
                                         final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.collectCollection(collectionProviderId, mandateProviderId, secretKey, authenticationToken),
                SC_OK);

        checkCollectionState(mandateId, collectionId, MandateCollectionState.PAID, secretKey, authenticationToken);
    }

    public static void collectCollection(final String collectionProviderId,
                                         final String mandateProviderId,
                                         final String collectionId,
                                         final String mandateId,
                                         final MandateCollectionState mandateCollectionState,
                                         final String secretKey,
                                         final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.collectCollection(collectionProviderId, mandateProviderId, secretKey, authenticationToken),
                SC_OK);

        checkCollectionState(mandateId, collectionId, mandateCollectionState, secretKey, authenticationToken);
    }

    public static void rejectCollection(final RejectCollectionModel rejectCollectionModel,
                                        final String collectionId,
                                        final String mandateId,
                                        final String secretKey,
                                        final String authenticationToken) {

        TestHelper.ensureAsExpected(15,
                () -> DirectDebitsService.rejectDirectDebitMandateCollection(rejectCollectionModel,
                        secretKey, mandateId, collectionId, authenticationToken),
                SC_NO_CONTENT);

        checkCollectionState(mandateId, collectionId, MandateCollectionState.REJECTED, secretKey, authenticationToken);
    }

    public static Response getDirectDebitMandates(final String secretKey,
                                                  final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> DirectDebitsService.getDirectDebitMandates(secretKey, Optional.empty(), authenticationToken),
                SC_OK);
    }

    public static Response getDirectDebitMandate(final String secretKey,
                                                 final String mandateId,
                                                 final String authenticationToken) {
        return TestHelper.ensureAsExpected(15,
                () -> DirectDebitsService.getDirectDebitMandate(secretKey, mandateId, authenticationToken),
                SC_OK);
    }

    public static void checkMandateState(final String mandateId,
                                         final MandateState mandateState,
                                         final String secretKey,
                                         final String authenticationToken) {

        TestHelper.ensureAsExpected(30,
                () -> DirectDebitsService.getDirectDebitMandate(secretKey, mandateId, authenticationToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("state").equals(mandateState.name()),
                Optional.of(String.format("Expecting 200 with mandate state %s, check logged payload",
                        mandateState.name())));
    }

    public static void checkCollectionState(final String mandateId,
                                            final String collectionId,
                                            final MandateCollectionState mandateCollectionState,
                                            final String secretKey,
                                            final String authenticationToken) {

        TestHelper.ensureAsExpected(60,
                () -> DirectDebitsService.getDirectDebitMandateCollection(secretKey, mandateId, collectionId, authenticationToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("state").equals(mandateCollectionState.name()),
                Optional.of(String.format("Expecting 200 with collection state %s, check logged payload",
                        mandateCollectionState.name())));
    }

    public static Pair<String, SimulateCreateMandateModel> createMandate(final Pair<String, FasterPaymentsBankDetailsModel> managedAccount,
                                                                         final String secretKey,
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
                                        x.as(GetDirectDebitMandatesResponse.class)
                                                .getMandate().stream().anyMatch(y -> y.getMerchantName().equals(simulateCreateMandateModel.getMerchantName())),
                        Optional.of(String.format("Expecting method to return 200 with the merchant name matching %s, check logged payload",
                                simulateCreateMandateModel.getMerchantName())))
                        .as(GetDirectDebitMandatesResponse.class);

        return Pair.of(response.getMandate().get(0).getId(), simulateCreateMandateModel);
    }

    public static Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> createMandateWithCollection(final Pair<String, FasterPaymentsBankDetailsModel> managedAccount,
                                                                                                                        final String currency,
                                                                                                                        final Long amount,
                                                                                                                        final String secretKey,
                                                                                                                        final String authenticationToken) {

        final Map<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandateCollections = new HashMap<>();

        final Pair<String, SimulateCreateMandateModel> mandateIds = createMandate(managedAccount, secretKey, authenticationToken);
        mandateCollections.put(Pair.of(mandateIds.getLeft(), mandateIds.getRight()),
                createMandateCollections(Pair.of(mandateIds.getKey(), mandateIds.getRight().getDdiId()),
                        Currency.valueOf(currency), Collections.singletonList(amount), secretKey, authenticationToken));

        return mandateCollections;
    }

    public static List<Pair<String, String>> createMandateCollections(final Pair<String, String> mandateIds,
                                                                      final Currency currency,
                                                                      final List<Long> amounts,
                                                                      final String secretKey,
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
                                x -> x.statusCode() == SC_OK && x.as(GetMandateCollectionsResponse.class).getResponseCount() == amounts.size(),
                        Optional.of(String.format("Expecting 200 with response count %s, check logged payloads", amounts.size())))
                        .as(GetMandateCollectionsResponse.class);

        providerIdAmountPair.forEach((key, value) -> {

            final GetCollectionModel mandateCollection =
                    response.getCollection().stream().filter(x -> x.getAmount().getAmount().equals(value)).collect(Collectors.toList()).get(0);
            collections.add(Pair.of(mandateCollection.getId(), key));
        });

        return collections;
    }
}
