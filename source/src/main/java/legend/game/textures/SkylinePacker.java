package legend.game.textures;

import legend.core.gpu.Rect4i;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Arrays;

/** Bottom-left skyline packing without native memory or desktop libraries. */
final class SkylinePacker {
  private SkylinePacker() { }
  static void pack(final Collection<Rect4i> rectangles, final int width, final int height) {
    if(width<=0 || height<=0)throw new IllegalArgumentException("Invalid atlas dimensions");
    final var ordered=new ArrayList<>(rectangles);
    ordered.sort(Comparator.comparingInt((Rect4i r)->r.h).reversed()
      .thenComparing(Comparator.comparingInt((Rect4i r)->r.w).reversed()));
    final int[] skyline=new int[width], deque=new int[width];
    for(final Rect4i rectangle:ordered) {
      if(rectangle.w<=0 || rectangle.h<=0 || rectangle.w>width || rectangle.h>height)
        throw new IllegalArgumentException("Invalid atlas rectangle "+rectangle.w+"x"+rectangle.h);
      int head=0,tail=0,bestX=-1,bestY=height;
      // Sliding maximum gives the height under each possible placement in O(width).
      for(int x=0;x<width;x++) {
        while(head<tail && deque[head]<=x-rectangle.w)head++;
        while(head<tail && skyline[deque[tail-1]]<=skyline[x])tail--;
        deque[tail++]=x;
        if(x>=rectangle.w-1) {
          final int y=skyline[deque[head]];
          if(y<=height-rectangle.h && y<bestY) {
            bestX=x-rectangle.w+1;bestY=y;
          }
        }
      }
      if(bestX<0)throw new IllegalArgumentException("Failed to pack texture atlas "+width+"x"+height);
      rectangle.x=bestX;rectangle.y=bestY;
      Arrays.fill(skyline,bestX,bestX+rectangle.w,bestY+rectangle.h);
    }
  }
}
