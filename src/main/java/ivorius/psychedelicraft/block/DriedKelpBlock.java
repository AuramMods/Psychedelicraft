package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

final class DriedKelpBlock extends Block {
    DriedKelpBlock() {
        super(Material.GRASS);
        setHardness(0.5F);
        setSoundType(SoundType.PLANT);
    }
}
