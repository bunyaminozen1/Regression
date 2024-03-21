package opc.performance.multi.authentication;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.enums.opc.IdentityType;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static opc.models.multi.consumers.CreateConsumerModel.createConsumerString;
import static opc.models.multi.corporates.CreateCorporateModel.createCorporateString;
import static opc.models.multi.passwords.CreatePasswordModel.defaultCreatePasswordStringModel;

public class CreatePasswordLoadTests extends Simulation {
  private final PerformanceHelper performanceHelper;
  private final ProgrammeDetailsModel testApplication;
  private final List<IdentityType> identityTypes = List.of(IdentityType.CONSUMER, IdentityType.CORPORATE);

  {
    before();
    performanceHelper = new PerformanceHelper();
    testApplication = performanceHelper.getTestApplication();
  }

  final Iterator<Map<String, Object>> identityFeeder =
          Stream.generate(() -> {
                    final String email = String.format("%s%s@weavrtest.io", System.currentTimeMillis(),
                            RandomStringUtils.randomAlphabetic(5));
                    final IdentityType identityType = identityTypes.get(new Random().nextInt(identityTypes.size()));
                    final Object identityPayload = identityType.equals(IdentityType.CORPORATE) ?
                            createCorporateString(testApplication.getCorporatesProfileId(), email) :
                            createConsumerString(testApplication.getConsumersProfileId(), email);
                    return Map.of("identityType", identityType.getValue(), "identityPayload", identityPayload);
                  }
          ).iterator();

  final ChainBuilder create =
          exec(http("Create Identity")
                      .post(String.format("/multi/%s", "#{identityType}"))
                      .body(StringBody("#{identityPayload}"))
                      .check(status().is(200))
                      .check(jsonPath("$.id.id").saveAs("identityId")))
                  .pause(1)
                  .exec(http("Create Password")
                          .post("/multi/passwords/${identityId}/create")
                          .body(StringBody(defaultCreatePasswordStringModel()))
                          .check(status().is(200)))
                  .pause(1);

  final ScenarioBuilder password = scenario("CreatePassword")
          .feed(identityFeeder)
          .exec(create);

  {
    setUp(
            password.injectClosed(constantConcurrentUsers(1).during(20),
                    rampConcurrentUsers(1).to(5).during(60))
    ).protocols(performanceHelper.apiKeyProtocol());
  }
}