/*
 * Guardian for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2023 Rammelkast
 * Copyright (c) 2023-2023 honklol
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

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.honklol.guardian.Guardian;
import com.honklol.guardian.command.CommandBase;
import com.honklol.guardian.util.Permission;

public class CommandMute extends CommandBase {

	private static final String NAME = "Guardian Mute";
	private static final String COMMAND = "mute";
	private static final String USAGE = "guardian mute";
	private static final Permission PERMISSION = Permission.SYSTEM_MUTE;
	private static final String[] HELP = {
			GRAY + "Use: " + AQUA + "/guardian mute" + GRAY + " to mute all notifications", };

	public CommandMute() {
		super(NAME, COMMAND, USAGE, HELP, PERMISSION);
	}

	@Override
	protected void execute(CommandSender cs, String[] args) {
		if (!(cs instanceof Player)) {
			cs.sendMessage(PREFIX + "This command is only for players!");
			return;
		}
		UUID uuid = ((Player) cs).getUniqueId();
		if (Guardian.MUTE_ENABLED_MODS.contains(uuid)) {
			cs.sendMessage(PREFIX + "Player alerts have been unmuted.");
			Guardian.MUTE_ENABLED_MODS.remove(uuid);
			return;
		} else {
			cs.sendMessage(PREFIX + "Player alerts have been muted.");
			Guardian.MUTE_ENABLED_MODS.add(uuid);
			return;
		}
	}
}
