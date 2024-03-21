package opc.performance.multi.consumers;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.PatchConsumerModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
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

public class PatchConsumerLoadTest extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private final List<String> consumerTokens = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getTestApplication();
        createConsumerIdentities();
    }

    final Iterator<Map<String, Object>> consumerFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        final String consumerToken = consumerTokens.get(new Random().nextInt(consumerTokens.size()));
                        return Collections.singletonMap("consumerToken", consumerToken);
                    }
            ).iterator();

    final ChainBuilder updateConsumer =
            exec(http("Update Consumer")
                    .patch("/multi/consumers")
                    .body(StringBody(PatchConsumerModel.patchConsumerString()))
                    .check(status().is(200)));
    final ScenarioBuilder update = scenario("Update Consumer")
            .feed(consumerFeeder)
            .exec(updateConsumer);

    {
        setUp(
                update.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(50).during(60))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol("#{consumerToken}"));
    }

    private void createConsumerIdentities() {

        IntStream.range(0, 10).forEach(i -> {

            final Pair<String, String> consumer =
                    ConsumersHelper.createAuthenticatedConsumer(testApplication.getConsumersProfileId(), testApplication.getSecretKey());

            consumerTokens.add(consumer.getRight());
        });
    }
}