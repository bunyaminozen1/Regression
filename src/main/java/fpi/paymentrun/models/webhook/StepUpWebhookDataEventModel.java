package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Getter
@Setter
@Builder
public class StepUpWebhookDataEventModel {
    private LinkedHashMap<String, String> credential;
    private String buyerId;
    private String challengeId;
    private String type;
    private String status;
    private String publishedTimestamp;
    private String authToken;
}
