package ivorius.psychedelicraft.core;

import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("Psychedelicraft")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions("ivorius.psychedelicraft.core")
public final class PsychedelicraftCore implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        boolean client = FMLLaunchHandler.side() == Side.CLIENT;
        List<String> transformers = new ArrayList<>();
        Collections.addAll(transformers, "ivorius.psychedelicraft.core.FurnaceRecipeTransformer",
                "ivorius.psychedelicraft.core.PlayerMechanicsTransformer");
        if (client) {
            Collections.addAll(transformers, "ivorius.psychedelicraft.core.SmokingPoseTransformer",
                    "ivorius.psychedelicraft.core.ClientEffectsTransformer",
                    "ivorius.psychedelicraft.core.CameraEffectsTransformer",
                    "ivorius.psychedelicraft.core.WorldGeometryTransformer",
                    "ivorius.psychedelicraft.core.GlitchLightingTransformer",
                    "ivorius.psychedelicraft.core.EntityGeometryTransformer",
                    "ivorius.psychedelicraft.core.ItemGeometryTransformer",
                    "ivorius.psychedelicraft.core.TileGeometryTransformer");
        }
        Collections.addAll(transformers,
                "ivorius.psychedelicraft.core.VillagerCareersTransformer",
                "ivorius.psychedelicraft.core.WaterloggingTransformer",
                "ivorius.psychedelicraft.core.OceanWaterloggingTransformer",
                "ivorius.psychedelicraft.core.BoatFlotationTransformer",
                "ivorius.psychedelicraft.core.BubbleColumnTransformer",
                "ivorius.psychedelicraft.core.LatticePistonTransformer",
                "ivorius.psychedelicraft.core.BeeNestTreeTransformer",
                "ivorius.psychedelicraft.core.TrayRayTraceTransformer",
                "ivorius.psychedelicraft.core.ExplosionDropTransformer",
                "ivorius.psychedelicraft.core.PlantMovementTransformer",
                "ivorius.psychedelicraft.core.WeightedPressurePlateTransformer",
                "ivorius.psychedelicraft.core.ProjectileGatewayTransformer",
                "ivorius.psychedelicraft.core.CampfireProjectileTransformer",
                "ivorius.psychedelicraft.core.TridentTransformer",
                "ivorius.psychedelicraft.core.MooshroomVariantsTransformer",
                "ivorius.psychedelicraft.core.ZombieConversionTransformer",
                "ivorius.psychedelicraft.core.TreasureMapTransformer",
                "ivorius.psychedelicraft.core.ConduitPowerTransformer",
                "ivorius.psychedelicraft.core.SoulFireTransformer",
                "ivorius.psychedelicraft.core.NetherSurfaceTransformer");
        if (client) transformers.add("ivorius.psychedelicraft.core.IntegratedShutdownTransformer");
        return transformers.toArray(new String[0]);
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
