package com.realbox.dragoon;
import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.io.*;
import legend.core.*;import legend.core.platform.*;import org.legendofdragoon.modloader.*;
/** Internal ART integration test; does not render or run a game scene. */
public final class CoreCheck {
 public static void main(String[] args)throws Exception{
  System.setProperty("log4j.provider","org.apache.logging.log4j.simple.internal.SimpleProvider");
  Path apk=Path.of(args[0]),root=Path.of(args[1]);Files.createDirectories(root);
  try(ZipFile zip=new ZipFile(apk.toFile())){var entries=zip.entries();while(entries.hasMoreElements()){
   ZipEntry entry=entries.nextElement();String n=entry.getName();if(entry.isDirectory()||!(n.equals("assets/gfx/fonts/default.json")||n.startsWith("assets/resources/")))continue;
   Path out=root.resolve(n.substring("assets/".length())).normalize();if(!out.startsWith(root))throw new IOException("Invalid asset path");Files.createDirectories(out.getParent());try(InputStream in=zip.getInputStream(entry)){Files.copy(in,out,StandardCopyOption.REPLACE_EXISTING);}
  }}
  System.out.println("STAGE: resources installed");
  GamePaths.configure(root);PlatformProvider.install(NoopPlatformManager::new);
  BuiltinCatalog.install(new BuiltinCatalog(List.of("legend.game.modding.coremod.CoreMod","legend.lodmod.LodMod","legend.turnorder.TurnOrderMod"),List.of("legend.game.modding.coremod.CoreMod","legend.lodmod.LodMod"),root.resolve("resources"),CoreCheck.class.getClassLoader()));
  Class.forName("legend.core.GameEngine");System.out.println("STAGE: actual engine static initialization completed");
  var field=GameEngine.class.getDeclaredField("MOD_ACCESS");field.setAccessible(true);ModManager.Access access=(ModManager.Access)field.get(null);access.findMods(root.resolve("mods"),Version.VERSION);
  Set<String> modules=Set.of("lod","lod_core","turn_order");if(!GameEngine.bootMods(modules).isEmpty()||!GameEngine.MODS.getFailedToLoad().isEmpty())throw new AssertionError("Module initialization failed");
  System.out.println("PASS ART core modules: config="+GameEngine.REGISTRIES.config.size()+",states="+GameEngine.REGISTRIES.engineStateTypes.size()+",inputs="+GameEngine.REGISTRIES.inputActions.size()+",characters="+GameEngine.REGISTRIES.characterTemplates.size());
 }
}
