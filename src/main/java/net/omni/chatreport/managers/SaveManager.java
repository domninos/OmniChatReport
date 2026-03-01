package net.omni.chatreport.managers;

import net.omni.chatreport.MutedPlayer;
import net.omni.chatreport.OmniChatReport;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

public class SaveManager {
    private static final Set<MutedPlayer> TO_SAVE = new HashSet<>();

    private final OmniChatReport plugin;

    public SaveManager(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    public void startPool() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAll, 20L, 20 * 30); // 30 mins (36,000 - 20 * (30 * 60) ticks

        plugin.sendConsole("&aInitiated save pool.");
    }

    public void saveAll() {
        if (!TO_SAVE.isEmpty())
            plugin.getDatabaseHandler().insertBatch(TO_SAVE);
    }

    public void saveAllDisable() {
        if (!TO_SAVE.isEmpty())
            plugin.getDatabaseHandler().insertBatchSync(TO_SAVE);
    }

    public void addToPool(String name) {
        MutedPlayer mutedPlayer = plugin.getMuteManager().getMutedPlayer(name);

        if (mutedPlayer == null) {
            plugin.sendConsole("&cCould not add to save pool because mutedPlayer is null.");
            return;
        }

        TO_SAVE.add(mutedPlayer);
    }

    public void addToPool(MutedPlayer mutedPlayer) {
        if (mutedPlayer != null)
            TO_SAVE.add(mutedPlayer);
    }

    public void removeFromPool(String name) {
        MutedPlayer mutedPlayer = plugin.getMuteManager().getMutedPlayer(name);

        if (mutedPlayer == null) {
            plugin.sendConsole("&cCould not remove from pool because mutedPlayer is null.");
            return;
        }

        TO_SAVE.remove(mutedPlayer);
    }

    public void removeFromPool(MutedPlayer mutedPlayer) {
        if (mutedPlayer != null)
            TO_SAVE.remove(mutedPlayer);
    }

    public boolean isBeingSaved(MutedPlayer mutedPlayer) {
        return mutedPlayer != null && TO_SAVE.contains(mutedPlayer);
    }

    public void flush() {
        TO_SAVE.clear();
    }
}
