package com.realbox.dragoon;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
import java.util.function.BiConsumer;

/** Permanent translucent overlay. Game context never hides or rearranges the keys. */
final class PlayStationControls extends View {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final VirtualPad pad;
  private final Runnable toggle, diagnostic;
  private final Map<Integer,Set<VirtualPad.Key>> started=new HashMap<>();
  private final Set<Integer> longPressed=new HashSet<>();
  private boolean remastered;
  private final Runnable holdDiagnostic;

  PlayStationControls(Context context,BiConsumer<String,Boolean> input,Runnable toggle,Runnable diagnostic) {
    super(context); this.toggle=toggle; this.diagnostic=diagnostic;
    pad=new VirtualPad((key,down)->{
      String name=switch(key) {
        case LEFT->"←"; case UP->"↑"; case DOWN->"↓"; case RIGHT->"→";
        case CROSS->"X"; case CIRCLE->"O"; case SQUARE->"Square"; case TRIANGLE->"Triangle";
        case START->"Start"; case L1,L2,R1,R2->key.name(); default->null;
      };
      if(name!=null) input.accept(name,down);
    });
    holdDiagnostic=()->{
      for(var entry:started.entrySet()) if(entry.getValue().contains(VirtualPad.Key.VIEW) && pad.held(VirtualPad.Key.VIEW)) {
        longPressed.add(entry.getKey()); this.diagnostic.run(); break;
      }
    };
    setContentDescription("Mando PlayStation: cruceta, triangulo, circulo, cruz, cuadrado, L1, L2, R1, R2, SELECT y START. Mantener Vista abre el diagnostico.");
  }
  void setRemastered(boolean value) { remastered=value; invalidate(); }
  void releaseAll() { removeCallbacks(holdDiagnostic); pad.clear(); started.clear(); longPressed.clear(); invalidate(); }
  private float scaleX() { return getWidth() / 1280f; }
  private float scaleY() { return getHeight() / 720f; }

  @Override public boolean onTouchEvent(MotionEvent event) {
    float sx=scaleX(), sy=scaleY(); if(sx<=0 || sy<=0) return false;
    int action=event.getActionMasked(), index=event.getActionIndex(), id=event.getPointerId(index);
    if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
      Set<VirtualPad.Key> hit=VirtualPad.hit(event.getX(index)/sx,event.getY(index)/sy);
      if(action==MotionEvent.ACTION_DOWN && hit.isEmpty() &&
         (Math.abs(event.getX(index)/sx-VirtualPad.DPAD_X)>127 || Math.abs(event.getY(index)/sy-VirtualPad.DPAD_Y)>127)) return false;
      started.put(id,hit); pad.move(id,event.getX(index)/sx,event.getY(index)/sy);
      if(hit.contains(VirtualPad.Key.VIEW)) postDelayed(holdDiagnostic,ViewConfiguration.getLongPressTimeout());
    } else if(action==MotionEvent.ACTION_MOVE) {
      for(int i=0;i<event.getPointerCount();i++) pad.move(event.getPointerId(i),event.getX(i)/sx,event.getY(i)/sy);
    } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) {
      Set<VirtualPad.Key> initial=started.remove(id), hit=VirtualPad.hit(event.getX(index)/sx,event.getY(index)/sy);
      pad.up(id);
      if(initial!=null && !longPressed.contains(id)) {
        if((initial.contains(VirtualPad.Key.VIEW)&&hit.contains(VirtualPad.Key.VIEW)) ||
           (initial.contains(VirtualPad.Key.SELECT)&&hit.contains(VirtualPad.Key.SELECT))) toggle.run();
      }
      longPressed.remove(id); if(!pad.held(VirtualPad.Key.VIEW)) removeCallbacks(holdDiagnostic);
      performClick();
    } else if(action==MotionEvent.ACTION_CANCEL) releaseAll();
    invalidate(); return true;
  }
  @Override public boolean performClick() { super.performClick(); return true; }
  @Override protected void onDetachedFromWindow() { releaseAll(); super.onDetachedFromWindow(); }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas); canvas.save(); canvas.scale(scaleX(),scaleY());
    for(VirtualPad.Button button:VirtualPad.BUTTONS) {
      float x=button.x(),y=button.y(),w=button.w(),h=button.h(),cx=x+w/2,cy=y+h/2;
      paint.setStyle(Paint.Style.FILL); paint.setColor(pad.held(button.key())?0x667d96b0:0x2e0a0f1a);
      canvas.drawRoundRect(x,y,x+w,y+h,Math.min(32,h/2),Math.min(32,h/2),paint);
      paint.setColor(0xa6ffffff); paint.setStrokeWidth(3.5f); paint.setStyle(Paint.Style.STROKE);
      float half=16;
      switch(button.key()) {
        case CROSS -> { canvas.drawLine(cx-half,cy-half,cx+half,cy+half,paint); canvas.drawLine(cx-half,cy+half,cx+half,cy-half,paint); }
        case CIRCLE -> canvas.drawCircle(cx,cy,half,paint);
        case SQUARE -> canvas.drawRect(cx-half,cy-half,cx+half,cy+half,paint);
        case TRIANGLE -> { Path p=new Path();p.moveTo(cx,cy-half-2);p.lineTo(cx+half+2,cy+half);p.lineTo(cx-half-2,cy+half);p.close();canvas.drawPath(p,paint); }
        case LEFT,RIGHT,UP,DOWN -> {
          canvas.save();canvas.rotate(switch(button.key()){case UP->-90;case LEFT->180;case DOWN->90;default->0;},cx,cy);
          Path p=new Path();p.moveTo(cx-half,cy-half);p.lineTo(cx+half,cy);p.lineTo(cx-half,cy+half);p.close();
          paint.setStyle(Paint.Style.FILL);canvas.drawPath(p,paint);canvas.restore();
        }
        default -> {
          paint.setStyle(Paint.Style.FILL); paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
          paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(button.key()==VirtualPad.Key.VIEW?17:button.h()>42?20:17);
          String text=button.key()==VirtualPad.Key.VIEW?(remastered?"Vista: Remaster":"Vista: Original"):button.key().name();
          canvas.drawText(text,cx,cy-(paint.ascent()+paint.descent())/2,paint);
        }
      }
    }
    canvas.restore();
  }
}
