package com.honklol.guardian.extras.gui.generators;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.honklol.guardian.manage.CheckManager;
import com.honklol.guardian.check.CheckType;
import com.honklol.guardian.Guardian;
import static com.honklol.guardian.extras.gui.DataManager.createItem;
import static com.honklol.guardian.extras.gui.DataManager.fillEmptySlotsWithPane;
import static com.honklol.guardian.extras.gui.DataManager.PREFIX;

public class Checks {
    public static Inventory createGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 36, PREFIX + "Checks");

        CheckManager checkManager = Guardian.getManager().getCheckManager();
        for (int i = 0; i < CheckType.values().length; i++) {
            CheckType type = CheckType.values()[i];
            Material woolType = checkManager.isActive(type) ? Material.GREEN_WOOL : Material.RED_WOOL;
            String woolColor = checkManager.isActive(type) ? "&a&l" : "&c&l";

            ItemStack checkItem = createItem(woolType, woolColor + type.getName());
            gui.setItem(i, checkItem);
        }

        gui.setItem(35, createItem(Material.BARRIER, "&f&lGo back", "&r&fReturn to the main page."));

        fillEmptySlotsWithPane(gui);

        return gui;
    }
}
