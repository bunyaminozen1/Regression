package opc.models.multi.authfactors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthyEnrolmentNotificationModel {


    @JsonProperty("Name")
    private String name;

    @JsonProperty("Email")
    private String email;

    public String getName() {
        return name;
    }

    public AuthyEnrolmentNotificationModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public AuthyEnrolmentNotificationModel setEmail(String email) {
        this.email = email;
        return this;
    }
}
