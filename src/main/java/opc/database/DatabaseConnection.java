package opc.database;

import commons.config.ConfigHelper;
import commons.config.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private static Connection conn;

    private DatabaseConnection() throws SQLException {

        final Configuration configuration = ConfigHelper.getEnvironmentConfiguration();

        try {
            conn = DriverManager.getConnection(configuration.getDatabaseUrl(), configuration.getDatabaseUsername(), configuration.getDatabasePassword());
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    public Connection getConnection() {
        return conn;
    }

    public static DatabaseConnection getInstance() throws SQLException {
        if (instance==null) {
            instance = new DatabaseConnection();
        }else if (instance.getConnection().isClosed()) {
            instance= new DatabaseConnection();
        }
        return instance;
    }
}
