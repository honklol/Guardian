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

package com.honklol.guardian.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import com.honklol.guardian.Guardian;

public class VehicleListener extends EventListener {

	@EventHandler(ignoreCancelled = true)
	public void onVehicleEnter(VehicleEnterEvent event) {
		if (event.getEntered() instanceof Player) {
			getBackend().logEnterExit((Player) event.getEntered());
		}

		Guardian.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler(ignoreCancelled = true)
	public void onVehicleExit(VehicleExitEvent event) {
		if (event.getExited() instanceof Player) {
			getBackend().logEnterExit((Player) event.getExited());
		}

		Guardian.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler(ignoreCancelled = true)
	public void onVehicleDestroy(VehicleDestroyEvent event) {
		if (!event.getVehicle().getPassengers().isEmpty()) {
			for (Entity entity : event.getVehicle().getPassengers()) {
				if (entity instanceof Player) {
					getBackend().logEnterExit((Player) entity);
				}
			}
		}
		Guardian.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}
}
