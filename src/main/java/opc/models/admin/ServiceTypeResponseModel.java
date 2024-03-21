package opc.models.admin;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class ServiceTypeResponseModel {

    private String serviceType;
    private List<String> accountCurrencies;
    private Boolean active;
    private String category;
    private List <String> identityTypes;
    private String identityType;
    private List <String> paymentRails;
    private int transactionAmountLimits;
    private List<String> countryOfIncorporation;
    private List<String> countryOfResidence;
    private List<String> currencies;
    private List<String> kybLevels;
    private List<String> kycLevels;
    private List<String> transactionType;
    private String profileType;
    private Long serviceId;
    private String cardLevelClassification;
    private String cardType;
    private String deliveryAddressCountry;
    private String deliveryMethod;
    private String maximumBatchSize;
    private String minimumBatchSize;
    private String cardBrand;
    private String binCurrency;
    private String cardMode;
    private List<String> cardTypes;
    private List<String> channelProviders;
    private List<String> digitalWallets;
    private boolean virtualIban;
    private boolean manualProvisioning;
    private boolean pushProvisioning;
    private List<String> cardFundingType;
    private String cardBureauServiceId;
    private String digitalWalletServiceId;
    private int transactionAmountLimit;
    private int cardVelocity;
    private int cardValidity;
    private int accountRangeFrom;
    private int accountRangeTo;
    private String cardProcessingServiceId;
    private String emiServiceId;
    private String binLicence;
    private int bankIdentification;
    private String brandPromoCode;
    private String brandProdName;
    private String coBrand;
    private String binCountry;
}
