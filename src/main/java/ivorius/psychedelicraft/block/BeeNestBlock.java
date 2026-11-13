package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Random;

public final class BeeNestBlock extends Block {
    public static final int FULL_HONEY = 5;
    public static final PropertyInteger HONEY_LEVEL = PropertyInteger.create("honey_level", 0, FULL_HONEY);

    BeeNestBlock() {
        super(Material.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(HONEY_LEVEL, 0));
        setHardness(0.3F);
        setSoundType(SoundType.WOOD);
        setTickRandomly(true);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HONEY_LEVEL);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HONEY_LEVEL, meta >= 0 && meta <= FULL_HONEY ? meta : 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HONEY_LEVEL);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return state.getValue(HONEY_LEVEL);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        int level = state.getValue(HONEY_LEVEL);
        if (level < FULL_HONEY && world.getWorldTime() % 24000L < 12000L
                && !world.isRaining() && hasFlowers(world, pos)) {
            world.setBlockState(pos, state.withProperty(HONEY_LEVEL, level + 1), 3);
        }
    }

    private static boolean hasFlowers(World world, BlockPos pos) {
        BlockPos.MutableBlockPos flower = new BlockPos.MutableBlockPos();
        int bottom = Math.max(0, pos.getY() - 6);
        int top = Math.min(world.getHeight() - 1, pos.getY() + 2);
        for (int x = pos.getX() - 4; x <= pos.getX() + 4; x++) {
            for (int z = pos.getZ() - 4; z <= pos.getZ() + 4; z++) {
                if (!world.isBlockLoaded(flower.setPos(x, pos.getY(), z))) {
                    continue;
                }
                for (int y = bottom; y <= top; y++) {
                    IBlockState state = world.getBlockState(flower.setPos(x, y, z));
                    Block block = state.getBlock();
                    if (block instanceof BlockFlower || (block instanceof BlockDoublePlant
                            && state.getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.LOWER
                            && state.getValue(BlockDoublePlant.VARIANT) != BlockDoublePlant.EnumPlantType.GRASS
                            && state.getValue(BlockDoublePlant.VARIANT) != BlockDoublePlant.EnumPlantType.FERN)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        ItemStack held = player.getHeldItem(hand);
        Item item = held.getItem();
        if (state.getValue(HONEY_LEVEL) != FULL_HONEY || (item != Items.SHEARS && item != Items.GLASS_BOTTLE)) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        if (!world.setBlockState(pos, state.withProperty(HONEY_LEVEL, 0), 3)) {
            return false;
        }
        if (item == Items.SHEARS) {
            spawnAsEntity(world, pos, new ItemStack(PSItems.HONEYCOMB, 3));
            held.damageItem(1, player);
            world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1, 1);
        } else {
            ItemStack bottle = FluidVesselHandler.forCrafting(held).craftContents(PSFluids.HONEY, -1, null);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            if (held.isEmpty()) {
                player.setHeldItem(hand, bottle);
            } else {
                ItemHandlerHelper.giveItemToPlayer(player, bottle);
            }
            world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1, 1);
        }
        return true;
    }
}
