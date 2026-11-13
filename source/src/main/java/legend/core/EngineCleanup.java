package legend.core;

/** Finish all teardown steps without replacing the original engine failure. */
public final class EngineCleanup {
  private EngineCleanup() { }

  public static void run(final Throwable primary, final Runnable... steps) {
    Throwable failure = primary;
    for(final Runnable step : steps) {
      try {
        step.run();
      } catch(final RuntimeException | Error cleanupFailure) {
        if(failure == null) failure = cleanupFailure;
        else if(failure != cleanupFailure) failure.addSuppressed(cleanupFailure);
      }
    }
    if(primary == null) {
      if(failure instanceof RuntimeException error) throw error;
      if(failure instanceof Error error) throw error;
    }
  }
}
