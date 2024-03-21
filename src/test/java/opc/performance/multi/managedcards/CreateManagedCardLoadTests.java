package opc.performance.multi.managedcards;

import commons.enums.Currency;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class CreateManagedCardLoadTests extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final String authenticationToken;
    private final String prepaidProfileId;

    {
        before();
        performanceHelper = new PerformanceHelper();
        final ProgrammeDetailsModel testApplication = performanceHelper.getTestApplication();
        authenticationToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(testApplication.getCorporatesProfileId(), testApplication.getSecretKey()).getRight();
        prepaidProfileId = testApplication.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
    }

    final ChainBuilder createCardLoad =
            exec(http("Create Card Load")
                    .post("/multi/managed_cards")
                    .body(StringBody(CreateManagedCardModel.createPrepaidManagedCardStringModel(prepaidProfileId, Currency.getRandomCurrency().name())))
                    .check(status().is(200)));

    final ChainBuilder createCardLongRun =
            exec(http("Create Card Long Run")
                    .post("/multi/managed_cards")
                    .body(StringBody(CreateManagedCardModel.createPrepaidManagedCardStringModel(prepaidProfileId, Currency.getRandomCurrency().name())))
                    .check(status().is(200)));
    final ScenarioBuilder loadScenario = scenario("CreateCardLoad").exec(createCardLoad);
    final ScenarioBuilder longRunScenario = scenario("CreateCardLongRun").exec(createCardLongRun);

    {
        setUp(
                loadScenario.injectClosed(constantConcurrentUsers(1).during(10),
                                rampConcurrentUsers(1).to(50).during(60))
                        .andThen(longRunScenario.injectClosed(constantConcurrentUsers(1).during(10),
                                rampConcurrentUsers(1).to(10).during(150),
                                rampConcurrentUsers(1).to(20).during(150)))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol(authenticationToken));
    }
}
