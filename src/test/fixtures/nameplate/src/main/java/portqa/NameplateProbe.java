package portqa;

import ivorius.psychedelicraft.client.render.DrugRenderer;
import ivorius.psychedelicraft.client.render.HallucinationManager;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

@Mod(modid = "portnameplateqa", name = "Nameplate probe", version = "1", clientSideOnly = true)
@Mod.EventBusSubscriber(modid = "portnameplateqa")
public final class NameplateProbe {
    private static final Minecraft CLIENT = Minecraft.getMinecraft();
    private static final FloatBuffer UNIFORM = BufferUtils.createFloatBuffer(16);
    private static final ByteBuffer IMAGE = BufferUtils.createByteBuffer(320 * 96 * 4);
    private static final boolean BASELINE = "baseline".equals(System.getProperty("portqa.mode"));
    private static boolean launched;
    private static boolean finished;
    private static int frames;
    private static FontProbe font;

    @SubscribeEvent
    public static void endTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (finished) {
            CLIENT.shutdown();
        } else if (!launched && CLIENT.currentScreen instanceof GuiMainMenu) {
            launched = true;
            CLIENT.gameSettings.renderDistanceChunks = 4;
            CLIENT.gameSettings.pauseOnLostFocus = false;
            CLIENT.launchIntegratedServer("NameplateQA", "Nameplate QA",
                    new WorldSettings(9124L, GameType.SURVIVAL, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void sample(RenderWorldLastEvent event) {
        if (finished || CLIENT.player == null || CLIENT.currentScreen != null || ++frames < 10) return;
        font = new FontProbe();
        HallucinationManager effects = ReflectionHelper.getPrivateValue(DrugRenderer.class, null, "hallucinations");
        float[] strengths = ReflectionHelper.getPrivateValue(HallucinationManager.class, effects, "strengths");
        float[] categories = ReflectionHelper.getPrivateValue(HallucinationManager.class, effects, "categoryStrengths");
        float[] mindColor = ReflectionHelper.getPrivateValue(HallucinationManager.class, effects, "mindColor");
        Arrays.fill(strengths, 0F);
        Arrays.fill(categories, 1F);
        mindColor[0] = 1F;
        mindColor[1] = 0F;
        mindColor[2] = 0F;
        strengths[Effect.PULSES.ordinal()] = 1F;
        CLIENT.player.ticksExisted = 24;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(-3, 3, -1.5, 1.5, 0, 20);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        try {
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.disableCull();
            GlStateManager.disableFog();
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.clearColor(0.25F, 0.25F, 0.25F, 1F);

            long visibleNative = draw(false, false, false);
            long occludedNative = draw(false, true, false);
            long sneakingNative = draw(false, true, true);
            long visible = draw(true, false, false);
            long occluded = draw(true, true, false);
            long sneaking = draw(true, true, true);
            strengths[Effect.WIGGLE_WAVES.ordinal()] = 0.4F;
            strengths[Effect.SURFACE_FRACTALS.ordinal()] = 0.15F;
            draw(true, false, false);
            require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error");
            require(sneakingNative == sneaking && sneaking == 0, "sneaking label hidden behind surface");
            require((visible != visibleNative) != BASELINE, "visible label pixel result");
            require((occluded != occludedNative) != BASELINE, "see-through label pixel result");
            require(visible > occluded && occluded > 0, "visible text adds a distinct foreground pass");
            LogManager.getLogger().info("NAMEPLATE_PASS baseline={} nativeVisible={} nativeOccluded={} shadedVisible={} shadedOccluded={} sneaking={}",
                    BASELINE, visibleNative, occludedNative, visible, occluded, sneaking);
            finished = true;
        } finally {
            Arrays.fill(strengths, 0);
            PSConfig.visual.shader3DEnabled = true;
            GlStateManager.clearDepth(1);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GL11.glPopAttrib();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }
    }

    private static long draw(boolean enabled, boolean blocked, boolean sneaking) {
        boolean shaded = enabled && !BASELINE;
        PSConfig.visual.shader3DEnabled = enabled;
        DrugRenderer.captureEntityView();
        GlStateManager.clearDepth(blocked ? 0.1 : 1);
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        font.reset(shaded, sneaking);
        int previous = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        EntityRenderer.drawNameplate(font, "Label HHH", 0, 0, -8, 0, 0, 0, false, sneaking);
        require(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) == previous, "restored program");
        require(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) && GL11.glIsEnabled(GL11.GL_DEPTH_TEST), "restored depth state");
        require(font.calls == (sneaking ? 1 : 2), "native label passes");
        int count = CLIENT.getFramebuffer().framebufferWidth;
        int height = CLIENT.getFramebuffer().framebufferHeight;
        IMAGE.clear();
        GL11.glReadPixels(count / 2 - 160, height / 2 - 48, 320, 96, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, IMAGE);
        long changes = 0;
        for (int i = 0; i < IMAGE.capacity(); i += 4) {
            int red = IMAGE.get(i) & 255, green = IMAGE.get(i + 1) & 255, blue = IMAGE.get(i + 2) & 255;
            changes += Math.abs(red - 64) + Math.abs(green - 64) + Math.abs(blue - 64);
        }
        return changes;
    }

    private static void require(boolean condition, String failure) {
        if (!condition) throw new AssertionError(failure);
    }

    private static final class FontProbe extends FontRenderer {
        private int calls;
        private boolean shaded;
        private boolean sneaking;

        FontProbe() {
            super(CLIENT.gameSettings, new ResourceLocation("textures/font/ascii.png"), CLIENT.getTextureManager(), false);
            onResourceManagerReload(CLIENT.getResourceManager());
        }

        void reset(boolean shaded, boolean sneaking) {
            calls = 0;
            this.shaded = shaded;
            this.sneaking = sneaking;
        }

        @Override
        public int drawString(String text, int x, int y, int color) {
            boolean seeThrough = !sneaking && calls == 0;
            calls++;
            int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            require((program != 0) == shaded, "font shader scope");
            require(GL11.glIsEnabled(GL11.GL_DEPTH_TEST) != seeThrough, "font depth test");
            require(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) != seeThrough, "font depth write");
            if (shaded) {
                UNIFORM.clear();
                GL20.glGetUniform(program, GL20.glGetUniformLocation(program, "PulseShape"), UNIFORM);
                require(UNIFORM.get(0) == (seeThrough ? 0F : 3F), "font pulse phase");
            }
            return super.drawString(text, x, y, color);
        }
    }
}
