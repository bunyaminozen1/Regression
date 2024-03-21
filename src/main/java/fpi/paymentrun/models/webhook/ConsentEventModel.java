package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ConsentEventModel {
    private String expiresAt;
    private Long expiresIn;
    private String status;

}
