package opc.enums.opc;

import java.util.Random;

public enum InstrumentType {
    PHYSICAL,
    VIRTUAL;

    public static InstrumentType getRandomInstrumentType() {
        final Random random = new Random();
        return values()[random.nextInt(values().length-1)];
    }
}
