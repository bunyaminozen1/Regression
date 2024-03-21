package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class SetScaModel {

    private final boolean scaMaEnabled;
    private final boolean scaMcEnabled;
    private final boolean scaEnrolEnabled;
    private final boolean scaAuthUserEnabled;
}
