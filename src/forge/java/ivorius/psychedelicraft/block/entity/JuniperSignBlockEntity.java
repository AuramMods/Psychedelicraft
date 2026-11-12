package ivorius.psychedelicraft.block.entity;

import java.util.UUID;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.block.JuniperSignBlock;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.World;

public final class JuniperSignBlockEntity extends TileEntity {
    private final JuniperSignText frontText = new JuniperSignText();
    private final JuniperSignText backText = new JuniperSignText();
    private boolean waxed;
    private UUID placementEditor;
    private UUID editor;
    private long editToken;
    private boolean editingFront;

    @Nullable
    public static JuniperSignBlockEntity get(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof JuniperSignBlockEntity ? (JuniperSignBlockEntity) tile : null;
    }

    public JuniperSignText text(boolean front) {
        return front ? frontText : backText;
    }

    public boolean isWaxed() {
        return waxed;
    }

    public boolean wax() {
        if (waxed) {
            return false;
        }
        waxed = true;
        placementEditor = null;
        editor = null;
        changed();
        return true;
    }

    public boolean setColor(boolean front, EnumDyeColor color) {
        if (waxed || !text(front).setColor(color)) {
            return false;
        }
        changed();
        return true;
    }

    public boolean setGlowing(boolean front, boolean glowing) {
        if (waxed || !text(front).setGlowing(glowing)) {
            return false;
        }
        changed();
        return true;
    }

    private boolean isNearby(@Nullable EntityPlayer player) {
        return player != null && player.isEntityAlive() && player.getDistanceSqToCenter(pos) <= 64;
    }

    public boolean canEdit(EntityPlayerMP player) {
        if (editor != null && !isNearby(world.getPlayerEntityByUUID(editor))) {
            editor = null;
        }
        return player.world == world && !waxed && !player.isSpectator() && player.capabilities.allowEdit && isNearby(player)
                && world.isBlockModifiable(player, pos)
                && player.canPlayerEdit(pos, EnumFacing.UP, ItemStack.EMPTY)
                && (editor == null || editor.equals(player.getUniqueID()));
    }

    public void queuePlacementEditor(EntityPlayerMP player) {
        placementEditor = player.getUniqueID();
        // The placement event can still roll the block back before this tick runs.
        world.scheduleUpdate(pos, world.getBlockState(pos).getBlock(), 1);
    }

    public void openQueuedEditor() {
        if (placementEditor != null) {
            EntityPlayer player = world.getPlayerEntityByUUID(placementEditor);
            placementEditor = null;
            if (player instanceof EntityPlayerMP && editor == null) {
                openEditor((EntityPlayerMP) player, true);
            }
        }
    }

    public boolean openEditor(EntityPlayerMP player, boolean front) {
        JuniperSignText face = text(front);
        if (!canEdit(player) || !face.isEditable()) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof JuniperSignBlock)) {
            return false;
        }
        editor = player.getUniqueID();
        editToken = world.rand.nextLong();
        editingFront = front;
        Channel.openSignEditor(player, pos, editToken, front, ((JuniperSignBlock) state.getBlock()).isStanding(), face);
        return true;
    }

    public void finishEdit(EntityPlayerMP player, long token, String[] lines) {
        if (editor == null || !editor.equals(player.getUniqueID()) || editToken != token || !canEdit(player)) {
            return;
        }
        editor = null;
        text(editingFront).setPlainLines(lines);
        changed();
    }

    public boolean executeClickCommands(EntityPlayerMP player, boolean front) {
        JuniperSignText face = text(front);
        boolean executed = false;
        for (int index = 0; index < JuniperSignText.LINE_COUNT; index++) {
            ClickEvent click = face.line(index).getStyle().getClickEvent();
            if (click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND) {
                // Only operator-authored block data can contain a command; edit packets store literal text.
                player.getServer().getCommandManager().executeCommand(commandSource(player), click.getValue());
                executed = true;
            }
        }
        return executed;
    }

    private ICommandSender commandSource(EntityPlayerMP player) {
        return new ICommandSender() {
            @Override public String getName() { return player.getName(); }
            @Override public boolean canUseCommand(int level, String name) { return level <= 2; }
            @Override public BlockPos getPosition() { return pos; }
            @Override public Vec3d getPositionVector() { return new Vec3d(pos).addVector(0.5, 0.5, 0.5); }
            @Override public World getEntityWorld() { return world; }
            @Override public Entity getCommandSenderEntity() { return player; }
            @Override public MinecraftServer getServer() { return player.getServer(); }
        };
    }

    private void changed() {
        if (world != null && !world.isRemote) {
            markDirty();
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        frontText.read(tag.getCompoundTag("FrontText"));
        backText.read(tag.getCompoundTag("BackText"));
        waxed = tag.getBoolean("Waxed");
        placementEditor = null;
        editor = null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("FrontText", frontText.write());
        tag.setTag("BackText", backText.write());
        tag.setBoolean("Waxed", waxed);
        return tag;
    }

    @Override
    public boolean onlyOpsCanSetNbt() {
        return true;
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState before, IBlockState after) {
        return before.getBlock() != after.getBlock();
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager network, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }
}
