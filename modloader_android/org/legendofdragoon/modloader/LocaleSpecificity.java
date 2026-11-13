package org.legendofdragoon.modloader;

/**
 * The priority of a lang file from highest to lowest
 */
public enum LocaleSpecificity {
  /** An override provided by the user */
  OVERRIDE,
  /** Example: en_CA */
  LANGUAGE_COUNTRY,
  /** Example: en */
  LANGUAGE,
  /** Currently always falls back to en */
  FALLBACK,
}
