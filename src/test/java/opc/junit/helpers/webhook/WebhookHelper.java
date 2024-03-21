package opc.junit.helpers.webhook;

import com.fasterxml.jackson.databind.node.TextNode;
import io.cucumber.messages.internal.com.google.gson.Gson;
import io.restassured.path.json.JsonPath;
import commons.config.ConfigHelper;
import opc.enums.opc.ApiDocument;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.UrlType;
import opc.enums.opc.WebhookType;
import opc.junit.helpers.TestHelper;
import opc.models.webhook.WebhookDataResponse;
import opc.models.webhook.WebhookResponse;
import opc.services.webhook.WebhookService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.collect;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebhookHelper {

    public static Pair<String, String> generateWebhookUrl() {
        return getUrl(Optional.empty());
    }

    public static Pair<String, String> generateWebhookUrl(final Map<String, String> response) {
        return getUrl(Optional.of(response));
    }

    private static Pair<String, String> getUrl(final Optional<Map<String, String>> response) {

        final String baseUrl =
                ConfigHelper.getEnvironmentConfiguration().getBaseUrl(UrlType.WEBHOOK);

        final String uuid = TestHelper.ensureAsExpected(15,
                () -> response.isPresent() ? WebhookService.generateWebhookId(new Gson().toJson(response.get())) : WebhookService.generateWebhookId(),
                SC_CREATED).jsonPath().get("uuid");

        return Pair.of(uuid, String.format("%s%s", baseUrl, uuid));
    }

    public static WebhookDataResponse getWebhookServiceEvent(final String uuid,
                                                             final long timestamp,
                                                             final WebhookType webhookType) {

        return getWebhookServiceEvents(uuid, timestamp, webhookType, 1).get(0);
    }

    public static List<WebhookDataResponse> getWebhookServiceEvents(final String uuid,
                                                                    final long timestamp,
                                                                    final WebhookType webhookType,
                                                                    final int expectedEventCount) {
        final WebhookResponse webhookResponse =
                TestHelper.ensureAsExpected(240,
                        () -> WebhookService.getWebhookEvents(uuid),
                        x -> x.as(WebhookResponse.class).getData()
                                .stream()
                                .filter(y ->
                                        y.getUrl().contains(webhookType.getValue())
                                                && Long.parseLong(y.getHeaders().getPublishedTimestamp().get(0)) > timestamp)
                                .count() == expectedEventCount,
                        Optional.of(String.format("Expecting a webhook event count of %s, check logged payload", expectedEventCount))).as(WebhookResponse.class);

        return webhookResponse.getData().stream().filter(x -> x.getUrl().contains(webhookType.getValue()) &&
                Long.parseLong(x.getHeaders().getPublishedTimestamp().get(0)) > timestamp).collect(Collectors.toList());
    }


    public static Map<WebhookType, WebhookDataResponse> getWebhookServiceEvents(final String uuid, final long timestamp, final List<WebhookType> webhookTypes) {
        final Map<WebhookType, WebhookDataResponse> webhookDataModels = new HashMap<>();

        webhookTypes.forEach(type -> webhookDataModels.put(type, getWebhookServiceEvent(uuid, timestamp, type)));

        return webhookDataModels;
    }

    public static <T> Object getWebhookServiceEvent(final String uuid,
                                                    final long timestamp,
                                                    final WebhookType webhookType,
                                                    final Pair<String, String> webhookFilter,
                                                    final Class<T> responseClass,
                                                    final ApiSchemaDefinition apiSchema) {
        final WebhookResponse webhookResponse =
                TestHelper.ensureAsExpected(240,
                        () -> WebhookService.getWebhookEvents(uuid),
                        x -> x.as(WebhookResponse.class).getData()
                                .stream()
                                .filter(y ->
                                        y.getUrl().contains(webhookType.getValue())
                                                && Long.parseLong(y.getHeaders().getPublishedTimestamp().get(0)) > timestamp
                                                && JsonPath.from(y.getContent()).getString(webhookFilter.getLeft()).equals(webhookFilter.getRight()))
                                .count() == 1,
                        Optional.of(String.format("Expecting a webhook event (%s) count of %s, check logged payload for id %s with uuid %s, timestamp: %s",
                                webhookType.getValue(), 1, webhookFilter.getRight(), uuid, timestamp))).as(WebhookResponse.class);

        final WebhookDataResponse webhookDataResponse =
                webhookResponse.getData().stream().filter(x -> x.getUrl().contains(webhookType.getValue()) &&
                        Long.parseLong(x.getHeaders().getPublishedTimestamp().get(0)) > timestamp
                        && JsonPath.from(x.getContent()).getString(webhookFilter.getLeft()).equals(webhookFilter.getRight())).collect(Collectors.toList()).get(0);

        TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, apiSchema, webhookDataResponse.getContent());

        return new Gson().fromJson(webhookDataResponse.getContent(), responseClass);
    }

    public static <T> Object getPluginWebhookServiceEvent(final String uuid,
                                                          final long timestamp,
                                                          final String webhookType,
                                                          final Pair<String, String> webhookFilter,
                                                          final Class<T> responseClass,
                                                          final ApiSchemaDefinition apiSchema) {
        final WebhookResponse webhookResponse =
                TestHelper.ensureAsExpected(120,
                        () -> WebhookService.getWebhookEvents(uuid),
                        x -> x.as(WebhookResponse.class).getData()
                                .stream()
                                .filter(y ->
                                        Long.parseLong(y.getHeaders().getPublishedTimestamp().get(0)) > timestamp
                                                && JsonPath.from(y.getContent()).getString("type").equals(webhookType)
                                                && JsonPath.from(y.getContent()).getString(webhookFilter.getLeft()).equals(webhookFilter.getRight()))
                                .count() == 1,
                        Optional.of(String.format("Expecting a webhook event (%s) count of %s, check logged payload for id %s with uuid %s, timestamp: %s",
                                webhookType, 1, webhookFilter.getRight(), uuid, timestamp))).as(WebhookResponse.class);


        final WebhookDataResponse webhookDataResponse =
                webhookResponse.getData().stream().filter(x ->
                        Long.parseLong(x.getHeaders().getPublishedTimestamp().get(0)) > timestamp
                                && JsonPath.from(x.getContent()).getString("type").equals(webhookType)
                                && JsonPath.from(x.getContent()).getString(webhookFilter.getLeft()).equals(webhookFilter.getRight())).collect(Collectors.toList()).get(0);
        TestHelper.validateSchemaDefinition(ApiDocument.PLUGIN_WEBHOOK, apiSchema, webhookDataResponse.getContent());

        return new Gson().fromJson(webhookDataResponse.getContent(), responseClass);
    }

    public static <T> Object getPluginWebhookServiceEventByState(final String uuid,
                                                                 final long timestamp,
                                                                 final String webhookType,
                                                                 final Pair<String, String> webhookFilter,
                                                                 final Pair<String, String> webhookFilterState,
                                                                 final Class<T> responseClass,
                                                                 final ApiSchemaDefinition apiSchema) {
        final WebhookResponse webhookResponse =
                TestHelper.ensureAsExpected(120,
                        () -> WebhookService.getWebhookEvents(uuid),
                        x -> x.as(WebhookResponse.class).getData()
                                .stream()
                                .filter(y ->
                                        Long.parseLong(y.getHeaders().getPublishedTimestamp().get(0)) > timestamp
                                                && JsonPath.from(y.getContent()).getString("type").equals(webhookType)
                                                && JsonPath.from(y.getContent()).getString(webhookFilterState.getLeft()).equals(webhookFilterState.getRight())
                                                && JsonPath.from(y.getContent()).getString(webhookFilter.getLeft()).equals(webhookFilter.getRight()))
                                .count() == 1,
                        Optional.of(String.format("Expecting a webhook event (%s) count of %s, check logged payload for id %s with uuid %s, timestamp: %s",
                                webhookType, 1, webhookFilter.getRight(), uuid, timestamp))).as(WebhookResponse.class);


        final WebhookDataResponse webhookDataResponse =
                webhookResponse.getData().stream().filter(x ->
                        Long.parseLong(x.getHeaders().getPublishedTimestamp().get(0)) > timestamp
                                && JsonPath.from(x.getContent()).getString("type").equals(webhookType)
                                && JsonPath.from(x.getContent()).getString(webhookFilterState.getLeft()).equals(webhookFilterState.getRight())
                                && JsonPath.from(x.getContent()).getString(webhookFilter.getLeft()).equals(webhookFilter.getRight()))
                        .collect(Collectors.toList()).get(0);
        TestHelper.validateSchemaDefinition(ApiDocument.PLUGIN_WEBHOOK, apiSchema, webhookDataResponse.getContent());

        return new Gson().fromJson(webhookDataResponse.getContent(), responseClass);
    }

    public static <T> List<T> getWebhookServiceEvents(final String uuid,
                                                      final long timestamp,
                                                      final WebhookType webhookType,
                                                      final Pair<String, String> webhookFilter,
                                                      final Class<T> responseClass,
                                                      final ApiSchemaDefinition apiSchema,
                                                      final int expectedEventCount) {
        final WebhookResponse webhookResponse =
                TestHelper.ensureAsExpected(240,
                        () -> WebhookService.getWebhookEvents(uuid),
                        x -> x.as(WebhookResponse.class).getData()
                                .stream()
                                .filter(y ->
                                        y.getUrl().contains(webhookType.getValue())
                                                && Long.parseLong(y.getHeaders().getPublishedTimestamp().get(0)) > timestamp
                                                && JsonPath.from(y.getContent()).getString(webhookFilter.getLeft()).equals(webhookFilter.getRight()))
                                .count() == expectedEventCount,
                        Optional.of(String.format("Expecting a webhook event (%s) count of %s, check logged payload for id %s with uuid %s, timestamp: %s",
                                webhookType.getValue(), 1, webhookFilter.getRight(), uuid, timestamp))).as(WebhookResponse.class);

        final List<WebhookDataResponse> listResponses = webhookResponse.getData().stream().filter(x -> x.getUrl().contains(webhookType.getValue()) &&
                        Long.parseLong(x.getHeaders().getPublishedTimestamp().get(0)) > timestamp
                        && JsonPath.from(x.getContent()).getString(webhookFilter.getLeft()).equals(webhookFilter.getRight()))
                .collect(Collectors.toList());

        listResponses.forEach(event -> TestHelper.validateSchemaDefinition(ApiDocument.WEBHOOK, apiSchema,
                event.getContent()));

        final List<T> listEvents = new ArrayList<>();
        listResponses.forEach(event -> listEvents.add(new Gson().fromJson(event.getContent(), responseClass)));

        return listEvents;
    }

    public static String maskData(final String text) {
        final int unmaskedAmount = text.length() / 5;

        return new TextNode(StringUtils.overlay(text, "***", unmaskedAmount,
                text.length() - unmaskedAmount)).asText();
    }
}
