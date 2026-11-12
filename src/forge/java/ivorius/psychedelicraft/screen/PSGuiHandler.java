package ivorius.psychedelicraft.screen;

import ivorius.psychedelicraft.block.DryingTableBlock;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.client.screen.DryingTableScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public final class PSGuiHandler implements IGuiHandler {
    public static final int DRYING_TABLE = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        DryingTableBlockEntity table = DryingTableBlock.getTable(world, new BlockPos(x, y, z));
        return id == DRYING_TABLE && table != null && table.isUsableByPlayer(player)
                ? new DryingTableContainer(player.inventory, table) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        DryingTableBlockEntity table = DryingTableBlock.getTable(world, new BlockPos(x, y, z));
        return id == DRYING_TABLE && table != null ? new DryingTableScreen(player.inventory, table) : null;
    }
}
