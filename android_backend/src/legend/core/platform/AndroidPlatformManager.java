package legend.core.platform;

import android.view.Surface;
import legend.core.platform.input.InputAction;
import legend.core.renderer.android.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Experimental host backend. Input is submitted by an Activity and drained by the hardware thread. */
public final class AndroidPlatformManager extends NoopPlatformManager {
 final Object surfaceLock=new Object();
 Surface surface;
 long surfaceGeneration;
 private final AndroidShader.Source shaders;
 private AndroidWindow window;
 private final ConcurrentLinkedQueue<Runnable> input=new ConcurrentLinkedQueue<>();
 private final Set<InputAction> held=new HashSet<>(),pressed=new HashSet<>(),repeat=new HashSet<>();
 private final Map<InputAction,Float> axes=new HashMap<>();
 public AndroidPlatformManager(Surface surface,AndroidShader.Source shaders){this.surface=Objects.requireNonNull(surface);this.shaders=Objects.requireNonNull(shaders);}
 @Override protected Window createWindow(String title,int w,int h){if(window!=null)throw new IllegalStateException("Only one Android window is supported");synchronized(surfaceLock){return window=new AndroidWindow(this,surface,new AndroidRenderApi(shaders));}}
 @Override public Window getLastWindow(){return window;}
 @Override public void destroy(){super.destroy();if(window!=null){try{window.destroy();}finally{window.release();window=null;}}input.clear();held.clear();pressed.clear();repeat.clear();axes.clear();}
 @Override protected void removeWindows(Collection<Window> removed){if(window!=null && removed.contains(window))stop();}
 @Override public boolean isContextCurrent(){return window!=null&&window.isCurrent();}
 @Override public void onEngineLoopStopped(Throwable cause){
  if(cause==null){android.util.Log.i("DragoonEngine","Engine loop returned normally");return;}
  android.util.Log.e("DragoonEngine","Engine loop failed before cleanup",cause);
  java.io.StringWriter text=new java.io.StringWriter();cause.printStackTrace(new java.io.PrintWriter(text));
  try{legend.core.TextFiles.writeString(legend.core.GamePaths.root().getParent().resolve("engine-error.txt"),text.toString());}catch(java.io.IOException e){cause.addSuppressed(e);}
 }
 @Override public String[] listDisplays(){return new String[]{"Android Surface"};}
 /** Synchronous handoff: no draw can touch the old Surface after this returns. */
 public void setSurface(Surface value){synchronized(surfaceLock){surface=value;surfaceGeneration++;}}
 public void submitPointer(double x,double y,int action){input.add(()->{if(window==null)return;window.events().onMouseMove(x,y);if(action==0)window.events().onMousePress(0,java.util.Set.of());else if(action==1||action==3)window.events().onMouseRelease(0,java.util.Set.of());});}
 public void submitScroll(double amount){input.add(()->{if(window!=null)window.events().onMouseScroll(0,amount);});}
 public void submitCommand(Runnable command){input.add(java.util.Objects.requireNonNull(command));}
 public void submitButton(InputAction action,boolean down,boolean repeating){input.add(()->{if(down){if(held.add(action))pressed.add(action);if(repeating)repeat.add(action);if(window!=null)window.events().onInputActionPressed(action,repeating);}else if(held.remove(action)&&window!=null)window.events().onInputActionReleased(action);});}
 public void submitAxis(InputAction action,float value){if(!Float.isFinite(value))throw new IllegalArgumentException("Invalid axis");input.add(()->axes.put(action,Math.max(-1,Math.min(1,value))));}
 public void submitFocus(boolean focused){input.add(()->{if(!focused)resetActionStates();if(window!=null)window.setFocused(focused);legend.core.GameEngine.AUDIO_THREAD.setHostPaused(!focused);});}
 public void submitResize(int width,int height){input.add(()->{if(window!=null)window.resize(width,height);});}
 @Override protected void tickInput(){Runnable task;while((task=input.poll())!=null)task.run();}
 @Override public boolean isActionPressed(InputAction a){return pressed.contains(a);}
 @Override public boolean isActionRepeat(InputAction a){return repeat.contains(a);}
 @Override public boolean isActionHeld(InputAction a){return held.contains(a);}
 @Override public float getAxis(InputAction a){return axes.getOrDefault(a,0f);}
 @Override public void clearPressed(){pressed.clear();repeat.clear();}
 @Override public void resetActionStates(){for(InputAction action:held)if(window!=null)window.events().onInputActionReleased(action);held.clear();pressed.clear();repeat.clear();axes.clear();}
}
