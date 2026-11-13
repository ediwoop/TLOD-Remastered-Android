package legend.core;

import java.nio.file.Path;
import java.util.Objects;

/** Host-selected root for game-owned files, independent of the process working directory. */
public final class GamePaths {
  private static Path root;
  private static boolean configured;
  private GamePaths() { }

  /** Call before loading GameEngine or any resource-owning subsystem. */
  public static synchronized void configure(final Path directory) {
    Objects.requireNonNull(directory, "directory");
    if(root != null || configured) {
      throw new IllegalStateException("Game storage must be configured before resource initialization");
    }
    root = directory.toAbsolutePath().normalize();
    configured = true;
  }

  public static synchronized Path root() {
    if(root == null) {
      root = Path.of(".").toAbsolutePath().normalize();
    }
    return root;
  }

  /** For game-owned relative resources only; external file selections keep their original paths. */
  public static Path resolve(final String first, final String... more) {
    final Path relative = Path.of(first, more);
    if(relative.isAbsolute()) {
      throw new IllegalArgumentException("Expected a relative game resource");
    }
    final Path base = root();
    final Path result = base.resolve(relative).normalize();
    if(!result.startsWith(base)) {
      throw new IllegalArgumentException("Game resource is outside the configured root");
    }
    return result;
  }
}
