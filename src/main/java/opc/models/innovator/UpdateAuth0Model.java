package opc.models.innovator;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import commons.config.ConfigHelper;

@Data
@Builder
@Getter
@Setter
public class UpdateAuth0Model {

    private String issuer;
    private String clientId;
    private String clientSecret;
    private String callbackUrl;

    public static UpdateAuth0Model defaultUpdateAuth0Model() {

        final String baseUrl = ConfigHelper.getEnvironmentConfiguration().getBaseUrl();

        return UpdateAuth0Model.builder()
                .issuer("https://weavr.eu.auth0.com")
                .clientId("g5xWyHI29mxwLB3lWFs5d0sr6SZdttOH")
                .clientSecret("gpSkHkAyqSZwNQc-_Eg51p_PbFguJsDYWe3F5r6icRLCYZDt08iT70DiqwLN-uyL")
                .callbackUrl(String.format("%s/iam/callback", baseUrl))
                .build();
    }
}
