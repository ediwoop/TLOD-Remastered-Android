package legend.core.image;
import java.nio.ByteBuffer;
import java.util.Objects;
/** RGBA8 pixels owned until close; host may release native storage there. */
public final class DecodedImage implements AutoCloseable {
 public final ByteBuffer pixels;
 public final int width,height;
 private Runnable release;
 public DecodedImage(ByteBuffer pixels,int width,int height,Runnable release){
  if(width<=0||height<=0||pixels.remaining()<Math.multiplyExact(Math.multiplyExact(width,height),4))throw new IllegalArgumentException("Invalid RGBA image");
  this.pixels=pixels;this.width=width;this.height=height;this.release=Objects.requireNonNull(release);
 }
 public synchronized void close(){if(release!=null){Runnable cleanup=release;release=null;cleanup.run();}}
}
