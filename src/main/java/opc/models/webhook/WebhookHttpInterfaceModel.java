package opc.models.webhook;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookHttpInterfaceModel {

    private String endpoint_url;
    public String getEndpoint_url() { return endpoint_url;  }
}
