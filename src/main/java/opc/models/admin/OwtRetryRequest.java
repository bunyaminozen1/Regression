package opc.models.admin;

import opc.enums.opc.RetryType;

public class OwtRetryRequest {

    private String retryType;

    public OwtRetryRequest(final RetryType retryType) {
        this.retryType = retryType.name();
    }

    public String getRetryType() {
        return retryType;
    }

    public OwtRetryRequest setRetryType(String retryType) {
        this.retryType = retryType;
        return this;
    }
}
