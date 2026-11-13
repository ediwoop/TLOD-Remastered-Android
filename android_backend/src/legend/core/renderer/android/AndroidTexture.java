package legend.core.renderer.android;

import android.opengl.GLES32;
import legend.core.renderer.*;
import java.nio.*;

/** Texture uploads through Android Buffer APIs; no desktop memory-address bindings. */
public final class AndroidTexture extends Texture {
  private static final ThreadLocal<java.util.BitSet> usedUnits=ThreadLocal.withInitial(java.util.BitSet::new);
  static void unbind(){java.util.BitSet units=usedUnits.get();for(int unit=units.nextSetBit(0);unit>=0;unit=units.nextSetBit(unit+1)){GLES32.glActiveTexture(GLES32.GL_TEXTURE0+unit);GLES32.glBindTexture(GLES32.GL_TEXTURE_2D,0);}units.clear();GLES32.glActiveTexture(GLES32.GL_TEXTURE0);}
  final int id;
  private final TextureInternalFormat internal;
  private final TextureDataFormat format;
  private final TextureDataType type;
  private final boolean min,mag,s,t;
  private boolean destroyed;
  public AndroidTexture(Buffer pixels,String name,int width,int height,TextureInternalFormat internal,TextureDataFormat format,TextureDataType type,boolean min,boolean mag,boolean s,boolean t){
    super(name,width,height);
    if(width<=0||height<=0)throw new IllegalArgumentException("Invalid texture dimensions");
    this.internal=internal;this.format=format;this.type=type;this.min=min;this.mag=mag;this.s=s;this.t=t;
    int[] ids=new int[1];GLES32.glGenTextures(1,ids,0);id=ids[0];use();
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D,GLES32.GL_TEXTURE_MIN_FILTER,min?GLES32.GL_LINEAR:GLES32.GL_NEAREST);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D,GLES32.GL_TEXTURE_MAG_FILTER,mag?GLES32.GL_LINEAR:GLES32.GL_NEAREST);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D,GLES32.GL_TEXTURE_WRAP_S,s?GLES32.GL_REPEAT:GLES32.GL_CLAMP_TO_EDGE);
    GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D,GLES32.GL_TEXTURE_WRAP_T,t?GLES32.GL_REPEAT:GLES32.GL_CLAMP_TO_EDGE);
    int internalGl=switch(internal){case RGB_8->GLES32.GL_RGB8;case RGBA_8->GLES32.GL_RGBA8;case R_32_UINT->GLES32.GL_R32UI;case DEPTH_COMPONENT->type==TextureDataType.FLOAT?GLES32.GL_DEPTH_COMPONENT32F:GLES32.GL_DEPTH_COMPONENT24;};
    int[] alignment=new int[1];GLES32.glGetIntegerv(GLES32.GL_UNPACK_ALIGNMENT,alignment,0);GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT,1);
    try{GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D,0,internalGl,width,height,0,format(format),type(type),pixels);}finally{GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT,alignment[0]);}
  }
  private static int format(TextureDataFormat value){return switch(value){case RGB->GLES32.GL_RGB;case RGBA->GLES32.GL_RGBA;case RED_INT->GLES32.GL_RED_INTEGER;case DEPTH_COMPONENT->GLES32.GL_DEPTH_COMPONENT;};}
  private static int type(TextureDataType value){return switch(value){case UBYTE->GLES32.GL_UNSIGNED_BYTE;case UINT->GLES32.GL_UNSIGNED_INT;case FLOAT->GLES32.GL_FLOAT;};}
  private void write(int x,int y,int w,int h,TextureDataType dataType,Buffer pixels){
    if(x<0||y<0||w<0||h<0||w>width-x||h>height-y)throw new IndexOutOfBoundsException("Texture upload outside bounds");
    use();int[] alignment=new int[1];GLES32.glGetIntegerv(GLES32.GL_UNPACK_ALIGNMENT,alignment,0);GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT,1);
    try{GLES32.glTexSubImage2D(GLES32.GL_TEXTURE_2D,0,x,y,w,h,format(format),type(dataType),pixels);}finally{GLES32.glPixelStorei(GLES32.GL_UNPACK_ALIGNMENT,alignment[0]);}
  }
  public void data(int x,int y,int w,int h,TextureDataType type,ByteBuffer pixels){write(x,y,w,h,type,pixels);}
  public void data(int x,int y,int w,int h,TextureDataType type,int[] pixels){IntBuffer buf=ByteBuffer.allocateDirect(Math.multiplyExact(pixels.length,4)).order(ByteOrder.nativeOrder()).asIntBuffer();buf.put(pixels).flip();write(x,y,w,h,type,buf);}
  public void use(){use(0);}
  public void use(int unit){if(destroyed)throw new IllegalStateException("Texture deleted");if(unit<0)throw new IllegalArgumentException("Negative texture unit");GLES32.glActiveTexture(GLES32.GL_TEXTURE0+unit);GLES32.glBindTexture(GLES32.GL_TEXTURE_2D,id);usedUnits.get().set(unit);}
  protected void performDelete(){if(destroyed)return;destroyed=true;GLES32.glDeleteTextures(1,new int[]{id},0);}
  public TextureInternalFormat internalFormat(){return internal;}
  public TextureDataFormat dataFormat(){return format;}
  public TextureDataType dataType(){return type;}
  public boolean minFilter(){return min;}
  public boolean magFilter(){return mag;}
  public boolean wrapS(){return s;}
  public boolean wrapT(){return t;}
}
