package opc.performance.multi.transfers;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import commons.enums.Currency;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class CreateTransfersLoadTests extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private String authenticationToken;
    private final List<String> sourceAccounts = new ArrayList<>();
    private final List<String> destinationAccounts = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getTestApplication();
        createIdentityWithAccounts();
    }

    final Iterator<Map<String, Object>> managedAccountFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        final String sourceManagedAccountId = sourceAccounts.get(new Random().nextInt(sourceAccounts.size()));
                        final String destinationManagedAccountId = destinationAccounts.get(new Random().nextInt(destinationAccounts.size()));
                        return Map.of("sourceManagedAccountId", sourceManagedAccountId,
                                "destinationManagedAccountId", destinationManagedAccountId);
                    }
            ).iterator();

    final ChainBuilder createTransferLoad =
            exec(http("Create Transfer Load")
                    .post("/multi/transfers")
                    .body(StringBody(TransferFundsModel.transferFundsString(testApplication.getTransfersProfileId(),
                            "#{sourceManagedAccountId}",
                            "#{destinationManagedAccountId}")))
                    .check(status().is(200)));

    final ChainBuilder createTransferLongRun =
            exec(http("Create Transfer Long Run")
                    .post("/multi/transfers")
                    .body(StringBody(TransferFundsModel.transferFundsString(testApplication.getTransfersProfileId(),
                            "#{sourceManagedAccountId}",
                            "#{destinationManagedAccountId}")))
                    .check(status().is(200)));

    final ScenarioBuilder loadScenario =
            scenario("CreateTransferLoad").feed(managedAccountFeeder).exec(createTransferLoad);

    final ScenarioBuilder longRunScenario =
            scenario("CreateTransferLongRun").feed(managedAccountFeeder).exec(createTransferLongRun);

    {
        setUp(
                loadScenario.injectClosed(constantConcurrentUsers(1).during(10),
                                rampConcurrentUsers(1).to(50).during(60))
                        .andThen(longRunScenario.injectClosed(constantConcurrentUsers(1).during(10),
                                rampConcurrentUsers(1).to(10).during(150),
                                rampConcurrentUsers(1).to(20).during(150)))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol(authenticationToken));
    }

    private void createIdentityWithAccounts() {

        authenticationToken =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(testApplication.getCorporatesProfileId(),
                                testApplication.getSecretKey())
                        .getRight();

        IntStream.range(0, 10).forEach(i -> {
            final String sourceManagedAccountId =
                    ManagedAccountsHelper.createFundedManagedAccount(testApplication.getCorporatePayneticsEeaManagedAccountsProfileId(), Currency.EUR.name(),
                            testApplication.getSecretKey(), authenticationToken, 50000L).getLeft();
            sourceAccounts.add(sourceManagedAccountId);

            final String destinationManagedAccountId =
                    ManagedAccountsHelper.createManagedAccount(testApplication.getCorporatePayneticsEeaManagedAccountsProfileId(), Currency.EUR.name(),
                            testApplication.getSecretKey(), authenticationToken);
            destinationAccounts.add(destinationManagedAccountId);
        });
    }
}