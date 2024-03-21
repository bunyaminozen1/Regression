package opc.performance.multi.consumers;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class CreateConsumerLoadTest extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final String consumersProfileId;

    {
        before();
        performanceHelper = new PerformanceHelper();
        final ProgrammeDetailsModel testApplication = performanceHelper.getTestApplication();
        consumersProfileId = testApplication.getConsumersProfileId();
    }

    final Iterator<Map<String, Object>> emailFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        final String email = String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                                RandomStringUtils.randomAlphabetic(5));
                        return Collections.singletonMap("email", email);
                    }
            ).iterator();

    final ChainBuilder consumerLoad =
            exec(http("Consumer Load")
                    .post("/multi/consumers")
                    .body(StringBody(CreateConsumerModel.createConsumerString(consumersProfileId, "#{email}")))
                    .check(status().is(200)));

    final ChainBuilder consumerLongRun =
            exec(http("Consumer Long Run")
                    .post("/multi/consumers")
                    .body(StringBody(CreateConsumerModel.createConsumerString(consumersProfileId, "#{email}")))
                    .check(status().is(200)));

    final ScenarioBuilder loadScenario = scenario("ConsumerLoad").feed(emailFeeder).exec(consumerLoad);
    final ScenarioBuilder longRunScenario = scenario("ConsumerLongRun").feed(emailFeeder).exec(consumerLongRun);

    {
        setUp(
                loadScenario.injectClosed(constantConcurrentUsers(1).during(10),
                                rampConcurrentUsers(1).to(50).during(60))
                        .andThen(longRunScenario.injectClosed(constantConcurrentUsers(1).during(10),
                                rampConcurrentUsers(1).to(10).during(150),
                                rampConcurrentUsers(1).to(20).during(150)))
        ).protocols(performanceHelper.apiKeyProtocol());
    }
}