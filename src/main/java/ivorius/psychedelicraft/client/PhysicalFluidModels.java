package ivorius.psychedelicraft.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.physical.PhysicalFluidBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelFluid;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class PhysicalFluidModels implements IBakedModel {
    private static final ModelResourceLocation MODEL = new ModelResourceLocation(Psychedelicraft.id("physical_fluid"), "normal");

    private final LoadingCache<Appearance, IBakedModel> models = CacheBuilder.newBuilder().maximumSize(64)
            .build(new CacheLoader<Appearance, IBakedModel>() {
                @Override
                public IBakedModel load(Appearance key) {
                    ModelFluid model = new ModelFluid(new Fluid("psychedelicraft_render", key.still, key.flowing, key.color));
                    return model.bake(model.getDefaultState(), DefaultVertexFormats.BLOCK, PhysicalFluidModels::sprite);
                }
            });

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (Fluid fluid : PSFluids.all()) {
            ModelLoader.setCustomStateMapper(fluid.getBlock(), block -> {
                java.util.Map<IBlockState, ModelResourceLocation> states = new java.util.HashMap<>();
                for (IBlockState state : block.getBlockState().getValidStates()) states.put(state, MODEL);
                return states;
            });
        }
    }

    @SubscribeEvent
    public static void bakeModels(ModelBakeEvent event) {
        event.getModelRegistry().putObject(MODEL, new PhysicalFluidModels());
    }

    @SubscribeEvent
    public static void fogDensity(EntityViewRenderEvent.FogDensity event) {
        if (!(event.getState().getBlock() instanceof PhysicalFluidBlock)
                || !((PhysicalFluidBlock) event.getState().getBlock()).isWaterLike()) return;
        float density = 0.1F;
        if (event.getEntity() instanceof EntityLivingBase) {
            EntityLivingBase entity = (EntityLivingBase) event.getEntity();
            if (entity.isPotionActive(MobEffects.BLINDNESS)) return;
            density = entity.isPotionActive(MobEffects.WATER_BREATHING) ? 0.01F
                    : Math.max(0.01F, density - EnchantmentHelper.getRespirationModifier(entity) * 0.03F);
        }
        GlStateManager.setFog(GlStateManager.FogMode.EXP);
        event.setDensity(density);
        event.setCanceled(true);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long random) {
        if (state == null || !(state.getBlock() instanceof PhysicalFluidBlock)) return Collections.emptyList();
        FluidStack contents = state instanceof IExtendedBlockState
                ? ((IExtendedBlockState) state).getValue(PhysicalFluidBlock.CONTENTS) : null;
        Fluid fluid = ((PhysicalFluidBlock) state.getBlock()).getFluid();
        if (contents == null) contents = new FluidStack(fluid, Fluid.BUCKET_VOLUME);
        Appearance key = new Appearance(fluid.getStill(contents), fluid.getFlowing(contents), fluid.getColor(contents));
        return models.getUnchecked(key).getQuads(state, side, random);
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture.toString());
    }

    @Override public boolean isAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return false; }
    @Override public boolean isBuiltInRenderer() { return false; }
    @Override public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return sprite(FluidRegistry.WATER.getStill());
    }

    private static final class Appearance {
        private final ResourceLocation still;
        private final ResourceLocation flowing;
        private final int color;

        private Appearance(ResourceLocation still, ResourceLocation flowing, int color) {
            this.still = still;
            this.flowing = flowing;
            this.color = color;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Appearance)) return false;
            Appearance key = (Appearance) other;
            return color == key.color && still.equals(key.still) && flowing.equals(key.flowing);
        }

        @Override
        public int hashCode() {
            return Objects.hash(still, flowing, color);
        }
    }
}
