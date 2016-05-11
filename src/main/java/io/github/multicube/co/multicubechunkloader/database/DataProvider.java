package io.github.multicube.co.multicubechunkloader.database;

import io.github.multicube.co.multicubechunkloader.ChunkLoader;
import io.github.multicube.co.multicubechunkloader.MulticubeChunkLoader;
import io.github.multicube.co.multicubechunkloader.PlayerInfo;

import java.util.List;
import java.util.UUID;

public abstract class DataProvider {
    public MulticubeChunkLoader plugin;

    public DataProvider(MulticubeChunkLoader plugin) {
        this.plugin = plugin;

    }

    public abstract List<ChunkLoader> getWorldChunks();

    public abstract List<ChunkLoader> getPersonalChunks(UUID uuid);

    public abstract List<ChunkLoader> getPersonalChunks(String world);

    public abstract List<ChunkLoader> getPersonalChunks();

    public abstract ChunkLoader getChunkLoader(int id);

    public abstract PlayerInfo getPlayerInfo(UUID uuid);

    public abstract UUID getUUID(String name);

    public abstract void saveChunkLoader(ChunkLoader chunkLoader);

    public abstract ChunkLoader getChunkLoaderAtLocation(String locationString);

    public abstract void deleteChunkLoader(ChunkLoader chunkLoader);

    public abstract void updatePlayerInfo(PlayerInfo playerInfo);
}
