package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

final class JuniperPlanksBlock extends Block {
    JuniperPlanksBlock() {
        super(Material.WOOD, MapColor.SILVER);
        setHardness(2);
        setResistance(5);
        setSoundType(SoundType.WOOD);
    }
}
