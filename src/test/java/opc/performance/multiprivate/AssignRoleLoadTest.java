package opc.performance.multiprivate;

import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.models.admin.AssignRoleModel;
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

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class AssignRoleLoadTest extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private String buyerToken;
    private final List<String> userIds = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getPluginsTestApplication();
        createBuyers();
    }

    final Iterator<Map<String, Object>> buyerFeeder = Stream.generate((Supplier<Map<String, Object>>) () -> {
                final String userId = userIds.get(new Random().nextInt(userIds.size()));
                return Map.of("userId", userId);
            }
    ).iterator();

    final ChainBuilder assignRole =
            exec(http("Assign Role")
                    .post(String.format("/multi_private/users/%s/roles", "#{userId}"))
                    .body(StringBody(AssignRoleModel.assignRoleModelString("883161961923504128")))
                    .check(status().is(200)));
    final ScenarioBuilder assignRoleScenario = scenario("AssignRole")
            .feed(buyerFeeder)
            .exec(assignRole);

    {
        setUp(
                assignRoleScenario.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(50).during(60))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol(buyerToken, testApplication.getSecretKey()));
    }

    private void createBuyers() {

        final Pair<String, String> buyer =
                BuyersHelper.createAuthenticatedBuyer(testApplication.getSecretKey());

        buyerToken = buyer.getRight();

        IntStream.range(0, 10).forEach(i ->
            userIds.add(BuyerAuthorisedUserHelper.createAuthenticatedUser(testApplication.getSecretKey(), buyerToken).getLeft()));
    }
}