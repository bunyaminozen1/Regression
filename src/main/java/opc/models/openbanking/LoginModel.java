package opc.models.openbanking;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class LoginModel {

    private final String email;
    private final PasswordModel password;

    public static LoginModel defaultLoginModel(final String email) {
        return LoginModel.builder()
                .email(email)
                .password(PasswordModel.builder().value("Pass1234").build())
                .build();
    }
}
