package org.legendofdragoon.modloader.registries;

public class IncorrectModIdException extends RuntimeException {
  public IncorrectModIdException() {
    super();
  }

  public IncorrectModIdException(final String message) {
    super(message);
  }

  public IncorrectModIdException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public IncorrectModIdException(final Throwable cause) {
    super(cause);
  }

  protected IncorrectModIdException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
