package opc.services.webhook;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import opc.enums.opc.UrlType;
import commons.services.BaseService;

public class WebhookService extends BaseService {

    public static Response generateWebhookId() {
        return webhookRestAssured()
                .header("Content-type", "application/json")
                .when()
                .post("/token/");
    }

    public static Response generateWebhookId(final String body) {
        return webhookRestAssured()
                .header("Content-type", "application/json")
                .body(body)
                .when()
                .post("/token/");
    }

    public static Response getWebhookEvents(final String uuid) {
        return webhookRestAssured()
                .header("Content-type", "application/json")
                .pathParam("uuid", uuid)
                .body("{\"per_page\":1000}")
                .when()
                .get("/token/{uuid}/requests");
    }

    public static Response deleteEvents(final String uuid) {
        return webhookRestAssured()
                .header("Content-type", "application/json")
                .pathParam("uuid", uuid)
                .when()
                .delete("/token/{uuid}/request");
    }

    private static RequestSpecification webhookRestAssured() {
        return restAssured(UrlType.WEBHOOK);
    }
}
