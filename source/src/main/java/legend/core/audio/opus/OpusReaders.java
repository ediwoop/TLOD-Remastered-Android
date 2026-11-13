package legend.core.audio.opus;
import java.util.Objects;
public final class OpusReaders {
 @FunctionalInterface public interface Factory { OpusReader open(byte[] compressed); }
 private static Factory host;private static boolean used;
 private OpusReaders(){}
 public static synchronized void install(Factory factory){if(host!=null||used)throw new IllegalStateException("Install Opus reader before decoding");host=Objects.requireNonNull(factory);}
 public static OpusReader open(byte[] compressed){Factory factory;synchronized(OpusReaders.class){used=true;factory=host;}
  if(factory!=null)return Objects.requireNonNull(factory.open(compressed));
  if(legend.core.audio.output.AudioOutputs.hosted())throw new UnsupportedOperationException("The host has not installed an Opus decoder");
  return new DesktopOpusReader(compressed);
 }
}
