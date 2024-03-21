package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum TransactionType {
     INSTRUMENT_CREATE,
     INSTRUMENT_BLOCK,
     INSTRUMENT_UNBLOCK,
     INSTRUMENT_DELETE,
     AUTHORISATION,
     SETTLEMENT,
     MANUAL_TRANSACTION,
     TRANSFER,
     SEND,
     DEPOSIT,
     AUTHORISATION_REVERSAL,
     AUTHORISATION_EXPIRY,
     AUTHORISATION_DECLINE,
     AUTHORISATION_MANUAL_CLOSE,
     MERCHANT_REFUND,
     MERCHANT_REFUND_REVERSAL,
     ORIGINAL_CREDIT_TRANSACTION,
     SETTLEMENT_REVERSAL,
     ADJUSTMENT,
     CHARGE_FEE,
     WITHDRAWAL_RESERVE,
     WITHDRAWAL_RELEASE,
     FEE_REVERSAL,
     CARD_UPGRADE_TO_PHYSICAL,
     ACTIVATE_PHYSICAL_CARD,
     INSTRUMENT_REPLACE,
     OUTGOING_WIRE_TRANSFER,
     AUTHORISATION_CANCELLATION,
     SYSTEM_TRANSACTION,
     OUTGOING_DIRECT_DEBIT_COLLECTION,
     OUTGOING_DIRECT_DEBIT_REFUND;

    public static TransactionType getRandomTransactionType() {
        final Random random = new Random();
        return values()[random.nextInt(values().length)];
    }

    public static TransactionType getRandomWithExcludedTransactionType(final TransactionType transactionType) {

        final List<TransactionType> enums =
                Arrays.stream(values()).filter(x -> !x.equals(transactionType)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }

    public static TransactionType getRandomWithExcludedTransactionTypes(final List<TransactionType> transactionTypes) {

        final List<TransactionType> enums =
                Arrays.stream(values()).filter(x -> !transactionTypes.contains(x)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
