/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PortableFluidItem;
import ivorius.psychedelicraft.item.FluidMachineItem;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID)
public final class FluidVesselHandler extends FluidHandlerItemStack {
    public static final String CONTENTS="fluid";
    private final FluidVessel vessel;
    private final LazyOptional<IFluidHandlerItem> capability=LazyOptional.of(()->this);
    public FluidVesselHandler(ItemStack stack,FluidVessel vessel){super(stack,vessel.capacity);this.vessel=vessel;}
    public FluidVesselHandler(ItemStack stack,int capacity){super(stack,capacity);vessel=null;}
    public void invalidate(){capability.invalidate();}
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<ItemStack> event){
        Item item=event.getObject().getItem();
        if(item==Items.BOWL||item==Items.GLASS_BOTTLE||item==Items.POTION||item==Items.HONEY_BOTTLE){
            var handler=new FluidVesselHandler(event.getObject(),item==Items.BOWL?FluidVessel.BOWL:FluidVessel.GLASS_BOTTLE);
            event.addCapability(Psychedelicraft.id("fluid_vessel"),handler);event.addListener(handler::invalidate);
        }
    }
    public static @Nullable FluidVesselHandler forCrafting(ItemStack stack){
        if(stack.isEmpty())return null;Item item=stack.getItem();FluidVessel vessel;
        if(item instanceof FluidMachineItem machine){ItemStack single=stack.copy();single.setCount(1);return new FluidVesselHandler(single,machine.getCapacity());}
        if(item instanceof PortableFluidItem portable)vessel=portable.getVessel();
        else if(item==Items.BUCKET||item==Items.WATER_BUCKET||item==Items.LAVA_BUCKET||item==Items.MILK_BUCKET)vessel=FluidVessel.BUCKET;
        else if(item==Items.BOWL)vessel=FluidVessel.BOWL;
        else if(item==Items.GLASS_BOTTLE||item==Items.HONEY_BOTTLE||isWaterBottle(stack))vessel=FluidVessel.GLASS_BOTTLE;
        else return null;
        ItemStack single=stack.copy();single.setCount(1);return new FluidVesselHandler(single,vessel);
    }
    public static @Nullable IFluidHandlerItem forTransfer(ItemStack stack){
        FluidVesselHandler own=forCrafting(stack);if(own!=null)return own;if(stack.isEmpty())return null;
        ItemStack single=stack.copy();single.setCount(1);return FluidUtil.getFluidHandler(single).orElse(null);
    }
    public static FluidStack contents(ItemStack stack,FluidVessel vessel){
        if(stack.isEmpty())return FluidStack.EMPTY;
        CompoundTag root=stack.getTag();
        if(root!=null&&root.contains(CONTENTS,Tag.TAG_COMPOUND)){
            FluidStack saved=FluidContents.read(root.getCompound(CONTENTS),vessel.capacity);if(!saved.isEmpty())return saved;
        }
        Item item=stack.getItem();
        if(item==Items.WATER_BUCKET||isWaterBottle(stack))return new FluidStack(Fluids.WATER,vessel.capacity);
        if(item==Items.LAVA_BUCKET)return new FluidStack(Fluids.LAVA,vessel.capacity);
        if(item==Items.MILK_BUCKET)return new FluidStack(PSFluids.MILK.get(),vessel.capacity);
        if(vessel==FluidVessel.GLASS_BOTTLE&&item==Items.HONEY_BOTTLE)return new FluidStack(PSFluids.HONEY.get(),vessel.capacity);
        return FluidStack.EMPTY;
    }
    private static boolean isWaterBottle(ItemStack stack){return stack.is(Items.POTION)&&PotionUtils.getPotion(stack)==Potions.WATER&&PotionUtils.getMobEffects(stack).isEmpty();}
    @Override public FluidStack getFluid(){return vessel==null?(container.isEmpty()?FluidStack.EMPTY:FluidContents.read(container.getTag()==null?new CompoundTag():container.getTag().getCompound(CONTENTS),capacity)):contents(container,vessel);}
    @Override public boolean canFillFluidType(FluidStack fluid){return vessel==null||vessel.accepts(fluid.getFluid());}
    @Override public boolean isFluidValid(int tank,FluidStack fluid){return tank==0&&!fluid.isEmpty()&&canFillFluidType(fluid);}
    @Override public int fill(FluidStack resource,FluidAction action){return super.fill(FluidContents.normalize(resource),action);}
    @Override public FluidStack drain(FluidStack resource,FluidAction action){return super.drain(FluidContents.normalize(resource),action);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> type,@Nullable Direction side){
        return !container.isEmpty()&&(!container.is(Items.POTION)||isWaterBottle(container))?ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(type,capability):LazyOptional.empty();
    }
    public ItemStack craftContents(Fluid fluid,int amount,@Nullable CompoundTag attributes){
        int existing=getFluid().getAmount(),level=amount<=0?capacity:existing+Math.min(amount,capacity-existing);
        setFluid(new FluidStack(PSFluids.canonical(fluid),level,attributes==null?null:attributes.copy()));return container;
    }
    public ItemStack replaceContents(FluidStack fluid){
        FluidStack contents=FluidContents.normalize(fluid);if(contents.isEmpty())setContainerToEmpty();else{contents.setAmount(Math.min(capacity,contents.getAmount()));setFluid(contents);}return container;
    }
    @Override protected void setFluid(FluidStack supplied){
        FluidStack stack=FluidContents.normalize(supplied);if(stack.isEmpty()){setContainerToEmpty();return;}Fluid fluid=stack.getFluid();
        if(vessel!=null&&stack.getAmount()==capacity&&!stack.hasTag()){
            if(vessel==FluidVessel.BUCKET){
                if(fluid==Fluids.WATER){replace(Items.WATER_BUCKET);return;}if(fluid==Fluids.LAVA){replace(Items.LAVA_BUCKET);return;}
                if(fluid==PSFluids.MILK.get()){replace(Items.MILK_BUCKET);return;}
            }
            if(vessel==FluidVessel.GLASS_BOTTLE){
                if(fluid==PSFluids.HONEY.get()){replace(Items.HONEY_BOTTLE);return;}
                if(fluid==Fluids.WATER){replace(Items.POTION);PotionUtils.setPotion(container,Potions.WATER);return;}
            }
        }
        replace(vessel==null?container.getItem():vessel.taggedItem(container));container.getOrCreateTag().put(CONTENTS,FluidContents.write(stack));
    }
    @Override protected void setContainerToEmpty(){Item empty=vessel==null?null:vessel.emptyItem();replace(empty==null?container.getItem():empty);}
    private void replace(Item item){
        Item old=container.getItem();CompoundTag tag=container.getTag()==null?null:container.getTag().copy();
        if(old!=item){
            // A replacement consumes the previous single container. Cached views must not retain its contents.
            container.setCount(0);invalidate();container=new ItemStack(item);
        }
        if(tag!=null){tag.remove(CONTENTS);if(old==Items.POTION)tag.remove("Potion");container.setTag(tag.isEmpty()?null:tag);}
    }
}
