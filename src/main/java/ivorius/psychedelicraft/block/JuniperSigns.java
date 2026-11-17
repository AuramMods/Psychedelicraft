package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.block.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

/** Thin registry adapters retain all native sign interactions and the editor-distance ticker. */
public final class JuniperSigns {
    private JuniperSigns(){}
    private static <T extends BlockEntity> BlockEntityTicker<T> ticker(BlockEntityType<T> actual,BlockEntityType<?> expected){return actual==expected?(level,pos,state,entity)->SignBlockEntity.tick(level,pos,state,(SignBlockEntity)entity):null;}
    public static final class Standing extends StandingSignBlock {
        public Standing(Properties properties){super(properties,PSWoodTypes.JUNIPER);}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new JuniperSignBlockEntity(pos,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:ticker(type,PSBlockEntities.JUNIPER_SIGN.get());}
    }
    public static final class Wall extends WallSignBlock {
        public Wall(Properties properties){super(properties,PSWoodTypes.JUNIPER);}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new JuniperSignBlockEntity(pos,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:ticker(type,PSBlockEntities.JUNIPER_SIGN.get());}
    }
    public static final class Hanging extends CeilingHangingSignBlock {
        public Hanging(Properties properties){super(properties,PSWoodTypes.JUNIPER);}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new JuniperHangingSignBlockEntity(pos,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:ticker(type,PSBlockEntities.JUNIPER_HANGING_SIGN.get());}
    }
    public static final class HangingWall extends WallHangingSignBlock {
        public HangingWall(Properties properties){super(properties,PSWoodTypes.JUNIPER);}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new JuniperHangingSignBlockEntity(pos,state);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:ticker(type,PSBlockEntities.JUNIPER_HANGING_SIGN.get());}
    }
}
