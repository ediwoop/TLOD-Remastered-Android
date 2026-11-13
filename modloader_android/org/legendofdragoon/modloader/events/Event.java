package org.legendofdragoon.modloader.events;

public class Event {
  private boolean propagate = true;

  /**
   * Stops the event from being sent to any other mods after the one
   * that calls this. Play nice - only use this if your mod needs it.
   */
  public void stopPropagation() {
    this.propagate = false;
  }

  boolean shouldPropagate() {
    return this.propagate;
  }

  /** If true, exceptions thrown in event handlers will be rethrown instead of caught and logged */
  protected boolean isCritical() {
    return false;
  }
}
