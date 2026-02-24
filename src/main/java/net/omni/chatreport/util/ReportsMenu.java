package net.omni.chatreport.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ReportsMenu implements InventoryHolder {
    private final Inventory inventory;

    public ReportsMenu(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public boolean isReportsMenu(Inventory other) {
        return other.equals(this.inventory);
    }
}
