package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CreateAisAuthorisationUrlRequestModel {
    private String institutionId;
    private String redirectUrl;
    private String state;

    public static CreateAisAuthorisationUrlRequestModel.CreateAisAuthorisationUrlRequestModelBuilder defaultCreateAuthorisationUrlRequestModel() {
        return CreateAisAuthorisationUrlRequestModel.builder()
                .institutionId("natwest-sandbox")
                .redirectUrl("https://www.fake.com")
                .state("state");
    }

    public static CreateAisAuthorisationUrlRequestModel.CreateAisAuthorisationUrlRequestModelBuilder defaultCreateAuthorisationUrlRequestModel(final String institutionId) {
        return CreateAisAuthorisationUrlRequestModel.builder()
                .institutionId(institutionId)
                .redirectUrl("https://www.fake.com")
                .state("state");
    }

}
