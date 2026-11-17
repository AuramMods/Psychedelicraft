package ivorius.psychedelicraft.mixin;

import ivorius.psychedelicraft.item.MolotovCocktailItem;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native empty-slot quick move ignores item limits; the donor recipe makes four unstackable bottles. */
@Mixin(AbstractContainerMenu.class)
public abstract class MolotovMenuTransferMixin {
    @Shadow protected abstract boolean moveItemStackTo(ItemStack stack,int start,int end,boolean reverse);

    @Inject(method="moveItemStackTo",at=@At("HEAD"),cancellable=true)
    private void psychedelicraft$splitMolotovResult(ItemStack stack,int start,int end,boolean reverse,
                                                   CallbackInfoReturnable<Boolean> callback){
        if(!(stack.getItem() instanceof MolotovCocktailItem)||stack.getCount()<=1)return;
        boolean moved=false;
        while(!stack.isEmpty()){
            var single=stack.copyWithCount(1);
            // A single item follows native slot validation, ordering and transfer rules without reentering this branch.
            if(!moveItemStackTo(single,start,end,reverse))break;
            if(!single.isEmpty())break;
            stack.shrink(1);
            moved=true;
        }
        callback.setReturnValue(moved);
    }
}
