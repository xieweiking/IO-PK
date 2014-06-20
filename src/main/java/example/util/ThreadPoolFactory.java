package example.util;

import java.util.List;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolFactory implements ThreadFactory, RejectedExecutionHandler {

    public static final int CPUs = Runtime.getRuntime().availableProcessors();

    static final long THREAD_IDLE_TIME = 10;
    static final TimeUnit THREAD_IDLE_TIME_UNIT = TimeUnit.SECONDS;

    private final String threadNamePrefix;
    private final boolean daemon;
    private final AtomicInteger count = new AtomicInteger(0);

    private ThreadPoolFactory(final String prefix, final boolean daemon) {
        this.threadNamePrefix = prefix;
        this.daemon = daemon;
    }

    public static ThreadPoolExecutor create(final String threadNamePrefix,
            final boolean daemon) {
        return create(2 * CPUs, threadNamePrefix, daemon);
    }

    public static ThreadPoolExecutor create(final int coreSize, final String threadNamePrefix,
            final boolean daemon) {
        return create(coreSize, coreSize * 300, threadNamePrefix, daemon);
    }

    public static ThreadPoolExecutor create(final int coreSize, final int maxThreads, final String threadNamePrefix,
            final boolean daemon) {
        final ThreadPoolFactory factory = new ThreadPoolFactory(threadNamePrefix, daemon);
        return new ThreadPoolExecutor(coreSize, maxThreads,
                THREAD_IDLE_TIME, THREAD_IDLE_TIME_UNIT,
                new SynchronousQueue<Runnable>(), factory, factory);
    }

    public static int shutdown(final ThreadPoolExecutor pool) {
        try {
            if (pool.getActiveCount() > 0) {
                pool.awaitTermination(THREAD_IDLE_TIME / 10, THREAD_IDLE_TIME_UNIT);
            }
        }
        catch (final InterruptedException ignored) {}
        finally {
            final List<Runnable> notCompletedTasks = pool.shutdownNow();
            if (notCompletedTasks != null) {
                return notCompletedTasks.size();
            }
        }
        return 0;
    }

    public Thread newThread(final Runnable r) {
        final Thread t = new Thread(r, this.threadNamePrefix + "-" + this.count.incrementAndGet());
        t.setDaemon(this.daemon);
        return t;
    }

    public static void run(final Runnable r, final String name) {
        run(r, name, false);
    }

    public static void runDaemon(final Runnable r, final String name) {
        run(r, name, true);
    }

    public static void run(final Runnable r, final String name, final boolean daemon) {
        final Thread t = new Thread(r, name);
        t.setDaemon(daemon);
        t.start();
    }

    public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
        synchronized (executor) {
            try { executor.wait(100); }
            catch (final InterruptedException ignored) {}
            if (!executor.isShutdown()) {
                executor.execute(r);
            }
        }
    }

}
