package legend.core.renderer.android;

import android.opengl.GLES32;
import legend.core.renderer.*;

public final class AndroidFrameBuffer implements FrameBuffer {
  private final int id;
  private boolean deleted;
  public AndroidFrameBuffer(String name,FrameBufferAttachment[] attachments){
    for(FrameBufferAttachment a:attachments)if(!(a.texture instanceof AndroidTexture))throw new IllegalArgumentException("Framebuffer requires Android textures");
    int[] old=new int[1],ids=new int[1];GLES32.glGetIntegerv(GLES32.GL_FRAMEBUFFER_BINDING,old,0);GLES32.glGenFramebuffers(1,ids,0);id=ids[0];
    try{GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER,id);
      for(FrameBufferAttachment a:attachments){int type=switch(a.type){case COLOUR->GLES32.GL_COLOR_ATTACHMENT0;case DEPTH->GLES32.GL_DEPTH_ATTACHMENT;};GLES32.glFramebufferTexture2D(GLES32.GL_FRAMEBUFFER,type,GLES32.GL_TEXTURE_2D,((AndroidTexture)a.texture).id,0);}
      int status=GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER);if(status!=GLES32.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Incomplete framebuffer "+name+": "+status);
    }catch(RuntimeException ex){GLES32.glDeleteFramebuffers(1,new int[]{id},0);throw ex;}finally{GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER,old[0]);}
  }
  public void bind(){if(deleted)throw new IllegalStateException("Framebuffer deleted");GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER,id);}
  public void delete(){if(deleted)return;deleted=true;GLES32.glDeleteFramebuffers(1,new int[]{id},0);}
}
