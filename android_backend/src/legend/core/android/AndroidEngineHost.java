package legend.core.android;

import android.view.Surface;
import legend.core.*;
import legend.core.platform.*;
import org.legendofdragoon.modloader.BuiltinCatalog;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Experimental entry point: run once, on a dedicated engine thread, after Surface and files exist. */
public final class AndroidEngineHost {
 private static final AtomicBoolean started=new AtomicBoolean();
 private volatile AndroidPlatformManager platform;
 private final Object lifecycleLock=new Object();
 private Surface currentSurface;
 private boolean surfaceSpecified,focused=true;
 public void setSurface(Surface value){synchronized(lifecycleLock){surfaceSpecified=true;currentSurface=value;if(platform!=null)platform.setSurface(value);}}
 public void setFocused(boolean value){synchronized(lifecycleLock){focused=value;if(platform!=null)platform.submitFocus(value);}}
 public void run(Surface surface,Path appData)throws Exception {
  System.setProperty("log4j.provider","org.apache.logging.log4j.simple.internal.SimpleProvider");
  System.setProperty("org.apache.logging.log4j.simplelog.level","INFO");

  Path root=appData.toAbsolutePath().normalize();
  Files.deleteIfExists(root.resolve("android-boot.log"));
  Files.deleteIfExists(root.getParent().resolve("engine-error.txt"));
  trace(root,"resources-ready");
  if(!Files.isRegularFile(root.resolve("gfx/fonts/default.json"))||!Files.isDirectory(root.resolve("resources/lod_core/lang"))||!Files.isRegularFile(root.resolve("android-shaders/tmd.gsh")))throw new IllegalStateException("Engine resources have not been installed");
  if(!started.compareAndSet(false,true))throw new IllegalStateException("Engine restart in the same process is not supported yet");
  GamePaths.configure(root);
  legend.game.types.LodString.usePalSpanishCharset(true);
  legend.core.audio.output.AudioOutputs.install(legend.core.audio.android.AndroidPcmStream::new);
  legend.core.video.VideoDecoders.install(legend.core.video.android.AndroidVideoDecoder::new);
  legend.core.audio.opus.OpusReaders.install(legend.core.audio.android.AndroidOpusReader::new);
  legend.core.image.ImageCodecs.install(new legend.core.renderer.android.AndroidImageDecoder());
  BuiltinCatalog.install(new BuiltinCatalog(
    List.of("legend.game.modding.coremod.CoreMod","legend.lodmod.LodMod","legend.turnorder.TurnOrderMod"),
    List.of("legend.game.modding.coremod.CoreMod","legend.lodmod.LodMod"),
    root.resolve("resources"),AndroidEngineHost.class.getClassLoader()));
  AndroidPlatformManager backend=new AndroidPlatformManager(surface,path->legend.core.TextFiles.readString(root.resolve("android-shaders").resolve(path.getFileName())));
  synchronized(lifecycleLock){platform=backend;if(surfaceSpecified)backend.setSurface(currentSurface);backend.submitFocus(focused);}
  PlatformProvider.install(()->backend);
  trace(root,"android-platform-installed");
  boolean coreLoaded=false;
  Throwable runFailure=null;
  try{
   Class.forName("legend.core.GameEngine",true,AndroidEngineHost.class.getClassLoader());
   coreLoaded=true;
   trace(root,"core-initialized");
   GameEngine.start(EngineStartupOptions.android());
  }catch(Exception|Error failure){
   runFailure=failure;
   // Preserve the original error even when a driver blocks during EGL cleanup.
   android.util.Log.e("DragoonEngine","Engine failed before platform cleanup",failure);
   java.io.StringWriter message=new java.io.StringWriter();failure.printStackTrace(new java.io.PrintWriter(message));
   try{legend.core.TextFiles.writeString(root.getParent().resolve("engine-error.txt"),message.toString());}catch(java.io.IOException writeFailure){failure.addSuppressed(writeFailure);}
   throw failure;
  }finally{
   final boolean loaded=coreLoaded;
   EngineCleanup.run(runFailure,
    ()->{if(loaded)legend.game.unpacker.Unpacker.stop();},
    ()->{if(loaded)GameEngine.AUDIO_THREAD.destroy();},
    backend::destroy,
    ()->platform=null);
  }
 }
 private static void trace(Path root,String phase){
  android.util.Log.i("DragoonBoot",phase);
  try{Files.write(root.resolve("android-boot.log"),(android.os.SystemClock.elapsedRealtime()+" "+phase+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(java.io.IOException e){android.util.Log.w("DragoonBoot","Could not write startup trace",e);}
 }
 public void requestClose(){AndroidPlatformManager p=platform;if(p!=null){Window w=p.getLastWindow();if(w!=null)w.close();}}
 public AndroidPlatformManager platform(){return platform;}
}
