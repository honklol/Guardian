package com.honklol.guardian.extras.gui;

import com.honklol.guardian.Guardian;
import com.honklol.guardian.extras.gui.generators.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static com.honklol.guardian.extras.gui.DataManager.*;

/**
 * A listener for inventory click events to handle GUI interactions.
 */
public class DataListener implements Listener {

    /**
     * Handles inventory click events.
     *
     * @param event The InventoryClickEvent to handle.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getItemMeta() == null) {
            return;
        }

        PersistentDataContainer container = clickedItem.getItemMeta().getPersistentDataContainer();
        PersistentDataType<String, String> dataType = PersistentDataType.STRING;
        NamespacedKey key = new NamespacedKey(Guardian.getInstance(), "guardian-interface-item");
        String storedData = container.get(key, dataType);
        if (!Objects.equals(storedData, "item_843p4ui")) {
            return;
        }

        event.setCancelled(true);

        String command = getCommand(clickedItem);
        if (command != null && !command.isEmpty()) {
            player.performCommand(command);
        }

        if (clickedItem.hasItemMeta() && Objects.requireNonNull(clickedItem.getItemMeta()).hasLore()) {
            String lore = ChatColor.stripColor(Objects.requireNonNull(clickedItem.getItemMeta().getLore()).toString());
            if (lore.equals("[Return to the commands page.]")) {
                player.openInventory(Commands.createGUI(player));
            }
            if (lore.equals("[Return to the main page.]")) {
                player.openInventory(Primary.createGUI(player));
            }
        }

        String displayName = ChatColor.stripColor(Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName());
        if (displayName.contains("Log to file") || displayName.contains("Log to console")) {
            player.openInventory(Logging.createGUI(player, isFileLoggingEnabled(), isConsoleLoggingEnabled()));
        }

        switch (clickedItem.getType()) {
            case COMMAND_BLOCK:
                player.openInventory(Commands.createGUI(player));
                break;
            case OAK_FENCE:
                player.openInventory(Checks.createGUI(player));
                break;
            case PLAYER_HEAD:
                player.openInventory(Players.createGUI(player));
                break;
            case OBSERVER:
                player.openInventory(Logging.createGUI(player, isFileLoggingEnabled(), isConsoleLoggingEnabled()));
                break;
        }
    }

    /**
     * Gets the corresponding command based on the clicked item.
     *
     * @param item The clicked ItemStack.
     * @return The command associated with the item, or null if no match.
     */
    private String getCommand(ItemStack item) {
        if (item != null && item.hasItemMeta() && Objects.requireNonNull(item.getItemMeta()).hasDisplayName()) {
            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            switch (displayName) {
                case "Reload":
                    return "guardian reload";
                case "Mute player alerts":
                    return "guardian mute";
                case "Log to file (on)":
                case "Log to file (off)":
                    return "guardian log file " + (displayName.endsWith("(on)") ? "off" : "on");
                case "Log to console (on)":
                case "Log to console (off)":
                    return "guardian log console " + (displayName.endsWith("(on)") ? "off" : "on");
            }
        }
        return null;
    }
}