package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import opc.models.shared.AddressModel;
import opc.models.shared.CurrencyAmount;

import java.util.List;

@Data
@Builder
@Getter
@Setter
public class ModulrSubscriptionInfoModel {

    private String name;
    private String type;
    private String email;
    private String industry;
    private AddressModel registrationAddress;
    private ModulrApplicantModel applicant;
    private List<ModulrSupportingDocumentsModel> supportingDocuments;
    private AddressModel tradingAddress;
    private CurrencyAmount expectedMonthlySpend;
    private List<ModulrApplicantModel> associates;
}
