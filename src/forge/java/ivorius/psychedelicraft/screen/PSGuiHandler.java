package ivorius.psychedelicraft.screen;

import ivorius.psychedelicraft.block.DryingTableBlock;
import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.client.screen.DryingTableScreen;
import ivorius.psychedelicraft.client.screen.MashTubScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public final class PSGuiHandler implements IGuiHandler {
    public static final int DRYING_TABLE = 0;
    public static final int MASH_TUB = 1;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == MASH_TUB) {
            MashTubBlockEntity vat = MashTubBlock.getVat(world, new BlockPos(x, y, z));
            return vat != null && vat.isUsableByPlayer(player) ? new FluidMachineContainer(player.inventory, vat) : null;
        }
        DryingTableBlockEntity table = DryingTableBlock.getTable(world, new BlockPos(x, y, z));
        return id == DRYING_TABLE && table != null && table.isUsableByPlayer(player)
                ? new DryingTableContainer(player.inventory, table) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == MASH_TUB) {
            MashTubBlockEntity vat = MashTubBlock.getVat(world, new BlockPos(x, y, z));
            return vat == null ? null : new MashTubScreen(player.inventory, vat);
        }
        DryingTableBlockEntity table = DryingTableBlock.getTable(world, new BlockPos(x, y, z));
        return id == DRYING_TABLE && table != null ? new DryingTableScreen(player.inventory, table) : null;
    }
}
