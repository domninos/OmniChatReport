package net.omni.chatreport.listeners;

import net.omni.chatreport.OmniChatReport;
import net.omni.chatreport.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final OmniChatReport plugin;

    public PlayerListener(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getMuteManager().isMuted(player))
            plugin.getMuteManager().loadMuted(player.getName());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        boolean muted = plugin.getMuteManager().isMuted(player);

        if (muted) {
            event.setCancelled(true);

            plugin.getMuteManager().updateCache(player);

            long timeLeft = plugin.getMuteManager().getTimeLeft(player);

            if (!plugin.getMuteManager().isMuted(player)) {
                plugin.sendMessage(player, "&aYou are now unmuted.");
            } else if (timeLeft > 0)
                plugin.sendMessage(player, "&cYou are currently muted. Remaining: "
                        + TimeUtil.getTimeRemainingString(timeLeft));

            if (timeLeft == 0)
                plugin.getMuteManager().unmutePlayer(player);
        }
    }

    public void register() {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
