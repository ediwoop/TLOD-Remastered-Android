package legend.core.platform;

import android.opengl.*;
import android.view.Surface;
import legend.core.platform.input.InputClass;
import legend.core.renderer.RenderApi;
import java.nio.file.Path;

/** EGL window for the engine hardware thread. The Activity retains ownership of Surface. */
public final class AndroidWindow extends Window {
 private final AndroidPlatformManager manager;
 private final RenderApi api;
 private final Action render;
 private final EGLDisplay display;
 private EGLConfig config;
 private EGLSurface parking=EGL14.EGL_NO_SURFACE;
 private Surface attached;
 private long surfaceGeneration=-1;
 private EGLContext context=EGL14.EGL_NO_CONTEXT;
 private EGLSurface surface=EGL14.EGL_NO_SURFACE;
 private volatile boolean closed;
 private volatile boolean focused=true;
 private boolean deleted;
 private boolean released;
 private volatile long frameCount;
 private volatile int width,height;
 private boolean captured;
 private final java.util.Set<String> capturedStates=new java.util.HashSet<>();
 public long framesDrawn(){return frameCount;}
 public AndroidWindow(AndroidPlatformManager manager,Surface nativeSurface,RenderApi api){
  this.manager=manager;this.api=api;
  display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
  if(display==EGL14.EGL_NO_DISPLAY||!EGL14.eglInitialize(display,new int[2],0,new int[2],0))throw fail("EGL display");
  try{
   EGLConfig[] configs=new EGLConfig[1];int[] count=new int[1];
   int[] attrs={EGL14.EGL_RENDERABLE_TYPE,0x40,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT|EGL14.EGL_PBUFFER_BIT,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_DEPTH_SIZE,24,EGL14.EGL_STENCIL_SIZE,8,EGL14.EGL_NONE};
   if(!EGL14.eglChooseConfig(display,attrs,0,configs,0,1,count,0)||count[0]==0)throw fail("EGL configuration");
   config=configs[0];
   context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL15.EGL_CONTEXT_MINOR_VERSION,2,EGL14.EGL_NONE},0);
   if(context==EGL14.EGL_NO_CONTEXT)throw fail("GLES 3.2 EGL context");
   parking=EGL14.eglCreatePbufferSurface(display,config,new int[]{EGL14.EGL_WIDTH,1,EGL14.EGL_HEIGHT,1,EGL14.EGL_NONE},0);
   if(parking==EGL14.EGL_NO_SURFACE||!EGL14.eglMakeCurrent(display,parking,parking,context))throw fail("EGL parking surface");
   width=960;height=540;
   attach(nativeSurface);

  }catch(RuntimeException e){releaseEgl();throw e;}
  render=manager.addAction(new Action(this::draw,60));
 }
 private IllegalStateException fail(String step){return new IllegalStateException(step+" failed: 0x"+Integer.toHexString(EGL14.eglGetError()));}
 private void releaseEgl(){EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);if(surface!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,surface);if(parking!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,parking);if(context!=EGL14.EGL_NO_CONTEXT)EGL14.eglDestroyContext(display,context);EGL14.eglTerminate(display);}
 public boolean isCurrent(){return EGL14.eglGetCurrentContext().equals(context);}
 public RenderApi getRenderApi(){return api;}
 protected boolean shouldClose(){return closed;}
 protected void destroy(){if(deleted)return;deleted=true;manager.removeAction(render);events().onClose();}
 public void release(){if(released)return;released=true;releaseEgl();}
 private void attach(Surface target){
  park();
  if(target==null||!target.isValid())return;
  EGLSurface next=EGL14.eglCreateWindowSurface(display,config,target,new int[]{EGL14.EGL_NONE},0);
  if(next==EGL14.EGL_NO_SURFACE){android.util.Log.w("DragoonLifecycle","Surface not ready; retrying");return;}
  if(!EGL14.eglMakeCurrent(display,next,next,context)){EGL14.eglDestroySurface(display,next);throw fail("EGL reattach");}
  surface=next;attached=target;EGL14.eglSwapInterval(display,1);
  resize(dimension(EGL14.EGL_WIDTH),dimension(EGL14.EGL_HEIGHT));
  android.util.Log.i("DragoonLifecycle","Surface attached; context retained");
 }
 private void park(){
  if(!EGL14.eglMakeCurrent(display,parking,parking,context))throw fail("EGL park");
  if(surface!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,surface);
  surface=EGL14.EGL_NO_SURFACE;attached=null;
 }
 private void draw(){synchronized(manager.surfaceLock){
  if(closed)return;
  if(manager.surfaceGeneration!=surfaceGeneration||manager.surface!=attached){attach(manager.surface);surfaceGeneration=manager.surfaceGeneration;}
  if(!focused||surface==EGL14.EGL_NO_SURFACE)return;events().onDraw();frameCount++;
  var state=legend.game.EngineStates.currentEngineState_8004dd04;
  String phase=legend.game.fmv.Fmv.isPlaying?"movie":state==null?"startup":state.getClass().getSimpleName();
  if(state instanceof legend.game.submap.SMap)phase+="-"+legend.game.Scus94491BpeSegment_8005.submapCut_80052c30;
  if(!capturedStates.contains(phase)||frameCount%120==0){java.nio.ByteBuffer pixel=java.nio.ByteBuffer.allocateDirect(4);long sum=0;int w=getWidth(),h=getHeight();for(int y=1;y<8;y++)for(int x=1;x<8;x++){GLES32.glReadPixels(x*w/8,y*h/8,1,1,GLES32.GL_RGBA,GLES32.GL_UNSIGNED_BYTE,pixel);sum+=(pixel.get(0)&255)+(pixel.get(1)&255)+(pixel.get(2)&255);}android.util.Log.i("DragoonFrame","Rendered frame="+frameCount+" phase="+phase+" colourSum="+sum+" glError="+GLES32.glGetError());
   if(!capturedStates.contains(phase)&&sum>0){try{java.nio.ByteBuffer raw=java.nio.ByteBuffer.allocateDirect(w*h*4);GLES32.glReadPixels(0,0,w,h,GLES32.GL_RGBA,GLES32.GL_UNSIGNED_BYTE,raw);java.nio.ByteBuffer flipped=java.nio.ByteBuffer.allocate(w*h*4);byte[] row=new byte[w*4];for(int i=h-1;i>=0;i--){raw.position(i*w*4);raw.get(row);flipped.put(row);}flipped.flip();byte[] png=legend.game.textures.PngWriter.compress(flipped,w,h);java.nio.file.Files.write(legend.core.GamePaths.resolve("render-proof-"+phase+".png"),png);if(!captured)java.nio.file.Files.write(legend.core.GamePaths.resolve("render-proof.png"),png);captured=true;capturedStates.add(phase);}catch(java.io.IOException e){android.util.Log.e("DragoonFrame","Could not save frame proof",e);}}
}
  manager.clearPressed();if(!EGL14.eglSwapBuffers(display,surface)){
   int error=EGL14.eglGetError();
   if(error==EGL14.EGL_BAD_SURFACE||error==EGL14.EGL_BAD_NATIVE_WINDOW){park();android.util.Log.w("DragoonLifecycle","Surface unavailable; waiting for recreation");}
   else throw new IllegalStateException("EGL swap failed: 0x"+Integer.toHexString(error));
  }
 }}
 public void show(){events().onResize(getWidth(),getHeight());}
 public void close(){if(!closed)android.util.Log.i("DragoonWindow","Window close requested",new Throwable("Close caller"));closed=true;}
 public void updateMonitor(){events().onResize(getWidth(),getHeight());}
 public void makeFullscreen(){} public void makeWindowed(){} public void centerWindow(){} public void setTitle(String title){}
 private int dimension(int attr){int[] n=new int[1];if(!EGL14.eglQuerySurface(display,surface,attr,n,0))throw fail("EGL dimensions");return n[0];}
 public int getWidth(){return width;} public int getHeight(){return height;}
 public void resize(int w,int h){if(w<=0||h<=0||(w==width&&h==height))return;width=w;height=h;events().onResize(w,h);}
 public boolean hasFocus(){return focused;}
 public void setFocused(boolean value){focused=value;}
 public InputClass getInputClass(){return InputClass.GAMEPAD;}
 public void startTextInput(){} public void stopTextInput(){} public void disableCursor(){} public void showCursor(){} public void hideCursor(){} public void useNormalCursor(){} public void usePointerCursor(){} public void setWindowIcon(Path path){}
 public void setFpsLimit(int hz){render.setExpectedFps(hz);} public int getFpsLimit(){return render.getExpectedFps();}
}
