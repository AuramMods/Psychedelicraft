package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.PSBlockEntities;
import ivorius.psychedelicraft.client.render.blocks.DryingTableRenderer;
import ivorius.psychedelicraft.client.render.blocks.FlaskRenderer;
import ivorius.psychedelicraft.client.render.blocks.BarrelRenderer;
import ivorius.psychedelicraft.client.render.blocks.MashTubRenderer;
import ivorius.psychedelicraft.client.render.blocks.PlacedDrinksRenderer;
import ivorius.psychedelicraft.client.render.blocks.BottleRackRenderer;
import ivorius.psychedelicraft.client.screen.FluidMachineScreen;
import ivorius.psychedelicraft.client.screen.DryingTableScreen;
import ivorius.psychedelicraft.screen.PSMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientMachines {
    private ClientMachines(){}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{MenuScreens.register(PSMenus.DRYING_TABLE.get(),DryingTableScreen::new);MenuScreens.register(PSMenus.FLUID_MACHINE.get(),FluidMachineScreen::new);});}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(ivorius.psychedelicraft.entity.PSEntities.MOLOTOV_COCKTAIL.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);event.registerBlockEntityRenderer(PSBlockEntities.JUNIPER_SIGN.get(),net.minecraft.client.renderer.blockentity.SignRenderer::new);event.registerBlockEntityRenderer(PSBlockEntities.JUNIPER_HANGING_SIGN.get(),net.minecraft.client.renderer.blockentity.HangingSignRenderer::new);event.registerEntityRenderer(ivorius.psychedelicraft.entity.PSEntities.JUNIPER_BOAT.get(),context->new ivorius.psychedelicraft.client.render.entity.JuniperBoatRenderer(context,false));event.registerEntityRenderer(ivorius.psychedelicraft.entity.PSEntities.JUNIPER_CHEST_BOAT.get(),context->new ivorius.psychedelicraft.client.render.entity.JuniperBoatRenderer(context,true));event.registerBlockEntityRenderer(PSBlockEntities.DRYING_TABLE.get(),DryingTableRenderer::new);event.registerBlockEntityRenderer(PSBlockEntities.FLASK.get(),FlaskRenderer::new);event.registerBlockEntityRenderer(PSBlockEntities.DISTILLERY.get(),FlaskRenderer::new);event.registerBlockEntityRenderer(PSBlockEntities.BARREL.get(),BarrelRenderer::new);event.registerBlockEntityRenderer(PSBlockEntities.MASH_TUB.get(),MashTubRenderer::new);event.registerBlockEntityRenderer(PSBlockEntities.PLACED_DRINK.get(),PlacedDrinksRenderer::new);event.registerBlockEntityRenderer(PSBlockEntities.BOTTLE_RACK.get(),BottleRackRenderer::new);}
}
