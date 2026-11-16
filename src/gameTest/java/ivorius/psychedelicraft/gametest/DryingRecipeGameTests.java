package ivorius.psychedelicraft.gametest;

import ivorius.psychedelicraft.recipe.DryingRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

@GameTestHolder("psychedelicrafttests")
@PrefixGameTestTemplate(false)
public class DryingRecipeGameTests {
    private static DryingRecipe recipe(){
        ItemStack output=new ItemStack(Items.BREAD,3);output.getOrCreateTag().putInt("sampleColor",0x22aa77);
        return new DryingRecipe(new ResourceLocation("psychedelicraft:test"),"test",Ingredient.of(Items.WHEAT),output,.2F,100);
    }
    private static SimpleContainer batch(){SimpleContainer container=new SimpleContainer(10);for(int slot=1;slot<=9;slot++)container.setItem(slot,new ItemStack(Items.WHEAT));return container;}
    @GameTest(template="empty") public static void allNineSingleInputsAndAnEmptyOutputAreRequired(GameTestHelper helper){
        DryingRecipe recipe=recipe();SimpleContainer inventory=batch();assertTrue(recipe.matches(inventory,null));
        inventory.setItem(1,ItemStack.EMPTY);assertFalse(recipe.matches(inventory,null));
        inventory.setItem(1,new ItemStack(Items.WHEAT,2));assertFalse(recipe.matches(inventory,null));
        inventory.setItem(1,new ItemStack(Items.CARROT));assertFalse(recipe.matches(inventory,null));
        inventory.setItem(1,new ItemStack(Items.WHEAT));inventory.setItem(0,new ItemStack(Items.BREAD));assertFalse(recipe.matches(inventory,null));
        helper.succeed();
    }
    @GameTest(template="empty") public static void recipeCopiesAndNetworkRoundTripPreserveOutputData(GameTestHelper helper){
        DryingRecipe original=recipe();FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
        try{
            DryingRecipe.Serializer serializer=new DryingRecipe.Serializer();serializer.toNetwork(buffer,original);
            DryingRecipe restored=serializer.fromNetwork(original.getId(),buffer);
            assertEquals(original.getId(),restored.getId());assertEquals("test",restored.getGroup());
            assertEquals(.2F,restored.getExperience(),0);assertEquals(100,restored.getCookTime());assertTrue(restored.matches(batch(),null));
            ItemStack output=restored.assemble(batch(),RegistryAccess.EMPTY);assertEquals(3,output.getCount());assertEquals(0x22aa77,output.getTag().getInt("sampleColor"));
            output.getTag().putInt("sampleColor",0);output.shrink(1);assertTrue(ItemStack.matches(original.getOutput(),restored.getOutput()));
        }finally{buffer.release();}
        helper.succeed();
    }
    @GameTest(template="empty") public static void nativeCraftingRemaindersAreRetainedForAllNineInputs(GameTestHelper helper){
        DryingRecipe recipe=new DryingRecipe(new ResourceLocation("psychedelicraft:containers"),"",Ingredient.of(Items.MILK_BUCKET),new ItemStack(Items.BREAD),0,100);
        SimpleContainer container=new SimpleContainer(10);for(int slot=1;slot<=9;slot++)container.setItem(slot,new ItemStack(Items.MILK_BUCKET));
        var remainder=recipe.getRemainingItems(container);assertTrue(remainder.get(0).isEmpty());
        for(int slot=1;slot<=9;slot++)assertTrue(remainder.get(slot).is(Items.BUCKET));
        helper.succeed();
    }
    private static void assertTrue(boolean condition){if(!condition)throw new AssertionError("Expected condition to be true");}
    private static void assertFalse(boolean condition){assertTrue(!condition);}
    private static void assertEquals(Object expected,Object actual){if(!java.util.Objects.equals(expected,actual))throw new AssertionError("Expected "+expected+" but got "+actual);}
    private static void assertEquals(float expected,float actual,float delta){if(Math.abs(expected-actual)>delta)throw new AssertionError("Expected "+expected+" but got "+actual);}
}
