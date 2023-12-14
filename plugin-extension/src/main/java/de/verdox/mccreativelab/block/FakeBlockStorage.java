package de.verdox.mccreativelab.block;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import de.verdox.mccreativelab.MCCreativeLabExtension;
import de.verdox.mccreativelab.registry.palette.NBTPalettedContainer;
import de.verdox.mccreativelab.registry.exception.PaletteValueUnknownException;
import de.verdox.mccreativelab.registry.palette.IdMap;
import de.verdox.mccreativelab.util.PaletteUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FakeBlockStorage {
    private static final NamespacedKey FAKE_BLOCK_PALETTE_KEY = new NamespacedKey("mccreativelab", "fake_block_storage");
    private static final NamespacedKey BLOCK_STATE_BLOCK_KEY = new NamespacedKey("mccreativelab", "fake_block_key");
    private static final NamespacedKey BLOCK_STATE_ID = new NamespacedKey("mccreativelab", "block_state_id");
    private static final NamespacedKey ITEM_DISPLAY_LINKING_KEY = new NamespacedKey("mccreativelab", "linked_fake_block_state");

    public static boolean setFakeBlock(Location location, @Nullable FakeBlock fakeBlock, boolean forceLoad) {
        return setFakeBlockState(location, fakeBlock != null ? fakeBlock.getDefaultBlockState() : null, forceLoad);
    }

    public static FakeBlock getFakeBlockOrThrow(Location location, boolean forceLoad) {
        try {
            return getFakeBlock(location, forceLoad);
        } catch (PaletteValueUnknownException e) {
            throw new RuntimeException(e);
        }
    }

    public static FakeBlock getFakeBlock(Location location, boolean forceLoad) throws PaletteValueUnknownException {
        FakeBlock.FakeBlockState fakeBlockState = getFakeBlockState(location, forceLoad);
        if (fakeBlockState == null)
            return null;
        return fakeBlockState.getFakeBlock();
    }

    public static boolean setFakeBlockState(Location location, @Nullable FakeBlock.FakeBlockState fakeBlockState, boolean forceLoad) {
        FakeBlockStorage fakeBlockStorage = MCCreativeLabExtension.getInstance().getFakeBlockStorage();

        int localX = PaletteUtil.worldXToPaletteXCoordinate(location.getBlockX());
        int localY = PaletteUtil.worldYCoordinateToPaletteCoordinate(location.getWorld(), location.getBlockY());
        int localZ = PaletteUtil.worldZToPaletteXCoordinate(location.getBlockZ());

        if (!location.getChunk().isLoaded()) {
            if (forceLoad)
                location.getChunk().load(true);
            else return false;
        }
        FakeBlockPalettedContainer fakeBlockPaletteContainer = fakeBlockStorage.getFakeBlockPalette(location.getChunk());

        // If there is already a fake block at this location -> Remove its display
        FakeBlockUtil.removeFakeDisplayOfCustomBlock(location.getBlock());

        if(fakeBlockState != null){
            ItemDisplay itemDisplay = fakeBlockState.getFakeBlockDisplay().spawnFakeBlock(location);
            PersistentDataContainer nbtSerializedFakeBlockStateData = fakeBlockPaletteContainer.dataToNbt(itemDisplay
                .getPersistentDataContainer().getAdapterContext(), fakeBlockState);

            itemDisplay.getPersistentDataContainer()
                       .set(ITEM_DISPLAY_LINKING_KEY, PersistentDataType.TAG_CONTAINER, nbtSerializedFakeBlockStateData);
            location.getBlock()
                    .setMetadata("fakeBlockItemDisplay", new FixedMetadataValue(MCCreativeLabExtension.getInstance(), itemDisplay.getUniqueId()));
        }
        fakeBlockPaletteContainer.setData(fakeBlockState, localX, localY, localZ);
        return true;
    }

    public static void linkDisplayEntityToFakeBlock(ItemDisplay itemDisplay) {
        Block vanillaBlock = itemDisplay.getLocation().getBlock();
        if (!itemDisplay.getPersistentDataContainer().has(ITEM_DISPLAY_LINKING_KEY))
            return;
        var serializedFakeBlockState = itemDisplay.getPersistentDataContainer()
                                                  .get(ITEM_DISPLAY_LINKING_KEY, PersistentDataType.TAG_CONTAINER);
        if (serializedFakeBlockState == null)
            return;
        FakeBlockPalettedContainer fakeBlockPaletteContainer = MCCreativeLabExtension.getInstance()
                                                                                     .getFakeBlockStorage()
                                                                                     .getFakeBlockPalette(itemDisplay.getChunk());

        FakeBlock.FakeBlockState fakeBlockStateStoredInDisplay = fakeBlockPaletteContainer.nbtToData(serializedFakeBlockState);


        FakeBlock.FakeBlockState fakeBlockStateStoredInBlock = getFakeBlockStateOrThrow(vanillaBlock.getLocation(), false);


        if (fakeBlockStateStoredInBlock != null && fakeBlockStateStoredInBlock.equals(fakeBlockStateStoredInDisplay)) {
            vanillaBlock.setMetadata("fakeBlockItemDisplay", new FixedMetadataValue(MCCreativeLabExtension.getInstance(), itemDisplay.getUniqueId()));
        } else if (fakeBlockStateStoredInDisplay == null && fakeBlockStateStoredInBlock != null) {
            setFakeBlockState(vanillaBlock.getLocation(), fakeBlockStateStoredInBlock, false);
        } else if (fakeBlockStateStoredInBlock == null) {
            Bukkit.getScheduler().runTask(MCCreativeLabExtension.getInstance(), itemDisplay::remove);
        }
    }

    public static void unlinkDisplayEntityAndBlock(ItemDisplay itemDisplay, Block block){
        block.removeMetadata("fakeBlockItemDisplay", MCCreativeLabExtension.getInstance());
        itemDisplay.getPersistentDataContainer().remove(ITEM_DISPLAY_LINKING_KEY);
    }

    @Nullable
    public static ItemDisplay getLinkedDisplayEntity(Block block) {
        if (!block.hasMetadata("fakeBlockItemDisplay"))
            return null;
        UUID entityUUID = UUID.fromString(block.getMetadata("fakeBlockItemDisplay").get(0).asString());
        Entity foundEntity = Bukkit.getEntity(entityUUID);
        if (foundEntity instanceof ItemDisplay itemDisplay)
            return itemDisplay;

        return null;
    }

    public static @Nullable FakeBlock.FakeBlockState getFakeBlockStateOrThrow(Location location, boolean forceLoad) {
        try {
            return getFakeBlockState(location, forceLoad);
        } catch (PaletteValueUnknownException e) {
            throw new RuntimeException(e);
        }
    }

    public static @Nullable FakeBlock.FakeBlockState getFakeBlockState(Location location, boolean forceLoad) throws PaletteValueUnknownException {
        FakeBlockStorage fakeBlockStorage = MCCreativeLabExtension.getInstance().getFakeBlockStorage();

        int localX = PaletteUtil.worldXToPaletteXCoordinate(location.getBlockX());
        int localY = PaletteUtil.worldYCoordinateToPaletteCoordinate(location.getWorld(), location.getBlockY());
        int localZ = PaletteUtil.worldXToPaletteXCoordinate(location.getBlockZ());

        if (!location.getChunk().isLoaded()) {
            if (forceLoad)
                location.getChunk().load(true);
            else return null;
        }

        FakeBlockPalettedContainer fakeBlockPaletteContainer = fakeBlockStorage.getFakeBlockPalette(location.getChunk());
        return fakeBlockPaletteContainer.getData(localX, localY, localZ);
    }

    private final Map<ChunkEntry, FakeBlockPalettedContainer> fakeBlockPalettes = new ConcurrentHashMap<>();

    public FakeBlockStorage() {
    }

    public void saveAll() {
        for (World world : Bukkit.getWorlds()) {
            save(world);
        }
    }

    void save(World world) {
        for (Chunk loadedChunk : world.getLoadedChunks())
            saveChunk(loadedChunk, loadedChunk.getPersistentDataContainer(), false);
    }

    FakeBlockPalettedContainer getFakeBlockPalette(Chunk chunk) {
        ChunkEntry chunkEntry = new ChunkEntry(chunk.getWorld().getName(), chunk.getChunkKey());
        return fakeBlockPalettes.get(chunkEntry);
    }

    FakeBlockPalettedContainer createData(World world, long chunkKey) {
        ChunkEntry chunkEntry = new ChunkEntry(world.getName(), chunkKey);
        FakeBlockPalettedContainer fakeBlockPaletteContainer = new FakeBlockPalettedContainer(CustomBlockRegistry.FAKE_BLOCK_STATE_ID_MAP, 16, PaletteUtil.getMaxYPaletteFromWorldLimits(world), 16);
        fakeBlockPalettes.put(chunkEntry, fakeBlockPaletteContainer);
        return fakeBlockPaletteContainer;
    }

    void loadChunk(Chunk chunk, PersistentDataContainer persistentDataContainer) {
        ChunkEntry chunkEntry = new ChunkEntry(chunk.getWorld().getName(), chunk.getChunkKey());
        FakeBlockPalettedContainer fakeBlockPaletteContainer = new FakeBlockPalettedContainer(CustomBlockRegistry.FAKE_BLOCK_STATE_ID_MAP, 16, PaletteUtil.getMaxYPaletteFromWorldLimits(chunk.getWorld()), 16);

        if (persistentDataContainer.has(FAKE_BLOCK_PALETTE_KEY)) {
            PersistentDataContainer fakeBlockPaletteNBT = persistentDataContainer.get(FAKE_BLOCK_PALETTE_KEY, PersistentDataType.TAG_CONTAINER);
            if (fakeBlockPaletteNBT == null) {
                Bukkit.getLogger()
                      .warning("Corrupt fake block palette found for chunk " + chunk.getX() + " " + chunk.getZ() + " in world " + chunk
                          .getWorld().getName());
            } else
                fakeBlockPaletteContainer.deSerialize(FAKE_BLOCK_PALETTE_KEY, persistentDataContainer);
        }

        fakeBlockPalettes.put(chunkEntry, fakeBlockPaletteContainer);
    }

    void saveChunk(Chunk chunk, PersistentDataContainer persistentDataContainer, boolean unload) {
        ChunkEntry chunkEntry = new ChunkEntry(chunk.getWorld().getName(), chunk.getChunkKey());
        FakeBlockPalettedContainer fakeBlockPaletteContainer = fakeBlockPalettes.get(chunkEntry);
        if (fakeBlockPaletteContainer == null)
            return;
        fakeBlockPaletteContainer.serialize(FAKE_BLOCK_PALETTE_KEY, persistentDataContainer);
        if (unload)
            fakeBlockPalettes.remove(chunkEntry);
    }

    public static class FakeBlockPalettedContainer extends NBTPalettedContainer<FakeBlock.FakeBlockState> {
        public FakeBlockPalettedContainer(IdMap<FakeBlock.FakeBlockState> idMap, int xSize, int ySize, int zSize) {
            super(idMap, xSize, ySize, zSize);
        }

        @Override
        public PersistentDataContainer dataToNbt(PersistentDataAdapterContext adapterContext, FakeBlock.FakeBlockState data) {
            PersistentDataContainer blockStateData = adapterContext.newPersistentDataContainer();

            FakeBlock fakeBlock = data.getFakeBlock();
            int blockStateID = fakeBlock.getBlockStateID(data);
            if (blockStateID == -1)
                return null;
            NamespacedKey blockKey = MCCreativeLabExtension.getCustomBlockRegistry().getKey(fakeBlock);

            blockStateData.set(BLOCK_STATE_BLOCK_KEY, PersistentDataType.STRING, blockKey.asString());
            blockStateData.set(BLOCK_STATE_ID, PersistentDataType.INTEGER, blockStateID);

            return blockStateData;
        }

        @Override
        @Nullable
        public FakeBlock.FakeBlockState nbtToData(PersistentDataContainer persistentDataContainer) {
            String blockKeyAsString = persistentDataContainer.get(BLOCK_STATE_BLOCK_KEY, PersistentDataType.STRING);
            if (blockKeyAsString == null)
                return null;
            if (!persistentDataContainer.has(BLOCK_STATE_ID))
                return null;
            int blockStateSubId = persistentDataContainer.get(BLOCK_STATE_ID, PersistentDataType.INTEGER).intValue();

            NamespacedKey namespacedKey = NamespacedKey.fromString(blockKeyAsString);
            FakeBlock foundFakeBlock = MCCreativeLabExtension.getCustomBlockRegistry().get(namespacedKey);
            if (foundFakeBlock == null)
                return null;
            return foundFakeBlock.getBlockState(blockStateSubId);
        }
    }


    private record ChunkEntry(String worldName, long chunkKey) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkEntry that = (ChunkEntry) o;
            return chunkKey == that.chunkKey && Objects.equals(worldName, that.worldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldName, chunkKey);
        }
    }
}