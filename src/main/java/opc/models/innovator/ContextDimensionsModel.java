package opc.models.innovator;

import java.util.List;

public class ContextDimensionsModel {
    private List<ContextDimensionKeyValueModel> dimension;

    public ContextDimensionsModel(List<ContextDimensionKeyValueModel> dimension) {
        this.dimension = dimension;
    }

    public List<ContextDimensionKeyValueModel> getDimension() {
        return dimension;
    }

    public ContextDimensionsModel setDimension(List<ContextDimensionKeyValueModel> dimension) {
        this.dimension = dimension;
        return this;
    }
}
