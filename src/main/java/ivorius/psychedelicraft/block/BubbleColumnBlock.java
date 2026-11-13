package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public final class BubbleColumnBlock extends BlockStaticLiquid {
    public static final PropertyBool DRAG_DOWN = PropertyBool.create("drag");

    BubbleColumnBlock() {
        super(Material.WATER);
        setDefaultState(getDefaultState().withProperty(DRAG_DOWN, true));
        setLightOpacity(3);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LEVEL, DRAG_DOWN);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(DRAG_DOWN, (meta & 1) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(DRAG_DOWN) ? 1 : 0;
    }

    public static boolean isColumn(IBlockState state) {
        return state.getBlock() instanceof BubbleColumnBlock;
    }

    public static boolean isSupport(Block block) {
        return block == Blocks.SOUL_SAND || block == Blocks.MAGMA;
    }

    private static boolean canForm(IBlockState state) {
        Block block = state.getBlock();
        return isColumn(state) || (block == Blocks.WATER || block == Blocks.FLOWING_WATER) && state.getValue(LEVEL) == 0;
    }

    public static void onWaterChanged(World world, BlockPos pos, IBlockState previous, IBlockState state) {
        if (world.isRemote) return;
        Block block = state.getBlock();
        if (!isSupport(block) && !isSupport(previous.getBlock()) && !canForm(previous) && !canForm(state)) return;
        BlockPos above = pos.up();
        IBlockState aboveState = world.getBlockState(above);
        if (isColumn(aboveState)) world.scheduleUpdate(above, aboveState.getBlock(), 5);
        if (isSupport(block) && canForm(aboveState)) world.scheduleUpdate(pos, block, 20);
        if (!canForm(state) || isColumn(state)) return;
        BlockPos below = pos.down();
        Block support = world.getBlockState(below).getBlock();
        if (isSupport(support)) world.scheduleUpdate(below, support, 20);
        else if (support instanceof BubbleColumnBlock) world.scheduleUpdate(below, support, 5);
    }

    public static void updateAbove(World world, BlockPos support) {
        updateColumn(world, support.up());
    }

    private static void updateColumn(World world, BlockPos first) {
        IBlockState below = world.getBlockState(first.down());
        IBlockState next = Blocks.FLOWING_WATER.getDefaultState();
        if (isColumn(below)) next = below;
        else if (isSupport(below.getBlock())) {
            next = PSBlocks.BUBBLE_COLUMN.getDefaultState().withProperty(DRAG_DOWN, below.getBlock() == Blocks.MAGMA);
        }
        for (BlockPos pos = first; pos.getY() < world.getHeight() && canForm(world.getBlockState(pos)); pos = pos.up()) {
            IBlockState current = world.getBlockState(pos);
            if (current != next && (isColumn(current) || isColumn(next))) world.setBlockState(pos, next, 2);
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) world.scheduleUpdate(pos, this, 5);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (!world.isRemote) world.scheduleUpdate(pos, this, 5);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        updateColumn(world, pos);
        if (isColumn(world.getBlockState(pos))) {
            Blocks.FLOWING_WATER.updateTick(world, pos, Blocks.FLOWING_WATER.getDefaultState(), random);
        }
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (entity instanceof EntityBoat) return;
        boolean surface = world.isAirBlock(pos.up());
        moveEntity(entity, state.getValue(DRAG_DOWN), surface);
        if (surface) splash(world, pos);
    }

    public static boolean needsAir(EntityLivingBase entity, Material material) {
        if (!entity.isInsideOfMaterial(material)) return false;
        return !isColumn(entity.world.getBlockState(new BlockPos(entity.getPositionEyes(1))));
    }

    public static void moveEntity(Entity entity, boolean down, boolean surface) {
        if (down) entity.motionY = Math.max(surface ? -0.9 : -0.3, entity.motionY - 0.03);
        else entity.motionY = Math.min(surface ? 1.8 : 0.7, entity.motionY + (surface ? 0.1 : 0.06));
        if (!surface) entity.fallDistance = 0;
    }

    public static void splash(World world, BlockPos pos) {
        if (!(world instanceof WorldServer)) return;
        WorldServer server = (WorldServer) world;
        for (int i = 0; i < 2; i++) {
            server.spawnParticle(EnumParticleTypes.WATER_SPLASH, pos.getX() + world.rand.nextDouble(),
                    pos.getY() + 1, pos.getZ() + world.rand.nextDouble(), 1, 0, 0, 0, 1.0);
            server.spawnParticle(EnumParticleTypes.WATER_BUBBLE, pos.getX() + world.rand.nextDouble(),
                    pos.getY() + 1, pos.getZ() + world.rand.nextDouble(), 1, 0, 0.01, 0, 0.2);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        boolean down = state.getValue(DRAG_DOWN);
        world.spawnParticle(down ? EnumParticleTypes.WATER_WAKE : EnumParticleTypes.WATER_BUBBLE,
                pos.getX() + 0.5, pos.getY() + (down ? 0.8 : 0.01), pos.getZ() + 0.5,
                0, down ? -0.03 : 0.2, 0);
        if (!down) world.spawnParticle(EnumParticleTypes.WATER_BUBBLE,
                pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(),
                0, 0.2, 0);
        if (random.nextInt(200) == 0) {
            world.playSound(pos.getX(), pos.getY(), pos.getZ(), down ? SoundEvents.BLOCK_WATER_AMBIENT
                    : SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 0.2F + random.nextFloat() * 0.2F,
                    0.9F + random.nextFloat() * 0.15F, false);
        }
    }
}
