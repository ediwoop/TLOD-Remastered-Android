package org.legendofdragoon.modloader.registries;

import org.legendofdragoon.modloader.events.EventManager;
import org.legendofdragoon.modloader.events.registries.RegistryEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class Registries implements Iterable<Registry<?>> {
  private final EventManager events;
  private final Map<RegistryId, MutableRegistry<?>> registries = new LinkedHashMap<>();
  private final Map<MutableRegistry<?>, Function<MutableRegistry<?>, RegistryEvent.Register<?>>> registryEvents = new HashMap<>();

  protected Registries(final EventManager events, final Consumer<Access> access) {
    this.events = events;
    access.accept(new Access());
  }

  protected <Type extends RegistryEntry> Registry<Type> addRegistry(final Registry<Type> registry, final Function<MutableRegistry<Type>, RegistryEvent.Register<Type>> registryEvent) {
    final MutableRegistry<Type> mutableRegistry = (MutableRegistry<Type>)registry;
    this.registries.put(mutableRegistry.id, mutableRegistry);
    //noinspection unchecked
    this.registryEvents.put(mutableRegistry, (Function<MutableRegistry<?>, RegistryEvent.Register<?>>)(Object)registryEvent);
    return registry;
  }

  public <T extends RegistryEntry> Registry<T> get(final RegistryId id) {
    return (Registry<T>)this.registries.get(id);
  }

  public int count() {
    return this.registries.size();
  }

  @Override
  public Iterator<Registry<?>> iterator() {
    return (Iterator)this.registries.values().iterator();
  }

  public class Access {
    private Access() { }

    private final Set<Registry<?>> initialized = new HashSet<>();

    public void initialize(final Registry<?> registry) {
      if(this.initialized.contains(registry)) {
        throw new IllegalStateException("Registry " + registry + " already initialized");
      }

      final MutableRegistry<?> mutableRegistry = (MutableRegistry<?>)registry;

      if(!Registries.this.registryEvents.containsKey(mutableRegistry)) {
        throw new IllegalArgumentException("Unknown registry " + registry);
      }

      Registries.this.events.postEvent(Registries.this.registryEvents.get(mutableRegistry).apply(mutableRegistry));
      mutableRegistry.lock();
      this.initialized.add(mutableRegistry);
    }

    public void initializeRemaining() {
      for(final MutableRegistry<?> registry : Registries.this.registries.values()) {
        if(!this.initialized.contains(registry)) {
          this.initialize(registry);
        }
      }
    }

    public void reset() {
      this.initialized.clear();

      for(final MutableRegistry<?> registry : Registries.this.registries.values()) {
        registry.reset();
      }
    }
  }
}
