package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;

@Getter
@Setter
@Builder
public class SweepingDestinationEventModel {
    private String linkedAccountId;
    private LinkedHashMap<String, String> bankAccountDetails;
}
