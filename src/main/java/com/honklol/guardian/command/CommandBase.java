/*
 * Guardian for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2022 Rammelkast
 * Copyright (c) 2022-2023 honklol
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.honklol.guardian.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.honklol.guardian.Guardian;
import com.honklol.guardian.config.Configuration;
import com.honklol.guardian.manage.CheckManager;
import com.honklol.guardian.manage.UserManager;
import com.honklol.guardian.util.Permission;

public class CommandBase {

    public static final Configuration CONFIG = Guardian.getManager().getConfiguration();
    public static final UserManager USER_MANAGER = Guardian.getManager().getUserManager();
    public static final CheckManager CHECK_MANAGER = Guardian.getManager().getCheckManager();
    public static final ChatColor RED = ChatColor.RED;
    public static final ChatColor YELLOW = ChatColor.YELLOW;
    public static final ChatColor GREEN = ChatColor.GREEN;
    public static final ChatColor WHITE = ChatColor.WHITE;
    public static final ChatColor GRAY = ChatColor.GRAY;
    public static final ChatColor GOLD = ChatColor.GOLD;
    public static final ChatColor AQUA = ChatColor.AQUA;
    public static final Server SERVER = Bukkit.getServer();
    public static final String PERMISSIONS_ERROR = RED + "You don't have the permissions to use this command.";
    public static final String MENU_END = RED + "-----------------------------------------------------";

    private final String name;
    private final String command;
    private final String usage;
    private final String[] help;
    private final Permission permission;

    public CommandBase(String name, String command, String usage, String help[], Permission permission) {
        this.name = name;
        this.command = command;
        this.usage = usage;
        this.help = help;
        this.permission = permission;
    }

    public void run(CommandSender cs, String[] args) {
        if (permission.get(cs)) {
            execute(cs, args);
		} else {
			if (args == null) {
				cs.sendMessage(RED + "" + ChatColor.BOLD + "Guardian" + ChatColor.DARK_GRAY + " > "
						+ ChatColor.GRAY + "Running " + RED + "Guardian AntiCheat " + GRAY + "version "
						+ RED + Guardian.getVersion());
			} else {
				cs.sendMessage(PERMISSIONS_ERROR + " (" + WHITE + permission.toString() + RED + ")");
			}
		}
	}

    protected void execute(CommandSender cs, String[] args) {
        return; // Stub
    }

    public void sendHelp(CommandSender cs) {
        cs.sendMessage(RED + "" + ChatColor.BOLD + "Guardian" + ChatColor.DARK_GRAY + " > " + ChatColor.GRAY + "Help Menu");
        cs.sendMessage(RED + "Usage: " + GRAY + (cs instanceof Player ? "/" : "") + getUsage());
        cs.sendMessage(RED + "Permission: " + GRAY + getPermission().toString());
        for (String string : getHelp()) {
            cs.sendMessage(string);
        }
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }

    public String getUsage() {
        return usage;
    }

    public String[] getHelp() {
        return help;
    }

    public Permission getPermission() {
        return permission;
    }
}
