package org.legendofdragoon.modloader.i18n;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.legendofdragoon.modloader.LocaleSpecificity;
import org.legendofdragoon.modloader.ModContainer;
import org.legendofdragoon.modloader.ModManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LangManager {
  private static final Logger LOGGER = LogManager.getFormatterLogger(LangManager.class);

  private static final Pattern LANG_PATTERN = Pattern.compile("^(?:\\w+\\.)?([a-z]+)(?:_([A-Z]+))?\\.lang$");

  private final Map<LocaleSpecificity, Map<String, String>> translations = new EnumMap<>(LocaleSpecificity.class);

  public LangManager(final Consumer<Access> access) {
    for(final LocaleSpecificity specificity : LocaleSpecificity.values()) {
      this.translations.put(specificity, new HashMap<>());
    }

    access.accept(new Access());
  }

  private String findTranslation(final String key) {
    for(final LocaleSpecificity specificity : LocaleSpecificity.values()) {
      final String value = this.translations.get(specificity).get(key);

      if(value != null) {
        return value;
      }
    }

    return key;
  }

  public String getTranslation(final String key) {
    final String format = this.findTranslation(key);

    // Simple strings don't need to be run through `formatted`
    if(!format.contains("%")) {
      return format;
    }

    return format.formatted();
  }

  public String getTranslation(final String key, final Object... args) {
    return this.findTranslation(key).formatted(args);
  }

  public Set<Locale> findAvailableOverrideLocales(final Path directory) throws IOException {
    final Set<Locale> locales = new HashSet<>();

    try(final Stream<Path> stream = Files.walk(directory, FileVisitOption.FOLLOW_LINKS)) {
      stream.forEach(file -> {
        final Matcher matcher = LANG_PATTERN.matcher(file.getFileName().toString());

        if(matcher.matches()) {
          final String language = matcher.group(1);
          final String country = matcher.group(2);
          locales.add(new Locale(language, country != null ? country : ""));
        }
      });
    }

    return locales;
  }

  public class Access {
    private Access() { }

    public void initialize(final ModManager mods, final Locale locale) {
      final Map<LocaleSpecificity, Map<String, String>> allLang = new EnumMap<>(LocaleSpecificity.class);

      for(final LocaleSpecificity specificity : LocaleSpecificity.values()) {
        allLang.put(specificity, new HashMap<>());
      }

      for(final ModContainer mod : mods.getLoadedMods()) {
        try {
          final Map<LocaleSpecificity, URL> langUrls = mod.getLangResource(locale);

          for(final var entry : langUrls.entrySet()) {
            final LocaleSpecificity specificity = entry.getKey();
            final URL langUrl = entry.getValue();
            this.loadLang(langUrl, allLang.get(specificity));
          }
        } catch(final IOException e) {
          LOGGER.warn("Failed to load %s for mod %d", locale, mod.modId);
        }
      }

      for(final LocaleSpecificity specificity : LocaleSpecificity.values()) {
        this.loadLang(specificity, allLang.get(specificity));
      }
    }

    private void loadLang(final URL langUrl, final Map<String, String> lang) throws IOException {
      final Properties properties = new Properties();

      try(final InputStreamReader reader = new InputStreamReader(langUrl.openStream(), StandardCharsets.UTF_8)) {
        properties.load(reader);
      }

      for(final String key : properties.stringPropertyNames()) {
        lang.put(key, properties.getProperty(key));
      }
    }

    public void loadLangOverrides(final Path directory, final Locale locale) throws IOException {
      final Map<String, String> lang = new HashMap<>();
      final Map<LocaleSpecificity, List<Path>> langPaths = this.findLangPaths(directory, locale);

      for(final LocaleSpecificity specificity : LocaleSpecificity.values()) {
        final List<Path> paths = langPaths.get(specificity);

        if(paths != null) {
          for(final Path path : paths) {
            final Properties properties = new Properties();
            try(final InputStream stream = Files.newInputStream(path); final InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
              properties.load(reader);
            }

            for(final String key : properties.stringPropertyNames()) {
              lang.put(key, properties.getProperty(key));
            }
          }
        }
      }

      this.loadLang(LocaleSpecificity.OVERRIDE, lang);
    }

    public void addLangOverrides(final Map<String, String> lang) {
      this.loadLang(LocaleSpecificity.OVERRIDE, lang);
    }

    private void loadLang(final LocaleSpecificity specificity, final Map<String, String> lang) {
      LangManager.this.translations.get(specificity).putAll(lang);
    }

    private Map<LocaleSpecificity, List<Path>> findLangPaths(final Path directory, final Locale locale) throws IOException {
      final Map<LocaleSpecificity, List<Path>> langPaths = new EnumMap<>(LocaleSpecificity.class);

      try(final Stream<Path> stream = Files.walk(directory, FileVisitOption.FOLLOW_LINKS)) {
        stream.forEach(file -> {
          final Matcher matcher = LANG_PATTERN.matcher(file.getFileName().toString());

          if(matcher.matches()) {
            final String language = matcher.group(1);
            final String country = matcher.group(2);

            if(locale.getLanguage().equals(language) && locale.getCountry().equals(country)) {
              langPaths.computeIfAbsent(LocaleSpecificity.LANGUAGE_COUNTRY, key -> new ArrayList<>()).add(file);
            } else if(locale.getLanguage().equals(language) && locale.getCountry().isBlank() && country == null) {
              langPaths.computeIfAbsent(LocaleSpecificity.LANGUAGE, key -> new ArrayList<>()).add(file);
            } else if("en".equals(language) && country == null) {
              langPaths.computeIfAbsent(LocaleSpecificity.FALLBACK, key -> new ArrayList<>()).add(file);
            }
          }
        });
      }

      return langPaths;
    }

    public void reset() {
      for(final LocaleSpecificity specificity : LocaleSpecificity.values()) {
        LangManager.this.translations.put(specificity, new HashMap<>());
      }
    }
  }
}
