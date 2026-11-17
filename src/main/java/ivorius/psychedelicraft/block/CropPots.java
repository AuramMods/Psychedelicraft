/* Copyright (c) 2014, Lukas Tenbrink. Modern flower-pot integration by Sollace. */
package ivorius.psychedelicraft.block;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;

/** Native decorative pots: planting/recovery use Minecraft's normal block interaction. */
public final class CropPots {
    private CropPots() {}
    public static void setup() {
        FlowerPotBlock empty = (FlowerPotBlock) Blocks.FLOWER_POT;
        empty.addPlant(PSBlocks.CANNABIS.getId(), PSBlocks.POTTED_CANNABIS);
        empty.addPlant(PSBlocks.HOP.getId(), PSBlocks.POTTED_HOP);
        empty.addPlant(PSBlocks.TOBACCO.getId(), PSBlocks.POTTED_TOBACCO);
        empty.addPlant(PSBlocks.COCA.getId(), PSBlocks.POTTED_COCA);
        empty.addPlant(PSBlocks.COFFEA.getId(), PSBlocks.POTTED_COFFEA);
    }
}
