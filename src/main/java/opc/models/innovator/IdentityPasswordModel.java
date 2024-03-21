package opc.models.innovator;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IdentityPasswordModel {

    @JsonProperty("ROOT")
    private PasswordConfigModel root;
    @JsonProperty("USER")
    private PasswordConfigModel user;

    public PasswordConfigModel getRoot() {
        return root;
    }

    public IdentityPasswordModel setRoot(PasswordConfigModel root) {
        this.root = root;
        return this;
    }

    public PasswordConfigModel getUser() {
        return user;
    }

    public IdentityPasswordModel setUser(PasswordConfigModel user) {
        this.user = user;
        return this;
    }
}
