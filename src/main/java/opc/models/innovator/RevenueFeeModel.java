package opc.models.innovator;

import java.util.LinkedHashMap;

public class RevenueFeeModel {

    private LinkedHashMap<String, String> instrument;
    private LinkedHashMap<String, String> instrumentOwner;
    private LinkedHashMap<String, String> fee;
    private String feeType;

    public LinkedHashMap<String, String> getInstrument() {
        return instrument;
    }

    public RevenueFeeModel setInstrument(LinkedHashMap<String, String> instrument) {
        this.instrument = instrument;
        return this;
    }

    public LinkedHashMap<String, String> getInstrumentOwner() {
        return instrumentOwner;
    }

    public RevenueFeeModel setInstrumentOwner(LinkedHashMap<String, String> instrumentOwner) {
        this.instrumentOwner = instrumentOwner;
        return this;
    }

    public LinkedHashMap<String, String> getFee() {
        return fee;
    }

    public RevenueFeeModel setFee(LinkedHashMap<String, String> fee) {
        this.fee = fee;
        return this;
    }

    public String getFeeType() {
        return feeType;
    }

    public RevenueFeeModel setFeeType(String feeType) {
        this.feeType = feeType;
        return this;
    }
}