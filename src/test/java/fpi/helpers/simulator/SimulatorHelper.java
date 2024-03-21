package fpi.helpers.simulator;

import fpi.paymentrun.models.simulator.SimulateLinkedAccountModel;
import fpi.paymentrun.services.simulator.SimulatorService;
import io.restassured.response.ValidatableResponse;
import opc.junit.helpers.TestHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_CREATED;
import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;

public class SimulatorHelper {

    public static Pair<String, SimulateLinkedAccountModel> createLinkedAccount(final String buyerId,
                                                                               final String institutionId,
                                                                               final String secretKey) {
        final SimulateLinkedAccountModel simulateLinkedAccountModel =
                SimulateLinkedAccountModel.defaultSimulateLinkedAccountModel(institutionId, buyerId).build();

        final String linkedAccountId =
                TestHelper.ensureAsExpected(15,
                                () -> SimulatorService.simulateLinkedAccount(secretKey, simulateLinkedAccountModel),
                                SC_CREATED)
                        .jsonPath()
                        .get("id");

        return Pair.of(linkedAccountId, simulateLinkedAccountModel);
    }

    public static ValidatableResponse simulateLinkedAccount(final String buyerId,
                                                            final String institutionId,
                                                            final String secretKey) {
        final SimulateLinkedAccountModel simulateLinkedAccountModel =
                SimulateLinkedAccountModel.defaultSimulateLinkedAccountModel(institutionId, buyerId).build();

        final ValidatableResponse linkedAccount =
                TestHelper.ensureAsExpected(15,
                        () -> SimulatorService.simulateLinkedAccount(secretKey, simulateLinkedAccountModel),
                        SC_CREATED).then();

        return linkedAccount;
    }

    public static List<String> createLinkedAccounts(final Pair<String, String> buyer,
                                                    final int numberOfAccounts,
                                                    final String secretKey) {
        final List<String> accounts = new ArrayList<>();
        IntStream.range(0, numberOfAccounts)
                .forEach(x -> {
                    final Pair<String, SimulateLinkedAccountModel> account =
                            createLinkedAccount(buyer.getLeft(), secretKey);
                    accounts.add(account.getLeft());
                });
        return accounts;
    }

    public static Pair<String, SimulateLinkedAccountModel> createLinkedAccount(final String buyerId,
                                                                               final String secretKey) {
        final SimulateLinkedAccountModel simulateLinkedAccountModel =
                SimulateLinkedAccountModel.defaultSimulateLinkedAccountModel(buyerId).build();

        final String linkedAccountId =
                TestHelper.ensureAsExpected(15,
                                () -> SimulatorService.simulateLinkedAccount(secretKey, simulateLinkedAccountModel),
                                SC_CREATED)
                        .jsonPath()
                        .get("id");

        return Pair.of(linkedAccountId, simulateLinkedAccountModel);
    }

    public static void simulateFunding(final String paymentRunId,
                                       final String groupReference,
                                       final String secretKey) {
        TestHelper.ensureAsExpected(15,
                () -> SimulatorService.simulateFunding(paymentRunId, groupReference, secretKey),
                SC_NO_CONTENT);
    }
}
