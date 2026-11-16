/* Copyright (c) 2014, Lukas Tenbrink. Native models/reload design by Sollace. */
package ivorius.psychedelicraft.client.render;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.HarmoniumItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.*;
import java.util.*;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class PlacedDrinkModels {
    private static volatile Map<ResourceLocation,Geometry> geometry=Map.of();
    public record Geometry(float height,float fluidOrigin){}
    private static Map<ResourceLocation,Geometry> load(ResourceManager resources){
        Map<ResourceLocation,Geometry> entries=new LinkedHashMap<>();String[] names={"wooden_mug","stone_cup","shot_glass","glass_chalice","bottle"};float[] heights={.25F,.25F,.2F,.4F,.5F};
        for(int i=0;i<names.length;i++)entries.put(Psychedelicraft.id(names[i]),new Geometry(heights[i],names[i].equals("glass_chalice")?3.5F:.5F));
        try(var reader=resources.getResource(Psychedelicraft.id("placeable_drinks.json")).orElseThrow().openAsReader()){
            for(JsonElement value:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values")){
                JsonObject entry=value.getAsJsonObject();ResourceLocation id=ResourceLocation.tryParse(GsonHelper.getAsString(entry,"id"));float height=GsonHelper.getAsFloat(entry,"height"),origin=GsonHelper.getAsFloat(entry,"fluid_origin");
                if(id!=null&&ForgeRegistries.ITEMS.containsKey(id)&&Float.isFinite(height)&&height>0&&Float.isFinite(origin))entries.put(id,new Geometry(height,origin));
            }
        }catch(IOException|RuntimeException error){LogUtils.getLogger().warn("Could not load placeable drink geometry",error);}return Map.copyOf(entries);
    }
    @SubscribeEvent public static void register(ModelEvent.RegisterAdditional event){geometry=load(Minecraft.getInstance().getResourceManager());geometry.keySet().forEach(id->{event.register(modelId(id,false));event.register(modelId(id,true));});}
    public static ResourceLocation modelId(ResourceLocation item,boolean fluid){return new ResourceLocation(item.getNamespace(),"item/"+item.getPath()+"_on_ground"+(fluid?"_fluid":""));}
    public static Geometry geometry(ItemStack stack){return geometry.getOrDefault(ForgeRegistries.ITEMS.getKey(stack.getItem()),new Geometry(.5F,0));}
    public static boolean hasGeometry(ItemStack stack){return geometry.containsKey(ForgeRegistries.ITEMS.getKey(stack.getItem()));}
    public static void render(ItemStack stack,BlockPos anchor,PoseStack pose,MultiBufferSource vertices,int light,int overlay){
        if(stack.isEmpty())return;var client=Minecraft.getInstance();var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        if(!geometry.containsKey(id)){pose.pushPose();pose.translate(.5,.125,.5);client.getItemRenderer().renderStatic(stack,net.minecraft.world.item.ItemDisplayContext.GROUND,light,overlay,pose,vertices,client.level,42);pose.popPose();return;}
        int tint=id.getPath().equals("bottle")||id.getPath().equals("molotov_cocktail")?HarmoniumItem.getColor(stack):0xffffff;
        draw(client.getModelManager().getModel(modelId(id,false)),stack,tint,pose,vertices,light,overlay);
        var handler=FluidVesselHandler.forCrafting(stack);var fluid=handler==null?net.minecraftforge.fluids.FluidStack.EMPTY:handler.getFluid();
        float fill=handler==null?0:fluid.getAmount()/(float)handler.getTankCapacity(0);
        if(fill>.01F){
            int color=IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
            if(fluid.getFluid()==Fluids.WATER&&client.level!=null)color=client.getBlockColors().getColor(Blocks.WATER.defaultBlockState(),client.level,anchor,0);
            pose.pushPose();float origin=geometry(stack).fluidOrigin()/16;pose.translate(0,origin,0);pose.scale(1,fill,1);pose.translate(0,-origin,0);
            draw(client.getModelManager().getModel(modelId(id,true)),ItemStack.EMPTY,color,pose,vertices,light,overlay);pose.popPose();
        }
    }
    private static void draw(BakedModel model,ItemStack stack,int color,PoseStack pose,MultiBufferSource vertices,int light,int overlay){
        VertexConsumer buffer=ItemRenderer.getFoilBufferDirect(vertices,RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS),true,stack.hasFoil());RandomSource random=RandomSource.create(42);
        float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
        for(Direction face:Direction.values()){random.setSeed(42);for(var quad:model.getQuads(null,face,random))buffer.putBulkData(pose.last(),quad,r,g,b,light,overlay);}
        random.setSeed(42);for(var quad:model.getQuads(null,null,random))buffer.putBulkData(pose.last(),quad,r,g,b,light,overlay);
    }
}
