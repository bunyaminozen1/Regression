package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsentResponseModel {
    private String expiresAt;
    private String status;
    private Long expiresIn;
}
