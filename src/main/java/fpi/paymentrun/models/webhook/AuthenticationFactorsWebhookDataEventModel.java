package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;

@Getter
@Setter
@Builder
public class AuthenticationFactorsWebhookDataEventModel {
    private LinkedHashMap<String, String> credential;
    private String type;
    private String status;
    private String publishedTimestamp;
}
