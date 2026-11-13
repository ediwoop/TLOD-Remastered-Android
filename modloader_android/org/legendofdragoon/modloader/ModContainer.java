package org.legendofdragoon.modloader;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModContainer {
  private static final Pattern LANG_PATTERN = Pattern.compile("^([a-z]+)(?:_([A-Z]+))?\\.lang$");

  public final String modId;
  public final Object mod;
  public final ClassLoader classLoader;
  ModState state = ModState.INITIALIZED;

  private final List<Locale> supportedLocales;

  private static ModManager activeManager;
  private static Class<?> activeModClass;

  public ModContainer(final String modId, final Object mod) {
    this.modId = modId;
    this.mod = mod;
    this.classLoader = mod.getClass().getClassLoader();

    this.supportedLocales = Collections.unmodifiableList(this.findAvailableLocales());
  }

  public ClassLoader getClassLoader() {
    return this.classLoader;
  }

  public List<Locale> getSupportedLocales() {
    return this.supportedLocales;
  }

  private List<Locale> findAvailableLocales() {
    final List<Locale> locales = new ArrayList<>();
    final List<String> langFiles = IoHelper.findJarResources(this.mod.getClass(), this.modId + "/lang");

    for(final String langFile : langFiles) {
      final Matcher matcher = LANG_PATTERN.matcher(langFile);

      if(matcher.matches()) {
        final String language = matcher.group(1);
        final String country = matcher.group(2);
        final Locale locale = new Locale(language, country != null ? country : "");
        locales.add(locale);
      }
    }

    return locales;
  }

  public URL getResource(final String path) {
    if(BuiltinCatalog.current() != null) {
      return BuiltinCatalog.current().resource(this.modId + '/' + path);
    }
    return this.classLoader.getResource(this.modId + '/' + path);
  }

  /**
   * Searches for a lang file in the following order:
   * <ol>
   *   <li>lang_COUNTRY (e.g. en_CA)</li>
   *   <li>lang (e.g. en)</li>
   *   <li>en (fallback)</li>
   *   <li>null (if en isn't found)</li>
   * </ol>
   */
  public Map<LocaleSpecificity, URL> getLangResource(final Locale locale) {
    final Map<LocaleSpecificity, URL> langs = new EnumMap<>(LocaleSpecificity.class);

    final URL langCountry = this.getResource("lang/%s_%s.lang".formatted(locale.getLanguage(), locale.getCountry()));

    if(langCountry != null) {
      langs.put(LocaleSpecificity.LANGUAGE_COUNTRY, langCountry);
    }

    final URL lang = this.getResource("lang/%s.lang".formatted(locale.getLanguage()));

    if(lang != null) {
      langs.put(LocaleSpecificity.LANGUAGE, lang);
    }

    final URL fallback = this.getResource("lang/en.lang");

    if(fallback != null) {
      langs.put(LocaleSpecificity.FALLBACK, fallback);
    }

    return langs;
  }

  public static void setActiveMod(final ModManager manager, @Nullable final Class<?> handlerCls) {
    activeManager = manager;
    activeModClass = handlerCls;
  }

  public static boolean isModActive(final String modId) {
    return activeManager.getModsByClass(activeModClass) != null && activeManager.getModsByClass(activeModClass).contains(modId);
  }
}
