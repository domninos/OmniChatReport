package net.omni.chatreport.managers;

import net.omni.chatreport.MutedPlayer;
import net.omni.chatreport.OmniChatReport;
import net.omni.chatreport.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MuteManager {
    private static final Map<String, MutedPlayer> PLAYER_MAP = new HashMap<>();

    private final OmniChatReport plugin;

    public MuteManager(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    public void loadMuted(String name) {
        plugin.getDatabaseHandler().loadPlayer(name); // also load to PLAYER_MAP
    }

    public boolean mutePlayer(String issuer, Player player, String timeString, String reason) {
        long millisTime;

        try {
            millisTime = TimeUtil.parseDuration(timeString);
        } catch (Exception e) {
            plugin.sendConsole(e.getMessage());
            return false;
        }

        return mutePlayer(issuer, player, millisTime, reason);
    }

    public boolean mutePlayer(String issuer, Player player, long millisTime, String reason) {
        if (player == null || !player.isOnline()) {
            plugin.sendConsole("&cCould not mute player because player is not found.");
            return false;
        }

        if (isMuted(player)) {
            Player issuerPlayer = Bukkit.getPlayer(issuer);

            // what if console
            if (issuerPlayer != null)
                plugin.sendMessage(issuerPlayer, "&c" + player.getName() + " is already muted.");
            else
                plugin.sendConsole("&c" + player.getName() + " is already muted.");
            return false;
        }

        MutedPlayer mutedPlayer = new MutedPlayer(issuer, player, millisTime, reason);

        PLAYER_MAP.put(player.getName(), mutedPlayer);

        // check if redis
        if (plugin.useRedis())
            plugin.getRedisHandler().mutePlayer(issuer, player.getName(), millisTime, reason, plugin.checkServer());

        return true;
    }

    public boolean isMuted(Player player) {
        if (player == null || !player.isOnline())
            return false;

        boolean cache = PLAYER_MAP.containsKey(player.getName());

        if (!cache) {
            if (plugin.useRedis())
                return plugin.getRedisHandler().isMuted(player.getName());

            // MySQL fallback
            try {
                return plugin.getDatabaseHandler().exists(player.getName()).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return cache;
    }

    public void saveToDatabase(Player player) {
        if (!isMuted(player)) {
            plugin.sendConsole("&cCould not save to database because " + player.getName() + " is not muted.");
            return;
        }

        MutedPlayer mutedPlayer = PLAYER_MAP.get(player.getName());

        plugin.getDatabaseHandler().insert(mutedPlayer);
    }

    public void updateCache(Player player) {
        if (!isMuted(player))
            return;

        long expiry = getTimeLeft(player);

        if (expiry == 0 || expiry <= System.currentTimeMillis())
            unmutePlayer(player);
    }

    public long getTimeLeft(Player player) {
        return isMuted(player) ? getMutedPlayer(player.getName()).getExpiry() : 0L;
    }

    public void unmutePlayer(Player player) {
        if (!isMuted(player))
            return;

        PLAYER_MAP.remove(player.getName());

        if (plugin.useRedis())
            plugin.getRedisHandler().unmute(player.getName(), plugin.checkServer());

        plugin.getDatabaseHandler().delete(player.getName());
    }

    public MutedPlayer getMutedPlayer(String playerName) {
        return PLAYER_MAP.get(playerName);
    }

    public void flush() {
        PLAYER_MAP.clear();
    }
}
