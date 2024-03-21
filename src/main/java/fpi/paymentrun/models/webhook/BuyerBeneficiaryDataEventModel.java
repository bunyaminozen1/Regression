package fpi.paymentrun.models.webhook;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BuyerBeneficiaryDataEventModel {
    private BuyerBeneficiaryAdditionalInformationModel additionalInformation;
    private List<String> event;
    private String[] eventDetails;
    private String rejectionComment;
}
