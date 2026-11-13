package com.realbox.dragoon;
import android.content.res.AssetManager;
import java.nio.file.*;import java.io.*;import java.util.*;import java.util.zip.*;
import legend.game.unpacker.PalSpanishLayout;
/** Installs the privately supplied disc's transformed data, never saves or settings. */
final class PalDataInstaller {
 interface Progress { void update(int done,int total); }
 static void install(AssetManager assets,Path root,Progress progress)throws IOException {
  String expected;try(InputStream in=assets.open("pal-data.sha256")){expected=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();}catch(FileNotFoundException noData){return;}
  Path marker=root.resolve("pal-installed.sha256"),target=root.resolve("files");
  if(Files.isRegularFile(marker)&&legend.core.TextFiles.readString(marker).equals(expected)&&PalSpanishLayout.isPrepared(target))return;
  int expectedCount;try(InputStream in=assets.open("pal-data.count")){expectedCount=Integer.parseInt(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim());}
  if(expectedCount<1)throw new IOException("Invalid PAL dataset count");
  Path staging=root.resolve("files-import");remove(staging);Files.createDirectories(staging);int count=0;
  Set<Path> directories=new HashSet<>();directories.add(staging);progress.update(0,expectedCount);
  try(ZipInputStream zip=new ZipInputStream(assets.open("pal-data.zip"))){ZipEntry entry;byte[] buffer=new byte[65536];
   while((entry=zip.getNextEntry())!=null){Path out=staging.resolve(entry.getName()).normalize();if(!out.startsWith(staging)||entry.getName().startsWith("/"))throw new IOException("Invalid data archive path");
    if(entry.isDirectory()){if(directories.add(out))Files.createDirectories(out);continue;}
    if(directories.add(out.getParent()))Files.createDirectories(out.getParent());
    try(OutputStream writer=new FileOutputStream(out.toFile())){int n;while((n=zip.read(buffer))!=-1)writer.write(buffer,0,n);}count++;
    if(count%500==0){progress.update(count,expectedCount);android.util.Log.i("DragoonData","Installing PAL "+count+"/"+expectedCount);}
   }
  }
  if(count!=expectedCount||!PalSpanishLayout.isPrepared(staging))throw new IOException("PAL data installation is incomplete");
  Path previous=root.resolve("files-previous");remove(previous);if(Files.exists(target))Files.move(target,previous);
  Files.move(staging,target);legend.core.TextFiles.writeString(marker,expected);remove(previous);
  android.util.Log.i("DragoonData","Installed verified PAL dataset, files="+count);
  progress.update(count,expectedCount);
 }
 private static void remove(Path path)throws IOException{if(!Files.exists(path))return;try(var stream=Files.walk(path)){for(Path p:stream.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
}
