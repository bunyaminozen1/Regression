package opc.enums.opc;

import java.util.Random;

public enum ServiceTypeCategory {
    IDENTITIES,
    INSTRUMENTS,
    TRANSACTIONS,
    UNKNOWN;

    public static ServiceTypeCategory getRandomServiceTypeCategory() {
        final Random random = new Random();
        return values()[random.nextInt(values().length-1)];
    }

}