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

package com.honklol.guardian.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class tracks the ping (latency) of players on the server.
 */
public class PingTracker {
    private static PingTracker instance;
    private final Plugin plugin;
    private final Map<UUID, Integer> playerPingKeepalive;
    private final Map<UUID, Integer> playerPingPacket;
    private final Map<UUID, Long> timingsKeepalive;
    private final Map<UUID, Long> timingsPing;

    public static void setInstance(PingTracker pingTracker) {
        instance = pingTracker;
    }

    public static PingTracker getInstance() {
        return instance;
    }

    /**
     * Constructor to initialize the PingTracker with the given plugin.
     *
     * @param plugin the plugin instance
     */
    public PingTracker(Plugin plugin) {
        this.plugin = plugin;
        this.playerPingKeepalive = new HashMap<>();
        this.playerPingPacket = new HashMap<>();
        this.timingsKeepalive = new HashMap<>();
        this.timingsPing = new HashMap<>();

        setupPingListeners();
        startPingTask(1);
    }

    /**
     * Sends a Ping packet to the specified player to measure latency.
     *
     * @param player the player to send the Ping packet to
     */
    public void sendPingPacket(Player player) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer pingPacket = protocolManager.createPacket(PacketType.Play.Server.PING);

        try {
            protocolManager.sendServerPacket(player, pingPacket);
            timingsPing.put(player.getUniqueId(), System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a BukkitRunnable that sends Ping packets to all online players periodically.
     *
     * @param intervalInSeconds the interval in seconds between each Ping packet
     */
    public void startPingTask(int intervalInSeconds) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sendPingPacket(player);
                }
            }
        }.runTaskTimer(plugin, 0L, intervalInSeconds * 20L); // Convert seconds to ticks
    }

    /**
     * Sets up the necessary listeners to track player pings.
     */
    private void setupPingListeners() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        // Listener for server's keepAlive packet
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.KEEP_ALIVE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                long serverSentTime = System.currentTimeMillis();
                timingsKeepalive.put(player.getUniqueId(), serverSentTime);
            }
        });

        // Listener for client's keepAlive response
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.KEEP_ALIVE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                long clientReceivedTime = System.currentTimeMillis();
                Long serverSentTime = timingsKeepalive.get(player.getUniqueId());
                if (serverSentTime != null) {
                    int ping = (int) (clientReceivedTime - serverSentTime);
                    playerPingKeepalive.put(player.getUniqueId(), ping);
                    timingsKeepalive.remove(player.getUniqueId());
                }
            }
        });

        // Listener for client's Pong packet
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.PONG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                Long serverSentTime = timingsPing.get(player.getUniqueId());
                if (serverSentTime != null) {
                    int ping = (int) (System.currentTimeMillis() - serverSentTime);
                    playerPingPacket.put(player.getUniqueId(), ping);
                    timingsPing.remove(player.getUniqueId());
                }
            }
        });
    }

    /**
     * Gets the ping of a specific player.
     *
     * @param player the player whose ping is to be retrieved
     * @return the player's calculated ping, or -1 if it's not available
     */
    public int getPlayerPing(Player player) {
        Integer pingKeepalive = playerPingKeepalive.get(player.getUniqueId());
        Integer pingPacket = playerPingPacket.get(player.getUniqueId());

        // Prefer ping packet over keepalive
        if (pingPacket != null) {
            return pingPacket;
        }

        // Fallback to keepalive packet
        if (pingKeepalive != null) {
            return pingKeepalive;
        }

        // No data available, return -1 as latency
        return -1;
    }

    public String getCurrentMethod(Player player) {
        Integer pingKeepalive = playerPingKeepalive.get(player.getUniqueId());
        Integer pingPacket = playerPingPacket.get(player.getUniqueId());

        // Prefer ping packet over keepalive
        if (pingPacket != null) {
            return "(ping)";
        }

        // Fallback to keepalive packet
        if (pingKeepalive != null) {
            return "(keepalive)";
        }

        // No data available, return -1 as latency
        return "(unknown)";
    }
}
