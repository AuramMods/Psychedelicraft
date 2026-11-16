package ivorius.psychedelicraft.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

import net.minecraftforge.common.ToolActions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.gameevent.GameEvent;

public class NightshadeBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final int MAX_AGE = 7;

    private static final VoxelShape[] SHAPES = {
            Block.box(7, 0, 7, 9, 2, 9),
            Block.box(6.5, 0, 6.5, 9.5, 4, 9.5),
            Block.box(6, 0, 6, 10, 6, 10),
            Block.box(5, 0, 5, 11, 9, 11),
            Block.box(2, 0, 2, 14, 10, 14),
            Block.box(2, 0, 2, 14, 14, 14),
            Block.box(1, 0, 1, 15, 15, 15),
            Block.box(1, 0, 1, 15, 15, 15)
    };

    private final ItemLike fruit;
    private final ItemLike leaf;

    public NightshadeBlock(BlockBehaviour.Properties settings, ItemLike fruit, ItemLike leaf) {
        super(settings);
        this.fruit = fruit;
        this.leaf = leaf;
        registerDefaultState(defaultBlockState().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) < MAX_AGE && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(world, pos, state, random.nextInt(5) == 0) && world.getRawBrightness(pos.above(), 0) >= 9) {
            BlockState previous = state;
            state = state.cycle(AGE);
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
            net.minecraftforge.common.ForgeHooks.onCropsGrowPost(world, pos, previous);
        }
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity) || state.getValue(AGE) < 3 || entity.getType() == EntityType.FOX || entity.getType() == EntityType.BEE) {
            return;
        }
        entity.makeStuckInBlock(state, new Vec3(0.8f, 0.75, 0.8f));
    }

    @Deprecated
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack stack = player.getItemInHand(hand);
        int age = state.getValue(AGE);

        if ((stack.canPerformAction(ToolActions.SHEARS_HARVEST) && age >= 1) || (stack.is(Items.BONE_MEAL) && age == MAX_AGE)) {
            if (world.isClientSide) return InteractionResult.SUCCESS;
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            popResource(world, pos, new ItemStack((age == MAX_AGE ? fruit : leaf).asItem(), 1 + world.random.nextInt(2)));

            state = state.setValue(AGE, age - 1);
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
            world.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));

            if (stack.is(Items.BONE_MEAL)) {
                return InteractionResult.PASS;
            }

            world.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS,
                    1,
                    0.8F + world.random.nextFloat() * 0.4F
            );
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, boolean client) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        int i = Math.min(MAX_AGE, state.getValue(AGE) + 1);
        world.setBlock(pos, state.setValue(AGE, i), Block.UPDATE_CLIENTS);
    }
}
