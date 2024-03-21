package opc.enums.opc;

import java.util.Random;

public enum CardFundingType {
    PREPAID,
    DEBIT,
    UNKNOWN;

    public static CardFundingType getRandomCardFundingType() {
        final Random random = new Random();
        return values()[random.nextInt(values().length-1)];
    }
}
