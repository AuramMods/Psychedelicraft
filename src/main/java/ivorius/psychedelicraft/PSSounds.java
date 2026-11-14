package ivorius.psychedelicraft;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PSSounds {
    public static final SoundEvent BREATH = sound("entity.player.breath");
    public static final SoundEvent HEARTBEAT = sound("entity.player.heartbeat");
    public static final SoundEvent RIFT_JAR_TOGGLE = sound("block.rift_jar.toggle");
    public static final SoundEvent RIFT_JAR_OPEN = sound("block.rift_jar.open");
    public static final SoundEvent RIFT_JAR_CLOSE = sound("block.rift_jar.close");
    public static final SoundEvent BOTTLE_RACK_USE = sound("block.bottle_rack.use");
    public static final SoundEvent PLACED_DRINK_SET_DOWN = sound("block.placed_drink.set_down");
    public static final SoundEvent PAPER_BAG_INSERT = sound("item.paper_bag.insert");
    public static final SoundEvent PAPER_BAG_REMOVE = sound("item.paper_bag.remove");
    public static final SoundEvent SIGN_DYE = sound("block.sign.dye");
    public static final SoundEvent SIGN_INK = sound("block.sign.ink");
    public static final SoundEvent SIGN_GLOW_INK = sound("block.sign.glow_ink");
    public static final SoundEvent SIGN_WAX = sound("block.sign.wax");
    public static final SoundEvent WAXED_SIGN_INTERACT_FAIL = sound("block.sign.waxed_interact_fail");
    public static final SoundEvent AXE_STRIP = sound("item.axe.strip");
    public static final SoundEvent BEE_NEST_DRIP = sound("block.bee_nest.drip");
    public static final SoundEvent HONEY_DRINK = sound("item.honey_bottle.drink");
    public static final SoundEvent CROP_PLANT = sound("item.crop.plant");
    public static final SoundEvent DRUG_GENERIC = sound("drug.generic");

    private PSSounds() {
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = Psychedelicraft.id(name);
        return new SoundEvent(id).setRegistryName(id);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(BREATH, HEARTBEAT, RIFT_JAR_TOGGLE, RIFT_JAR_OPEN, RIFT_JAR_CLOSE,
                BOTTLE_RACK_USE, PLACED_DRINK_SET_DOWN, PAPER_BAG_INSERT, PAPER_BAG_REMOVE,
                SIGN_DYE, SIGN_INK, SIGN_GLOW_INK, SIGN_WAX, WAXED_SIGN_INTERACT_FAIL, AXE_STRIP, BEE_NEST_DRIP,
                HONEY_DRINK, CROP_PLANT, DRUG_GENERIC);
    }
}
