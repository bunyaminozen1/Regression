package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ReAuthoriseAisConsentModel {
    private String redirectUrl;
    private String state;

    public static ReAuthoriseAisConsentModel.ReAuthoriseAisConsentModelBuilder defaultReAuthoriseAisConsentModel(){
        return ReAuthoriseAisConsentModel.builder()
                .redirectUrl("https://www.fake.com")
                .state("state");
    }
}
