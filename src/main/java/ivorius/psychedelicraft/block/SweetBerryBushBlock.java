package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.entity.BeeEntity;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.Random;

public final class SweetBerryBushBlock extends AgePlantBlock {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 3);
    private static final AxisAlignedBB[] SHAPES = {shape(5, 8), shape(7, 16), shape(7, 16), FULL_BLOCK_AABB};

    SweetBerryBushBlock() {
        super(3);
        setSoundType(new SoundType(1F, 1F, PSSounds.BERRY_BUSH_BREAK, SoundType.PLANT.getStepSound(),
                PSSounds.BERRY_BUSH_PLACE, SoundType.PLANT.getHitSound(), SoundType.PLANT.getFallSound()));
    }

    @Override
    protected PropertyInteger ageProperty() {
        return AGE;
    }

    @Override
    protected AxisAlignedBB[] shapes() {
        return SHAPES;
    }

    @Override
    protected int growthInterval() {
        return 5;
    }

    @Override
    protected boolean hasGrowthLight(World world, BlockPos pos) {
        return world.getLightFromNeighbors(pos.up()) >= 9;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                && canBlockStay(world, pos, getDefaultState());
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        Block soil = world.getBlockState(pos.down()).getBlock();
        return soil == Blocks.GRASS || soil == Blocks.DIRT || soil == Blocks.MYCELIUM || soil == Blocks.FARMLAND;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing face, float x, float y, float z) {
        int age = state.getValue(AGE);
        ItemStack held = player.getHeldItem(hand);
        boolean bonemeal = held.getItem() == Items.DYE && held.getMetadata() == EnumDyeColor.WHITE.getDyeDamage();
        if (age < 2 || (age < maxAge() && bonemeal)) return false;
        if (!world.isRemote) {
            spawnAsEntity(world, pos, new ItemStack(PSItems.SWEET_BERRIES, quantityDropped(state, 0, world.rand)));
            world.setBlockState(pos, state.withProperty(AGE, 1), 2);
            world.playSound(null, pos, PSSounds.BERRY_BUSH_PICK, SoundCategory.BLOCKS,
                    1F, 0.8F + world.rand.nextFloat() * 0.4F);
        }
        return true;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!(entity instanceof EntityLivingBase) || entity instanceof BeeEntity) return;
        slow(entity);
        if (!world.isRemote && state.getValue(AGE) > 0) {
            double movedX = Math.abs(entity.posX - entity.lastTickPosX);
            double movedZ = Math.abs(entity.posZ - entity.lastTickPosZ);
            if (Math.max(movedX, movedZ) >= 0.003F) entity.attackEntityFrom(PSDamageTypes.SWEET_BERRY_BUSH, 1F);
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return PSItems.SWEET_BERRIES;
    }

    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        int age = state.getValue(AGE);
        return age < 2 ? 0 : age - 1 + random.nextInt(2) + (fortune > 0 ? random.nextInt(fortune + 1) : 0);
    }

    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (!BlockDrops.usesDecay(world, chance)) {
            super.dropBlockAsItemWithChance(world, pos, state, chance, fortune);
            return;
        }
        NonNullList<ItemStack> drops = NonNullList.create();
        getDrops(drops, world, pos, state, fortune);
        for (ItemStack stack : drops) BlockDrops.decay(stack, world.rand, chance);
        BlockDrops.spawn(drops, world, pos, state, fortune, harvesters.get());
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return new ItemStack(PSItems.SWEET_BERRIES);
    }
}
