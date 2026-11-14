package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Random;

public final class BeeNestBlock extends Block {
    public static final int FULL_HONEY = 5;
    public static final PropertyInteger HONEY_LEVEL = PropertyInteger.create("honey_level", 0, FULL_HONEY);
    private static final String STATE_TAG = "BlockStateTag";

    BeeNestBlock() {
        super(Material.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(HONEY_LEVEL, 0));
        setHardness(0.3F);
        setSoundType(SoundType.WOOD);
        setTickRandomly(true);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HONEY_LEVEL);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HONEY_LEVEL, meta >= 0 && meta <= FULL_HONEY ? meta : 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HONEY_LEVEL);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return state.getValue(HONEY_LEVEL);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        ItemStack nest = new ItemStack(PSItems.BEE_NEST);
        NBTTagCompound savedState = new NBTTagCompound();
        savedState.setString(HONEY_LEVEL.getName(), HONEY_LEVEL.getName(state.getValue(HONEY_LEVEL)));
        nest.setTagInfo(STATE_TAG, savedState);
        return nest;
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        boolean removed = super.removedByPlayer(state, world, pos, player, willHarvest);
        if (removed && !world.isRemote && player.isCreative() && state.getValue(HONEY_LEVEL) > 0) {
            spawnAsEntity(world, pos, getSilkTouchDrop(state));
        }
        return removed;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        int level = storedHoneyLevel(stack);
        if (level != state.getValue(HONEY_LEVEL)) {
            world.setBlockState(pos, state.withProperty(HONEY_LEVEL, level), 3);
        }
    }

    static int storedHoneyLevel(ItemStack stack) {
        NBTTagCompound savedState = stack.getSubCompound(STATE_TAG);
        if (savedState == null) return 0;
        String key = HONEY_LEVEL.getName();
        if (savedState.hasKey(key, Constants.NBT.TAG_STRING)) return HONEY_LEVEL.parseValue(savedState.getString(key)).or(0);
        if (!savedState.hasKey(key, Constants.NBT.TAG_INT)) return 0;
        int level = savedState.getInteger(key);
        return level >= 0 && level <= FULL_HONEY ? level : 0;
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        int level = state.getValue(HONEY_LEVEL);
        if (level < FULL_HONEY && world.getWorldTime() % 24000L < 12000L
                && !world.isRaining() && hasFlowers(world, pos)) {
            world.setBlockState(pos, state.withProperty(HONEY_LEVEL, level + 1), 3);
        }
    }

    private static boolean hasFlowers(World world, BlockPos pos) {
        BlockPos.MutableBlockPos flower = new BlockPos.MutableBlockPos();
        int bottom = Math.max(0, pos.getY() - 6);
        int top = Math.min(world.getHeight() - 1, pos.getY() + 2);
        for (int x = pos.getX() - 4; x <= pos.getX() + 4; x++) {
            for (int z = pos.getZ() - 4; z <= pos.getZ() + 4; z++) {
                if (!world.isBlockLoaded(flower.setPos(x, pos.getY(), z))) {
                    continue;
                }
                for (int y = bottom; y <= top; y++) {
                    IBlockState state = world.getBlockState(flower.setPos(x, y, z));
                    Block block = state.getBlock();
                    if (block instanceof BlockFlower || (block instanceof BlockDoublePlant
                            && state.getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.LOWER
                            && state.getValue(BlockDoublePlant.VARIANT) != BlockDoublePlant.EnumPlantType.GRASS
                            && state.getValue(BlockDoublePlant.VARIANT) != BlockDoublePlant.EnumPlantType.FERN)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        ItemStack held = player.getHeldItem(hand);
        Item item = held.getItem();
        if (state.getValue(HONEY_LEVEL) != FULL_HONEY || (item != Items.SHEARS && item != Items.GLASS_BOTTLE)) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        if (!resetHoney(world, pos, state)) {
            return false;
        }
        if (item == Items.SHEARS) {
            dropCombs(world, pos);
            held.damageItem(1, player);
        } else {
            ItemStack bottle = new ItemStack(PSItems.HONEY_BOTTLE);
            held.shrink(1);
            if (held.isEmpty()) {
                player.setHeldItem(hand, bottle);
            } else {
                ItemHandlerHelper.giveItemToPlayer(player, bottle);
            }
            world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1, 1);
        }
        player.addStat(StatList.getObjectUseStats(item));
        return true;
    }

    private static boolean fullNest(IBlockState state) {
        return state.getBlock() == PSBlocks.BEE_NEST && state.getValue(HONEY_LEVEL) == FULL_HONEY;
    }

    private static boolean resetHoney(World world, BlockPos pos, IBlockState state) {
        return world.setBlockState(pos, state.withProperty(HONEY_LEVEL, 0), 3);
    }

    private static void dropCombs(World world, BlockPos pos) {
        spawnAsEntity(world, pos, new ItemStack(PSItems.HONEYCOMB, 3));
        world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1, 1);
    }

    public static void registerDispenserBehaviors() {
        registerDispenserHarvest(Items.GLASS_BOTTLE);
        registerDispenserHarvest(Items.SHEARS);
    }

    private static void registerDispenserHarvest(Item item) {
        IBehaviorDispenseItem fallback = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(item);
        IBehaviorDispenseItem harvest = new BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
                BlockPos pos = source.getBlockPos().offset(facing);
                if (item == Items.SHEARS) {
                    dropCombs(world, pos);
                    return stack.attemptDamageItem(1, world.rand, null) ? ItemStack.EMPTY : stack;
                }

                ItemStack bottle = new ItemStack(PSItems.HONEY_BOTTLE);
                stack.shrink(1);
                if (stack.isEmpty()) return bottle;
                TileEntityDispenser dispenser = source.getBlockTileEntity();
                if (dispenser.addItemStack(bottle) < 0) {
                    doDispense(world, bottle, 6, facing, BlockDispenser.getDispensePosition(source));
                }
                return stack;
            }
        };
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(item, (source, stack) -> {
            World world = source.getWorld();
            BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
            IBlockState state = world.getBlockState(pos);
            if (fullNest(state) && resetHoney(world, pos, state)) return harvest.dispense(source, stack);
            if (item == Items.SHEARS && state.getBlock() == PSBlocks.BEE_NEST) {
                world.playEvent(1001, source.getBlockPos(), 0);
                return stack;
            }
            return fallback.dispense(source, stack);
        });
    }
}
