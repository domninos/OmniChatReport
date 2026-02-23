package net.omni.chatreport.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.omni.chatreport.OmniChatReport;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHandler {

    private final OmniChatReport plugin;

    private HikariDataSource dataSource;

    private static final String TABLE_NAME = "mutes";

    public DatabaseHandler(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    public void loadDatabase() {
        plugin.sendConsole("&7Attempting to establish database connection...");

        String username = plugin.getConfig().getString("database.username");
        String password = plugin.getConfig().getString("database.password");
        String host = plugin.getConfig().getString("database.host");
        int port = plugin.getConfig().getInt("database.port");
        String database = "ocr_mutes";

        if (username == null || password == null || host == null || port == 0) {
            plugin.sendConsole("&cCould not find SQL credentials in config.yml. Aborting.");
            return;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);

        this.dataSource = new HikariDataSource(config);

        // TODO also Redis for mem-cache

        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("""
                    CREATE TABLE IF NOT EXISTS 'mutes' (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        uuid VARCHAR(36) NOT NULL,
                        name VARCHAR(36) NOT NULL,
                        reason VARCHAR(200) NOT NULL,
                        issued_by VARCHAR(36) NOT NULL,
                        expires_at BIGINT NOT NULL,
                        created_at BIGINT DEFAULT (UNIX_TIMESTAP() * 1000)
                    );
                """);
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }

            plugin.sendConsole("&aSuccessfully established MySQL connection.");
        });
    }

    public void insert(String playerName, long millisTime, String reason) {
        // TODO
        String sql = "INSERT INTO '" + TABLE_NAME
                + "'(uuid, name, reason, issued_by, expires_at) VALUES (?, ?, ?, ?, ?)";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void remove(String playerName) {
        // TODO
    }

    public boolean isMuted(String playerName) {
        // TODO

        return false;
    }

    public void closeDB() {
        try {
            if (dataSource != null)
                dataSource.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }
}