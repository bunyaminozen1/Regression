package fpi.paymentrun.models.innovator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class OwtModel {

    private List<String> supportedType;
}
