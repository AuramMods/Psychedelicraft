package ivorius.psychedelicraft.world.gen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import java.util.List;

/** The native weighted list and its expanded sampling list must be updated together. */
public interface VillagePoolExtension {
    List<Pair<StructurePoolElement,Integer>> psychedelicraft$entries();
    void psychedelicraft$append(ResourceLocation sourceId,StructureTemplatePool source);
}
