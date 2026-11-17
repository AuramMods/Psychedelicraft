package ivorius.psychedelicraft.block.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class JuniperHangingSignBlockEntity extends HangingSignBlockEntity {
    public JuniperHangingSignBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.JUNIPER_HANGING_SIGN.get(),pos,state);}
}
