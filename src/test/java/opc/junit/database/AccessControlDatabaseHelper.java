package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccessControlDatabaseHelper {

    private final static String DATABASE_NAME = "access_control";

    public static Map<Integer, Map<String, String>> getPermissionByType(final String permissionType) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.permission p where permission_type = '%s' and deprecated_on IS NULL;", DATABASE_NAME, permissionType));
    }

    public static Map<Integer, Map<String, String>> getPermissionByTypes(final List<String> permissionTypes) throws SQLException {

        final List<String> formattedTypes = new ArrayList<>();

        permissionTypes.forEach(permission -> formattedTypes.add(String.format("'%s'", permission)));

        return DatabaseHelper
                .retrieveData(String.format("SELECT * FROM %s.permission p where permission_type IN (%s) and deprecated_on IS NULL;", DATABASE_NAME, String.join(",", formattedTypes)));
    }
}
