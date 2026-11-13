package org.legendofdragoon.modloader.registries;

import org.legendofdragoon.modloader.ModContainer;

public class MutableRegistry<Type extends RegistryEntry> extends Registry<Type> {
  private boolean locked;

  public MutableRegistry(final RegistryId id) {
    super(id);
  }

  public Type register(final RegistryId id, final Type entry) {
    if(this.locked) {
      throw new RegistryLockedException();
    }

    if(!ModContainer.isModActive(id.modId())) {
      throw new IncorrectModIdException("Mod tried to register " + this + " entry " + id.entryId() + " for incorrect mod ID " + id.modId());
    }

    if(this.entries.containsKey(id)) {
      throw new DuplicateRegistryIdException("Registry ID " + id + " already registered");
    }

    entry.setRegistry(this, id);
    this.entries.put(id, entry);
    return entry;
  }

  void lock() {
    this.locked = true;
  }

  void reset() {
    this.locked = false;

    for(final RegistryDelegate<Type> delegate : this.delegates.values()) {
      delegate.clear();
    }

    this.entries.clear();
  }
}
