package opc.models.admin;

import commons.enums.Currency;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.CardBrand;
import opc.enums.opc.CardFundingType;
import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.CardModeThirdPartyRegistry;
import opc.enums.opc.CountryCode;
import opc.enums.opc.DeliveryMethod;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.KybLevel;
import opc.enums.opc.KycLevel;
import opc.enums.opc.OwtType;
import opc.enums.opc.ServiceType;

import java.util.List;

@Builder
@Getter
@Setter
public class CreateServiceTypeModel {

    private ServiceType serviceType;

    private List<IdentityType> identityTypes;
    private Boolean active;
    private KycLevel kycLevels;
    private KybLevel kybLevels;
    private String countryOfResidence;
    private String countryOfIncorporation;
    private String transactionType;
    private Currency currencies;
    private InstrumentType cardType;

    private CardLevelClassification cardLevelClassification;
    private DeliveryMethod deliveryMethod;
    private CountryCode deliveryAddressCountry;
    private Integer maximumBatchSize;
    private Integer minimumBatchSize;
    private CardBrand cardBrand;
    private Currency binCurrency;
    private CardModeThirdPartyRegistry cardMode;
    private InstrumentType cardTypes;
    private List<String> channelProviders;
    private List<Currency> accountCurrencies;
    private List<OwtType> paymentRails;
    private IdentityType identityType;

    private Boolean virtualIban;

    private boolean manualProvisioning;
    private boolean pushProvisioning;

    private List<CardFundingType> cardFundingType;
    private String cardBureauServiceId;
    private String digitalWalletServiceId;
    private Integer transactionAmountLimit;
    private Integer cardVelocity;
    private Integer cardValidity;

    private Integer accountRangeFrom;
    private Integer accountRangeTo;
    private String cardProcessingServiceId;
    private String emiServiceId;
    private String binLicence;
    private Integer bankIdentification;
    private String brandPromoCode;
    private String brandProdName;
    private String coBrand;
    private String binCountry;

    public static CreateServiceTypeModelBuilder defaultCreateServiceTypeModel(final ServiceType serviceType) {

        switch (serviceType) {
            case BIN_SPONSORSHIP:
                return commonCreateServiceTypeModel(serviceType)
                        .accountRangeFrom(1)
                        .accountRangeTo(100)
                        .binCurrency(Currency.EUR)
                        .cardBrand(CardBrand.VISA)
                        .cardMode(CardModeThirdPartyRegistry.DEBIT_MODE)
                        .cardProcessingServiceId("1")
                        .emiServiceId("1")
                        .binLicence("XX")
                        .bankIdentification(1)
                        .brandPromoCode("MDT")
                        .brandProdName("MDT")
                        .coBrand("Weavr")
                        .binCountry("DEU")
                        .identityType(IdentityType.CORPORATE);
            case EMI_LICENSE:
                return commonCreateServiceTypeModel(serviceType)
                        .identityTypes(List.of(IdentityType.CONSUMER, IdentityType.CORPORATE));
            case CARDBUREAU_SERVICE:
                return commonCreateServiceTypeModel(serviceType)
                        .cardTypes(InstrumentType.getRandomInstrumentType())
                        .cardType(InstrumentType.getRandomInstrumentType())
                        .cardLevelClassification(CardLevelClassification.getRandomCardLevelClassification())
                        .deliveryMethod(DeliveryMethod.getRandomDeliveryMethod())
                        .deliveryAddressCountry(CountryCode.getRandomEeaCountry())
                        .maximumBatchSize(10000)
                        .minimumBatchSize(1)
                        .cardBrand(CardBrand.VISA)
                        .binCurrency(Currency.EUR)
                        .identityTypes(List.of(IdentityType.CORPORATE, IdentityType.CONSUMER))
                        .cardMode(CardModeThirdPartyRegistry.DEBIT_MODE)
                        .channelProviders(List.of("channelProviders"));
            case IBAN_SERVICE:
                return commonCreateServiceTypeModel(serviceType)
                        .accountCurrencies(List.of(Currency.EUR, Currency.USD))
                        .channelProviders(List.of("channelProviders"))
                        .paymentRails(List.of(OwtType.SWIFT, OwtType.FASTER_PAYMENTS, OwtType.SEPA))
                        .virtualIban(true)
                        .identityType(IdentityType.CORPORATE);
            case DIGITALWALLET_SERVICE:
                return commonCreateServiceTypeModel(serviceType)
                        .manualProvisioning(true)
                        .pushProvisioning(true);
            case CARDPROCESSING_SERVICE:
                return commonCreateServiceTypeModel(serviceType)
                        .cardFundingType(List.of(CardFundingType.DEBIT,CardFundingType.PREPAID))
                        .transactionAmountLimit(1000)
                        .cardVelocity(100)
                        .cardValidity(12)
                        .cardBureauServiceId("1")
                        .digitalWalletServiceId("1");
            default:
                throw new IllegalArgumentException("Service Type is not supported");
        }
    }

    public static CreateServiceTypeModelBuilder commonCreateServiceTypeModel(final ServiceType serviceType) {
        return CreateServiceTypeModel
                .builder()
                .serviceType(serviceType)
                .active(true);
    }

    public static CreateServiceTypeModelBuilder commonCreateServiceTypeWithoutRequiredModel(final ServiceType serviceType) {
        return CreateServiceTypeModel
                .builder()
                .serviceType(serviceType);
    }

}
