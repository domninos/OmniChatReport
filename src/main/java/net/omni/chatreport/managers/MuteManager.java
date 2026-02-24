package net.omni.chatreport.managers;

import net.omni.chatreport.OmniChatReport;
import net.omni.chatreport.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MuteManager {
    private static final Map<String, Long> PLAYER_TO_EXPIRE = new HashMap<>();
    private static final Map<String, String> PLAYER_TO_REASON = new HashMap<>();

    private final OmniChatReport plugin;

    public MuteManager(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    public void loadMuted(String name) {
        plugin.getDatabaseHandler().loadPlayer(name); // also load to PLAYER_TO maps
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

        PLAYER_TO_EXPIRE.put(player.getName(), millisTime);
        PLAYER_TO_REASON.put(player.getName(), reason);

        plugin.getDatabaseHandler().insert(issuer, player.getName(), millisTime, reason);

        // check if redis
        if (plugin.useRedis())
            plugin.getRedisHandler().mutePlayer(issuer, player.getName(), millisTime, reason, plugin.checkServer());

        return true;
    }

    public boolean isMuted(Player player) {
        if (player == null)
            return false;

        boolean cache = PLAYER_TO_EXPIRE.containsKey(player.getName());

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

    public void updateCache(Player player) {
        if (!isMuted(player))
            return;

        long expiry = getTimeLeft(player);

        if (expiry == 0 || expiry <= System.currentTimeMillis())
            unmutePlayer(player);
    }

    public long getTimeLeft(Player player) {
        return PLAYER_TO_EXPIRE.getOrDefault(player.getName(), 0L);
    }

    // TODO support multi-server
    public void unmutePlayer(Player player) {
        if (!isMuted(player))
            return;

        PLAYER_TO_EXPIRE.remove(player.getName());
        PLAYER_TO_REASON.remove(player.getName());

        if (plugin.useRedis())
            plugin.getRedisHandler().unmute(player.getName());

        plugin.getDatabaseHandler().delete(player.getName());

    }

    public String getReason(Player player) {
        return player != null ? PLAYER_TO_REASON.getOrDefault(player.getName(), "N/A") : "N/A";
    }

    public void flush() {
        PLAYER_TO_EXPIRE.clear();
        PLAYER_TO_REASON.clear();
    }
}
