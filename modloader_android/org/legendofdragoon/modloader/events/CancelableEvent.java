package org.legendofdragoon.modloader.events;

public class CancelableEvent extends Event {
  private boolean canceled;

  public void cancel() {
    this.stopPropagation();
    this.canceled = true;
  }

  public boolean isCanceled() {
    return this.canceled;
  }
}
