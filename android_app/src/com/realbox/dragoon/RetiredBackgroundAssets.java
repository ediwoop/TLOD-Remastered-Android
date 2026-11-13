package com.realbox.dragoon;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
/** Remove only the discarded background masters left by older installed APKs. */
public final class RetiredBackgroundAssets {
 private RetiredBackgroundAssets() { }
 public static void remove(final Path gameRoot)throws IOException {
  for(final String old:new String[]{"gfx/remaster/user-rose/atlas.png","gfx/remaster/user-rose/runtime.json"})Files.deleteIfExists(gameRoot.resolve(old));
  for(final String oldFolder:new String[]{"gfx/remaster/rose-fbx","gfx/remaster/user-rose","gfx/remaster/characters"}) {
   final Path old=gameRoot.resolve(oldFolder);
   if(Files.isDirectory(old))try(var files=Files.walk(old)){for(Path path:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
  }
  final Path folder=gameRoot.resolve("gfx/remaster/submaps");
  for(final int cut:new int[]{675,704,10})for(final String suffix:new String[]{".png","-surround.png","-master20.png","-master21.png"})
   Files.deleteIfExists(folder.resolve(cut+suffix));
 }
}
