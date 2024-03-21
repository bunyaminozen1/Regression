package opc.performance.multi.managedcards;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import opc.enums.opc.CardLevelClassification;
import commons.enums.Currency;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.listFeeder;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class DeleteSpendRulesLoadTests  extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private String identityToken;
    private final List<Map<String, Object>> managedCards = new ArrayList<>();
    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getTestApplication();
        createCards();
    }

    final FeederBuilder<Object> managedCardFeeder = listFeeder(managedCards);

    final ChainBuilder deleteRules =
            exec(http("DeleteRules")
                        .delete(String.format("/multi/managed_cards/%s/spend_rules", "#{cardId}"))
                        .check(status().is(204)));

    final ScenarioBuilder delete = scenario("DeleteRules")
            .feed(managedCardFeeder)
            .exec(deleteRules);

    {
        setUp(
                delete.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(40).during(40))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol(identityToken));
    }

    private void createCards() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(testApplication.getCorporatesProfileId())
                        .setBaseCurrency(Currency.EUR.name())
                        .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, testApplication.getSecretKey());

        identityToken = corporate.getRight();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(testApplication.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        createCorporateModel.getBaseCurrency(), testApplication.getSecretKey(), identityToken);

        final List<String> managedCards = ManagedCardsHelper.cardBatchWithSpendRules(managedAccountId,
                testApplication.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(),
                testApplication.getCorporateNitecrestEeaDebitManagedCardsProfileId(),
                createCorporateModel.getBaseCurrency(), CardLevelClassification.CORPORATE,
                InnovatorHelper.loginInnovator(testApplication.getInnovatorEmail(), testApplication.getInnovatorPassword()),
                500, testApplication.getSecretKey(), identityToken);

        managedCards.forEach(card -> this.managedCards.add(Collections.singletonMap("cardId", card)));
    }
}