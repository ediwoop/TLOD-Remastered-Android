package legend.core.renderer.android;

import android.opengl.GLES32;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import static android.opengl.GLES32.*;

/**
 * Detect a geometry-stage clip/depth mismatch without changing conforming drivers.
 * The loop and averaged input depth mirror the production TMD geometry stage.
 * EmitVertex stays in main on both paths, avoiding a helper conversion defect.
 */
final class GeometryDepthProbe {
  enum Profile { OPENGL, ZERO_TO_ONE, ZERO_TO_ONE_CLIP_OPENGL_DEPTH }
  private GeometryDepthProbe() { }
  static Profile run() {
    int[] viewport=new int[4],program=new int[1],vao=new int[1],draw=new int[1],read=new int[1],active=new int[1],texture=new int[1];
    float[] clear=new float[4];boolean[] mask=new boolean[4];
    glGetIntegerv(GL_VIEWPORT,viewport,0);glGetIntegerv(GL_CURRENT_PROGRAM,program,0);glGetIntegerv(GL_VERTEX_ARRAY_BINDING,vao,0);
    glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING,draw,0);glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING,read,0);glGetIntegerv(GL_ACTIVE_TEXTURE,active,0);
    glGetFloatv(GL_COLOR_CLEAR_VALUE,clear,0);glGetBooleanv(GL_COLOR_WRITEMASK,mask,0);
    int[] states={GL_DEPTH_TEST,GL_CULL_FACE,GL_BLEND,GL_SCISSOR_TEST,GL_RASTERIZER_DISCARD};boolean[] enabled=new boolean[states.length];
    for(int i=0;i<states.length;i++){enabled[i]=glIsEnabled(states[i]);glDisable(states[i]);}
    glActiveTexture(GL_TEXTURE0);glGetIntegerv(GL_TEXTURE_BINDING_2D,texture,0);
    int[] target=new int[1],fbo=new int[1],testVao=new int[1];int testProgram=0;List<Integer> shaders=new ArrayList<>();
    try {
      glGenTextures(1,target,0);glBindTexture(GL_TEXTURE_2D,target[0]);
      glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
      glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,4,4,0,GL_RGBA,GL_UNSIGNED_BYTE,null);
      glGenFramebuffers(1,fbo,0);glBindFramebuffer(GL_FRAMEBUFFER,fbo[0]);glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,target[0],0);
      if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Geometry depth probe framebuffer is incomplete");
      shaders.add(compile(GL_VERTEX_SHADER,"#version 320 es\nuniform float clipZ;const vec2 p[6]=vec2[6](vec2(-1,-1),vec2(0),vec2(3,-1),vec2(0),vec2(-1,3),vec2(0));void main(){gl_Position=vec4(p[gl_VertexID],clipZ,1.0);}"));
      shaders.add(compile(GL_GEOMETRY_SHADER,"#version 320 es\nprecision highp float;precision highp int;layout(triangles_adjacency) in;layout(triangle_strip,max_vertices=3) out;uniform int usePs1Depth;void main(){float depth=(gl_in[0].gl_Position.z+gl_in[2].gl_Position.z+gl_in[4].gl_Position.z)/3.0;for(int vertex=0;vertex<3;vertex++){int i=vertex*2;gl_Position=gl_in[i].gl_Position;if(usePs1Depth!=0){gl_Position.z=depth;}EmitVertex();}EndPrimitive();}"));
      shaders.add(compile(GL_FRAGMENT_SHADER,"#version 320 es\nprecision highp float;out vec4 colour;void main(){gl_FragDepth=gl_FragCoord.z;colour=vec4(gl_FragCoord.z,1.0,0.0,1.0);}"));
      testProgram=glCreateProgram();for(int shader:shaders)glAttachShader(testProgram,shader);glLinkProgram(testProgram);int[] linked=new int[1];glGetProgramiv(testProgram,GL_LINK_STATUS,linked,0);
      if(linked[0]==0)throw new IllegalStateException("Geometry depth probe link failed: "+glGetProgramInfoLog(testProgram));
      glGenVertexArrays(1,testVao,0);glBindVertexArray(testVao[0]);glUseProgram(testProgram);glViewport(0,0,4,4);glColorMask(true,true,true,true);
      int at=glGetUniformLocation(testProgram,"clipZ");int[] positive=sample(at,.5f),negative=sample(at,-.5f);
      int error=glGetError();if(error!=GL_NO_ERROR)throw new IllegalStateException("Geometry depth probe GL error: 0x"+Integer.toHexString(error));
      Profile profile;
      if(positive[1]<200)throw new IllegalStateException("Geometry depth probe could not draw the positive-depth triangle");
      else if(negative[1]>200)profile=Profile.OPENGL;
      else if(Math.abs(positive[0]-128)<12)profile=Profile.ZERO_TO_ONE;
      else if(Math.abs(positive[0]-191)<12)profile=Profile.ZERO_TO_ONE_CLIP_OPENGL_DEPTH;
      else throw new IllegalStateException("Unexpected geometry depth response: "+positive[0]+","+negative[0]);
      android.util.Log.i("DragoonGL","Geometry depth profile="+profile+" positive="+positive[0]+","+positive[1]+" negative="+negative[0]+","+negative[1]);
      return profile;
    } finally {
      glUseProgram(program[0]);glBindVertexArray(vao[0]);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw[0]);glBindFramebuffer(GL_READ_FRAMEBUFFER,read[0]);
      glBindTexture(GL_TEXTURE_2D,texture[0]);glActiveTexture(active[0]);glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
      glClearColor(clear[0],clear[1],clear[2],clear[3]);glColorMask(mask[0],mask[1],mask[2],mask[3]);
      for(int i=0;i<states.length;i++){if(enabled[i])glEnable(states[i]);else glDisable(states[i]);}
      if(testVao[0]!=0)glDeleteVertexArrays(1,testVao,0);if(fbo[0]!=0)glDeleteFramebuffers(1,fbo,0);if(target[0]!=0)glDeleteTextures(1,target,0);
      if(testProgram!=0)glDeleteProgram(testProgram);for(int shader:shaders)glDeleteShader(shader);
    }
  }
  private static int compile(int type,String source){int shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader);int[] ok=new int[1];glGetShaderiv(shader,GL_COMPILE_STATUS,ok,0);if(ok[0]==0){String log=glGetShaderInfoLog(shader);glDeleteShader(shader);throw new IllegalStateException("Geometry depth probe compilation failed: "+log);}return shader;}
  private static int[] sample(int location,float z){glClearColor(0,0,0,1);glClear(GL_COLOR_BUFFER_BIT);glUniform1f(location,z);glDrawArrays(GL_TRIANGLES_ADJACENCY,0,6);ByteBuffer pixel=ByteBuffer.allocateDirect(4);glReadPixels(2,2,1,1,GL_RGBA,GL_UNSIGNED_BYTE,pixel);return new int[]{pixel.get(0)&255,pixel.get(1)&255};}
}
