package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.block.entity.BrushableBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

public final class BrushItem extends Item {
    private static final int USE_TICKS = 200;
    private static final int STROKE_TICKS = 10;

    BrushItem() {
        setMaxStackSize(1);
        setMaxDamage(64);
        addPropertyOverride(new ResourceLocation("brushing"), (stack, world, user) -> user != null
                && user.isHandActive() && user.getActiveItemStack() == stack
                ? (user.getItemInUseMaxCount() % STROKE_TICKS) / (float) STROKE_TICKS : 0);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_TICKS;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing face, float x, float y, float z) {
        if (!player.isSpectator()) player.setActiveHand(hand);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void onUsingTick(ItemStack brush, EntityLivingBase user, int remaining) {
        if (!(user instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) user;
        World world = player.world;
        RayTraceResult hit = rayTrace(world, player, false);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || player.isSpectator()
                || !world.isBlockModifiable(player, hit.getBlockPos())
                || !player.canPlayerEdit(hit.getBlockPos(), hit.sideHit, brush)) {
            player.stopActiveHand();
            return;
        }
        if ((USE_TICKS - remaining + 1) % STROKE_TICKS != 5) return;
        BlockPos pos = hit.getBlockPos();
        TileEntity tile = world.getTileEntity(pos);
        boolean suspicious = tile instanceof BrushableBlockEntity;
        world.playSound(player, pos, suspicious ? PSSounds.BRUSH_SAND : PSSounds.BRUSH_GENERIC,
                SoundCategory.BLOCKS, 1F, 1F);
        if (world.isRemote) {
            dust(player, hit, world.getBlockState(pos));
        } else if (suspicious && ((BrushableBlockEntity) tile).brush(player, hit.sideHit)) {
            ItemStack before = brush.copy();
            brush.damageItem(1, player);
            if (brush.isEmpty()) ForgeEventFactory.onPlayerDestroyItem(player, before, player.getActiveHand());
        }
    }

    private static void dust(EntityPlayer player, RayTraceResult hit, IBlockState state) {
        World world = player.world;
        Vec3d view = player.getLookVec();
        EnumFacing face = hit.sideHit;
        double dx;
        double dz;
        if (face.getAxis().isVertical()) {
            dx = view.z;
            dz = -view.x;
        } else if (face.getAxis() == EnumFacing.Axis.Z) {
            dx = -face.getFrontOffsetZ();
            dz = 0.1D * face.getFrontOffsetZ();
        } else {
            dx = 0.1D * face.getFrontOffsetX();
            dz = face.getFrontOffsetX();
        }
        EnumHandSide arm = player.getPrimaryHand();
        if (player.getActiveHand() == EnumHand.OFF_HAND) arm = arm.opposite();
        double spread = arm == EnumHandSide.RIGHT ? 3 : -3;
        int count = 7 + world.rand.nextInt(5);
        int stateId = Block.getStateId(state);
        for (int i = 0; i < count; i++) {
            world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, hit.hitVec.x, hit.hitVec.y, hit.hitVec.z,
                    dx * spread * world.rand.nextDouble(), 0, dz * spread * world.rand.nextDouble(), stateId);
        }
    }
}
