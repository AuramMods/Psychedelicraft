/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render.block;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PortableFluidItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class DrinkModels {
    private static final Map<Item, Geometry> MODELS = new LinkedHashMap<>();

    static {
        register(PSItems.WOODEN_MUG, 0.25F, 0.5F);
        register(PSItems.STONE_CUP, 0.25F, 0.5F);
        register(PSItems.SHOT_GLASS, 0.2F, 0.5F);
        register(PSItems.GLASS_CHALICE, 0.4F, 3.5F);
        register(PSItems.BOTTLE, 0.5F, 0.5F);
    }

    private DrinkModels() {
    }

    private static void register(Item item, float height, float fluidOrigin) {
        MODELS.put(item, new Geometry(item, height, fluidOrigin));
    }

    static void registerVariants() {
        MODELS.forEach((item, geometry) -> ModelBakery.registerItemVariants(item, geometry.vessel, geometry.fluid));
    }

    static void loadGeometry(IResourceManager resources) {
        MODELS.values().forEach(Geometry::reset);
        ResourceLocation location = Psychedelicraft.id("placeable_drinks.json");
        try (IResource resource = resources.getResource(location);
             InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject config = new JsonParser().parse(reader).getAsJsonObject();
            for (JsonElement element : config.getAsJsonArray("values")) {
                JsonObject entry = element.getAsJsonObject();
                Item item = Item.REGISTRY.getObject(new ResourceLocation(entry.get("id").getAsString()));
                Geometry geometry = MODELS.get(item);
                if (geometry != null) {
                    float height = entry.get("height").getAsFloat();
                    float origin = entry.get("fluid_origin").getAsFloat();
                    if (Float.isFinite(height) && height > 0 && Float.isFinite(origin)) {
                        geometry.height = height;
                        geometry.fluidOrigin = origin / 16;
                    }
                }
            }
        } catch (IOException | RuntimeException error) {
            LogManager.getLogger(Psychedelicraft.MOD_ID).warn("Could not load {}", location, error);
        }
    }

    static float render(ItemStack stack) {
        Item item = stack.getItem();
        Geometry geometry = MODELS.get(item);
        if (geometry == null) return 0;
        PortableFluidItem vessel = (PortableFluidItem) item;
        FluidStack contents = vessel.contents(stack);
        Minecraft client = Minecraft.getMinecraft();
        client.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        int tint = item == PSItems.BOTTLE ? client.getItemColors().colorMultiplier(stack, 0) : -1;
        draw(client, geometry.vessel, tint);
        if (contents != null) {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(0, geometry.fluidOrigin, 0);
                GlStateManager.scale(1, contents.amount / (float) vessel.getVessel().capacity, 1);
                GlStateManager.translate(0, -geometry.fluidOrigin, 0);
                draw(client, geometry.fluid, contents.getFluid().getColor(contents));
            } finally {
                GlStateManager.popMatrix();
            }
        }
        return geometry.height;
    }

    private static void draw(Minecraft client, ModelResourceLocation name, int tint) {
        RenderItem renderer = client.getRenderItem();
        IBakedModel model = renderer.getItemModelMesher().getModelManager().getModel(name);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
        for (EnumFacing face : EnumFacing.VALUES) {
            renderer.renderQuads(buffer, model.getQuads(null, face, 42), tint | 0xff000000, ItemStack.EMPTY);
        }
        renderer.renderQuads(buffer, model.getQuads(null, null, 42), tint | 0xff000000, ItemStack.EMPTY);
        tessellator.draw();
    }

    private static final class Geometry {
        private final ModelResourceLocation vessel;
        private final ModelResourceLocation fluid;
        private final float defaultHeight;
        private final float defaultFluidOrigin;
        private float height;
        private float fluidOrigin;

        private Geometry(Item item, float height, float fluidOrigin) {
            String path = item.getRegistryName().getResourcePath();
            this.vessel = new ModelResourceLocation(Psychedelicraft.id(path + "_on_ground"), "inventory");
            this.fluid = new ModelResourceLocation(Psychedelicraft.id(path + "_on_ground_fluid"), "inventory");
            defaultHeight = height;
            defaultFluidOrigin = fluidOrigin / 16;
            reset();
        }

        private void reset() {
            height = defaultHeight;
            fluidOrigin = defaultFluidOrigin;
        }
    }
}
