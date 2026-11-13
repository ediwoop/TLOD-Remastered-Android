package legend.game.tmd;

import legend.core.GamePaths;
import legend.core.renderer.*;
import org.joml.Vector3f;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.*;

/** Original retail geometry, articulation and animation; painted HD surfaces only. */
public final class NativeRoseHd {
 private NativeRoseHd() { }
 private static final String SIGNATURE="f634c43fd100fc6d8f0f77f52d5a1bcd7129577472c7b179dc772391c4702596";
 private static final Set<TmdObjTable1c> parts=Collections.newSetFromMap(new WeakHashMap<>());
 private static final ThreadLocal<Boolean> building=ThreadLocal.withInitial(()->false);
 private static Texture atlas;
 private static final Map<TmdObjTable1c,Integer> partIds=new WeakHashMap<>();
 public static boolean hair(TmdObjTable1c t){int id=partIds.getOrDefault(t,-1);return building()&&(id==5||id==6);}
 public static float hairUv(TmdObjTable1c t,Vector3f p,boolean vertical){float lo=Float.POSITIVE_INFINITY,hi=Float.NEGATIVE_INFINITY;for(var v:t.vert_top_00){float n=vertical?v.y:v.x;lo=Math.min(lo,n);hi=Math.max(hi,n);}float n=vertical?p.y:p.x;float q=(n-lo)/Math.max(.001f,hi-lo);return vertical?(350.5f+q*110)/464f:(2.5f+q*62)/272f;}
 private static final Map<TmdObjTable1c,UvAdjustmentMetrics14> placements=new WeakHashMap<>();
 public static void setPlacement(TmdObjTable1c table,UvAdjustmentMetrics14 metrics){if(parts.contains(table))placements.put(table,metrics);}
 public static int row(TmdObjTable1c table,int clut){UvAdjustmentMetrics14 m=placements.get(table);return ((clut>>>6)-(m==null?112:m.clutY))&15;}
 public static int uOffset(TmdObjTable1c table){UvAdjustmentMetrics14 m=placements.get(table);return m==null?0:m.tpageX%64*4;}
 public static int vOffset(TmdObjTable1c table){UvAdjustmentMetrics14 m=placements.get(table);return m==null?0:m.tpageY%256;}

 public static boolean building(){return building.get();}
 public static void register(Tmd model){
  if(model.objTable.length!=17)return;
  try{MessageDigest md=MessageDigest.getInstance("SHA-256");ByteBuffer b=ByteBuffer.allocate(4);
   for(var p:model.objTable)for(Vector3f[] a:new Vector3f[][]{p.vert_top_00,p.normal_top_08}){b.clear();b.putInt(a.length);md.update(b.array());for(var v:a)for(float f:new float[]{v.x,v.y,v.z}){b.clear();b.putFloat(f);md.update(b.array());}}
   if(HexFormat.of().formatHex(md.digest()).equals(SIGNATURE)){parts.addAll(Arrays.asList(model.objTable));for(int i=0;i<model.objTable.length;i++)partIds.put(model.objTable[i],i);}
  }catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 public static MeshObj wrap(String name,TmdObjTable1c table,int flags,int width,int height,MeshObj original){
  if(building()||!parts.contains(table))return original;
  MeshObj hd;building.set(true);try{hd=TmdObjLoader.fromObjTable(name+" original HD",table,flags,width,height);}finally{building.set(false);}
  return new Switched(name,original,hd);
 }
 private static Texture atlas(){if(atlas==null)atlas=Texture.filteredPng("Original Rose faithful HD atlas",GamePaths.resolve("gfx/remaster/rose-original-hd/atlas.png"));return atlas;}
 public static final class Switched extends MeshObj {
  public final MeshObj original,hd;
  Switched(String name,MeshObj original,MeshObj hd){super(name,new Mesh[0]);this.original=original;this.hd=hd;}
  private MeshObj active(){return RemasterMode.isEnabled()?hd:original;}
  public boolean useBackfaceCulling(){return active().useBackfaceCulling();}
  public boolean hasTexture(){return active().hasTexture();}
  public boolean hasTexture(int i){return active().hasTexture(i);}
  public boolean hasTranslucency(){return active().hasTranslucency();}
  public boolean hasTranslucency(int i){return active().hasTranslucency(i);}
  public boolean shouldRender(@Nullable Translucency t){return active().shouldRender(t);}
  public boolean shouldRender(@Nullable Translucency t,int i){return active().shouldRender(t,i);}
  public int getLayers(){return active().getLayers();}
  public void render(int layer,int start,int count){if(RemasterMode.isEnabled())atlas().use(0);active().render(layer,start,count);}
  public void render(@Nullable Translucency t,int layer,int start,int count){if(RemasterMode.isEnabled())atlas().use(0);active().render(t,layer,start,count);}
  protected void performDelete(){original.delete();hd.delete();}
 }
}
