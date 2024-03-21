package opc.enums.opc;

import java.util.Random;

public enum CardLevelClassification {
    CONSUMER,
    CORPORATE;

    public static CardLevelClassification getRandomCardLevelClassification() {
        final Random random = new Random();
        return values()[random.nextInt(values().length-1)];
    }
}
