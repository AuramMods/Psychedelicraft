package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ContextualHallucinationsTest {
    @Test
    public void naturalSelectionRespectsCannabisAndAvoidsADuplicateHead() {
        assertSame(HallucinationType.RASTA_HEAD, ContextualHallucinations.pickNatural(
                rolls(false, 0.05F), 0.4F, 0.41, false));
        assertSame(HallucinationType.MULTIPLE_ENTITY, ContextualHallucinations.pickNatural(
                rolls(false, 0.05F, 0.1F), 0.4F, 0.41, true));
        assertSame(HallucinationType.SINGLE_ENTITY, ContextualHallucinations.pickNatural(
                rolls(false, 0.05F, 0.7F), 0.4F, 0.4, false));
    }

    @Test
    public void identitySwapsRequireTheAuthoredStrength() {
        assertSame(HallucinationType.SINGLE_ENTITY, ContextualHallucinations.pickNatural(
                rolls(true, 0.6F, 0.7F), 0.79F, 0, false));
        assertSame(HallucinationType.HOSTILE_VILLAGERS, ContextualHallucinations.pickNatural(
                rolls(true, 0.6F, 0.7F), 0.8F, 0, false));
        assertSame(HallucinationType.FRIENDLY_ZOMBIES, ContextualHallucinations.pickNatural(
                rolls(false, 0.6F, 0.7F), 0.8F, 0, false));
    }

    @Test
    public void identitySwapPreservesVisibleStateOnADetachedSubstitute() {
        Bootstrap.register();
        EntityZombie original = new EntityZombie(new RenderWorld());
        original.setCustomNameTag("Named zombie");
        original.setAlwaysRenderNameTag(true);
        original.setLeftHanded(true);
        original.setChild(true);
        original.setGlowing(true);
        original.setFire(4);
        ItemStack helmet = new ItemStack(Items.GOLDEN_HELMET).setStackDisplayName("Tagged helmet");
        original.setItemStackToSlot(EntityEquipmentSlot.HEAD, helmet);

        IdentitySwapHallucination swap = new IdentitySwapHallucination(HallucinationType.FRIENDLY_ZOMBIES);
        EntityVillager substitute = (EntityVillager) swap.disguise(original);
        assertNotEquals(original.getEntityId(), substitute.getEntityId());
        assertEquals(original.getUniqueID(), substitute.getUniqueID());
        assertEquals("Named zombie", substitute.getCustomNameTag());
        assertTrue(substitute.getAlwaysRenderNameTag());
        assertTrue(substitute.isLeftHanded());
        assertTrue(substitute.isSilent());
        assertFalse(original.isSilent());
        assertTrue(substitute.isChild());
        assertTrue(substitute.isGlowing());
        assertTrue(substitute.isBurning());
        ItemStack copiedHelmet = substitute.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        assertNotSame(helmet, copiedHelmet);
        assertTrue(ItemStack.areItemStacksEqual(helmet, copiedHelmet));

        original.extinguish();
        original.setGlowing(false);
        assertSame(substitute, swap.disguise(original));
        assertFalse(substitute.isGlowing());
        assertFalse(substitute.isBurning());
    }

    @Test
    public void preservesTheDonorIdsAndResolvesLegacyAliases() {
        for (String id : new String[]{"allay", "bee", "axolotl", "camel", "dolphin", "warden", "glow_squid",
                "cod", "salmon", "tropical_fish"}) {
            assertNotNull(id, ApparitionModels.forId(new ResourceLocation(id)));
        }
        assertNull(ApparitionModels.forId(new ResourceLocation("anothermod", "allay")));
        assertEquals(new ResourceLocation("snowman"), ApparitionPool.legacyId(new ResourceLocation("snow_golem")));
        assertEquals(new ResourceLocation("villager_golem"), ApparitionPool.legacyId(new ResourceLocation("iron_golem")));
    }

    private static Random rolls(boolean choice, float... floats) {
        return new Random() {
            private int index;

            @Override
            public float nextFloat() {
                return floats[index++];
            }

            @Override
            public boolean nextBoolean() {
                return choice;
            }
        };
    }

    private static final class RenderWorld extends World {
        RenderWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), true);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return false;
        }
    }
}
