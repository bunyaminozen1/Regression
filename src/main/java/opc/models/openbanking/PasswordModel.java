package opc.models.openbanking;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class PasswordModel {

    private final String value;
}
