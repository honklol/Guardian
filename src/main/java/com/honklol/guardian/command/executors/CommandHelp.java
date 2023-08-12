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

package com.honklol.guardian.command.executors;

import org.bukkit.command.CommandSender;

import com.honklol.guardian.command.CommandBase;
import com.honklol.guardian.util.Permission;

public class CommandHelp extends CommandBase {

    private static final String NAME = "Guardian Help";
    private static final String COMMAND = "help";
    private static final String USAGE = "anticheat help";
    private static final Permission PERMISSION = Permission.SYSTEM_HELP;
    private static final String[] HELP = {
    		RED + "/guardian " + GRAY + "help",
    		RED + "/guardian " + GRAY + "reload",
            RED + "/guardian " + GRAY + "version",
            RED + "/guardian " + GRAY + "mute",
    		RED + "/guardian " + GRAY + "checks",
            RED + "/guardian " + GRAY + "log " + WHITE + "[file/console] [on/off]",
            RED + "/guardian " + GRAY + "report " + WHITE + "[group/user]",
            RED + "/guardian " + GRAY + "reset " + WHITE + "[user]",
    };

    public CommandHelp() {
        super(NAME, COMMAND, USAGE, HELP, PERMISSION);
    }

    @Override
    protected void execute(CommandSender cs, String[] args) {
        sendHelp(cs);
    }
}
