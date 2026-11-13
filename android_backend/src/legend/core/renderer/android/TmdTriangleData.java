package legend.core.renderer.android;

/**
 * Expand the TMD adjacency stream to independent triangles. The four original
 * face positions travel alongside each vertex for PS1 face clipping/depth.
 * There is no dependence on a geometry shader, including for split quads.
 */
final class TmdTriangleData {
  final float[] vertices;
  final float[] faces;
  final int vertexCount;
  private final float[] source;
  private final int[] indices;
  private final int count, stride, positionOffset, flagsOffset;

  TmdTriangleData(float[] source, int[] indices, int count, int stride, int positionOffset, int flagsOffset) {
    if(count < 0 || count % 6 != 0 || stride < 3 || positionOffset < 0 || positionOffset + 3 > stride || flagsOffset < 0 || flagsOffset >= stride || source.length % stride != 0) {
      throw new IllegalArgumentException("Invalid TMD adjacency layout");
    }
    if(indices != null && count > indices.length || indices == null && count > source.length / stride) {
      throw new IllegalArgumentException("TMD adjacency count exceeds its data");
    }
    this.source=source;this.indices=indices;this.count=count;this.stride=stride;
    this.positionOffset=positionOffset;this.flagsOffset=flagsOffset;
    this.vertexCount=count / 2;
    this.vertices=new float[Math.multiplyExact(this.vertexCount,stride)];
    this.faces=new float[Math.multiplyExact(this.vertexCount,16)];
    refresh();
  }

  private int vertex(int element) {
    int index=this.indices == null ? element : this.indices[element];
    if(index < 0 || index >= this.source.length / this.stride) {
      throw new IllegalArgumentException("TMD face references an invalid vertex: " + index);
    }
    return index;
  }

  void refresh() {
    for(int element=0;element<this.count;element+=6) {
      int a=vertex(element),b=vertex(element+2),c=vertex(element+4);
      boolean quad=((int)this.source[a*this.stride+this.flagsOffset]&16)!=0;
      // Triangle adjacency is intentionally unused. Its final index can be
      // one past the vertex array in the retail loader; never dereference it.
      int d=quad ? vertex(element+1) : a;
      int output=element / 2;
      for(int i=0;i<3;i++) {
        System.arraycopy(this.source,(i==0?a:i==1?b:c)*this.stride,this.vertices,(output+i)*this.stride,this.stride);
        int base=(output+i)*16;
        for(int j=0;j<4;j++) {
          System.arraycopy(this.source,(j==0?a:j==1?b:j==2?c:d)*this.stride+this.positionOffset,this.faces,base+j*4,3);
          this.faces[base+j*4+3]=j==0 && quad ? 1.0f : 0.0f;
        }
      }
    }
  }
}
