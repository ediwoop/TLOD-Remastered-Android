package com.realbox.dragoon;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.opengl.*;
import legend.core.platform.*;
import legend.core.renderer.android.AndroidRenderApi;
import java.lang.reflect.Method;

/** Real Android EGL regression: replace Surface repeatedly without losing GPU resources. */
public final class LifecycleCheck {
 public static void main(String[] args){try{run();System.exit(0);}catch(Throwable error){error.printStackTrace();System.exit(1);}}
 private static void require(boolean condition,String why){if(!condition)throw new AssertionError(why+" egl="+Integer.toHexString(EGL14.eglGetError()));}
 private static void run()throws Exception {
  AndroidPlatformManager manager=new AndroidPlatformManager(nullSurface(),path->"");
  AndroidWindow window=new AndroidWindow(manager,null,new AndroidRenderApi(path->""));
  Method attach=AndroidWindow.class.getDeclaredMethod("attach",Surface.class);attach.setAccessible(true);
  int[] texture=new int[1];GLES32.glGenTextures(1,texture,0);GLES32.glBindTexture(GLES32.GL_TEXTURE_2D,texture[0]);
  GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D,0,GLES32.GL_RGBA,1,1,0,GLES32.GL_RGBA,GLES32.GL_UNSIGNED_BYTE,java.nio.ByteBuffer.wrap(new byte[]{23,45,67,(byte)255}));
  EGLContext context=EGL14.eglGetCurrentContext();
  for(int i=0;i<24;i++){
   SurfaceTexture target=new SurfaceTexture(false);target.setDefaultBufferSize(64,64);Surface surface=new Surface(target);
   attach.invoke(window,surface);require(EGL14.eglGetCurrentContext().equals(context),"Context replaced");
   require(GLES32.glIsTexture(texture[0]),"Texture lost on attach "+i);
   GLES32.glViewport(0,0,64,64);GLES32.glClearColor(.2f,.4f,.6f,1);GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);
   require(EGL14.eglSwapBuffers(EGL14.eglGetCurrentDisplay(),EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)),"Swap failed");
   attach.invoke(window,new Object[]{null});surface.release();target.release();
   require(EGL14.eglGetCurrentContext().equals(context)&&GLES32.glIsTexture(texture[0]),"Texture/context lost while parked "+i);
   require(GLES32.glGetError()==GLES32.GL_NO_ERROR,"GL error");
  }
  GLES32.glDeleteTextures(1,texture,0);window.release();
  System.out.println("PASS: 24 native Surface detach/recreate cycles retain EGL context and uploaded texture; swaps resume without engine restart");
 }
 private static Surface nullSurface(){return new Surface(new SurfaceTexture(false));}
}
