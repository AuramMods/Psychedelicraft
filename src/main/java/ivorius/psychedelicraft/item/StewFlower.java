package ivorius.psychedelicraft.item;

import net.minecraft.block.BlockFlower.EnumFlowerType;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public enum StewFlower {
    DANDELION(EnumFlowerType.DANDELION, MobEffects.SATURATION, 7),
    POPPY(EnumFlowerType.POPPY, MobEffects.NIGHT_VISION, 100),
    BLUE_ORCHID(EnumFlowerType.BLUE_ORCHID, MobEffects.SATURATION, 7),
    ALLIUM(EnumFlowerType.ALLIUM, MobEffects.FIRE_RESISTANCE, 80),
    AZURE_BLUET(EnumFlowerType.HOUSTONIA, MobEffects.BLINDNESS, 160),
    RED_TULIP(EnumFlowerType.RED_TULIP, MobEffects.WEAKNESS, 180),
    ORANGE_TULIP(EnumFlowerType.ORANGE_TULIP, MobEffects.WEAKNESS, 180),
    WHITE_TULIP(EnumFlowerType.WHITE_TULIP, MobEffects.WEAKNESS, 180),
    PINK_TULIP(EnumFlowerType.PINK_TULIP, MobEffects.WEAKNESS, 180),
    OXEYE_DAISY(EnumFlowerType.OXEYE_DAISY, MobEffects.REGENERATION, 160),
    CORNFLOWER(() -> PSItems.CORNFLOWER, 0, MobEffects.JUMP_BOOST, 120),
    LILY_OF_THE_VALLEY(() -> PSItems.LILY_OF_THE_VALLEY, 0, MobEffects.POISON, 240),
    WITHER_ROSE(() -> PSItems.WITHER_ROSE, 0, MobEffects.WITHER, 160),
    TORCHFLOWER(() -> PSItems.TORCHFLOWER, 0, MobEffects.NIGHT_VISION, 100);

    private static final StewFlower[] FLOWERS = values();
    private final Supplier<Item> item;
    private final int metadata;
    private final Potion potion;
    private final int duration;

    StewFlower(EnumFlowerType type, Potion potion, int duration) {
        this(() -> Item.getItemFromBlock(type.getBlockType().getBlock()), type.getMeta(), potion, duration);
    }

    StewFlower(Supplier<Item> item, int metadata, Potion potion, int duration) {
        this.item = item;
        this.metadata = metadata;
        this.potion = potion;
        this.duration = duration;
    }

    public ItemStack ingredient() {
        return new ItemStack(item.get(), 1, metadata);
    }

    public ItemStack stew() {
        return SuspiciousStewItem.withEffect(createEffect());
    }

    public PotionEffect createEffect() {
        return new PotionEffect(potion, duration);
    }

    @Nullable
    public static StewFlower fromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (StewFlower flower : FLOWERS) {
            if (stack.getMetadata() == flower.metadata && stack.getItem() == flower.item.get()) return flower;
        }
        return null;
    }
}
