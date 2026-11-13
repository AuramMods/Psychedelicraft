package ivorius.psychedelicraft.block;

import java.util.Random;

import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.fluid.physical.Waterloggable;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

abstract class JuniperSlabBlock extends BlockSlab {
    private static final PropertyEnum<Wood> VARIANT = PropertyEnum.create("variant", Wood.class);

    JuniperSlabBlock() {
        super(Material.WOOD, MapColor.SILVER);
        setHardness(2);
        setResistance(5);
        setSoundType(SoundType.WOOD);
        setDefaultState(getStateFromMeta(0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return isDouble() ? new BlockStateContainer(this, VARIANT) : new BlockStateContainer(this, HALF, VARIANT);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState state = blockState.getBaseState();
        return isDouble() ? state : state.withProperty(HALF, (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return !isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0;
    }

    @Override
    public String getUnlocalizedName(int meta) {
        return getUnlocalizedName();
    }

    @Override
    public IProperty<?> getVariantProperty() {
        return VARIANT;
    }

    @Override
    public Comparable<?> getTypeForItem(ItemStack stack) {
        return Wood.JUNIPER;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return PSItems.JUNIPER_SLAB;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(PSItems.JUNIPER_SLAB);
    }

    private enum Wood implements IStringSerializable {
        JUNIPER;

        @Override
        public String getName() {
            return "juniper";
        }
    }

    static final class Single extends JuniperSlabBlock implements Waterloggable {
        @Override
        public boolean isDouble() {
            return false;
        }
    }

    static final class Double extends JuniperSlabBlock {
        @Override
        public boolean isDouble() {
            return true;
        }
    }
}
