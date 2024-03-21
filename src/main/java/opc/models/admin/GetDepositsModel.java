package opc.models.admin;

public class GetDepositsModel {

    private long accountId;

    public GetDepositsModel(long accountId) {
        this.accountId = accountId;
    }

    public long getAccountId() {
        return accountId;
    }

    public GetDepositsModel setAccountId(long accountId) {
        this.accountId = accountId;
        return this;
    }
}
