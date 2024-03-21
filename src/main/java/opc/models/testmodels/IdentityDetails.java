package opc.models.testmodels;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.IdentityType;

@Builder
@Getter
@Setter
public class IdentityDetails {

    private final String email;
    private final String id;
    private final String token;
    private final IdentityType identityType;
    private final String name;
    private final String surname;

    public static IdentityDetails generateDetails(final String email,
                                                  final String id,
                                                  final String token,
                                                  final IdentityType identityType,
                                                  final String name,
                                                  final String surname) {
        return IdentityDetails.builder()
                .email(email)
                .id(id)
                .token(token)
                .identityType(identityType)
                .name(name)
                .surname(surname)
                .build();
    }
}
