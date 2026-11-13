package legend.core;

/** This is dumb but javafx applications won't start if the main method is in the same class as the application */
public final class UpdaterMain {
  private UpdaterMain() { }

  static {
    System.setProperty("log4j.skipJansi", "false");
    System.setProperty("log4j2.configurationFile", "log4j2-updater.xml");
  }

  static void main(final String[] args) {
    UpdaterApplication.main(args);
  }
}
