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
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.performance.PerformanceHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.listFeeder;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class PostSpendRulesLoadTests extends Simulation {

    private final PerformanceHelper performanceHelper;
    private final ProgrammeDetailsModel testApplication;
    private String identityToken;
    private final String currency = Currency.EUR.name();
    private final List<Map<String, Object>> managedDebitCards = new ArrayList<>();
    private final List<Map<String, Object>> managedPrepaidCards = new ArrayList<>();

    {
        before();
        performanceHelper = new PerformanceHelper();
        testApplication = performanceHelper.getTestApplication();
        createIdentity();
        createDebitCards();
        createPrepaidCards();
    }

    final FeederBuilder<Object> managedDebitCardFeeder = listFeeder(managedDebitCards);
    final FeederBuilder<Object> managedPrepaidCardFeeder = listFeeder(managedPrepaidCards);

    final ChainBuilder postRulesDebit =
            exec(http("PostRules")
                    .post(String.format("/multi/managed_cards/%s/spend_rules", "#{cardId}"))
                    .body(StringBody(SpendRulesModel.createFullDefaultSpendRulesModelString(currency)))
                    .check(status().is(204)));
    final ChainBuilder postRulesPrepaid =
            exec(http("PostRules")
                    .post(String.format("/multi/managed_cards/%s/spend_rules", "#{cardId}"))
                    .body(StringBody(SpendRulesModel.createDefaultSpendRulesModelString()))
                    .check(status().is(204)));

    final ScenarioBuilder post = scenario("PostRules")
            .feed(managedDebitCardFeeder)
            .exec(postRulesDebit)
            .feed(managedPrepaidCardFeeder)
            .exec(postRulesPrepaid);

    {
        setUp(
                post.injectClosed(constantConcurrentUsers(1).during(10),
                        rampConcurrentUsers(1).to(40).during(40))
        ).protocols(performanceHelper.apiKeyAuthenticatedProtocol(identityToken));
    }

    private void createIdentity(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(testApplication.getCorporatesProfileId())
                        .setBaseCurrency(currency)
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, testApplication.getSecretKey());

        identityToken = corporate.getRight();
    }

    private void createDebitCards() {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(testApplication.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        currency, testApplication.getSecretKey(), identityToken);

        final List<String> managedCards = ManagedCardsHelper.cardBatchDebitWithoutSpendRules(managedAccountId,
                testApplication.getCorporateNitecrestEeaDebitManagedCardsProfileId(), CardLevelClassification.CORPORATE,
                InnovatorHelper.loginInnovator(testApplication.getInnovatorEmail(), testApplication.getInnovatorPassword()),
                500, testApplication.getSecretKey(), identityToken);

        managedCards.forEach(card -> this.managedDebitCards.add(Collections.singletonMap("cardId", card)));
    }

    private void createPrepaidCards() {

        final List<String> managedCards = ManagedCardsHelper.cardBatchPrepaidWithoutSpendRules(
                testApplication.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), currency, CardLevelClassification.CORPORATE,
                InnovatorHelper.loginInnovator(testApplication.getInnovatorEmail(), testApplication.getInnovatorPassword()),
                500, testApplication.getSecretKey(), identityToken);

        managedCards.forEach(card -> this.managedPrepaidCards.add(Collections.singletonMap("cardId", card)));
    }
}