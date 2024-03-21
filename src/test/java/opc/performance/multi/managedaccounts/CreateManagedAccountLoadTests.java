package opc.performance.multi.managedaccounts;

import commons.enums.Currency;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class CreateManagedAccountLoadTests extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final String authenticationToken;
    private final String managedAccountProfileId;

    {
        before();
        performanceHelper = new PerformanceHelper();
        final ProgrammeDetailsModel testApplication = performanceHelper.getTestApplication();
        authenticationToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(testApplication.getCorporatesProfileId(), testApplication.getSecretKey()).getRight();
        managedAccountProfileId = testApplication.getCorporatePayneticsEeaManagedAccountsProfileId();
    }

    final ChainBuilder createAccountLoad =
            exec(http("Create Account Load")
                    .post("/multi/managed_accounts")
                    .body(StringBody(CreateManagedAccountModel.createManagedAccountStringModel(managedAccountProfileId, Currency.getRandomCurrency().name())))
                    .check(status().is(200)));

    final ChainBuilder createAccountLongRun =
            exec(http("Create Account Long Run")
                    .post("/multi/managed_accounts")
                    .body(StringBody(CreateManagedAccountModel.createManagedAccountStringModel(managedAccountProfileId, Currency.getRandomCurrency().name())))
                    .check(status().is(200)));
    final ScenarioBuilder loadScenario = scenario("CreateAccountLong").exec(createAccountLoad);
    final ScenarioBuilder longRunScenario = scenario("CreateAccountLongRun").exec(createAccountLongRun);

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
