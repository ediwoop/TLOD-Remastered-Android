package org.legendofdragoon.modloader.events;

public class EventHandlerException extends RuntimeException {
  public EventHandlerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
