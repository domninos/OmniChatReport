package net.omni.chatreport.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.omni.chatreport.MutedPlayer;
import net.omni.chatreport.OmniChatReport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class DatabaseHandler {

    public static final String TABLE_NAME = "mutes";

    private final OmniChatReport plugin;
    private HikariDataSource dataSource;

    // TODO make a new Thread dedicated for database

    public DatabaseHandler(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    public void loadDatabase() {
        plugin.sendConsole("&7Attempting to establish database connection...");

        String username = plugin.getConfig().getString("database.username");
        String password = plugin.getConfig().getString("database.password");
        String host = plugin.getConfig().getString("database.host");
        int port = plugin.getConfig().getInt("database.port");
        String database = plugin.getConfig().getString("database.database");

        if (username == null || password == null || host == null || port == 0 || database == null) {
            plugin.sendConsole("&cCould not find SQL credentials in config.yml. Aborting.");
            return;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);

        this.dataSource = new HikariDataSource(config);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {

                statement.execute("""
                            CREATE TABLE IF NOT EXISTS mutes (
                                id INT AUTO_INCREMENT PRIMARY KEY,
                                name VARCHAR(36) NOT NULL UNIQUE,
                                reason VARCHAR(200) NOT NULL,
                                issued_by VARCHAR(36) NOT NULL,
                                expires_at BIGINT NOT NULL,
                                created_at BIGINT NOT NULL
                            );
                        """);
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }

            plugin.sendConsole("&aSuccessfully established MySQL connection.");
        });
    }

    /*
    name
    id
    reason
    issued_by
    expires_at
    created_at
     */

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public void loadPlayer(String name) {
        String sql = "SELECT reason, issued_by, expires_at FROM " + TABLE_NAME + " WHERE name = ? LIMIT 1";

        Player player = Bukkit.getPlayer(name);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, name);

                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    String reason = rs.getString("reason");
                    String issuer = rs.getString("issued_by");
                    long expires_at = rs.getLong("expires_at");

                    if (expires_at <= System.currentTimeMillis())
                        delete(name);
                    else
                        plugin.getMuteManager().mutePlayer(issuer, player, expires_at, reason);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // SHOULD NOT BE USED FREQUENTLY
    public void delete(String playerName) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE name = ?";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerName);

                statement.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // SHOULD NOT BE USED FREQUENTLY
    public void insert(MutedPlayer mutedPlayer) {
        String issuer = mutedPlayer.issuer();
        String playerName = mutedPlayer.playerName();
        long millisTime = mutedPlayer.expiry();
        String reason = mutedPlayer.reason();

        String sql = "INSERT IGNORE INTO " + TABLE_NAME + "(name, reason, issued_by, expires_at, created_at) VALUES (?, ?, ?, ?, ?)";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerName);
                statement.setString(2, reason);
                statement.setString(3, issuer);
                statement.setLong(4, millisTime);

                statement.setLong(5, System.currentTimeMillis());

                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // SHOULD NOT BE USED FREQUENTLY (ONLY ON JOIN)
    public CompletableFuture<Boolean> exists(String playerName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String sql = "SELECT 1 FROM mutes WHERE name = ? LIMIT 1;";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerName);

                try (ResultSet rs = statement.executeQuery()) {
                    future.complete(rs.next());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public void closeDB() {
        try {
            if (dataSource != null)
                dataSource.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}