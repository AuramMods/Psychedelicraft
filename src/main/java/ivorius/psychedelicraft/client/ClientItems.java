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
            ivorius.psychedelicraft.block.PSBlocks.BLOCKS.getEntries().forEach(block ->
                    net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(block.get(),net.minecraft.client.renderer.RenderType.cutout()));
            for (var item : java.util.List.of(PSItems.COCAINE_POWDER,PSItems.OBSIDIAN_DUST,PSItems.JOINT,PSItems.PEYOTE_JOINT,PSItems.CIGARETTE,PSItems.CIGAR,PSItems.SMOKING_PIPE,PSItems.BONG)) {
                ItemProperties.register(item.get(), Psychedelicraft.id("using"),
                        (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0);
            }
            for(var item:java.util.List.of(PSItems.SMOKING_PIPE,PSItems.BONG))ItemProperties.register(item.get(),Psychedelicraft.id("filled"),
                    (stack,level,entity,seed)->entity instanceof Player player&&(item.get().canDraw(player)
                            ||!player.isLocalPlayer()&&player.isUsingItem()&&player.getUseItem()==stack)?1:0);
            for(var crop:java.util.List.of(PSItems.WINE_GRAPE_LATTICE,PSItems.MORNING_GLORY_LATTICE))ItemProperties.register(crop.get(),Psychedelicraft.id("age"),
                    (stack,level,entity,seed)->ivorius.psychedelicraft.item.LatticeCropItem.age(stack)/10F);
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
                if(vessel==PSItems.BOTTLE.get()&&layer==0)return HarmoniumItem.getColor(stack);
                int fluidLayer=vessel==PSItems.FILLED_GLASS_BOTTLE.get()?0:1;
                var contents=vessel.contents(stack);
                return layer!=fluidLayer||contents.isEmpty()?-1:IClientFluidTypeExtensions.of(contents.getFluid()).getTintColor(contents);
            },vessel);
        });
    }
}
