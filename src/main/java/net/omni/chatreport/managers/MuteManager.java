package net.omni.chatreport.managers;

import net.omni.chatreport.OmniChatReport;
import net.omni.chatreport.util.TimeUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MuteManager {
    private static final Map<String, Long> PLAYER_TO_EXPIRE = new HashMap<>();
    private static final Map<String, String> PLAYER_TO_REASON = new HashMap<>();


    private final OmniChatReport plugin;

    public MuteManager(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // load to cache from database

        if (plugin.useRedis())
            plugin.getRedisHandler().subscribe();
    }

    // TODO load from database
    public void loadMuted(String string, long duration) {

    }

    // TODO load from database
    public void loadReasons(String string, String reason) {

    }

    // TODO support multi-server (check for each server's playerlist)
    public boolean mutePlayer(String issuer, Player player, String timeString, String reason) {
        if (player == null || !player.isOnline())
            return false;

        if (isMuted(player)) {
            plugin.sendConsole("&c" + player.getName() + " is already muted.");
            return false;
        }

        long millisTime;

        try {
            millisTime = TimeUtil.parseDuration(timeString);
        } catch (Exception e) {
            plugin.sendConsole(e.getMessage());
            return false;
        }

        PLAYER_TO_EXPIRE.put(player.getName(), millisTime);
        PLAYER_TO_REASON.put(player.getName(), reason);

        plugin.getDatabaseHandler().insert(issuer, player.getName(), millisTime, reason);

        // check if redis
        if (plugin.useRedis()) {
            // TODO
            plugin.getRedisHandler().mutePlayer(player.getName(), millisTime);
            plugin.getRedisHandler().publishMute(player.getName(), millisTime);
        }

        return true;
    }

    public boolean isMuted(Player player) {
        if (plugin.useRedis())  {
            if (plugin.getRedisHandler().isMuted(player.getName()))
                return true;
        }

        return player != null && PLAYER_TO_EXPIRE.containsKey(player.getName());
    }

    // TODO support multi-server support
    public void unmutePlayer(Player player) {
        if (!isMuted(player))
            return;

        if (plugin.useRedis()) {
            plugin.getRedisHandler().unmute(player.getName());
            plugin.getRedisHandler().publishUnmute(player.getName());
        }

        PLAYER_TO_EXPIRE.remove(player.getName());
        PLAYER_TO_REASON.remove(player.getName());

//        plugin.getDatabaseHandler().

    }

    public String getReason(Player player) {
        return player != null ? PLAYER_TO_REASON.getOrDefault(player.getName(), "N/A") : "N/A";
    }

    public void flush() {
        PLAYER_TO_EXPIRE.clear();
        PLAYER_TO_REASON.clear();
    }
}
