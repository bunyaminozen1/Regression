package opc.performance.multi.simulator;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class KyiSimulatorTest extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private final List<String> corporateIds = new ArrayList<>();
    private final List<String> consumerIds = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getTestApplication();
        createCorporateIdentities();
        createConsumerIdentities();
    }

    final Iterator<Map<String, Object>> simulateFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        final String corporateId = corporateIds.get(new Random().nextInt(corporateIds.size()));
                        final String consumerId = consumerIds.get(new Random().nextInt(consumerIds.size()));
                        return Map.of("corporateId", corporateId,
                                "consumerId", consumerId);
                    }
            ).iterator();

    final ChainBuilder simulateKyi =
            exec(http("Simulate Kyb Corporate")
                    .post(String.format("/simulate/api/corporates/%s/verify", "#{corporateId}"))
                    .check(status().is(204)))
                    .exec(http("Simulate Kyc Consumer")
                            .post(String.format("/simulate/api/consumers/%s/verify", "#{consumerId}"))
                            .check(status().is(204)));
    final ScenarioBuilder simulateKyb = scenario("Simulate Kyb Corporate")
            .feed(simulateFeeder)
            .exec(simulateKyi);

    final ScenarioBuilder simulateKyc = scenario("Simulate Kyc Consumer")
            .feed(simulateFeeder)
            .exec(simulateKyi);

    {
        setUp(
                simulateKyb.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(50).during(60)),
                simulateKyc.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(50).during(60))
        ).protocols(performanceHelper.apiKeyProtocolSimulator());
    }

    private void createCorporateIdentities() {

        IntStream.range(0, 10).forEach(i -> {

            final Pair<String, String> corporate =
                    CorporatesHelper.createAuthenticatedCorporate(testApplication.getCorporatesProfileId(), testApplication.getSecretKey());
            corporateIds.add(corporate.getLeft());
        });
    }

    private void createConsumerIdentities() {

        IntStream.range(0, 10).forEach(i -> {

            final Pair<String, String> consumer =
                    ConsumersHelper.createAuthenticatedConsumer(testApplication.getConsumersProfileId(), testApplication.getSecretKey());
            consumerIds.add(consumer.getLeft());
        });
    }
}
