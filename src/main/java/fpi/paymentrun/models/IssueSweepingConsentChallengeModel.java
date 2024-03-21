package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class IssueSweepingConsentChallengeModel {

    private String linkedAccountId;
    private String managedAccountId;
}
