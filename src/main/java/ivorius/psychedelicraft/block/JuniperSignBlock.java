package ivorius.psychedelicraft.block;

import java.util.Random;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.block.entity.JuniperSignBlockEntity;
import ivorius.psychedelicraft.client.particle.SignWaxParticle;
import ivorius.psychedelicraft.fluid.physical.Waterloggable;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class JuniperSignBlock extends BlockSign implements Waterloggable {
    private static final int WAX_PARTICLES = 1;
    public static final PropertyInteger ROTATION = BlockStandingSign.ROTATION;
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public enum Form {
        STANDING, WALL, HANGING_CEILING, HANGING_WALL;

        public boolean isHanging() {
            return this == HANGING_CEILING || this == HANGING_WALL;
        }

        public int textWidth() {
            return isHanging() ? 60 : 90;
        }

        public int textLineHeight() {
            return isHanging() ? 9 : 10;
        }
    }

    private final Form form;

    JuniperSignBlock(Form form) {
        this.form = form;
        setHardness(1);
        setSoundType(SoundType.WOOD);
    }

    public Form form() {
        return form;
    }

    public abstract float rotationDegrees(IBlockState state);

    public abstract boolean canStay(World world, BlockPos pos, IBlockState state);

    public boolean canAttachForPlacement(World world, BlockPos pos, IBlockState state) {
        return canStay(world, pos, state);
    }

    public boolean facesPlayer(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        AxisAlignedBB bounds = state.getBoundingBox(world, pos);
        double x = player.posX - (pos.getX() + (bounds.minX + bounds.maxX) * 0.5);
        double z = player.posZ - (pos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5);
        double angle = Math.toDegrees(MathHelper.atan2(z, x)) - 90;
        return Math.abs(MathHelper.wrapDegrees(rotationDegrees(state) - angle)) <= 90;
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return form.isHanging() ? MapColor.CYAN : MapColor.CLAY;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new JuniperSignBlockEntity();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean canPistonMove() {
        return false;
    }

    @Override
    public boolean hasOwnWaterTile() {
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return form.isHanging() ? PSItems.JUNIPER_HANGING_SIGN : PSItems.JUNIPER_SIGN;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(getItemDropped(state, world.rand, 0));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (world.isRemote || form == Form.HANGING_WALL) return;

        BlockPos support = form == Form.STANDING ? pos.down() : form == Form.HANGING_CEILING ? pos.up()
                : pos.offset(state.getValue(FACING).getOpposite());
        if (from.equals(support) && !canStay(world, pos, state)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!world.isRemote) {
            JuniperSignBlockEntity sign = JuniperSignBlockEntity.get(world, pos);
            if (sign != null) {
                sign.openQueuedEditor();
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float x, float y, float z) {
        JuniperSignBlockEntity sign = JuniperSignBlockEntity.get(world, pos);
        if (sign == null) {
            return false;
        }
        boolean front = facesPlayer(world, pos, state, player);
        ItemStack held = player.getHeldItem(hand);
        if (Waterlogging.bucketItem(held)) return false;
        if (held.getItem() == PSItems.JUNIPER_HANGING_SIGN && canChainFrom(state, facing)
                && !(sign.isWaxed() && sign.text(front).hasClickCommands())) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        if (!(player instanceof EntityPlayerMP)) {
            return false;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        boolean commandsExecuted = sign.executeClickCommands(serverPlayer, front);
        if (sign.isWaxed()) {
            world.playSound(null, pos, PSSounds.WAXED_SIGN_INTERACT_FAIL, SoundCategory.BLOCKS, 1F, 1F);
            return false;
        }
        if (!sign.canEdit(serverPlayer)) {
            return commandsExecuted;
        }
        SoundEvent treatment = applyTreatment(sign, front, held);
        if (treatment != null) {
            player.addStat(StatList.getObjectUseStats(held.getItem()));
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            world.playSound(null, pos, treatment, SoundCategory.BLOCKS, 1F, 1F);
            if (treatment == PSSounds.SIGN_WAX) world.addBlockEvent(pos, this, WAX_PARTICLES, 0);
            return true;
        }
        return commandsExecuted || sign.openEditor(serverPlayer, front);
    }

    @Override
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        if (id != WAX_PARTICLES) return super.eventReceived(state, world, pos, id, param);
        if (world.isRemote) SignWaxParticle.spawn(world, pos);
        return true;
    }

    private boolean canChainFrom(IBlockState state, EnumFacing side) {
        return form == Form.HANGING_CEILING && side == EnumFacing.DOWN
                || form == Form.HANGING_WALL && side.getAxis() != state.getValue(FACING).getAxis();
    }

    @Nullable
    private static SoundEvent applyTreatment(JuniperSignBlockEntity sign, boolean front, ItemStack held) {
        Item item = held.getItem();
        if (item == PSItems.SIGN_WAX || item == PSItems.HONEYCOMB) {
            return sign.wax() ? PSSounds.SIGN_WAX : null;
        }
        if (!sign.text(front).hasText()) return null;
        if (item == PSItems.GLOWING_INK) {
            return sign.setGlowing(front, true) ? PSSounds.SIGN_GLOW_INK : null;
        }
        if (item == PSItems.SIGN_BLACK_DYE) {
            return sign.setColor(front, EnumDyeColor.BLACK) ? PSSounds.SIGN_DYE : null;
        }
        if (item == Items.DYE) {
            EnumDyeColor color = EnumDyeColor.byDyeDamage(held.getMetadata());
            if (color == EnumDyeColor.BLACK) return sign.setGlowing(front, false) ? PSSounds.SIGN_INK : null;
            return sign.setColor(front, color) ? PSSounds.SIGN_DYE : null;
        }
        return null;
    }

    static abstract class Rotating extends JuniperSignBlock {
        Rotating(Form form) {
            super(form);
            setDefaultState(blockState.getBaseState().withProperty(ROTATION, 0));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, ROTATION);
        }

        @Override
        public IBlockState getStateFromMeta(int metadata) {
            return getDefaultState().withProperty(ROTATION, metadata & 15);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(ROTATION);
        }

        @Override
        public IBlockState withRotation(IBlockState state, Rotation rotation) {
            return state.withProperty(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
        }

        @Override
        public IBlockState withMirror(IBlockState state, Mirror mirror) {
            return state.withProperty(ROTATION, mirror.mirrorRotation(state.getValue(ROTATION), 16));
        }

        @Override
        public float rotationDegrees(IBlockState state) {
            return state.getValue(ROTATION) * 22.5F;
        }
    }

    static final class Standing extends Rotating {
        Standing() {
            super(Form.STANDING);
        }

        @Override
        public boolean canStay(World world, BlockPos pos, IBlockState state) {
            return world.getBlockState(pos.down()).getMaterial().isSolid();
        }
    }

    static abstract class Horizontal extends JuniperSignBlock {
        Horizontal(Form form) {
            super(form);
            setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING);
        }

        @Override
        public IBlockState getStateFromMeta(int metadata) {
            EnumFacing facing = EnumFacing.getFront(metadata);
            return getDefaultState().withProperty(FACING, facing.getAxis().isHorizontal() ? facing : EnumFacing.NORTH);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(FACING).getIndex();
        }

        @Override
        public IBlockState withRotation(IBlockState state, Rotation rotation) {
            return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        public IBlockState withMirror(IBlockState state, Mirror mirror) {
            return withRotation(state, mirror.toRotation(state.getValue(FACING)));
        }

        @Override
        public float rotationDegrees(IBlockState state) {
            return state.getValue(FACING).getHorizontalAngle();
        }
    }

    static final class Wall extends Horizontal {
        private static final AxisAlignedBB NORTH = new AxisAlignedBB(0, 0.28125, 0.875, 1, 0.78125, 1);
        private static final AxisAlignedBB SOUTH = new AxisAlignedBB(0, 0.28125, 0, 1, 0.78125, 0.125);
        private static final AxisAlignedBB WEST = new AxisAlignedBB(0.875, 0.28125, 0, 1, 0.78125, 1);
        private static final AxisAlignedBB EAST = new AxisAlignedBB(0, 0.28125, 0, 0.125, 0.78125, 1);

        Wall() {
            super(Form.WALL);
        }

        @Override
        public boolean canStay(World world, BlockPos pos, IBlockState state) {
            return world.getBlockState(pos.offset(state.getValue(FACING).getOpposite())).getMaterial().isSolid();
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
            switch (state.getValue(FACING)) {
                case SOUTH: return SOUTH;
                case WEST: return WEST;
                case EAST: return EAST;
                default: return NORTH;
            }
        }
    }
}
