package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.entity.PSTradeOffers;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @Redirect(method="moveFromInventoryToPaymentSlot",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private int psychedelicraft$collectBulkPayment(ItemStack stack){
        // Native matching still requires identical item/data. Only the internal merchant payment slot can collect this batch.
        return PSTradeOffers.bulkUnstackablePayment(stack)?64:stack.getMaxStackSize();
    }
}
