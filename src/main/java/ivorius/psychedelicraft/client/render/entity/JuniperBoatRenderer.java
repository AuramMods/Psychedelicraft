package ivorius.psychedelicraft.client.render.entity;
import com.mojang.datafixers.util.Pair;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
/** Original juniper skin on native boat/chest-boat geometry and water mask. */
public final class JuniperBoatRenderer extends BoatRenderer {
    private final ResourceLocation texture;
    public JuniperBoatRenderer(EntityRendererProvider.Context context,boolean chest){super(context,chest);texture=Psychedelicraft.id("textures/entity/"+(chest?"chest_boat/":"boat/")+"juniper.png");}
    @Override public Pair<ResourceLocation,ListModel<Boat>> getModelWithLocation(Boat boat){return Pair.of(texture,super.getModelWithLocation(boat).getSecond());}
}
