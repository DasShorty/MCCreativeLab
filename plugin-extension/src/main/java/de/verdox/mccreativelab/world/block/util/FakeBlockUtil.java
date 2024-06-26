package de.verdox.mccreativelab.world.block.util;

import de.verdox.mccreativelab.MCCreativeLabExtension;
import de.verdox.mccreativelab.world.block.FakeBlock;
import de.verdox.mccreativelab.world.block.FakeBlockSoundManager;
import de.verdox.mccreativelab.world.block.FakeBlockStorage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class FakeBlockUtil {
    public static void simulateBlockBreakWithParticlesAndSound(@Nullable FakeBlock.FakeBlockState fakeBlockState, Block block) {
        BlockData destroyParticleBlockData = block.getBlockData();

        if(fakeBlockState != null && fakeBlockState.getFakeBlockDisplay().getDestroyParticleData() != null)
            destroyParticleBlockData = fakeBlockState.getFakeBlockDisplay().getDestroyParticleData();


        //if(fakeBlockState != null && fakeBlockState.getFakeBlockSoundGroup() != null || FakeBlockSoundManager.isBlockWithoutStandardSound(block))
            //FakeBlockSoundManager.simulateBreakSound(block, fakeBlockState);

        block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, destroyParticleBlockData);
        //block.getWorld().spawnParticle(Particle.BLOCK_CRACK, block.getLocation().clone().add(0.5, 0.5, 0.5), 40, 0.1, 0.1, 0.1, destroyParticleBlockData);
    }

    public static boolean playerNotInEffectRange(Player onlinePlayer, Block block) {
        if (!onlinePlayer.getWorld().equals(block.getWorld()))
            return true;

        double xDistance = (double) block.getX() - onlinePlayer.getX();
        double yDistance = (double) block.getY() - onlinePlayer.getY();
        double zDistance = (double) block.getZ() - onlinePlayer.getZ();

        return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance >= 1024.0D;
    }

    public static void spawnDiggingParticles(Player player, Block block, FakeBlock.FakeBlockState fakeBlockState, Vector normalOfBlockFace) {
        float xOffset = 0;
        float yOffset = 0;
        float zOffset = 0;

        float xPos = 0;
        float yPos = 0;
        float zPos = 0;

        float offset = 0.15f;
        float pos = 0.5f;

        if (normalOfBlockFace.getBlockX() != 0) {
            yOffset = offset;
            zOffset = offset;

            xPos = pos * normalOfBlockFace.getBlockX();
        } else if (normalOfBlockFace.getBlockY() != 0) {
            xOffset = offset;
            zOffset = offset;

            yPos = pos * normalOfBlockFace.getBlockY();
        } else if (normalOfBlockFace.getBlockZ() != 0) {
            xOffset = offset;
            yOffset = offset;

            zPos = pos * normalOfBlockFace.getBlockZ();
        }

        player.spawnParticle(Particle.BLOCK,
                 block.getLocation().clone()
                      .add(0.5, 0.5, 0.5)
                      .add(xPos, yPos, zPos)
                      .add(normalOfBlockFace.multiply(0.05)), 1, xOffset, yOffset, zOffset, 0.01, fakeBlockState
                     .getFakeBlockDisplay().getDestroyParticleData());
    }

    public static void removeFakeBlockIfPossible(Block block) {
        FakeBlock.FakeBlockState fakeBlockState = FakeBlockStorage.getFakeBlockState(block.getLocation(), false);
        if (fakeBlockState == null)
            return;
        FakeBlockStorage.setFakeBlockState(block.getLocation(), null, false);
    }
}
