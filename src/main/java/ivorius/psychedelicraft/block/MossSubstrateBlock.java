package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

final class MossSubstrateBlock extends Block {
    MossSubstrateBlock() {
        super(Material.GRASS, MapColor.GREEN);
        setHardness(0.1F);
        setSoundType(SoundType.PLANT);
        setHarvestLevel("hoe", 0);
    }
}
