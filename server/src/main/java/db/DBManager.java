package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManager {
    //private static final String URL = "jdbc:postgresql://pg/studs";
    //private static final String USER = "s467892";
    private static final String URL = "jdbc:postgresql://localhost:5432/lab_db";
    private static final String USER = "postgres";
    //private static final String PASSWORD = System.getenv("DB_PASSWORD");
    private static final String PASSWORD = "vLdOuTmfex0keFkI";
    private Connection connection;

    public DBManager() throws SQLException {
        this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Ошибка при закрытии подключения: " + e.getMessage());
        }
    }

}
