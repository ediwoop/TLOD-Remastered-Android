package com.realbox.dragoon;

import android.opengl.*;
import legend.core.image.*;
import legend.core.renderer.*;
import legend.core.renderer.android.*;
import legend.game.textures.PngWriter;
import org.joml.Matrix4f;
import java.nio.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.io.*;

/** Internal offscreen Android graphics integration test, not gameplay. */
public final class GraphicsCheck {
 private static void require(boolean value,String why){if(!value)throw new AssertionError(why+" EGL="+Integer.toHexString(EGL14.eglGetError()));}
 public static void main(String[] args){try{check(args);System.exit(0);}catch(Throwable failure){failure.printStackTrace(System.err);System.exit(1);}}
 private static void check(String[] args)throws Exception{
  byte[] expected={(byte)255,0,0,(byte)255,0,0,(byte)255,(byte)128};byte[] png=PngWriter.compress(ByteBuffer.wrap(expected),2,1);
  try(DecodedImage image=new AndroidImageDecoder().decode(ByteBuffer.wrap(png))){byte[] actual=new byte[8];image.pixels.get(actual);require(Arrays.equals(expected,actual),"Android RGBA decoding mismatch");}
  System.out.println("PASS: actual Android PNG encode/decode preserves RGBA including alpha");
  EGLDisplay d=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);require(d!=EGL14.EGL_NO_DISPLAY,"Display");int[] version=new int[2];require(EGL14.eglInitialize(d,version,0,version,1),"EGL init");
  EGLContext context=EGL14.EGL_NO_CONTEXT;EGLSurface surface=EGL14.EGL_NO_SURFACE;
  try{
   EGLConfig[] configs=new EGLConfig[1];int[] count=new int[1];int[] attrs={EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_RENDERABLE_TYPE,0x40,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_NONE};
   require(EGL14.eglChooseConfig(d,attrs,0,configs,0,1,count,0)&&count[0]>0,"Config");
   context=EGL14.eglCreateContext(d,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL15.EGL_CONTEXT_MINOR_VERSION,2,EGL14.EGL_NONE},0);
   if(context==EGL14.EGL_NO_CONTEXT){EGL14.eglGetError();context=EGL14.eglCreateContext(d,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL14.EGL_NONE},0);}require(context!=EGL14.EGL_NO_CONTEXT,"Context");
   surface=EGL14.eglCreatePbufferSurface(d,configs[0],new int[]{EGL14.EGL_WIDTH,16,EGL14.EGL_HEIGHT,16,EGL14.EGL_NONE},0);require(surface!=EGL14.EGL_NO_SURFACE,"Pbuffer");require(EGL14.eglMakeCurrent(d,surface,surface,context),"Current context");
   System.out.println("ANDROID_GL_VERSION: "+GLES32.glGetString(GLES32.GL_VERSION));System.out.println("ANDROID_GL_RENDERER: "+GLES32.glGetString(GLES32.GL_RENDERER));
   int[] minor=new int[1];GLES32.glGetIntegerv(GLES32.GL_MINOR_VERSION,minor,0);boolean fullShaderSupport=minor[0]>=2;
   if(!fullShaderSupport)System.out.println("LIMITATION: component checks with ES300 shader only; full engine still requires GLES 3.2");
   try(ZipFile zip=new ZipFile(args[0])){
    AndroidShader.Source sources=path->{
     if(!fullShaderSupport){if(path.getFileName().toString().equals("copy.vsh"))return "#version 300 es\nuniform mat4 projection; layout(location=0) in vec2 inPos; layout(location=1) in vec2 inUv; out vec2 uv; void main(){gl_Position=projection*vec4(inPos,1.0,1.0);uv=inUv;}";
      if(path.getFileName().toString().equals("copy.fsh"))return "#version 300 es\nprecision highp float; uniform sampler2D tex; in vec2 uv; out vec4 colour; void main(){colour=texture(tex,uv);colour.a=1.0;}";
     }
     ZipEntry e=zip.getEntry("assets/android-shaders/"+path.getFileName());if(e==null)throw new IOException("Missing shader "+path);try(InputStream in=zip.getInputStream(e)){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}};
    AndroidShader<ShaderOptions> shader=new AndroidShader<>("engine copy",Path.of("copy.vsh"),Path.of("copy.fsh"),sources,s->()->()->{});
    AndroidTexture texture=new AndroidTexture(ByteBuffer.allocateDirect(4).put(new byte[]{(byte)255,0,0,(byte)255}).flip(),"test red",1,1,TextureInternalFormat.RGBA_8,TextureDataFormat.RGBA,TextureDataType.UBYTE,false,false,false,false);
    AndroidMesh mesh=new AndroidMesh("test triangle",VertexOrder.TRIANGLES,new float[]{-1,-1,0,0,3,-1,0,0,-1,3,0,0},3,true,false,null,BufferUsage.STATIC);
    try{
     mesh.attribute(0,0,2,4);mesh.attribute(1,2,2,4);shader.use();shader.uniformInt("tex").set(0);shader.uniformMat4("projection").set(new Matrix4f());texture.use();GLES32.glViewport(0,0,16,16);GLES32.glClearColor(0,0,1,1);GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);mesh.draw();
     ByteBuffer pixel=ByteBuffer.allocateDirect(4);GLES32.glReadPixels(8,8,1,1,GLES32.GL_RGBA,GLES32.GL_UNSIGNED_BYTE,pixel);require((pixel.get(0)&255)>240&&(pixel.get(1)&255)<10&&(pixel.get(2)&255)<10,"Engine mesh/shader/texture readback mismatch");require(GLES32.glGetError()==GLES32.GL_NO_ERROR,"GLES error");System.out.println("PASS: Android backend mesh + shader adapter + texture render red pixel");
    }finally{mesh.delete();texture.delete();Texture.deleteTextures();shader.delete();}
    AndroidTexture target=new AndroidTexture(null,"offscreen target",4,4,TextureInternalFormat.RGBA_8,TextureDataFormat.RGBA,TextureDataType.UBYTE,false,false,false,false);
    AndroidFrameBuffer framebuffer=new AndroidFrameBuffer("offscreen test",new FrameBufferAttachment[]{new FrameBufferAttachment(FrameBufferAttachmentType.COLOUR,target)});
    try{framebuffer.bind();GLES32.glViewport(0,0,4,4);GLES32.glClearColor(0,1,0,1);GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT);ByteBuffer pixel=ByteBuffer.allocateDirect(4);GLES32.glReadPixels(1,1,1,1,GLES32.GL_RGBA,GLES32.GL_UNSIGNED_BYTE,pixel);require((pixel.get(1)&255)>240&&(pixel.get(0)&255)<10,"Framebuffer readback");System.out.println("PASS: Android backend framebuffer renders and reads green pixel");}
    finally{GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER,0);framebuffer.delete();target.delete();Texture.deleteTextures();}
    AndroidUniformBuffer uniform=new AndroidUniformBuffer(16,3);
    try{FloatBuffer values=ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asFloatBuffer();values.put(new float[]{1f,2f,3f,4f}).flip();uniform.set(values);int[] id=new int[1];GLES32.glGetIntegeri_v(GLES32.GL_UNIFORM_BUFFER_BINDING,3,id,0);GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER,id[0]);Buffer mapped=GLES32.glMapBufferRange(GLES32.GL_UNIFORM_BUFFER,0,16,GLES32.GL_MAP_READ_BIT);require(mapped instanceof ByteBuffer,"Uniform map");ByteBuffer bytes=((ByteBuffer)mapped).order(ByteOrder.nativeOrder());for(int i=0;i<4;i++)require(bytes.getFloat(i*4)==i+1f,"Uniform upload mismatch");require(GLES32.glUnmapBuffer(GLES32.GL_UNIFORM_BUFFER),"Uniform unmap");GLES32.glBindBuffer(GLES32.GL_UNIFORM_BUFFER,0);System.out.println("PASS: Android backend uniform buffer preserves four float values in GPU memory");}finally{uniform.delete();}
    if(fullShaderSupport)for(String[] program:new String[][]{{"simple","simple","simple"},{"standard","standard","standard"},{"tmd","tmd","tmd","tmd"},{"battle","battle_tmd","tmd","battle_tmd"},{"screen","post","screen"}}){
     AndroidShader<ShaderOptions> compiled=program.length==3?new AndroidShader<>(program[0],Path.of(program[1]+".vsh"),Path.of(program[2]+".fsh"),sources,s->()->()->{}):new AndroidShader<>(program[0],Path.of(program[1]+".vsh"),Path.of(program[2]+".gsh"),Path.of(program[3]+".fsh"),sources,s->()->()->{});compiled.delete();System.out.println("PASS: Android linked program "+program[0]);
    }
   }
  }finally{EGL14.eglMakeCurrent(d,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);if(surface!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(d,surface);if(context!=EGL14.EGL_NO_CONTEXT)EGL14.eglDestroyContext(d,context);EGL14.eglTerminate(d);}
 }
}
