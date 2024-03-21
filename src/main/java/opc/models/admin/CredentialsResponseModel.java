package opc.models.admin;

import java.util.List;

public class CredentialsResponseModel {

    private List<CredentialResponseModel> credential;

    public List<CredentialResponseModel> getCredential() {
        return credential;
    }

    public void setCredential(List<CredentialResponseModel> credential) {
        this.credential = credential;
    }
}
