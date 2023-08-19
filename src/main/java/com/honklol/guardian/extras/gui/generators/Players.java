package com.honklol.guardian.extras.gui.generators;

import com.honklol.guardian.Guardian;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.honklol.guardian.util.User;
import com.honklol.guardian.check.CheckType;

import static com.honklol.guardian.extras.gui.DataManager.createItem;
import static com.honklol.guardian.extras.gui.DataManager.fillEmptySlotsWithPane;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Players {
    public static Inventory createGUI(Player player) {
        int inventorySize = Math.min(54, (int) Math.ceil(Bukkit.getOnlinePlayers().size() / 9.0) * 9);
        Inventory gui = Bukkit.createInventory(player, inventorySize, ChatColor.BOLD + "Players");
        gui.setItem(inventorySize - 1, createItem(Material.BARRIER, "&f&lGo back", "&r&fReturn to the main page."));

        List<PlayerViolation> playerViolations = new ArrayList<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            User user = Guardian.getManager().getUserManager().getUser(onlinePlayer.getUniqueId());
            int totalViolations = calculateTotalViolations(user);
            playerViolations.add(new PlayerViolation(onlinePlayer, totalViolations));
        }

        playerViolations.sort(Comparator.comparingInt(PlayerViolation::getViolations).reversed());

        int slot = 0;
        for (PlayerViolation playerViolation : playerViolations) {
            if (slot >= inventorySize) {
                break; // Inventory is full
            }

            Player onlinePlayer = playerViolation.getPlayer();
            User user = Guardian.getManager().getUserManager().getUser(onlinePlayer.getUniqueId());
            int totalViolations = playerViolation.getViolations();

            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
            assert skullMeta != null;
            skullMeta.setOwningPlayer(onlinePlayer);

            // Display total violations in display name
            skullMeta.setDisplayName(ChatColor.GOLD + onlinePlayer.getName() + ChatColor.WHITE + " (" + totalViolations + " violations)");

            List<String> userDataLore = new ArrayList<>();
            List<String> violationsLore = new ArrayList<>();
            userDataLore.add(ChatColor.GRAY + "Ping: " + user.getPing() + "ms " + ChatColor.DARK_GRAY + user.getMethod() + " "  + (user.isLagging() ? "(lagging)" : "(not lagging)"));

            // Iterate through check types and add violations to lore
            for (CheckType type : CheckType.values()) {
                int use = type.getUses(onlinePlayer.getUniqueId());
                if (use > 0) {
                    ChatColor color = ChatColor.WHITE;
                    if (use <= 20) {
                        color = ChatColor.YELLOW;
                    } else {
                        color = ChatColor.RED;
                    }
                    violationsLore.add(ChatColor.GRAY + type.getName() + ": " + color + use);
                }
            }

            if (violationsLore.isEmpty()) {
                userDataLore.add(ChatColor.GRAY + "This user has not failed any checks.");
            }

            // Sort lore by violations count in descending order
            violationsLore.sort((s1, s2) -> {
                int violations1 = Integer.parseInt(ChatColor.stripColor(s1).split(": ")[1]);
                int violations2 = Integer.parseInt(ChatColor.stripColor(s2).split(": ")[1]);
                return Integer.compare(violations2, violations1);
            });

            userDataLore.addAll(violationsLore);
            skullMeta.setLore(userDataLore);
            playerHead.setItemMeta(skullMeta);

            gui.setItem(slot, playerHead);
            slot++;
        }

        fillEmptySlotsWithPane(gui);

        return gui;
    }

    private static int calculateTotalViolations(User user) {
        int totalViolations = 0;
        for (CheckType type : CheckType.values()) {
            int use = type.getUses(user.getUUID());
            totalViolations += use;
        }
        return totalViolations;
    }

    @Getter
    public static class PlayerViolation {
        private final Player player;
        private final int violations;

        public PlayerViolation(Player player, int violations) {
            this.player = player;
            this.violations = violations;
        }
    }
}