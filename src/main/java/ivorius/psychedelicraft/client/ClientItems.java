package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.HarmoniumItem;
import ivorius.psychedelicraft.item.PortableFluidItem;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.world.entity.player.Player;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID, value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientItems {
    private ClientItems() {}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ivorius.psychedelicraft.item.BagOfVomitItem.disguise(BagDisguise::form);
            ItemProperties.register(PSItems.BAG_O_VOMIT.get(),Psychedelicraft.id("disguise"),(stack,level,entity,seed)->BagDisguise.form());
            ItemProperties.register(PSItems.PAPER_BAG.get(),Psychedelicraft.id("filled"),(stack,level,entity,seed)->{int count=ivorius.psychedelicraft.item.PaperBagItem.getContents(stack).count();return count>16000?1:count>0?.5F:0;});
            for(var fluid:ivorius.psychedelicraft.fluid.PSFluids.all()){net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(fluid.get(),net.minecraft.client.renderer.RenderType.translucent());net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(fluid.flowing.get(),net.minecraft.client.renderer.RenderType.translucent());}
            net.minecraft.client.renderer.Sheets.addWoodType(ivorius.psychedelicraft.block.PSWoodTypes.JUNIPER);
            ivorius.psychedelicraft.block.PSBlocks.BLOCKS.getEntries().forEach(block ->
                    net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(block.get(),net.minecraft.client.renderer.RenderType.cutout()));
            for(var leaf:java.util.List.of(ivorius.psychedelicraft.block.PSBlocks.JUNIPER_LEAVES,ivorius.psychedelicraft.block.PSBlocks.FRUITING_JUNIPER_LEAVES))net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(leaf.get(),net.minecraft.client.renderer.RenderType.cutoutMipped());
            for (var item : java.util.List.of(PSItems.COCAINE_POWDER,PSItems.OBSIDIAN_DUST,PSItems.JOINT,PSItems.PEYOTE_JOINT,PSItems.CIGARETTE,PSItems.CIGAR,PSItems.SMOKING_PIPE,PSItems.BONG)) {
                ItemProperties.register(item.get(), Psychedelicraft.id("using"),
                        (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0);
            }
            for(var item:java.util.List.of(PSItems.SMOKING_PIPE,PSItems.BONG))ItemProperties.register(item.get(),Psychedelicraft.id("filled"),
                    (stack,level,entity,seed)->entity instanceof Player player&&(item.get().canDraw(player)
                            ||!player.isLocalPlayer()&&player.isUsingItem()&&player.getUseItem()==stack)?1:0);
            for(var crop:java.util.List.of(PSItems.WINE_GRAPE_LATTICE,PSItems.MORNING_GLORY_LATTICE))ItemProperties.register(crop.get(),Psychedelicraft.id("age"),
                    (stack,level,entity,seed)->ivorius.psychedelicraft.item.LatticeCropItem.age(stack)/10F);
            ItemProperties.register(PSItems.MOLOTOV_COCKTAIL.get(),Psychedelicraft.id("flying"),(stack,level,entity,seed)->stack.hasTag()&&stack.getTag().getBoolean(ivorius.psychedelicraft.item.MolotovCocktailItem.FLYING)?1:0);
            PSItems.ITEMS.getEntries().forEach(entry -> {
                if(entry.get() instanceof PortableFluidItem vessel)ItemProperties.register(vessel,Psychedelicraft.id("filled"),
                        (stack,level,entity,seed)->vessel.contents(stack).isEmpty()?0:1);
            });
        });
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){
        event.register((stack,tint)->tint==0?HarmoniumItem.getColor(stack):-1,PSItems.HARMONIUM.get());
        PSItems.ITEMS.getEntries().forEach(entry -> {
            if(entry.get() instanceof PortableFluidItem vessel)event.register((stack,layer)->{
                if((vessel==PSItems.BOTTLE.get()||vessel==PSItems.MOLOTOV_COCKTAIL.get())&&layer==0)return HarmoniumItem.getColor(stack);
                int fluidLayer=vessel==PSItems.FILLED_GLASS_BOTTLE.get()?0:1;
                var contents=vessel.contents(stack);
                return layer!=fluidLayer||contents.isEmpty()?-1:IClientFluidTypeExtensions.of(contents.getFluid()).getTintColor(contents);
            },vessel);
        });
    }
}
