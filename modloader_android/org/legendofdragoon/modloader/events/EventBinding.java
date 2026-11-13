package org.legendofdragoon.modloader.events;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EventBinding {
  public final Class<?> eventClass;
  public final Class<?> listenerClass;
  public final Priority priority;
  private final WeakReference<Object> instance;
  private final Method method;
  private boolean invalid;

  public EventBinding(final Class<?> eventClass, final Class<?> listenerClass, final Priority priority, @Nullable final Object instance, final Method method) {
    this.eventClass = eventClass;
    this.listenerClass = listenerClass;
    this.instance = instance != null ? new WeakReference<>(instance) : null;
    this.method = method;
    this.priority = priority;
  }

  public void execute(final Event event) throws InvocationTargetException, IllegalAccessException {
    if(this.instance == null) {
      this.method.invoke(null, event);
    } else {
      final Object instance = this.instance.get();

      if(instance != null) {
        this.method.invoke(instance, event);
      } else {
        this.invalid = true;
      }
    }
  }

  public boolean isInvalid() {
    return this.invalid;
  }
}
