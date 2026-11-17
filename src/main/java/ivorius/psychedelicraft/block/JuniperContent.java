package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.world.level.block.*;
/** Register the family's vanilla integrations on the common lifecycle work queue. */
public final class JuniperContent {
    private JuniperContent(){}
    public static void setup(){
        ((FlowerPotBlock)Blocks.FLOWER_POT).addPlant(Psychedelicraft.id("juniper_sapling"),PSBlocks.POTTED_JUNIPER_SAPLING);
        PSItems.JUNIPER_BOAT.get().registerDispenser();PSItems.JUNIPER_CHEST_BOAT.get().registerDispenser();
        FireBlock fire=(FireBlock)Blocks.FIRE;
        for(var log:java.util.List.of(PSBlocks.JUNIPER_LOG,PSBlocks.JUNIPER_WOOD,PSBlocks.STRIPPED_JUNIPER_LOG,PSBlocks.STRIPPED_JUNIPER_WOOD))fire.setFlammable(log.get(),5,5);
        for(var leaf:java.util.List.of(PSBlocks.JUNIPER_LEAVES,PSBlocks.FRUITING_JUNIPER_LEAVES))fire.setFlammable(leaf.get(),30,60);
        for(var wood:java.util.List.of(PSBlocks.JUNIPER_PLANKS,PSBlocks.JUNIPER_STAIRS,PSBlocks.JUNIPER_SLAB,PSBlocks.JUNIPER_FENCE,PSBlocks.JUNIPER_FENCE_GATE,PSBlocks.LATTICE,PSBlocks.WINE_GRAPE_LATTICE,PSBlocks.MORNING_GLORY_LATTICE,PSBlocks.DRYING_TABLE,PSBlocks.BOTTLE_RACK))fire.setFlammable(wood.get(),5,20);
    }
}
