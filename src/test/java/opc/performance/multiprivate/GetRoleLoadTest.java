package opc.performance.multiprivate;

import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.junit.helpers.multiprivate.MultiPrivateHelper;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

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

public class GetRoleLoadTest extends Simulation {

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

    final ChainBuilder getRole =
            exec(http("Get Role")
                    .get(String.format("/multi_private/users/%s/roles", "#{userId}"))
                    .queryParam("plugin", "PAYMENT_RUN")
                    .check(status().is(200)));
    final ScenarioBuilder getRoleScenario = scenario("GetRole")
            .feed(buyerFeeder)
            .exec(getRole);

    {
        setUp(
                getRoleScenario.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(50).during(60))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol(buyerToken, testApplication.getSecretKey()));
    }

    private void createBuyers() {

        final Pair<String, String> buyer =
                BuyersHelper.createAuthenticatedBuyer(testApplication.getSecretKey());

        buyerToken = buyer.getRight();

        IntStream.range(0, 10).forEach(i -> {

            final Triple<String, BuyerAuthorisedUserModel, String> user = BuyerAuthorisedUserHelper.createAuthenticatedUser(testApplication.getSecretKey(), buyerToken);
            MultiPrivateHelper.assignRole(testApplication.getSecretKey(), buyerToken, user.getLeft(), "883161961923504128");
            userIds.add(user.getLeft());
        });
    }
}