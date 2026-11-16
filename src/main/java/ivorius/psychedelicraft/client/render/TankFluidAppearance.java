/* Copyright (c) 2014, Lukas Tenbrink. Native appearance selection by Sollace. */
package ivorius.psychedelicraft.client.render;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public record TankFluidAppearance(TextureAtlasSprite sprite,int color) {
    public static TankFluidAppearance of(FluidStack stack,BlockPos position){
        var client=Minecraft.getInstance();var extension=IClientFluidTypeExtensions.of(stack.getFluid());ResourceLocation still=extension.getStillTexture(stack);int color=extension.getTintColor(stack);
        if(stack.getFluid()==Fluids.WATER&&client.level!=null)color=0xff000000|client.getBlockColors().getColor(Blocks.WATER.defaultBlockState(),client.level,position,0);
        else if(stack.getFluid()==PSFluids.KAVA.get()||PSFluids.isFlowerExtract(stack.getFluid())){still=new ResourceLocation("minecraft","block/water_still");color|=0xff000000;}
        else if(still!=null&&still.getNamespace().equals(Psychedelicraft.MOD_ID))color=0xffffffff;
        if(still==null)still=new ResourceLocation("minecraft","missingno");
        return new TankFluidAppearance(client.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still),color);
    }
}
