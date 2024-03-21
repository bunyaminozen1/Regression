package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PluginWebhookKybDataEventModel {
    private String buyerId;
    private String status;
    private String[] details;
    private String rejectionComment;
    private String ongoingStatus;
}
