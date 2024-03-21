package opc.models.webhookcallback;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class WebhookCallbackModel {

    private String payload;
    private String operation;
    private Long programmeId;

    public static WebhookCallbackModel.WebhookCallbackModelBuilder defaultWebhookCallbackModel() {
        return WebhookCallbackModel.builder()
                .payload ("{\"credential\" : {\"id\" : \"111209866929177304\",\"type\" : \"ROOT\"},\"identity\" : {\"id\" : \"111209866929177304\",\"type\" : \"corporates\"},\"publishedTimestamp\" : \"1696927909273\",\"status\" : \"VERIFIED\",\"type\" : \"PASSWORD\"}")
                .operation ("/login/watch");
    }
}


