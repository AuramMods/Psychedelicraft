package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.entity.PSTradeOffers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {
    @Redirect(method="getCostA",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private int psychedelicraft$bulkPaymentLimit(ItemStack stack){
        // Native demand/reputation math otherwise clamps a five-cigar payment to a single cigar.
        return PSTradeOffers.bulkUnstackablePayment(stack)?64:stack.getMaxStackSize();
    }
}
