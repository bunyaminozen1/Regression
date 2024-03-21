package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CreatePisAuthorisationUrlRequestModel {
    private String reference;
    private String redirectUrl;
    private String state;

    public static CreatePisAuthorisationUrlRequestModel.CreatePisAuthorisationUrlRequestModelBuilder  defaultCreatePisAuthorisationUrlRequestModel(final String paymentGroupReference){
        return CreatePisAuthorisationUrlRequestModel.builder()
                .reference(paymentGroupReference)
                .redirectUrl("https://www.fake.com")
                .state("state");
    }
}
