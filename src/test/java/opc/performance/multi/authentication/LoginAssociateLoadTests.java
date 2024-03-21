package opc.performance.multi.authentication;

import commons.enums.Currency;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.LoginModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;

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
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class LoginAssociateLoadTests extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private final List<String> emails = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getTestApplication();
        createUsers();
    }

    final Iterator<Map<String, Object>> emailFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        final String email = emails.get(new Random().nextInt(emails.size()));
                        return Map.of("email", email);
                    }
            ).iterator();

    final ChainBuilder createCardLoad =
            exec(http("Login")
                    .post("/multi/login_with_password")
                    .body(StringBody(LoginModel.loginString("#{email}", TestHelper.DEFAULT_PASSWORD)))
                    .check(status().is(200))
                    .check(jsonPath("$.token").saveAs("token")))
                    .exec(http("Associate")
                            .post("/app/secure/api/session/associate")
                            .header("Content-type", "application/json")
                            .header("Authorization", "Bearer ${token}")
                            .header("origin", "https://qa.weavr.io")
                            .header("programme-key", testApplication.getSharedKey())
                            .check(status().is(200)));
    final ScenarioBuilder loadScenario = scenario("Login")
            .feed(emailFeeder)
            .exec(createCardLoad);

    {
        setUp(
                loadScenario.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(400).during(600))
        ).protocols(performanceHelper.apiKeyProtocol());
    }

    private void createUsers() {

        IntStream.range(0, 20).forEach(i -> {

            final CreateCorporateModel createCorporateModel =
                    CreateCorporateModel.DefaultCreateCorporateModel(testApplication.getCorporatesProfileId())
                            .setBaseCurrency(Currency.EUR.name())
                            .build();
            CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, testApplication.getSecretKey());

            emails.add(createCorporateModel.getRootUser().getEmail());
        });
    }
}