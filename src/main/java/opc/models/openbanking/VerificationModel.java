package opc.models.openbanking;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class VerificationModel {

    private final String verificationCode;

    public static VerificationModel defaultVerification() {
        return VerificationModel.builder().verificationCode("123456").build();
    }
}
