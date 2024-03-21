package opc.enums.opc;

import java.util.Random;

public enum DeliveryMethod {
    STANDARD_DELIVERY,
    REGISTERED_MAIL,
    COURIER,
    FIRST_CLASS_MAIL;

    public static DeliveryMethod getRandomDeliveryMethod() {
        final Random random = new Random();
        return values()[random.nextInt(values().length-1)];
    }
}