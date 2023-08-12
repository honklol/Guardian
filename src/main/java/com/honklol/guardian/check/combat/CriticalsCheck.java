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
package com.honklol.guardian.check.combat;

import com.honklol.guardian.event.EventListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;

import com.honklol.guardian.check.CheckResult;
import com.honklol.guardian.check.CheckType;
import com.honklol.guardian.util.Utilities;

public final class CriticalsCheck {

	public static void doDamageEvent(final EntityDamageByEntityEvent event, final Player damager) {
		if (!(event.getDamager() instanceof Player) || event.getCause() != DamageCause.ENTITY_ATTACK) {
			return;
		}
		final Player player = (Player) event.getDamager();
		if (isCritical(player)) {
			if ((player.getLocation().getY() % 1.0 == 0 || player.getLocation().getY() % 0.5 == 0)
					&& player.getLocation().clone().subtract(0, 1.0, 0).getBlock().getType().isSolid()) {
				event.setCancelled(true);
				EventListener.log(
						new CheckResult(CheckResult.Result.FAILED, "tried to do a critical without needed conditions")
								.getMessage(),
						player, CheckType.CRITICALS, null);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static boolean isCritical(final Player player) {
		return player.getFallDistance() > 0.0f && !player.isOnGround() && !player.isInsideVehicle()
				&& !player.hasPotionEffect(PotionEffectType.BLINDNESS)
				&& !Utilities.isHoveringOverWater(player.getLocation())
				&& player.getEyeLocation().getBlock().getType() != Material.LADDER;
	}

}
