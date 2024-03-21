package opc.models.admin;


public class SubscriptionStatusPayneticsModel {

    private EntitySubscriptionStatusPayneticsModel entity;
    private String kyiLogCase;
    private String message;
    private String status;

    public EntitySubscriptionStatusPayneticsModel getEntity() {
        return entity;
    }

    public void setEntity(EntitySubscriptionStatusPayneticsModel entity) {
        this.entity = entity;
    }

    public String getKyiLogCase() {
        return kyiLogCase;
    }

    public void setKyiLogCase(String kyiLogCase) {
        this.kyiLogCase = kyiLogCase;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
