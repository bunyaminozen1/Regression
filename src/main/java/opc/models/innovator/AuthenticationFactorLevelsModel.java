package opc.models.innovator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticationFactorLevelsModel {

    @JsonProperty("1")
    private AuthenticationFactorsModel credentialConfiguration;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("2")
    private AuthenticationFactorsModel credentialConfiguration2;

    public AuthenticationFactorLevelsModel(final AuthenticationFactorsModel credentialConfiguration) {
        this.credentialConfiguration = credentialConfiguration;
    }

    public AuthenticationFactorLevelsModel(final AuthenticationFactorsModel credentialConfiguration, final AuthenticationFactorsModel credentialConfiguration2) {
        this.credentialConfiguration = credentialConfiguration;
        this.credentialConfiguration2 = credentialConfiguration2;
    }

    public AuthenticationFactorsModel getCredentialConfiguration() {
        return credentialConfiguration;
    }

    public void setCredentialConfiguration(AuthenticationFactorsModel credentialConfiguration) {
        this.credentialConfiguration = credentialConfiguration;
    }

    public AuthenticationFactorsModel getCredentialConfiguration2() {
        return credentialConfiguration2;
    }

    public void setCredentialConfiguration2(AuthenticationFactorsModel credentialConfiguration2) {
        this.credentialConfiguration2 = credentialConfiguration2;
    }
}
