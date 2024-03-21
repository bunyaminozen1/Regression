package opc.models.shared;

public class ManagedInstrumentBalance {
    private long availableBalance;
    private long actualBalance;

    public ManagedInstrumentBalance() {
    }

    public ManagedInstrumentBalance(final long availableBalance, final long actualBalance) {
        this.availableBalance = availableBalance;
        this.actualBalance = actualBalance;
    }

    public long getAvailableBalance() {
        return availableBalance;
    }

    public long getActualBalance() {
        return actualBalance;
    }
}
