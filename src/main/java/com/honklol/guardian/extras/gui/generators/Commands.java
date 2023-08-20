package com.honklol.guardian.extras.gui.generators;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import static com.honklol.guardian.extras.gui.DataManager.createItem;
import static com.honklol.guardian.extras.gui.DataManager.fillEmptySlotsWithPane;
import static com.honklol.guardian.extras.gui.DataManager.PREFIX;

public class Commands {
    public static Inventory createGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, PREFIX + "Commands");

        gui.setItem(10, createItem(Material.MAGMA_BLOCK, "&f&lReload", "&r&fReload Guardian's configuration."));
        gui.setItem(11, createItem(Material.NOTE_BLOCK, "&f&lMute player alerts", "&r&fToggle the player alert mute."));
        gui.setItem(12, createItem(Material.OBSERVER, "&f&lLogging", "&r&fConfigure Guardian's logging."));
        gui.setItem(16, createItem(Material.BARRIER, "&f&lGo back", "&r&fReturn to the main page."));

        fillEmptySlotsWithPane(gui);

        return gui;
    }
}
