package legend.core.image;
import java.nio.ByteBuffer;
import java.util.Objects;
/** Host override for image decoding, installed before image/texture loading. */
public final class ImageCodecs {
 @FunctionalInterface public interface Decoder { DecodedImage decode(ByteBuffer encoded); }
 private static Decoder decoder;
 private static boolean used;
 private ImageCodecs(){}
 public static synchronized void install(Decoder host){if(decoder!=null||used)throw new IllegalStateException("Image decoder must be installed before decoding");decoder=Objects.requireNonNull(host);}
 public static DecodedImage decode(ByteBuffer encoded){Decoder host;synchronized(ImageCodecs.class){used=true;host=decoder;}return host!=null?host.decode(encoded.duplicate()):DesktopPngDecoder.decode(encoded.duplicate());}
}
