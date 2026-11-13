package legend.core.video;
import java.nio.file.Path;import java.io.IOException;import java.util.Objects;
public final class VideoDecoders {
 @FunctionalInterface public interface Factory{VideoDecoder open(Path path)throws IOException;}
 private static Factory host;private static boolean used;
 private VideoDecoders(){}
 public static synchronized void install(Factory factory){if(host!=null||used)throw new IllegalStateException("Install movie decoder before playback");host=Objects.requireNonNull(factory);}
 public static VideoDecoder open(Path path)throws IOException{Factory factory;synchronized(VideoDecoders.class){used=true;factory=host;}if(path.getFileName().toString().toUpperCase(java.util.Locale.ROOT).endsWith(".IKI"))return new IkiVideoDecoder(path);return factory==null?new DesktopVideoDecoder(path):factory.open(path);}
}
