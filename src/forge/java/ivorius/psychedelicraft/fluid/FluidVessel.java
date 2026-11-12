package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import javax.annotation.Nullable;

public enum FluidVessel {
    MUG(500, 250, 32),
    CUP(250, 250, 32),
    CHALICE(200, 200, 32),
    SHOT_GLASS(40, 40, 8),
    BOTTLE(2000, 250, 32),
    SYRINGE(10, 10, 32),
    BOWL(50, 50, 32),
    GLASS_BOTTLE(125, 125, 32),
    BUCKET(1000, 0, 0);

    public final int capacity;
    public final int serving;
    public final int useTicks;

    FluidVessel(int capacity, int serving, int useTicks) {
        this.capacity = capacity;
        this.serving = serving;
        this.useTicks = useTicks;
    }

    boolean accepts(Fluid fluid) {
        if (this == SYRINGE) return PSFluids.isInjectable(fluid);
        if (this == BUCKET) return true;
        if (fluid == FluidRegistry.LAVA) return this == GLASS_BOTTLE;
        if (PSFluids.isInjectable(fluid)) return false;
        if (fluid == PSFluids.COFFEE) return this == CUP;
        return this != MUG || (fluid != PSFluids.COCA_TEA && fluid != PSFluids.CANNABIS_TEA
                && fluid != PSFluids.PEYOTE_JUICE && fluid != PSFluids.KAVA);
    }

    @Nullable
    public Item emptyItem() {
        switch (this) {
            case BUCKET: return Items.BUCKET;
            case BOWL: return Items.BOWL;
            case GLASS_BOTTLE: return Items.GLASS_BOTTLE;
            default: return null;
        }
    }

    Item taggedItem(ItemStack current) {
        switch (this) {
            case BUCKET: return PSItems.FILLED_BUCKET;
            case BOWL: return PSItems.FILLED_BOWL;
            case GLASS_BOTTLE: return PSItems.FILLED_GLASS_BOTTLE;
            default: return current.getItem();
        }
    }
}
