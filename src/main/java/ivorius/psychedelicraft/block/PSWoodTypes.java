package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/** Native wood/set registration supplies signs, doors, buttons and gate sounds. */
public final class PSWoodTypes {
    public static final BlockSetType JUNIPER_SET=BlockSetType.register(new BlockSetType(Psychedelicraft.id("juniper").toString()));
    public static final WoodType JUNIPER=WoodType.register(new WoodType(Psychedelicraft.id("juniper").toString(),JUNIPER_SET));
    private PSWoodTypes(){}
}
