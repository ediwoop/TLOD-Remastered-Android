package legend.core.renderer.android;
import android.graphics.*;
import java.nio.*;
import legend.core.image.*;
/** Decode to unpremultiplied RGBA8, matching the texture upload contract. */
public final class AndroidImageDecoder implements ImageCodecs.Decoder {
 public DecodedImage decode(ByteBuffer encoded){byte[] bytes=new byte[encoded.remaining()];encoded.get(bytes);
  BitmapFactory.Options options=new BitmapFactory.Options();options.inPremultiplied=false;options.inScaled=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB);
  Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)throw new IllegalArgumentException("Android could not decode image");
  try{int w=bitmap.getWidth(),h=bitmap.getHeight();int[] argb=new int[Math.multiplyExact(w,h)];bitmap.getPixels(argb,0,w,0,0,w,h);
   ByteBuffer rgba=ByteBuffer.allocateDirect(Math.multiplyExact(argb.length,4));for(int p:argb){rgba.put((byte)(p>>16)).put((byte)(p>>8)).put((byte)p).put((byte)(p>>>24));}rgba.flip();return new DecodedImage(rgba,w,h,()->{});
  }finally{bitmap.recycle();}
 }
}
