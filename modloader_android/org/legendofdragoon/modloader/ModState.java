package org.legendofdragoon.modloader;

public enum ModState {
  INITIALIZED,
  READY,
  ;

  public boolean isReady() {
    return this == READY;
  }
}
