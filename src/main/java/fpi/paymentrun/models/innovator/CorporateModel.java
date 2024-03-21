package fpi.paymentrun.models.innovator;

import commons.models.innovator.IdentityProfileAuthenticationModel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class CorporateModel {

    private List<String> companyType;
    private String baseUrl;
    private String baseCurrency;
    private String emailSender;
    private IdentityProfileAuthenticationModel accountInformationFactors;
    private IdentityProfileAuthenticationModel paymentInitiationFactors;
    private IdentityProfileAuthenticationModel beneficiaryManagementFactors;
    private IdentityProfileAuthenticationModel threedsInitiationFactors;
}
