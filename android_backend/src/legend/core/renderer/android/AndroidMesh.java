package legend.core.renderer.android;

import android.opengl.GLES32;
import legend.core.renderer.*;
import java.nio.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** GLES mesh. Android TMDs can use independent triangles plus per-face data. */
public final class AndroidMesh implements Mesh {
  private record Attribute(int offset,int size,int stride) { }
  private final float[] vertices;
  private final int[] indices;
  private FloatBuffer upload,faceUpload;
  private final int vao,vbo,ebo,count,mode,glUsage;
  private int faceVbo;
  private final boolean indexed,portableTmd,textured,translucent;
  private final Translucency translucency;
  private final Map<Integer,Attribute> attributes=new LinkedHashMap<>();
  private TmdTriangleData triangleData;
  private boolean deleted,trianglesDirty=true,triangleBuffersAllocated;

  public AndroidMesh(String name,VertexOrder order,float[] vertices,int[] indices,boolean textured,boolean translucent,Translucency translucency,BufferUsage usage) {
    this(order,vertices,indices,indices.length,textured,translucent,translucency,usage,false);
  }
  public AndroidMesh(String name,VertexOrder order,float[] vertices,int vertexCount,boolean textured,boolean translucent,Translucency translucency,BufferUsage usage) {
    this(order,vertices,null,vertexCount,textured,translucent,translucency,usage,false);
  }
  AndroidMesh(String name,VertexOrder order,float[] vertices,int[] indices,boolean textured,boolean translucent,Translucency translucency,BufferUsage usage,boolean portableTmd) {
    this(order,vertices,indices,indices.length,textured,translucent,translucency,usage,portableTmd);
  }
  AndroidMesh(String name,VertexOrder order,float[] vertices,int vertexCount,boolean textured,boolean translucent,Translucency translucency,BufferUsage usage,boolean portableTmd) {
    this(order,vertices,null,vertexCount,textured,translucent,translucency,usage,portableTmd);
  }
  private AndroidMesh(VertexOrder order,float[] vertices,int[] indices,int count,boolean textured,boolean translucent,Translucency translucency,BufferUsage usage,boolean portableTmd) {
    if(count<0)throw new IllegalArgumentException("Negative vertex count");
    this.vertices=vertices;this.indices=indices;this.count=count;this.indexed=indices!=null;
    this.portableTmd=portableTmd&&order==VertexOrder.TRIANGLES_ADJACENCY;
    this.textured=textured;this.translucent=translucent;this.translucency=translucency;
    mode=switch(order){case TRIANGLES->GLES32.GL_TRIANGLES;case TRIANGLES_ADJACENCY->GLES32.GL_TRIANGLES_ADJACENCY;case TRIANGLE_STRIP->GLES32.GL_TRIANGLE_STRIP;case LINES->GLES32.GL_LINES;case LINE_LOOP->GLES32.GL_LINE_LOOP;case LINE_STRIP->GLES32.GL_LINE_STRIP;};
    glUsage=switch(usage){case STREAMING->GLES32.GL_STREAM_DRAW;case STATIC->GLES32.GL_STATIC_DRAW;case DYNAMIC->GLES32.GL_DYNAMIC_DRAW;};
    int[] ids=new int[1];GLES32.glGenVertexArrays(1,ids,0);vao=ids[0];GLES32.glBindVertexArray(vao);
    GLES32.glGenBuffers(1,ids,0);vbo=ids[0];GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER,vbo);
    if(!this.portableTmd){upload=floats(vertices.length);upload.put(vertices).flip();GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,vertices.length*Float.BYTES,upload,glUsage);}
    if(indexed&&!this.portableTmd){GLES32.glGenBuffers(1,ids,0);ebo=ids[0];GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER,ebo);
      IntBuffer buffer=ByteBuffer.allocateDirect(Math.multiplyExact(indices.length,Integer.BYTES)).order(ByteOrder.nativeOrder()).asIntBuffer();buffer.put(indices).flip();
      GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER,indices.length*Integer.BYTES,buffer,glUsage);
    }else ebo=0;
    GLES32.glBindVertexArray(0);
  }
  private static FloatBuffer floats(int count){return ByteBuffer.allocateDirect(Math.multiplyExact(count,Float.BYTES)).order(ByteOrder.nativeOrder()).asFloatBuffer();}
  public void update(){
    if(deleted)return;
    if(portableTmd){trianglesDirty=true;return;}
    upload.clear();upload.put(vertices).flip();GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER,vbo);GLES32.glBufferSubData(GLES32.GL_ARRAY_BUFFER,0,vertices.length*Float.BYTES,upload);
  }
  public void attribute(int index,long offset,int size,int stride){
    if(deleted)return;
    if(portableTmd){if(index<0||index>6)throw new IllegalArgumentException("TMD attributes 7-10 are reserved for face positions");attributes.put(index,new Attribute(Math.toIntExact(offset),size,stride));triangleData=null;triangleBuffersAllocated=false;trianglesDirty=true;return;}
    GLES32.glBindVertexArray(vao);GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER,vbo);GLES32.glVertexAttribPointer(index,size,GLES32.GL_FLOAT,false,Math.multiplyExact(stride,Float.BYTES),Math.toIntExact(Math.multiplyExact(offset,Float.BYTES)));GLES32.glEnableVertexAttribArray(index);GLES32.glBindVertexArray(0);
  }
  private void prepareTriangles(){
    if(!trianglesDirty)return;
    Attribute position=attributes.get(0),flags=attributes.get(6);
    if(position==null||flags==null||position.size<3||flags.size!=1||position.stride!=flags.stride)throw new IllegalStateException("TMD position/flags layout is missing");
    for(Attribute a:attributes.values())if(a.stride!=position.stride||a.offset<0||a.offset+a.size>a.stride)throw new IllegalArgumentException("Inconsistent TMD vertex layout");
    if(triangleData==null){triangleData=new TmdTriangleData(vertices,indices,count,position.stride,position.offset,flags.offset);upload=floats(triangleData.vertices.length);faceUpload=floats(triangleData.faces.length);}
    else triangleData.refresh();
    upload.clear();upload.put(triangleData.vertices).flip();faceUpload.clear();faceUpload.put(triangleData.faces).flip();
    GLES32.glBindVertexArray(vao);GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER,vbo);
    if(!triangleBuffersAllocated)GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,triangleData.vertices.length*Float.BYTES,upload,glUsage);
    else GLES32.glBufferSubData(GLES32.GL_ARRAY_BUFFER,0,triangleData.vertices.length*Float.BYTES,upload);
    for(var entry:attributes.entrySet()){Attribute a=entry.getValue();GLES32.glVertexAttribPointer(entry.getKey(),a.size,GLES32.GL_FLOAT,false,a.stride*Float.BYTES,a.offset*Float.BYTES);GLES32.glEnableVertexAttribArray(entry.getKey());}
    if(faceVbo==0){int[] ids=new int[1];GLES32.glGenBuffers(1,ids,0);faceVbo=ids[0];}
    GLES32.glBindBuffer(GLES32.GL_ARRAY_BUFFER,faceVbo);
    if(!triangleBuffersAllocated)GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER,triangleData.faces.length*Float.BYTES,faceUpload,glUsage);
    else GLES32.glBufferSubData(GLES32.GL_ARRAY_BUFFER,0,triangleData.faces.length*Float.BYTES,faceUpload);
    for(int i=0;i<4;i++){GLES32.glVertexAttribPointer(7+i,4,GLES32.GL_FLOAT,false,16*Float.BYTES,i*4*Float.BYTES);GLES32.glEnableVertexAttribArray(7+i);}
    GLES32.glBindVertexArray(0);triangleBuffersAllocated=true;trianglesDirty=false;
  }
  public void draw(){
    if(deleted)return;
    if(portableTmd){prepareTriangles();GLES32.glBindVertexArray(vao);GLES32.glDrawArrays(GLES32.GL_TRIANGLES,0,triangleData.vertexCount);return;}
    GLES32.glBindVertexArray(vao);if(indexed)GLES32.glDrawElements(mode,count,GLES32.GL_UNSIGNED_INT,0);else GLES32.glDrawArrays(mode,0,count);
  }
  public void draw(int start,int count){
    if(deleted)return;if(count==0){draw();return;}
    if(portableTmd){if(start<0||count<0||start%6!=0||count%6!=0||start>this.count||count>this.count-start)throw new IllegalArgumentException("TMD range must contain whole adjacency faces");prepareTriangles();GLES32.glBindVertexArray(vao);GLES32.glDrawArrays(GLES32.GL_TRIANGLES,start/2,count/2);return;}
    GLES32.glBindVertexArray(vao);if(indexed)GLES32.glDrawRangeElements(mode,start,start+count-1,count,GLES32.GL_UNSIGNED_INT,0);else GLES32.glDrawArrays(mode,start,count);
  }
  public void delete(){if(deleted)return;deleted=true;if(ebo!=0)GLES32.glDeleteBuffers(1,new int[]{ebo},0);if(faceVbo!=0)GLES32.glDeleteBuffers(1,new int[]{faceVbo},0);GLES32.glDeleteBuffers(1,new int[]{vbo},0);GLES32.glDeleteVertexArrays(1,new int[]{vao},0);}
  public float[] vertices(){return vertices;}
  public boolean textured(){return textured;}
  public boolean translucent(){return translucent;}
  public Translucency translucencyMode(){return translucency;}
}
