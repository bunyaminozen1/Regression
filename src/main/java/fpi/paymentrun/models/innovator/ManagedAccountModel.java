package fpi.paymentrun.models.innovator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class ManagedAccountModel {

    private List<String> currency;
}
