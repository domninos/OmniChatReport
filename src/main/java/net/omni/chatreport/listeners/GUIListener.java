package net.omni.chatreport.listeners;

import net.omni.chatreport.OmniChatReport;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final OmniChatReport plugin;

    public GUIListener(OmniChatReport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();

        if (!plugin.getGuiManager().getReportsMenu().isReportsMenu(inventory))
            return;

        ItemStack item = event.getCurrentItem();

        if (item == null || item.getType() == Material.AIR)
            return;

        if (!plugin.getGuiManager().isReportItem(item))
            return;

        String command = plugin.getGuiManager().getCodeCommand(item);

        if (command == null) {
            plugin.sendConsole("&cCould not run command since command is null.");
            return;
        }

        if (command.startsWith("/"))
            command = command.substring(1);

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        // replace {player}
        command = command.replace("{player}", player.getName());

        // TODO analyze previous chats (from database)
        // TODO detect if inappropriate

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

        player.closeInventory();
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
}
