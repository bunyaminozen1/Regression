package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Getter
@Setter
@Builder
public class SweepingDataEventModel {
    private String buyerId;
    private String sweepingId;
    private String reference;
    private String transactionDate;
    private LinkedHashMap<String, String> transactionAmount;
    private String status;
    private SweepingDestinationEventModel destination;

}
