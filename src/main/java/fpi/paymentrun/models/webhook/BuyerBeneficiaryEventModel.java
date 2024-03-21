package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BuyerBeneficiaryEventModel {
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private String ongoingKybStatus;
    private String type;
    private String status;
}
