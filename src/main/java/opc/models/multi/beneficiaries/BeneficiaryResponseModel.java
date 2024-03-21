package opc.models.multi.beneficiaries;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BeneficiaryResponseModel {

    private BeneficiaryDetailsResponseModel beneficiaryDetails;
    private BeneficiaryInformationResponseModel beneficiaryInformation;
    private List<String> externalRefs;
    private String group;
    private String id;
    private List<RelatedOperationBatchesResponseModel> relatedOperationBatches;
    private String state;
    private String trustLevel;
}
