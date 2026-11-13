package legend.game.submap;

import legend.core.gpu.Rect4i;
import legend.core.image.DecodedImage;

/** Gradient-domain join restricted to the generated exterior. The protected core is a Dirichlet boundary. */
final class ExteriorPlateJoin {
  private ExteriorPlateJoin() { }

  static void apply(final DecodedImage core, final Rect4i coreBounds,
                    final DecodedImage exterior, final Rect4i exteriorBounds) {
    float[] previous=null;
    int previousWidth=0,previousHeight=0;
    for(int level=4;level>=0;level--) {
      final int width=Math.max(3,exterior.width>>level),height=Math.max(3,exterior.height>>level);
      final float[] field=new float[width*height*3];
      final boolean[] fixed=new boolean[width*height];
      for(int y=0;y<height;y++) {
        final float worldY=exteriorBounds.y+(y+0.5f)*exteriorBounds.h/height;
        for(int x=0;x<width;x++) {
          final float worldX=exteriorBounds.x+(x+0.5f)*exteriorBounds.w/width;
          final int pixel=y*width+x,offset=pixel*3;
          final boolean inside=worldX>=coreBounds.x && worldX<coreBounds.right() && worldY>=coreBounds.y && worldY<coreBounds.bottom();
          fixed[pixel]=inside || x==0 || y==0 || x==width-1 || y==height-1;
          if(inside) {
            final float cx=(worldX-coreBounds.x)*core.width/coreBounds.w-0.5f;
            final float cy=(worldY-coreBounds.y)*core.height/coreBounds.h-0.5f;
            final float ex=(x+0.5f)*exterior.width/width-0.5f,ey=(y+0.5f)*exterior.height/height-0.5f;
            for(int c=0;c<3;c++)field[offset+c]=sample(core,cx,cy,c)-sample(exterior,ex,ey,c);
          } else if(!fixed[pixel] && previous!=null) {
            for(int c=0;c<3;c++)field[offset+c]=sampleField(previous,previousWidth,previousHeight,
              (x+0.5f)*previousWidth/width-0.5f,(y+0.5f)*previousHeight/height-0.5f,c);
          }
        }
      }
      // Harmonic correction leaves the generated image's Laplacian (its painted detail)
      // unchanged outside the core. It is not an edge extrusion or a blurred image fill.
      final int iterations=level==4?180:50;
      for(int iteration=0;iteration<iterations;iteration++) {
        float maximumChange=0;
        for(int y=1;y<height-1;y++)for(int x=1;x<width-1;x++) {
          final int pixel=y*width+x;if(fixed[pixel])continue;
          final int o=pixel*3;
          for(int c=0;c<3;c++) {
            final float change=1.65f*((field[o-3+c]+field[o+3+c]+field[o-width*3+c]+field[o+width*3+c])*0.25f-field[o+c]);
            field[o+c]+=change;maximumChange=Math.max(maximumChange,Math.abs(change));
          }
        }
        if(maximumChange<0.02f)break;
      }
      previous=field;previousWidth=width;previousHeight=height;
    }
    for(int y=0;y<exterior.height;y++)for(int x=0;x<exterior.width;x++) {
      final float wx=exteriorBounds.x+(x+0.5f)*exteriorBounds.w/exterior.width;
      final float wy=exteriorBounds.y+(y+0.5f)*exteriorBounds.h/exterior.height;
      if(wx>=coreBounds.x && wx<coreBounds.right() && wy>=coreBounds.y && wy<coreBounds.bottom())continue;
      final int pixel=y*exterior.width+x;
      for(int c=0;c<3;c++) {
        final float value=(exterior.pixels.get(pixel*4+c)&255)+previous[pixel*3+c];
        exterior.pixels.put(pixel*4+c,(byte)Math.round(Math.max(0,Math.min(255,value))));
      }
    }
  }

  private static float sampleField(float[] values,int width,int height,float x,float y,int channel) {
    x=Math.max(0,Math.min(width-1,x));y=Math.max(0,Math.min(height-1,y));
    final int x0=(int)x,y0=(int)y,x1=Math.min(width-1,x0+1),y1=Math.min(height-1,y0+1);
    final float fx=x-x0,fy=y-y0;
    return (values[(y0*width+x0)*3+channel]*(1-fx)+values[(y0*width+x1)*3+channel]*fx)*(1-fy)
      +(values[(y1*width+x0)*3+channel]*(1-fx)+values[(y1*width+x1)*3+channel]*fx)*fy;
  }
  private static float sample(DecodedImage image,float x,float y,int channel) {
    x=Math.max(0,Math.min(image.width-1,x));y=Math.max(0,Math.min(image.height-1,y));
    final int x0=(int)x,y0=(int)y,x1=Math.min(image.width-1,x0+1),y1=Math.min(image.height-1,y0+1);
    final float fx=x-x0,fy=y-y0;
    return ((image.pixels.get((y0*image.width+x0)*4+channel)&255)*(1-fx)+(image.pixels.get((y0*image.width+x1)*4+channel)&255)*fx)*(1-fy)
      +((image.pixels.get((y1*image.width+x0)*4+channel)&255)*(1-fx)+(image.pixels.get((y1*image.width+x1)*4+channel)&255)*fx)*fy;
  }
}
