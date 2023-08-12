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
package com.honklol.guardian.api.event;

import java.util.List;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.honklol.guardian.util.User;

/**
 * Fired when a player is punished by AntiCheatReloaded
 */
public final class PlayerPunishEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private final User user;
	private final List<String> actions;

	private boolean cancelled;

	public PlayerPunishEvent(final User user, final List<String> actions) {
		this.user = user;
		this.actions = actions;
	}

	/**
	 * Get the {@link User} who will be
	 * punished
	 * 
	 * @return a {@link User}
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Get the list of actions the punishment will perform
	 *
	 * @return a a list of punishment strings
	 */
	public List<String> getActions() {
		return actions;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

}