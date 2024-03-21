package opc.models.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import opc.models.backoffice.IdentityModel;

public class LoginModel {

    private String email;
    private PasswordModel password;

    public LoginModel(final String email, final PasswordModel password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public LoginModel setEmail(String email) {
        this.email = email;
        return this;
    }

    public PasswordModel getPassword() {
        return password;
    }

    public LoginModel setPassword(PasswordModel password) {
        this.password = password;
        return this;
    }

    @SneakyThrows
    public static String loginString(final String email, final String password) {
        return new ObjectMapper().writeValueAsString(new LoginModel(email, new PasswordModel(password)));
    }
}