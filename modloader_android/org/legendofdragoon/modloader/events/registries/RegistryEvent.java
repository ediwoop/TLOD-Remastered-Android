package org.legendofdragoon.modloader.events.registries;

import org.legendofdragoon.modloader.events.Event;
import org.legendofdragoon.modloader.registries.MutableRegistry;
import org.legendofdragoon.modloader.registries.RegistryEntry;
import org.legendofdragoon.modloader.registries.RegistryId;

public abstract class RegistryEvent<Type extends RegistryEntry> extends Event {
  protected final MutableRegistry<Type> registry;

  RegistryEvent(final MutableRegistry<Type> registry) {
    this.registry = registry;
  }

  public static abstract class Register<Type extends RegistryEntry> extends RegistryEvent<Type> {
    public Register(final MutableRegistry<Type> registry) {
      super(registry);
    }

    public Type register(final RegistryId id, final Type entry) {
      return this.registry.register(id, entry);
    }

    @Override
    protected boolean isCritical() {
      return true;
    }
  }
}
