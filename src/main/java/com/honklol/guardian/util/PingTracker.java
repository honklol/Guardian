package com.honklol.guardian.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class tracks the ping (latency) of players on the server.
 */
public class PingTracker {
    private static PingTracker instance;
    private final Plugin plugin;
    private final Map<UUID, Integer> playerPings;
    private final Map<UUID, Long> timings;

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
        this.playerPings = new HashMap<>();
        this.timings = new HashMap<>();

        setupPingListeners();
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
                timings.put(player.getUniqueId(), serverSentTime);
            }
        });

        // Listener for client's keepAlive response
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.KEEP_ALIVE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                long clientReceivedTime = System.currentTimeMillis();
                Long serverSentTime = timings.get(player.getUniqueId());
                if (serverSentTime != null) {
                    int ping = (int) (clientReceivedTime - serverSentTime);
                    playerPings.put(player.getUniqueId(), ping);
                    timings.remove(player.getUniqueId());
                }
            }
        });
    }

    /**
     * Gets the ping of a specific player.
     *
     * @param player the player whose ping is to be retrieved
     * @return the player's calculated ping, or 50 if it's not available yet
     */
    public int getPlayerPing(Player player) {
        return playerPings.getOrDefault(player.getUniqueId(), -1);
    }
}