package legend.game.inventory.screens;

import legend.core.GamePaths;
import legend.core.gpu.*;
import legend.core.gte.*;
import legend.core.lang.RawText;
import legend.core.renderer.*;
import legend.core.platform.input.InputMod;
import static legend.game.Text.renderText;
import legend.game.submap.*;
import legend.game.tim.Tim;
import legend.game.tmd.*;
import legend.game.types.*;
import legend.game.unpacker.*;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import static legend.core.GameEngine.*;

/** Lightweight resource browser; preview objects never replace campaign state. */
public final class VisorScreen extends MenuScreen {
 public enum Kind { CHARACTERS, BACKGROUNDS }
 private final Kind kind;
 private record Entry(String label,int cut,Path model,int index){}
 private final List<Entry> backgrounds=new ArrayList<>(),models=new ArrayList<>();
 private boolean firstFrame=true;private boolean modelMode;private int selected;private final Runnable unload;
 private Texture original,replacement;private Obj plate,backdrop;private Model124 model;
 private int scroll;
 private final List<Entry> catalogue=new ArrayList<>();
 private final legend.core.font.Font font=FONTS.get(GamePaths.resolve("gfx/fonts/menu-readable.json"));
 private float angle;
 private static final int LIST_WIDTH=150,ROW_HEIGHT=14,VISIBLE_ROWS=16;
 public VisorScreen(Kind kind,Runnable unload){this.kind=Objects.requireNonNull(kind);this.unload=unload;modelMode=kind==Kind.CHARACTERS;
  backdrop=new QuadBuilder("Visor backdrop").pos(-200,-200,60000).posSize(1000,1000).rgb(.025f,.03f,.04f).build();
  try{
   if(kind==Kind.BACKGROUNDS){
   backgrounds.add(new Entry("Titulo",-1,null,0));
   byte[] rootData=Files.readAllBytes(GamePaths.resolve("files/SUBMAP/NEWROOT.RDT"));ByteBuffer nr=ByteBuffer.wrap(rootData).order(ByteOrder.LITTLE_ENDIAN);
   for(int cut=0;cut<1024;cut++){int e=nr.getShort(cut*8)&65535;Path folder=GamePaths.resolve("files/SECT/DRGN"+(21+(e>>>13))+".BIN/"+((e&8191)*3+4));if(Files.isRegularFile(folder.resolve("0")))backgrounds.add(new Entry(cut==675?"Montanas — Rose":cut==676?"Bosque — Dart":"Escenario "+cut,cut,null,0));}
   }
   if(kind==Kind.CHARACTERS)try(var folders=Files.list(GamePaths.resolve("files/SECT/DRGN21.BIN"))){
    Set<String> seen=new HashSet<>();
    for(Path folder:folders.filter(Files::isDirectory).sorted().toList())try{
     int dir=Integer.parseInt(folder.getFileName().toString());if(dir%3!=2||!Files.isRegularFile(folder.resolve("textures/mrg")))continue;
     Map<Integer,Integer> assets=mrg(folder.resolve("mrg")),textures=mrg(folder.resolve("textures/mrg"));
     for(int n:new TreeSet<>(assets.keySet())){
      if(n%33!=0||!assets.containsKey(n+1))continue;int real=assets.get(n),tex=textures.getOrDefault(n/33,-1);if(real<0||tex<0)continue;
      Path raw=folder.resolve(""+real),texture=folder.resolve("textures/"+tex);if(!Files.isRegularFile(raw)||!Files.isRegularFile(texture)||Files.size(raw)<128)continue;
      byte[] bytes=Files.readAllBytes(raw);ByteBuffer header=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);int offset=header.getInt(0);if(offset<0||offset+12>bytes.length)continue;int parts=header.getInt(offset+8);if(parts<1||parts>128)continue;
      var digest=java.security.MessageDigest.getInstance("SHA-256");digest.update(bytes);digest.update(Files.readAllBytes(texture));String key=Base64.getEncoder().encodeToString(digest.digest());
      if(dir==695&&(n==0||n==33)||seen.add(key))models.add(new Entry(dir==695&&n==0?"Dart":dir==695&&n==33?"Rose":"Personaje / NPC "+(models.size()+1),0,folder.resolve(""+n),n/33));
     }
    }catch(Exception ignored){}
   }

  }catch(Exception error){throw new IllegalStateException("Cannot index viewer resources",error);}
  backgrounds.sort(Comparator.comparingInt(e->e.cut()<0?0:e.cut()==675?1:e.cut()==676?2:3));
  models.sort(Comparator.comparingInt(e->e.label().equals("Dart")?0:e.label().equals("Rose")?1:2));
  catalogue.addAll(entries());load();
 }
 private static Map<Integer,Integer> mrg(Path path)throws java.io.IOException{Map<Integer,Integer> out=new HashMap<>();for(String line:Files.readAllLines(path)){String[] values=line.split("[=;]",-1);if(values.length==3)out.put(Integer.parseInt(values[0]),values[1].isBlank()?-1:Integer.parseInt(values[1]));}return out;}
 private List<Entry> entries(){return modelMode?models:backgrounds;}
 private void release(){if(original!=null)original.delete();if(replacement!=null)replacement.delete();if(plate!=null)plate.delete();if(model!=null)model.deleteModelParts();original=null;replacement=null;plate=null;model=null;}
 private void load(){release();angle=0;int index=catalogueIndex();if(index<scroll)scroll=index;if(index>=scroll+VISIBLE_ROWS)scroll=index-VISIBLE_ROWS+1;if(entries().isEmpty())return;Entry e=entries().get(selected);try{
  if(modelMode){var assets=Loader.loadDirectorySync(e.model().getParent());var textures=Loader.loadDirectorySync(e.model().getParent().resolve("textures"));model=new Model124(e.label());model.uvAdjustments_9d=new UvAdjustmentMetrics14(1,576,256);legend.game.Models.initModel(model,new CContainer(e.label(),new FileData(assets.get(e.index()*33).getBytes())),new TmdAnimationFile(assets.get(e.index()*33+1)));Tim tim=new Tim(textures.get(e.index()));var im=tim.getImageRect();var cl=tim.getClutRect();GPU.uploadData15(new Rect4i(576,256,im.w,im.h),tim.getImageData());GPU.uploadData15(new Rect4i(576,368,cl.w,cl.h),tim.getClutData());TmdObjLoader.fromModel("Visor original",model);
  }else{
   if(e.cut()<0){original=Texture.filteredPng("Visor title original",GamePaths.resolve("gfx/visor/title-original.png"));replacement=Texture.filteredPng("Visor title remaster",GamePaths.resolve("gfx/remaster/title/background.png"));}
   else{original=field(e.cut());replacement=legend.game.submap.SubmapBackgroundOverride.previewTexture(e.cut());}
   plate=new QuadBuilder("Visor plate").posSize(1,1).uvSize(1,1).bpp(Bpp.BITS_24).build();
  }
 }catch(Exception error){release();System.err.println("Visor: "+e.label()+" unavailable: "+error);}}
 private Texture field(int cut)throws Exception{
  byte[] nr=Files.readAllBytes(GamePaths.resolve("files/SUBMAP/NEWROOT.RDT"));int e=ByteBuffer.wrap(nr).order(ByteOrder.LITTLE_ENDIAN).getShort(cut*8)&65535;
  var files=Loader.loadDirectorySync("SECT/DRGN"+(21+(e>>>13))+".BIN/"+((e&8191)*3+4));EnvironmentFile env=new EnvironmentFile(files.get(0));List<Tim> ts=new ArrayList<>();for(int i=3;i<files.size();i++)ts.add(new Tim(files.get(i)));
  int l=99999,t=99999,r=-99999,b=-99999;for(int i=0;i<env.backgroundTextureCount_15;i++){var m=env.environments_18[i];l=Math.min(l,m.textureOffsetX_10);t=Math.min(t,m.textureOffsetY_12);r=Math.max(r,m.textureOffsetX_10+m.vramPos_08.w);b=Math.max(b,m.textureOffsetY_12+m.vramPos_08.h);}int w=r-l,h=b-t;ByteBuffer out=ByteBuffer.allocateDirect(w*h*4);
  for(var m:env.environments_18){int tx=(m.tpage_20&15)*64,ty=(m.tpage_20&16)!=0?256:0;for(Tim tim:ts)if(tim.getImageRect().contains(tx,ty)){int[] p=VramTextureLoader.textureFromTim(tim).applyPalette(VramTextureLoader.palettesFromTim(tim)[0],m.vramPos_08);for(int y=0;y<m.vramPos_08.h;y++)for(int x=0;x<m.vramPos_08.w;x++){int xx=m.textureOffsetX_10-l+x,yy=m.textureOffsetY_12-t+y,c=p[y*m.vramPos_08.w+x];if(xx<0||yy<0||xx>=w||yy>=h||c==0||(c&0xffffff)==0xffff00)continue;int o=(yy*w+xx)*4;out.put(o,(byte)c);out.put(o+1,(byte)(c>>>8));out.put(o+2,(byte)(c>>>16));out.put(o+3,(byte)255);}break;}}
  return Texture.create("Visor original "+cut,builder->{builder.data(out,w,h);builder.minFilter(true);builder.magFilter(true);});
 }
 private boolean previewRemastered = true;
 private int catalogueIndex(){return selected;}
 private void selectCatalogue(int index){if(catalogue.isEmpty())return;index=Math.clamp(index,0,catalogue.size()-1);selected=index;if(index<scroll)scroll=index;if(index>=scroll+VISIBLE_ROWS)scroll=index-VISIBLE_ROWS+1;load();}
 private void leave(){release();backdrop.delete();unload.run();}
 @Override protected InputPropagation mouseClick(double x,double y,int button,Set<InputMod> mods){
  double fullX=x+RENDERER.getWidescreenOrthoOffsetX();
  if(button==0&&fullX>=0&&fullX<LIST_WIDTH&&y>=8&&y<8+ROW_HEIGHT*VISIBLE_ROWS){selectCatalogue(scroll+(int)((y-8)/ROW_HEIGHT));return InputPropagation.HANDLED;}
  return super.mouseClick(x,y,button,mods);
 }
 @Override protected InputPropagation mouseScroll(int dx,int dy){scroll=Math.clamp(scroll-dy*3,0,Math.max(0,catalogue.size()-VISIBLE_ROWS));return InputPropagation.HANDLED;}
 @Override protected InputPropagation inputActionPressed(legend.core.platform.input.InputAction action,boolean repeat){
  if(action==legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_UP.get())selectCatalogue(catalogueIndex()-1);
  else if(action==legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_DOWN.get())selectCatalogue(catalogueIndex()+1);
  else if(action==legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_LEFT.get())selectCatalogue(catalogueIndex()-VISIBLE_ROWS);
  else if(action==legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_RIGHT.get())selectCatalogue(catalogueIndex()+VISIBLE_ROWS);
  else if(action==legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_CONFIRM.get())this.previewRemastered = !this.previewRemastered;
  else if(action==legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_BACK.get())leave();
  else return super.inputActionPressed(action,repeat);
  return InputPropagation.HANDLED;
 }
 @Override protected void render(){
  if(firstFrame){legend.game.FullScreenEffects.startFadeEffect(2,10);firstFrame=false;}
  RENDERER.setRenderMode(legend.game.EngineState.RenderMode.LEGACY);RENDERER.queueOrthoModel(backdrop,QueuedModelStandard.class);
  boolean hd=this.previewRemastered;
  float fullWidth=RENDERER.getNativeHeight()*RENDERER.getRenderAspectRatio();
  float previewX=LIST_WIDTH+8,previewWidth=fullWidth-previewX-8;
  for(int row=0;row<VISIBLE_ROWS&&scroll+row<catalogue.size();row++){
   int index=scroll+row;Entry item=catalogue.get(index);
   String label=item.label();
   float size=Math.min(.65f,(LIST_WIDTH-12)/(float)Math.max(1,font.textWidth(label)));
   renderText(font,label,6-RENDERER.getWidescreenOrthoOffsetX(),8+row*ROW_HEIGHT,new FontOptions().size(size).colour(index==catalogueIndex()?TextColour.GOLD:TextColour.WHITE));
  }
  if(plate!=null&&original!=null){Texture tex=hd&&replacement!=null?replacement:original;float scale=Math.min(previewWidth/tex.width,224f/tex.height);RENDERER.queueOrthoModel(plate,new Matrix4f().translation(previewX+(previewWidth-tex.width*scale)/2,(240-tex.height*scale)/2,2000).scale(tex.width*scale,tex.height*scale,1),QueuedModelStandard.class).texture(tex);}
  if(model!=null){legend.game.Models.animateModel(model,4);angle+=.006f;MV bone=new MV();Matrix4f[] poses=new Matrix4f[model.modelParts_00.length];Vector3f min=new Vector3f(Float.POSITIVE_INFINITY),max=new Vector3f(Float.NEGATIVE_INFINITY);
   for(int i=0;i<poses.length;i++){legend.game.Graphics.GsGetLw(model.modelParts_00[i].coord2_04,bone);poses[i]=new Matrix4f().rotateY(angle).mul(new Matrix4f(bone).setTranslation(bone.transfer));for(Vector3f v:model.modelParts_00[i].tmd_08.vert_top_00){Vector3f p=poses[i].transformPosition(new Vector3f(v));min.min(p);max.max(p);}}
   float scale=Math.min((previewWidth-24)/Math.max(1,max.x-min.x),200/Math.max(1,max.y-min.y));Vector3f centre=new Vector3f(min).add(max).mul(.5f);
   for(int i=0;i<poses.length;i++){Matrix4f transform=new Matrix4f().translation(previewX+previewWidth/2,120,1000).scale(scale).translate(-centre.x,-centre.y,-centre.z).mul(poses[i]);var q=RENDERER.queueOrthoModel(model.modelParts_00[i].tmd_08.getObj(),transform,QueuedModelTmd.class);MV light=new MV();poses[i].get3x3(light);q.setLightTransforms(light);q.lightDirection(new Matrix3f(-.45f,.75f,.25f,-.55f,-.1f,-.5f,-.70f,-.65f,.8f)).lightColour(new Matrix3f(.60f,.60f,.60f,.18f,.18f,.18f,.25f,.25f,.25f)).backgroundColour(new Vector3f(.22f));}
  }
 }
}
