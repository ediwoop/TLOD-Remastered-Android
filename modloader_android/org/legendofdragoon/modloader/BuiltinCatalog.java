package org.legendofdragoon.modloader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/** Explicit catalog for built-in DEX classes. Installed before the engine initializes. */
public final class BuiltinCatalog {
  private static BuiltinCatalog current;
  private final List<String> modTypes;
  private final List<String> listenerTypes;
  private final Path resources;
  private final ClassLoader loader;

  public BuiltinCatalog(Collection<String> mods,Collection<String> listeners,Path resources,ClassLoader loader) {
    this.modTypes=List.copyOf(mods);this.listenerTypes=List.copyOf(listeners);
    if(new HashSet<>(modTypes).size()!=modTypes.size()||new HashSet<>(listenerTypes).size()!=listenerTypes.size())throw new IllegalArgumentException("Duplicate catalog classes");
    this.resources=resources.toAbsolutePath().normalize();this.loader=Objects.requireNonNull(loader);
  }
  public static synchronized void install(BuiltinCatalog catalog){if(current!=null)throw new IllegalStateException("Catalog already installed");current=Objects.requireNonNull(catalog);}
  public static synchronized BuiltinCatalog current(){return current;}
  private Set<Class<?>> resolve(List<String> names){Set<Class<?>> classes=new LinkedHashSet<>();for(String name:names)try{classes.add(Class.forName(name,false,loader));}catch(ClassNotFoundException e){throw new IllegalStateException("Missing built-in class "+name,e);}return Collections.unmodifiableSet(classes);}
  public Set<Class<?>> mods(){return resolve(modTypes);}
  public Set<Class<?>> listeners(){return resolve(listenerTypes);}
  private Path path(String relative){Path r=Path.of(relative);if(r.isAbsolute())throw new IllegalArgumentException("Expected relative resource");Path p=resources.resolve(r).normalize();if(!p.startsWith(resources))throw new IllegalArgumentException("Outside resources");return p;}
  public URL resource(String relative){Path p=path(relative);if(!Files.isRegularFile(p))return null;try{return p.toUri().toURL();}catch(java.net.MalformedURLException e){throw new IllegalArgumentException(e);}}
  public List<String> list(String relative){Path p=path(relative);if(!Files.isDirectory(p))return List.of();try(var files=Files.list(p)){List<String> result=new ArrayList<>();files.filter(Files::isRegularFile).sorted().forEach(f->result.add(f.getFileName().toString()));return Collections.unmodifiableList(result);}catch(IOException e){throw new IllegalStateException("Cannot list "+p,e);}}
}
