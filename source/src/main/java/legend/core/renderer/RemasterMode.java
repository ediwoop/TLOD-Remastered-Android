package legend.core.renderer;

/** Visual selection shared by platform controls and asset renderers. */
public final class RemasterMode {
  private static volatile boolean enabled = true;

  private RemasterMode() { }

  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(final boolean value) {
    enabled = value;
  }
}
