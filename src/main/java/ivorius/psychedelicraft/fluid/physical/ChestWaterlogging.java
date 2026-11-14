package ivorius.psychedelicraft.fluid.physical;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class ChestWaterlogging extends WaterloggedFluid implements ICapabilityProvider {
    private static final String WATER = "PsychedelicraftWaterlogged";
    private final TileEntityChest chest;

    private ChestWaterlogging(TileEntityChest chest) {
        super(chest);
        this.chest = chest;
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<TileEntity> event) {
        if (event.getObject().getClass() == TileEntityChest.class) {
            event.addCapability(Psychedelicraft.id("chest_water"), new ChestWaterlogging((TileEntityChest) event.getObject()));
        }
    }

    @Override
    public boolean isWet() {
        return isWetChest(chest);
    }

    static boolean isWetChest(@Nullable TileEntity tile) {
        return tile != null && tile.getClass() == TileEntityChest.class && tile.getTileData().getBoolean(WATER);
    }

    @Override
    protected void store(boolean wet) {
        // Keep a false value after draining so existing trackers receive the change.
        chest.getTileData().setBoolean(WATER, wet);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        return hasCapability(capability, side) ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this) : null;
    }

    @Nullable
    public static SPacketUpdateTileEntity packet(TileEntity tile) {
        if (tile.getClass() != TileEntityChest.class || !tile.getTileData().hasKey(WATER)) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(WATER, tile.getTileData().getBoolean(WATER));
        return new SPacketUpdateTileEntity(tile.getPos(), 0, tag);
    }

    @SideOnly(Side.CLIENT)
    public static void receive(TileEntity tile, SPacketUpdateTileEntity packet) {
        if (tile.getClass() != TileEntityChest.class || !packet.getNbtCompound().hasKey(WATER)) return;
        tile.getTileData().setBoolean(WATER, packet.getNbtCompound().getBoolean(WATER));
        BlockPos pos = tile.getPos();
        tile.getWorld().markBlockRangeForRenderUpdate(pos.add(-1, -1, -1), pos.add(1, 1, 1));
    }
}
