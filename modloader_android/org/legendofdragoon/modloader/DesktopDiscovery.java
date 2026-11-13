package org.legendofdragoon.modloader;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.legendofdragoon.modloader.events.EventListener;
import java.net.URL;
import java.util.List;
import java.util.Set;

/** Desktop-only discovery, kept outside the DEX bootstrap dependency path. */
public final class DesktopDiscovery {
 private DesktopDiscovery(){}
 public static Set<Class<?>> mods(ClassLoader core,ClassLoader plugins,List<URL> urls){
  ConfigurationBuilder cfg=new ConfigurationBuilder().addClassLoaders(core).addUrls(ClasspathHelper.forPackage("legend")).addClassLoaders(plugins).addUrls(urls);
  URL root=core.getResource("");if(root!=null)cfg.addUrls(root);
  return new Reflections(cfg).getTypesAnnotatedWith(Mod.class);
 }
 public static Set<Class<?>> listeners(ModManager mods){
  ClassLoader core=DesktopDiscovery.class.getClassLoader();
  ConfigurationBuilder cfg=new ConfigurationBuilder().addClassLoaders(core).addUrls(ClasspathHelper.forPackage("legend")).addUrls(mods.discoveryUrls()).addClassLoaders(mods.discoveryLoaders().toArray(ClassLoader[]::new));
  URL root=core.getResource("");if(root!=null)cfg.addUrls(root);
  return new Reflections(cfg).getTypesAnnotatedWith(EventListener.class);
 }
}
