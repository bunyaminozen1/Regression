package opc.models.innovator;

import commons.enums.Jurisdiction;
import lombok.Builder;
import lombok.Getter;
import opc.enums.opc.CardBureau;
import opc.enums.opc.IdentityType;
import opc.models.multi.managedcards.AuthForwardingModel;
import opc.models.shared.CurrencyAmount;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class AbstractCreateManagedCardsProfileModel {
    private final String code;
    private final String payletTypeCode;
    private final List<String> tag;
    private final List<FeeDetailsModel> purchaseFee;
    private final List<FeeDetailsModel> refundFee;
    private final List<FeeDetailsModel> chargebackFee;
    private final List<FeeDetailsModel> atmWithdrawalFee;
    private final List<FeeDetailsModel> cardBalanceInquiryFee;
    private final String cardType;
    private final String cardBrand;
    private final int expiryPeriodMonths;
    private final String defaultRenewalType;
    private final List<String> supportedRenewalTypes;
    private final boolean singleSpend;
    private final String forexFeePercentage;
    private final String cardBureau;
    private final String unassignedCardPoolEnabled;
    private final String cardFundingType;
    private final DigitalWalletsEnabledModel digitalWalletsEnabled;
    private final AuthForwardingModel authForwarding;

    public static AbstractCreateManagedCardsProfileModelBuilder DefaultCorporatePrepaidCreateManagedCardsProfileModel() {
        return DefaultAbstractCreateManagedCardsProfileModel(IdentityType.CORPORATE, "PREPAID", CardBureau.NITECREST);
    }

    public static AbstractCreateManagedCardsProfileModelBuilder DefaultCorporateDebitCreateManagedCardsProfileModel() {
        return DefaultAbstractCreateManagedCardsProfileModel(IdentityType.CORPORATE, "DEBIT", CardBureau.NITECREST);
    }

    public static AbstractCreateManagedCardsProfileModelBuilder DefaultConsumerPrepaidCreateManagedCardsProfileModel() {
        return DefaultAbstractCreateManagedCardsProfileModel(IdentityType.CONSUMER, "PREPAID", CardBureau.NITECREST);
    }

    public static AbstractCreateManagedCardsProfileModelBuilder DefaultConsumerDebitCreateManagedCardsProfileModel() {
        return DefaultAbstractCreateManagedCardsProfileModel(IdentityType.CONSUMER, "DEBIT", CardBureau.NITECREST);
    }

    public static AbstractCreateManagedCardsProfileModelBuilder DefaultAbstractCreateManagedCardsProfileModel(final IdentityType identityType,
                                                                        final String cardFundingType,
                                                                        final CardBureau cardBureau){
        final String code = String.format("%s_managed_cards", identityType.name().toLowerCase());

        return new AbstractCreateManagedCardsProfileModelBuilder()
                .code(code)
                .payletTypeCode(code)
                .cardType("VIRTUAL")
                .cardBrand("MASTERCARD")
                .expiryPeriodMonths(36)
                .defaultRenewalType("NO_RENEW")
                .supportedRenewalTypes(List.of("NO_RENEW", "RENEW"))
                .singleSpend(false)
                .forexFeePercentage("ONE_PERCENT")
                .cardBureau(cardBureau.name())
                .purchaseFee(Collections.singletonList(new FeeDetailsModel()
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 12L),
                                        new CurrencyAmount("GBP", 10L),
                                        new CurrencyAmount("USD", 14L))))))
                .refundFee(Collections.singletonList(new FeeDetailsModel()
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 11L),
                                        new CurrencyAmount("GBP", 9L),
                                        new CurrencyAmount("USD", 13L))))))
                .unassignedCardPoolEnabled("TRUE")
                .cardFundingType(cardFundingType)
                .digitalWalletsEnabled(new DigitalWalletsEnabledModel(false, false));
    }

    public static AbstractCreateManagedCardsProfileModelBuilder DefaultAbstractCreateManagedCardsProfileModel(final IdentityType identityType,
                                                                        final String cardFundingType,
                                                                        final CardBureau cardBureau,
                                                                        final Jurisdiction jurisdiction){
        final String code = String.format("%s_managed_cards_%s", identityType.name().toLowerCase(), jurisdiction.name().toLowerCase());
        final String payletTypeCode = String.format("%s_managed_cards", identityType.name().toLowerCase());

        return new AbstractCreateManagedCardsProfileModelBuilder()
                .code(code)
                .payletTypeCode(payletTypeCode)
                .cardType("VIRTUAL")
                .cardBrand("MASTERCARD")
                .expiryPeriodMonths(36)
                .defaultRenewalType("NO_RENEW")
                .supportedRenewalTypes(List.of("NO_RENEW", "RENEW"))
                .singleSpend(false)
                .forexFeePercentage("ONE_PERCENT")
                .cardBureau(cardBureau.name())
                .purchaseFee(Collections.singletonList(new FeeDetailsModel()
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 12L),
                                        new CurrencyAmount("GBP", 10L),
                                        new CurrencyAmount("USD", 14L))))))
                .refundFee(Collections.singletonList(new FeeDetailsModel()
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 11L),
                                        new CurrencyAmount("GBP", 9L),
                                        new CurrencyAmount("USD", 13L))))))
                .unassignedCardPoolEnabled("TRUE")
                .cardFundingType(cardFundingType)
                .digitalWalletsEnabled(new DigitalWalletsEnabledModel(false, false));
    }
}
