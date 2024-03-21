package opc.models.webhook;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookConfiguredPropsModel {

    private WebhookHttpInterfaceModel httpInterface;
    public WebhookHttpInterfaceModel getHttpInterface() { return httpInterface;  }

}