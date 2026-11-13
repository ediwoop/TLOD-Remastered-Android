package legend.core.audio.output;
import java.util.*;
/** Installed by the host before any AudioSource is initialized. */
public final class AudioOutputs {
 @FunctionalInterface public interface Factory { PcmStream create(int bufferCount); }
 private static Factory host;private static boolean used;
 private AudioOutputs(){}
 public static synchronized void install(Factory factory){if(host!=null||used)throw new IllegalStateException("Install audio output before initialization");host=Objects.requireNonNull(factory);}
 public static synchronized boolean hosted(){return host!=null;}
 public static PcmStream create(int buffers){Factory factory;synchronized(AudioOutputs.class){used=true;factory=host;}return Objects.requireNonNull(factory==null?new DesktopPcmStream(buffers):factory.create(buffers));}
}
