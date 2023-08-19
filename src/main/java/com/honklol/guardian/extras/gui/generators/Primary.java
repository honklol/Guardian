package com.honklol.guardian.extras.gui.generators;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import static com.honklol.guardian.extras.gui.DataManager.*;

public class Primary {
    public static Inventory createGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 27, ChatColor.BOLD + "Guardian");

        gui.setItem(10, createItem(Material.COMMAND_BLOCK, "&f&lCommands", "&r&fExecute available commands interactively."));
        gui.setItem(11, createItem(Material.OAK_FENCE, "&f&lChecks", "&r&fModify anti-cheat check settings."));
        gui.setItem(12, createItem(Material.PLAYER_HEAD, "&f&lPlayers", "&r&fDisplay the online players, sorted from most violations to least."));
        gui.setItem(15, createConditionalWool(isLatestVersion(), "&a&lGuardian v" + getCurrentVersion(), "&r&fYou're up to date!", "&c&lGuardian v" + getCurrentVersion(), "&r&fYou're out of date! Please update the plugin (https://gac.lol)."));
        gui.setItem(16, createConditionalWool(isSupportedMCVersion(), "&a&lMinecraft " + getMCVersion(), "&r&fYou're running a supported MC version!", "&c&lMinecraft " + getMCVersion(), "&r&fYou're running an unsupported MC version! No support will be provided."));

        fillEmptySlotsWithPane(gui);

        return gui;
    }
}
