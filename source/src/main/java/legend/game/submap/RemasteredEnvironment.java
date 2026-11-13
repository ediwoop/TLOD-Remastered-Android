package legend.game.submap;
import legend.core.gpu.Bpp;
import legend.core.gte.MV;
import legend.core.renderer.*;
import java.nio.ByteBuffer;
import static legend.core.GameEngine.RENDERER;
/** Newly authored fields and motion, with retail geometry used only as an anchor reference.
 * No original pixels, palettes, meshes or animation frames are used by these effects. */
final class RemasteredEnvironment implements AutoCloseable {
 enum Kind { MIST, SHAFT, RIPPLE, SMOKE, MOTE }
 record Layer(Kind kind,float x,float y,float w,float h,float opacity,float phase) { }
 private final Layer[] layers;
 private final Texture[] textures=new Texture[Kind.values().length];
 private final MV transform=new MV();
 private Obj quad;
 private double seconds;
 private int renderedFrames;
 private boolean closed;
 RemasteredEnvironment(int cut){layers=profile(cut);}
 static boolean supports(int cut){return cut==675||cut==676||cut==703||cut==9;}
 static Layer[] profile(int cut){return switch(cut){
  case 675 -> new Layer[]{new Layer(Kind.MIST,62,-8,260,65,.16f,0),new Layer(Kind.MIST,250,0,290,58,.13f,2.1f),new Layer(Kind.MIST,150,127,275,74,.13f,1.3f),new Layer(Kind.MIST,224,185,310,74,.18f,3.2f)};
  case 676 -> new Layer[]{new Layer(Kind.SHAFT,-2,-12,146,285,.20f,0),new Layer(Kind.SHAFT,213,12,66,177,.15f,1.7f),new Layer(Kind.SHAFT,288,-8,70,150,.12f,3),new Layer(Kind.MIST,46,170,140,106,.07f,2),new Layer(Kind.MOTE,68,105,2,2,.27f,0),new Layer(Kind.MOTE,90,156,1.2f,1.2f,.25f,2),new Layer(Kind.MOTE,112,217,1.4f,1.4f,.22f,4),new Layer(Kind.MOTE,239,94,1,1,.22f,1)};
  case 703 -> new Layer[]{new Layer(Kind.MIST,139,5,245,92,.09f,0),new Layer(Kind.MIST,470,-2,288,100,.08f,2.1f),new Layer(Kind.RIPPLE,338,165,109,16,.18f,0),new Layer(Kind.RIPPLE,416,168,126,18,.20f,1.6f),new Layer(Kind.RIPPLE,511,163,91,18,.17f,3.1f),new Layer(Kind.RIPPLE,598,161,87,19,.15f,4.4f)};
  case 9 -> new Layer[]{new Layer(Kind.SMOKE,86,103,61,69,.18f,0),new Layer(Kind.SMOKE,125,87,57,81,.20f,1.8f),new Layer(Kind.SMOKE,174,70,62,87,.16f,3.3f),new Layer(Kind.SMOKE,218,59,48,82,.15f,4.6f)};
  default -> new Layer[0];};}
 void tick(float deltaSeconds,boolean reduceMotion){if(!closed&&!reduceMotion)seconds+=deltaSeconds;}
 static float[] pose(Layer layer,double time){
  float t=(float)time,p=layer.phase;
  float speed=layer.kind==Kind.RIPPLE?1.6f:layer.kind==Kind.SHAFT?.32f:.19f;
  float wave=(float)Math.sin(t*speed+p),drift=layer.kind==Kind.RIPPLE?1.2f:layer.kind==Kind.SHAFT?.45f:3.2f;
  if(layer.kind==Kind.SMOKE){
   float age=(float)((time*.075+layer.phase*.17)%1),width=layer.w*(.28f+.67f*age),height=layer.h*(.22f+.40f*age);
   return new float[]{layer.x+(layer.w-width)*.5f+5*age+2*(float)Math.sin(age*5+p),layer.y+layer.h-age*layer.h*.85f-height*.75f,width,height,layer.opacity*(float)Math.pow(Math.sin(Math.PI*age),1.3)};
  }
  float dx=wave*drift;
  float dy=layer.kind==Kind.MOTE?3*(float)Math.sin(t*.23f+p):.35f*(float)Math.cos(t*.27f+p);
  float breath=1+.025f*(float)Math.sin(t*.23f+p);
  return new float[]{layer.x+dx,layer.y+dy,layer.w*(layer.kind==Kind.SHAFT?1:breath),layer.h,layer.opacity*(.82f+.18f*(float)Math.sin(t*speed+p))};
 }
 void render(float sceneX,float sceneY){
  if(closed||layers.length==0)return;
  if(quad==null)quad=new QuadBuilder("New Remastered environmental effects").bpp(Bpp.BITS_24).posSize(1,1).uvSize(1,1).build();
  for(Layer layer:layers){int kind=layer.kind.ordinal();
   if(textures[kind]==null){ByteBuffer pixels=paint(layer.kind,128,128);textures[kind]=Texture.create("New procedural "+layer.kind,b->{b.data(pixels,128,128);b.minFilter(true);b.magFilter(true);b.wrapS(false);b.wrapT(false);});}
   int copies=layer.kind==Kind.SMOKE?3:1;
   for(int puff=0;puff<copies;puff++){
    float[] p=pose(layer,seconds+puff*(1.0/.075)/copies);
    transform.scaling(p[2],p[3],1);transform.transfer.set(sceneX+p[0],sceneY+p[1],12000);
    float r=layer.kind==Kind.SMOKE?.63f:layer.kind==Kind.RIPPLE?.76f:.84f,g=layer.kind==Kind.SMOKE?.64f:layer.kind==Kind.RIPPLE?.83f:.86f,b=layer.kind==Kind.SMOKE?.65f:layer.kind==Kind.SHAFT?.77f:.88f;
    RENDERER.queueOrthoModel(quad,transform,QueuedModelStandard.class).texture(textures[kind]).colour(r,g,b).translucency(Translucency.HALF_B_PLUS_HALF_F).alpha(p[4]).useTextureAlpha();
   }
  }
  renderedFrames++;
 }
 /** Code-native texture generation happens once per scene, never per frame. */
 static ByteBuffer paint(Kind kind,int w,int h){
  ByteBuffer pixels=ByteBuffer.allocateDirect(w*h*4);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   double u=(x+.5)/w,v=(y+.5)/h,edge=Math.pow(Math.sin(Math.PI*u)*Math.sin(Math.PI*v),1.2);
   double broad=.58+.18*Math.sin(u*13+v*7)+.13*Math.cos(v*19-u*5)+.07*Math.sin(u*29+v*17),alpha;
   switch(kind){
    case SHAFT -> {double center=.20+.49*v,width=.05+.12*v;alpha=Math.exp(-Math.pow((u-center)/width,2)*1.5)*Math.sin(Math.PI*v)*(.85+.15*Math.cos(v*17));}
    case RIPPLE -> {double band=Math.sin(v*59+Math.sin(u*18)*1.9+Math.cos(u*31)*.6);alpha=edge*Math.pow(Math.max(0,band),12)*(.45+.55*Math.pow(Math.sin(u*23+v*7),2));}
    case MOTE -> alpha=Math.exp(-28*((u-.5)*(u-.5)+(v-.5)*(v-.5)))*edge;
    case SMOKE -> {double center=.44+.11*Math.sin(v*9),width=.20+.15*(1-v);alpha=edge*broad*Math.exp(-Math.pow((u-center)/width,2)*1.5);}
    default -> alpha=edge*broad;
   }
   int a=(int)Math.round(Math.max(0,Math.min(1,alpha))*255);pixels.put((byte)255).put((byte)255).put((byte)255).put((byte)a);
  }return pixels.flip();
 }
 double seconds(){return seconds;}
 int renderedFrames(){return renderedFrames;}
 int layerCount(){return layers.length;}
 public void close(){if(closed)return;closed=true;if(quad!=null)quad.delete();for(Texture t:textures)if(t!=null)t.delete();}
}
