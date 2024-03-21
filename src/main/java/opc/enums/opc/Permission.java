package opc.enums.opc;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public enum Permission {
    ADMIN_INVITE_CREATE("admin-invite:create", "SYSTEM"),
    ROLE_GET("role:get", "FIN_PLUGIN"),
    ROLE_READ("role:read", "SYSTEM"),
    ROLE_ASSIGN("role:assign", "FIN_PLUGIN"),
    ROLE_UNASSIGN("role:unassign", "FIN_PLUGIN"),
    ROLE_REQUEST_CREATE("role:requestCreate", "SYSTEM"),
    ROLE_REVIEW_CREATE("role:reviewCreate", "SYSTEM"),
    ROLE_REQUEST_UPDATE("role:requestUpdate", "SYSTEM"),
    ROLE_REVIEW_UPDATE("role:reviewUpdate", "SYSTEM"),
    ROLE_DELETE("role:delete", "SYSTEM"),
    DEPLOYMENT_CREATE("deployment:create", "SYSTEM"),
    DEPLOYMENT_READ("deployment:read", "SYSTEM"),
    DEPLOYMENT_EDIT("deployment:update", "SYSTEM"),
    DEPLOYMENT_DELETE("deployment:delete", "SYSTEM");

    private final String permission;
    private final String permissionType;

    Permission(String permission, String permissionType) {
        this.permission = permission;
        this.permissionType = permissionType;
    }

    public String getPermission() {
        return permission;
    }

    public String getPermissionType() {
        return permission;
    }

    public static Permission getRandomPermission() {
        final Random random = new Random();
        return values()[random.nextInt(values().length)];
    }
    public static Permission getRandomWithExcludedPermission(final Permission permission) {

        final List<Permission> enums =
                Arrays.stream(values()).filter(x -> !x.equals(permission)).collect(Collectors.toList());
        final Random random = new Random();
        return enums.get(random.nextInt(enums.size()));
    }
}
