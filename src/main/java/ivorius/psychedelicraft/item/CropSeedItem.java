package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.block.BlockBush;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;

final class CropSeedItem extends ItemBlock implements IPlantable {
    private final BlockBush crop;
    private final String name;

    CropSeedItem(BlockBush crop, String name) {
        super(crop);
        this.crop = crop;
        this.name = "item." + Psychedelicraft.MOD_ID + "." + name;
    }

    @Override
    public String getUnlocalizedName() {
        return name;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return name;
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
        return crop.getPlantType(world, pos);
    }

    @Override
    public IBlockState getPlant(IBlockAccess world, BlockPos pos) {
        return crop.getDefaultState();
    }
}
