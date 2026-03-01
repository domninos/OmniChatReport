package net.omni.chatreport.listeners;

import net.omni.chatreport.OmniChatReport;
import net.omni.chatreport.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final OmniChatReport plugin;

    public PlayerListener(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getMuteManager().isMuted(player).thenAccept(isMuted -> {
            if (isMuted)
                plugin.getMuteManager().checkMute(player.getName());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getMuteManager().isMuted(player).thenAccept(isMuted -> {
            if (isMuted)
                plugin.getMuteManager().saveToDatabase(player);
        });
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        plugin.getMuteManager().isMuted(player).thenAccept(isMuted -> {
            if (!isMuted)
                return;

            Bukkit.getScheduler().runTask(plugin, () -> event.setCancelled(true));

            plugin.getMuteManager().getTimeLeft(player).thenAccept(time -> {
                if (!(time <= 0))
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.sendMessage(player, "&cYou are currently muted. Remaining: "
                            + TimeUtil.getTimeRemainingString(time)));
                else
                    plugin.getMuteManager().unmutePlayer(player.getName());
            });
        });
    }

    public void register() {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
