package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.LatticeCropBlock;
import net.minecraft.item.ItemBlock;

final class LatticeCropItem extends ItemBlock {
    LatticeCropItem(LatticeCropBlock block) {
        super(block);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage & LatticeCropBlock.MAX_AGE;
    }
}
