package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateThirdPartyProviderModel {

    private String name;
    private String state;
    private boolean aisEnabled;
    private boolean pisEnabled;
    private String baseUrl;

    public static CreateThirdPartyProviderModel defaultCreateTtpProviderModel() {
        return CreateThirdPartyProviderModel.builder()
                .name("Test")
                .state("ENABLED")
                .aisEnabled(true)
                .pisEnabled(true)
                .baseUrl("https://sample.tpp/weavr")
                .build();
    }
}
