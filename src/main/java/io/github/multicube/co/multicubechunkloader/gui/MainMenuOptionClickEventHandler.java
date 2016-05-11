package io.github.multicube.co.multicubechunkloader.gui;

import io.github.multicube.co.multicubechunkloader.ChunkLoader;
import io.github.multicube.co.multicubechunkloader.ChunkLoaderConfiguration;
import io.github.multicube.co.multicubechunkloader.MulticubeChunkLoader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MainMenuOptionClickEventHandler implements IconMenu.OptionClickEventHandler {
    @Override
    public void onOptionClick(IconMenu.OptionClickEvent event) {
        ChunkLoaderConfiguration config = MulticubeChunkLoader.instance.getConfiguration();

        Map<Integer, Material> type = new HashMap<>(3);
        type.put(3, config.getPersonalLoaderBlock());
        type.put(4, config.getWorldLoaderBlock());
        type.put(5, config.getCreativeLoaderBlock());

        Bukkit.getScheduler().scheduleSyncDelayedTask(MulticubeChunkLoader.instance, new Runnable() {
            private Player player;
            private Material material;

            @Override
            public void run() {
                MulticubeChunkLoader plugin = MulticubeChunkLoader.instance;
                ChunkLoader.ChunkType type = plugin.getChunkTypeFromMaterial(material);
                plugin.getChunkLoaderIconMenu(type).open(player);
            }

            public Runnable init(Player player, Material material) {
                this.player = player;
                this.material = material;
                return (this);
            }
        }.init(event.getPlayer(), type.get(event.getPosition())), 2L);


        event.setWillClose(true);
    }
}
