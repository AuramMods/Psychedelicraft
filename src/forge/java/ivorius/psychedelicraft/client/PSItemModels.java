package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.HarmoniumItem;
import ivorius.psychedelicraft.block.BarrelBlock;
import ivorius.psychedelicraft.block.JuniperLeavesBlock;
import ivorius.psychedelicraft.block.JuniperSignBlock;
import ivorius.psychedelicraft.block.LatticeCropBlock;
import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.RiftJarBlock;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import ivorius.psychedelicraft.block.entity.BottleRackBlockEntity;
import ivorius.psychedelicraft.block.entity.DistilleryBlockEntity;
import ivorius.psychedelicraft.block.entity.FlaskBlockEntity;
import ivorius.psychedelicraft.block.entity.JuniperSignBlockEntity;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.block.entity.PeyoteBlockEntity;
import ivorius.psychedelicraft.block.entity.PlacedDrinkBlockEntity;
import ivorius.psychedelicraft.block.entity.RiftJarBlockEntity;
import ivorius.psychedelicraft.client.render.block.DryingTableRenderer;
import ivorius.psychedelicraft.client.render.block.BarrelRenderer;
import ivorius.psychedelicraft.client.render.block.BottleRackRenderer;
import ivorius.psychedelicraft.client.render.block.FlaskRenderer;
import ivorius.psychedelicraft.client.render.block.JuniperSignRenderer;
import ivorius.psychedelicraft.client.render.block.MashTubRenderer;
import ivorius.psychedelicraft.client.render.block.PeyoteRenderer;
import ivorius.psychedelicraft.client.render.block.PlacedDrinksRenderer;
import ivorius.psychedelicraft.client.render.block.RiftJarRenderer;
import ivorius.psychedelicraft.client.render.entity.JuniperBoatRenderer;
import ivorius.psychedelicraft.client.render.entity.RealityRiftRenderer;
import ivorius.psychedelicraft.entity.EntityJuniperBoat;
import ivorius.psychedelicraft.entity.EntityJuniperChestBoat;
import ivorius.psychedelicraft.entity.RealityRiftEntity;
import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PortableFluidItem;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class PSItemModels {
    private PSItemModels() {
    }

    @SubscribeEvent
    public static void registerItemColors(ColorHandlerEvent.Item event) {
        event.getItemColors().registerItemColorHandler((stack, layer) -> layer == 0 ? HarmoniumItem.getColor(stack) : -1,
                PSItems.HARMONIUM);
        for (Item item : PSItems.items()) {
            if (!(item instanceof PortableFluidItem)) continue;
            PortableFluidItem vessel = (PortableFluidItem) item;
            event.getItemColors().registerItemColorHandler((stack, layer) -> {
                if (vessel == PSItems.BOTTLE && layer == 0) {
                    return stack.hasTagCompound() && stack.getTagCompound().getCompoundTag("display").hasKey("color", 99)
                            ? stack.getTagCompound().getCompoundTag("display").getInteger("color") : 0xA06540;
                }
                int fluidLayer = vessel == PSItems.FILLED_GLASS_BOTTLE ? 0 : 1;
                FluidStack fluid = layer == fluidLayer ? vessel.contents(stack) : null;
                return fluid == null ? -1 : fluid.getFluid().getColor(fluid);
            }, vessel);
        }
    }

    @SubscribeEvent
    public static void stitchFluids(TextureStitchEvent.Pre event) {
        for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
            if (fluid.getStill() != null && Psychedelicraft.MOD_ID.equals(fluid.getStill().getResourceDomain())) {
                event.getMap().registerSprite(fluid.getStill());
                event.getMap().registerSprite(fluid.getFlowing());
            }
        }
        for (String texture : new String[]{"rum_mature", "rum_semi_mature", "ketchup", "clear", "slurry", "wine", "beer", "rice_wine", "cider", "mead", "tomato_juice"}) {
            event.getMap().registerSprite(BrewFluid.texture(texture, "_still"));
            event.getMap().registerSprite(BrewFluid.texture(texture, "_flow"));
        }
        for (String symbol : new String[]{"beer", "gin", "mead", "rum", "vodka", "whiskey", "wine"}) {
            event.getMap().registerSprite(Psychedelicraft.id("fluid/" + symbol));
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        PSItems.BAG_O_VOMIT.registerDisguise();
        for (Item item : PSItems.items()) {
            if (item == PSItems.MORNING_GLORY_LATTICE || item == PSItems.WINE_GRAPE_LATTICE) {
                for (int age = 0; age <= LatticeCropBlock.MAX_AGE; age++) {
                    ModelLoader.setCustomModelResourceLocation(item, age, new ModelResourceLocation(
                            Psychedelicraft.id(item.getRegistryName().getResourcePath() + "_" + age), "inventory"));
                }
            } else {
                register(item);
            }
        }
        shareModel(PSBlocks.NATURAL_CANNABIS, PSBlocks.CANNABIS);
        shareModel(PSBlocks.NATURAL_HOP, PSBlocks.HOP);
        shareModel(PSBlocks.NATURAL_COCA, PSBlocks.COCA);
        shareModel(PSBlocks.NATURAL_TOBACCO, PSBlocks.TOBACCO);
        shareModel(PSBlocks.NATURAL_COFFEA, PSBlocks.COFFEA);
        shareModel(PSBlocks.POWERED_JUNIPER_TRAPDOOR, PSBlocks.JUNIPER_TRAPDOOR);
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_DOOR, new StateMap.Builder().ignore(BlockDoor.POWERED).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_FENCE_GATE, new StateMap.Builder().ignore(BlockFenceGate.POWERED).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_SLAB, new StateMap.Builder()
                .ignore(PSBlocks.JUNIPER_SLAB.getVariantProperty()).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_DOUBLE_SLAB, new StateMap.Builder()
                .ignore(PSBlocks.JUNIPER_DOUBLE_SLAB.getVariantProperty()).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_LEAVES, new StateMap.Builder()
                .ignore(JuniperLeavesBlock.DISTANCE, JuniperLeavesBlock.PERSISTENT).build());
        ModelLoader.setCustomStateMapper(PSBlocks.FRUITING_JUNIPER_LEAVES, new StateMap.Builder()
                .ignore(JuniperLeavesBlock.DISTANCE, JuniperLeavesBlock.PERSISTENT).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_SIGN, new StateMap.Builder().ignore(JuniperSignBlock.ROTATION).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_WALL_SIGN, new StateMap.Builder().ignore(JuniperSignBlock.FACING).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_HANGING_SIGN, new StateMap.Builder().ignore(JuniperSignBlock.ROTATION).build());
        ModelLoader.setCustomStateMapper(PSBlocks.JUNIPER_WALL_HANGING_SIGN, new StateMap.Builder().ignore(JuniperSignBlock.FACING).build());
        ModelLoader.setCustomStateMapper(PSBlocks.MASH_TUB, new StateMap.Builder().ignore(MashTubBlock.OFFSET).build());
        ModelLoader.setCustomStateMapper(PSBlocks.MASH_TUB_EDGE, new StateMap.Builder().ignore(MashTubBlock.OFFSET).build());
        for (Block barrel : new Block[]{PSBlocks.OAK_BARREL, PSBlocks.SPRUCE_BARREL, PSBlocks.BIRCH_BARREL,
                PSBlocks.JUNGLE_BARREL, PSBlocks.ACACIA_BARREL, PSBlocks.DARK_OAK_BARREL}) {
            ModelLoader.setCustomStateMapper(barrel, new StateMap.Builder().ignore(BarrelBlock.FACING, BarrelBlock.TAPPED).build());
        }
        ClientRegistry.bindTileEntitySpecialRenderer(BarrelBlockEntity.class, new BarrelRenderer());
        PlacedDrinksRenderer.registerModels();
        ClientRegistry.bindTileEntitySpecialRenderer(PlacedDrinkBlockEntity.class, new PlacedDrinksRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(BottleRackBlockEntity.class, new BottleRackRenderer());
        FlaskRenderer flaskRenderer = new FlaskRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(FlaskBlockEntity.class, flaskRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(DistilleryBlockEntity.class, flaskRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(MashTubBlockEntity.class, new MashTubRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(JuniperSignBlockEntity.class, new JuniperSignRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(DryingTableBlockEntity.class, new DryingTableRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(PeyoteBlockEntity.class, new PeyoteRenderer());
        ModelLoader.setCustomStateMapper(PSBlocks.RIFT_JAR, new StateMap.Builder().ignore(RiftJarBlock.FACING).build());
        RiftJarRenderer riftJar = new RiftJarRenderer();
        ClientRegistry.bindTileEntitySpecialRenderer(RiftJarBlockEntity.class, riftJar);
        PSItems.RIFT_JAR.setTileEntityItemStackRenderer(riftJar.itemRenderer());
        RenderingRegistry.registerEntityRenderingHandler(RealityRiftEntity.class, RealityRiftRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityJuniperBoat.class, manager -> new JuniperBoatRenderer(manager, false));
        RenderingRegistry.registerEntityRenderingHandler(EntityJuniperChestBoat.class, manager -> new JuniperBoatRenderer(manager, true));
    }

    @SubscribeEvent
    public static void openMountedInventory(GuiOpenEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (event.getGui() instanceof GuiInventory && player != null
                && player.getRidingEntity() instanceof EntityJuniperChestBoat) {
            event.setCanceled(true);
            Channel.openMountedBoatCargo();
        }
    }

    private static void shareModel(Block block, Block model) {
        ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return new ModelResourceLocation(model.getRegistryName(), getPropertyString(state.getProperties()));
            }
        });
    }

    private static void register(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
