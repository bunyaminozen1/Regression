package opc.database;

import java.sql.*;
import java.util.*;
import java.util.stream.IntStream;

public class DatabaseHelper {

    public static Map<Integer, Map<String, String>> retrieveData(final String query) throws SQLException {

        final ResultSet resultSet = BaseDatabaseExtension.DB_CONNECTION.createStatement().executeQuery(query);

        final List<String> columnNames = new LinkedList<>();
        final Map<Integer, Map<String, String>> results = new HashMap<>();

        try {

            final ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                final String columnName = resultSetMetaData.getColumnName(i);
                columnNames.add(columnName);
            }

            while (resultSet.next()) {

                final Map<String, String> resultsMap = new LinkedHashMap<>();
                for (final String columnName : columnNames) {
                    resultsMap.put(columnName, resultSet.getString(columnName));
                }

                results.put(resultSet.getRow()-1, resultsMap);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            resultSet.close();
        }

        return results;
    }

    public static void update(final String query) throws SQLException {

        try {
            BaseDatabaseExtension.DB_CONNECTION.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int update(final String query, final List<String> fieldUpdates) throws SQLException {

        try {
            BaseDatabaseExtension.DB_CONNECTION.setAutoCommit(false);
            PreparedStatement preparedStatement = BaseDatabaseExtension.DB_CONNECTION.prepareStatement(query);

            IntStream.range(0, fieldUpdates.size()).forEach(i -> {
                try {
                    preparedStatement.setString(i + 1, fieldUpdates.get(i));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            final int result =  preparedStatement.executeUpdate();
            if (result > 0) {
                BaseDatabaseExtension.DB_CONNECTION.commit();
                BaseDatabaseExtension.DB_CONNECTION.setAutoCommit(true);
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
        return 0;
    }
}
