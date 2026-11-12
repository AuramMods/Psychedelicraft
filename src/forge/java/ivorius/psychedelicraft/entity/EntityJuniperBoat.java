package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    @Nullable
    public EntityItem entityDropItem(ItemStack stack, float offset) {
        // EntityBoat uses vanilla planks when a fall breaks its hull.
        if (stack.getItem() == Item.getItemFromBlock(Blocks.PLANKS)) {
            stack = new ItemStack(PSItems.JUNIPER_PLANKS, stack.getCount());
        }
        return super.entityDropItem(stack, offset);
    }
}
