package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.models.webhook.WebhookLoginPasswordEventModel;

@Getter
@Setter
@Builder
public class PluginWebhookLoginPasswordEventModel {

    private String type;
    private PluginWebhookLoginPasswordDataEventModel data;

}
