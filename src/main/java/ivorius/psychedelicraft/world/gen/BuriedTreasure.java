package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandLocate;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class BuriedTreasure {
    private static final ResourceLocation LOOT = Psychedelicraft.id("chests/buried_treasure");
    private static final int SALT = 10387320;

    private BuriedTreasure() {
    }

    public static void register() {
        LootTableList.register(LOOT);
        TreasureMapLoot.register();
    }

    @SubscribeEvent
    public static void populate(PopulateChunkEvent.Pre event) {
        World world = event.getWorld();
        if (!WarmOceanRuin.allowsOceanStructures(world)) return;
        int x = event.getChunkX();
        int z = event.getChunkZ();
        if (!candidate(world, x, z)) return;
        BlockPos column = anchor(x, z);
        BlockPos surface = world.getTopSolidOrLiquidBlock(column);
        for (BlockPos pos = surface; pos.getY() > 0; pos = pos.down()) {
            IBlockState support = world.getBlockState(pos.down());
            if (!supportsChest(support)) continue;
            place(world, pos, support);
            return;
        }
    }

    private static void place(World world, BlockPos pos, IBlockState support) {
        IBlockState displaced = world.getBlockState(pos);
        if (displaced.getBlock() == Blocks.CHEST || world.getTileEntity(pos) != null) return;
        IBlockState cover = open(world, pos) ? Blocks.SAND.getDefaultState() : displaced;
        for (EnumFacing face : EnumFacing.VALUES) {
            BlockPos neighbor = pos.offset(face);
            if (open(world, neighbor)) {
                world.setBlockState(neighbor, face != EnumFacing.UP && open(world, neighbor.down()) ? support : cover, 2);
            }
        }
        IBlockState chest = Blocks.CHEST.correctFacing(world, pos, Blocks.CHEST.getDefaultState());
        if (!world.setBlockState(pos, chest, 2)) return;
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityChest) {
            long seed = new Random(seed(world.getSeed(), pos.getX() >> 4, pos.getZ() >> 4)).nextLong();
            ((TileEntityChest) tile).setLootTable(LOOT, seed == 0L ? 1L : seed);
            tile.markDirty();
        }
    }

    private static boolean supportsChest(IBlockState state) {
        return state == Blocks.SANDSTONE.getDefaultState()
                || state.getBlock() == Blocks.STONE && state.getValue(BlockStone.VARIANT).isNatural();
    }

    private static boolean open(World world, BlockPos pos) {
        return world.isAirBlock(pos) || world.getBlockState(pos).getMaterial().isLiquid();
    }

    private static long seed(long worldSeed, int x, int z) {
        return worldSeed + x * 341873128712L + z * 132897987541L + SALT;
    }

    static boolean rollsTreasure(long worldSeed, int x, int z) {
        return new Random(seed(worldSeed, x, z)).nextFloat() < .01F;
    }

    static BlockPos anchor(int x, int z) {
        return new BlockPos((x << 4) + 9, 0, (z << 4) + 9);
    }

    private static boolean candidate(World world, int x, int z) {
        return rollsTreasure(world.getSeed(), x, z) && Shipwreck.isBeach(world.getBiomeProvider().getBiome(anchor(x, z)));
    }

    @Nullable
    static BlockPos find(World world, BlockPos origin) {
        if (!WarmOceanRuin.allowsOceanStructures(world)) return null;
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        BlockPos nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (int ring = 0; ring <= 50; ring++) {
            for (int x = -ring; x <= ring; x++) {
                for (int z = -ring; z <= ring; z++) {
                    if (Math.abs(x) != ring && Math.abs(z) != ring) continue;
                    BlockPos pos = anchor(chunkX + x, chunkZ + z);
                    double distance = pos.distanceSq(origin.getX(), 0, origin.getZ());
                    if (distance < best && candidate(world, chunkX + x, chunkZ + z)) {
                        nearest = pos;
                        best = distance;
                    }
                }
            }
            double radius = (double) ring * 16;
            if (nearest != null && best <= radius * radius) break;
        }
        return nearest;
    }

    @SubscribeEvent
    public static void locate(CommandEvent event) {
        if (!(event.getCommand() instanceof CommandLocate) || event.getParameters().length != 1) return;
        String name = event.getParameters()[0];
        if (!"Buried_Treasure".equalsIgnoreCase(name) && !"psychedelicraft:buried_treasure".equals(name)) return;
        ICommandSender sender = event.getSender();
        World world = sender.getEntityWorld();
        if (world.isRemote || world.findNearestStructure(name, sender.getPosition(), false) != null) return;
        event.setCanceled(true);
        BlockPos target = find(world, sender.getPosition());
        if (target == null) {
            TextComponentTranslation failure = new TextComponentTranslation("commands.locate.failure", name);
            failure.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(failure);
        } else {
            sender.sendMessage(new TextComponentTranslation("commands.locate.success", name, target.getX(), target.getZ()));
        }
    }
}
