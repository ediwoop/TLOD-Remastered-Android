package legend.core.renderer.android;

import android.opengl.GLES32;
import legend.core.renderer.*;
import org.joml.*;
import java.io.IOException;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/** Loads GLES source exported at build time, without runtime shaderc or SPIR-V natives. */
public final class AndroidShader<O extends ShaderOptions> implements Shader<O> {
  @FunctionalInterface public interface Source { String read(Path path) throws IOException; }
  private final String name;
  private final LinkedHashMap<Path,Integer> stages=new LinkedHashMap<>();
  private final Source source;
  private final Function<Shader<O>,Supplier<O>> factory;
  private Supplier<O> options;
  private int program;
  public AndroidShader(String name,Path vertex,Path fragment,Source source,Function<Shader<O>,Supplier<O>> factory)throws IOException{this(name,vertex,null,fragment,source,factory);}
  public AndroidShader(String name,Path vertex,Path geometry,Path fragment,Source source,Function<Shader<O>,Supplier<O>> factory)throws IOException{
    this.name=name;this.source=source;this.factory=factory;stages.put(vertex,GLES32.GL_VERTEX_SHADER);if(geometry!=null)stages.put(geometry,GLES32.GL_GEOMETRY_SHADER);stages.put(fragment,GLES32.GL_FRAGMENT_SHADER);reload();
  }
  public void reload()throws IOException{
    List<Integer> compiled=new ArrayList<>();int next=0;
    try{
      for(var entry:stages.entrySet()){
        String glsl=source.read(entry.getKey());if(!glsl.contains("#version 320 es") && !glsl.contains("#version 310 es") && !glsl.contains("#version 300 es"))throw new IOException("Expected precompiled GLES shader: "+entry.getKey());
        int shader=GLES32.glCreateShader(entry.getValue());compiled.add(shader);GLES32.glShaderSource(shader,glsl);GLES32.glCompileShader(shader);
        int[] ok=new int[1];GLES32.glGetShaderiv(shader,GLES32.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IOException("Shader "+entry.getKey()+": "+GLES32.glGetShaderInfoLog(shader));
      }
      next=GLES32.glCreateProgram();for(int shader:compiled)GLES32.glAttachShader(next,shader);GLES32.glLinkProgram(next);int[] ok=new int[1];GLES32.glGetProgramiv(next,GLES32.GL_LINK_STATUS,ok,0);
      if(ok[0]==0)throw new IOException("Program "+name+": "+GLES32.glGetProgramInfoLog(next));
      int previous=program;program=next;next=0;if(previous!=0)GLES32.glDeleteProgram(previous);options=factory.apply(this);
    }finally{for(int shader:compiled)GLES32.glDeleteShader(shader);if(next!=0)GLES32.glDeleteProgram(next);}
  }
  private int location(String name){if(program==0)throw new IllegalStateException("Program deleted");return GLES32.glGetUniformLocation(program,name);}
  public O makeOptions(){return options.get();}
  public void use(){if(program==0)throw new IllegalStateException("Program deleted");GLES32.glUseProgram(program);}
  public void delete(){if(program!=0){GLES32.glDeleteProgram(program);program=0;}}
  public void bindUniformBlock(CharSequence name,int binding){int index=GLES32.glGetUniformBlockIndex(program,name.toString());if(index!=GLES32.GL_INVALID_INDEX)GLES32.glUniformBlockBinding(program,index,binding);}
  public ShaderUniformFloat uniformFloat(String name){int at=location(name);return v->GLES32.glUniform1f(at,v);}
  public ShaderUniformInt uniformInt(String name){int at=location(name);return v->GLES32.glUniform1i(at,v);}
  public ShaderUniformVec2 uniformVec2(String name){int at=location(name);return new ShaderUniformVec2(){public void set(FloatBuffer b){GLES32.glUniform2fv(at,b.remaining()/2,b);}public void set(Vector2fc v){set(v.x(),v.y());}public void set(float x,float y){GLES32.glUniform2f(at,x,y);}};}
  public ShaderUniformVec3 uniformVec3(String name){int at=location(name);return new ShaderUniformVec3(){public void set(FloatBuffer b){GLES32.glUniform3fv(at,b.remaining()/3,b);}public void set(Vector3fc v){set(v.x(),v.y(),v.z());}public void set(float x,float y,float z){GLES32.glUniform3f(at,x,y,z);}};}
  public ShaderUniformVec4 uniformVec4(String name){int at=location(name);return new ShaderUniformVec4(){public void set(FloatBuffer b){GLES32.glUniform4fv(at,b.remaining()/4,b);}public void set(Vector4fc v){set(v.x(),v.y(),v.z(),v.w());}public void set(float x,float y,float z,float w){GLES32.glUniform4f(at,x,y,z,w);}};}
  public ShaderUniformMat4 uniformMat4(String name){int at=location(name);return new ShaderUniformMat4(){
    private final FloatBuffer data=ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer();
    public void set(Matrix4fc m){set(m,false);}public void set(Matrix4fc m,boolean transpose){data.clear();m.get(data);set(data,transpose);}
    public void set(FloatBuffer b){set(b,false);}public void set(FloatBuffer b,boolean transpose){if(transpose)throw new IllegalArgumentException("GLES requires untransposed matrices");GLES32.glUniformMatrix4fv(at,b.remaining()/16,false,b);}
  };}
}
