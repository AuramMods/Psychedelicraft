package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.BeeNestBlockEntity;
import ivorius.psychedelicraft.client.particle.HoneyDripParticle;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class BeeNestBlock extends Block {
    public static final int FULL_HONEY = 5;
    public static final PropertyInteger HONEY_LEVEL = PropertyInteger.create("honey_level", 0, FULL_HONEY);
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private static final String STATE_TAG = "BlockStateTag";

    BeeNestBlock() {
        super(Material.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(HONEY_LEVEL, 0).withProperty(FACING, EnumFacing.NORTH));
        setHardness(0.3F);
        setSoundType(SoundType.WOOD);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HONEY_LEVEL, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HONEY_LEVEL, meta >= 0 && meta <= FULL_HONEY ? meta : 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        // Keep legacy honey metadata. The block entity stores the entrance direction.
        return state.getValue(HONEY_LEVEL);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new BeeNestBlockEntity(state.getValue(FACING));
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof BeeNestBlockEntity ? state.withProperty(FACING, ((BeeNestBlockEntity) tile).getFacing()) : state;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withProperty(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof BeeNestBlockEntity)) return false;
        BeeNestBlockEntity nest = (BeeNestBlockEntity) tile;
        nest.setFacing(nest.getFacing().rotateY());
        return true;
    }

    public void setFacing(World world, BlockPos pos, EnumFacing facing) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof BeeNestBlockEntity) ((BeeNestBlockEntity) tile).setFacing(facing);
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
        TileEntity tile = world.getTileEntity(pos);
        BeeNestBlockEntity nest = tile instanceof BeeNestBlockEntity ? (BeeNestBlockEntity) tile : null;
        boolean silk = willHarvest && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, player.getHeldItemMainhand()) > 0;
        if (nest != null) nest.setMovingWithOccupants(player.isCreative() || silk);
        boolean removed = super.removedByPlayer(state, world, pos, player, willHarvest);
        if (!removed && nest != null) nest.setMovingWithOccupants(false);
        if (removed && !world.isRemote && player.isCreative() && (state.getValue(HONEY_LEVEL) > 0
                || nest != null && nest.getOccupantCount() > 0)) {
            spawnAsEntity(world, pos, nestItem(state, nest));
        }
        return removed;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof BeeNestBlockEntity) ((BeeNestBlockEntity) tile).releaseOnBreak();
        super.breakBlock(world, pos, state);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity tile, ItemStack tool) {
        if (!(tile instanceof BeeNestBlockEntity) || ((BeeNestBlockEntity) tile).getOccupantCount() == 0
                || EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
            super.harvestBlock(world, player, pos, state, tile, tool);
            return;
        }
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);
        List<ItemStack> drops = new ArrayList<>();
        drops.add(nestItem(state, (BeeNestBlockEntity) tile));
        ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, 0, 1F, true, player);
        for (ItemStack drop : drops) spawnAsEntity(world, pos, drop);
    }

    private ItemStack nestItem(IBlockState state, @Nullable BeeNestBlockEntity nest) {
        ItemStack item = getSilkTouchDrop(state);
        if (nest != null && nest.getOccupantCount() > 0) {
            item.getOrCreateSubCompound("BlockEntityTag").setTag("Bees", nest.writeOccupants());
        }
        return item;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        setFacing(world, pos, placer.getHorizontalFacing().getOpposite());
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
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (state.getValue(HONEY_LEVEL) == FULL_HONEY) HoneyDripParticle.trySpawn(world, pos);
    }

    public static boolean isFlower(IBlockState state) {
        Block block = state.getBlock();
        return block instanceof BlockFlower || (block instanceof BlockDoublePlant
                && state.getValue(BlockDoublePlant.HALF) == BlockDoublePlant.EnumBlockHalf.LOWER
                && state.getValue(BlockDoublePlant.VARIANT) != BlockDoublePlant.EnumPlantType.GRASS
                && state.getValue(BlockDoublePlant.VARIANT) != BlockDoublePlant.EnumPlantType.FERN);
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
