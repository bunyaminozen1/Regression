package opc.enums.opc;

import java.util.Random;

public enum PermissionType {
    PLATFORM,
    SYSTEM,
    FIN_PLUGIN,
    UNKNOWN;

    public static PermissionType getRandomPermissionType() {
        final Random random = new Random();
        return values()[random.nextInt(values().length-2)];
    }
}
