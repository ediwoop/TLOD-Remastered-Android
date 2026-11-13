package legend.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class Async {
  private Async() { }

  private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
  private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(AVAILABLE_PROCESSORS);

  public static CompletableFuture<Void> run(final Runnable task) {
    return CompletableFuture.runAsync(task, EXECUTOR);
  }

  public static <T> CompletableFuture<T> run(final Supplier<T> task) {
    return CompletableFuture.supplyAsync(task, EXECUTOR);
  }

  /** Non-blocking completed-result access, including on Android without Future.resultNow(). */
  public static <T> T resultNow(final Future<T> future) {
    if(!future.isDone()) {
      throw new IllegalStateException("Task has not completed");
    }

    boolean interrupted = false;
    try {
      for(;;) {
        try {
          return future.get();
        } catch(final InterruptedException e) {
          interrupted = true;
        } catch(final ExecutionException e) {
          throw new IllegalStateException("Task completed with an exception", e.getCause());
        } catch(final CancellationException e) {
          throw new IllegalStateException("Task was cancelled", e);
        }
      }
    } finally {
      if(interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public static void shutdown() {
    EXECUTOR.shutdown();
  }

  public static void shutdownNow() {
    EXECUTOR.shutdownNow();
  }

  public static boolean awaitShutdown() throws InterruptedException {
    return EXECUTOR.awaitTermination(5, TimeUnit.SECONDS);
  }
}
