package portqa;

import java.nio.ByteBuffer;
import java.io.IOException;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.HallucinationManager;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Material;
import ivorius.psychedelicraft.client.render.shader.WorldGeometryShader.Scope;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.NVFogDistance;

@Mod(modid = "portpulsetest", name = "Terrain pulse probe", version = "1", clientSideOnly = true)
@Mod.EventBusSubscriber(modid = "portpulsetest")
public final class TerrainPulseProbe {
    private static final Minecraft CLIENT = Minecraft.getMinecraft();
    private static final ByteBuffer PIXEL = BufferUtils.createByteBuffer(4);
    private static boolean launched;
    private static boolean done;
    private static int frames;
    private static int width;
    private static int height;
    private static final boolean OLD_TERRAIN = "baseline".equals(System.getProperty("portqa.mode"));
    private static final boolean OLD_WORLD = "previous".equals(System.getProperty("portqa.mode"));

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (done) {
            CLIENT.shutdown();
        } else if (!launched && CLIENT.currentScreen instanceof GuiMainMenu) {
            launched = true;
            CLIENT.gameSettings.renderDistanceChunks = 4;
            String save = "TerrainPulse-" + System.currentTimeMillis();
            CLIENT.launchIntegratedServer(save, save, new WorldSettings(123L, GameType.SURVIVAL,
                    false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public static void sample(RenderWorldLastEvent event) throws IOException {
        if (done || CLIENT.player == null || CLIENT.currentScreen != null || ++frames < 8) return;
        WorldGeometryShader shader = new WorldGeometryShader();
        HallucinationManager hallucinations = new HallucinationManager();
        float[] strengths = ReflectionHelper.getPrivateValue(HallucinationManager.class, hallucinations, "strengths");
        float[] categories = ReflectionHelper.getPrivateValue(HallucinationManager.class, hallucinations, "categoryStrengths");
        java.util.Arrays.fill(categories, 1F);
        strengths[Effect.PULSES.ordinal()] = 1F;

        width = CLIENT.getFramebuffer().framebufferWidth;
        height = CLIENT.getFramebuffer().framebufferHeight;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-12, 12, -12, 12, 0, 20);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        int texture = GL11.glGenTextures();
        try {
            GL11.glViewport(0, 0, width, height);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_FOG);
            if (GLContext.getCapabilities().GL_NV_fog_distance) {
                GL11.glFogi(NVFogDistance.GL_FOG_DISTANCE_MODE_NV, NVFogDistance.GL_EYE_PLANE_ABSOLUTE_NV);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            ByteBuffer white = BufferUtils.createByteBuffer(4);
            white.putInt(-1).flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, white);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glClearColor(0, 0, 0, 1);
            if (!OLD_TERRAIN) sampleMaterials(shader, hallucinations, texture);

            int flat;
            int side;
            int corner;
            int interpolated;
            try (Scope ignored = start(shader, hallucinations, true)) {
                quad(-0.5, -0.5, 0.5, 0.5);
                quad(7.5, -0.5, 8.5, 0.5);
                quad(9.5, 9.5, 10.5, 10.5);
                flat = red(0, 0);
                side = red(8, 0);
                corner = red(10, 10);
                quad(-8, -3, 8, -2);
                interpolated = red(0, -2.5);
            }

            int tilted;
            GL11.glRotatef(90, 0, 0, 1);
            try (Scope ignored = start(shader, hallucinations, true)) {
                quad(9.5, 9.5, 10.5, 10.5);
                tilted = red(-10, 10);
            }
            GL11.glLoadIdentity();

            CLIENT.player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 100));
            int blind;
            int blindSlope;
            try (Scope ignored = start(shader, hallucinations, true)) {
                quad(9.5, 9.5, 10.5, 10.5);
                blind = red(10, 10);
                quad(2.5, 4.5, 3.5, 5.5);
                blindSlope = red(3, 5);
            } finally {
                CLIENT.player.removePotionEffect(MobEffects.BLINDNESS);
            }

            GL11.glEnable(GL11.GL_FOG);
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
            GL11.glFogf(GL11.GL_FOG_START, 0F);
            GL11.glFogf(GL11.GL_FOG_END, 8F);
            GL11.glFog(GL11.GL_FOG_COLOR, (java.nio.FloatBuffer) BufferUtils.createFloatBuffer(4)
                    .put(new float[] {1, 1, 1, 1}).flip());
            int fogCenter;
            int fogSide;
            try (Scope ignored = start(shader, hallucinations, false)) {
                quad(-0.5, -0.5, 0.5, 0.5);
                quad(7.5, -0.5, 8.5, 0.5);
                fogCenter = red(0, 0);
                fogSide = red(8, 0);
            }
            LogManager.getLogger().info("PULSE_SAMPLES baseline={} center={} side={} corner={} tilted={} blind={} blindSlope={} interpolated={} fogCenter={} fogSide={}",
                    OLD_TERRAIN, flat, side, corner, tilted, blind, blindSlope, interpolated, fogCenter, fogSide);
            near(flat, 64, 2, "center");
            near(side, OLD_TERRAIN ? 64 : shade(Math.hypot(8, 2)), 3, "side");
            near(corner, OLD_TERRAIN ? 64 : shade(Math.hypot(10, 2)), 4, "corner");
            near(tilted, corner, 3, "rotated up axis");
            near(blind, OLD_TERRAIN ? 64 : shade(Math.sqrt(204)), 4, "blindness");
            near(blindSlope, OLD_TERRAIN ? 64 : shade(Math.sqrt(38)), 4, "spherical pulse at a shallower offset");
            near(interpolated, OLD_TERRAIN ? 64 : shade(Math.hypot(8, 2)), 4, "per-vertex interpolation");
            near(fogCenter, 112, 2, "ambient fog");
            near(fogSide, fogCenter, 2, "ambient fog at equal depth");
            near(GL11.glGetError(), GL11.GL_NO_ERROR, 0, "OpenGL error");
            LogManager.getLogger().info("TERRAIN_PULSE_PASS baseline={}", OLD_TERRAIN);
            done = true;
        } finally {
            GL11.glDeleteTextures(texture);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }
    }

    private static void sampleMaterials(WorldGeometryShader shader, HallucinationManager hallucinations, int texture) throws IOException {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        shader.captureEntityView();
        for (Material material : new Material[] {Material.PARTICLES, Material.BLOCK_OUTLINE,
                Material.VANILLA_FAST_CLOUDS, Material.VANILLA_FANCY_CLOUDS,
                Material.FORGE_FAST_CLOUDS, Material.FORGE_FANCY_CLOUDS}) {
            drawPair(begin(shader, hallucinations, material), material.name(), 0, 8, -2,
                    64, OLD_WORLD ? 64 : 216);
        }
        for (Material material : new Material[] {Material.ENTITY, Material.ENTITY_GLINT}) {
            // The model's local rotation maps the test corner to (-10, 10).
            GL11.glRotatef(90, 0, 0, 1);
            try {
                drawPair(begin(shader, hallucinations, material), material.name(), 0, 10, -2,
                        64, OLD_WORLD ? 64 : 189, -10, 10);
            } finally {
                GL11.glLoadIdentity();
            }
        }
        drawPair(begin(shader, hallucinations, Material.RIFT_CENTER), "RIFT_CENTER", 0, 8, -2,
                64, OLD_WORLD ? 64 : 216);
        for (Material material : new Material[] {Material.BLOCK_DAMAGE, Material.WORLD_BORDER, Material.END_PORTAL}) {
            drawPair(begin(shader, hallucinations, material), material.name(), -2.5F, 8, -8,
                    OLD_WORLD ? 182 : 84, OLD_WORLD ? 182 : 84);
        }
        drawPair(shader.beginScreen(CLIENT, CLIENT.player, hallucinations), "SCREEN", -2.5F, 8, -8,
                OLD_WORLD ? 182 : 84, OLD_WORLD ? 182 : 84);
        for (boolean textured : new boolean[] {false, true}) {
            Scope sky = shader.beginSky(CLIENT, hallucinations, textured);
            if (OLD_WORLD) {
                if (sky != null) throw new AssertionError("Previous sky should omit a pulse-only draw");
                LogManager.getLogger().info("MATERIAL_PULSE {} skipped", textured ? "END_SKY" : "SKY_GRADIENT");
            } else {
                drawPair(sky, textured ? "END_SKY" : "SKY_GRADIENT", -2.5F, 8, -8, 84, 84);
            }
        }
        ShaderManager zero = new ShaderManager(CLIENT.getResourceManager(), Psychedelicraft.id("zero_matter").toString());
        try {
            zero.addSamplerTexture("ScreenSampler", texture);
            int black = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, black);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                    BufferUtils.createByteBuffer(4));
            try {
                zero.addSamplerTexture("EndPortalSampler", black);
                zero.useShader();
                GL11.glDisable(GL11.GL_BLEND);
                drawPair(shader.beginZeroMatter(zero, CLIENT, CLIENT.player, hallucinations, DrugCapability.get(CLIENT.player)),
                        "ZERO_MATTER", -2.5F, 8, -8, OLD_WORLD ? 182 : 84, OLD_WORLD ? 182 : 84);
                drawPair(shader.beginZeroMatter(zero, CLIENT, CLIENT.player, hallucinations, DrugCapability.get(CLIENT.player)),
                        "ZERO_MATTER_DARK_PHASE", 0, 8, -8, OLD_WORLD ? 217 : 64, OLD_WORLD ? 217 : 64);
            } finally {
                zero.endShader();
                GL11.glDeleteTextures(black);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL13.glActiveTexture(GL13.GL_TEXTURE1);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
            }
        } finally {
            zero.deleteShader();
            shader.clearEntityView();
        }
    }

    private static Scope begin(WorldGeometryShader shader, HallucinationManager hallucinations, Material material) {
        return shader.begin(CLIENT, CLIENT.player, hallucinations, DrugCapability.get(CLIENT.player), material, 0, null);
    }

    private static void drawPair(Scope scope, String name, float time, int offset, int depth, int center, int side) {
        drawPair(scope, name, time, offset, depth, center, side, offset, 0);
    }

    private static void drawPair(Scope scope, String name, float time, int offset, int depth, int center, int side,
                                 int sampleX, int sampleY) {
        if (scope == null) throw new AssertionError(name + " shader unavailable");
        try (Scope ignored = scope) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            uploadPulse(time, true);
            quad(-0.5, -0.5, 0.5, 0.5, depth);
            int localY = sampleY;
            quad(offset - 0.5, localY - 0.5, offset + 0.5, localY + 0.5, depth);
            int middle = red(0, 0);
            int edge = red(sampleX, sampleY);
            LogManager.getLogger().info("MATERIAL_PULSE {} center={} edge={}", name, middle, edge);
            near(middle, center, 4, name + " center");
            near(edge, side, 4, name + " edge");
        }
    }

    private static Scope start(WorldGeometryShader shader, HallucinationManager hallucinations, boolean pulse) {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        Scope scope = shader.begin(CLIENT, CLIENT.player, hallucinations, DrugCapability.get(CLIENT.player),
                Material.TERRAIN, 0, null);
        if (scope == null) throw new AssertionError("Terrain shader unavailable");
        uploadPulse(0F, pulse);
        return scope;
    }

    private static void uploadPulse(float time, boolean enabled) {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "WorldTicks"), time);
        GL20.glUniform4f(GL20.glGetUniformLocation(program, "Pulses"), 1F, 0F, 0F, enabled ? 1F : 0F);
    }

    private static void quad(double left, double bottom, double right, double top) {
        quad(left, bottom, right, top, -2);
    }

    private static void quad(double left, double bottom, double right, double top, int depth) {
        GL11.glColor4f(0.25F, 0.25F, 0.25F, 1F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(left, bottom, depth);
        GL11.glVertex3d(right, bottom, depth);
        GL11.glVertex3d(right, top, depth);
        GL11.glVertex3d(left, top, depth);
        GL11.glEnd();
    }

    private static int red(double x, double y) {
        PIXEL.clear();
        GL11.glReadPixels((int) ((x + 12) * width / 24), (int) ((y + 12) * height / 24), 1, 1,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXEL);
        return PIXEL.get(0) & 255;
    }

    private static int shade(double distance) {
        return (int) Math.round(255 * (0.25 + Math.max(0, Math.sin(distance / 5) - 0.4)));
    }

    private static void near(int actual, int expected, int tolerance, String name) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(name + " actual=" + actual + " expected=" + expected);
        }
    }
}
