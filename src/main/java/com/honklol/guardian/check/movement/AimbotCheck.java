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
package com.honklol.guardian.check.movement;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.honklol.guardian.Guardian;
import com.honklol.guardian.check.Backend;
import com.honklol.guardian.check.CheckResult;
import com.honklol.guardian.check.CheckType;
import com.honklol.guardian.config.providers.Checks;
import com.honklol.guardian.util.MovementManager;
import com.honklol.guardian.util.User;
import com.honklol.guardian.util.Utilities;

public final class AimbotCheck {

	private static final double EXPANDER = Math.pow(2, 24);
	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);
	
	/**
	 * This falses sometimes?
	 * TODO we need to check yaw deltas
	 */
	public static CheckResult runCheck(final Player player, final EntityDamageByEntityEvent event) {
		final Backend backend = Guardian.getManager().getBackend();
		if (backend.isMovingExempt(player)) {
			return PASS;
		}
		
		final User user = Guardian.getManager().getUserManager()
				.getUser(player.getUniqueId());
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = Guardian.getManager().getConfiguration().getChecks();
		final float deltaPitch = movementManager.deltaPitch;
		final float pitchAcceleration = Math.abs(deltaPitch - movementManager.lastDeltaPitch);

		final long gcd = Utilities.getGcd((long) (deltaPitch * EXPANDER),
				(long) (movementManager.lastDeltaPitch * EXPANDER));
		final double mod = Math.abs(player.getLocation().getPitch() % (gcd / EXPANDER));
		final double minAcceleration = checksConfig.getDouble(CheckType.AIMBOT, "minAcceleration");
		final double maxMod = checksConfig.getDouble(CheckType.AIMBOT, "maxMod");
		if ((gcd > 0L && gcd < 131072L) && mod <= maxMod && pitchAcceleration > minAcceleration && deltaPitch > 5.0f && deltaPitch < 20.0f) {
			return new CheckResult(CheckResult.Result.FAILED,
					"failed computational check (gcd=" + gcd + ", mod="
							+ Utilities.roundDouble(mod, 5) + ", accel=" + Utilities.roundDouble(pitchAcceleration, 3)
							+ ", delta=" + Utilities.roundDouble(deltaPitch, 1) + ")");
		}
		return PASS;
	}

}
