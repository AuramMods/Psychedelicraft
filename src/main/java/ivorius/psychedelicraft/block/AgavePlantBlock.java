/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.Set;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class AgavePlantBlock extends SucculentPlantBlock {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 5);
    private static final AxisAlignedBB[] SHAPES = {
            shape(2, 4), shape(2, 8), shape(3, 10), shape(3, 10), shape(4, 12), shape(6, 14)
    };

    AgavePlantBlock() {
        super(5);
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
        return 320;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (entity instanceof EntityLivingBase) {
            slow(entity);
            if (!world.isRemote && state.getValue(AGE) > 0 && !entity.isSneaking()) {
                double movedX = Math.abs(entity.posX - entity.lastTickPosX);
                double movedZ = Math.abs(entity.posZ - entity.lastTickPosZ);
                if (Math.max(movedX, movedZ) >= 0.003F) {
                    entity.attackEntityFrom(DamageSource.CACTUS, 1);
                }
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing face, float x, float y, float z) {
        ItemStack tool = player.getHeldItem(hand);
        if (isShears(tool) && state.getValue(AGE) > 0) {
            harvest(world, pos, state, player, tool, PSItems.AGAVE_LEAF, true);
            return true;
        }
        if (tool.isEmpty()) {
            if (!world.isRemote) {
                player.attackEntityFrom(DamageSource.CACTUS, 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (!world.isRemote && !isAppropriateTool(player.getHeldItemMainhand())) {
            player.attackEntityFrom(DamageSource.CACTUS, 1);
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    private static boolean isAppropriateTool(ItemStack stack) {
        Set<String> classes = stack.getItem().getToolClasses(stack);
        return isShears(stack) || stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemHoe
                || classes.contains("pickaxe") || classes.contains("shovel") || classes.contains("axe")
                || classes.contains("sword") || classes.contains("hoe");
    }
}
