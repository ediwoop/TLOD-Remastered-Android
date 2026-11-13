package org.legendofdragoon.modloader.registries;

import java.util.Objects;
import java.util.regex.Pattern;

public final class RegistryId {
  private static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9_-]*");

  private final String modId;
  private final String entryId;

  public RegistryId(final String modId, final String entryId) {
    if(modId.length() + entryId.length() + 1 > 255) {
      throw new IllegalArgumentException("Full registry ID must be less than 255 characters");
    }

    this.modId = checkId(modId);
    this.entryId = checkId(entryId);
  }

  public RegistryId(final String id) {
    if(id.length() > 255) {
      throw new IllegalArgumentException("Full registry ID must be less than 255 characters");
    }

    final String[] parts = id.split(":");

    if(parts.length != 2) {
      throw new IllegalArgumentException("Invalid registry ID " + id);
    }

    this.modId = checkId(parts[0]);
    this.entryId = checkId(parts[1]);
  }

  private static String checkId(final String id) {
    if(!ID_PATTERN.matcher(id).matches()) {
      throw new IllegalArgumentException("Registry IDs must be lowercase, at least 3 characters, start with a latter, and contain only a-z, 0-9, _, and -");
    }

    return id;
  }

  @Override
  public String toString() {
    return this.modId + ':' + this.entryId;
  }

  public String modId() {
    return this.modId;
  }

  public String entryId() {
    return this.entryId;
  }

  @Override
  public boolean equals(final Object obj) {
    if(obj == this) {
      return true;
    }

    if(obj == null || obj.getClass() != this.getClass()) {
      return false;
    }

    final var that = (RegistryId)obj;
    return Objects.equals(this.modId, that.modId) && Objects.equals(this.entryId, that.entryId);
  }

  @Override
  public int hashCode() {
    return this.modId.hashCode() * 31 + this.entryId.hashCode();
  }
}
