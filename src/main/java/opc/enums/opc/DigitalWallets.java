package opc.enums.opc;

import commons.enums.Currency;

import java.util.Random;

public enum DigitalWallets {
    APPLE_PAY,
    GOOGLE_PAY;

    public static DigitalWallets getRandomDigitalWallet() {
        final Random random = new Random();
        return values()[random.nextInt(values().length)];
    }
}
