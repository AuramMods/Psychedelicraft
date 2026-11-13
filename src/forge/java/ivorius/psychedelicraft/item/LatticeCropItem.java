package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.block.LatticeCropBlock;

final class LatticeCropItem extends WaterloggableBlockItem {
    LatticeCropItem(LatticeCropBlock block) {
        super(block);
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage & LatticeCropBlock.MAX_AGE;
    }
}
