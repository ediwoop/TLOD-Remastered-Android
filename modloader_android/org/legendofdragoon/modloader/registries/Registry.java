package org.legendofdragoon.modloader.registries;

import org.apache.commons.collections4.map.ReferenceMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Registry<Type extends RegistryEntry> implements Iterable<RegistryId> {
  public final RegistryId id;
  protected final Map<RegistryId, Type> entries = new HashMap<>();
  protected final Map<RegistryId, RegistryDelegate<Type>> delegates = new ReferenceMap<>();

  public Registry(final RegistryId id) {
    this.id = id;
  }

  public boolean hasEntry(final RegistryId id) {
    return this.entries.containsKey(id);
  }

  public RegistryDelegate<Type> getEntry(final RegistryId id) {
    return this.delegates.computeIfAbsent(id, key -> new RegistryDelegate<>(id, this, (Class<Registry<Type>>)this.getClass(), () -> this.entries.get(key)));
  }

  public RegistryDelegate<Type> getEntry(final String id) {
    return this.getEntry(new RegistryId(id));
  }

  public RegistryDelegate<Type> getEntry(final String modId, final String entryId) {
    return this.getEntry(new RegistryId(modId, entryId));
  }

  public int size() {
    return this.entries.size();
  }

  @Override
  public Iterator<RegistryId> iterator() {
    return this.entries.keySet().iterator();
  }

  @Override
  public String toString() {
    return "Registry " + this.id;
  }
}
