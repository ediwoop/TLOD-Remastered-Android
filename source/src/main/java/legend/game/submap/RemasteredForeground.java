package legend.game.submap;

import legend.core.gpu.Rect4i;
import legend.core.image.DecodedImage;
import java.nio.ByteBuffer;

/** Samples the new plate through the original cutout's coverage, without its old colours. */
final class RemasteredForeground {
  private RemasteredForeground() { }

  /** A texel-aligned crop of the full plate; fractional scene bounds preserve the original pixel grid. */
  record Layer(ByteBuffer pixels,int width,int height,int pixelX,int pixelY,float x,float y,float w,float h) { }

  static Layer compact(final DecodedImage plate,final Rect4i bounds,final Rect4i maskBounds,final int[] mask) {
    if(mask.length!=maskBounds.w*maskBounds.h)throw new IllegalArgumentException("Cutout mask size");
    // Include a native-unit halo for the bilinear silhouette and transparent filtering border.
    final int left=Math.max(0,(int)Math.floor((maskBounds.x-1.0f-bounds.x)*plate.width/bounds.w));
    final int top=Math.max(0,(int)Math.floor((maskBounds.y-1.0f-bounds.y)*plate.height/bounds.h));
    final int right=Math.min(plate.width,(int)Math.ceil((maskBounds.right()+1.0f-bounds.x)*plate.width/bounds.w));
    final int bottom=Math.min(plate.height,(int)Math.ceil((maskBounds.bottom()+1.0f-bounds.y)*plate.height/bounds.h));
    if(right<=left || bottom<=top)return null;
    final int width=right-left,height=bottom-top;
    final ByteBuffer output=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(width,height),4));
    boolean visible=false;
    for(int y=top;y<bottom;y++) {
      final float my=bounds.y+(y+0.5f)*bounds.h/plate.height-maskBounds.y-0.5f;
      for(int x=left;x<right;x++) {
        final float mx=bounds.x+(x+0.5f)*bounds.w/plate.width-maskBounds.x-0.5f;
        final int offset=(y*plate.width+x)*4;
        final int alpha=Math.round(coverage(mask,maskBounds.w,maskBounds.h,mx,my)*(plate.pixels.get(offset+3)&255));
        output.put(plate.pixels.get(offset)).put(plate.pixels.get(offset+1)).put(plate.pixels.get(offset+2)).put((byte)alpha);
        visible|=alpha!=0;
      }
    }
    if(!visible)return null;
    return new Layer(output.flip(),width,height,left,top,
      bounds.x+left*(float)bounds.w/plate.width,bounds.y+top*(float)bounds.h/plate.height,
      width*(float)bounds.w/plate.width,height*(float)bounds.h/plate.height);
  }

  static ByteBuffer colourize(final DecodedImage plate, final Rect4i bounds,
                             final Rect4i maskBounds, final int[] mask) {
    if(mask.length != maskBounds.w * maskBounds.h) throw new IllegalArgumentException("Cutout mask size");
    final ByteBuffer output=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(plate.width,plate.height),4));
    for(int y=0;y<plate.height;y++) {
      final float my=bounds.y+(y+0.5f)*bounds.h/plate.height-maskBounds.y-0.5f;
      for(int x=0;x<plate.width;x++) {
        final float mx=bounds.x+(x+0.5f)*bounds.w/plate.width-maskBounds.x-0.5f;
        final int offset=(y*plate.width+x)*4;
        final float coverage=coverage(mask,maskBounds.w,maskBounds.h,mx,my);
        output.put(plate.pixels.get(offset)).put(plate.pixels.get(offset+1)).put(plate.pixels.get(offset+2));
        output.put((byte)Math.round(coverage*(plate.pixels.get(offset+3)&255)));
      }
    }
    return output.flip();
  }

  /** Optional regression oracle, enabled only by the native verification harness. */
  static void verifyCompact(final DecodedImage plate,final Rect4i bounds,final Rect4i maskBounds,final int[] mask,final Layer layer) {
    final ByteBuffer reference=colourize(plate,bounds,maskBounds,mask);
    for(int y=0;y<plate.height;y++)for(int x=0;x<plate.width;x++) {
      final int offset=(y*plate.width+x)*4;
      if(layer!=null && x>=layer.pixelX && x<layer.pixelX+layer.width && y>=layer.pixelY && y<layer.pixelY+layer.height) {
        final int target=((y-layer.pixelY)*layer.width+x-layer.pixelX)*4;
        for(int c=0;c<4;c++)if(reference.get(offset+c)!=layer.pixels.get(target+c))throw new AssertionError("Foreground crop changed a texel");
        final float wx=layer.x+(x-layer.pixelX+0.5f)*layer.w/layer.width;
        final float wy=layer.y+(y-layer.pixelY+0.5f)*layer.h/layer.height;
        if(Math.abs(wx-(bounds.x+(x+0.5f)*bounds.w/plate.width))>0.0002f || Math.abs(wy-(bounds.y+(y+0.5f)*bounds.h/plate.height))>0.0002f)
          throw new AssertionError("Foreground crop moved its pixel grid");
      } else if(reference.get(offset+3)!=0)throw new AssertionError("Foreground crop discarded visible coverage");
    }
  }

  private static float coverage(final int[] mask,final int width,final int height,final float x,final float y) {
    final int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
    final float fx=x-ix,fy=y-iy;
    final float top=sample(mask,width,height,ix,iy)*(1-fx)+sample(mask,width,height,ix+1,iy)*fx;
    final float bottom=sample(mask,width,height,ix,iy+1)*(1-fx)+sample(mask,width,height,ix+1,iy+1)*fx;
    return top*(1-fy)+bottom*fy;
  }
  private static float sample(final int[] mask,final int width,final int height,final int x,final int y) {
    return x>=0&&y>=0&&x<width&&y<height&&mask[y*width+x]!=0 ? 1:0;
  }
}
