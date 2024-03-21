package opc.models.admin;

import opc.models.shared.TypeIdResponseModel;

import java.util.List;

public class CredentialResponseModel {
    private String id;
    private String code;
    private String type;
    private boolean active;
    private TypeIdResponseModel identity;
    private List<FactorsResponseModel> factors;
    private List<String> action;
    private List<LinkedUsersResponseModel> linkedUsers;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public TypeIdResponseModel getIdentity() {
        return identity;
    }

    public void setIdentity(TypeIdResponseModel identity) {
        this.identity = identity;
    }

    public List<FactorsResponseModel> getFactors() {
        return factors;
    }

    public void setFactors(List<FactorsResponseModel> factors) {
        this.factors = factors;
    }

    public List<String> getAction() {
        return action;
    }

    public void setAction(List<String> action) {
        this.action = action;
    }

    public List<LinkedUsersResponseModel> getLinkedUsers() {
        return linkedUsers;
    }

    public void setLinkedUsers(List<LinkedUsersResponseModel> linkedUsers) {
        this.linkedUsers = linkedUsers;
    }
}
