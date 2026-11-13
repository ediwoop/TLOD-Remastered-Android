package org.legendofdragoon.modloader;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Mod {
  String id();
  String version();
}
