package ivorius.psychedelicraft.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.entity.drug.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.recipe.FluidAwareShapelessRecipe;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class AcquisitionRecipeGameTests {
    private record Workbench(ServerPlayer player, CraftingMenu menu) {}
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    private static Workbench workbench(GameTestHelper h){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"CraftTest"));
        p.connection=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"CraftConn")).connection;
        p.setGameMode(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(1,2,1)).getCenter());p.initInventoryMenu();
        var pos=h.absolutePos(new BlockPos(1,2,2));h.setBlock(new BlockPos(1,2,2),Blocks.CRAFTING_TABLE);
        p.gameMode.useItemOn(p,h.getLevel(),ItemStack.EMPTY,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        check(p.containerMenu instanceof CraftingMenu,"native workbench interaction opens crafting menu");
        return new Workbench(p,(CraftingMenu)p.containerMenu);
    }
    private static void clear(Workbench w){w.menu.setCarried(ItemStack.EMPTY);for(int i=1;i<=9;i++)w.menu.getSlot(i).set(ItemStack.EMPTY);}
    private static ItemStack take(Workbench w,Item expected){
        check(w.menu.getSlot(0).getItem().is(expected),"native result is "+expected);w.menu.clicked(0,0,ClickType.PICKUP,w.player);
        var output=w.menu.getCarried().copy();check(output.is(expected)&&output.getCount()==1,"native output click takes one result");w.menu.setCarried(ItemStack.EMPTY);return output;
    }
    private static ItemStack lavaGlass(int amount){return FluidVesselHandler.forCrafting(new ItemStack(Items.GLASS_BOTTLE)).replaceContents(new net.minecraftforge.fluids.FluidStack(Fluids.LAVA,amount));}
    private static FluidAwareShapelessRecipe obsidian(GameTestHelper h){return (FluidAwareShapelessRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation("psychedelicraft:obsidian_bottle")).orElseThrow();}
    @GameTest(template="empty") public static void muffinRecipeCraftsThroughTheNativeWorkbench(GameTestHelper h){
        var w=workbench(h);for(int i:new int[]{1,2,3,7,8,9})w.menu.getSlot(i).set(new ItemStack(PSItems.DRIED_CANNABIS_LEAF.get()));
        for(int i:new int[]{4,6})w.menu.getSlot(i).set(new ItemStack(Items.WHEAT));w.menu.getSlot(5).set(new ItemStack(Items.BLUE_DYE));
        check(w.menu.getSlot(0).getItem().isEmpty(),"wrong dye does not produce a muffin");w.menu.getSlot(5).set(new ItemStack(Items.LIGHT_BLUE_DYE));
        var muffin=take(w,PSItems.HASH_MUFFIN.get());for(int i=1;i<=9;i++)check(w.menu.getSlot(i).getItem().isEmpty(),"one of each shaped input consumed");
        w.player.getInventory().add(muffin);w.menu.broadcastChanges();var advancement=h.getLevel().getServer().getAdvancements().getAdvancement(new ResourceLocation("psychedelicraft:hash_a_muffin"));
        check(w.player.getAdvancements().getOrStartProgress(advancement).isDone(),"crafted acquisition earns the native muffin advancement");h.succeed();
    }
    @GameTest(template="empty") public static void oneNativeLavaBucketPoursExactlyEightGlassBottles(GameTestHelper h){
        var w=workbench(h);ItemStack source=new ItemStack(Items.LAVA_BUCKET);source.setHoverName(Component.literal("Named lava"));source.getOrCreateTag().putInt("custom",27);
        for(int i=0;i<8;i++){
            clear(w);w.menu.getSlot(1).set(source);w.menu.getSlot(2).set(new ItemStack(Items.GLASS_BOTTLE));
            var glass=take(w,PSItems.FILLED_GLASS_BOTTLE.get());var contents=FluidVesselHandler.forCrafting(glass).getFluid();
            check(contents.getFluid()==Fluids.LAVA&&contents.getAmount()==125,"one glass receives exactly 125 mB");
            source=w.menu.getSlot(1).getItem().copy();check(w.menu.getSlot(2).getItem().isEmpty(),"filled output consumes its target glass");
            var remaining=FluidVesselHandler.forCrafting(source).getFluid();check(remaining.getAmount()==1000-(i+1)*125,"source remainder conserves lava");
            check(source.getHoverName().getString().equals("Named lava")&&source.getTag().getInt("custom")==27,"native/partial/empty source keeps metadata");
        }
        check(source.is(Items.BUCKET),"eighth pour leaves one empty bucket");h.succeed();
    }
    @GameTest(template="empty") public static void quenchingRequiresFullLavaAndConsumesTheGlassExactlyOnce(GameTestHelper h){
        var w=workbench(h);for(int amount:new int[]{1,124}){clear(w);w.menu.getSlot(1).set(lavaGlass(amount));w.menu.getSlot(2).set(new ItemStack(Items.WATER_BUCKET));check(w.menu.getSlot(0).getItem().isEmpty(),"partial lava cannot create an obsidian bottle");}
        clear(w);w.menu.getSlot(1).set(new ItemStack(Items.WATER_BUCKET));w.menu.getSlot(2).set(lavaGlass(125));take(w,PSItems.OBSIDIAN_BOTTLE.get());
        check(w.menu.getSlot(1).getItem().is(Items.BUCKET)&&w.menu.getSlot(1).getItem().getCount()==1,"one water bucket returns one bucket");
        check(w.menu.getSlot(2).getItem().isEmpty()&&w.player.getInventory().countItem(Items.GLASS_BOTTLE)==0,"glass is incorporated into solid product, with no duplicate remainder");
        check(PSItems.OBSIDIAN_BOTTLE.get().getDefaultInstance().getMaxStackSize()==16,"accepted solid bottle stack size");h.succeed();
    }
    @GameTest(template="empty") public static void crushingAndCompactingUseAllNineOriginalMaterials(GameTestHelper h){
        var w=workbench(h);int dust=0;for(int i=0;i<9;i++){clear(w);w.menu.getSlot(1).set(new ItemStack(PSItems.OBSIDIAN_BOTTLE.get()));take(w,PSItems.OBSIDIAN_DUST.get());check(w.menu.getSlot(1).getItem().isEmpty(),"crushing consumes solid bottle");dust++;}
        clear(w);for(int i=1;i<=8;i++)w.menu.getSlot(i).set(new ItemStack(PSItems.OBSIDIAN_DUST.get()));check(w.menu.getSlot(0).getItem().isEmpty(),"eight dust cannot compact");
        w.menu.getSlot(9).set(new ItemStack(PSItems.OBSIDIAN_DUST.get()));take(w,Items.OBSIDIAN);for(int i=1;i<=9;i++)check(w.menu.getSlot(i).getItem().isEmpty(),"nine dust consumed for one block");check(dust==9,"nine solid bottles produced nine dust");h.succeed();
    }
    @GameTest(template="empty") public static void acquiredDustCompletesTheBathSaltsSyringeRecipe(GameTestHelper h){
        var w=workbench(h);w.menu.getSlot(1).set(new ItemStack(PSItems.OBSIDIAN_BOTTLE.get()));var dust=take(w,PSItems.OBSIDIAN_DUST.get());clear(w);
        w.menu.getSlot(1).set(new ItemStack(PSItems.SYRINGE.get()));w.menu.getSlot(2).set(new ItemStack(Items.LAVA_BUCKET));w.menu.getSlot(3).set(dust);var syringe=take(w,PSItems.SYRINGE.get());
        var fluid=FluidVesselHandler.forCrafting(syringe).getFluid();check(fluid.getFluid()==PSFluids.BATH_SALTS.get()&&fluid.getAmount()==10,"original recipe fills the ten-mB syringe");
        check(w.menu.getSlot(1).getItem().isEmpty()&&w.menu.getSlot(2).getItem().is(Items.BUCKET)&&w.menu.getSlot(3).getItem().isEmpty(),"syringe/powder consumed and empty bucket returned");
        var empty=syringe.finishUsingItem(h.getLevel(),w.player);check(FluidVesselHandler.forCrafting(empty).getFluid().isEmpty(),"native injection spends the actual syringe contents");
        var props=DrugCapability.get(w.player);for(int i=0;i<40;i++)props.tick(i);check(props.getActiveValue(DrugType.BATH_SALTS)>0,"crafted dose reaches the real effect system");h.succeed();
    }
    @GameTest(template="empty") public static void containerConsumptionSurvivesRecipeNetworkSyncAndFluidOnlyParsing(GameTestHelper h){
        var serializer=new FluidAwareShapelessRecipe.Serializer();var original=obsidian(h);var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{serializer.toNetwork(buffer,original);var restored=serializer.fromNetwork(original.getId(),buffer);check(restored.getFluidAwareIngredients().get(0).consumeContainer(),"network preserves whole-container input rule");
            var grid=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}@Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}},3,3);
            var glass=lavaGlass(125);var saved=glass.save(new CompoundTag());grid.setItem(0,glass);grid.setItem(1,new ItemStack(Items.WATER_BUCKET));
            check(restored.matches(grid,h.getLevel()),"synchronized recipe matches actual full glass");var remains=restored.getRemainingItems(grid);check(remains.get(0).isEmpty()&&remains.get(1).is(Items.BUCKET)&&glass.save(new CompoundTag()).equals(saved),"remainder calculation is read-only and consumes only specified container");
            var fluidOnly=serializer.fromJson(new ResourceLocation("psychedelicrafttests:fluid_only"),JsonParser.parseString("{\"ingredients\":[{\"fluid\":{\"fluid\":\"minecraft:lava\",\"level\":125},\"consume_container\":true}],\"result\":{\"item\":\"psychedelicraft:obsidian_bottle\"}}").getAsJsonObject());
            grid.setItem(1,ItemStack.EMPTY);check(fluidOnly.matches(grid,h.getLevel())&&fluidOnly.getRemainingItems(grid).get(0).isEmpty(),"fluid-only ingredient may consume its vessel without an item predicate");
        }finally{buffer.release();}h.succeed();
    }
}
