package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class EntityJuniperBoat extends EntityBoat {
    public EntityJuniperBoat(World world) {
        super(world);
    }

    @Override
    public Item getItemBoat() {
        return PSItems.JUNIPER_BOAT;
    }

    @Override
    public float getEyeHeight() {
        return height;
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return getPassengers().size() < passengerLimit() && !hasWaterAtEyes();
    }

    private boolean hasWaterAtEyes() {
        double eyeY = posY + getEyeHeight() - (double) (1F / 9F);
        BlockPos eye = new BlockPos(posX, eyeY, posZ);
        IBlockState fluid = Waterlogging.fluidState(world, eye);
        return Waterlogging.isWater(world, eye, fluid)
                && eyeY < eye.getY() + Waterlogging.liquidShapeHeight(world, eye, fluid);
    }

    protected int passengerLimit() {
        return 2;
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        return canFitPassenger(player) && super.processInitialInteract(player, hand);
    }

    @Override
    @Nullable
    public EntityItem entityDropItem(ItemStack stack, float offset) {
        // EntityBoat uses vanilla planks when a fall breaks its hull.
        if (stack.getItem() == Item.getItemFromBlock(Blocks.PLANKS)) {
            stack = new ItemStack(PSItems.JUNIPER_PLANKS, stack.getCount());
        }
        return super.entityDropItem(stack, offset);
    }
}
