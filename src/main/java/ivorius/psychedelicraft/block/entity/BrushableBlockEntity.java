package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.SuspiciousSandBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public final class BrushableBlockEntity extends TileEntity {
    private static final int REQUIRED_STROKES = 10;
    private static final int STROKE_COOLDOWN = 10;
    private static final int RESET_DELAY = 40;

    private ItemStack item = ItemStack.EMPTY;
    @Nullable private EnumFacing face;
    @Nullable private ResourceLocation lootTable;
    private long lootSeed;
    private int strokes;
    private long nextStroke;
    private long resetAt;

    public void setLootTable(ResourceLocation table, long seed) {
        lootTable = table;
        lootSeed = seed;
        item = ItemStack.EMPTY;
        face = null;
        strokes = 0;
        nextStroke = 0;
        resetAt = 0;
        markDirty();
    }

    @Override
    public void onLoad() {
        if (!world.isRemote && strokes > 0) {
            world.scheduleUpdate(pos, PSBlocks.SUSPICIOUS_SAND, (int) MathHelper.clamp(
                    resetAt - world.getTotalWorldTime(), 1L, RESET_DELAY));
        }
    }

    public ItemStack getExposedItem() {
        return face == null ? ItemStack.EMPTY : item;
    }

    @Nullable
    public EnumFacing getBrushFace() {
        return face;
    }

    public boolean brush(EntityPlayer player, EnumFacing side) {
        if (!(world instanceof WorldServer) || isInvalid()) return false;
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof SuspiciousSandBlock)) return false;
        long now = world.getTotalWorldTime();
        resetAt = now + RESET_DELAY;
        if (now < nextStroke) return false;
        nextStroke = now + STROKE_COOLDOWN;
        if (face == null) face = side;
        revealLoot((WorldServer) world, player);
        if (++strokes == REQUIRED_STROKES) {
            if (complete(state)) return true;
            strokes--;
        }
        updateSurface(state);
        world.scheduleUpdate(pos, state.getBlock(), RESET_DELAY);
        return false;
    }

    private void revealLoot(WorldServer server, EntityPlayer player) {
        if (lootTable == null) return;
        LootContext context = new LootContext.Builder(server).withPlayer(player).withLootedEntity(player)
                .withLuck(player.getLuck()).build();
        List<ItemStack> loot = server.getLootTableManager().getLootTableFromLocation(lootTable)
                .generateLootForPools(new Random(lootSeed), context);
        item = loot.isEmpty() ? ItemStack.EMPTY : loot.get(0);
        lootTable = null;
        lootSeed = 0;
    }

    private boolean complete(IBlockState state) {
        ItemStack recovered = item;
        BlockPos exit = pos.offset(face == null ? EnumFacing.UP : face);
        if (!world.setBlockState(pos, Blocks.SAND.getDefaultState(), 3)) return false;
        if (!recovered.isEmpty()) {
            EntityItem drop = new EntityItem(world, exit.getX() + .5, exit.getY() + .625, exit.getZ() + .5, recovered);
            drop.motionX = drop.motionY = drop.motionZ = 0;
            drop.setDefaultPickupDelay();
            world.spawnEntity(drop);
        }
        world.playEvent(2001, pos, Block.getStateId(state));
        world.playSound(null, pos, PSSounds.BRUSH_SAND_COMPLETE, SoundCategory.BLOCKS, 1F, 1F);
        return true;
    }

    public void checkReset() {
        if (strokes == 0) return;
        long now = world.getTotalWorldTime();
        IBlockState state = world.getBlockState(pos);
        if (now >= resetAt) {
            strokes = Math.max(0, strokes - 2);
            if (strokes == 0) {
                face = null;
                nextStroke = 0;
            }
            resetAt = now + 4;
            updateSurface(state);
        }
        if (strokes != 0) world.scheduleUpdate(pos, state.getBlock(), (int) Math.max(1, resetAt - now));
    }

    static int dustStage(int strokes) {
        if (strokes == 0) return 0;
        if (strokes < 3) return 1;
        return strokes < 6 ? 2 : 3;
    }

    private void updateSurface(IBlockState state) {
        markDirty();
        int dust = dustStage(strokes);
        if (state.getValue(SuspiciousSandBlock.DUSTED) != dust) {
            world.setBlockState(pos, state.withProperty(SuspiciousSandBlock.DUSTED, dust), 3);
        } else {
            world.notifyBlockUpdate(pos, state, state, 2);
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Strokes", strokes);
        tag.setLong("NextStroke", nextStroke);
        tag.setLong("ResetAt", resetAt);
        if (face != null) tag.setByte("BrushFace", (byte) face.getIndex());
        if (lootTable != null) {
            tag.setString("LootTable", lootTable.toString());
            tag.setLong("LootTableSeed", lootSeed);
        } else if (!item.isEmpty()) {
            tag.setTag("Item", item.writeToNBT(new NBTTagCompound()));
        }
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        strokes = MathHelper.clamp(tag.getInteger("Strokes"), 0, REQUIRED_STROKES - 1);
        nextStroke = tag.getLong("NextStroke");
        resetAt = tag.getLong("ResetAt");
        lootTable = tag.hasKey("LootTable", Constants.NBT.TAG_STRING) ? new ResourceLocation(tag.getString("LootTable")) : null;
        lootSeed = tag.getLong("LootTableSeed");
        readAppearance(tag);
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readAppearance(tag);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.writeToNBT(new NBTTagCompound());
        if (face != null) tag.setByte("BrushFace", (byte) face.getIndex());
        if (!getExposedItem().isEmpty()) tag.setTag("Item", item.writeToNBT(new NBTTagCompound()));
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager network, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }

    private void readAppearance(NBTTagCompound tag) {
        face = tag.hasKey("BrushFace", Constants.NBT.TAG_BYTE) ? EnumFacing.getFront(tag.getByte("BrushFace")) : null;
        item = new ItemStack(tag.getCompoundTag("Item"));
    }
}
