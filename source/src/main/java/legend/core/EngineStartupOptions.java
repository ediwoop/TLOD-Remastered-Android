package legend.core;

/** Optional desktop integrations; does not replace required audio/render/data backends. */
public record EngineStartupOptions(boolean checkForUpdates, boolean discordPresence, legend.game.unpacker.Unpacker.Edition edition) {
  public EngineStartupOptions(boolean checkForUpdates, boolean discordPresence) { this(checkForUpdates, discordPresence, legend.game.unpacker.Unpacker.Edition.US_FULL); }
  public static EngineStartupOptions desktop() { return new EngineStartupOptions(true, true); }
  public static EngineStartupOptions android() { return new EngineStartupOptions(false, false, legend.game.unpacker.Unpacker.Edition.PAL_ES_DISC1); }
}
