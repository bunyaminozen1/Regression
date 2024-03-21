package opc.enums.opc;

public enum TestMerchant {
    EUR("1111", "old_simulator_eur", "1111"),
    GBP("2222", "old_simulator_gbp", "2222"),
    USD("3333", "old_simulator_usd", "3333");

    private final String merchantId;
    private final String merchantName;
    private final String merchantCategoryCode;

    TestMerchant(final String merchantId, final String merchantName, final String merchantCategoryCode){
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantCategoryCode = merchantCategoryCode;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getMerchantCategoryCode() {
        return merchantCategoryCode;
    }
}