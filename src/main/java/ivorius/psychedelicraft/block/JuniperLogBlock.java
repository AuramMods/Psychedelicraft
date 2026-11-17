package ivorius.psychedelicraft.block;

import java.util.function.Supplier;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public final class JuniperLogBlock extends RotatedPillarBlock {
    private final Supplier<? extends Block> stripped;
    public JuniperLogBlock(Properties properties,Supplier<? extends Block> stripped){super(properties);this.stripped=stripped;}
    @Override public BlockState getToolModifiedState(BlockState state,UseOnContext context,ToolAction action,boolean simulate){
        return action==ToolActions.AXE_STRIP&&context.getItemInHand().canPerformAction(action)?stripped.get().defaultBlockState().setValue(AXIS,state.getValue(AXIS)):super.getToolModifiedState(state,context,action,simulate);
    }
}
