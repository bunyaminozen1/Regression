package fpi.paymentrun.enums;

import java.util.Random;

public enum AuthorisedUserRole {

    CREATOR,
    CONTROLLER;

    public static AuthorisedUserRole getRandomRole() {
        final Random random = new Random();
        return values()[random.nextInt(values().length)];
    }
}
