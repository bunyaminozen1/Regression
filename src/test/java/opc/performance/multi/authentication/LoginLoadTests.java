package opc.performance.multi.authentication;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.LoginModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;

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

public class LoginLoadTests extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final List<String> loginEmail = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        final ProgrammeDetailsModel testApplication = performanceHelper.getTestApplication();
        createIdentities(testApplication.getCorporatesProfileId(),
                testApplication.getConsumersProfileId(), testApplication.getSecretKey());
    }

    final Iterator<Map<String, Object>> emailFeeder =
            Stream.generate((Supplier<Map<String, Object>>) () -> {
                        final String email = loginEmail.get(new Random().nextInt(loginEmail.size()));
                        return Collections.singletonMap("email", email);
                    }
            ).iterator();

    final ChainBuilder create =
            exec(http("Login")
                    .post("/multi/login_with_password")
                    .body(StringBody(LoginModel.loginString("#{email}", TestHelper.DEFAULT_PASSWORD)))
                    .check(status().is(200)));
    final ScenarioBuilder login = scenario("Login")
            .feed(emailFeeder)
            .exec(create);

    {
        setUp(
                login.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(50).during(60))
        ).protocols(performanceHelper.apiKeyProtocol());
    }

    private void createIdentities(final String corporateProfileId,
                                  final String consumerProfileId,
                                  final String secretKey) {
        IntStream.range(0, 10).forEach(i -> {
            final CreateCorporateModel createCorporateModel =
                    CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
            CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
            loginEmail.add(createCorporateModel.getRootUser().getEmail());

            final CreateConsumerModel createConsumerModel =
                    CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
            ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
            loginEmail.add(createConsumerModel.getRootUser().getEmail());
        });
    }
}
