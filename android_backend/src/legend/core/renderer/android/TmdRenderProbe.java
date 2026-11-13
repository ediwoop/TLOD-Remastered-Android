package legend.core.renderer.android;

import java.nio.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import legend.core.renderer.*;
import static android.opengl.GLES32.*;

/** One startup check of the actual mobile TMD factory, mesh and texture path. */
final class TmdRenderProbe {
  private TmdRenderProbe() { }
  private static FloatBuffer floats(int size) { return ByteBuffer.allocateDirect(size*4).order(ByteOrder.nativeOrder()).asFloatBuffer(); }
  private static void identity(FloatBuffer data,int at) { for(int i=0;i<4;i++)data.put(at+i*5,1); }
  private static void require(boolean value,String message) { if(!value)throw new IllegalStateException(message); }

  static String run(AndroidRenderApi api) {
    int[] viewport=new int[4],program=new int[1],vao=new int[1],array=new int[1],uniform=new int[1],draw=new int[1],read=new int[1],renderbuffer=new int[1],active=new int[1],textures=new int[2],bindings=new int[6];
    int[] depthFunc=new int[1],frontFace=new int[1],cullFace=new int[1];
    float[] clear=new float[4],clearDepth=new float[1];boolean[] colourMask=new boolean[4],depthMask=new boolean[1];
    glGetIntegerv(GL_VIEWPORT,viewport,0);glGetIntegerv(GL_CURRENT_PROGRAM,program,0);glGetIntegerv(GL_VERTEX_ARRAY_BINDING,vao,0);glGetIntegerv(GL_ARRAY_BUFFER_BINDING,array,0);glGetIntegerv(GL_UNIFORM_BUFFER_BINDING,uniform,0);
    glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING,draw,0);glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING,read,0);glGetIntegerv(GL_RENDERBUFFER_BINDING,renderbuffer,0);glGetIntegerv(GL_ACTIVE_TEXTURE,active,0);
    for(int i=0;i<2;i++){glActiveTexture(GL_TEXTURE0+i);int[] t=new int[1];glGetIntegerv(GL_TEXTURE_BINDING_2D,t,0);textures[i]=t[0];}
    for(int i=0;i<bindings.length;i++){int[] b=new int[1];glGetIntegeri_v(GL_UNIFORM_BUFFER_BINDING,i,b,0);bindings[i]=b[0];}
    glGetIntegerv(GL_DEPTH_FUNC,depthFunc,0);glGetIntegerv(GL_FRONT_FACE,frontFace,0);glGetIntegerv(GL_CULL_FACE_MODE,cullFace,0);
    glGetFloatv(GL_COLOR_CLEAR_VALUE,clear,0);glGetFloatv(GL_DEPTH_CLEAR_VALUE,clearDepth,0);glGetBooleanv(GL_COLOR_WRITEMASK,colourMask,0);glGetBooleanv(GL_DEPTH_WRITEMASK,depthMask,0);
    int[] states={GL_DEPTH_TEST,GL_CULL_FACE,GL_BLEND,GL_SCISSOR_TEST,GL_STENCIL_TEST,GL_RASTERIZER_DISCARD};boolean[] enabled=new boolean[states.length];
    for(int i=0;i<states.length;i++){enabled[i]=glIsEnabled(states[i]);glDisable(states[i]);}
    int[] targets=new int[3],fbo=new int[1],depth=new int[1];
    Shader<ShaderOptions> shader=null;Mesh mesh=null,quad=null;List<AndroidUniformBuffer> buffers=new ArrayList<>();
    try {
      int inherited=glGetError();if(inherited!=GL_NO_ERROR)android.util.Log.w("DragoonGL","GL error before TMD probe: 0x"+Integer.toHexString(inherited));
      glGenTextures(3,targets,0);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,targets[0]);textureParameters();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,16,16,0,GL_RGBA,GL_UNSIGNED_BYTE,null);
      glGenFramebuffers(1,fbo,0);glBindFramebuffer(GL_FRAMEBUFFER,fbo[0]);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,targets[0],0);
      glGenRenderbuffers(1,depth,0);glBindRenderbuffer(GL_RENDERBUFFER,depth[0]);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT24,16,16);glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depth[0]);
      require(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE,"TMD test framebuffer");
      glBindTexture(GL_TEXTURE_2D,targets[1]);textureParameters();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,1,1,0,GL_RGBA,GL_UNSIGNED_BYTE,ByteBuffer.allocateDirect(4).put(new byte[]{-1,-1,-1,-1}).flip());
      glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,targets[2]);textureParameters();
      IntBuffer pixels=ByteBuffer.allocateDirect(32*4).order(ByteOrder.nativeOrder()).asIntBuffer();pixels.put(0,0x7c00);glTexImage2D(GL_TEXTURE_2D,0,GL_R32UI,16,2,0,GL_RED_INTEGER,GL_UNSIGNED_INT,pixels);
      shader=api.makeShader("TMD startup probe",Path.of("tmd.vsh"),Path.of("tmd.gsh"),Path.of("tmd.fsh"),s->()->()->{});
      String[] names={"transforms","transforms2","lighting","projectionInfo","scissor","clutAnimation"};int[] sizes={32,128*20,128*32,4,4,1024*4};
      FloatBuffer[] values=new FloatBuffer[names.length];
      for(int i=0;i<names.length;i++){
        FloatBuffer v=floats(sizes[i]);values[i]=v;
        if(i==0){identity(v,0);orthographic(v);}
        if(i==1)identity(v,127*20);
        if(i==2){identity(v,127*32);v.put(127*32+16,1);v.put(127*32+17,1);v.put(127*32+18,1);}
        if(i==3)v.put(new float[]{0,320,1.0f/320,1}).rewind();
        if(i==4)v.put(new float[]{0,0,16,16}).rewind();
        if(i==5)v.put(0,-1);
        AndroidUniformBuffer buffer=new AndroidUniformBuffer(sizes[i]*4L,i);buffer.set(v);buffers.add(buffer);shader.bindUniformBlock(names[i],i);
      }
      shader.use();shader.uniformInt("tex24").set(0);shader.uniformInt("tex15").set(1);shader.uniformFloat("modelIndex").set(127);shader.uniformVec3("recolour").set(1,1,1);shader.uniformFloat("discardTranslucency").set(1);shader.uniformInt("usePs1Depth").set(1);
      float[] vertices=new float[3*16];float[][] points={{-2048,-2048},{2048,-2048},{0,2048}};
      for(int i=0;i<3;i++){int at=i*16;vertices[at]=points[i][0];vertices[at+1]=points[i][1];vertices[at+2]=4096;vertices[at+3]=i;vertices[at+4]=1;vertices[at+11]=1;vertices[at+14]=1;vertices[at+15]=5;}
      mesh=api.makeMesh("TMD startup triangle",VertexOrder.TRIANGLES_ADJACENCY,vertices,new int[]{2,3,1,3,0,3},true,false,null,BufferUsage.DYNAMIC);attributes(mesh);
      glViewport(0,0,16,16);glColorMask(true,true,true,true);glDepthMask(true);glClearDepthf(1);glDepthFunc(GL_LEQUAL);glEnable(GL_DEPTH_TEST);glEnable(GL_CULL_FACE);glCullFace(GL_BACK);glFrontFace(GL_CCW);
      draw(mesh);pixel(0,"lit PS1 perspective/modelIndex127");
      for(int i=0;i<3;i++){int at=i*16;vertices[at+9]=256;vertices[at+11]=vertices[at+12]=vertices[at+13]=1;vertices[at+15]=3;}mesh.update();
      draw(mesh);pixel(2,"lit 15-bit VRAM");
      pixels.clear();for(int i=0;i<32;i++)pixels.put(i,0);pixels.put(0,1);pixels.put(17,0x3e0);glTexSubImage2D(GL_TEXTURE_2D,0,0,0,16,2,GL_RED_INTEGER,GL_UNSIGNED_INT,pixels);
      for(int i=0;i<3;i++){vertices[i*16+9]=0;vertices[i*16+10]=64;}mesh.update();draw(mesh);pixel(1,"lit 4-bit CLUT");
      vertices[2]=100;mesh.update();draw(mesh);black("near triangle must be fully culled");vertices[2]=4096;mesh.update();
      float[] q=new float[4*16];System.arraycopy(vertices,0,q,0,vertices.length);System.arraycopy(vertices,0,q,3*16,16);q[3*16+2]=100;for(int i=0;i<4;i++)q[i*16+15]=19;
      quad=api.makeMesh("TMD startup quad",VertexOrder.TRIANGLES_ADJACENCY,q,new int[]{2,3,1,3,0,3,1,0,2,0,3,0},true,false,null,BufferUsage.STATIC);attributes(quad);draw(quad);black("fourth quad vertex must cull both triangles");
      // Exercise the fragment-depth path used by the perspective renderer.
      FloatBuffer transform=values[0];for(int i=16;i<32;i++)transform.put(i,0);transform.put(16,1.7f);transform.put(21,-1.7f);transform.put(26,1.0000002f);transform.put(27,1);transform.put(30,-.20000002f);buffers.get(0).set(transform);
      values[3].put(2,.000001f);values[3].put(3,2);buffers.get(3).set(values[3]);draw(mesh);pixel(1,"perspective fragment depth");
      require(glGetError()==GL_NO_ERROR,"GL error after TMD checks");
      return "PASS: iluminacion, perspectiva PS1, VRAM15, CLUT4, recorte de triangulo/cuadrilatero, profundidad y modelo127";
    } catch(Exception failure) {
      return "FAIL: "+failure.getMessage()+"; GL=0x"+Integer.toHexString(glGetError());
    } finally {
      if(mesh!=null)mesh.delete();if(quad!=null)quad.delete();if(shader!=null)shader.delete();for(AndroidUniformBuffer buffer:buffers)buffer.delete();
      if(depth[0]!=0)glDeleteRenderbuffers(1,depth,0);if(fbo[0]!=0)glDeleteFramebuffers(1,fbo,0);glDeleteTextures(3,targets,0);
      glUseProgram(program[0]);glBindVertexArray(vao[0]);glBindBuffer(GL_ARRAY_BUFFER,array[0]);for(int i=0;i<bindings.length;i++)glBindBufferBase(GL_UNIFORM_BUFFER,i,bindings[i]);glBindBuffer(GL_UNIFORM_BUFFER,uniform[0]);
      glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw[0]);glBindFramebuffer(GL_READ_FRAMEBUFFER,read[0]);glBindRenderbuffer(GL_RENDERBUFFER,renderbuffer[0]);for(int i=0;i<2;i++){glActiveTexture(GL_TEXTURE0+i);glBindTexture(GL_TEXTURE_2D,textures[i]);}glActiveTexture(active[0]);
      glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);glClearColor(clear[0],clear[1],clear[2],clear[3]);glClearDepthf(clearDepth[0]);glColorMask(colourMask[0],colourMask[1],colourMask[2],colourMask[3]);glDepthMask(depthMask[0]);glDepthFunc(depthFunc[0]);glFrontFace(frontFace[0]);glCullFace(cullFace[0]);
      for(int i=0;i<states.length;i++){if(enabled[i])glEnable(states[i]);else glDisable(states[i]);}
    }
  }
  private static void orthographic(FloatBuffer v){v.put(16,1.0f/160);v.put(21,-1.0f/120);v.put(26,.000002f);v.put(30,-1);v.put(31,1);}
  private static void attributes(Mesh m){m.attribute(0,0,4,16);m.attribute(1,4,3,16);m.attribute(2,7,2,16);m.attribute(3,9,1,16);m.attribute(4,10,1,16);m.attribute(5,11,4,16);m.attribute(6,15,1,16);}
  private static void textureParameters(){glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);}
  private static void draw(Mesh mesh){glClearColor(0,0,0,1);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);mesh.draw();}
  private static int[] sample(){ByteBuffer p=ByteBuffer.allocateDirect(4);glReadPixels(8,8,1,1,GL_RGBA,GL_UNSIGNED_BYTE,p);return new int[]{p.get(0)&255,p.get(1)&255,p.get(2)&255};}
  private static void pixel(int channel,String name){int[] p=sample();require(p[channel]>240,name+" pixel="+java.util.Arrays.toString(p));}
  private static void black(String name){int[] p=sample();require(p[0]==0&&p[1]==0&&p[2]==0,name+" pixel="+java.util.Arrays.toString(p));}
}
