package legend.core.renderer.android;

import android.opengl.GLES32;
import legend.core.renderer.ShaderUniformBuffer;
import java.nio.FloatBuffer;

/** Engine uniform data uploaded through Android GLES; context-thread only. */
public final class AndroidUniformBuffer implements ShaderUniformBuffer {
  private final int id,size;
  private boolean deleted;
  public AndroidUniformBuffer(long bytes,int binding){
    if(bytes<=0||binding<0)throw new IllegalArgumentException("Invalid uniform buffer size or binding");
    size=Math.toIntExact(bytes);int[] ids=new int[1];GLES32.glGenBuffers(1,ids,0);id=ids[0];
    GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER,id);GLES32.glBufferData(GLES32.GL_UNIFORM_BUFFER,size,null,GLES32.GL_DYNAMIC_DRAW);GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER,0);GLES32.glBindBufferBase(GLES32.GL_UNIFORM_BUFFER,binding,id);
  }
  public void set(FloatBuffer buffer){set(0,buffer);}
  public void set(long offset,FloatBuffer buffer){
    if(deleted)throw new IllegalStateException("Buffer deleted");
    int bytes=Math.multiplyExact(buffer.remaining(),Float.BYTES);
    if(offset<0||offset>size||bytes>size-offset)throw new IndexOutOfBoundsException("Uniform write exceeds buffer");
    GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER,id);GLES32.glBufferSubData(GLES32.GL_UNIFORM_BUFFER,Math.toIntExact(offset),bytes,buffer);GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER,0);
  }
  public void delete(){if(deleted)return;deleted=true;GLES32.glDeleteBuffers(1,new int[]{id},0);}
}
