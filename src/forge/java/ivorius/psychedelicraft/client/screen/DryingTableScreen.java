/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on 3 Jan 2023.
 */

package ivorius.psychedelicraft.client.screen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.screen.DryingTableContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

public final class DryingTableScreen extends GuiContainer {
    private static final ResourceLocation TEXTURE = Psychedelicraft.id("textures/gui/drying_table.png");
    private final InventoryPlayer inventory;
    private final DryingTableBlockEntity table;

    public DryingTableScreen(InventoryPlayer inventory, DryingTableBlockEntity table) {
        super(new DryingTableContainer(inventory, table));
        this.inventory = inventory;
        this.table = table;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        Slot hovered = getSlotUnderMouse();
        if (inventory.getItemStack().isEmpty() && (hovered == null || !hovered.getHasStack())
                && mouseX > guiLeft + xSize - 30 && mouseX < guiLeft + xSize
                && mouseY > guiTop && mouseY < guiTop + 30) {
            drawHoveringText(I18n.format("block.psychedelicraft.drying_table.daylight",
                    (int) (table.getHeatRatio() * 100)), mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(table.getDisplayName().getFormattedText(), 26, 6, 0x404040);
        fontRenderer.drawString(inventory.getDisplayName().getFormattedText(), 8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        if (table.getDryingProgress() > 0) {
            drawTexturedModalRect(guiLeft + 88, guiTop + 34, 176, 59, 25, 16);
        }
        int progress = (int) (table.getDryingProgress() * 24);
        drawTexturedModalRect(guiLeft + 88, guiTop + 34, 176, 42, progress + 1, 16);
        int heat = (int) (table.getHeatRatio() * 20);
        drawTexturedModalRect(guiLeft + 148, guiTop + 26 - heat, 176, 41 - heat, 20, heat);
    }
}
