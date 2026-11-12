/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.screen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.FluidMachineBlockEntity;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.screen.FluidMachineContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public final class MashTubScreen extends GuiContainer {
    private static final ResourceLocation TEXTURE = Psychedelicraft.id("textures/gui/wooden_vat.png");

    private final InventoryPlayer inventory;
    private final MashTubBlockEntity vat;

    public MashTubScreen(InventoryPlayer inventory, MashTubBlockEntity vat) {
        super(new FluidMachineContainer(inventory, vat));
        this.inventory = inventory;
        this.vat = vat;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        if (isPointInRegion(65, 33, 40, 30, mouseX, mouseY) && inventory.getItemStack().isEmpty()) {
            FluidStack fluid = vat.getTank().getFluid();
            List<String> info = new ArrayList<>();
            if (fluid != null) info.add(I18n.format(PSFluids.name(fluid)));
            info.add(I18n.format("psychedelicraft.container.levels", vat.getTank().getFluidAmount(), vat.getTank().getCapacity()));
            drawHoveringText(info, mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("tile.psychedelicraft.mash_tub.name");
        fontRenderer.drawString(title, 83 - fontRenderer.getStringWidth(title) / 2, 10, 0x404040);
        fontRenderer.drawString(inventory.getDisplayName().getFormattedText(), 8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft + 30, guiTop + 20, 0, ySize, 110, 50);

        FluidStack fluid = vat.getTank().getFluid();
        if (fluid != null) {
            int height = Math.max(1, 27 * fluid.amount / vat.getTank().getCapacity());
            int color = fluid.getFluid().getColor(fluid);
            GlStateManager.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
            for (int x = 48; x < 112; x += 16) {
                for (int y = 59 - height; y < 59; y += 16) {
                    drawTexturedModalRect(guiLeft + x, guiTop + y, sprite, 16, Math.min(16, 59 - y));
                }
            }
        }
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        float stew = vat.getStewProgress();
        if (stew > 0 && stew < 1) {
            drawTexturedModalRect(guiLeft + 140, guiTop + 14, 233, 22, 23, 22);
            int unfilled = (int) (22 * (1 - stew));
            drawTexturedModalRect(guiLeft + 140, guiTop + 15 + unfilled, 233, unfilled, 23, 23 - unfilled);
        }
        ItemStack inputStack = vat.getVessels().getStackInSlot(FluidMachineBlockEntity.INPUT);
        float input = 1 - FluidMachineBlockEntity.fillRatio(inputStack);
        if (!inputStack.isEmpty() && input > 0 && input < 1) {
            int width = (int) (45 * input);
            drawTexturedModalRect(guiLeft + 20 + width, guiTop + 40, 176 + width, 0, 45 - width, 16);
        }
        float output = FluidMachineBlockEntity.fillRatio(vat.getVessels().getStackInSlot(FluidMachineBlockEntity.OUTPUT));
        if (output > 0 && output < 1) {
            drawTexturedModalRect(guiLeft + 68, guiTop + 60, 176, 17, (int) (53 * output), 20);
        }
        GlStateManager.disableBlend();
    }
}
