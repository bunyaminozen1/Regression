package opc.models.simulator;


public class AdditionalMerchantDataModel {
    private final String merchantStreet;
    private final String merchantCity;
    private final String merchantState;
    private final String merchantPostalCode;
    private final String merchantCountry;
    private final String merchantTelephone;
    private final String merchantURL;
    private final String merchantNameOther;
    private final String merchantNetworkId;
    private final String merchantContact;

    public AdditionalMerchantDataModel(final AdditionalMerchantDataModel.Builder builder) {
        this.merchantStreet = builder.merchantStreet;
        this.merchantCity = builder.merchantCity;
        this.merchantState = builder.merchantState;
        this.merchantPostalCode = builder.merchantPostalCode;
        this.merchantCountry = builder.merchantCountry;
        this.merchantTelephone = builder.merchantTelephone;
        this.merchantURL = builder.merchantURL;
        this.merchantNameOther = builder.merchantNameOther;
        this.merchantNetworkId = builder.merchantNetworkId;
        this.merchantContact = builder.merchantContact;
    }

    public String getMerchantCity() {
        return merchantCity;
    }

    public String getMerchantState() {
        return merchantState;
    }

    public String getMerchantStreet() {
        return merchantStreet;
    }

    public String getMerchantPostalCode() {
        return merchantPostalCode;
    }

    public String getMerchantCountry() {
        return merchantCountry;
    }

    public String getMerchantTelephone() {
        return merchantTelephone;
    }

    public String getMerchantURL() {
        return merchantURL;
    }

    public String getMerchantNameOther() {
        return merchantNameOther;
    }

    public String getMerchantNetworkId() {
        return merchantNetworkId;
    }

    public String getMerchantContact() {
        return merchantContact;
    }

    public static class Builder {
        private String merchantStreet;
        private String merchantCity;
        private String merchantState;
        private String merchantPostalCode;
        private String merchantCountry;
        private String merchantTelephone;
        private String merchantURL;
        private String merchantNameOther;
        private String merchantNetworkId;
        private String merchantContact;

        public AdditionalMerchantDataModel.Builder setMerchantStreet(String merchantStreet) {
            this.merchantStreet = merchantStreet;
            return this;
        }

        public AdditionalMerchantDataModel.Builder setMerchantCity(String merchantCity) {
            this.merchantCity = merchantCity;
            return this;
        }

        public AdditionalMerchantDataModel.Builder setMerchantState(String merchantState) {
            this.merchantState = merchantState;
            return this;
        }

        public AdditionalMerchantDataModel.Builder setMerchantCountry(String merchantCountry) {
            this.merchantCountry = merchantCountry;
            return this;
        }

        public AdditionalMerchantDataModel.Builder setMerchantTelephone(String merchantTelephone) {
            this.merchantTelephone = merchantTelephone;
            return this;
        }

        public AdditionalMerchantDataModel.Builder setMerchantURL(String merchantURL) {
            this.merchantURL = merchantURL;
            return this;
        }

        public AdditionalMerchantDataModel.Builder setMerchantPostalCode(String merchantPostalCode) {
            this.merchantPostalCode = merchantPostalCode;
            return this;
        }

        public AdditionalMerchantDataModel.Builder setMerchantNameOther(String merchantNameOther) {
            this.merchantNameOther = merchantNameOther;
            return this;
        }

        public AdditionalMerchantDataModel.Builder setMerchantNetworkId(String merchantNetworkId) {
            this.merchantNetworkId = merchantNetworkId;
            return this;
        }

        public AdditionalMerchantDataModel.Builder setMerchantContact(String merchantContact) {
            this.merchantContact = merchantContact;
            return this;
        }

        public AdditionalMerchantDataModel build() { return new AdditionalMerchantDataModel(this); }
    }

    public static AdditionalMerchantDataModel.Builder builder(){
        return new AdditionalMerchantDataModel.Builder();
    }

    public static Builder DefaultAdditionalDataModel() {
        return new AdditionalMerchantDataModel.Builder()
                .setMerchantCity("Merchant City")
                .setMerchantContact("Merchant Contact")
                .setMerchantCountry("MLT")
                .setMerchantNameOther("Merchant Name Other")
                .setMerchantNetworkId("Merchant Network Id")
                .setMerchantPostalCode("Merchant Postal Code")
                .setMerchantState("MT")
                .setMerchantStreet("Merchant Street")
                .setMerchantTelephone("+35621565369")
                .setMerchantURL("https://amazon.com");
    }
}