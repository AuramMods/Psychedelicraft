package ivorius.psychedelicraft.gametest;

import com.mojang.authlib.GameProfile;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("psychedelicrafttests")
@PrefixGameTestTemplate(false)
public final class CropPotGameTests {
    private static final BlockPos POS = new BlockPos(1, 2, 1);
    private record Plant(Item item, Block pot) {}
    private static List<Plant> crops() {
        return List.of(new Plant(PSItems.CANNABIS_SEEDS.get(), PSBlocks.POTTED_CANNABIS.get()),
                new Plant(PSItems.HOP_SEEDS.get(), PSBlocks.POTTED_HOP.get()),
                new Plant(PSItems.TOBACCO_SEEDS.get(), PSBlocks.POTTED_TOBACCO.get()),
                new Plant(PSItems.COCA_SEEDS.get(), PSBlocks.POTTED_COCA.get()),
                new Plant(PSItems.COFFEA_CHERRIES.get(), PSBlocks.POTTED_COFFEA.get()));
    }
    private static List<Plant> all() {
        List<Plant> plants = new ArrayList<>(crops());
        plants.addAll(List.of(new Plant(PSItems.MORNING_GLORY_SEEDS.get(), PSBlocks.POTTED_MORNING_GLORY.get()),
                new Plant(PSItems.JUNIPER_SAPLING.get(), PSBlocks.POTTED_JUNIPER_SAPLING.get()),
                new Plant(Items.TORCHFLOWER, Blocks.POTTED_TORCHFLOWER), new Plant(Items.CORNFLOWER, Blocks.POTTED_CORNFLOWER),
                new Plant(Items.LILY_OF_THE_VALLEY, Blocks.POTTED_LILY_OF_THE_VALLEY), new Plant(Items.WITHER_ROSE, Blocks.POTTED_WITHER_ROSE)));
        return plants;
    }
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "PotTest"));
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.setPos(Vec3.atCenterOf(h.absolutePos(POS)));
        h.setBlock(POS.below(), Blocks.STONE);
        return player;
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    @GameTest(template="empty") public static void allAcceptedPottedPlantsUseNativeInsertionAndRecovery(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(POS); var player = player(h);
        for (Plant plant : all()) {
            h.setBlock(POS, Blocks.FLOWER_POT);
            var input = new ItemStack(plant.item(), 3); player.setItemInHand(InteractionHand.MAIN_HAND, input);
            var empty = level.getBlockState(pos);
            check(empty.use(level, player, InteractionHand.MAIN_HAND, hit(pos)).consumesAction(), "native pot use");
            check(level.getBlockState(pos).is(plant.pot()) && input.getCount() == 2, "one seed/plant consumed for " + plant.item());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit(pos));
            check(level.getBlockState(pos).is(Blocks.FLOWER_POT), "native pot restored");
            check(player.getMainHandItem().is(plant.item()) && player.getMainHandItem().getCount() == 1, "exact original item recovered");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void pottedCropsPickAndBreakIntoOnePotAndOnePlant(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(POS); var player = player(h);
        for (Plant plant : crops()) {
            h.setBlock(POS, plant.pot()); var state = level.getBlockState(pos);
            var pick = plant.pot().getCloneItemStack(level, pos, state);
            check(pick.is(plant.item()) && pick.getCount() == 1, "pick recovers planting item");
            check(!state.isRandomlyTicking(), "decorative crop is not a growing field plant");
            var drops = Block.getDrops(state, level, pos, null, player, new ItemStack(Items.DIAMOND_PICKAXE));
            check(drops.size() == 2 && drops.stream().filter(s -> s.is(Items.FLOWER_POT)).mapToInt(ItemStack::getCount).sum() == 1
                    && drops.stream().filter(s -> s.is(plant.item())).mapToInt(ItemStack::getCount).sum() == 1, "native loot conserves pot and plant");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void heldObjectsAndCreativePlantingFollowNativePotRules(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(POS); var player = player(h);
        for (Plant plant : crops()) {
            h.setBlock(POS, plant.pot()); player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 4));
            level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit(pos));
            check(level.getBlockState(pos).is(Blocks.FLOWER_POT) && player.getMainHandItem().getCount() == 4, "held object is not consumed during removal");
            check(player.getInventory().countItem(plant.item()) == 1, "removed plant goes into inventory once");
            player.setGameMode(GameType.CREATIVE); var input = new ItemStack(plant.item(), 3); player.setItemInHand(InteractionHand.MAIN_HAND, input);
            level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit(pos));
            check(level.getBlockState(pos).is(plant.pot()) && input.getCount() == 3, "native creative insertion keeps input");
            player.setGameMode(GameType.SURVIVAL);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void unlistedFieldCropsDoNotAcquirePotForms(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(POS); var player = player(h);
        for (var item : List.of(PSItems.AGAVE_LEAF.get(), PSItems.JIMSONWEED_SEEDS.get(), PSItems.BELLADONNA_SEEDS.get(), PSItems.TOMATO_SEEDS.get())) {
            h.setBlock(POS, Blocks.FLOWER_POT); var input = new ItemStack(item, 2); player.setItemInHand(InteractionHand.MAIN_HAND, input);
            level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, hit(pos));
            check(level.getBlockState(pos).is(Blocks.FLOWER_POT) && input.getCount() == 2, "unlisted seed leaves pot and input intact");
        }
        h.succeed();
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
