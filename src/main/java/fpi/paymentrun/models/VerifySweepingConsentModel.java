package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class VerifySweepingConsentModel {

    private String verificationCode;

    public static VerifySweepingConsentModel defaultVerificationModel() {
        return defaultVerificationModel("123456");
    }

    public static VerifySweepingConsentModel defaultVerificationModel(final String verificationCode) {
        return VerifySweepingConsentModel.builder().verificationCode(verificationCode).build();
    }
}
