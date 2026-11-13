/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.function.Supplier;

import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class NightshadeBlock extends AgePlantBlock {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 7);
    private static final AxisAlignedBB[] SHAPES = {
            shape(1, 2), shape(1.5, 4), shape(2, 6), shape(3, 9),
            shape(6, 10), shape(6, 14), shape(7, 15), shape(7, 15)
    };

    private final Supplier<Item> fruit;
    private final Supplier<Item> leaf;

    NightshadeBlock(Supplier<Item> fruit, Supplier<Item> leaf) {
        super(7);
        this.fruit = fruit;
        this.leaf = leaf;
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
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (entity instanceof EntityLivingBase && state.getValue(AGE) >= 3) {
            slow(entity);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing face, float x, float y, float z) {
        int age = state.getValue(AGE);
        ItemStack tool = player.getHeldItem(hand);
        boolean shearing = isShears(tool) && age >= 1;
        boolean fertilizing = tool.getItem() == Items.DYE && tool.getMetadata() == EnumDyeColor.WHITE.getDyeDamage()
                && age == maxAge();
        if (shearing || fertilizing) {
            harvest(world, pos, state, player, tool, (age == maxAge() ? fruit : leaf).get(), shearing);
        }
        // A ripe plant harvested with bonemeal lets the item regrow the just-harvested stage.
        return shearing;
    }
}
