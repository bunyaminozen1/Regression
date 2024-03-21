package opc.models.admin;

import org.apache.commons.lang3.RandomStringUtils;

public class InviteUserModel {
    private String friendlyName;
    private String email;
    private String role;
    private Long roleId;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public InviteUserModel(String friendlyName, String email, String role) {
        this.friendlyName = friendlyName;
        this.email = email;
        this.role = role;
    }

    public InviteUserModel(final Builder builder) {
        this.friendlyName = builder.friendlyName;
        this.email = builder.email;
        this.role = builder.role;
        this.roleId = builder.roleId;
    }

    public static class Builder {
        private String friendlyName;
        private String email;
        private String role;
        private Long roleId;

        public Builder setRoleId(Long roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setRole(String role) {
            this.role = role;
            return this;
        }

        public InviteUserModel build() {
            return new InviteUserModel(this);
        }
    }

    public static Builder DefaultCreateInviteUserModel() {
        final InviteUserModel.Builder builder = new InviteUserModel.Builder();
        builder.setEmail(RandomStringUtils.randomAlphabetic(7) + "@weavr.io");
        builder.setRole("ADMIN_SUPER");
        builder.setFriendlyName(RandomStringUtils.randomAlphabetic(5));
        builder.setRoleId(null);
        return builder;
    }


}
