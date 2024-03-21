package opc.services.webhookcallback;

import commons.services.BaseService;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import opc.enums.opc.UrlType;
import opc.models.webhookcallback.WebhookCallbackModel;

public class WebhookCallbackService extends BaseService {

    public static Response getWebhookCallback (final WebhookCallbackModel webhookCallbackModel,
                                               final String fpiKey) {
        return getBodyWebhookCallbackRequest(webhookCallbackModel, fpiKey)
                .when()
                .post("/webhooks/callbacks");
    }

    protected static RequestSpecification getBodyWebhookCallbackRequest (final Object body, final String fpiKey){
        return webhookCallbackRestAssured()
                .header("Content-type", "application/json")
                .header("Fpi-key", fpiKey)
                .and()
                .body(body);
    }

    private static RequestSpecification webhookCallbackRestAssured() {
        return restAssured(UrlType.WEBHOOK_CALLBACK);
    }
}
