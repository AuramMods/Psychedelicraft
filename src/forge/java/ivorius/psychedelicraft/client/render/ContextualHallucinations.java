package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.INetHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class ContextualHallucinations {
    private static final int FORCED_TICKS = 400;
    private static ContextualHallucinations current;

    private final EntityPlayerSP player;
    private final WorldClient world;
    private final List<RastaHeadHallucination> heads = new ArrayList<>();
    private final List<IdentitySwapHallucination> swaps = new ArrayList<>();
    private float naturalStrength;
    private int forcedTicks;
    private int previousForcedTicks;

    private ContextualHallucinations(EntityPlayerSP player, WorldClient world) {
        this.player = player;
        this.world = world;
    }

    static void track(@Nullable EntityPlayerSP player, @Nullable WorldClient world) {
        if (player == null || world == null) {
            current = null;
        } else if (current == null || current.player != player || current.world != world) {
            current = new ContextualHallucinations(player, world);
        }
    }

    public static void receive(INetHandler connection, int entityId, int dimension, HallucinationType type,
                               @Nullable BlockPos position) {
        Minecraft client = Minecraft.getMinecraft();
        EntityPlayerSP player = client.player;
        if (client.getConnection() != connection || client.world == null || player == null
                || player.getEntityId() != entityId || player.dimension != dimension) return;
        track(player, client.world);
        current.add(type, position == null ? player.getPositionVector() : new Vec3d(position).addVector(0.5, 0.5, 0.5));
        current.forcedTicks = FORCED_TICKS;
        current.previousForcedTicks = FORCED_TICKS;
    }

    static void tick(DrugProperties drugs, float strength) {
        ContextualHallucinations state = current;
        if (state == null) return;
        state.naturalStrength = strength;
        state.previousForcedTicks = state.forcedTicks;
        if (state.forcedTicks > 0) state.forcedTicks--;
        state.tryNaturalSpawn(drugs);
        boolean visible = state.forcedAlpha(1F) > 0 || strength > 0;
        state.heads.removeIf(head -> head.tick(visible));
        state.swaps.removeIf(IdentitySwapHallucination::tick);
    }

    private void tryNaturalSpawn(DrugProperties drugs) {
        float chance = naturalStrength * 0.05F;
        if (chance <= 0) return;
        Random random = player.getRNG();
        if (random.nextInt(Math.max(1, (int) (1F / chance))) != 0) return;
        if (heads.isEmpty() && drugs.getActiveValue(DrugType.CANNABIS) > 0.4 && random.nextFloat() <= 0.1F) {
            add(HallucinationType.RASTA_HEAD, player.getPositionVector());
        } else if (naturalStrength >= 0.8F) {
            HallucinationType type = random.nextBoolean() ? HallucinationType.HOSTILE_VILLAGERS : HallucinationType.FRIENDLY_ZOMBIES;
            add(type, player.getPositionVector());
        }
    }

    private void add(HallucinationType type, Vec3d position) {
        if (type == HallucinationType.RASTA_HEAD) heads.add(new RastaHeadHallucination(player, position));
        else swaps.add(new IdentitySwapHallucination(type));
    }

    static void render(Minecraft client, float partialTicks) {
        ContextualHallucinations state = forClient(client);
        if (state == null || state.heads.isEmpty()) return;
        float alpha = MathHelper.clamp(state.naturalStrength * 15F, 0F, 1F);
        float forced = state.forcedAlpha(partialTicks);
        if (forced > 0F) alpha = (alpha + forced) / 2F;
        for (RastaHeadHallucination head : state.heads) head.render(client, partialTicks, alpha);
    }

    private float forcedAlpha(float partialTicks) {
        float remaining = (previousForcedTicks + (forcedTicks - previousForcedTicks) * partialTicks) / FORCED_TICKS;
        if (remaining <= 0.01F) return 0;
        float alpha = Math.abs(Math.min(MathHelper.sin(remaining * (float) Math.PI) * 2F, 1F));
        if (remaining < 0.1F) {
            alpha = (alpha + 0.5F + MathHelper.sin((player.ticksExisted + partialTicks) / 3F) * 0.5F) / 2F;
        }
        return alpha;
    }

    public static Entity forRender(Entity original) {
        ContextualHallucinations state = forClient(Minecraft.getMinecraft());
        if (state == null || original.world != state.world) return original;
        for (IdentitySwapHallucination swap : state.swaps) {
            Entity disguise = swap.disguise(original);
            if (disguise != null) return disguise;
        }
        return original;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChatReceived(ClientChatReceivedEvent event) {
        if (event.getType() != ChatType.CHAT) return;
        ContextualHallucinations state = forClient(Minecraft.getMinecraft());
        if (state == null || state.heads.isEmpty()) return;
        ITextComponent message = event.getMessage();
        String sender = "";
        if (message instanceof TextComponentTranslation) {
            TextComponentTranslation translation = (TextComponentTranslation) message;
            Object[] args = translation.getFormatArgs();
            if ("chat.type.text".equals(translation.getKey()) && args.length > 0) {
                sender = args[0] instanceof ITextComponent ? ((ITextComponent) args[0]).getUnformattedText() : String.valueOf(args[0]);
            }
        }
        for (RastaHeadHallucination head : state.heads) head.chat.receive(sender, message);
    }

    @Nullable
    private static ContextualHallucinations forClient(Minecraft client) {
        return current != null && current.player == client.player && current.world == client.world ? current : null;
    }
}
