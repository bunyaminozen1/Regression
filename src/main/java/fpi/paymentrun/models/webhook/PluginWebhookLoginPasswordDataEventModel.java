package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Getter
@Setter
@Builder
public class PluginWebhookLoginPasswordDataEventModel {
    public LinkedHashMap<String, String> credential;
    public String buyerId;
    public String publishedTimestamp;
    public String status;
    public String type;
}
