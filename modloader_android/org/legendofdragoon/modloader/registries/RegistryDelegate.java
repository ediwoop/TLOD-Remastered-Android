package org.legendofdragoon.modloader.registries;

import org.legendofdragoon.modloader.Latch;
import org.legendofdragoon.modloader.ModNotLoadedException;

import java.util.function.Supplier;

public class RegistryDelegate<Type extends RegistryEntry> implements Supplier<Type> {
  private final RegistryId id;
  private final Registry<Type> registry;
  private final Class<Registry<Type>> cls;
  private final Latch<Type> latch;

  RegistryDelegate(final RegistryId id, final Registry<Type> registry, final Class<Registry<Type>> cls, final Supplier<Type> supplier) {
    this.id = id;
    this.registry = registry;
    this.cls = cls;
    this.latch = new Latch<>(supplier);
  }

  public boolean isValid() {
    return this.registry.hasEntry(this.id);
  }

  public void clear() {
    this.latch.clear();
  }

  public Type get() {
    if(!this.isValid()) {
      throw new ModNotLoadedException(this.id + " not found in " + this.registry);
    }

    return this.latch.get();
  }

  public RegistryId getId() {
    return this.id;
  }

  public String getTranslationKey() {
    return this.get().getTranslationKey();
  }

  @Override
  public boolean equals(final Object obj) {
    if(obj instanceof final RegistryDelegate<?> other) {
      return this.cls.equals(other.cls) && this.id.equals(other.id);
    }

    return false;
  }

  @Override
  public String toString() {
    if(!this.isValid()) {
      return "Invalid delegate[" + this.id + ']';
    }

    return "Delegate[" + this.get() + ']';
  }
}
