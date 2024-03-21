package opc.models.shared;

import commons.models.MobileNumberModel;

public class EnrolUserModel {

    private String factorType;
    private MobileNumberModel channelData;

    public EnrolUserModel(final String factorType) {
        this.factorType = factorType;
    }

    public EnrolUserModel(final String factorType, final MobileNumberModel mobileNumberModel) {
        this.factorType = factorType;
        this.channelData = mobileNumberModel;
    }

    public String getFactorType() {
        return factorType;
    }

    public EnrolUserModel setFactorType(final String factorType) {
        this.factorType = factorType;
        return this;
    }

    public MobileNumberModel getChannelData() {
        return channelData;
    }

    public EnrolUserModel setChannelData(final MobileNumberModel channelData) {
        this.channelData = channelData;
        return this;
    }
}
