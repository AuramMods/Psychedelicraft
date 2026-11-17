package portqa;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import ivorius.psychedelicraft.entity.drug.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.SharedConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Separate QA mod: inventory observations never ship in the production mod. */
@Mod(RegistrationAudit.ID)
@Mod.EventBusSubscriber(modid = RegistrationAudit.ID)
public final class RegistrationAudit {
  static final String ID = "psregistryaudit", MOD = "psychedelicraft";
  static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

  static boolean owned(ResourceLocation id) {
    return id.getNamespace().equals(MOD);
  }

  static void write(Path path, JsonObject data) throws Exception {
    Files.createDirectories(path.getParent());
    Files.writeString(path, JSON.toJson(data));
  }

  static JsonObject registry(IForgeRegistry<?> registry) {
    var result = new JsonObject();
    registry.getKeys().stream()
        .filter(RegistrationAudit::owned)
        .sorted()
        .forEach(
            id -> {
              Object value = registry.getValue(id);
              var row = new JsonObject();
              row.addProperty("class", value.getClass().getName());
              if (value instanceof Item item) {
                row.addProperty("description", item.getDescriptionId(item.getDefaultInstance()));
                row.addProperty("maxStack", item.getDefaultInstance().getMaxStackSize());
                if (item instanceof BlockItem b)
                  row.addProperty("block", ForgeRegistries.BLOCKS.getKey(b.getBlock()).toString());
              }
              if (value instanceof Block block) {
                row.addProperty("states", block.getStateDefinition().getPossibleStates().size());
                row.addProperty("shape", block.defaultBlockState().getRenderShape().toString());
                row.addProperty("item", ForgeRegistries.ITEMS.getKey(block.asItem()).toString());
              }
              if (value instanceof net.minecraft.world.level.material.Fluid fluid) {
                row.addProperty("waterTag", fluid.is(FluidTags.WATER));
                row.addProperty(
                    "type",
                    ForgeRegistries.FLUID_TYPES.get().getKey(fluid.getFluidType()).toString());
                if (fluid instanceof FlowingFluid f)
                  row.addProperty(
                      "source", ForgeRegistries.FLUIDS.getKey(f.getSource()).toString());
              }
              result.add(id.toString(), row);
            });
    return result;
  }

  static JsonObject server(MinecraftServer server) throws Exception {
    var result = new JsonObject();
    result.addProperty("minecraft", SharedConstants.getCurrentVersion().getName());
    result.addProperty("forge", net.minecraftforge.versions.forge.ForgeVersion.getVersion());
    var registries = new JsonObject();
    for (var field : ForgeRegistries.class.getFields())
      if (IForgeRegistry.class.isAssignableFrom(field.getType())) {
        var rows = registry((IForgeRegistry<?>) field.get(null));
        if (rows.size() > 0) registries.add(field.getName(), rows);
      }
    registries.add("FLUID_TYPES", registry(ForgeRegistries.FLUID_TYPES.get()));
    result.add("registries", registries);
    var dynamic = new JsonObject();
    server
        .registryAccess()
        .registries()
        .forEach(
            entry -> {
              var ids = new JsonArray();
              entry.value().keySet().stream()
                  .filter(RegistrationAudit::owned)
                  .sorted()
                  .forEach(id -> ids.add(id.toString()));
              if (!ids.isEmpty()) dynamic.add(entry.key().location().toString(), ids);
            });
    result.add("datapackRegistries", dynamic);
    var recipes = new JsonObject();
    server.getRecipeManager().getRecipes().stream()
        .filter(r -> owned(r.getId()))
        .sorted(Comparator.comparing(r -> r.getId().toString()))
        .forEach(
            r -> {
              var row = new JsonObject();
              row.addProperty("type", BuiltInRegistries.RECIPE_TYPE.getKey(r.getType()).toString());
              row.addProperty(
                  "serializer",
                  ForgeRegistries.RECIPE_SERIALIZERS.getKey(r.getSerializer()).toString());
              row.addProperty("class", r.getClass().getName());
              recipes.add(r.getId().toString(), row);
            });
    result.add("recipes", recipes);
    var advancements = new JsonArray();
    server.getAdvancements().getAllAdvancements().stream()
        .filter(a -> owned(a.getId()))
        .sorted(Comparator.comparing(a -> a.getId().toString()))
        .forEach(a -> advancements.add(a.getId().toString()));
    result.add("advancements", advancements);
    var effects = new JsonObject();
    for (var type : DrugType.values()) {
      var row = new JsonObject();
      row.addProperty("class", type.create().getClass().getName());
      effects.add(type.id(), row);
    }
    result.add("effects", effects);
    var criteria = new JsonArray();
    for (var criterion : net.minecraft.advancements.CriteriaTriggers.all())
      if (owned(criterion.getId())) criteria.add(criterion.getId().toString());
    result.add("criteria", criteria);
    var commands = new JsonObject();
    for (String name : List.of("drug", "vomit", "hallucinate")) {
      var node = server.getCommands().getDispatcher().getRoot().getChild(name);
      var row = new JsonObject();
      row.addProperty("present", node != null);
      if (node != null)
        row.add(
            "usage",
            JSON.toJsonTree(
                server
                    .getCommands()
                    .getDispatcher()
                    .getAllUsage(node, server.createCommandSourceStack(), false)));
      commands.add(name, row);
    }
    result.add("commands", commands);
    return result;
  }

  static void settings(
      com.electronwill.nightconfig.core.UnmodifiableConfig values,
      String scope,
      net.minecraftforge.common.ForgeConfigSpec spec,
      JsonObject result) {
    for (var entry : values.entrySet()) {
      if (entry.getValue() instanceof com.electronwill.nightconfig.core.UnmodifiableConfig nested)
        settings(nested, scope, spec, result);
      else {
        var value = (net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>) entry.getValue();
        var row = new JsonObject();
        net.minecraftforge.common.ForgeConfigSpec.ValueSpec definition =
            spec.getSpec().get(value.getPath());
        row.add("default", JSON.toJsonTree(definition.getDefault()));
        result.add(scope + ":" + String.join(".", value.getPath()), row);
      }
    }
  }

  @SubscribeEvent
  public static void serverStarted(ServerStartedEvent event) throws Exception {
    write(
        event
            .getServer()
            .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
            .resolve("registration-audit/server.json"),
        server(event.getServer()));
  }

  @Mod.EventBusSubscriber(modid = ID, value = Dist.CLIENT)
  public static final class Client {
    static int ticks, age, page, frames, phase;
    static boolean starting, failed, done, captured;
    static String pending;
    static JsonObject report;
    static CompletableFuture<Void> reload;
    static List<ResourceLocation> items;

    static Minecraft c() {
      return Minecraft.getInstance();
    }

    static Path root() {
      return c().gameDirectory.toPath().resolve("registration-audit");
    }

    static void check(boolean b, String m) {
      if (!b) throw new AssertionError(m);
    }

    static JsonObject model(
        BakedModel model, net.minecraft.world.level.block.state.BlockState state) {
      var result = new JsonObject();
      result.addProperty("missing", model == c().getModelManager().getMissingModel());
      result.addProperty("customRenderer", model.isCustomRenderer());
      var sprites = new TreeSet<String>();
      int quads = 0;
      var directions = new ArrayList<Direction>(Arrays.asList(Direction.values()));
      directions.add(null);
      for (var direction : directions)
        for (var quad : model.getQuads(state, direction, RandomSource.create(0))) {
          sprites.add(quad.getSprite().contents().name().toString());
          quads++;
        }
      result.addProperty("quads", quads);
      result.add("sprites", JSON.toJsonTree(sprites));
      return result;
    }

    static void inventory() throws Exception {
      report =
          c().getSingleplayerServer()
              .submit(
                  () -> {
                    try {
                      return server(c().getSingleplayerServer());
                    } catch (Exception e) {
                      throw new CompletionException(e);
                    }
                  })
              .join();
      report.addProperty("language", c().options.languageCode);
      var settings = new JsonObject();
      settings(
          ivorius.psychedelicraft.config.PSConfig.COMMON.getValues(),
          "common",
          ivorius.psychedelicraft.config.PSConfig.COMMON,
          settings);
      settings(
          ivorius.psychedelicraft.config.PSClientConfig.SPEC.getValues(),
          "client",
          ivorius.psychedelicraft.config.PSClientConfig.SPEC,
          settings);
      report.add("settings", settings);
      var itemModels = new JsonObject();
      for (var id : items) {
        var stack = ForgeRegistries.ITEMS.getValue(id).getDefaultInstance();
        var row = model(c().getItemRenderer().getModel(stack, c().level, c().player, 0), null);
        row.addProperty("name", stack.getHoverName().getString());
        row.addProperty("description", stack.getDescriptionId());
        itemModels.add(id.toString(), row);
      }
      report.add("itemModels", itemModels);
      var blocks = new JsonObject();
      for (var id :
          ForgeRegistries.BLOCKS.getKeys().stream()
              .filter(RegistrationAudit::owned)
              .sorted()
              .toList()) {
        var block = ForgeRegistries.BLOCKS.getValue(id);
        var states = new JsonObject();
        for (var state : block.getStateDefinition().getPossibleStates())
          if (state.getRenderShape() == RenderShape.MODEL)
            states.add(state.toString(), model(c().getBlockRenderer().getBlockModel(state), state));
        blocks.add(id.toString(), states);
      }
      report.add("blockModels", blocks);
      var entities = new JsonObject();
      for (var id :
          ForgeRegistries.ENTITY_TYPES.getKeys().stream()
              .filter(RegistrationAudit::owned)
              .sorted()
              .toList()) {
        var entity = ForgeRegistries.ENTITY_TYPES.getValue(id).create(c().level);
        var renderer = c().getEntityRenderDispatcher().getRenderer(entity);
        entities.addProperty(
            id.toString(), renderer == null ? "MISSING" : renderer.getClass().getName());
      }
      report.add("entityRenderers", entities);
      write(root().resolve("manifest-" + c().options.languageCode + ".json"), report);
      LogUtils.getLogger()
          .info(
              "REGISTRY_AUDIT language={} items={} blocks={} recipes={}",
              c().options.languageCode,
              items.size(),
              blocks.size(),
              report.getAsJsonObject("recipes").size());
    }

    static void show() {
      c().setScreen(new ItemSheet(page));
      pending = c().options.languageCode + "-items-" + (page + 1);
      frames = 0;
      captured = false;
      age = 0;
    }

    static void fail(Throwable t) {
      failed = true;
      LogUtils.getLogger().error("REGISTRY_AUDIT failure", t);
      try {
        var r = new JsonObject();
        r.addProperty("failed", t.toString());
        write(root().resolve("result.json"), r);
      } catch (Exception ignored) {
      }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) return;
      try {
        if (done || failed) {
          c().stop();
          return;
        }
        check(++ticks < 20000, "audit timeout");
        age++;
        if (!starting && c().screen instanceof TitleScreen) {
          starting = true;
          var world =
              Files.readString(c().gameDirectory.toPath().resolve("audit-world.txt")).strip();
          check(
              c().options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER) == 0,
              "QA master volume muted");
          c().getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
          ivorius.psychedelicraft.config.PSConfig.RIFT_SPAWN_INTERVAL.set(0);
          ivorius.psychedelicraft.config.PSConfig.COMMON.save();
          c().createWorldOpenFlows().loadLevel(c().screen, world);
        }
        if (c().level == null || c().player == null || c().getOverlay() != null) return;
        if (phase == 0 && age > 100 && c().screen == null) {
          c().getSingleplayerServer()
              .submit(
                  () -> {
                    var p =
                        c().getSingleplayerServer().getPlayerList().getPlayer(c().player.getUUID());
                    DrugCapability.get(p).fromNbt(new CompoundTag());
                    DrugCapability.get(p).markDirty();
                    p.removeAllEffects();
                    p.setNoGravity(true);
                  })
              .join();
          c().options.guiScale().set(1);
          c().resizeDisplay();
          items =
              ForgeRegistries.ITEMS.getKeys().stream()
                  .filter(RegistrationAudit::owned)
                  .sorted()
                  .toList();
          inventory();
          show();
          phase = 1;
        } else if (phase == 1 && captured && age > 15) {
          if (++page * 60 < items.size()) show();
          else if (c().options.languageCode.equals("en_us")) {
            c().options.languageCode = "pl_pl";
            c().getLanguageManager().setSelected("pl_pl");
            c().options.save();
            c().setScreen(null);
            reload = c().reloadResourcePacks();
            phase = 2;
            age = 0;
          } else {
            var result = new JsonObject();
            result.addProperty("done", true);
            result.addProperty("itemsPerLanguage", items.size());
            result.addProperty("languages", 2);
            write(root().resolve("result.json"), result);
            done = true;
          }
        } else if (phase == 2 && age > 80 && reload.isDone()) {
          reload.join();
          inventory();
          page = 0;
          show();
          phase = 1;
        }
      } catch (Throwable t) {
        fail(t);
      }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void frame(TickEvent.RenderTickEvent event) {
      if (event.phase != TickEvent.Phase.END || pending == null || c().getOverlay() != null) return;
      try {
        if (++frames < 16) return;
        String name = pending;
        pending = null;
        Files.createDirectories(root());
        Screenshot.grab(
            root().toFile(),
            name + ".png",
            c().getMainRenderTarget(),
            m -> {
              if (m.getString().contains("Couldn't")) fail(new AssertionError(m.getString()));
            });
        captured = true;
      } catch (Throwable t) {
        fail(t);
      }
    }

    static final class ItemSheet extends Screen {
      final int page;

      ItemSheet(int page) {
        super(net.minecraft.network.chat.Component.literal("Registration audit"));
        this.page = page;
      }

      @Override
      public boolean isPauseScreen() {
        return false;
      }

      @Override
      public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xff20242c);
        g.drawString(
            font,
            "Psychedelicraft · "
                + c().options.languageCode
                + " · "
                + (page + 1)
                + "/"
                + ((items.size() + 59) / 60),
            12,
            8,
            0xffffff);
        for (int cell = 0; cell < 60 && page * 60 + cell < items.size(); cell++) {
          var id = items.get(page * 60 + cell);
          int x = (cell % 10) * width / 10 + width / 20, y = 30 + (cell / 10) * (height - 36) / 6;
          var stack = ForgeRegistries.ITEMS.getValue(id).getDefaultInstance();
          g.pose().pushPose();
          g.pose().translate(x - 16, y, 0);
          g.pose().scale(2, 2, 1);
          g.renderItem(stack, 0, 0);
          g.pose().popPose();
          g.pose().pushPose();
          g.pose().scale(.6F, .6F, 1);
          g.drawCenteredString(
              font, id.getPath(), (int) (x / .6F), (int) ((y + 35) / .6F), 0xffffff);
          g.drawCenteredString(
              font,
              font.plainSubstrByWidth(
                  stack.getHoverName().getString(), (int) ((width / 10 - 4) / .6F)),
              (int) (x / .6F),
              (int) ((y + 44) / .6F),
              0xaaccff);
          g.pose().popPose();
        }
      }
    }
  }
}
