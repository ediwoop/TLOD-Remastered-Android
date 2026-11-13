package legend.core.memory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/** Direct native-order data buffers without LWJGL runtime initialization. */
public final class BufferUtils {
  private BufferUtils() { }
  public static ByteBuffer createByteBuffer(int bytes) {
    return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
  }
  public static FloatBuffer createFloatBuffer(int values) {
    return createByteBuffer(Math.multiplyExact(values,Float.BYTES)).asFloatBuffer();
  }
  public static IntBuffer createIntBuffer(int values) {
    return createByteBuffer(Math.multiplyExact(values,Integer.BYTES)).asIntBuffer();
  }
  public static ShortBuffer createShortBuffer(int values) {
    return createByteBuffer(Math.multiplyExact(values,Short.BYTES)).asShortBuffer();
  }
}
