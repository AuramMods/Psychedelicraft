/* Copyright (c) 2014, Lukas Tenbrink. Modern professions and offers by Sollace. */
package ivorius.psychedelicraft.entity;

import com.google.common.collect.ImmutableSet;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import java.util.List;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID)
public final class PSTradeOffers {
    private static final DeferredRegister<PoiType> POIS=DeferredRegister.create(ForgeRegistries.POI_TYPES,Psychedelicraft.MOD_ID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS=DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS,Psychedelicraft.MOD_ID);
    public static final ResourceKey<PoiType> DEALER_POI=ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,Psychedelicraft.id("drug_dealer"));
    public static final RegistryObject<PoiType> DRUG_DEALER_POI=POIS.register("drug_dealer",()->new PoiType(
            Stream.of(PSBlocks.DRYING_TABLE.get(),PSBlocks.IRON_DRYING_TABLE.get()).flatMap(block->block.getStateDefinition().getPossibleStates().stream()).collect(ImmutableSet.toImmutableSet()),1,1));
    public static final RegistryObject<VillagerProfession> DRUG_DEALER=PROFESSIONS.register("drug_dealer",()->new VillagerProfession(
            "psychedelicraft.drug_dealer",poi->poi.is(DEALER_POI),poi->poi.is(DEALER_POI),ImmutableSet.of(
                    PSItems.CANNABIS_SEEDS.get(),PSItems.HOP_SEEDS.get(),PSItems.TOBACCO_SEEDS.get(),PSItems.COCA_SEEDS.get(),
                    PSItems.COFFEA_CHERRIES.get(),PSItems.MORNING_GLORY_SEEDS.get(),PSItems.CANNABIS_BUDS.get(),PSItems.CANNABIS_LEAF.get(),
                    PSItems.TOBACCO_LEAVES.get(),PSItems.COCA_LEAVES.get(),PSItems.PEYOTE.get(),Items.BONE_MEAL),
            ImmutableSet.of(Blocks.FARMLAND),SoundEvents.WANDERING_TRADER_DRINK_POTION));
    // Addicts keep their profession without claiming a workplace; their restock behavior uses the native daily stock rules.
    public static final RegistryObject<VillagerProfession> DRUG_ADDICT=PROFESSIONS.register("drug_addict",()->new VillagerProfession(
            "psychedelicraft.drug_addict",PoiType.NONE,PoiType.NONE,ImmutableSet.of(),ImmutableSet.of(),null));
    private PSTradeOffers() {}
    public static void register(IEventBus bus){POIS.register(bus);PROFESSIONS.register(bus);}

    @SubscribeEvent public static void trades(VillagerTradesEvent event){
        if(event.getType()==DRUG_DEALER.get()){
            var trades=event.getTrades();
            trades.get(1).addAll(List.of(
                    sell(1,PSItems.CANNABIS_LEAF.get(),1,9,1,.7F),sell(1,PSItems.CANNABIS_SEEDS.get(),5,12,2,.5F),
                    sell(1,PSItems.HASH_MUFFIN.get(),2,3,1,.7F),sell(1,PSItems.COCA_LEAVES.get(),4,4,1,.5F),
                    sell(2,PSItems.COCA_SEEDS.get(),4,4,1,.5F),sell(1,PSItems.PEYOTE.get(),5,4,4,.5F),sell(1,PSItems.CIGARETTE.get(),4,2,3,.8F)));
            trades.get(2).addAll(List.of(
                    sell(2,PSItems.DRIED_CANNABIS_BUDS.get(),2,8,2,.9F),sell(2,PSItems.DRIED_CANNABIS_LEAF.get(),2,5,3,.8F),
                    sell(3,PSItems.DRIED_PEYOTE.get(),10,2,2,.5F),sell(3,PSItems.DRIED_COCA_LEAVES.get(),20,3,2,.5F),
                    sell(1,PSItems.CIGAR.get(),5,2,3,.5F),sell(1,PSItems.SMOKING_PIPE.get(),5,2,3,.5F),sell(6,PSItems.DRYING_TABLE.get(),1,2,3,.5F)));
            trades.get(3).addAll(List.of(
                    sell(5,PSItems.BROWN_MAGIC_MUSHROOMS.get(),8,3,3,.5F),sell(2,PSItems.RED_MAGIC_MUSHROOMS.get(),8,3,3,.5F),
                    sell(3,PSItems.SYRINGE.get(),4,3,1,.5F),sell(3,PSItems.BONG.get(),4,3,1,.5F),sell(1,PSItems.PEYOTE_JOINT.get(),3,2,3,.5F),
                    sell(2,PSItems.LSD_PILL.get(),3,2,3,.5F),
                    (merchant,random)->new MerchantOffer(new ItemStack(Items.EMERALD,3),new ItemStack(Items.PAPER,2),new ItemStack(PSItems.LSA_SQUARE.get(),3),2,3,.5F),
                    sell(1,PSItems.JOINT.get(),2,2,3,.5F)));
            if(PSConfig.ENABLE_HARMONIUM.get())trades.get(3).add(PSTradeOffers::dyedHarmonium);
        }else if(event.getType()==DRUG_ADDICT.get()){
            event.getTrades().get(1).addAll(List.of(
                    buy(5,PSItems.BROWN_MAGIC_MUSHROOMS.get(),8,3,3,.5F),buy(2,PSItems.RED_MAGIC_MUSHROOMS.get(),8,3,3,.5F),
                    buy(2,PSItems.LSD_PILL.get(),3,2,3,.5F),buy(1,PSItems.JOINT.get(),2,2,3,.5F),buy(1,PSItems.CIGARETTE.get(),4,2,3,.8F),
                    buy(1,PSItems.CIGAR.get(),5,2,3,.5F),buy(1,PSItems.HASH_MUFFIN.get(),2,3,1,.7F),
                    buy(4,PSItems.DRIED_CANNABIS_BUDS.get(),2,8,2,.9F),buy(4,PSItems.DRIED_CANNABIS_LEAF.get(),2,5,3,.8F),
                    buy(5,PSItems.DRIED_PEYOTE.get(),10,2,2,.5F),buy(5,PSItems.DRIED_COCA_LEAVES.get(),20,3,2,.5F)));
        }else if(event.getType()==VillagerProfession.FARMER&&PSConfig.FARMER_DRUG_DEALS.get()){
            event.getTrades().get(1).addAll(List.of(
                    sell(2,PSItems.WINE_GRAPES.get(),3,8,1,.5F),sell(1,PSItems.HOP_CONES.get(),1,4,1,.6F),sell(1,PSItems.HOP_SEEDS.get(),1,4,1,.4F),
                    sell(1,PSItems.WOODEN_MUG.get(),1,4,1,.5F),sell(4,PSItems.DRIED_TOBACCO.get(),1,4,1,.3F),
                    sell(2,PSItems.CIGARETTE.get(),1,4,1,.8F),sell(2,PSItems.CIGAR.get(),1,4,1,.8F),sell(2,PSItems.TOBACCO_SEEDS.get(),1,4,1,.3F),
                    sell(1,PSItems.COFFEE_BEANS.get(),1,4,1,.8F),sell(1,PSItems.COFFEA_CHERRIES.get(),1,4,1,.6F)));
        }
    }
    public static boolean bulkUnstackablePayment(ItemStack stack){
        var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        return stack.getCount()>1&&stack.getMaxStackSize()==1&&id!=null&&id.getNamespace().equals(Psychedelicraft.MOD_ID);
    }
    private static MerchantOffer dyedHarmonium(net.minecraft.world.entity.Entity merchant,net.minecraft.util.RandomSource random){
        var dyes=new java.util.ArrayList<DyeItem>();
        dyes.add(DyeItem.byColor(DyeColor.byId(random.nextInt(16))));
        if(random.nextFloat()>.7F)dyes.add(DyeItem.byColor(DyeColor.byId(random.nextInt(16))));
        if(random.nextFloat()>.8F)dyes.add(DyeItem.byColor(DyeColor.byId(random.nextInt(16))));
        // Use native leather color mixing without making harmonium an armor item.
        var sample=DyeableLeatherItem.dyeArmor(new ItemStack(Items.LEATHER_CHESTPLATE),dyes);
        var output=new ItemStack(PSItems.HARMONIUM.get());
        ivorius.psychedelicraft.item.HarmoniumItem.setColor(output,((DyeableLeatherItem)sample.getItem()).getColor(sample));
        return new MerchantOffer(new ItemStack(Items.EMERALD,3),output,7,2,.2F);
    }
    private static VillagerTrades.ItemListing sell(int emeralds,Item item,int count,int uses,int xp,float multiplier){
        return (merchant,random)->new MerchantOffer(new ItemStack(Items.EMERALD,emeralds),new ItemStack(item,count),uses,xp,multiplier);
    }
    private static VillagerTrades.ItemListing buy(int emeralds,Item item,int count,int uses,int xp,float multiplier){
        return (merchant,random)->new MerchantOffer(new ItemStack(item,count),new ItemStack(Items.EMERALD,emeralds),uses,xp,multiplier);
    }
}
