package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public enum FluidVessel {
    MUG(500,250,32),CUP(250,250,32),CHALICE(200,200,32),SHOT_GLASS(40,40,8),BOTTLE(2000,250,32),SYRINGE(10,10,32),
    BOWL(50,50,32),GLASS_BOTTLE(125,125,32),BUCKET(1000,0,0);
    public final int capacity,serving,useTicks;
    FluidVessel(int capacity,int serving,int useTicks){this.capacity=capacity;this.serving=serving;this.useTicks=useTicks;}
    public boolean accepts(Fluid supplied){
        Fluid fluid=PSFluids.canonical(supplied);
        if(this==SYRINGE)return PSFluids.isInjectable(fluid);if(this==BUCKET)return true;
        if(fluid==Fluids.LAVA)return this==GLASS_BOTTLE;if(PSFluids.isInjectable(fluid))return false;
        if(fluid==PSFluids.COFFEE.get())return this==CUP;if(PSFluids.brew(fluid)!=null)return this!=CUP;
        return this!=MUG||(fluid!=PSFluids.COCA_TEA.get()&&fluid!=PSFluids.CANNABIS_TEA.get()&&fluid!=PSFluids.PEYOTE_JUICE.get()&&fluid!=PSFluids.KAVA.get()&&!PSFluids.isFlowerExtract(fluid));
    }
    public Item emptyItem(){return switch(this){case BUCKET->Items.BUCKET;case BOWL->Items.BOWL;case GLASS_BOTTLE->Items.GLASS_BOTTLE;default->null;};}
    Item taggedItem(ItemStack current){return switch(this){case BUCKET->PSItems.FILLED_BUCKET.get();case BOWL->PSItems.FILLED_BOWL.get();case GLASS_BOTTLE->PSItems.FILLED_GLASS_BOTTLE.get();default->current.getItem();};}
}
