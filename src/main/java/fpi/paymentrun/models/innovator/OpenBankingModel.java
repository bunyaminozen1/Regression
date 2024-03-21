package fpi.paymentrun.models.innovator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class OpenBankingModel {

    private String redirectUrl;
}
