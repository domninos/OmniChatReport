package net.omni.chatreport.managers;

import com.google.common.collect.Lists;
import net.omni.chatreport.OmniChatReport;
import net.omni.chatreport.util.ReportsMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIManager {

    private static final Map<String, ItemStack> ITEMS = new HashMap<>();

    private static final Map<ItemStack, String> ITEM_TO_COMMAND = new HashMap<>();

    private final OmniChatReport plugin;
    private final ReportsMenu reportsMenu;
    private Inventory inventory;

    public GUIManager(OmniChatReport plugin) {
        this.plugin = plugin;

        loadInventory();

        reportsMenu = new ReportsMenu(inventory);
    }

    public void loadInventory() {
        String name = plugin.getConfig().getString("gui.name");
        int slots = plugin.getConfig().getInt("gui.slots");
        boolean hashtag_glass = plugin.getConfig().getBoolean("gui.hashtag_glass");

        inventory = Bukkit.createInventory(null, slots, plugin.translate(name));

        List<String> item_rows = plugin.getConfig().getStringList("gui.item_rows");

        if (item_rows.isEmpty()) {
            plugin.sendConsole("&cCould not load reports because gui.item_rows is empty.");
            return;
        }

        ConfigurationSection item_section = plugin.getConfig().getConfigurationSection("gui.items");

        if (item_section == null) {
            plugin.sendConsole("&cCould not load reports because gui.items section is not found.");
            return;
        }


        for (String item_code : item_section.getKeys(false)) {
            if (item_code == null)
                continue;

            String type = item_section.getString(item_code + ".type");

            if (type == null) {
                plugin.sendConsole("&cCould not load " + item_code + " because 'type' is empty.");
                continue;
            }

            Material material = Material.matchMaterial(type.toUpperCase());

            if (material == null) {
                plugin.sendConsole("&cMaterial type: '" + type.toUpperCase() + "' not found.");
                continue;
            }

            String displayName = item_section.getString(item_code + ".name");
            List<String> lore = item_section.getStringList(item_code + ".lore");

            String command = item_section.getString(item_code + ".command");

            ItemStack item = loadItemStack(material, displayName, lore);

            ITEM_TO_COMMAND.put(item, command);

            ITEMS.put(item_code, item);
        }

        int slot;

        for (String row : item_rows) {
            if (row == null || row.isBlank()) {
                plugin.sendConsole("&cCould not load inventory because a row is empty.");
                continue;
            }

            slot = 0;

            for (String item_slot : row.split(" ")) {
                // # - glass_pane if hashtag_glass else empty

                if (item_slot.equals("#")) {
                    if (hashtag_glass)
                        inventory.setItem(slot, ITEMS.get("HASHTAG"));
                } else {
                    // CUSTOM ITEM
                    // SWEARING, ADVERTISING, DOMAINS, LINKS

                    if (ITEMS.containsKey(item_slot)) {
                        // one of the custom items
                        inventory.setItem(slot, ITEMS.get(item_slot));
                    }
                }

                slot++;
            }
        }

        plugin.sendConsole("&aLoaded reports GUI.");
    }

    private ItemStack loadItemStack(Material type, String displayName, List<String> lore) {
        ItemStack itemStack = new ItemStack(type);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta == null)
            meta = Bukkit.getItemFactory().getItemMeta(type);

        assert meta != null;

        if (displayName != null && !displayName.isEmpty())
            meta.setDisplayName(plugin.translate(displayName));
        else
            meta.setDisplayName(" ");

        if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = Lists.newArrayList();

            for (String line : lore)
                coloredLore.add(plugin.translate(line));

            meta.setLore(coloredLore);

            coloredLore.clear(); // dump
        }

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    public boolean isReportItem(ItemStack itemStack) {
        if (ITEMS.isEmpty())
            return false;

        for (ItemStack item : ITEMS.values()) {
            if (item != null && item.isSimilar(itemStack))
                return true;
        }

        return false;
    }

    public String getCodeCommand(ItemStack itemStack) {
        return ITEM_TO_COMMAND.getOrDefault(itemStack, "N/A");
    }

    public void flush() {
        ITEMS.clear();
        ITEM_TO_COMMAND.clear();
    }

    public void openGUI(Player player) {
        if (player != null)
            player.openInventory(this.inventory);
    }

    public ReportsMenu getReportsMenu() {
        return reportsMenu;
    }

}
