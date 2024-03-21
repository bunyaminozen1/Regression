package spi.openbanking.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CreateAccountAuthorisationModel {

    private String institutionId;
    private String callbackUrl;
    private String state;
}
