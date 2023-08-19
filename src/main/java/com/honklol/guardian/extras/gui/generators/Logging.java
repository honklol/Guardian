package com.honklol.guardian.extras.gui.generators;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import static com.honklol.guardian.extras.gui.DataManager.*;

public class Logging {
    public static Inventory createGUI(Player player, boolean fileLoggingEnabled, boolean consoleLoggingEnabled) {
        Inventory gui = Bukkit.createInventory(player, 27, ChatColor.BOLD + "Logging");

        gui.setItem(10, createLogToggleItem(fileLoggingEnabled, "file"));
        gui.setItem(12, createLogToggleItem(consoleLoggingEnabled, "console"));
        gui.setItem(19, createLogStatusItem(fileLoggingEnabled));
        gui.setItem(21, createLogStatusItem(consoleLoggingEnabled));
        gui.setItem(16, createItem(Material.BARRIER, "&f&lGo back", "&r&fReturn to the commands page."));

        fillEmptySlotsWithPane(gui);

        return gui;
    }
}
