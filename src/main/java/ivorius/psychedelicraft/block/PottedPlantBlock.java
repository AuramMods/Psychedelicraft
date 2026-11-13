package ivorius.psychedelicraft.block;

import java.util.function.Supplier;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PottedPlantBlock extends Block {
    private static final AxisAlignedBB POT = new AxisAlignedBB(0.3125, 0, 0.3125, 0.6875, 0.375, 0.6875);
    private final Supplier<Item> plant;

    PottedPlantBlock(Supplier<Item> plant) {
        super(Material.CIRCUITS);
        this.plant = plant;
        setHardness(0);
        setSoundType(SoundType.STONE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fillPot(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || player.isSpectator()
                || event.getUseBlock() == Event.Result.DENY
                || !world.isBlockModifiable(player, pos) || world.getBlockState(pos).getBlock() != Blocks.FLOWER_POT) {
            return;
        }
        if (player.isSneaking() && event.getUseBlock() != Event.Result.ALLOW
                && !(player.getHeldItemMainhand().doesSneakBypassUse(world, pos, player)
                && player.getHeldItemOffhand().doesSneakBypassUse(world, pos, player))) {
            return;
        }
        PottedPlantBlock filled = forItem(held.getItem());
        if (filled == null) return;

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityFlowerPot) || !((TileEntityFlowerPot) tile).getFlowerItemStack().isEmpty()) {
            return;
        }
        if (!world.isRemote) {
            if (!world.setBlockState(pos, filled.getDefaultState(), 3)) return;
            if (!player.capabilities.isCreativeMode) held.shrink(1);
            player.addStat(StatList.FLOWER_POTTED);
        }
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static PottedPlantBlock forItem(Item item) {
        for (PottedPlantBlock filled : PSBlocks.POTTED_PLANTS) {
            if (item == filled.plant.get()) return filled;
        }
        return null;
    }

    private static boolean isVanillaPottable(ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        return block == Blocks.YELLOW_FLOWER || block == Blocks.RED_FLOWER || block == Blocks.CACTUS
                || block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM || block == Blocks.SAPLING
                || block == Blocks.DEADBUSH || (block == Blocks.TALLGRASS
                && stack.getMetadata() == BlockTallGrass.EnumType.FERN.getMeta());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing face, float x, float y, float z) {
        ItemStack held = player.getHeldItem(hand);
        if (forItem(held.getItem()) == null && !isVanillaPottable(held)
                && !world.isRemote && world.setBlockState(pos, Blocks.FLOWER_POT.getDefaultState(), 3)) {
            ItemStack contents = new ItemStack(plant.get());
            if (held.isEmpty()) {
                player.setHeldItem(hand, contents);
            } else if (!player.addItemStackToInventory(contents)) {
                player.dropItem(contents, false);
            }
        }
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return POT;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(plant.get());
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(new ItemStack(Items.FLOWER_POT));
        drops.add(new ItemStack(plant.get()));
    }
}
