package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Getter
@Setter
@Builder
public class LinkedAccountWebhookDataEventModel {
    private String id;
    private LinkedHashMap<String, String> accountIdentification;
    private String currency;
    private InstitutionEventModel institution;
    private ConsentEventModel consent;
    private String status;

}
