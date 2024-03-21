package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BuyerBeneficiaryAdditionalInformationModel {
    private String adminUserEmail;
    private BuyerBeneficiaryEventModel beneficiary;
    private String buyerId;
    private String buyerName;
    private String kybStatus;
}
