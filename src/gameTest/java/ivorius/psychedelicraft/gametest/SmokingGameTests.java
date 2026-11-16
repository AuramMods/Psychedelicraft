package ivorius.psychedelicraft.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.item.*;
import ivorius.psychedelicraft.particle.PSParticles;
import ivorius.psychedelicraft.particle.SmokeParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("psychedelicrafttests")
@PrefixGameTestTemplate(false)
public final class SmokingGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static FakePlayer player(GameTestHelper helper){
        FakePlayer player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"SmokingTest"));
        player.setPos(helper.absolutePos(POS).getCenter());player.setGameMode(GameType.SURVIVAL);player.getAbilities().invulnerable=true;
        player.getFoodData().setFoodLevel(20);player.getFoodData().setSaturation(0);return player;
    }
    private static ItemStack draw(FakePlayer player,ItemStack stack,InteractionHand hand){
        var properties=DrugCapability.get(player);while(properties.isBreathingSmoke())properties.tick(1,null);
        player.setItemInHand(hand,stack);
        check(stack.getItem().use(player.level(),player,hand).getResult().consumesAction(),"draw starts through native use");
        ItemStack result=stack.getItem().finishUsingItem(stack,player.level(),player);player.setItemInHand(hand,result);player.stopUsingItem();return result;
    }
    private static int count(FakePlayer player,Item item){int count=0;for(int slot=0;slot<player.getInventory().getContainerSize();slot++)if(player.getInventory().getItem(slot).is(item))count+=player.getInventory().getItem(slot).getCount();return count;}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}

    @GameTest(template="empty") public static void freshStacksSeparateOnePartialAndPreserveItsSavedData(GameTestHelper helper){
        FakePlayer player=player(helper);ItemStack fresh=new ItemStack(PSItems.JOINT.get(),32);
        fresh.setHoverName(Component.literal("Saved joint"));fresh.getOrCreateTag().putInt("Sample",37);
        player.getInventory().add(fresh.copy());player.getInventory().add(fresh.copy());
        check(player.getInventory().getItem(0).getCount()==64&&player.getInventory().getItem(1).isEmpty(),"native inventory merges fresh joints to 64");
        ItemStack remaining=draw(player,player.getInventory().getItem(0),InteractionHand.MAIN_HAND);
        check(remaining.getCount()==63&&remaining.getDamageValue()==0,"63 fresh joints remain in hand");
        ItemStack partial=player.getInventory().removeItem(1,1);check(partial.getCount()==1&&partial.getDamageValue()==1&&partial.getMaxStackSize()==1,"one partial is separate and unstackable");
        ItemStack restored=ItemStack.of(partial.save(new CompoundTag()));
        check(restored.getTag().getInt("Sample")==37&&restored.getHoverName().getString().equals("Saved joint"),"custom data and name survive wear/save");
        check(DrugCapability.get(player).isBreathingSmoke()&&!PSItems.JOINT.get().use(player.level(),player,InteractionHand.MAIN_HAND).getResult().consumesAction(),"breathing prevents another draw");
        check(draw(player,restored,InteractionHand.OFF_HAND).isEmpty()&&count(player,PSItems.JOINT.get())==63,"second draw consumes only the partial");helper.succeed();
    }
    @GameTest(template="empty") public static void fullInventoryDropsOneOffhandPartialAndCreativeKeepsFreshStack(GameTestHelper helper){
        FakePlayer player=player(helper);for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));
        ItemStack remaining=draw(player,new ItemStack(PSItems.PEYOTE_JOINT.get(),3),InteractionHand.OFF_HAND);
        check(remaining.getCount()==2&&remaining.getDamageValue()==0,"offhand keeps two fresh peyote joints");
        var drops=helper.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(player.blockPosition()).inflate(2),item->item.getItem().is(PSItems.PEYOTE_JOINT.get()));
        check(drops.size()==1&&drops.get(0).getItem().getCount()==1&&drops.get(0).getItem().getDamageValue()==1,"full inventory drops exactly one partial");drops.forEach(Entity::discard);
        player.getInventory().clearContent();player.setGameMode(GameType.CREATIVE);
        ItemStack creative=draw(player,new ItemStack(PSItems.JOINT.get(),5),InteractionHand.MAIN_HAND);
        check(creative.getCount()==5&&creative.getDamageValue()==0&&count(player,PSItems.JOINT.get())==5,"creative draw preserves the entire fresh stack");helper.succeed();
    }
    @GameTest(template="empty") public static void nicotineRestoresAcceptedSaturationAndCigarBreaksOnThirdDraw(GameTestHelper helper){
        FakePlayer player=player(helper);
        check(draw(player,new ItemStack(PSItems.CIGARETTE.get()),InteractionHand.MAIN_HAND).isEmpty(),"cigarette lasts one draw");
        check(player.getFoodData().getSaturationLevel()==2,"cigarette restores two saturation");
        ItemStack cigar=new ItemStack(PSItems.CIGAR.get());
        for(int draw=1;draw<=3;draw++){
            player.getFoodData().setSaturation(0);cigar=draw(player,cigar,InteractionHand.OFF_HAND);
            check(player.getFoodData().getSaturationLevel()==4,"each cigar draw restores four saturation");
            check(draw==3?cigar.isEmpty():cigar.getCount()==1&&cigar.getDamageValue()==draw,"cigar wears once per draw and breaks at three");
        }
        helper.succeed();
    }
    @GameTest(template="empty") public static void nondamageableSmokingStackStillConsumesOneItem(GameTestHelper helper){
        FakePlayer player=player(helper);ItemStack stack=new ItemStack(PSItems.JOINT.get(),2);stack.getOrCreateTag().putBoolean("Unbreakable",true);
        ItemStack result=draw(player,stack,InteractionHand.MAIN_HAND);
        check(result.getCount()==1&&count(player,PSItems.JOINT.get())==1,"accepted non-damageable fallback consumes one joint without creating a reusable partial");helper.succeed();
    }
    @GameTest(template="empty") public static void pipeAndBongUseEveryAcceptedIngredientAndItsOwnData(GameTestHelper helper){
        FakePlayer player=player(helper);Item[] ingredients={PSItems.DRIED_CANNABIS_BUDS.get(),PSItems.DRIED_TOBACCO.get(),PSItems.DRIED_BELLADONNA_LEAF.get(),PSItems.DRIED_JIMSONWEED_LEAF.get(),PSItems.HARMONIUM.get()};
        String[] types={"cannabis","tobacco","atropine","atropine","harmonium"};
        for(boolean bong:new boolean[]{false,true})for(int ingredient=0;ingredient<ingredients.length;ingredient++){
            player.getInventory().clearContent();DrugCapability.get(player).fromNbt(new CompoundTag());player.getFoodData().setSaturation(0);
            ItemStack supplied=new ItemStack(ingredients[ingredient],2);if(ingredient==4)HarmoniumItem.setColor(supplied,0x22aa77);
            player.getInventory().setItem(1,supplied);ItemStack tool=draw(player,new ItemStack(bong?PSItems.BONG.get():PSItems.SMOKING_PIPE.get()),InteractionHand.MAIN_HAND);
            check(tool.getDamageValue()==1&&supplied.getCount()==1,"one ingredient and one tool use consumed");
            var doses=DrugCapability.get(player).toNbt().getList("drugInfluences",Tag.TAG_COMPOUND);check(doses.size()==1,"one pending dose");CompoundTag dose=doses.getCompound(0);
            check(dose.getString("drugType").equals("psychedelicraft:"+types[ingredient])&&dose.getInt("delay")== (bong?0:20),"accepted ingredient and tool onset: "+dose);
            double expected=(bong?new double[]{.2,.6,.4,.1,.9}:new double[]{.25,.8,.9,.2,.65})[ingredient];
            check(dose.getDouble("maxInfluence")==expected,"accepted ingredient dose");
            if(ingredient==4)check(dose.getInt("color")==0x22aa77&&HarmoniumItem.getColor(supplied)==0x22aa77,"harmonium color preserved without modifying remaining ingredient");
            check(player.getFoodData().getSaturationLevel()==(ingredient==1?2:0),"only nicotine restores two saturation");
        }
        helper.succeed();
    }
    @GameTest(template="empty") public static void absentOrRemovedIngredientCannotWearToolOrCreateDose(GameTestHelper helper){
        FakePlayer player=player(helper);ItemStack tool=new ItemStack(PSItems.BONG.get());player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        check(!tool.getItem().use(player.level(),player,InteractionHand.MAIN_HAND).getResult().consumesAction(),"empty bong refuses use");
        player.getInventory().setItem(1,new ItemStack(PSItems.DRIED_TOBACCO.get()));
        check(tool.getItem().use(player.level(),player,InteractionHand.MAIN_HAND).getResult().consumesAction(),"loaded bong starts");player.getInventory().setItem(1,ItemStack.EMPTY);
        tool.getItem().finishUsingItem(tool,player.level(),player);
        check(tool.getDamageValue()==0&&DrugCapability.get(player).toNbt().getList("drugInfluences",Tag.TAG_COMPOUND).isEmpty()&&!DrugCapability.get(player).isBreathingSmoke(),"mid-draw ingredient removal creates neither dose nor wear");helper.succeed();
    }
    @GameTest(template="empty") public static void harmoniumDyeAndParticleNetworkPreserveCustomData(GameTestHelper helper){
        var menu=new AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}};
        var grid=new TransientCraftingContainer(menu,2,2);ItemStack source=new ItemStack(PSItems.HARMONIUM.get(),4);source.setHoverName(Component.literal("Named"));source.getOrCreateTag().putInt("Sample",37);
        grid.setItem(0,source);grid.setItem(1,new ItemStack(Items.BLUE_DYE));
        var recipe=helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,helper.getLevel()).orElseThrow();
        ItemStack result=recipe.assemble(grid,helper.getLevel().registryAccess());
        check(result.getCount()==1&&HarmoniumItem.getColor(result)==HarmoniumItem.packColor(DyeColor.BLUE.getTextureDiffuseColors()),"native dye recipe colors one harmonium");
        check(source.getCount()==4&&!HarmoniumItem.hasColor(source)&&result.getTag().getInt("Sample")==37&&result.getHoverName().getString().equals("Named"),"dye result retains custom data and leaves inputs unchanged");
        grid.setItem(2,new ItemStack(PSItems.HARMONIUM.get()));check(!recipe.matches(grid,helper.getLevel()),"multiple harmonium inputs refused");
        var effect=new SmokeParticleOptions(0x22aa77,2.5F);FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{effect.writeToNetwork(buffer);var restored=SmokeParticleOptions.DESERIALIZER.fromNetwork(PSParticles.EXHALED_SMOKE.get(),buffer);
            check(restored.getType()==PSParticles.EXHALED_SMOKE.get()&&restored.getColor().equals(effect.getColor())&&restored.getScale()==2.5F,"native particle packet retains type, RGB and size");
            var encoded=SmokeParticleOptions.CODEC.encodeStart(JsonOps.INSTANCE,effect).getOrThrow(false,message->{});
            check(SmokeParticleOptions.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow(false,message->{}).getType()==PSParticles.EXHALED_SMOKE.get(),"particle codec retains custom type");
        }finally{buffer.release();}helper.succeed();
    }
    @GameTest(template="empty") public static void nativeFurnaceConsumptionDosesNearbyPlayersExactlyOnce(GameTestHelper helper){
        FakePlayer nearby=player(helper),distant=player(helper),spectator=player(helper);
        distant.setPos(nearby.position().add(12,0,0));spectator.setGameMode(GameType.SPECTATOR);
        var level=helper.getLevel();level.addNewPlayer(nearby);level.addNewPlayer(distant);level.addNewPlayer(spectator);
        for(int i=0;i<10;i++)ForgeHooks.getBurnTime(new ItemStack(PSItems.CIGAR.get()),RecipeType.SMELTING);
        check(nearby.getFoodData().getSaturationLevel()==0,"burn-time queries do not dose players");
        helper.setBlock(POS,Blocks.FURNACE);var furnace=(AbstractFurnaceBlockEntity)level.getBlockEntity(helper.absolutePos(POS));
        furnace.setItem(0,new ItemStack(PSItems.COFFEA_CHERRIES.get()));furnace.setItem(1,new ItemStack(PSItems.CIGAR.get()));
        helper.runAtTickTime(10,()->{
            try{
                check(furnace.getItem(1).isEmpty()&&level.getBlockState(helper.absolutePos(POS)).getValue(net.minecraft.world.level.block.AbstractFurnaceBlock.LIT),"real furnace consumed one cigar fuel");
                check(nearby.getFoodData().getSaturationLevel()==4&&DrugCapability.get(nearby).toNbt().getList("drugInfluences",Tag.TAG_COMPOUND).size()==1,"nearby player receives one cigar dose and four saturation");
                check(distant.getFoodData().getSaturationLevel()==0&&spectator.getFoodData().getSaturationLevel()==0,"outside radius and spectator players are excluded");helper.succeed();
            }finally{for(FakePlayer player:new FakePlayer[]{nearby,distant,spectator})level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);}
        });
    }
}
