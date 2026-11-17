package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native empty-slot quick move ignores item limits; mod recipes and trades can produce multiple unstackable items. */
@Mixin(AbstractContainerMenu.class)
public abstract class ModMenuTransferMixin {
    @Shadow @Final public NonNullList<Slot> slots;
    @Shadow protected abstract boolean moveItemStackTo(ItemStack stack,int start,int end,boolean reverse);

    @Inject(method="moveItemStackTo",at=@At("HEAD"),cancellable=true)
    private void psychedelicraft$splitUnstackableResult(ItemStack stack,int start,int end,boolean reverse,
                                                   CallbackInfoReturnable<Boolean> callback){
        if(stack.getMaxStackSize()!=1||stack.getCount()<=1||!ForgeRegistries.ITEMS.getKey(stack.getItem()).getNamespace().equals(Psychedelicraft.MOD_ID))return;
        // Result-slot callbacks spend the recipe/trade once. Refuse a partial transfer before changing either inventory.
        int capacity=0;var single=stack.copyWithCount(1);
        for(int index=start;index<end;index++){
            var slot=slots.get(index);
            if(!slot.hasItem()&&slot.mayPlace(single)&&slot.getMaxStackSize(single)>=1)capacity++;
        }
        if((Object)this instanceof net.minecraft.world.inventory.MerchantMenu&&capacity<stack.getCount()){callback.setReturnValue(false);return;}
        boolean moved=false;
        while(!stack.isEmpty()){
            single=stack.copyWithCount(1);
            // A single item follows native slot validation, ordering and transfer rules without reentering this branch.
            if(!moveItemStackTo(single,start,end,reverse))break;
            if(!single.isEmpty())break;
            stack.shrink(1);
            moved=true;
        }
        callback.setReturnValue(moved);
    }
}
