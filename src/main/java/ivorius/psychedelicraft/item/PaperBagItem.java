/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.core.*;
import net.minecraft.core.dispenser.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** The accepted single-kind, full-data bag, using native 1.20.1 secondary-click hooks. */
public final class PaperBagItem extends Item {
    public static final TagKey<Item> ALLOWED=TagKey.create(Registries.ITEM,Psychedelicraft.id("can_go_into_paper_bag"));
    public PaperBagItem(){super(new Properties());}
    @Override public int getMaxStackSize(ItemStack stack){return getContents(stack).isEmpty()?64:1;}
    private static int capacity(ItemStack stack){return stack.is(PSItems.BOTTLE.get())?1:64000;}
    public static boolean canStore(ItemStack stack){return !stack.isEmpty()&&!(stack.getItem() instanceof PaperBagItem)&&stack.is(ALLOWED);}
    public static Contents getContents(ItemStack bag){
        CompoundTag data=bag.getTagElement("contents");if(data==null)return new Contents(ItemStack.EMPTY,0);
        ItemStack prototype=ItemStack.of(data.getCompound("stack"));if(prototype.isEmpty())return new Contents(ItemStack.EMPTY,0);
        return new Contents(prototype.copyWithCount(1),Math.min(capacity(prototype),Math.max(0,data.getInt("count"))));
    }
    public static void setContents(ItemStack bag,Contents contents){
        if(contents.isEmpty()){bag.removeTagKey("contents");return;}
        var data=new CompoundTag();data.put("stack",contents.stack.save(new CompoundTag()));data.putInt("count",contents.count);bag.getOrCreateTag().put("contents",data);
    }
    public static final class Contents {
        private ItemStack stack;private int count;
        public Contents(ItemStack stack,int count){this.stack=stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(1);this.count=stack.isEmpty()?0:Math.min(capacity(stack),Math.max(0,count));}
        public boolean isEmpty(){return count<=0||stack.isEmpty();}
        public int count(){return count;}
        public int roomFor(ItemStack incoming){return canStore(incoming)&&(isEmpty()||ItemStack.isSameItemSameTags(stack,incoming))?Math.max(0,capacity(incoming)-count):0;}
        public boolean insert(ItemStack incoming){int amount=Math.min(incoming.getCount(),roomFor(incoming));if(amount<=0)return false;if(isEmpty())stack=incoming.copyWithCount(1);incoming.shrink(amount);count+=amount;return true;}
        public ItemStack peek(int amount){return isEmpty()||amount<=0?ItemStack.EMPTY:stack.copyWithCount(Math.min(count,amount));}
        public ItemStack take(int amount){var result=peek(amount);count-=result.getCount();return result;}
    }
    @Override public boolean overrideStackedOnOther(ItemStack bag,Slot slot,ClickAction action,Player player){
        if(action!=ClickAction.SECONDARY||bag.getCount()!=1)return false;var contents=getContents(bag);var stored=slot.getItem();boolean remove=stored.isEmpty(),changed;
        if(stored.isEmpty()&&!contents.isEmpty()){
            var output=contents.peek(contents.stack.getMaxStackSize());int before=output.getCount();slot.safeInsert(output);int inserted=before-output.getCount();changed=inserted>0;if(changed)contents.take(inserted);
        }else if(contents.roomFor(stored)>0){var taken=slot.safeTake(stored.getCount(),contents.roomFor(stored),player);changed=contents.insert(taken);}else return false;
        if(changed){setContents(bag,contents);sound(player,remove);}return changed;
    }
    @Override public boolean overrideOtherStackedOnMe(ItemStack bag,ItemStack cursor,Slot slot,ClickAction action,Player player,SlotAccess access){
        if(action!=ClickAction.SECONDARY||bag.getCount()!=1||!slot.allowModification(player))return false;var contents=getContents(bag);boolean changed;
        if(cursor.isEmpty()&&!contents.isEmpty()){var output=contents.peek(contents.stack.getMaxStackSize());changed=access.set(output);if(changed)contents.take(output.getCount());}
        else changed=contents.insert(cursor);
        if(changed){setContents(bag,contents);slot.setChanged();sound(player,false);}return changed;
    }
    private static void sound(Player player,boolean remove){if(!player.level().isClientSide)player.level().playSound(null,player,remove?SoundEvents.BUNDLE_REMOVE_ONE:SoundEvents.BUNDLE_INSERT,player.getSoundSource(),1,remove?.85F:1.25F);}
    public static boolean offer(Player player,ItemStack stack){return player.getInventory().add(stack)||stack.isEmpty()||release(player,stack);}
    private static boolean release(Player player,ItemStack stack){var drop=player.drop(stack,false,false);return drop!=null&&(drop.isAddedToWorld()||player.captureDrops()!=null&&player.captureDrops().contains(drop));}
    private InteractionResult spill(Player player,ItemStack bag){
        var contents=getContents(bag);if(bag.getCount()!=1||contents.isEmpty())return InteractionResult.PASS;
        if(!player.level().isClientSide){var output=contents.peek(player.isShiftKeyDown()?1:contents.stack.getMaxStackSize());if(!release(player,output))return InteractionResult.FAIL;contents.take(output.getCount());setContents(bag,contents);sound(player,true);player.awardStat(Stats.ITEM_USED.get(this));}
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){var bag=player.getItemInHand(hand);return new InteractionResultHolder<>(spill(player,bag),bag);}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();if(player==null||!context.getLevel().mayInteract(player,context.getClickedPos())||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand()))return InteractionResult.PASS;
        var bag=context.getItemInHand();var contents=getContents(bag);var point=context.getClickLocation();var nearby=context.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(point,point).inflate(.25),item->!item.isRemoved()&&!item.getItem().isEmpty());
        var moves=new LinkedHashMap<ItemEntity,Integer>();
        for(var entity:nearby){var copy=entity.getItem().copy();int before=copy.getCount();if(contents.insert(copy))moves.put(entity,before-copy.getCount());}
        if(moves.isEmpty())return spill(player,bag);
        if(!context.getLevel().isClientSide){
            if(bag.getCount()>1){var filled=bag.copyWithCount(1);setContents(filled,contents);if(!offer(player,filled))return InteractionResult.FAIL;bag.shrink(1);}
            else setContents(bag,contents);
            moves.forEach((entity,amount)->{var remaining=entity.getItem().copy();remaining.shrink(amount);if(remaining.isEmpty())entity.discard();else entity.setItem(remaining);});sound(player,false);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void inventoryTick(ItemStack bag,Level level,Entity entity,int slot,boolean selected){
        if(level.isClientSide||!selected||bag.getCount()!=1||!(entity instanceof Player player)||slot<0||slot>=player.getInventory().items.size()||player.getInventory().items.get(slot)!=bag)return;
        var contents=getContents(bag);if(contents.isEmpty())return;boolean changed=slot+1<player.getInventory().items.size()&&contents.insert(player.getInventory().items.get(slot+1));changed|=slot>0&&contents.insert(player.getInventory().items.get(slot-1));
        if(changed){setContents(bag,contents);player.getInventory().setChanged();sound(player,true);}
    }
    public static void registerDispenser(){DispenserBlock.registerBehavior(PSItems.PAPER_BAG.get(),new DefaultDispenseItemBehavior(){
        @Override protected ItemStack execute(BlockSource source,ItemStack bag){var contents=getContents(bag);if(bag.getCount()!=1||contents.isEmpty())return super.execute(source,bag);var output=contents.peek(1);var level=source.getLevel();var direction=source.getBlockState().getValue(DispenserBlock.FACING);var pos=DispenserBlock.getDispensePosition(source);
            var drop=new ItemEntity(level,pos.x(),pos.y()-(direction.getAxis()==Direction.Axis.Y?.125:.15625),pos.z(),output);double speed=level.random.nextDouble()*.1+.2;
            drop.setDeltaMovement(level.random.triangle(direction.getStepX()*speed,.0172275*6),level.random.triangle(.2,.0172275*6),level.random.triangle(direction.getStepZ()*speed,.0172275*6));
            if(level.addFreshEntity(drop)){contents.take(1);setContents(bag,contents);}return bag;}
    });}
    @Override public Component getName(ItemStack bag){var contents=getContents(bag);return contents.isEmpty()?super.getName(bag):Component.translatable(getDescriptionId(bag)+".filled",contents.stack.getHoverName());}
    @Override public void appendHoverText(ItemStack bag,@Nullable Level level,List<Component> tooltip,TooltipFlag flag){var contents=getContents(bag);if(!contents.isEmpty())tooltip.add(Component.literal(contents.count+" × ").append(contents.stack.getHoverName()));}
}
