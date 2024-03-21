package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookOwtDestinationModel {

    private String beneficiaryAddress;
    private String beneficiaryBankAddress;
    private String beneficiaryBankCountry;
    private String beneficiaryBankName;
    private String beneficiaryName;
    private LinkedHashMap<String, String> sepa;
    private LinkedHashMap<String, String> fasterPayments;

    public String getBeneficiaryAddress() {
        return beneficiaryAddress;
    }

    public WebhookOwtDestinationModel setBeneficiaryAddress(String beneficiaryAddress) {
        this.beneficiaryAddress = beneficiaryAddress;
        return this;
    }

    public String getBeneficiaryBankAddress() {
        return beneficiaryBankAddress;
    }

    public WebhookOwtDestinationModel setBeneficiaryBankAddress(String beneficiaryBankAddress) {
        this.beneficiaryBankAddress = beneficiaryBankAddress;
        return this;
    }

    public String getBeneficiaryBankCountry() {
        return beneficiaryBankCountry;
    }

    public WebhookOwtDestinationModel setBeneficiaryBankCountry(String beneficiaryBankCountry) {
        this.beneficiaryBankCountry = beneficiaryBankCountry;
        return this;
    }

    public String getBeneficiaryBankName() {
        return beneficiaryBankName;
    }

    public WebhookOwtDestinationModel setBeneficiaryBankName(String beneficiaryBankName) {
        this.beneficiaryBankName = beneficiaryBankName;
        return this;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public WebhookOwtDestinationModel setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
        return this;
    }

    public LinkedHashMap<String, String> getSepa() {
        return sepa;
    }

    public WebhookOwtDestinationModel setSepa(LinkedHashMap<String, String> sepa) {
        this.sepa = sepa;
        return this;
    }

    public LinkedHashMap<String, String> getFasterPayments() {
        return fasterPayments;
    }

    public WebhookOwtDestinationModel setFasterPayments(LinkedHashMap<String, String> fasterPayments) {
        this.fasterPayments = fasterPayments;
        return this;
    }
}
