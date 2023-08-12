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
package com.honklol.guardian.check.movement;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import com.cryptomorin.xseries.XMaterial;
import com.honklol.guardian.AntiCheatReloaded;
import com.honklol.guardian.check.Backend;
import com.honklol.guardian.check.CheckResult;
import com.honklol.guardian.check.CheckType;
import com.honklol.guardian.check.CheckResult.Result;
import com.honklol.guardian.config.providers.Checks;
import com.honklol.guardian.util.Distance;
import com.honklol.guardian.util.Friction;
import com.honklol.guardian.util.MovementManager;
import com.honklol.guardian.util.User;
import com.honklol.guardian.util.Utilities;
import com.honklol.guardian.util.VelocityTracker;
import com.honklol.guardian.util.VersionLib;

/**
 * @author Rammelkast TODO soulsand speed TODO buffer system for all
 */
public final class SpeedCheck {

	public static final Map<UUID, Double> PREDICT_BUFFER = new HashMap<>();
	public static final Map<UUID, Double> AIRSPEED_BUFFER = new HashMap<>();
	public static final Map<UUID, Double> AIRACCELERATION_BUFFER = new HashMap<>();
	
	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);

	private static boolean isSpeedExempt(final Player player, final Backend backend) {
		return backend.isMovingExempt(player) || VersionLib.isFlying(player);
	}
	
	/**
	 * Largely based on Elevated's Frequency speed check
	 * @see https://github.com/ElevatedDev/Frequency/blob/master/src/main/java/xyz/elevated/frequency/check/impl/speed/Speed.java
	 * 
	 * TODO fix falses with entity collisions
	 */
	public static CheckResult checkPredict(final Player player, final Location movingTowards) {
		final Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		if (!checksConfig.isSubcheckEnabled(CheckType.SPEED, "predict")) {
			return PASS;
		}
		
		final Backend backend = AntiCheatReloaded.getManager().getBackend();
		if (isSpeedExempt(player, backend) || player.getVehicle() != null || Utilities.isInWater(player)
				|| Utilities.isInWeb(player)) {
			return PASS;
		}
		
		final User user = AntiCheatReloaded.getManager().getUserManager().getUser(player.getUniqueId());
		final UUID uuid = player.getUniqueId();
		final MovementManager movementManager = user.getMovementManager();
		final VelocityTracker velocityTracker = user.getVelocityTracker();
		
		final double motion = movementManager.distanceXZ;
		final boolean boxedIn = movementManager.topSolid && movementManager.bottomSolid;
		final boolean sprinting = true; // TODO do this using packets
		
		double buffer = PREDICT_BUFFER.getOrDefault(uuid, 0.0);
		double movementSpeed = Utilities.getMovementSpeed(player);
		if (movementManager.wasOnGround) {
			if (sprinting) {
				movementSpeed *= 1.3f;
			}
			
			movementSpeed *= 0.16277136 / Math.pow((movementManager.friction *= 0.91f), 3);
			if (!movementManager.onGround && (movementManager.motionY >= 0.42f || (boxedIn && movementManager.motionY > 0.0f)) && sprinting) {
				movementSpeed += 0.2f;
			}
		} else {
			movementSpeed = sprinting ? 0.026f : 0.02f;
			movementManager.friction = 0.91f;
		}
		
		movementSpeed += velocityTracker.getHorizontal();
		
		final double factor = (motion - movementManager.adjusted) / movementSpeed;
		final boolean exempted = Utilities.isCollisionPoint(movingTowards,
				material -> material == XMaterial.HONEY_BLOCK.parseMaterial());
		if (factor > 1.001 && !exempted) {
			if (buffer++ > 2.5) {
				buffer /= 2;
				PREDICT_BUFFER.put(uuid, buffer);
				return new CheckResult(Result.FAILED, "Predict", "moved unexpectedly (factor=" + Utilities.roundDouble(factor, 3) + ")");
			}
		} else {
			buffer = Math.max(buffer - 0.05, 0.0);
		}
		
		PREDICT_BUFFER.put(uuid, buffer);
		
		movementManager.adjusted = motion * movementManager.friction;
		movementManager.friction = Friction.getFactor(movingTowards.clone().subtract(0, 1, 0).getBlock());
		return PASS;
	}
	
	public static CheckResult checkXZSpeed(final Player player, final Location movingTowards) {
		final Backend backend = AntiCheatReloaded.getManager().getBackend();
		if (isSpeedExempt(player, backend) || player.getVehicle() != null || Utilities.isInWater(player)) {
			return PASS;
		}

		final User user = AntiCheatReloaded.getManager().getUserManager().getUser(player.getUniqueId());
		final UUID uuid = player.getUniqueId();
		final MovementManager movementManager = user.getMovementManager();
		final VelocityTracker velocityTracker = user.getVelocityTracker();

		// Riptiding exemption
		if (movementManager.riptideTicks > 0) {
			return PASS;
		}
		
		final Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		final double distanceXZ = movementManager.distanceXZ;
		final boolean boxedIn = movementManager.topSolid && movementManager.bottomSolid;

		// AirSpeed
		if (checksConfig.isSubcheckEnabled(CheckType.SPEED, "airSpeed") && movementManager.airTicks > 1
				&& movementManager.elytraEffectTicks <= 0 && !Utilities.isNearClimbable(player)) {
			double buffer = AIRSPEED_BUFFER.getOrDefault(uuid, 0.0);
			
			final double multiplier = 0.985D;
			double predict = 0.36 * Math.pow(multiplier, movementManager.airTicks + 1);
			// Prevents false when falling from great heights
			if (movementManager.airTicks >= 115) {
				predict = Math.max(0.08, predict);
			}

			double limit = checksConfig.getDouble(CheckType.SPEED, "airSpeed", "baseLimit"); // Default 0.03125
			// Adjust for ice
			if (movementManager.iceInfluenceTicks > 0) {
				double iceIncrement = 0.025 * Math.pow(1.038, movementManager.iceInfluenceTicks);
				// Clamp to max value
				if (iceIncrement > 0.18D) {
					iceIncrement = 0.18D;
				}

				// Boxed in ice increment
				if (boxedIn) {
					iceIncrement += 0.45D;
				}

				// Ice increment moving off ice
				if (!Utilities.couldBeOnIce(movingTowards)) {
					iceIncrement *= 2.5D;
				}

				predict += iceIncrement;
			}

			// Leniency when boxed in
			if (boxedIn && movementManager.airTicks < 3) {
				predict *= 1.2D;
			}

			// Adjust for slime
			if (movementManager.slimeInfluenceTicks > 0) {
				double slimeIncrement = 0.022 * Math.pow(1.0375, movementManager.slimeInfluenceTicks);
				// Clamp to max value
				if (slimeIncrement > 0.12D) {
					slimeIncrement = 0.12D;
				}

				predict += slimeIncrement;
			}

			// Adjust for soul speed
			final ItemStack boots = player.getInventory().getBoots();
			if (boots != null && movementManager.soilInfluenceTicks > 0 && VersionLib.isSoulSpeed(boots)) {
				predict += boots.getEnchantmentLevel(Enchantment.SOUL_SPEED) * 0.05D;
			}

			// Adjust for speed effects
			if (movementManager.hasSpeedEffect) {
				predict += VersionLib.getPotionLevel(player, PotionEffectType.SPEED) * 0.05D;
			}

			// Adjust for speed effects ending
			if (movementManager.hadSpeedEffect && !movementManager.hasSpeedEffect) {
				limit *= 1.2D;
			}

			// Adjust for jump boost effects
			if (player.hasPotionEffect(PotionEffectType.JUMP)) {
				predict += VersionLib.getPotionLevel(player, PotionEffectType.JUMP) * 0.05D;
			}

			// Adjust for custom walking speed
			final double walkSpeedMultiplier = checksConfig.getDouble(CheckType.SPEED, "airSpeed",
					"walkSpeedMultiplier"); // Default 1.4
			predict += walkSpeedMultiplier * (Math.pow(1.1, ((player.getWalkSpeed() / 0.20) - 1)) - 1);

			// Slabs sometimes allow for a slight boost after jump
			if (movementManager.halfMovementHistoryCounter > 0) {
				predict *= 2.125D;
			}
			if (Utilities.couldBeOnHalfblock(movingTowards) && movementManager.halfMovementHistoryCounter == 0) {
				predict *= 1.25D;
			}

			// Boats sometimes give a false positive
			if (Utilities.couldBeOnBoat(player, 0.5, true)) {
				predict *= 1.25D;
			}

			// Strafing in air when nearing terminal velocity gives false positives
			// This fixes the issue but gives hackers some leniency which means we need
			// another check for this
			final double deltaMotionY = movementManager.motionY - movementManager.lastMotionY;
			if ((deltaMotionY < 0 && deltaMotionY >= -0.08) || movementManager.airTicks >= 40
					&& (movingTowards.getBlockY() - player.getWorld().getHighestBlockYAt(movingTowards)) <= 1.5) {
				predict *= (movementManager.airTicks > 60 ? 4.0D : (movementManager.airTicks > 30 ? 3.0D : 2.0D));
			}

			// Players can move faster in air with slow falling
			if (VersionLib.isSlowFalling(player)) {
				predict *= 1.25D;
			}

			// Prevent NoSlow
			if (movementManager.blockingTicks > 3 && movementManager.airTicks > 2) {
				predict *= 0.8D;
			}
			if (movementManager.blockingTicks > 10 && movementManager.airTicks > 2) {
				predict *= 0.5D;
			}

			// Fixes false positive when coming out of water
			if (movementManager.nearLiquidTicks >= 7 && movementManager.airTicks >= 14
					&& movementManager.motionY < -0.18 && movementManager.motionY > -0.182) {
				predict += Math.abs(movementManager.motionY);
			}

			// Adjust for velocity
			limit += velocityTracker.getHorizontal();

			if (distanceXZ - predict > limit) {
				if (buffer++ > 1.5) {
					buffer /= 2;
					AIRSPEED_BUFFER.put(uuid, buffer);
					return new CheckResult(CheckResult.Result.FAILED, "AirSpeed",
							"moved too fast in air (speed=" + Utilities.roundDouble(distanceXZ, 4) + ", limit="
									+ Utilities.roundDouble(predict, 4) + ", block=" + movementManager.blockingTicks
									+ ", box=" + boxedIn + ", at=" + movementManager.airTicks + ")");
				}
			} else {
				buffer = Math.max(buffer - 0.05, 0.0);
			}
			
			AIRSPEED_BUFFER.put(uuid, buffer);
		}

		// AirAcceleration
		if (checksConfig.isSubcheckEnabled(CheckType.SPEED, "airAcceleration") && movementManager.airTicks > 1
				&& movementManager.iceInfluenceTicks <= 0 && movementManager.slimeInfluenceTicks <= 0
				&& movementManager.elytraEffectTicks <= 0) {
			final double initialAcceleration = movementManager.acceleration;
			double buffer = AIRACCELERATION_BUFFER.getOrDefault(uuid, 0.0);
			double limit = checksConfig.getDouble(CheckType.SPEED, "airAcceleration", "baseLimit"); // Default 0.3725
			// Slight increase when boxed in
			if (boxedIn) {
				limit *= 1.08D;
			}

			// Adjust for soul speed
			final ItemStack boots = player.getInventory().getBoots();
			if (boots != null && movementManager.soilInfluenceTicks > 0 && VersionLib.isSoulSpeed(boots)) {
				limit += boots.getEnchantmentLevel(Enchantment.SOUL_SPEED) * 0.025D;
			}

			// Adjust for speed effects
			if (player.hasPotionEffect(PotionEffectType.SPEED)) {
				limit += VersionLib.getPotionLevel(player, PotionEffectType.SPEED) * 0.0225D;
			}

			// Adjust for slabs
			if (movementManager.halfMovementHistoryCounter > 15) {
				limit *= 2.05D;
			}

			// Adjust for custom walking speed
			final double walkSpeedMultiplier = checksConfig.getDouble(CheckType.SPEED, "airAcceleration",
					"walkSpeedMultiplier"); // Default 1.4
			limit += walkSpeedMultiplier * (Math.pow(1.1, ((player.getWalkSpeed() / 0.20) - 1)) - 1);

			// Boats sometimes give a false positive
			if (Utilities.couldBeOnBoat(player)) {
				limit *= 1.25D;
			}

			// Add velocity
			limit += velocityTracker.getHorizontal();

			if (initialAcceleration > limit) {
				if (buffer++ > 1.5) {
					buffer /= 2;
					AIRACCELERATION_BUFFER.put(uuid, buffer);
					return new CheckResult(CheckResult.Result.FAILED, "AirAcceleration",
							"exceeded acceleration limits (acceleration=" + Utilities.roundDouble(initialAcceleration, 4)
									+ ", max=" + Utilities.roundDouble(limit, 4) + ")");
				}
			} else {
				buffer = Math.max(buffer - 0.05, 0.0);
			}
			
			AIRACCELERATION_BUFFER.put(uuid, buffer);
		}

		// JumpBehaviour
		// Works against YPorts and mini jumps
		if (checksConfig.isSubcheckEnabled(CheckType.SPEED, "jumpBehaviour") && movementManager.touchedGroundThisTick
				&& !boxedIn && movementManager.slimeInfluenceTicks <= 10 && !Utilities.isNearHalfblock(movingTowards)
				&& !Utilities.isNearHalfblock(movingTowards.clone().subtract(0, 0.51, 0))
				&& !Utilities.couldBeOnBoat(player, 0.8, false) && movementManager.elytraEffectTicks <= 0) {
			// This happens naturally
			if (movementManager.airTicksBeforeGrounded == movementManager.groundTicks) {
				final double minimumDistXZ = checksConfig.getDouble(CheckType.SPEED, "jumpBehaviour", "minimumDistXZ"); // Default
				if (distanceXZ >= minimumDistXZ || movementManager.lastDistanceXZ >= minimumDistXZ) {
					return new CheckResult(CheckResult.Result.FAILED, "JumpBehaviour",
							"had unexpected jumping behaviour (dXZ=" + Utilities.roundDouble(distanceXZ, 4) + ", lXZ="
									+ Utilities.roundDouble(movementManager.lastDistanceXZ, 4) + ")");
				}
			}
		}

		// GroundSpeed
		if (checksConfig.isSubcheckEnabled(CheckType.SPEED, "groundSpeed") && movementManager.groundTicks > 1
				&& movementManager.elytraEffectTicks < 10) {
			final double initialLimit = checksConfig.getDouble(CheckType.SPEED, "groundSpeed", "initialLimit"); // Default
																												// 0.34
			double limit = initialLimit - 0.0055 * Math.min(9, movementManager.groundTicks);
			// Leniency when moving back on ground
			if (movementManager.groundTicks < 5) {
				limit += 0.1D;
			}

			// Slab leniency
			if (movementManager.halfMovementHistoryCounter > 8) {
				limit += 0.2D;
			}

			// LivingEntities can give players a small push boost
			if (!movingTowards.getWorld().getNearbyEntities(movingTowards, 0.3, 0.3, 0.3).isEmpty()) {
				limit += 0.2D;
			}

			// Leniency when boxed in
			if (boxedIn) {
				limit *= 1.1D;
			}

			// Adjust for speed effects
			if (movementManager.hasSpeedEffect) {
				limit += VersionLib.getPotionLevel(player, PotionEffectType.SPEED) * 0.06D;
			}
			if (movementManager.hasSpeedEffect && movementManager.groundTicks > 3) {
				limit *= 1.4D;
			}
			if (movementManager.hadSpeedEffect && !movementManager.hasSpeedEffect) {
				limit *= 1.2D;
			}

			// Ice adjustments
			if (movementManager.iceInfluenceTicks >= 50) {
				// When moving off ice
				if (!Utilities.couldBeOnIce(movingTowards)) {
					limit *= 2.5D;
				} else {
					// When boxed in and spamming space for boost
					if (movementManager.topSolid && movementManager.bottomSolid) {
						limit *= 3.0D;
					} else {
						limit *= 1.25D;
					}
				}
			}

			// Increased speed when stepping on/off half blocks
			if (Utilities.isNearBed(movingTowards) || Utilities.couldBeOnHalfblock(movingTowards)
					|| Utilities.isNearBed(movingTowards.clone().add(0, -0.5, 0))) {
				limit *= 2.0D;
			}

			// Increased speed when stepping on/off boat
			if (Utilities.couldBeOnBoat(player)) {
				limit += 0.2D;
			}

			// Adjust for custom walk speed
			limit += (player.getWalkSpeed() - 0.2) * 2.0D;

			// Prevent NoSlow
			if (player.isBlocking() && movementManager.groundTicks > 2) {
				limit *= 0.45D;
			}

			// Prevent NoWeb
			if (Utilities.isInWeb(player)) {
				limit *= 0.65D;
			}

			// Sneak speed check
			if (movementManager.sneakingTicks > 1) {
				limit *= 0.68D;
			}

			// Velocity adjustment
			limit += velocityTracker.getHorizontal();

			if (distanceXZ - limit > 0) {
				return new CheckResult(CheckResult.Result.FAILED, "GroundSpeed",
						"moved too fast on ground (speed=" + Utilities.roundDouble(distanceXZ, 4) + ", limit="
								+ Utilities.roundDouble(limit, 4) + ", blocking=" + player.isBlocking() + ", gt="
								+ movementManager.groundTicks + ")");
			}
		}

		return PASS;
	}

	public static CheckResult checkVerticalSpeed(final Player player, final Distance distance) {
		final Backend backend = AntiCheatReloaded.getManager().getBackend();
		if (isSpeedExempt(player, backend) || player.getVehicle() != null || player.isSleeping()
				|| Utilities.isNearWater(player)) {
			return PASS;
		}

		final User user = AntiCheatReloaded.getManager().getUserManager().getUser(player.getUniqueId());
		final MovementManager movementManager = user.getMovementManager();
		final VelocityTracker velocityTracker = user.getVelocityTracker();
		// Riptiding exemption
		if (movementManager.riptideTicks > 0) {
			return PASS;
		}

		final Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		if (!checksConfig.isSubcheckEnabled(CheckType.SPEED, "verticalSpeed")) {
			return PASS;
		}

		double maxMotionY = getMaxAcceptableMotionY(player, Utilities.isNearBed(distance.getTo()),
				Utilities.couldBeOnBoat(player),
				Utilities.isClimbableBlock(distance.getFrom().getBlock())
						|| Utilities.isClimbableBlock(distance.getFrom().getBlock().getRelative(BlockFace.DOWN)),
				(movementManager.halfMovement || Utilities.isNearWall(distance.getFrom())), checksConfig);
		// Fix false positive with soulsand boost
		if (movementManager.nearLiquidTicks > 6) {
			maxMotionY *= 1.0525D;
		}

		// Adjust for powdered snow
		if (distance.getFrom().getBlock().getType() == XMaterial.POWDER_SNOW.parseMaterial()) {
			maxMotionY += 0.17D;
		}

		// Adjust for velocity
		maxMotionY += velocityTracker.getVertical();

		if (movementManager.motionY > maxMotionY && movementManager.slimeInfluenceTicks <= 0) {
			return new CheckResult(CheckResult.Result.FAILED, "VerticalSpeed",
					"exceeded vertical speed limit (mY=" + Utilities.roundDouble(movementManager.motionY, 4) + ", max="
							+ Utilities.roundDouble(maxMotionY, 4) + ")");
		}
		return PASS;
	}

	private static double getMaxAcceptableMotionY(final Player player, final boolean nearBed,
			final boolean couldBeOnBoat, final boolean fromClimbable, final boolean halfMovement,
			final Checks checksConfig) {
		double base = couldBeOnBoat ? 0.6f : (nearBed ? 0.5625f : ((halfMovement) ? 0.6f : 0.42f));
		if (fromClimbable) {
			base += checksConfig.getDouble(CheckType.SPEED, "verticalSpeed", "climbableCompensation"); // Default 0.04
		}

		if (player.hasPotionEffect(PotionEffectType.JUMP)) {
			base += VersionLib.getPotionLevel(player, PotionEffectType.JUMP) * 0.2D;
		}
		return base;
	}

}
