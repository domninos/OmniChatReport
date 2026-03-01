package net.omni.chatreport.managers;

import net.omni.chatreport.MutedPlayer;
import net.omni.chatreport.OmniChatReport;
import net.omni.chatreport.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MuteManager {
    private static final Map<String, MutedPlayer> PLAYER_MAP = new HashMap<>();

    private final OmniChatReport plugin;

    public MuteManager(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    public void checkMute(String name) {
        if (PLAYER_MAP.containsKey(name))
            return;

        // add to cache

        CompletableFuture<Map<String, Object>> future = plugin.getDatabaseHandler().select(name);

        future.thenAccept(result -> {
            String issuer = String.valueOf(result.get("issuer"));
            long millisTime = Long.parseLong(String.valueOf(result.get("expires_at")));
            String reason = String.valueOf(result.get("reason"));
            String server = String.valueOf(result.get("server"));

            MutedPlayer mutedPlayer = new MutedPlayer(issuer, name, millisTime, reason, server);

            PLAYER_MAP.put(name, mutedPlayer);

            // check if redis
            if (plugin.useRedis())
                plugin.getRedisHandler().mutePlayer(issuer, name, millisTime, reason, server, false);

            result.clear(); // flush
        });
    }

    public CompletableFuture<Boolean> mutePlayer(String issuer, Player player, String server, String timeString, String reason) {
        long millisTime;

        try {
            millisTime = TimeUtil.parseDuration(timeString);
        } catch (Exception e) {
            plugin.sendConsole(e.getMessage());
            return CompletableFuture.completedFuture(false);
        }

        return mutePlayerServer(issuer, player.getName(), server, millisTime, reason, false, false, true);
    }

    public CompletableFuture<Boolean> mutePlayerServer(String issuer, String playerName, String server, long millisTime, String reason, boolean fromAnother, boolean onJoin, boolean publish) {
        Player player = Bukkit.getPlayer(playerName);

        if (!fromAnother && (player == null || !player.isOnline()))
            return CompletableFuture.completedFuture(false);

        return isMuted(player).thenApply(isMuted -> {
            if (isMuted && !onJoin) {
                Player issuerPlayer = Bukkit.getPlayer(issuer);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (issuerPlayer != null)
                        plugin.sendMessage(issuerPlayer, "&c" + playerName + " is already muted.");
                    else
                        plugin.sendConsole("&c" + playerName + " is already muted.");
                });

                return false;
            }

            MutedPlayer mutedPlayer = new MutedPlayer(issuer, playerName, millisTime, reason, server);

            PLAYER_MAP.put(playerName, mutedPlayer);

            if (plugin.useRedis())
                plugin.getRedisHandler().mutePlayer(issuer, playerName, millisTime, reason, server, publish);

            plugin.getSaveManager().addToPool(mutedPlayer);

            return true;
        });
    }

    public CompletableFuture<Boolean> isMuted(Player player) {
        if (player == null || !player.isOnline())
            return CompletableFuture.completedFuture(false);

        if (PLAYER_MAP.containsKey(player.getName()))
            return CompletableFuture.completedFuture(true);

        if (plugin.useRedis())
            return CompletableFuture.completedFuture(plugin.getRedisHandler().isMuted(player.getName()));

        return plugin.getDatabaseHandler().exists(player.getName());
    }

    public CompletableFuture<Boolean> mutePlayer(String issuer, String playerName, String server, String timeString, String reason, boolean fromAnother, boolean publish) {
        long millisTime;

        try {
            millisTime = TimeUtil.parseDuration(timeString);
        } catch (Exception e) {
            plugin.sendConsole(e.getMessage());
            return CompletableFuture.completedFuture(false);
        }

        return mutePlayerServer(issuer, playerName, server, millisTime, reason, fromAnother, false, publish);
    }

    public void saveToDatabase(Player player) {
        isMuted(player).thenAccept(isMuted -> {
            if (!isMuted) {
                plugin.sendConsole("&cCould not save to database because " + player.getName() + " is not muted.");
                return;
            }

            MutedPlayer mutedPlayer = PLAYER_MAP.get(player.getName());

            plugin.getDatabaseHandler().insert(mutedPlayer); // is async already
        });
    }

    public CompletableFuture<Long> getTimeLeft(Player player) {
        return isMuted(player).thenApply(isMuted -> {
            if (!isMuted) {
                plugin.sendConsole("not muted");
                return 0L;
            }

            MutedPlayer mutedPlayer = PLAYER_MAP.get(player.getName());

            if (mutedPlayer == null)
                return 0L;

            return mutedPlayer.getExpiry();
        });
    }

    public CompletableFuture<Boolean> unmutePlayer(String playerName) {
        return unmutePlayer(playerName, true);
    }

    public CompletableFuture<Boolean> unmutePlayer(String playerName, boolean publish) {
        return isMuted(playerName).thenApply(isMuted -> {
            if (!isMuted)
                return false;

            plugin.getSaveManager().removeFromPool(playerName);

            MutedPlayer mutedPlayer = PLAYER_MAP.remove(playerName);

            if (plugin.useRedis())
                plugin.getRedisHandler().unmute(playerName, mutedPlayer.getFromServer(), publish);

            plugin.getDatabaseHandler().delete(playerName); // async already

            return true;
        });
    }

    public CompletableFuture<Boolean> isMuted(String playerName) {
        return isMuted(Bukkit.getPlayer(playerName));
    }

    public MutedPlayer getMutedPlayer(String playerName) {
        return PLAYER_MAP.get(playerName);
    }

    public void flush() {
        PLAYER_MAP.clear();
    }
}
