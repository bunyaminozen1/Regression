package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import opc.helpers.OpenBankingKeys;

@Data
@Builder
public class CreateThirdPartyProviderCertificateModel {

    private String state;
    private String certificate;

    public static CreateThirdPartyProviderCertificateModel defaultCreateThirdPartyProviderCertificateModel() {
        return CreateThirdPartyProviderCertificateModel.builder()
                .state("ENABLED")
                .certificate(OpenBankingKeys.CLIENT_CERTIFICATE)
                .build();
    }
}
