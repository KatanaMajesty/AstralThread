package com.astralsmp.modules;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final SQLiteDataSource dataSource = new SQLiteDataSource();
    private final String DB_URL;

    public Database(String dbName) {
        DB_URL = "jdbc:sqlite:C:/SQLite3/" + dbName;
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

    public static void insertValues(Object[] userInfo, String tableName) {
        if (!dataSource.getUrl().isEmpty()) {
            try (Connection connection = dataSource.getConnection()) {
                String query = "INSERT INTO " + tableName + " (uuid, display_name, discord_id) VALUES (?, ?, ?)";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setObject(1, userInfo[0]);
                statement.setObject(2, userInfo[1]);
                statement.setObject(3, userInfo[2]);
                statement.executeUpdate();
                System.out.println("Данные добавлены в бд");
            } catch (SQLException exception) {
                exception.printStackTrace();
                System.out.println("Не удалось добавить значения");
            }
        } else System.out.println("Ссылка на бд пуста");
    }

}
