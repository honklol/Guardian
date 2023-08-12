/*
 * AntiCheatReloaded for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2022 Rammelkast
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
package com.honklol.guardian.event;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.temporary.TemporaryPlayer;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerDigType;
import com.honklol.guardian.check.movement.NoSlowCheck;
import com.honklol.guardian.check.packet.BadPacketsCheck;
import com.honklol.guardian.check.packet.MorePacketsCheck;

public final class PacketListener extends PacketAdapter {

	public PacketListener(final Plugin plugin) {
		super(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK,
				PacketType.Play.Client.LOOK, PacketType.Play.Server.POSITION, PacketType.Play.Client.BLOCK_DIG);
	}

	@Override
	public void onPacketReceiving(final PacketEvent event) {
		final Player player = event.getPlayer();
		if (player == null || !player.isOnline()) {
			return;
		}
		
		// Check if we have an actual player object
		if (player instanceof TemporaryPlayer) {
			return;
		}
		
		final PacketType type = event.getPacketType();
		final PacketContainer packet = event.getPacket();
		if (type == PacketType.Play.Client.POSITION || type == PacketType.Play.Client.POSITION_LOOK) {
			// Run MorePackets check
			MorePacketsCheck.runCheck(player, event);
			// Run BadPackets check
			BadPacketsCheck.runCheck(player, event);
			return;
		}
		
		if (event.getPacketType() == PacketType.Play.Client.BLOCK_DIG) {
			if (packet.getPlayerDigTypes().read(0) == PlayerDigType.RELEASE_USE_ITEM) {
				// Run NoSlow check
				NoSlowCheck.runCheck(event.getPlayer(), event);
			}
			
			return;
		}
	}

	@Override
	public void onPacketSending(final PacketEvent event) {
		final Player player = event.getPlayer();
		if (player == null || !player.isOnline()) {
			return;
		}
		
		// Check if we have an actual player object
		if (player instanceof TemporaryPlayer) {
			return;
		}
		
		final PacketType type = event.getPacketType();
		if (type == PacketType.Play.Server.POSITION) {
			// Compensate for teleport
			MorePacketsCheck.compensate(player);
		}
	}
	
}
