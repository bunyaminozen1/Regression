package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;
import java.util.Map;

public class DocumentRepositoryDatabaseHelper {

    private final static String DATABASE_NAME = "documentrepository";

    public static Map<Integer, Map<String, String>> getFile(final String fileName, final String identityId) throws SQLException {
        return DatabaseHelper
                .retrieveData(String.format("select * from %s.file f WHERE f.file_name LIKE '%%%s%%' and f.filepath LIKE '%%%s%%' ORDER BY f.file_name ASC;", DATABASE_NAME, fileName, identityId));
    }
}
