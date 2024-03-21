package opc.enums.opc;

import java.util.Random;

public enum ServiceType {
    EMI_LICENSE,
    BIN_SPONSORSHIP,
    CARDBUREAU_SERVICE,
    IBAN_SERVICE,
    DIGITALWALLET_SERVICE,
    CARDPROCESSING_SERVICE,
    UNKNOWN;

    public static ServiceType getRandomServiceType() {
        final Random random = new Random();
        return values()[random.nextInt(values().length-1)];
    }
}


