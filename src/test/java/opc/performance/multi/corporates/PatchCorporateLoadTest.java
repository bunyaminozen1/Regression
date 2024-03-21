package opc.performance.multi.corporates;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.PatchCorporateModel;
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

public class PatchCorporateLoadTest extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private final List<String> corporateTokens = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getTestApplication();
        createCorporateIdentities();
    }

    final Iterator<Map<String, Object>> corporateFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                final String corporateToken = corporateTokens.get(new Random().nextInt(corporateTokens.size()));
                return Collections.singletonMap("corporateToken", corporateToken);
                    }
            ).iterator();

    final ChainBuilder updateCorporate =
            exec(http("Update Corporate")
                    .patch("/multi/corporates")
                    .body(StringBody(PatchCorporateModel.patchCorporateString()))
                    .check(status().is(200)));
    final ScenarioBuilder update = scenario("Update Corporate")
            .feed(corporateFeeder)
            .exec(updateCorporate);

    {
        setUp(
                update.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(50).during(60))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol("#{corporateToken}"));
    }

    private void createCorporateIdentities() {

        IntStream.range(0, 10).forEach(i -> {

            final Pair<String, String> corporate =
                    CorporatesHelper.createAuthenticatedCorporate(testApplication.getCorporatesProfileId(), testApplication.getSecretKey());

            corporateTokens.add(corporate.getRight());
        });
    }
}