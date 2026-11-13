package ivorius.psychedelicraft.client;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Uninterruptibles;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class IntegratedShutdown {
    private IntegratedShutdown() {
    }

    public static Object await(Future<?> cleanup, MinecraftServer server) {
        Thread worker = ReflectionHelper.getPrivateValue(MinecraftServer.class, server, "serverThread", "field_175590_aa");
        return await(cleanup, worker);
    }

    static Object await(Future<?> cleanup, Thread worker) {
        // The owner's disconnect can stop the server after this task is queued.
        while (!cleanup.isDone() && worker != null && worker.isAlive()) {
            try {
                return Uninterruptibles.getUninterruptibly(cleanup, 50, TimeUnit.MILLISECONDS);
            } catch (TimeoutException retry) {
                // Recheck the server thread before waiting again.
            } catch (ExecutionException failed) {
                break; // Preserve Forge's exception handling below.
            }
        }
        if (!cleanup.isDone() && cleanup.cancel(false)) return null;
        return Futures.getUnchecked(cleanup);
    }
}
