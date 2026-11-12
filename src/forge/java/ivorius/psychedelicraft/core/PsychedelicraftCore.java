package ivorius.psychedelicraft.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name("Psychedelicraft")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions("ivorius.psychedelicraft.core")
public final class PsychedelicraftCore implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"ivorius.psychedelicraft.core.FurnaceRecipeTransformer",
                "ivorius.psychedelicraft.core.PlayerMechanicsTransformer",
                "ivorius.psychedelicraft.core.SmokingPoseTransformer",
                "ivorius.psychedelicraft.core.ClientEffectsTransformer",
                "ivorius.psychedelicraft.core.CameraEffectsTransformer",
                "ivorius.psychedelicraft.core.WorldGeometryTransformer",
                "ivorius.psychedelicraft.core.EntityGeometryTransformer",
                "ivorius.psychedelicraft.core.ItemGeometryTransformer",
                "ivorius.psychedelicraft.core.VillagerCareersTransformer"};
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
