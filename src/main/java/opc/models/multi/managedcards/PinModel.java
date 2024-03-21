package opc.models.multi.managedcards;

public class PinModel {
    private final PinValueModel pin;

    public PinModel(PinValueModel pin) {
        this.pin = pin;
    }

    public PinValueModel getPin() {
        return pin;
    }
}
