package opc.enums.opc;

public enum CardBureau {

    NITECREST("NTCRST_PR_01", "NTCRST_CT_01"),
    DIGISEQ("DGSQ_PR_01", "DGSQ_CT_01");

    private final String productReference;
    private final String carrierType;

    CardBureau(final String productReference, final String carrierType){
        this.productReference = productReference;
        this.carrierType = carrierType;
    }

    public String getProductReference(){
        return productReference;
    }

    public String getCarrierType(){
        return carrierType;
    }
}
