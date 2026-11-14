package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class MudBlock extends Block {
    private static final AxisAlignedBB COLLISION = new AxisAlignedBB(0, 0, 0, 1, .875D, 1);

    MudBlock() {
        super(Material.GROUND);
        setHardness(.5F);
        setSoundType(SoundType.GROUND);
        setHarvestLevel("shovel", 0);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return COLLISION;
    }

    static boolean canConvert(IBlockState state) {
        if (state.getBlock() == PSBlocks.ROOTED_DIRT) return true;
        return state.getBlock() == Blocks.DIRT && state.getValue(BlockDirt.VARIANT) != BlockDirt.DirtType.PODZOL;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pour(PlayerInteractEvent.RightClickBlock event) {
        ItemStack bottle = event.getItemStack();
        EnumFacing face = event.getFace();
        if (event.getUseItem() == Event.Result.DENY || face == null || face == EnumFacing.DOWN
                || bottle.getItem() != Items.POTIONITEM || PotionUtils.getPotionFromItem(bottle) != PotionTypes.WATER) return;
        World world = event.getWorld();
        EntityPlayer player = event.getEntityPlayer();
        BlockPos pos = event.getPos();
        if (player.isSpectator() || !world.isBlockModifiable(player, pos) || !player.canPlayerEdit(pos, face, bottle)
                || !canConvert(world.getBlockState(pos))) return;
        if (!world.isRemote) {
            if (!world.setBlockState(pos, PSBlocks.MUD.getDefaultState(), 3)) return;
            if (!player.capabilities.isCreativeMode) {
                ItemStack empty = new ItemStack(Items.GLASS_BOTTLE);
                bottle.shrink(1);
                if (bottle.isEmpty()) player.setHeldItem(event.getHand(), empty);
                else if (!player.addItemStackToInventory(empty)) player.dropItem(empty, false);
            }
            world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1F, 1F);
        }
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }
}
