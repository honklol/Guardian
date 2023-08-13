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
package com.honklol.guardian.check.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.honklol.guardian.Guardian;
import com.honklol.guardian.config.providers.Checks;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import com.google.common.collect.EvictingQueue;
import com.honklol.guardian.check.CheckResult;
import com.honklol.guardian.check.CheckResult.Result;
import com.honklol.guardian.check.CheckType;
import com.honklol.guardian.util.MinecraftVersion;
import com.honklol.guardian.util.MovementManager;
import com.honklol.guardian.util.User;
import com.honklol.guardian.util.Utilities;
import com.honklol.guardian.util.VersionLib;

public final class KillAuraCheck {

	// Angle check
	public static final Map<UUID, Integer> ANGLE_FLAGS = new HashMap<UUID, Integer>();

	// PacketOrder check
	public static final Map<UUID, Integer> PACKETORDER_FLAGS = new HashMap<UUID, Integer>();

	// ThroughWalls check
	public static final Map<UUID, Integer> THROUGHWALLS_FLAGS = new HashMap<UUID, Integer>();
	private static final double RAY_LENGTH = 4.5;

	// Variance check
	public static final Map<UUID, EvictingQueue<Float>> FACTOR_MAP = new HashMap<UUID, EvictingQueue<Float>>();

	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);

	public static CheckResult checkReach(final Player player, final Entity target) {
		if (!(target instanceof LivingEntity) || player.getVehicle() != null || target.getVehicle() != null) {
			return PASS;
		}

		final User user = Guardian.getManager().getUserManager().getUser(player.getUniqueId());
		final Checks checksConfig = Guardian.getManager().getConfiguration().getChecks();

		// Check if enabled
		if (!checksConfig.isSubcheckEnabled(CheckType.KILLAURA, "reach")) {
			return PASS;
		}

		double allowedReach = target.getVelocity().length() < 0.08
				? checksConfig.getDouble(CheckType.KILLAURA, "reach", "baseMaxValue.normal")
				: checksConfig.getDouble(CheckType.KILLAURA, "reach", "baseMaxValue.velocitized");
		if (player.getGameMode() == GameMode.CREATIVE) {
			allowedReach += 1.5D;
		}
		// Lag compensation
		final double lagExtraReach = checksConfig.getDouble(CheckType.KILLAURA, "reach",
				"lagCompensation.lagExtraReach");
		final double pingCompensation = checksConfig.getDouble(CheckType.KILLAURA, "reach",
				"lagCompensation.pingCompensation");
		allowedReach += user.getPing() * pingCompensation;
		if (user.isLagging()) {
			allowedReach += lagExtraReach;
		}
		if (target instanceof Player) {
			final User targetUser = Guardian.getManager().getUserManager().getUser(target.getUniqueId());
			allowedReach += targetUser.getPing() * pingCompensation;
			if (targetUser.isLagging()) {
				allowedReach += lagExtraReach;
			}
		}
		// Velocity compensation
		final double velocityMultiplier = checksConfig.getDouble(CheckType.KILLAURA, "reach", "velocityMultiplier");
		allowedReach += Math.abs(target.getVelocity().length()) * velocityMultiplier;
		final double reachedDistance = Utilities.roundDouble(
				((LivingEntity) target).getLocation().toVector().distance(player.getLocation().toVector()), 2);
		if (reachedDistance > Utilities.roundDouble(allowedReach, 2)) {
			return new CheckResult(CheckResult.Result.FAILED, "Reach", "reached too far (distance=" + reachedDistance
					+ ", max=" + Utilities.roundDouble(allowedReach, 2) + ")");
		}
		return PASS;
	}

	public static CheckResult checkAngle(final Player player, final EntityDamageEvent event) {
		final UUID uuid = player.getUniqueId();
		final Entity entity = event.getEntity();

		// Do not check while in vehicles
		if (player.getVehicle() != null || entity.getVehicle() != null || VersionLib.isRiptiding(player)) {
			return PASS;
		}

		final Checks checksConfig = Guardian.getManager().getConfiguration().getChecks();

		// Check if enabled
		if (!checksConfig.isSubcheckEnabled(CheckType.KILLAURA, "angle")) {
			return PASS;
		}

		if (entity instanceof LivingEntity) {
			final LivingEntity living = (LivingEntity) entity;
			final Location eyeLocation = player.getEyeLocation();

			final double yawDifference = calculateYawDifference(eyeLocation, living.getLocation());
			final double playerYaw = player.getEyeLocation().getYaw();

			final double angleDifference = Math.abs(180 - Math.abs(Math.abs(yawDifference - playerYaw) - 180));
			final int maxDifference = checksConfig.getInteger(CheckType.KILLAURA, "angle", "maxDifference");
			if (Math.round(angleDifference) > maxDifference) {
				if (!ANGLE_FLAGS.containsKey(uuid)) {
					ANGLE_FLAGS.put(uuid, 1);
					return PASS;
				}

				int flags = ANGLE_FLAGS.get(uuid);
				int vlBeforeFlag = checksConfig.getInteger(CheckType.KILLAURA, "angle", "vlBeforeFlag");
				if (flags >= vlBeforeFlag) {
					ANGLE_FLAGS.remove(uuid);
					return new CheckResult(CheckResult.Result.FAILED, "Angle",
							"tried to attack from an illegal angle (angle=" + Math.round(angleDifference) + ")");
				}

				ANGLE_FLAGS.put(uuid, flags + 1);
			}
		}
		return PASS;
	}

	public static CheckResult checkPacketOrder(final Player player, final Entity entity) {
		final UUID uuid = player.getUniqueId();
		final User user = Guardian.getManager().getUserManager().getUser(uuid);
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = Guardian.getManager().getConfiguration().getChecks();

		// Check if enabled
		if (!checksConfig.isSubcheckEnabled(CheckType.KILLAURA, "packetOrder")) {
			return PASS;
		}

		if (user.isLagging() || (System.currentTimeMillis() - movementManager.lastTeleport) <= 100 || Guardian
				.getPlugin().getTPS() < checksConfig.getDouble(CheckType.KILLAURA, "packetOrder", "minimumTps")) {
			return PASS;
		}

		final long elapsed = System.currentTimeMillis() - movementManager.lastUpdate;
		if (elapsed < checksConfig.getInteger(CheckType.KILLAURA, "packetOrder", "minElapsedTime")) {
			if (!PACKETORDER_FLAGS.containsKey(uuid)) {
				PACKETORDER_FLAGS.put(uuid, 1);
				return PASS;
			}

			int flags = PACKETORDER_FLAGS.get(uuid);
			int vlBeforeFlag = checksConfig.getInteger(CheckType.KILLAURA, "packetOrder", "vlBeforeFlag");
			if (flags >= vlBeforeFlag) {
				PACKETORDER_FLAGS.remove(uuid);
				return new CheckResult(Result.FAILED, "PacketOrder",
						"suspicious packet order (elapsed=" + elapsed + ")");
			}

			PACKETORDER_FLAGS.put(uuid, flags + 1);
		}
		return PASS;
	}

	public static CheckResult checkVariance(final Player player) {
		final UUID uuid = player.getUniqueId();
		final User user = Guardian.getManager().getUserManager().getUser(uuid);
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = Guardian.getManager().getConfiguration().getChecks();

		// Check if enabled
		if (!checksConfig.isSubcheckEnabled(CheckType.KILLAURA, "variance")) {
			return PASS;
		}

		final int samples = checksConfig.getInteger(CheckType.KILLAURA, "variance", "samples");
		if (movementManager.deltaPitch > 0.0f && movementManager.lastDeltaPitch > 0.0f
				&& movementManager.deltaYaw > 5.0f) {
			final float factor = movementManager.deltaPitch % movementManager.lastDeltaPitch;
			final EvictingQueue<Float> queue = FACTOR_MAP.getOrDefault(uuid, EvictingQueue.create(samples));
			queue.add(factor);
			if (queue.size() >= samples) {
				final double variance = Utilities.getVariance(queue);
				if (variance < 0.25) {
					FACTOR_MAP.put(uuid, queue);
					return new CheckResult(Result.FAILED, "Variance",
							"had suspicous aim behaviour (variance: " + Utilities.roundDouble(variance, 3) + ")");
				}
			}
			FACTOR_MAP.put(uuid, queue);
		}

		return PASS;
	}

	public static CheckResult checkRepeatedAim(final Player player) {
		final UUID uuid = player.getUniqueId();
		final User user = Guardian.getManager().getUserManager().getUser(uuid);
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = Guardian.getManager().getConfiguration().getChecks();

		// Check if enabled
		if (!checksConfig.isSubcheckEnabled(CheckType.KILLAURA, "repeatedAim")) {
			return PASS;
		}

		final float deltaYaw = movementManager.deltaYaw;
		final float lastDeltaYaw = movementManager.lastDeltaYaw;
		final float deltaPitch = movementManager.deltaPitch;
		final float lastDeltaPitch = movementManager.lastDeltaPitch;
		if (deltaYaw > 0.0f && Math.abs(deltaYaw - lastDeltaYaw) < 1e-5 && deltaPitch > 10.0f) {
			return new CheckResult(Result.FAILED, "RepeatedAim",
					"repeated aim pattern (type=yaw, deltaPitch=" + Utilities.roundFloat(deltaPitch, 1) + ")");
		} else if (deltaPitch > 0.0f && Math.abs(deltaPitch - lastDeltaPitch) < 1e-5 && deltaYaw > 12.0f) {
			return new CheckResult(Result.FAILED, "RepeatedAim",
					"repeated aim pattern (type=pitch, deltaYaw=" + Utilities.roundFloat(deltaYaw, 1) + ")");
		}
		return PASS;
	}

	public static CheckResult checkThroughWalls(final Player player, final Entity target) {
		// TODO this does not work on 1.12.2
		// Either we finally drop 1.12.2 as well, or we create a workaround
		// For now we just skip if server version is 1.12.2
		if (MinecraftVersion.getCurrentVersion().getMinor() <= 12) {
			return PASS;
		}
			
		final UUID uuid = player.getUniqueId();
		final User user = Guardian.getManager().getUserManager().getUser(uuid);
		final MovementManager movementManager = user.getMovementManager();
		final Checks checksConfig = Guardian.getManager().getConfiguration().getChecks();

		// Check if enabled
		if (!checksConfig.isSubcheckEnabled(CheckType.KILLAURA, "throughWalls")) {
			return PASS;
		}

		if ((user.isLagging() && checksConfig.getBoolean(CheckType.KILLAURA, "throughWalls", "disableForLagging"))
				|| (System.currentTimeMillis() - movementManager.lastTeleport) <= 100 || Guardian.getPlugin()
						.getTPS() < checksConfig.getDouble(CheckType.KILLAURA, "throughWalls", "minimumTps")) {
			return PASS;
		}

		if (Math.abs(player.getLocation().getPitch()) > 30.0f) {
			return PASS;
		}

		final Location eyes = player.getEyeLocation().clone();
		final World world = player.getWorld();
		final double expansion = checksConfig.getDouble(CheckType.KILLAURA, "throughWalls", "expand");
		final RayTraceResult result = world.rayTrace(eyes, eyes.getDirection(), RAY_LENGTH, FluidCollisionMode.NEVER,
				true, expansion, entity -> entity.equals(target));
		if (player.getLocation().toVector().clone().setY(0.0)
				.distance(target.getLocation().toVector().clone().setY(0)) <= expansion) {
			return PASS;
		}

		if (result != null) {
			final Block block = result.getHitBlock();
			final Entity entity = result.getHitEntity();
			final double distance = result.getHitPosition().distance(eyes.toVector());
			if (entity == null && block != null && distance > 0.3) {
				final int flags = THROUGHWALLS_FLAGS.getOrDefault(uuid, 0) + 1;
				if (flags > checksConfig.getInteger(CheckType.KILLAURA, "throughWalls", "vlBeforeFlag")) {
					THROUGHWALLS_FLAGS.put(uuid, 0);
					return new CheckResult(Result.FAILED, "ThroughWalls",
							"hit through object (distance: " + Utilities.roundDouble(distance, 2) + ", block: "
									+ block.getType().name().toLowerCase() + ", pitch: "
									+ Utilities.roundFloat(player.getLocation().getPitch(), 1) + ")");
				}
				THROUGHWALLS_FLAGS.put(uuid, flags);
			} else {
				final int flags = THROUGHWALLS_FLAGS.getOrDefault(uuid, 1) - 1;
				THROUGHWALLS_FLAGS.put(uuid, flags);
			}
		} else {
			// TODO should be covered by angle & reach check, but maybe continue anyway?
		}
		return PASS;
	}

	public static double calculateYawDifference(final Location from, final Location to) {
		final Location clonedFrom = from.clone();
		final Vector startVector = clonedFrom.toVector();
		final Vector targetVector = to.toVector();
		clonedFrom.setDirection(targetVector.subtract(startVector));
		return clonedFrom.getYaw();
	}

}
