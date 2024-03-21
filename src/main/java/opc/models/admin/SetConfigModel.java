package opc.models.admin;

public class SetConfigModel {

    private boolean enabled;

    public SetConfigModel(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SetConfigModel setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
