package legend.game.submap;

/** Remove the baked old sky from retail storm frames, retaining the original additive light. */
final class RemasteredLightning {
 private RemasteredLightning(){}
 static int[] difference(int[] base,int[] frame){
  if(base.length!=frame.length)throw new IllegalArgumentException("Storm frame dimensions");
  int[] out=new int[frame.length];
  for(int i=0;i<out.length;i++){
   int r=Math.max(0,(frame[i]&255)-(base[i]&255));
   int g=Math.max(0,(frame[i]>>>8&255)-(base[i]>>>8&255));
   int b=Math.max(0,(frame[i]>>>16&255)-(base[i]>>>16&255));
   if(Math.max(r,Math.max(g,b))>=16)out[i]=0xff000000|b<<16|g<<8|r;
  }return out;
 }
}
