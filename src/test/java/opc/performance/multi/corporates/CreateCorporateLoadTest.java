package opc.performance.multi.corporates;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.models.multi.corporates.CreateCorporateModel;
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

public class CreateCorporateLoadTest extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final String corporatesProfileId;

    {
        before();
        performanceHelper = new PerformanceHelper();
        final ProgrammeDetailsModel testApplication = performanceHelper.getTestApplication();
        corporatesProfileId = testApplication.getCorporatesProfileId();
    }

    final Iterator<Map<String, Object>> emailFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        final String email = String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                                RandomStringUtils.randomAlphabetic(5));
                        return Collections.singletonMap("email", email);
                    }
            ).iterator();

    final ChainBuilder corporateLoad =
            exec(http("Corporate Load")
                    .post("/multi/corporates")
                    .body(StringBody(CreateCorporateModel.createCorporateString(corporatesProfileId, "#{email}")))
                    .check(status().is(200)));

    final ChainBuilder corporateLongRun =
            exec(http("Corporate Long Run")
                    .post("/multi/corporates")
                    .body(StringBody(CreateCorporateModel.createCorporateString(corporatesProfileId, "#{email}")))
                    .check(status().is(200)));

    final ScenarioBuilder loadScenario = scenario("CorporateLoad").feed(emailFeeder).exec(corporateLoad);
    final ScenarioBuilder longRunScenario = scenario("CorporateLongRun").feed(emailFeeder).exec(corporateLongRun);

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