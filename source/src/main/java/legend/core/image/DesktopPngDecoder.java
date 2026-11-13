package legend.core.image;
import java.nio.*;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.stb.STBImage.*;
/** Desktop-only decoder: no native library is touched when a host decoder is installed. */
public final class DesktopPngDecoder {
 private DesktopPngDecoder(){}
 public static DecodedImage decode(ByteBuffer encoded){try(MemoryStack stack=stackPush()){
  IntBuffer w=stack.mallocInt(1),h=stack.mallocInt(1),components=stack.mallocInt(1);
  ByteBuffer pixels=stbi_load_from_memory(encoded,w,h,components,4);
  if(pixels==null)throw new IllegalArgumentException("Failed to load image: "+stbi_failure_reason());
  try{return new DecodedImage(pixels,w.get(0),h.get(0),()->stbi_image_free(pixels));}catch(RuntimeException ex){stbi_image_free(pixels);throw ex;}
 }}
}
