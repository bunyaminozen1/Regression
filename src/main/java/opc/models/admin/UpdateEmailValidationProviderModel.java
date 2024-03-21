package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateEmailValidationProviderModel {

    private String emailValidationProvider;

    public UpdateEmailValidationProviderModel(final String emailValidationProvider) {
        this.emailValidationProvider = emailValidationProvider;
    }
}
