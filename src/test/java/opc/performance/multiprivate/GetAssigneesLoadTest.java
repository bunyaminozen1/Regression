package opc.performance.multiprivate;

import fpi.helpers.BuyersHelper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
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

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class GetAssigneesLoadTest extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private final List<String> buyerTokens = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getPluginsTestApplication();
        createBuyers();
    }

    final Iterator<Map<String, Object>> buyerFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        final String buyerToken = buyerTokens.get(new Random().nextInt(buyerTokens.size()));
                        return Collections.singletonMap("buyerToken", buyerToken);
                    }
            ).iterator();

    final ChainBuilder getAssignees =
            exec(http("Create Account Load")
                    .get("/multi_private/roles/883161961923504128/assignees")
                    .check(status().is(200)));
    final ScenarioBuilder getAssigneesScenario = scenario("CreateAccountLong")
            .feed(buyerFeeder)
            .exec(getAssignees);

    {
        setUp(
                getAssigneesScenario.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(50).during(60))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol("#{buyerToken}", testApplication.getSecretKey()));
    }

    private void createBuyers() {

        IntStream.range(0, 10).forEach(i -> {

            final Pair<String, String> buyer =
                    BuyersHelper.createAuthenticatedBuyer(testApplication.getSecretKey());

            buyerTokens.add(buyer.getRight());
        });
    }
}