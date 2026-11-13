package org.legendofdragoon.modloader.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {
  /**
   * Higher priority handlers will be executed first. Mods should only use a high/low priority if
   * they need to, since other mods may want/need to override them. If two mods register a handler
   * with the same priority, the execution order is undefined.
   */
  Priority priority() default Priority.NORMAL;
}
