package ivorius.psychedelicraft.screen;

import ivorius.psychedelicraft.block.DryingTableBlock;
import ivorius.psychedelicraft.block.BarrelBlock;
import ivorius.psychedelicraft.block.FlaskBlock;
import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import ivorius.psychedelicraft.block.entity.DistilleryBlockEntity;
import ivorius.psychedelicraft.block.entity.FlaskBlockEntity;
import ivorius.psychedelicraft.block.entity.FluidMachineBlockEntity;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.client.screen.DryingTableScreen;
import ivorius.psychedelicraft.client.screen.FluidProcessingScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public final class PSGuiHandler implements IGuiHandler {
    public static final int DRYING_TABLE = 0;
    public static final int MASH_TUB = 1;
    public static final int BARREL = 2;
    public static final int FLASK = 3;
    public static final int DISTILLERY = 4;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == FLASK || id == DISTILLERY) {
            FluidMachineBlockEntity machine = flaskMachine(id, world, new BlockPos(x, y, z));
            return machine != null && machine.isUsableByPlayer(player) ? new FluidMachineContainer(player.inventory, machine) : null;
        }
        if (id == BARREL) {
            BarrelBlockEntity barrel = BarrelBlock.getBarrel(world, new BlockPos(x, y, z));
            return barrel != null && barrel.isUsableByPlayer(player) ? new FluidMachineContainer(player.inventory, barrel) : null;
        }
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
        if (id == FLASK || id == DISTILLERY) {
            FluidMachineBlockEntity machine = flaskMachine(id, world, new BlockPos(x, y, z));
            return machine == null ? null : new FluidProcessingScreen(player.inventory, machine,
                    id == FLASK ? "flask" : "distillery", 110, 14);
        }
        if (id == BARREL) {
            BarrelBlockEntity barrel = BarrelBlock.getBarrel(world, new BlockPos(x, y, z));
            return barrel == null ? null : new FluidProcessingScreen(player.inventory, barrel, "barrel", 110, 20);
        }
        if (id == MASH_TUB) {
            MashTubBlockEntity vat = MashTubBlock.getVat(world, new BlockPos(x, y, z));
            return vat == null ? null : new FluidProcessingScreen(player.inventory, vat, "wooden_vat", 140, 14);
        }
        DryingTableBlockEntity table = DryingTableBlock.getTable(world, new BlockPos(x, y, z));
        return id == DRYING_TABLE && table != null ? new DryingTableScreen(player.inventory, table) : null;
    }

    private static FluidMachineBlockEntity flaskMachine(int id, World world, BlockPos pos) {
        FluidMachineBlockEntity machine = FlaskBlock.getMachine(world, pos);
        return (id == FLASK ? machine instanceof FlaskBlockEntity : machine instanceof DistilleryBlockEntity)
                ? machine : null;
    }
}
