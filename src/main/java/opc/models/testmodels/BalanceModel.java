package opc.models.testmodels;

public class BalanceModel {
    private int availableBalance;
    private int actualBalance;

    public BalanceModel(final int availableBalance, final int actualBalance) {
        this.availableBalance = availableBalance;
        this.actualBalance = actualBalance;
    }

    public int getAvailableBalance() {
        return availableBalance;
    }

    public BalanceModel setAvailableBalance(int availableBalance) {
        this.availableBalance = availableBalance;
        return this;
    }

    public int getActualBalance() {
        return actualBalance;
    }

    public BalanceModel setActualBalance(int actualBalance) {
        this.actualBalance = actualBalance;
        return this;
    }
}
