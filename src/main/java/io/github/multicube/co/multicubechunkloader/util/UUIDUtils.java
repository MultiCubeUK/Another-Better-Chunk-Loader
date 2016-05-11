package io.github.multicube.co.multicubechunkloader.util;

import io.github.multicube.co.multicubechunkloader.MulticubeChunkLoader;
import io.github.multicube.co.multicubechunkloader.PlayerInfo;
import net.kaikk.mc.uuidprovider.UUIDProvider;
import org.bukkit.OfflinePlayer;
import java.util.*;


public class UUIDUtils 
{
    public static UUID getUUID(String name) 
	{
        if (MulticubeChunkLoader.instance.getServer().getPluginManager().getPlugin("UUIDProvider") != null)
           return UUIDProvider.get(name);

        UUID uuid = MulticubeChunkLoader.instance.getUUID(name);

        if (uuid == null)
		{
            try
            {
                Map<String, UUID> maps = new UUIDFetcher(Collections.singletonList(name)).call();
                for (String key : maps.keySet())
                    if (key.equalsIgnoreCase(name))
                        uuid = maps.get(key);
            } catch (Exception ex)
            {
                MulticubeChunkLoader.instance.getLogger().severe(ex.getMessage());
                return null;
            }
        }

        return uuid;
    }

    public static String getName(UUID uuid) {
        if (MulticubeChunkLoader.instance.getServer().getPluginManager().getPlugin("UUIDProvider") != null)
            return UUIDProvider.get(uuid);

        String name = null;

       PlayerInfo pi = MulticubeChunkLoader.instance.getPlayerInfo(uuid);
        if (pi != null)
            name = pi.getName();

        if (name == null) {
            try {
                Map<UUID, String> maps = new NameFetcher(Collections.singletonList(uuid)).call();
                name = maps.get(uuid);
            } catch (Exception ex) {
                return null;
            }
        }

        return name;
    }

    public static OfflinePlayer getPlayer(UUID uuid) {
        if (MulticubeChunkLoader.instance.getServer().getPluginManager().getPlugin("UUIDProvider") != null)
            return MulticubeChunkLoader.instance.getServer().getOfflinePlayer(UUIDProvider.get(uuid));

        OfflinePlayer player = MulticubeChunkLoader.instance.getServer().getOfflinePlayer(uuid);

        if (player == null || player.getName() == null) {
            try {
                String name = new NameFetcher(Collections.singletonList(uuid))
                        .call()
                        .get(uuid);

                if (name != null) {
                    player = MulticubeChunkLoader.instance.getServer().getOfflinePlayer(name);
                }
            } catch (Exception ex) {
                MulticubeChunkLoader.instance.getLogger().severe(ex.getMessage());
            }
        }

        return player;
    }
}
