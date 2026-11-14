package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class WitherRoseBlock extends SmallFlowerBlock {
    @Override
    protected boolean canSustainBush(IBlockState state) {
        Block soil = state.getBlock();
        return super.canSustainBush(state) || soil == Blocks.NETHERRACK || soil == Blocks.SOUL_SAND
                || soil == PSBlocks.SOUL_SOIL;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!world.isRemote && world.getDifficulty() != EnumDifficulty.PEACEFUL && entity instanceof EntityLivingBase
                && !(entity instanceof EntityWitherSkeleton) && !entity.isEntityInvulnerable(DamageSource.WITHER)) {
            ((EntityLivingBase) entity).addPotionEffect(new PotionEffect(MobEffects.WITHER, 40));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        AxisAlignedBB shape = getBoundingBox(state, world, pos);
        Vec3d center = shape.getCenter();
        for (int attempt = 0; attempt < 3; attempt++) {
            if (random.nextBoolean()) {
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                        pos.getX() + center.x + random.nextDouble() / 5,
                        pos.getY() + 0.5 - random.nextDouble(),
                        pos.getZ() + center.z + random.nextDouble() / 5, 0, 0, 0);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void witherKill(LivingDeathEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        World world = victim.world;
        if (world.isRemote) return;
        Entity attacker = victim.getAttackingEntity();
        if (attacker == null) attacker = event.getSource().getTrueSource();
        if (!(attacker instanceof EntityWither)) return;

        BlockPos pos = victim.getPosition();
        WitherRoseBlock rose = PSBlocks.WITHER_ROSE;
        IBlockState state = rose.getDefaultState();
        if (ForgeEventFactory.getMobGriefingEvent(world, attacker) && world.isAirBlock(pos)
                && rose.canBlockStay(world, pos, state) && world.setBlockState(pos, state, 3)) return;
        victim.entityDropItem(new ItemStack(PSItems.WITHER_ROSE), 0);
    }
}
