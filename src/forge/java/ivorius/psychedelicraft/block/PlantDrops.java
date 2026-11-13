package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

final class PlantDrops {
    private PlantDrops() {
    }

    static boolean usesDecay(World world, float chance) {
        return chance < 1F && !world.isRemote && !world.restoringBlockSnapshots;
    }

    static boolean survives(Random random, float chance) {
        return chance >= 1F || random.nextFloat() <= chance;
    }

    static void decay(ItemStack stack, Random random, float chance) {
        if (chance >= 1F) return;
        int survivors = 0;
        for (int item = 0; item < stack.getCount(); item++) {
            if (survives(random, chance)) survivors++;
        }
        stack.setCount(survivors);
    }

    static void spawn(List<ItemStack> drops, World world, BlockPos pos, IBlockState state,
                      int fortune, @Nullable EntityPlayer harvester) {
        drops.removeIf(ItemStack::isEmpty);
        // Apply the block's loot functions first, then let Forge listeners adjust the resulting drops.
        float chance = ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, fortune, 1F, false, harvester);
        for (ItemStack stack : drops) {
            if (survives(world.rand, chance)) Block.spawnAsEntity(world, pos, stack);
        }
    }
}
