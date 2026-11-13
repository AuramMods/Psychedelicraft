/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.FlaskBlockEntity;
import ivorius.psychedelicraft.screen.PSGuiHandler;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class FlaskBlock extends FluidMachineBlock {
    private final AxisAlignedBB[] shape;
    private final AxisAlignedBB bounds;
    private final int gui;

    FlaskBlock() {
        this(PSGuiHandler.FLASK, box(5, 0, 5, 11, 5, 11), box(4, 0, 6, 12, 4, 10),
                box(6, 0, 4, 10, 4, 12), box(6, 5, 6, 10, 11, 10),
                box(5, 7, 6, 11, 10, 10), box(6, 7, 5, 10, 10, 11));
    }

    protected FlaskBlock(int gui, AxisAlignedBB... shape) {
        super(Material.IRON);
        this.gui = gui;
        this.shape = shape;
        AxisAlignedBB bounds = shape[0];
        for (int i = 1; i < shape.length; i++) bounds = bounds.union(shape[i]);
        this.bounds = bounds;
        setHardness(1);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new FlaskBlockEntity();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return bounds;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB query,
                                     List<AxisAlignedBB> boxes, @Nullable Entity entity, boolean actualState) {
        for (AxisAlignedBB part : shape) addCollisionBoxToList(pos, query, boxes, part);
    }

    protected static AxisAlignedBB box(int x, int y, int z, int maxX, int maxY, int maxZ) {
        return new AxisAlignedBB(x / 16.0, y / 16.0, z / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (getMachine(world, pos) == null) return false;
        if (!world.isRemote) openMachine(world, pos, player, gui);
        return true;
    }
}
