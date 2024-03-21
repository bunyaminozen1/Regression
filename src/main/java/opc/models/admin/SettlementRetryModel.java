package opc.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.RetryType;

@Builder
@Getter
@Setter
public class SettlementRetryModel {
    private String note;
    private String relatedSettlementId;
    private RetryType retryType;
    private CurrencyAmountLimitModel transactionAmount;
    private CurrencyAmountLimitModel cardAmount;

    public static SettlementRetryModel SettlementRetryTransactionModel (final String note,
                                                                        final RetryType retryType,
                                                                        final String value,
                                                                        final String currency) {
        return CommonSettlementRetryModel(note, retryType)
                .transactionAmount(new CurrencyAmountLimitModel(currency, value))
                .build();
    }

    public static SettlementRetryModel SettlementRetryCardModel (final String note,
                                                                 final RetryType retryType,
                                                                 final String value,
                                                                 final String currency) {
        return CommonSettlementRetryModel(note, retryType)
                .cardAmount(new CurrencyAmountLimitModel(currency, value))
                .build();
    }

    public static SettlementRetryModelBuilder CommonSettlementRetryModel(final String note,
                                                                         final RetryType retryType) {
        return SettlementRetryModel
                .builder()
                .note(note)
                .retryType(retryType);
    }

    public static SettlementRetryModel DefaultSettlementRetryModel(final RetryType retryType, final String note) {
        return SettlementRetryModel
                .builder()
                .retryType(retryType)
                .note(note)
                .build();
    }
}
