/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.screen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import ivorius.psychedelicraft.block.entity.DistilleryBlockEntity;
import ivorius.psychedelicraft.block.entity.FluidMachineBlockEntity;
import ivorius.psychedelicraft.block.entity.FluidProcessingBlockEntity;
import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.BrewProcess;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.screen.FluidMachineContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FluidProcessingScreen extends GuiContainer {
    private final ResourceLocation texture;
    private final int progressX;
    private final int progressY;

    private final InventoryPlayer inventory;
    private final FluidMachineBlockEntity machine;

    public FluidProcessingScreen(InventoryPlayer inventory, FluidMachineBlockEntity machine,
                                 String background, int progressX, int progressY) {
        super(new FluidMachineContainer(inventory, machine));
        this.inventory = inventory;
        this.machine = machine;
        this.texture = Psychedelicraft.id("textures/gui/" + background + ".png");
        this.progressX = progressX;
        this.progressY = progressY;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        if (isPointInRegion(65, 33, 40, 30, mouseX, mouseY) && inventory.getItemStack().isEmpty()) {
            drawHoveringText(tankTooltip(), mouseX, mouseY);
        }
    }

    private List<String> tankTooltip() {
        List<String> info = new ArrayList<>();
        FluidStack fluid = machine.getTank().getFluid();
        boolean brew = fluid != null && fluid.getFluid() instanceof BrewFluid;
        info.add(fluid == null ? I18n.format("fluid.psychedelicraft.empty") : PSFluids.displayName(fluid));
        if (fluid != null) {
            info.add(TextFormatting.GRAY + I18n.format("psychedelicraft.container.levels", fluid.amount,
                    machine.getTank().getCapacity()));
            if (brew) BrewFluid.appendAttributes(fluid, info);
        }
        if (machine instanceof FluidProcessingBlockEntity) {
            FluidProcessingBlockEntity processor = (FluidProcessingBlockEntity) machine;
            BrewProcess stage = processor.getStage();
            if (brew || stage != BrewProcess.IDLE) {
                info.add(I18n.format("fluid.status", I18n.format("fluid.status." + stage.key())));
            }
            if (stage != BrewProcess.IDLE) {
                info.add(I18n.format("time.until." + stage.key(), formatCountdown(processor.getRemainingTicks())));
            }
        }
        if (brew) {
            boolean fermented = BrewFluid.stage(fluid, BrewFluid.FERMENTATION, BrewFluid.FERMENTATION_STEPS) > 0;
            if (machine instanceof DistilleryBlockEntity) {
                boolean unaged = BrewFluid.stage(fluid, BrewFluid.MATURATION, BrewFluid.MATURATION_STEPS) == 0;
                info.add(requirement(fermented, "psychedelicraft.processing.fermented",
                        "psychedelicraft.distillery.requires_fermentation"));
                info.add(requirement(unaged, "psychedelicraft.distillery.unaged",
                        "psychedelicraft.distillery.requires_unaged"));
            } else if (machine instanceof BarrelBlockEntity) {
                info.add(requirement(fermented, "psychedelicraft.processing.fermented",
                        "psychedelicraft.barrel.requires_fermentation"));
            }
        }
        if (machine instanceof DistilleryBlockEntity) {
            DistilleryBlockEntity.Outlet outlet = ((DistilleryBlockEntity) machine).getOutlet();
            if (outlet != DistilleryBlockEntity.Outlet.READY) {
                info.add(warning(outlet.key()));
            }
        }
        return info;
    }

    private static String requirement(boolean met, String satisfied, String needed) {
        return met ? TextFormatting.GRAY + I18n.format(satisfied) : warning(needed);
    }

    private static String warning(String key) {
        return TextFormatting.RED.toString() + TextFormatting.ITALIC + I18n.format(key);
    }

    private static String formatCountdown(int ticks) {
        int seconds = ticks / 20;
        int hours = seconds / 3600;
        int minutes = seconds / 60 % 60;
        return hours > 0 ? String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds % 60)
                : String.format(Locale.ROOT, "%02d:%02d", minutes, seconds % 60);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format(machine.getBlockType().getUnlocalizedName() + ".name");
        fontRenderer.drawString(title, 83 - fontRenderer.getStringWidth(title) / 2, 10, 0x404040);
        fontRenderer.drawString(inventory.getDisplayName().getFormattedText(), 8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(texture);
        drawTexturedModalRect(guiLeft + 30, guiTop + 20, 0, ySize, 110, 50);

        FluidStack fluid = machine.getTank().getFluid();
        if (fluid != null) drawFluid(fluid);
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(texture);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        float progress = machine instanceof FluidProcessingBlockEntity
                ? ((FluidProcessingBlockEntity) machine).getProcessingProgress() : 0;
        if (machine instanceof DistilleryBlockEntity || progress > 0 && progress < 1) {
            drawTexturedModalRect(guiLeft + progressX, guiTop + progressY, 233, 22, 23, 22);
            int unfilled = (int) (22 * (1 - progress));
            drawTexturedModalRect(guiLeft + progressX, guiTop + progressY + unfilled, 233, unfilled, 23, 23 - unfilled);
        }
        ItemStack inputStack = machine.getVessels().getStackInSlot(FluidMachineBlockEntity.INPUT);
        float input = machine.getVesselProgress(FluidMachineBlockEntity.INPUT);
        if (!inputStack.isEmpty() && input > 0 && input < 1) {
            int width = (int) (45 * input);
            drawTexturedModalRect(guiLeft + 20 + width, guiTop + 40, 176 + width, 0, 45 - width, 16);
        }
        float output = machine.getVesselProgress(FluidMachineBlockEntity.OUTPUT);
        if (output > 0 && output < 1) {
            drawTexturedModalRect(guiLeft + 68, guiTop + 60, 176, 17, (int) (53 * output), 20);
        }
        GlStateManager.disableBlend();
    }

    private void drawFluid(FluidStack fluid) {
        int height = MathHelper.ceil(27F * MathHelper.clamp((float) fluid.amount / machine.getTank().getCapacity(), 0F, 1F));
        int color = fluid.getFluid().getColor(fluid);
        GlStateManager.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(fluid.getFluid().getStill(fluid).toString());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertices = tessellator.getBuffer();
        vertices.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        for (int x = guiLeft + 48; x < guiLeft + 112; x += 16) {
            for (int top = guiTop + 59 - height; top < guiTop + 59; top += 16) {
                int rows = Math.min(16, guiTop + 59 - top);
                float bottomV = sprite.getInterpolatedV(rows);
                vertices.pos(x, top + rows, zLevel).tex(sprite.getMinU(), bottomV).endVertex();
                vertices.pos(x + 16, top + rows, zLevel).tex(sprite.getMaxU(), bottomV).endVertex();
                vertices.pos(x + 16, top, zLevel).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
                vertices.pos(x, top, zLevel).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            }
        }
        tessellator.draw();
    }
}
