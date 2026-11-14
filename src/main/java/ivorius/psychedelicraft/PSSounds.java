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
    public static final SoundEvent BEE_NEST_ENTER = sound("block.bee_nest.enter");
    public static final SoundEvent BEE_NEST_EXIT = sound("block.bee_nest.exit");
    public static final SoundEvent BEE_NEST_WORK = sound("block.bee_nest.work");
    public static final SoundEvent BEE_NEST_SHEAR = sound("block.bee_nest.shear");
    public static final SoundEvent BEE_LOOP = sound("entity.bee.loop");
    public static final SoundEvent BEE_LOOP_ANGRY = sound("entity.bee.loop_aggressive");
    public static final SoundEvent BEE_POLLINATE = sound("entity.bee.pollinate");
    public static final SoundEvent BEE_STING = sound("entity.bee.sting");
    public static final SoundEvent BEE_HURT = sound("entity.bee.hurt");
    public static final SoundEvent BEE_DEATH = sound("entity.bee.death");
    public static final SoundEvent HONEY_DRINK = sound("item.honey_bottle.drink");
    public static final SoundEvent CROP_PLANT = sound("item.crop.plant");
    public static final SoundEvent BERRY_BUSH_BREAK = sound("block.sweet_berry_bush.break");
    public static final SoundEvent BERRY_BUSH_PLACE = sound("block.sweet_berry_bush.place");
    public static final SoundEvent BERRY_BUSH_PICK = sound("block.sweet_berry_bush.pick_berries");
    public static final SoundEvent MOOSHROOM_CONVERT = sound("entity.mooshroom.convert");
    public static final SoundEvent MOOSHROOM_EAT = sound("entity.mooshroom.eat");
    public static final SoundEvent MOOSHROOM_MILK = sound("entity.mooshroom.milk");
    public static final SoundEvent MOOSHROOM_SUSPICIOUS_MILK = sound("entity.mooshroom.suspicious_milk");
    public static final SoundEvent BRUSH_GENERIC = sound("item.brush.brushing.generic");
    public static final SoundEvent BRUSH_SAND = sound("item.brush.brushing.sand");
    public static final SoundEvent BRUSH_SAND_COMPLETE = sound("item.brush.brushing.sand.complete");
    public static final SoundEvent SNIFFER_EGG_CRACK = sound("block.sniffer_egg.crack");
    public static final SoundEvent SNIFFER_EGG_HATCH = sound("block.sniffer_egg.hatch");
    public static final SoundEvent SNIFFER_EGG_PLOP = sound("block.sniffer_egg.plop");
    public static final SoundEvent SNIFFER_DEATH = sound("entity.sniffer.death");
    public static final SoundEvent SNIFFER_DIGGING = sound("entity.sniffer.digging");
    public static final SoundEvent SNIFFER_DIGGING_STOP = sound("entity.sniffer.digging_stop");
    public static final SoundEvent SNIFFER_DROP_SEED = sound("entity.sniffer.drop_seed");
    public static final SoundEvent SNIFFER_EAT = sound("entity.sniffer.eat");
    public static final SoundEvent SNIFFER_HAPPY = sound("entity.sniffer.happy");
    public static final SoundEvent SNIFFER_HURT = sound("entity.sniffer.hurt");
    public static final SoundEvent SNIFFER_IDLE = sound("entity.sniffer.idle");
    public static final SoundEvent SNIFFER_SCENTING = sound("entity.sniffer.scenting");
    public static final SoundEvent SNIFFER_SEARCHING = sound("entity.sniffer.searching");
    public static final SoundEvent SNIFFER_SNIFFING = sound("entity.sniffer.sniffing");
    public static final SoundEvent SNIFFER_STEP = sound("entity.sniffer.step");
    public static final SoundEvent DRUG_GENERIC = sound("drug.generic");
    public static final SoundEvent CONDUIT_ACTIVATE = sound("block.conduit.activate");
    public static final SoundEvent CONDUIT_DEACTIVATE = sound("block.conduit.deactivate");
    public static final SoundEvent CONDUIT_AMBIENT = sound("block.conduit.ambient");
    public static final SoundEvent CONDUIT_AMBIENT_SHORT = sound("block.conduit.ambient.short");
    public static final SoundEvent CONDUIT_ATTACK = sound("block.conduit.attack.target");

    public static final SoundEvent DROWNED_AMBIENT = sound("entity.drowned.ambient");
    public static final SoundEvent DROWNED_AMBIENT_WATER = sound("entity.drowned.ambient_water");
    public static final SoundEvent DROWNED_DEATH = sound("entity.drowned.death");
    public static final SoundEvent DROWNED_DEATH_WATER = sound("entity.drowned.death_water");
    public static final SoundEvent DROWNED_HURT = sound("entity.drowned.hurt");
    public static final SoundEvent DROWNED_HURT_WATER = sound("entity.drowned.hurt_water");
    public static final SoundEvent DROWNED_STEP = sound("entity.drowned.step");
    public static final SoundEvent DROWNED_SWIM = sound("entity.drowned.swim");

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
                BEE_NEST_ENTER, BEE_NEST_EXIT, BEE_NEST_WORK, BEE_NEST_SHEAR,
                BEE_LOOP, BEE_LOOP_ANGRY, BEE_POLLINATE, BEE_STING, BEE_HURT, BEE_DEATH,
                HONEY_DRINK, CROP_PLANT, DRUG_GENERIC, BERRY_BUSH_BREAK, BERRY_BUSH_PLACE, BERRY_BUSH_PICK,
                MOOSHROOM_CONVERT, MOOSHROOM_EAT, MOOSHROOM_MILK, MOOSHROOM_SUSPICIOUS_MILK,
                BRUSH_GENERIC, BRUSH_SAND, BRUSH_SAND_COMPLETE, SNIFFER_EGG_CRACK, SNIFFER_EGG_HATCH, SNIFFER_EGG_PLOP,
                SNIFFER_DEATH, SNIFFER_DIGGING, SNIFFER_DIGGING_STOP, SNIFFER_DROP_SEED, SNIFFER_EAT, SNIFFER_HAPPY,
                SNIFFER_HURT, SNIFFER_IDLE, SNIFFER_SCENTING, SNIFFER_SEARCHING, SNIFFER_SNIFFING, SNIFFER_STEP,
                CONDUIT_ACTIVATE, CONDUIT_DEACTIVATE, CONDUIT_AMBIENT, CONDUIT_AMBIENT_SHORT, CONDUIT_ATTACK,
                DROWNED_AMBIENT, DROWNED_AMBIENT_WATER, DROWNED_DEATH, DROWNED_DEATH_WATER,
                DROWNED_HURT, DROWNED_HURT_WATER, DROWNED_STEP, DROWNED_SWIM);
    }
}
