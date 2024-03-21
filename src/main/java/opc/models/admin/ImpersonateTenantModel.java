package opc.models.admin;

public class ImpersonateTenantModel {
    private String tenantId;

    public ImpersonateTenantModel(final String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public ImpersonateTenantModel setTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }
}
