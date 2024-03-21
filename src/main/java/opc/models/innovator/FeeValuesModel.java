package opc.models.innovator;

import opc.models.shared.CurrencyAmount;

import java.util.List;

public class FeeValuesModel {

    private String type;
    private List<CurrencyAmount> flatAmount;
    private PercentageModel percentage;

    public String getType() {
        return type;
    }

    public FeeValuesModel setType(String type) {
        this.type = type;
        return this;
    }

    public List<CurrencyAmount> getFlatAmount() {
        return flatAmount;
    }

    public FeeValuesModel setFlatAmount(List<CurrencyAmount> flatAmount) {
        this.flatAmount = flatAmount;
        return this;
    }

    public PercentageModel getPercentage() {
        return percentage;
    }

    public FeeValuesModel setPercentage(PercentageModel percentage) {
        this.percentage = percentage;
        return this;
    }
}
