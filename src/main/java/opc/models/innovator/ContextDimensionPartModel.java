package opc.models.innovator;

import java.util.List;

public class ContextDimensionPartModel {
    private List<String> part;

    public ContextDimensionPartModel(List<String> part) {
        this.part = part;
    }

    public List<String> getPart() {
        return part;
    }

    public ContextDimensionPartModel setPart(List<String> part) {
        this.part = part;
        return this;
    }
}
