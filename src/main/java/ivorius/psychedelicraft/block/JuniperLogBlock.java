/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class JuniperLogBlock extends BlockLog {
    private static final EnumAxis[] AXES = EnumAxis.values();
    @Nullable
    private final Supplier<JuniperLogBlock> stripped;

    JuniperLogBlock(@Nullable Supplier<JuniperLogBlock> stripped) {
        this.stripped = stripped;
        setResistance(10F / 3F);
        setDefaultState(blockState.getBaseState().withProperty(LOG_AXIS, EnumAxis.Y));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LOG_AXIS);
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState().withProperty(LOG_AXIS, AXES[metadata & 3]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LOG_AXIS).ordinal();
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(LOG_AXIS) == EnumAxis.Y ? MapColor.CYAN : MapColor.CLAY;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void strip(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseItem() == Event.Result.DENY) return;

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof JuniperLogBlock)) return;
        JuniperLogBlock log = (JuniperLogBlock) state.getBlock();
        if (log.stripped == null) return;

        ItemStack tool = event.getItemStack();
        if (tool.isEmpty() || !(tool.getItem() instanceof ItemAxe || tool.getItem().getToolClasses(tool).contains("axe"))) return;
        EntityPlayer player = event.getEntityPlayer();
        if (player.isSpectator() || !world.isBlockModifiable(player, pos)
                || !(player.capabilities.allowEdit || tool.canPlaceOn(log))) return;

        if (!world.isRemote) {
            IBlockState replacement = log.stripped.get().getDefaultState().withProperty(LOG_AXIS, state.getValue(LOG_AXIS));
            if (!world.setBlockState(pos, replacement, 11)) return;
            tool.damageItem(1, player);
            world.playSound(null, pos, PSSounds.AXE_STRIP, SoundCategory.BLOCKS, 1F, 1F);
        }
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }
}
