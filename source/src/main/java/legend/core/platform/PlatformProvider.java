package legend.core.platform;

import java.util.Objects;
import java.util.function.Supplier;

/** Installs a host backend before GameEngine is initialized. */
public final class PlatformProvider {
  private static Supplier<? extends PlatformManager> factory;
  private static PlatformManager platform;
  private static boolean creating;

  private PlatformProvider() { }

  public static synchronized void install(final Supplier<? extends PlatformManager> backend) {
    Objects.requireNonNull(backend, "backend");
    if(platform != null || creating) {
      throw new IllegalStateException("Install the platform before initializing GameEngine");
    }
    if(factory != null) {
      throw new IllegalStateException("A platform backend is already installed");
    }
    factory = backend;
  }

  public static synchronized PlatformManager get() {
    if(platform != null) {
      return platform;
    }
    if(creating) {
      throw new IllegalStateException("Platform factory must not initialize GameEngine recursively");
    }
    creating = true;
    try {
      platform = Objects.requireNonNull(factory == null ? new SdlPlatformManager() : factory.get(), "Platform factory returned null");
      return platform;
    } finally {
      creating = false;
    }
  }
}
