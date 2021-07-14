package com.astralsmp.modules;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private final SQLiteDataSource dataSource = new SQLiteDataSource();
    private final String DB_URL;

    public Database(String db_name) {
        DB_URL = "jdbc:sqlite:C:/SQLite3/" + db_name;
    }

    public void initialize() {
        dataSource.setUrl(DB_URL);

        try (Connection connection = dataSource.getConnection()) {
            tableCreation(connection);
            System.out.println("Подключено к бд");
        } catch (SQLException exception) {
            exception.printStackTrace();
            System.out.println("Не подключено к бд");
        }
    }

    private void tableCreation(Connection connection) throws SQLException {
        // astral linked players table
        String query = "CREATE TABLE IF NOT EXISTS astral_linked_players (" +
                "uuid TEXT, display_name VARCHAR(16), discord_id DECIMAL(18,0)" +
                ");";
        Statement statement = connection.createStatement();
        statement.execute(query);
        statement.close();

    }

}
