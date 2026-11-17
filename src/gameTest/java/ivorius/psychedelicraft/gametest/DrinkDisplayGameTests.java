package ivorius.psychedelicraft.gametest;
import com.mojang.authlib.GameProfile;
import ivorius.psychedelicraft.block.*;
import ivorius.psychedelicraft.block.entity.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class DrinkDisplayGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static void check(boolean condition,String text){if(!condition)throw new GameTestAssertException(text);}
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h){var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"DisplayTest"));player.setPos(h.absolutePos(POS).getX()+3,h.absolutePos(POS).getY(),h.absolutePos(POS).getZ()+3);player.getInventory().clearContent();return player;}
    private static ItemStack bottle(int amount){var fluid=new FluidStack(PSFluids.RED_GRAPES.get(),amount);fluid.getOrCreateTag().putInt("maturation",7);fluid.getOrCreateTag().putString("batch","display");var stack=FluidVesselHandler.forCrafting(new ItemStack(PSItems.BOTTLE.get())).replaceContents(fluid);stack.setHoverName(Component.literal("Display reserve"));HarmoniumItem.setColor(stack,0x3377ff);stack.getOrCreateTag().putInt("custom",73);return stack;}
    private static void same(ItemStack expected,ItemStack actual,String text){check(ItemStack.matches(expected,actual),text);}
    private static PlacedDrinkBlockEntity display(GameTestHelper h){h.setBlock(POS,PSBlocks.PLACED_DRINK.get());return(PlacedDrinkBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));}
    private static BottleRackBlockEntity rack(GameTestHelper h){h.setBlock(POS,PSBlocks.BOTTLE_RACK.get().defaultBlockState().setValue(BottleRackBlock.FACING,Direction.NORTH));return(BottleRackBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));}
    private static BlockHitResult hit(BlockPos pos,Direction side,double x,double y,double z){return new BlockHitResult(Vec3.atLowerCornerOf(pos).add(x,y,z),side,pos,false);}
    private static List<ItemEntity> drops(GameTestHelper h){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(2));}
    @GameTest(template="empty") public static void placementGridClampsEdgesAndSpiralDoesNotWrapColumns(GameTestHelper h){
        check(PlacedDrinkBlockEntity.cell(0,0)==0&&PlacedDrinkBlockEntity.cell(1,1)==255&&PlacedDrinkBlockEntity.cell(-.1F,2)==15,"sixteen-cell coordinates clamp both outer boundaries");var display=display(h);display.place(0,bottle(1200),25);
        check(display.findColumn(255)==-1&&display.peek(255).isEmpty(),"spiral never wraps the opposite corner");int near=PlacedDrinkBlockEntity.cell(.1F,.1F);check(display.findColumn(near)==0,"radius-two spiral finds nearby corner column");
        for(int i=1;i<5;i++)check(display.place(0,bottle(1200+i),i*20),"up to five independent vessels fit one column");check(!display.canPlace(0)&&!display.place(0,bottle(900),0),"sixth vessel is refused without replacing the top");same(bottle(1204),display.peek(0),"top keeps exact fluid and item data");h.succeed();
    }
    @GameTest(template="empty") public static void nativeItemUsePlacesOneNamedVesselAndCreativeKeepsItsCopy(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.OAK_PLANKS);var player=player(h);var original=bottle(1753);var held=original.copy();player.setItemInHand(InteractionHand.MAIN_HAND,held);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit(h.absolutePos(POS.below()),Direction.UP,.5,1,.5));check(held.useOn(context).consumesAction()&&held.isEmpty(),"native ItemStack.useOn places and consumes exactly one vessel");
        var display=(PlacedDrinkBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));same(original,display.peek(PlacedDrinkBlockEntity.cell(.5F,.5F)),"normal placed drink preserves amount, name, maturation, dye and root NBT");
        player.getAbilities().instabuild=true;held=bottle(900);player.setItemInHand(InteractionHand.MAIN_HAND,held);check(held.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit(h.absolutePos(POS),Direction.UP,.5,0,.5))).consumesAction()&&held.getCount()==1,"creative copies one vessel while preserving held count");same(bottle(900),display.take(PlacedDrinkBlockEntity.cell(.5F,.5F)),"native stacking copies creative metadata");player.getAbilities().instabuild=false;h.succeed();
    }
    @GameTest(template="empty") public static void nativeTaggedSmokingPlacementSeparatesFreshStacksAndPreservesPartialWear(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.STONE);var player=player(h);var fresh=new ItemStack(PSItems.JOINT.get(),64);player.setItemInHand(InteractionHand.MAIN_HAND,fresh);
        var target=hit(h.absolutePos(POS.below()),Direction.UP,.5,1,.5);check(fresh.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,target)).consumesAction()&&fresh.getCount()==63,"native optional placement tag moves one fresh joint from the stack");
        var display=(PlacedDrinkBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));int cell=PlacedDrinkBlockEntity.cell(.5F,.5F);var stored=display.take(cell);check(stored.getCount()==1&&stored.getDamageValue()==0,"placed fresh joint remains independent");
        var partial=new ItemStack(PSItems.JOINT.get());partial.setDamageValue(1);partial.setHoverName(Component.literal("Partial joint"));partial.getOrCreateTag().putInt("custom",83);var expected=partial.copy();player.setItemInHand(InteractionHand.MAIN_HAND,partial);
        check(partial.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,target)).consumesAction()&&partial.isEmpty(),"native partial-joint placement consumes its single held item");display=(PlacedDrinkBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));same(expected,display.take(cell),"pickup retains exact partial wear/name/NBT");check(fresh.getCount()==63&&drops(h).isEmpty(),"tagged placement/pickup cannot create a second partial or lose fresh items");h.succeed();
    }
    private static void pickupTrace(GameTestHelper h,String phase){var pos=h.absolutePos(POS);var chunk=h.getLevel().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);var items=drops(h);com.mojang.logging.LogUtils.getLogger().info("DISPLAY_PICKUP_STATE phase={} time={} pos={} status={} drops={} items={}",phase,h.getLevel().getGameTime(),pos,chunk==null?"not resident":chunk.getFullStatus(),items.size(),items.stream().limit(10).map(e->e.getItem()+"/age="+e.getAge()+"/uuid="+e.getUUID()+"/pos="+e.position()).toList());}
    @GameTest(template="empty") public static void pickupAndContextualPickKeepLifoDataAndRemoveTheFinalBlock(GameTestHelper h){
        pickupTrace(h,"before");var display=display(h);int cell=PlacedDrinkBlockEntity.cell(.5F,.5F);var first=bottle(777);var second=bottle(1333);display.place(cell,first,90);display.place(cell,second,180);var player=player(h);
        var target=hit(h.absolutePos(POS),Direction.UP,.5,.01,.5);var block=PSBlocks.PLACED_DRINK.get();var picked=block.getCloneItemStack(display.getBlockState(),target,h.getLevel(),display.getBlockPos(),player);same(second,picked,"native contextual pick gets top bottle");picked.getTag().putInt("custom",0);same(second,display.peek(cell),"pick returns an independent item snapshot");
        check(block.use(display.getBlockState(),h.getLevel(),display.getBlockPos(),player,InteractionHand.MAIN_HAND,target).consumesAction(),"normal empty-hand block use picks up a vessel");same(second,player.getMainHandItem(),"pickup returns exact top contents");same(first,display.take(cell),"remaining bottom bottle is independent");pickupTrace(h,"after");check(h.getLevel().getBlockState(display.getBlockPos()).isAir()&&drops(h).isEmpty(),"last pickup removes display without duplicating a drop");h.succeed();
    }
    @GameTest(template="empty") public static void cancelledNativePlacementRestoresHeldFluidWithoutDroppingACopy(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.STONE);var player=player(h);var original=bottle(1741);var held=original.copy();player.setItemInHand(InteractionHand.MAIN_HAND,held);int[] calls={0};Object listener=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void placed(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event){if(event.getEntity()==player){calls[0]++;event.setCanceled(true);}}};
        MinecraftForge.EVENT_BUS.register(listener);try{check(held.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit(h.absolutePos(POS.below()),Direction.UP,.5,1,.5)))==InteractionResult.FAIL,"actual Forge item-use cancellation refuses placement");}finally{MinecraftForge.EVENT_BUS.unregister(listener);}
        same(original,held,"cancelled placement restores exact held NBT and quantity");check(calls[0]==1&&h.getLevel().getBlockState(h.absolutePos(POS)).isAir()&&h.getLevel().getBlockEntity(h.absolutePos(POS))==null&&drops(h).isEmpty(),"snapshot rollback removes transient display without producing a duplicate bottle");h.succeed();
    }
    @GameTest(template="empty") public static void displaySaveRoundTripRetainsColumnsRotationAndFormerlyTaggedItems(GameTestHelper h){
        var display=display(h);display.place(0,bottle(1234),-37);display.place(255,bottle(1456),82);var saved=display.saveWithoutMetadata();var entries=saved.getList("entries",Tag.TAG_COMPOUND);var previous=new CompoundTag();previous.putInt("cell",7);previous.putFloat("rotation",Float.NaN);var legacy=new ItemStack(Items.DIRT);legacy.setHoverName(Component.literal("Former tag member"));previous.put("stack",legacy.save(new CompoundTag()));entries.add(previous);
        var restored=new PlacedDrinkBlockEntity(display.getBlockPos(),display.getBlockState());restored.load(saved);same(bottle(1234),restored.peek(0),"first saved column retains all item/fluid data");same(bottle(1456),restored.peek(255),"last saved column keeps independent contents");same(legacy,restored.peek(7),"previously stored items remain recoverable after placement tags change");
        check(!PlacedDrinksBlock.canPlace(legacy)&&!restored.place(8,legacy,0),"new insertion still enforces live placeable tag");int[] count={0};restored.forEachDrink((x,y,z,rotation,stack)->{check(Float.isFinite(rotation),"malformed saved angle normalizes safely");count[0]++;return .25F;});check(count[0]==3,"three saved entries recover once");h.succeed();
    }
    @GameTest(template="empty") public static void walkingKnocksVesselsOverAndSneakingAtFloorHeightDoesNot(GameTestHelper h){
        var display=display(h);display.place(128,bottle(1900),0);var player=player(h);var pos=display.getBlockPos();player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);player.xo=player.getX()-.1;player.zo=player.getZ();player.setShiftKeyDown(true);
        PSBlocks.PLACED_DRINK.get().entityInside(display.getBlockState(),h.getLevel(),pos,player);check(h.getLevel().getBlockState(pos).is(PSBlocks.PLACED_DRINK.get()),"native sneak flag protects floor-height drinks");player.setShiftKeyDown(false);PSBlocks.PLACED_DRINK.get().entityInside(display.getBlockState(),h.getLevel(),pos,player);
        check(h.getLevel().getBlockState(pos).isAir()&&drops(h).size()==1,"walking removes the display and drops one vessel");same(bottle(1900),drops(h).get(0).getItem(),"knocked vessel retains exact contents");drops(h).forEach(Entity::discard);h.succeed();
    }
    @GameTest(template="empty") public static void explosionAndNonLootRemovalReleaseEachStoredDrinkOnce(GameTestHelper h){
        // Reused test worlds can contain old drops outside the template's cleared box.
        // Keep exact per-operation conservation without counting unrelated pre-existing UUIDs.
        var before=drops(h).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        var display=display(h);display.place(10,bottle(1001),0);display.place(10,bottle(1002),0);display.place(200,bottle(1003),0);var pos=display.getBlockPos();
        try(var trace=new NativeItemTrace(h.getLevel(),e->e.getItem().is(PSItems.BOTTLE.get()))){
            h.getLevel().explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,Level.ExplosionInteraction.BLOCK);trace.log("display-explosion",new AABB(pos).inflate(2));
        }
        check(h.getLevel().getBlockState(pos).isAir(),"real native explosion removes display");
        var firstPrior=before;var items=drops(h).stream().filter(e->!firstPrior.contains(e.getUUID())).toList();check(items.size()==3,"explosion releases every stored vessel once: "+items.size());
        int amount=items.stream().mapToInt(e->PSItems.BOTTLE.get().contents(e.getItem()).getAmount()).sum();check(amount==3006,"explosion conserves all three independent volumes");items.forEach(Entity::discard);
        before=drops(h).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        display=display(h);display.place(128,bottle(877),0);h.getLevel().destroyBlock(display.getBlockPos(),false);
        var prior=before;items=drops(h).stream().filter(e->!prior.contains(e.getUUID())).toList();check(items.size()==1,"non-loot removal releases stored contents just once");same(bottle(877),items.get(0).getItem(),"non-loot removal keeps metadata");items.forEach(Entity::discard);h.succeed();
    }

    @GameTest(template="empty") public static void rackFrontSlotsClampEveryEdgeAndNativeUseMovesOneBottle(GameTestHelper h){
        for(Direction facing:Direction.Plane.HORIZONTAL)for(Direction side:Direction.values())for(double edge:new double[]{0,1}){int slot=BottleRackBlock.slotAt(facing,side,edge,edge,edge);check(side!=facing&&side.getAxis().isHorizontal()?slot==-1:slot>=0&&slot<9,"all allowed face boundaries clamp to nine valid slots");}
        var rack=rack(h);var player=player(h);var full=bottle(1654);player.setItemInHand(InteractionHand.MAIN_HAND,full.copy());var hit=hit(rack.getBlockPos(),Direction.NORTH,.9,.9,.3);
        check(PSBlocks.BOTTLE_RACK.get().use(rack.getBlockState(),h.getLevel(),rack.getBlockPos(),player,InteractionHand.MAIN_HAND,hit).consumesAction()&&player.getMainHandItem().isEmpty(),"front interaction inserts one bottle");same(full,rack.getItem(0),"rack front maps correct row and column with complete data");
        PSBlocks.BOTTLE_RACK.get().use(rack.getBlockState(),h.getLevel(),rack.getBlockPos(),player,InteractionHand.MAIN_HAND,hit);same(full,player.getMainHandItem(),"front extraction returns exact bottle into used hand");check(rack.isEmpty(),"extraction clears only its cell");h.succeed();
    }
    @GameTest(template="empty") public static void rackCapabilitiesEnforceSidesLimitsSimulationAndPermanentInvalidation(GameTestHelper h){
        var rack=rack(h);check(!rack.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).isPresent()&&!rack.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).isPresent(),"no vertical item capabilities");var optional=rack.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST);var handler=optional.orElseThrow(AssertionError::new);var held=new ItemStack(PSItems.BOTTLE.get(),3);
        check(handler.insertItem(0,held,true).getCount()==2&&rack.isEmpty()&&held.getCount()==3,"simulation bounds one slot and leaves both sides intact");check(handler.insertItem(0,held,false).getCount()==2&&rack.getItem(0).getCount()==1&&held.getCount()==3,"execution stores one and returns two, preserving supplied stack");check(handler.insertItem(1,new ItemStack(PSItems.WOODEN_MUG.get()),false).is(PSItems.WOODEN_MUG.get()),"rack rejects non-bottle placeables");
        rack.setItem(1,bottle(1777));var snapshot=handler.getStackInSlot(1);snapshot.getTag().putInt("custom",0);same(bottle(1777),rack.getItem(1),"capability read snapshots cannot mutate storage");same(bottle(1777),handler.extractItem(1,1,true),"extraction simulation preserves exact data");same(bottle(1777),rack.getItem(1),"simulation does not remove item");
        h.setBlock(POS,Blocks.AIR);check(!optional.isPresent()&&handler.extractItem(1,1,false).isEmpty()&&handler.insertItem(0,held,false).getCount()==3,"removed rack invalidates optional and resolved raw handler");rack.clearRemoved();rack.reviveCaps();check(handler.extractItem(0,1,false).isEmpty(),"reviving the old entity cannot reactivate its previous capability epoch");drops(h).forEach(Entity::discard);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80) public static void nativeHorizontalHopperFillsRackWhileVerticalHoppersStayBlocked(GameTestHelper h){
        var rack=rack(h);h.setBlock(POS.west(),Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST));var side=(HopperBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.west()));side.setItem(0,bottle(1709));h.setBlock(POS.above(),Blocks.HOPPER);var top=(HopperBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.above()));top.setItem(0,bottle(500));h.setBlock(POS.below(),Blocks.HOPPER);var bottom=(HopperBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.below()));
        h.runAtTickTime(40,()->{check(side.isEmpty()&&bottom.isEmpty()&&top.getItem(0).getCount()==1,"real native hoppers allow horizontal insertion and refuse vertical access");same(bottle(1709),rack.getItem(0),"native hopper retained exact bottle metadata");h.succeed();});
    }
    @GameTest(template="empty") public static void rackPersistenceAndBreakDropsKeepNineIndependentSingleBottles(GameTestHelper h){
        var rack=rack(h);for(int slot=0;slot<9;slot++)rack.setItem(slot,bottle(1000+slot));var restored=new BottleRackBlockEntity(rack.getBlockPos(),rack.getBlockState());restored.load(rack.saveWithoutMetadata());for(int slot=0;slot<9;slot++)same(bottle(1000+slot),restored.getItem(slot),"each saved rack slot retains its own volume and data");
        h.getLevel().destroyBlock(rack.getBlockPos(),true);int racks=0,bottles=0,volume=0;for(var entity:drops(h)){var stack=entity.getItem();if(stack.is(PSItems.BOTTLE_RACK.get()))racks+=stack.getCount();if(stack.is(PSItems.BOTTLE.get())){bottles+=stack.getCount();volume+=PSItems.BOTTLE.get().contents(stack).getAmount();}entity.discard();}check(racks==1&&bottles==9&&volume==9036,"native rack break drops one rack and nine full metadata-preserving bottles once");h.succeed();
    }
    @GameTest(template="empty") public static void portableVesselsDeferSpecializedBarrelAndMashInteractions(GameTestHelper h){
        var player=player(h);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.BOTTLE.get()));h.setBlock(POS,PSBlocks.OAK_BARREL.get());
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit(h.absolutePos(POS),Direction.NORTH,.5,.5,0));var barrel=(BarrelBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));barrel.getTank().replace(new FluidStack(PSFluids.WHEAT.get(),1700));
        check(PSItems.BOTTLE.get().onItemUseFirst(player.getMainHandItem(),context)==InteractionResult.PASS&&barrel.getTank().getFluidAmount()==1700,"portable vessel does not bypass native barrel interaction");h.setBlock(POS,PSBlocks.MASH_TUB.get());var vat=(MashTubBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));vat.getTank().replace(new FluidStack(PSFluids.WHEAT.get(),1700));
        check(PSItems.BOTTLE.get().onItemUseFirst(player.getMainHandItem(),context)==InteractionResult.PASS&&vat.getTank().getFluidAmount()==1700,"portable vessel defers native mash collection to its block hook");h.setBlock(POS.east(),MashTubBlock.wallState(1,0));context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit(h.absolutePos(POS.east()),Direction.UP,.5,.5,.5));check(PSItems.BOTTLE.get().onItemUseFirst(player.getMainHandItem(),context)==InteractionResult.PASS&&vat.getTank().getFluidAmount()==1700,"wall proxy also defers to its owning vat interaction");h.succeed();
    }
    @GameTest(template="empty") public static void nativeRackCraftingAndDecorativeLabPropsMatchSource(GameTestHelper h){
        var grid=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}@Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}},3,3);
        for(int slot=0;slot<9;slot++)grid.setItem(slot,new ItemStack(slot%2==0?Items.STICK:Items.OAK_PLANKS));var recipe=h.getLevel().getRecipeManager().byKey(new ResourceLocation("psychedelicraft:bottle_rack")).orElseThrow();check(((CraftingRecipe)recipe).matches(grid,h.getLevel())&&((CraftingRecipe)recipe).assemble(grid,h.getLevel().registryAccess()).is(PSItems.BOTTLE_RACK.get()),"native shaped rack recipe retains original sticks/planks pattern");
        check(new ItemStack(Items.IRON_AXE).getDestroySpeed(PSBlocks.BOTTLE_RACK.get().defaultBlockState())>1&&new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(PSBlocks.TRAY.get().defaultBlockState())>1,"native tool tags recognize rack and metal props");
        var tray=PSBlocks.TRAY.get();var state=tray.defaultBlockState();for(int i=0;i<4;i++)state=state.rotate(Rotation.CLOCKWISE_90);check(state==tray.defaultBlockState(),"four native tray rotations restore its horizontal axis");check(PSBlocks.BUNSEN_BURNER.get().defaultBlockState().getShape(h.getLevel(),h.absolutePos(POS)).max(Direction.Axis.Y)==.125&&DistilleryBlockEntity.heatRate(PSBlocks.BUNSEN_BURNER.get().defaultBlockState())==1,"burner keeps two-pixel decorative shape and does not invent a heat source");h.succeed();
    }
}
