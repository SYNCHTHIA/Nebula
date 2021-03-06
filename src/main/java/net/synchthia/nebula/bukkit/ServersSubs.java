package net.synchthia.nebula.bukkit;

import net.synchthia.api.nebula.NebulaProtos;
import net.synchthia.nebula.bukkit.server.ServerAPI;
import net.synchthia.nebula.bukkit.util.StringUtil;
import net.synchthia.nebula.client.APIClient;
import redis.clients.jedis.JedisPubSub;

import java.util.logging.Level;

/**
 * @author Laica-Lunasys
 */
public class ServersSubs extends JedisPubSub {
    private static final NebulaPlugin plugin = NebulaPlugin.getPlugin();

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        NebulaProtos.ServerEntryStream serverStream = APIClient.serverEntryStreamFromJson(message);
        assert serverStream != null;
        switch (serverStream.getType()) {
            case SYNC:
                NebulaProtos.ServerEntry entry = serverStream.getEntry();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    ServerAPI.putServer(entry);
                    if (entry.getName().equals(NebulaPlugin.getServerId()) && entry.getLockdown().getEnabled()) {
                        plugin.getServer().getOnlinePlayers().forEach(player -> {
                            if (!player.hasPermission("nebula.server." + entry.getName())) {
                                player.kickPlayer(StringUtil.coloring(entry.getLockdown().getDescription()));
                            }
                        });
                    }
                    plugin.getServerSignManager().updateSigns();
                });
                break;
            case REMOVE:
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    ServerAPI.removeServer(serverStream.getEntry().getName());
                    plugin.getServerSignManager().updateSigns();
                });
                break;
        }
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        plugin.getLogger().log(Level.INFO, "P Subscribed : " + pattern);
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        plugin.getLogger().log(Level.INFO, "P UN Subscribed : " + pattern);
    }
}
