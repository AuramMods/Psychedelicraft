package ivorius.psychedelicraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
public final class PSTags {
    public static final TagKey<Item> PLACEABLE=ItemTags.create(Psychedelicraft.id("placeable"));
    public static final TagKey<Item> BOTTLES=ItemTags.create(Psychedelicraft.id("bottles"));
    private PSTags(){}
}
