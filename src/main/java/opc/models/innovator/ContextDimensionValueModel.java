package opc.models.innovator;

import java.util.List;

public class ContextDimensionValueModel {
    private List<ContextDimensionPartModel> value;

    public ContextDimensionValueModel(List<ContextDimensionPartModel> value) {
        this.value = value;
    }

    public List<ContextDimensionPartModel> getValue() {
        return value;
    }

    public ContextDimensionValueModel setValue(List<ContextDimensionPartModel> value) {
        this.value = value;
        return this;
    }
}
