package opc.performance.multi.sends;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import commons.enums.Currency;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.sends.SendFundsModel;
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

public class CreateSendsLoadTests extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private String senderToken;
    private final List<String> senderAccounts = new ArrayList<>();
    private final List<String> receiverAccounts = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getTestApplication();
        createIdentitiesWithAccounts();
    }

    final Iterator<Map<String, Object>> managedAccountFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        final String sourceManagedAccountId = senderAccounts.get(new Random().nextInt(senderAccounts.size()));
                        final String destinationManagedAccountId = receiverAccounts.get(new Random().nextInt(receiverAccounts.size()));
                        return Map.of("sourceManagedAccountId", sourceManagedAccountId,
                                "destinationManagedAccountId", destinationManagedAccountId);
                    }
            ).iterator();

    final ChainBuilder createSendLoad =
            exec(http("Create Send Load")
                    .post("/multi/sends")
                    .body(StringBody(SendFundsModel.sendFundsString(testApplication.getSendProfileId(),
                            "#{sourceManagedAccountId}",
                            "#{destinationManagedAccountId}")))
                    .check(status().is(200)));

    final ChainBuilder createSendLongRun =
            exec(http("Create Send Long Run")
                    .post("/multi/sends")
                    .body(StringBody(SendFundsModel.sendFundsString(testApplication.getSendProfileId(),
                            "#{sourceManagedAccountId}",
                            "#{destinationManagedAccountId}")))
                    .check(status().is(200)));

    final ScenarioBuilder loadScenario =
            scenario("CreateSendLoad").feed(managedAccountFeeder).exec(createSendLoad);

    final ScenarioBuilder longRunScenario = 
            scenario("CreateSendLongRun").feed(managedAccountFeeder).exec(createSendLongRun);

    {
        setUp(
                loadScenario.injectClosed(constantConcurrentUsers(1).during(10),
                                rampConcurrentUsers(1).to(50).during(60))
                        .andThen(longRunScenario.injectClosed(constantConcurrentUsers(1).during(10),
                                rampConcurrentUsers(1).to(10).during(150),
                                rampConcurrentUsers(1).to(20).during(150)))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol(senderToken));
    }

    private void createIdentitiesWithAccounts() {

        senderToken  =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(testApplication.getCorporatesProfileId(),
                                testApplication.getSecretKey())
                        .getRight();

        final String receiverToken =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(testApplication.getConsumersProfileId(),
                        testApplication.getSecretKey()).getRight();

        IntStream.range(0, 20).forEach(i -> {
            final String corporateManagedAccountId =
                    ManagedAccountsHelper.createFundedManagedAccount(testApplication.getCorporatePayneticsEeaManagedAccountsProfileId(), Currency.EUR.name(),
                            testApplication.getSecretKey(), senderToken, 50000L).getLeft();
            senderAccounts.add(corporateManagedAccountId);

            final String consumerManagedAccountId =
                    ManagedAccountsHelper.createManagedAccount(testApplication.getConsumerPayneticsEeaManagedAccountsProfileId(), Currency.EUR.name(),
                            testApplication.getSecretKey(), receiverToken);
            receiverAccounts.add(consumerManagedAccountId);
        });
    }
}