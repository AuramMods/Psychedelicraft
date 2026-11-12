package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.BlockOre;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;

import java.util.Random;

final class CopperOreBlock extends BlockOre {
    CopperOreBlock() {
        setHardness(3);
        setResistance(5);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return PSItems.RAW_COPPER;
    }

    @Override
    public int quantityDropped(Random random) {
        return 2 + random.nextInt(4);
    }
}
