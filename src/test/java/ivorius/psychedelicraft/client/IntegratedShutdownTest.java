package ivorius.psychedelicraft.client;

import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IntegratedShutdownTest {
    @Test(timeout = 2000)
    public void cancelsCleanupWhenTheServerExitsWithoutRunningIt() throws Exception {
        CountDownLatch waiting = new CountDownLatch(1);
        FutureTask<Object> cleanup = new FutureTask<Object>(() -> {
            throw new AssertionError("cleanup should stay on the server thread");
        }) {
            @Override
            public Object get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                waiting.countDown();
                return super.get(timeout, unit);
            }
        };
        Thread server = new Thread(() -> {
            try {
                waiting.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        server.setDaemon(true);
        server.start();
        try {
            assertNull(IntegratedShutdown.await(cleanup, server));
            assertTrue(cleanup.isCancelled());
        } finally {
            server.interrupt();
            server.join();
        }
    }

    @Test(timeout = 2000)
    public void acceptsCompletedCleanupEvenAfterTheServerExits() {
        SettableFuture<String> cleanup = SettableFuture.create();
        cleanup.set("saved");
        assertEquals("saved", IntegratedShutdown.await(cleanup, new Thread()));
    }

    @Test(timeout = 2000)
    public void preservesCleanupFailure() {
        SettableFuture<Object> cleanup = SettableFuture.create();
        IllegalStateException failure = new IllegalStateException("cleanup failed");
        cleanup.setException(failure);
        try {
            IntegratedShutdown.await(cleanup, new Thread());
            fail("cleanup failure should propagate");
        } catch (UncheckedExecutionException propagated) {
            assertSame(failure, propagated.getCause());
        }
    }
}
