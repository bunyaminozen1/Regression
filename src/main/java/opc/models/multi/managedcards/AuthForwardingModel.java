package opc.models.multi.managedcards;



public class AuthForwardingModel {

    private boolean authForwardingEnabled;
    private String defaultTimeoutDecision;

    public AuthForwardingModel(boolean authForwardingEnabled, String defaultTimeoutDecision) {
        this.authForwardingEnabled = authForwardingEnabled;
        this.defaultTimeoutDecision = defaultTimeoutDecision;
    }

    public boolean isAuthForwardingEnabled() {
        return authForwardingEnabled;
    }

    public String getDefaultTimeoutDecision() {
        return defaultTimeoutDecision;
    }

    public void setAuthForwardingEnabled(boolean authForwardingEnabled) {
        this.authForwardingEnabled = authForwardingEnabled;
    }

    public void setDefaultTimeoutDecision(String defaultTimeoutDecision) {
        this.defaultTimeoutDecision = defaultTimeoutDecision;
    }
}