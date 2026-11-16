package ivorius.psychedelicraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;

/** Vanilla named seed behavior plus Forge's planting contract for farming integrations. */
public final class CropSeedItem extends ItemNameBlockItem implements IPlantable {
    public CropSeedItem(Block block) { super(block,new Properties()); }
    @Override public BlockState getPlant(BlockGetter world,BlockPos position) { return getBlock().defaultBlockState(); }
    @Override public PlantType getPlantType(BlockGetter world,BlockPos position) {
        return ((IPlantable)getBlock()).getPlantType(world,position);
    }
}
