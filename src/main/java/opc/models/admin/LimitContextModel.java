package opc.models.admin;

import opc.enums.opc.KycLevel;

import java.util.List;

public class LimitContextModel {

    private List<LimitDimensionModel> dimension;

    public LimitContextModel() {
        this.dimension = LimitDimensionModel.defaultLimitDimensionModel();
    }

    public LimitContextModel(final KycLevel kycLevel) {
        this.dimension = LimitDimensionModel.defaultLimitDimensionModel(kycLevel);
    }

    public LimitContextModel(final String userId, final KycLevel kycLevel) {
        this.dimension = LimitDimensionModel.userSpecificLimitDimensionModel(userId, kycLevel);
    }

    public LimitContextModel(List<LimitDimensionModel> dimension) {
        this.dimension = dimension;
    }

    public List<LimitDimensionModel> getDimension() {
        return dimension;
    }

    public LimitContextModel setDimension(List<LimitDimensionModel> dimension) {
        this.dimension = dimension;
        return this;
    }
}
