/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SmokeableItem extends SmokingItem {
    private final int color;
    private final float nicotineSaturation;
    private final DrugInfluence[] influences;
    private boolean freshStacks;
    public SmokeableItem(int uses,int burnTime,int color,DrugInfluence... influences){this(uses,burnTime,color,2,influences);}
    public SmokeableItem(int uses,int burnTime,int color,float nicotineSaturation,DrugInfluence... influences){
        super(uses,25,burnTime);this.color=color;this.nicotineSaturation=nicotineSaturation;this.influences=influences.clone();
    }
    SmokeableItem stackable(){freshStacks=true;return this;}
    @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){return ivorius.psychedelicraft.block.PlacedDrinksBlock.placeOnBlock(context);}
    @Override public int getMaxStackSize(ItemStack stack){return freshStacks&&!stack.isDamaged()?64:1;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(level.isClientSide)return stack;
        if(entity instanceof Player player){
            DrugProperties properties=DrugCapability.get(player);
            applyInfluences(player,properties);properties.startBreathingSmoke(player,color);recordDraw(player,stack);
            if(!player.getAbilities().instabuild&&stack.getCount()>1){
                ItemStack partial=wear(stack.split(1),player);
                if(!partial.isEmpty()&&!player.getInventory().add(partial))player.drop(partial,false);
                return stack;
            }
        }
        return wear(stack,entity);
    }
    public int smokeColor(){return color;}
    public void applyInfluences(Player player,DrugProperties properties){
        for(DrugInfluence influence:influences)properties.inhale(influence.copy(),player,nicotineSaturation);
    }
}
