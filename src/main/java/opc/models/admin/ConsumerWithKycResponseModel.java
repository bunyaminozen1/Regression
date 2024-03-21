package opc.models.admin;

public class ConsumerWithKycResponseModel {

    private ConsumerResponseModel consumer;
    private ConsumerKycResponseModel kyc;

    public ConsumerResponseModel getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerResponseModel consumer) {
        this.consumer = consumer;
    }

    public ConsumerKycResponseModel getKyc() {
        return kyc;
    }

    public void setKyc(ConsumerKycResponseModel kyc) {
        this.kyc = kyc;
    }
}
