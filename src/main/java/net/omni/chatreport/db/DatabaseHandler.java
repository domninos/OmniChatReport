package net.omni.chatreport.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.omni.chatreport.MutedPlayer;
import net.omni.chatreport.OmniChatReport;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DatabaseHandler {

    public static final String TABLE_NAME = "mutes";

    private final OmniChatReport plugin;
    private HikariDataSource dataSource;

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
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=true"
                + "&requireSSL=true"
                + "&verifyServerCertificate=false"
                + "&rewriteBatchedStatements=true");

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
                                server VARCHAR(36) NOT NULL,
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

    // returns issued_by, reason, server, expires_at
    public CompletableFuture<Map<String, Object>> select(String playerName) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

        String sql = "SELECT name, issued_by, reason, server, expires_at FROM " + TABLE_NAME + " WHERE name = ? LIMIT 1";
        // get all since pang-load to onjoin if muted

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerName);

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> map = new HashMap<>();

                        String issuer = rs.getString("issued_by");
                        String server = rs.getString("server");
                        String reason = rs.getString("reason");
                        long expires_at = rs.getLong("expires_at");

                        map.put("issuer", issuer);
                        map.put("server", server);
                        map.put("reason", reason);
                        map.put("expires_at", expires_at);

                        future.complete(map);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return future;
    }

    // SHOULD NOT BE USED FREQUENTLY
    public void insert(MutedPlayer mutedPlayer) {
        String issuer = mutedPlayer.issuer();
        String playerName = mutedPlayer.playerName();
        long millisTime = mutedPlayer.expiry();
        String reason = mutedPlayer.reason();

        String sql = "INSERT IGNORE INTO " + TABLE_NAME + "(name, reason, issued_by, server, expires_at, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, playerName);
                statement.setString(2, reason);
                statement.setString(3, issuer);
                statement.setString(4, mutedPlayer.getFromServer());
                statement.setLong(5, millisTime);

                statement.setLong(6, System.currentTimeMillis());

                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // SHOULD NOT BE USED FREQUENTLY
    public void insertBatch(Set<MutedPlayer> mutedPlayers) {
        if (mutedPlayers.isEmpty())
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> insertBatchSync(mutedPlayers));
    }

    // SHOULD NOT BE USED FREQUENTLY (ONLY ON SERVER DISABLE)
    public void insertBatchSync(Set<MutedPlayer> mutedPlayers) {
        if (mutedPlayers.isEmpty())
            return;

        String sql = "INSERT IGNORE INTO " + TABLE_NAME + "(name, reason, issued_by, server, expires_at, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int counter = 0;

            for (MutedPlayer mutedPlayer : mutedPlayers) {
                String playerName = mutedPlayer.playerName();
                String reason = mutedPlayer.reason();
                String issuer = mutedPlayer.issuer();
                long millisTime = mutedPlayer.expiry();

                statement.setString(1, playerName);
                statement.setString(2, reason);
                statement.setString(3, issuer);
                statement.setString(4, mutedPlayer.getFromServer());
                statement.setLong(5, millisTime);

                statement.setLong(6, System.currentTimeMillis());

                statement.addBatch();

                counter++;
            }

            statement.executeBatch();

            int finalCounter = counter;

            plugin.getSaveManager().flush();

            plugin.sendConsole("&aSaved batch of " + finalCounter + " muted players.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
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