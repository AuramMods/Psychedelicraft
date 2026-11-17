/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.*;
import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.*;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import java.util.*;
import java.util.function.Consumer;

public final class PSFluids {
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(ForgeRegistries.FLUIDS,Psychedelicraft.MOD_ID);
    private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,Psychedelicraft.MOD_ID);
    private static final Map<String,Family> FAMILIES=new LinkedHashMap<>();
    public static final Family WHEAT_HOP=family(BrewProfiles.wheatHop()),WHEAT=family(BrewProfiles.wheat()),POTATO=family(BrewProfiles.potato()),TOMATO=family(BrewProfiles.tomato()),
            RED_GRAPES=family(BrewProfiles.redGrapes()),RICE=family(BrewProfiles.rice()),JUNIPER=family(BrewProfiles.juniper()),HONEY=family(BrewProfiles.honey()),
            SUGAR_CANE=family(BrewProfiles.sugarCane()),CORN=family(BrewProfiles.corn()),APPLE=family(BrewProfiles.apple()),PINEAPPLE=family(BrewProfiles.pineapple()),
            BANANA=family(BrewProfiles.banana()),MILK=family(BrewProfiles.milk()),AGAVE=family(BrewProfiles.agave());
    public static final Family COFFEE=family(new FluidProfile("coffee","coffee",0xffa77d55,1){
        @Override public Component getDisplayName(FluidStack stack){return Component.translatable("fluid.psychedelicraft.coffee.temperature."+warmth(stack));}
    });
    public static final Family COCA_TEA=basic("coca_tea","tea",0x44787a36,1),CANNABIS_TEA=basic("cannabis_tea","tea",0x446d6f3c,1),
            PEYOTE_JUICE=basic("peyote_juice","tea",0x779bab62,2),KAVA=basic("kava","tea",0x779bab62,1),COCAINE=basic("cocaine","clear",0x44e8f4f8,1),
            CAFFEINE=basic("caffeine","clear",0x66eee2d3,1),BATH_SALTS=basic("bath_salts","clear",0x2233f4f8,1),
            MORNING_GLORY_EXTRACT=extract("morning_glory_extract"),BELLADONNA_EXTRACT=extract("belladonna_extract"),JIMSONWEED_EXTRACT=extract("jimsonweed_extract"),
            SLURRY=basic("slurry","slurry",0xcc704e21,4);
    private PSFluids(){}
    private static Family basic(String path,String texture,int color,int viscosity){return family(new FluidProfile(path,texture,color,viscosity));}
    private static Family extract(String path){return family(new FluidProfile(path,"clear",0x66eee2d3,1){
        @Override public Component getDisplayName(FluidStack stack){return Component.translatable("fluid.psychedelicraft."+path+".distilled."+BrewProfile.stage(stack,BrewProfile.DISTILLATION,2));}
        @Override public int getColor(FluidStack stack){return BrewProfile.blend(super.getColor(stack),0xffffffff,BrewProfile.stage(stack,BrewProfile.DISTILLATION,2)/16F);}
    });}
    private static Family family(FluidProfile profile){Family family=new Family(profile);FAMILIES.put(profile.path,family);return family;}
    public static Collection<Family> all(){return Collections.unmodifiableCollection(FAMILIES.values());}
    public static void register(IEventBus bus){TYPES.register(bus);FLUIDS.register(bus);}
    public static void registerInteractions(){
        for(var family:all())if(family.waterLike())FluidInteractionRegistry.addInteraction(net.minecraftforge.common.ForgeMod.LAVA_TYPE.get(),new FluidInteractionRegistry.InteractionInformation(family.get().getFluidType(),state->(state.isSource()?net.minecraft.world.level.block.Blocks.OBSIDIAN:net.minecraft.world.level.block.Blocks.COBBLESTONE).defaultBlockState()));
    }
    public static Fluid canonical(Fluid fluid){return fluid instanceof FlowingFluid flowing?flowing.getSource():fluid;}
    public static Fluid resolve(String id){ResourceLocation location=ResourceLocation.tryParse(id);Fluid fluid=location==null?null:ForgeRegistries.FLUIDS.getValue(location);return fluid==null||fluid==Fluids.EMPTY?null:canonical(fluid);}
    public static String id(Fluid fluid){return ForgeRegistries.FLUIDS.getKey(canonical(fluid)).toString();}
    public static FluidProfile profile(Fluid fluid){return canonical(fluid) instanceof WorldFluid world?world.family.profile:null;}
    public static BrewProfile brew(Fluid fluid){return profile(fluid) instanceof BrewProfile brew?brew:null;}
    public static Component displayName(FluidStack stack){FluidProfile profile=profile(stack.getFluid());return profile==null?stack.getDisplayName():profile.getDisplayName(stack);}
    public static int color(FluidStack stack){FluidProfile profile=profile(stack.getFluid());return profile==null?0xffffffff:profile.getColor(stack);}
    public static boolean isInjectable(Fluid fluid){fluid=canonical(fluid);return fluid==COCAINE.get()||fluid==CAFFEINE.get()||fluid==BATH_SALTS.get()||fluid==MORNING_GLORY_EXTRACT.get();}
    public static boolean isFlowerExtract(Fluid fluid){fluid=canonical(fluid);return fluid==MORNING_GLORY_EXTRACT.get()||fluid==BELLADONNA_EXTRACT.get()||fluid==JIMSONWEED_EXTRACT.get();}
    public static boolean canConsume(Fluid fluid,boolean injection){fluid=canonical(fluid);return injection?isInjectable(fluid):brew(fluid)!=null||isFlowerExtract(fluid)||fluid==COFFEE.get()||fluid==COCA_TEA.get()||fluid==CANNABIS_TEA.get()||fluid==PEYOTE_JUICE.get()||fluid==KAVA.get();}
    public static int warmth(FluidStack stack){return stack.hasTag()?Mth.clamp(stack.getTag().getInt("warmth"),0,2):0;}
    public static void consume(Player player,FluidStack portion){
        addServingInfluences(portion,DrugCapability.get(player));
        if(canonical(portion.getFluid())==COFFEE.get())StomachFoodStats.restoreSaturation(player,portion.getAmount()/(float)FluidVessel.CUP.serving);
    }
    public static void addServingInfluences(FluidStack portion,DrugProperties properties){
        if(portion.isEmpty())return;Fluid fluid=canonical(portion.getFluid());double buckets=portion.getAmount()/(double)FluidType.BUCKET_VOLUME;BrewProfile brew=brew(fluid);
        if(brew!=null)brew.addServingInfluences(portion,buckets,properties);
        else if(isFlowerExtract(fluid))properties.addInfluence(new DrugInfluence(fluid==MORNING_GLORY_EXTRACT.get()?DrugType.LSD:DrugType.ATROPINE,3,0,.03,Math.pow(96,BrewProfile.stage(portion,BrewProfile.DISTILLATION,2))*buckets));
        else if(fluid==COFFEE.get()){
            double warmth=warmth(portion)/2D;properties.addInfluence(new DrugInfluence(DrugType.CAFFEINE,20,.002,.001,(.25+warmth*.05)*buckets));
            if(warmth>0)properties.addInfluence(new DrugInfluence(DrugType.WARMTH,0,0,.1,.8*warmth*buckets));
        }else if(fluid==PEYOTE_JUICE.get())properties.addInfluence(new DrugInfluence(DrugType.PEYOTE,15,.005,.003,2*buckets));
        else if(fluid==CAFFEINE.get())properties.addInfluence(new DrugInfluence(DrugType.CAFFEINE,0,.005,.01,85*buckets));
        else if(fluid==COCA_TEA.get())properties.addInfluence(new DrugInfluence(DrugType.COCAINE,60,.005,.002,.2*buckets));
        else if(fluid==CANNABIS_TEA.get())properties.addInfluence(new DrugInfluence(DrugType.CANNABIS,60,.005,.002,.25*buckets));
        else if(fluid==KAVA.get())properties.addInfluence(new DrugInfluence(DrugType.KAVA,20,.005,.003,2*buckets));
        else if(fluid==COCAINE.get()||fluid==BATH_SALTS.get())properties.addInfluence(new DrugInfluence(fluid==COCAINE.get()?DrugType.COCAINE:DrugType.BATH_SALTS,0,.005,.01,50*buckets));
    }
    public static final class Family {
        public final FluidProfile profile;
        public final RegistryObject<WorldFluid.Source> source;
        public final RegistryObject<WorldFluid.Flowing> flowing;
        public final RegistryObject<ivorius.psychedelicraft.block.PhysicalFluidBlock> block;
        private final RegistryObject<FluidType> type;
        Family(FluidProfile profile){
            this.profile=profile;
            type=TYPES.register(profile.path,()->new FluidType(FluidType.Properties.create().descriptionId("fluid.psychedelicraft."+profile.path).density(1000).viscosity(profile.viscosity*1000).canConvertToSource(false).canSwim(waterLike()).canDrown(waterLike()).canExtinguish(waterLike()).supportsBoating(waterLike()).canHydrate(waterLike())
                    .sound(SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY)){
                @Override public boolean isVaporizedOnPlacement(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,FluidStack stack){return level.dimensionType().ultraWarm();}
                @Override public void onVaporize(@org.jetbrains.annotations.Nullable Player player,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,FluidStack stack){
                    level.playSound(player,pos,SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.5F,2.6F+(level.random.nextFloat()-level.random.nextFloat())*.8F);
                    if(level instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,8,.35,.35,.35,0);
                }
                @Override public Component getDescription(FluidStack stack){return profile.getDisplayName(stack);}
                @Override public ItemStack getBucket(FluidStack stack){if(stack.isEmpty())return ItemStack.EMPTY;ItemStack bucket=new ItemStack(PSItems.FILLED_BUCKET.get());FluidStack portion=FluidContents.normalize(stack);portion.setAmount(Math.min(portion.getAmount(),BUCKET_VOLUME));bucket.getOrCreateTag().put("fluid",FluidContents.write(portion));return bucket;}
                @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer){consumer.accept(new IClientFluidTypeExtensions(){
                    @Override public ResourceLocation getStillTexture(){return profile.getStill(FluidStack.EMPTY);}
                    @Override public ResourceLocation getFlowingTexture(){return profile.getFlowing(FluidStack.EMPTY);}
                    @Override public ResourceLocation getStillTexture(FluidStack stack){return profile.getStill(stack);}
                    @Override public ResourceLocation getFlowingTexture(FluidStack stack){return profile.getFlowing(stack);}
                    @Override public int getTintColor(){return profile.getColor(FluidStack.EMPTY);}
                    @Override public int getTintColor(FluidStack stack){return profile.getColor(stack);}
                    @Override public ResourceLocation getRenderOverlayTexture(net.minecraft.client.Minecraft client){return waterLike()?new ResourceLocation("minecraft","textures/misc/underwater.png"):null;}
                    private FluidStack worldContents(net.minecraft.world.level.BlockAndTintGetter level,net.minecraft.core.BlockPos pos){return block.get().contents(level,pos);}
                    private boolean waterTexture(){return Family.this==KAVA||isFlowerExtract(Family.this.get());}
                    @Override public ResourceLocation getStillTexture(FluidState state,net.minecraft.world.level.BlockAndTintGetter level,net.minecraft.core.BlockPos pos){return waterTexture()?new ResourceLocation("minecraft","block/water_still"):profile.getStill(worldContents(level,pos));}
                    @Override public ResourceLocation getFlowingTexture(FluidState state,net.minecraft.world.level.BlockAndTintGetter level,net.minecraft.core.BlockPos pos){return waterTexture()?new ResourceLocation("minecraft","block/water_flow"):profile.getFlowing(worldContents(level,pos));}
                    @Override public int getTintColor(FluidState state,net.minecraft.world.level.BlockAndTintGetter level,net.minecraft.core.BlockPos pos){return waterTexture()?0xff000000|profile.getColor(worldContents(level,pos)):0xffffffff;}
                    @Override public org.joml.Vector3f modifyFogColor(net.minecraft.client.Camera camera,float partialTick,net.minecraft.client.multiplayer.ClientLevel level,int distance,float darken,org.joml.Vector3f original){
                        if(!waterLike())return original;int color=profile.getColor(worldContents(level,camera.getBlockPosition()));return new org.joml.Vector3f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F);
                    }
                });}
            });
            source=FLUIDS.register(profile.path,()->new WorldFluid.Source(this));flowing=FLUIDS.register("flowing_"+profile.path,()->new WorldFluid.Flowing(this));
            block=ivorius.psychedelicraft.block.PSBlocks.BLOCKS.register(profile.path,()->new ivorius.psychedelicraft.block.PhysicalFluidBlock(this));
        }
        public WorldFluid get(){return source.get();}
        public boolean waterLike(){return !Set.of("agave","kava","bath_salts").contains(profile.path);}
        ForgeFlowingFluid.Properties properties(){return new ForgeFlowingFluid.Properties(type,source,flowing).bucket(()->PSItems.FILLED_BUCKET.get()).block(block).levelDecreasePerBlock(profile.viscosity).tickRate(5);}
    }
}
