package ivorius.psychedelicraft.block.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Native two-sided text, filtering, editing, wax and glow; only the registry type is mod-specific. */
public final class JuniperSignBlockEntity extends SignBlockEntity {
    public JuniperSignBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.JUNIPER_SIGN.get(),pos,state);}
}
