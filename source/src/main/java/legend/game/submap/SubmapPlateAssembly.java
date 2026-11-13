package legend.game.submap;

import legend.core.gpu.Rect4i;
import legend.core.image.DecodedImage;

/** Places the collision-aligned plate inside an extended panorama, keeping all interior landmarks fixed. */
final class SubmapPlateAssembly {
  private SubmapPlateAssembly() { }

  static DecodedImage registered(final DecodedImage base, final Rect4i baseBounds,
                                final DecodedImage source, final Rect4i canvas,
                                final float pixelsPerUnit,final float originU,final float originV) {
    final float maxU=canvas.right()*pixelsPerUnit+originU,maxV=canvas.bottom()*pixelsPerUnit+originV;
    final float minU=canvas.x*pixelsPerUnit+originU,minV=canvas.y*pixelsPerUnit+originV;
    if(pixelsPerUnit<=0 || minU < -0.5f || minV < -0.5f || maxU>source.width+0.5f || maxV>source.height+0.5f)
      throw new IllegalArgumentException("Registered generated scenery does not cover canvas: "+minU+","+minV+".."+maxU+","+maxV+" source="+source.width+"x"+source.height);
    final float outputScale=base.width/(float)baseBounds.w;
    final int width=Math.round(canvas.w*outputScale),height=Math.round(canvas.h*outputScale);
    final java.nio.ByteBuffer data=legend.core.memory.BufferUtils.createByteBuffer(Math.multiplyExact(Math.multiplyExact(width,height),4));
    final DecodedImage output=new DecodedImage(data,width,height,()->{});
    for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
      final float wx=canvas.x+(x+0.5f)*canvas.w/width;
      final float wy=canvas.y+(y+0.5f)*canvas.h/height;
      final float sx=wx*pixelsPerUnit+originU-0.5f,sy=wy*pixelsPerUnit+originV-0.5f;
      final int offset=(y*width+x)*4;
      for(int c=0;c<3;c++)data.put(offset+c,(byte)Math.round(sample(source,sx,sy,c)));
      data.put(offset+3,(byte)255);
    }
    assemble(base,baseBounds,output,canvas);
    return output;
  }

  static void assemble(final DecodedImage base, final Rect4i baseBounds,
                       final DecodedImage surround, final Rect4i surroundBounds) {
    ExteriorPlateJoin.apply(base,baseBounds,surround,surroundBounds);
    for(int y=0;y<surround.height;y++) {
      final float worldY=surroundBounds.y+(y+0.5f)*surroundBounds.h/surround.height;
      if(worldY<baseBounds.y || worldY>=baseBounds.bottom()) continue;
      final float sy=(worldY-baseBounds.y)*base.height/baseBounds.h-0.5f;
      for(int x=0;x<surround.width;x++) {
        final float worldX=surroundBounds.x+(x+0.5f)*surroundBounds.w/surround.width;
        if(worldX<baseBounds.x || worldX>=baseBounds.right()) continue;
        final float sx=(worldX-baseBounds.x)*base.width/baseBounds.w-0.5f;
        final int offset=(y*surround.width+x)*4;
        for(int channel=0;channel<3;channel++) {
          final float colour=sample(base,sx,sy,channel);
          surround.pixels.put(offset+channel,(byte)Math.round(colour));
        }
        surround.pixels.put(offset+3,(byte)255);
      }
    }
  }

  private static float sample(final DecodedImage image,final float x,final float y,final int channel) {
    final int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
    final float fx=x-ix,fy=y-iy;
    final float a=pixel(image,ix,iy,channel)*(1-fx)+pixel(image,ix+1,iy,channel)*fx;
    final float b=pixel(image,ix,iy+1,channel)*(1-fx)+pixel(image,ix+1,iy+1,channel)*fx;
    return a*(1-fy)+b*fy;
  }
  private static int pixel(final DecodedImage image,int x,int y,final int channel) {
    x=Math.max(0,Math.min(image.width-1,x));y=Math.max(0,Math.min(image.height-1,y));
    return image.pixels.get((y*image.width+x)*4+channel)&255;
  }
}
