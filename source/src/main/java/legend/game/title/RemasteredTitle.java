package legend.game.title;

import legend.core.GamePaths;
import legend.core.IoHelper;
import legend.core.gpu.Bpp;
import legend.core.image.DecodedImage;
import legend.core.image.ImageCodecs;
import legend.core.renderer.Obj;
import legend.core.renderer.QuadBuilder;
import legend.core.renderer.QueuedModelStandard;
import legend.core.renderer.Texture;
import legend.core.renderer.Translucency;
import org.joml.Matrix4f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static legend.core.GameEngine.RENDERER;

/** Fresh title art with a continuous flame halo attached to the actual logo's outline. */
final class RemasteredTitle implements AutoCloseable {
  final Texture background,logo,fire;
  final Obj logoObj;
  private final Obj[] flames=new Obj[4];
  private final List<float[]> anchors=new ArrayList<>();
  private int ticks;
  private final Matrix4f transforms=new Matrix4f();

  RemasteredTitle() {
    final var folder=GamePaths.resolve("gfx/remaster/title");
    this.background=Texture.filteredPng("Remastered title relief",folder.resolve("background.png"));
    this.logo=Texture.filteredPng("Remastered retail title logo",folder.resolve("logo.png"));
    this.fire=Texture.filteredPng("Remastered animated logo fire",folder.resolve("fire.png"));
    final int[] crop;
    try(final DecodedImage image=ImageCodecs.decode(IoHelper.pathToByteBuffer(folder.resolve("logo.png")))) {
      crop=alphaBounds(image);
      for(int col=0;col<28;col++) {
        final int x=crop[0]+Math.round((col+0.5f)*(crop[2]-crop[0])/28);
        int top=-1,bottom=-1;
        for(int y=crop[1];y<crop[3];y++)if((image.pixels.get((y*image.width+x)*4+3)&255)>96){if(top<0)top=y;bottom=y;}
        if(top>=0) {
          final float u=(x-crop[0])/(float)(crop[2]-crop[0])*352;
          this.anchors.add(new float[]{u,40+(top-crop[1])/(float)(crop[3]-crop[1])*88,1});
          this.anchors.add(new float[]{u,40+(bottom-crop[1])/(float)(crop[3]-crop[1])*88,0.55f});
        }
      }
    } catch(final IOException error){throw new IllegalStateException("Cannot read remastered title outline",error);}
    this.logoObj=new QuadBuilder("Remastered logo, original proportions")
      .pos(0,40,0).posSize(352,88).bpp(Bpp.BITS_24)
      .uv(crop[0]/(float)this.logo.width,crop[1]/(float)this.logo.height)
      .uvSize((crop[2]-crop[0])/(float)this.logo.width,(crop[3]-crop[1])/(float)this.logo.height).build();
    for(int frame=0;frame<4;frame++)this.flames[frame]=new QuadBuilder("Title flame frame "+frame)
      .pos(-0.5f,-1,0).posSize(1,1).bpp(Bpp.BITS_24)
      .uv(frame/4.0f+0.001f,0).uvSize(0.248f,1).build();
  }

  static int[] alphaBounds(final DecodedImage image) {
    int left=image.width,top=image.height,right=0,bottom=0;
    for(int y=0;y<image.height;y++)for(int x=0;x<image.width;x++)if((image.pixels.get((y*image.width+x)*4+3)&255)>64) {
      left=Math.min(left,x);right=Math.max(right,x+1);top=Math.min(top,y);bottom=Math.max(bottom,y+1);
    }
    if(left>=right||top>=bottom)throw new IllegalArgumentException("Empty remastered title logo");
    return new int[]{left,top,right,bottom};
  }

  void drawFire(final float fade) {
    final float time=this.ticks++/60.0f;
    for(int i=0;i<this.anchors.size();i++) {
      final float[] a=this.anchors.get(i);
      final float phase=time*3.7f+i*2.399963f;
      final float height=(18+5*(float)Math.sin(phase*0.73f))*a[2];
      final float width=7+2*(float)Math.cos(phase*0.91f);
      final float x=a[0]+1.1f*(float)Math.sin(phase),y=a[1]+1.2f*(float)Math.sin(phase*0.61f);
      final float frameTime=time*9+i*0.618034f;
      final int frame=(int)Math.floor(frameTime);
      final float blend=frameTime-frame;
      this.transforms.scaling(1.06f,0.82f,1).translate(-4+RENDERER.getWidescreenOrthoOffsetX()+x,-4+y,10010).scale(width,height,1);
      this.queueFlame(frame&3,fade*(1-blend)*0.75f);
      this.queueFlame((frame+1)&3,fade*blend*0.75f);
    }
  }

  private void queueFlame(final int frame,final float opacity) {
    RENDERER.queueOrthoModel(this.flames[frame],this.transforms,QueuedModelStandard.class)
      .texture(this.fire).translucency(Translucency.B_PLUS_F).alpha(opacity).useTextureAlpha();
  }

  @Override public void close(){this.background.delete();this.logo.delete();this.fire.delete();this.logoObj.delete();for(final Obj flame:this.flames)flame.delete();}
}
