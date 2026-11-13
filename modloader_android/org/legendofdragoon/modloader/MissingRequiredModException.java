package org.legendofdragoon.modloader;

public class MissingRequiredModException extends RuntimeException {
  public MissingRequiredModException(final String message) {
    super(message);
  }

  public MissingRequiredModException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public MissingRequiredModException(final Throwable cause) {
    super(cause);
  }

  protected MissingRequiredModException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
