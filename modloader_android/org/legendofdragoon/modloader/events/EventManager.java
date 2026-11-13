package org.legendofdragoon.modloader.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.legendofdragoon.modloader.ModManager;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EventManager {
  private static final Logger LOGGER = LogManager.getFormatterLogger(EventManager.class);

  private final Map<Class<?>, List<EventBinding>> listeners = new HashMap<>();
  private final Set<EventBinding> staleListeners = new HashSet<>();
  private final BiConsumer<String, Throwable> errorCallback;

  private final LinkedList<Class<?>> eventHandlerStack = new LinkedList<>();

  private ModManager modManager;

  public EventManager(final Consumer<Access> access, final BiConsumer<String, Throwable> errorCallback) {
    this.errorCallback = errorCallback;
    access.accept(new Access());
  }

  public class Access {
    private Access() { }

    public void initialize(final ModManager mods) {
      EventManager.this.modManager = mods;

      LOGGER.info("Scanning for event consumers...");

      final Set<Class<?>> listeners;
      if(org.legendofdragoon.modloader.BuiltinCatalog.current() != null) {
        listeners = org.legendofdragoon.modloader.BuiltinCatalog.current().listeners();
      } else {
        listeners = org.legendofdragoon.modloader.DesktopDiscovery.listeners(mods);
      }

      for(final Class<?> listener : listeners) {
        EventManager.this.register(listener, null);
      }

      for(final var entry : EventManager.this.listeners.entrySet()) {
        entry.getValue().sort(Comparator.comparing((EventBinding e) -> e.priority).reversed());
      }
    }

    public void reset() {
      EventManager.this.listeners.clear();
      EventManager.this.staleListeners.clear();
    }
  }

  private void register(final Class<?> listener, @Nullable final Object instance) {
    for(final Method method : listener.getDeclaredMethods()) {
      if(method.isAnnotationPresent(EventListener.class)) {
        if((instance == null && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) || !java.lang.reflect.Modifier.isPublic(method.getModifiers()) || !java.lang.reflect.Modifier.isPublic(listener.getModifiers())) {
          LOGGER.warn("Event listener %s must be static", listener);
          continue;
        }

        if(method.getParameterCount() != 1) {
          LOGGER.warn("Event listener %s must have one parameter", listener);
          continue;
        }

        final Class<?> eventType = method.getParameters()[0].getType();

        if(!Event.class.isAssignableFrom(eventType)) {
          LOGGER.warn("Event listener %s must have event parameter", listener);
          continue;
        }

        synchronized(this.listeners) {
          final List<EventBinding> bindings = this.listeners.computeIfAbsent(eventType, k -> new ArrayList<>());
          bindings.add(new EventBinding(eventType, listener, method.getAnnotation(EventListener.class).priority(), instance, method));
          bindings.sort(Comparator.comparing((EventBinding e) -> e.priority).reversed());
        }
      }
    }
  }

  public void register(final Object listener) {
    this.register(listener.getClass(), listener);
  }

  public <T extends Event> T postEvent(final T event) {
    synchronized(this.listeners) {
      final List<EventBinding> bindings = this.listeners.get(event.getClass());

      if(bindings != null) {
        for(int i = 0; i < bindings.size(); i++) {
          final EventBinding binding = bindings.get(i);

          if(event.shouldPropagate()) { // Don't exit immediately so that stale listeners still get cleared
            try {
              this.eventHandlerStack.push(binding.listenerClass);
              this.modManager.setActiveMod(binding.listenerClass);
              binding.execute(event);
            } catch(final IllegalAccessException | InvocationTargetException e) {
              String modId = "Unknown mod";
              final Set<String> modIds = this.modManager.getModsByClass(binding.listenerClass);

              if(modIds != null) {
                modId = modIds.stream().sorted().collect(Collectors.joining(","));
              }

              if(event.isCritical()) {
                Throwable t = e;

                if(t.getCause() != null) {
                  t = t.getCause();
                }

                throw new EventHandlerException(modId + " event handler for " + event.getClass().getSimpleName() + " threw an exception", t);
              }

              LOGGER.error("Failed to deliver event", e);
              this.errorCallback.accept(modId, e);
            } finally {
              this.eventHandlerStack.pop();
            }
          }

          if(binding.isInvalid()) {
            this.staleListeners.add(binding);
          }
        }
      }

      this.modManager.setActiveMod(this.eventHandlerStack.isEmpty() ? null : this.eventHandlerStack.peek());
    }

    return event;
  }

  public void clearStaleRefs() {
    synchronized(this.listeners) {
      for(final EventBinding binding : this.staleListeners) {
        this.listeners.get(binding.eventClass).remove(binding);
      }

      this.staleListeners.clear();
    }
  }
}
