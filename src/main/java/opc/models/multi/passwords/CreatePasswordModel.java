package opc.models.multi.passwords;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import opc.models.shared.PasswordModel;

public class CreatePasswordModel {
    private final PasswordModel password;

    public CreatePasswordModel(final Builder builder) {
        this.password = builder.password;
    }

    public PasswordModel getPassword() {
        return password;
    }

    public static class Builder {
        private PasswordModel password;

        public Builder setPassword(PasswordModel password) {
            this.password = password;
            return this;
        }

        public CreatePasswordModel build(){ return new CreatePasswordModel(this);}
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @SneakyThrows
    public static String defaultCreatePasswordStringModel() {
        return new ObjectMapper().writeValueAsString(newBuilder()
                .setPassword(new PasswordModel("Pass1234"))
                .build());
    }
}