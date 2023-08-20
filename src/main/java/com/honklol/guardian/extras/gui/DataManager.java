package com.honklol.guardian.extras.gui;

import com.honklol.guardian.Guardian;
import com.honklol.guardian.util.UpdateManager;
import com.honklol.guardian.util.VersionLib;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.honklol.guardian.command.CommandBase.CONFIG;

/**
 * A utility class for creating various GUI-related items and checking plugin & MC versions, plus logging statuses.
 */
public class DataManager {
    public static final String PREFIX = ChatColor.RED + "" + ChatColor.BOLD + "Guardian" + ChatColor.BLACK + " > " + ChatColor.DARK_GRAY;

    @Getter
    private enum LoggingType {
        FILE(Material.BOOK),
        CONSOLE(Material.ENDER_EYE);

        private final Material material;

        LoggingType(Material material) {
            this.material = material;
        }
    }

    /**
     * Creates an item with the specified material, display name, and lore.
     *
     * @param material    The material of the item.
     * @param displayName The display name of the item.
     * @param lore        The lore lines of the item.
     * @return The created ItemStack.
     */
    public static ItemStack createItem(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        meta.setLore(Arrays.stream(lore).map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList()));
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Guardian.getInstance(), "guardian-interface-item");
        container.set(key, PersistentDataType.STRING, "item_843p4ui");
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a conditional wool item based on the provided condition.
     *
     * @param condition           The condition to determine wool color.
     * @param enabledDisplayName  The display name for the enabled state.
     * @param enabledLore         The lore for the enabled state.
     * @param disabledDisplayName The display name for the disabled state.
     * @param disabledLore        The lore for the disabled state.
     * @return The created conditional wool ItemStack.
     */
    public static ItemStack createConditionalWool(boolean condition, String enabledDisplayName, String enabledLore, String disabledDisplayName, String disabledLore) {
        Material woolType = condition ? Material.GREEN_WOOL : Material.RED_WOOL;
        String displayName = condition ? enabledDisplayName : disabledDisplayName;
        String lore = condition ? enabledLore : disabledLore;
        return createItem(woolType, displayName, lore);
    }

    /**
     * Creates a logging toggle item with the specified type and status.
     *
     * @param isEnabled Whether the toggle is enabled.
     * @param type      The type of logging to toggle.
     * @return The created toggle ItemStack.
     */
    public static ItemStack createLogToggleItem(boolean isEnabled, String type) {
        LoggingType loggingType = LoggingType.valueOf(type.toUpperCase());
        String status = isEnabled ? ChatColor.GREEN + "(on)" : ChatColor.RED + "(off)";
        String displayName = String.format("&f&lLog to %s %s", type, status);
        String lore = "&r&fToggle the " + type + " logging switch.";
        return createItem(loggingType.getMaterial(), displayName, lore);
    }

    /**
     * Creates a logging status item indicating whether logging is enabled or disabled.
     *
     * @param isEnabled Whether logging is enabled.
     * @return The created status ItemStack.
     */
    public static ItemStack createLogStatusItem(boolean isEnabled) {
        Material material = isEnabled ? Material.GREEN_WOOL : Material.RED_WOOL;
        return createItem(material, isEnabled ? "&a&lEnabled" : "&c&lDisabled");
    }

    /**
     * Fills empty slots in the given GUI inventory with gray stained glass panes.
     *
     * @param gui The GUI inventory to fill.
     */
    public static void fillEmptySlotsWithPane(Inventory gui) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "");

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }

    /**
     * Retrieves the current version of the Guardian plugin.
     *
     * @return The current plugin version.
     */
    public static String getCurrentVersion() {
        UpdateManager updateManager = Guardian.getUpdateManager();
        return updateManager.getCurrentVersion();
    }

    /**
     * Checks if the current version of the Guardian plugin is the latest available version.
     *
     * @return {@code true} if the plugin is up to date, otherwise {@code false}.
     */
    public static boolean isLatestVersion() {
        UpdateManager updateManager = Guardian.getUpdateManager();
        return updateManager.isLatest();
    }

    /**
     * Retrieves the version of Minecraft being used.
     *
     * @return The version of Minecraft.
     */
    public static String getMCVersion() {
        return VersionLib.getVersion();
    }

    /**
     * Checks if the current version of Minecraft is supported by the plugin.
     *
     * @return {@code true} if the version is supported, otherwise {@code false}.
     */
    public static boolean isSupportedMCVersion() {
        return VersionLib.isSupported();
    }

    /**
     * Checks if file logging is enabled in the plugin's configuration.
     *
     * @return {@code true} if file logging is enabled, otherwise {@code false}.
     */
    public static boolean isFileLoggingEnabled() {
        return CONFIG.getConfig().logToFile.getValue();
    }

    /**
     * Checks if console logging is enabled in the plugin's configuration.
     *
     * @return {@code true} if console logging is enabled, otherwise {@code false}.
     */
    public static boolean isConsoleLoggingEnabled() {
        return CONFIG.getConfig().logToConsole.getValue();
    }
}