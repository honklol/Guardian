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
package com.honklol.guardian;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.honklol.guardian.command.CommandHandler;
import com.honklol.guardian.manage.AntiCheatManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.honklol.guardian.config.Configuration;
import com.honklol.guardian.event.BlockListener;
import com.honklol.guardian.event.EntityListener;
import com.honklol.guardian.event.InventoryListener;
import com.honklol.guardian.event.PacketListener;
import com.honklol.guardian.event.PlayerListener;
import com.honklol.guardian.event.VehicleListener;
import com.honklol.guardian.util.UpdateManager;
import com.honklol.guardian.util.User;
import com.honklol.guardian.util.VersionLib;

import lombok.Getter;
public final class Guardian extends JavaPlugin {
	public static final String PREFIX = ChatColor.RED + "" + ChatColor.BOLD + "Guardian " + ChatColor.DARK_GRAY + "> "
			+ ChatColor.GRAY;
	public static final List<UUID> MUTE_ENABLED_MODS = new ArrayList<UUID>();

	@Getter
	private static Guardian plugin;
	@Getter
	private static AntiCheatManager manager;
	@Getter
	private static ExecutorService executor;
	@Getter
	private static ProtocolManager protocolManager;
	@Getter
	private static UpdateManager updateManager;
	@Getter
	private static boolean floodgateEnabled;
	private static List<Listener> eventList = new ArrayList<Listener>();
	private static Configuration config;
	private static boolean verbose;

	private double tps = -1;

	@Override
	public void onLoad() {
		plugin = this;

		// Create executor service
		// Determine thread count based on available CPU cores/threads, max of 4
		final int threads = Math.max(Math.min(Runtime.getRuntime().availableProcessors() / 4, 4), 1);
		executor = Executors.newFixedThreadPool(threads);
		{
			Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GRAY + "Pool size is " + threads + " threads");
		}

		// Check for ProtocolLib
		if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
			Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.RED
					+ "ProtocolLib not found! Guardian requires ProtocolLib to work, please download and install it.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
	}

	@Override
	public void onEnable() {
		manager = new AntiCheatManager(this, getLogger());

		eventList.add(new PlayerListener());
		eventList.add(new BlockListener());
		eventList.add(new EntityListener());
		eventList.add(new VehicleListener());
		eventList.add(new InventoryListener());
		// Order is important in some cases, don't screw with these unless
		// needed, especially config
		setupConfig();
		setupEvents();
		setupCommands();
		// Enterprise must come before levels
		setupEnterprise();
		restoreLevels();
		// Setup ProtocolLib hooks
		setupProtocol();

		Bukkit.getConsoleSender()
				.sendMessage(PREFIX + ChatColor.GRAY + "Running Minecraft version " + VersionLib.getVersion() + " "
						+ (VersionLib.isSupported() ? (ChatColor.GREEN + "(supported)")
								: (ChatColor.RED + "(NOT SUPPORTED!)")));
		
		// Check for Floodgate
		if (Bukkit.getPluginManager().getPlugin("Floodgate") != null) {
			floodgateEnabled = true;
			Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.WHITE + "Floodgate support enabled");
		}

		// Create update manager
		updateManager = new UpdateManager();

		// Launch TPS check
		new BukkitRunnable() {
			long second;
			long currentSecond;
			int ticks;

			public void run() {
				second = (System.currentTimeMillis() / 1000L);
				if (currentSecond == second) {
					ticks += 1;
				} else {
					currentSecond = second;
					tps = (tps == 0.0D ? ticks : (tps + ticks) / 2.0D);
					ticks = 1;
				}

				// Check for updates every 12 hours
				if (ticks % 864000 == 0) {
					updateManager.update();
				}
			}
		}.runTaskTimer(this, 40L, 1L);

		// End tests
		verboseLog("Finished loading.");

		// Metrics
		getServer().getScheduler().runTaskLater(this, new Runnable() {
			@Override
			public void run() {
				try {
					final Metrics metrics = new Metrics(Guardian.this, 19498);
					metrics.addCustomChart(new SingleLineChart("cheaters_kicked", new Callable<Integer>() {
						@Override
						public Integer call() throws Exception {
							final int kicked = playersKicked;
							// Reset so we don't keep sending the same value
							playersKicked = 0;
							return kicked;
						}
					}));
					metrics.addCustomChart(new SimplePie("protocollib_version", new Callable<String>() {
						@Override
						public String call() throws Exception {
							return Bukkit.getPluginManager().getPlugin("ProtocolLib").getDescription().getVersion();
						}
					}));
					metrics.addCustomChart(new SimplePie("nms_version", new Callable<String>() {
						@Override
						public String call() throws Exception {
							return VersionLib.getVersion();
						}
					}));
					metrics.addCustomChart(new SimplePie("floodgate_enabled", new Callable<String>() {
						@Override
						public String call() throws Exception {
							return floodgateEnabled ? "Yes" : "No";
						}
					}));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 90L);
	}

	@Override
	public void onDisable() {
		// Cancel all running tasks
		getServer().getScheduler().cancelTasks(this);

		// Shut down executor service
		executor.shutdown();

		// Save user levels
		verboseLog("Saving user levels...");
		if (config != null) {
			config.getLevels().saveLevelsFromUsers(manager.getUserManager().getUsers());
		}

		AntiCheatManager.close();
		cleanup();
	}

	private void setupProtocol() {
		protocolManager = ProtocolLibrary.getProtocolManager();
		protocolManager.addPacketListener(new PacketListener(this));
		verboseLog("Hooked into ProtocolLib");
	}

	private void setupEvents() {
		for (Listener listener : eventList) {
			getServer().getPluginManager().registerEvents(listener, this);
			//verboseLog("Registered events for ".concat(listener.toString().split("@")[0].split(".anticheat.")[1]));
		}
	}

	private void setupCommands() {
		getCommand("anticheat").setExecutor(new CommandHandler());
		verboseLog("Registered commands.");
	}

	private void setupConfig() {
		config = manager.getConfiguration();
		verboseLog("Setup the config.");
	}

	private void setupEnterprise() {
		if (config.getConfig().enterprise.getValue()) {
			if (config.getEnterprise().loggingEnabled.getValue()) {
				config.getEnterprise().database.cleanEvents();
			}
		}
	}

	private void restoreLevels() {
		for (Player player : getServer().getOnlinePlayers()) {
			final UUID uuid = player.getUniqueId();

			final User user = new User(uuid);
			user.setIsWaitingOnLevelSync(true);
			config.getLevels().loadLevelToUser(user);

			manager.getUserManager().addUser(user);
			verboseLog("Data for " + uuid + " loaded");
		}
	}

	public static String getVersion() {
		return manager.getPlugin().getDescription().getVersion();
	}

	private void cleanup() {
		manager = null;
		plugin = null;
		eventList = null;
		config = null;
		protocolManager = null;
		updateManager = null;
		executor = null;
	}

	public static void debugLog(final String string) {
		Bukkit.getScheduler().runTask(getPlugin(), new Runnable() {
			public void run() {
				if (getManager().getConfiguration().getConfig().debugMode.getValue()) {
					manager.debugLog("[DEBUG] " + string);
				}
			}
		});
	}

	public void verboseLog(final String string) {
		if (verbose) {
			getLogger().info(string);
		}
	}

	public void setVerbose(boolean b) {
		verbose = b;
	}
	
	/**
	 * Amount of players kicked since start
	 */
	private int playersKicked = 0;

	public void onPlayerKicked() {
		this.playersKicked++;
	}

	public static void sendToMainThread(final Runnable runnable) {
		Bukkit.getScheduler().runTask(Guardian.getPlugin(), runnable);
	}

	public void sendToStaff(final String message) {
		Bukkit.getOnlinePlayers().forEach(player -> {
			if (player.hasPermission("anticheat.system.alert")) {
				if (!MUTE_ENABLED_MODS.contains(player.getUniqueId())) {
					player.sendMessage(message);
				}
			}
		});
	}

	public double getTPS() {
		return Math.min(Math.max(this.tps, 0.0D), 20.0D);
	}

}
