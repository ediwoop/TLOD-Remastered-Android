package org.legendofdragoon.modloader;

import com.vdurmont.semver4j.Semver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.annotation.IncompleteAnnotationException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ModManager {
  private static final Logger LOGGER = LogManager.getFormatterLogger(ModManager.class);

  /** These mods are force-loaded whether requested or not */
  private final Set<String> requiredModIds;

  private final Map<String, URL> allModUrls = new HashMap<>();
  private final Map<String, Class<?>> allModClasses = new HashMap<>();
  private final Set<String> allModIds = Collections.unmodifiableSet(this.allModClasses.keySet());
  /// modClass.getProtectionDomain().getCodeSource().getLocation().toString()
  private final Map<String, Set<String>> pathToModId = new HashMap<>();
  private final Map<String, String> wrongVersion = new HashMap<>();
  private final Map<String, String> failedToLoad = new HashMap<>();

  private final List<URL> loadedModUrls = new ArrayList<>();
  private final Map<String, ModContainer> loadedModInstances = new HashMap<>();
  private final Collection<ModContainer> unmodifiableLoadedModInstances = Collections.unmodifiableCollection(this.loadedModInstances.values());

  public ModManager(final Consumer<Access> access, final String... requiredModIds) {
    access.accept(new Access());
    this.requiredModIds = Set.of(requiredModIds);
  }

  public Set<String> getRequiredModIds() {
    return this.requiredModIds;
  }

  public Set<String> getAllModIds() {
    return this.allModIds;
  }

  public Map<String, String> getWrongVersions() {
    return Collections.unmodifiableMap(this.wrongVersion);
  }

  public Map<String, String> getFailedToLoad() {
    return Collections.unmodifiableMap(this.failedToLoad);
  }

  public boolean isLoaded(final String modId) {
    return this.loadedModInstances.containsKey(modId);
  }

  public boolean isReady(final String modId) {
    return this.loadedModInstances.containsKey(modId) && this.loadedModInstances.get(modId).state.isReady();
  }

  public Collection<ModContainer> getLoadedMods() {
    return this.unmodifiableLoadedModInstances;
  }

  public Set<String> getModsByPath(final String path) {
    return this.pathToModId.get(path);
  }

  public Set<String> getModsByClass(final Class<?> cls) {
    if(BuiltinCatalog.current() != null) {
      // Built-in game modules share one app bundle, just as they share one desktop JAR.
      return cls == null ? null : Collections.unmodifiableSet(this.allModClasses.keySet());
    }
    return this.pathToModId.get(cls.getProtectionDomain().getCodeSource().getLocation().toString());
  }

  public void setActiveMod(final Class<?> cls) {
    ModContainer.setActiveMod(this, cls);
  }

  public class Access {
    private Access() { }

    private void findBuiltins(final String gameVersion) {
      final Map<String, Class<?>> found = new HashMap<>();
      final Map<String, String> wrong = new HashMap<>();
      final Semver version = new Semver(gameVersion, Semver.SemverType.NPM);
      for(final Class<?> type : BuiltinCatalog.current().mods()) {
        final Mod annotation = type.getDeclaredAnnotation(Mod.class);
        if(annotation == null) { throw new IllegalArgumentException("Catalog entry has no @Mod: " + type.getName()); }
        if(!version.satisfies(annotation.version())) { wrong.put(annotation.id(), annotation.version()); continue; }
        if(found.putIfAbsent(annotation.id(), type) != null) { throw new IllegalArgumentException("Duplicate builtin mod ID: " + annotation.id()); }
      }
      final Set<String> missing = new HashSet<>(ModManager.this.requiredModIds);
      missing.removeAll(found.keySet());
      if(!missing.isEmpty()) { throw new MissingRequiredModException("Missing built-in mods: " + missing); }
      ModManager.this.allModClasses.clear();
      ModManager.this.allModClasses.putAll(found);
      ModManager.this.allModUrls.clear();
      ModManager.this.pathToModId.clear();
      ModManager.this.failedToLoad.clear();
      ModManager.this.wrongVersion.clear();
      ModManager.this.wrongVersion.putAll(wrong);
    }

    public void findMods(final Path modsDir, final String gameVersion) throws IOException {
      if(BuiltinCatalog.current() != null) {
        this.findBuiltins(gameVersion);
        return;
      }
      Files.createDirectories(modsDir);

      LOGGER.info("Scanning for mods...");
      final List<URL> urlList = new ArrayList<>();

      try(final DirectoryStream<Path> jars = Files.newDirectoryStream(modsDir, "*.jar")) {
        for(final Path jar : jars) {
          urlList.add(jar.toUri().toURL());
        }
      }

      final ClassLoader modClassLoader = new URLClassLoader(urlList.toArray(URL[]::new), this.getClass().getClassLoader());

      ModManager.this.allModUrls.clear();
      ModManager.this.wrongVersion.clear();
      ModManager.this.failedToLoad.clear();
      ModManager.this.pathToModId.clear();

      final Set<Class<?>> modClasses = DesktopDiscovery.mods(this.getClass().getClassLoader(), modClassLoader, urlList);

      final Semver semver = new Semver(gameVersion, Semver.SemverType.NPM);

      for(final Class<?> modClass : modClasses) {
        try {
          final Mod modAnnotation = modClass.getDeclaredAnnotation(Mod.class);

          if(!semver.satisfies(modAnnotation.version())) {
            LOGGER.warn("Mod %s is for another version! (%S)", modAnnotation.id(), modAnnotation.version());
            ModManager.this.wrongVersion.put(modAnnotation.id(), modAnnotation.version());
            continue;
          }

          if(ModManager.this.allModClasses.containsKey(modAnnotation.id())) {
            LOGGER.error("Duplicate mod ID %s! Skipping.", modAnnotation.id());
            ModManager.this.failedToLoad.put(modAnnotation.id(), "Duplicate mod ID");
            continue;
          }

          LOGGER.info("Found mod: %s", modAnnotation.id());
          final URL location = modClass.getProtectionDomain().getCodeSource().getLocation();
          ModManager.this.allModClasses.put(modAnnotation.id(), modClass);
          ModManager.this.allModUrls.put(modAnnotation.id(), location);
          ModManager.this.pathToModId.computeIfAbsent(location.toString(), k -> new HashSet<>()).add(modAnnotation.id());
        } catch(final IncompleteAnnotationException e) {
          LOGGER.warn("Mod %s is for an old version!", modClass.getSimpleName());
          LOGGER.warn("", e);
          ModManager.this.wrongVersion.put(modClass.getSimpleName(), "Unknown");
        } catch(final Throwable e) {
          LOGGER.error("Mod ID %s failed to load!", modClass.getSimpleName());
          LOGGER.error("", e);
          ModManager.this.failedToLoad.put(modClass.getSimpleName(), "Failed to load");
        }
      }

      // Ensure all required mods were found
      if(!ModManager.this.allModIds.containsAll(ModManager.this.requiredModIds)) {
        final Set<String> missingMods = new HashSet<>(ModManager.this.requiredModIds);
        missingMods.removeAll(ModManager.this.allModIds);
        throw new MissingRequiredModException("Missing required mods (" + String.join(", ", missingMods) + ')');
      }
    }

    /**
     * Load all found mods
     */
    public void loadMods() {
      this.loadMods(ModManager.this.allModIds);
    }

    /**
     * Load all mods in the list and return a list of missing mods
     */
    public Set<String> loadMods(final Set<String> modIds) {
      ModManager.this.loadedModUrls.clear();

      final Set<String> modsToLoad = new HashSet<>(modIds);
      modsToLoad.addAll(ModManager.this.requiredModIds);

      final Set<String> missingModIds = new HashSet<>();
      for(final String modId : modsToLoad) {
        if(ModManager.this.allModClasses.containsKey(modId)) {
          this.instantiateMod(modId);
        } else {
          missingModIds.add(modId);
        }
      }

      return missingModIds;
    }

    private void instantiateMod(final String modId) {
      try {
        final ModContainer modContainer = new ModContainer(modId, ModManager.this.allModClasses.get(modId).getDeclaredConstructor().newInstance());
        ModManager.this.loadedModInstances.put(modId, modContainer);
        final URL location = ModManager.this.allModUrls.get(modId);
        if(location != null) { ModManager.this.loadedModUrls.add(location); }
        LOGGER.info("Loaded mod: %s", modId);
      } catch(final Throwable ex) {
        LOGGER.warn("FAILED TO LOAD MOD: %s", modId);
        LOGGER.warn("Exception:", ex);
        ModManager.this.failedToLoad.put(modId, "Failed to create mod instance");
        ModManager.this.allModClasses.remove(modId);
        ModManager.this.allModUrls.remove(modId);
        ModManager.this.pathToModId.remove(modId);
      }
    }

    public void loadingComplete() {
      for(final ModContainer container : ModManager.this.loadedModInstances.values()) {
        container.state = ModState.READY;
      }
    }

    public void reset() {
      ModManager.this.loadedModUrls.clear();
      ModManager.this.loadedModInstances.clear();
    }
  }

  public List<URL> discoveryUrls() {
    return List.copyOf(this.loadedModUrls);
  }
  public List<ClassLoader> discoveryLoaders() {
    return this.loadedModInstances.values().stream().map(ModContainer::getClassLoader).toList();
  }
}
