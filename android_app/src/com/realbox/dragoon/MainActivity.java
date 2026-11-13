package com.realbox.dragoon;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.content.res.AssetManager;
import java.nio.file.*;
import java.io.*;
import legend.core.android.AndroidEngineHost;

/** Internal engine integration harness; not a playable release. */
public final class MainActivity extends Activity implements SurfaceHolder.Callback {
 static {System.setProperty("log4j.provider","org.apache.logging.log4j.simple.internal.SimpleProvider");System.setProperty("org.apache.logging.log4j.simplelog.level","INFO");}
 private TextView status;
 private PlayStationControls controls;
 private volatile AndroidEngineHost host;
 private volatile boolean surfaceReady;
 private boolean engineStarted;
 private boolean resumed;
 public void onCreate(Bundle state) {
  super.onCreate(state);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  legend.core.renderer.RemasterMode.setEnabled(getPreferences(MODE_PRIVATE).getBoolean("remastered",true));
  getWindow().setDecorFitsSystemWindows(false);
  WindowManager.LayoutParams window=getWindow().getAttributes();
  window.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
  getWindow().setAttributes(window);getWindow().setNavigationBarContrastEnforced(false);
  GameStage stage=new GameStage(this);
  FrameLayout root=stage.content();
  SurfaceView surface=new SurfaceView(this);surface.getHolder().addCallback(this);
  root.addView(surface,new FrameLayout.LayoutParams(-1,-1));
  final float[] pointerY={0};
  surface.setOnTouchListener((view,event)->{
   AndroidEngineHost engine=host;if(engine==null||engine.platform()==null)return false;
   int action=event.getActionMasked();
   if(action==MotionEvent.ACTION_DOWN)pointerY[0]=event.getY();
   if(action==MotionEvent.ACTION_MOVE&&legend.game.EngineStates.currentEngineState_8004dd04 instanceof legend.game.title.Ttle){float delta=event.getY()-pointerY[0];if(Math.abs(delta)>=8){engine.platform().submitScroll(delta/24.0);pointerY[0]=event.getY();}}
   engine.platform().submitPointer(event.getX(),event.getY(),action);return true;
  });
  status=new TextView(this);status.setTextColor(0xffffffff);status.setBackgroundColor(0xaa000000);
  status.setText("Preparando el motor…");root.addView(status,new FrameLayout.LayoutParams(-1,-2));
  controls=new PlayStationControls(this,
   (name,down)->EngineInput.button(host==null?null:host.platform(),name,down),
   this::toggleVisualMode,this::showGraphicsDiagnostic);
  root.addView(controls,new FrameLayout.LayoutParams(-1,-1));
  updateVisualModeLabel();setContentView(stage);hideSystemBars();
  new Thread(()->{while(!isFinishing()){AndroidEngineHost h=host;legend.core.platform.AndroidPlatformManager p=h==null?null:h.platform();if(p!=null&&p.getLastWindow() instanceof legend.core.platform.AndroidWindow w&&w.framesDrawn()>0){if(getIntent().getBooleanExtra("verify_opening",false))p.submitCommand(()->legend.core.platform.OpeningRuntimeCheck.install(p,getIntent().getIntExtra("verify_speed",1),(name,down)->EngineInput.button(p,name,down)));runOnUiThread(()->status.setVisibility(View.GONE));return;}android.os.SystemClock.sleep(250);}},"DragoonStatus").start();
 }
 private void showGraphicsDiagnostic() {
  String report=legend.core.renderer.android.AndroidRenderApi.graphicsDiagnostic();
  new android.app.AlertDialog.Builder(this).setTitle("Diagnostico grafico").setMessage(report)
   .setPositiveButton("Cerrar",(d,which)->hideSystemBars())
   .setNeutralButton("Copiar",(d,which)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("Dragoon",report));hideSystemBars();}).show();
 }
 private void updateVisualModeLabel(){controls.setRemastered(legend.core.renderer.RemasterMode.isEnabled());}
 private void toggleVisualMode(){boolean enabled=!legend.core.renderer.RemasterMode.isEnabled();legend.core.renderer.RemasterMode.setEnabled(enabled);getPreferences(MODE_PRIVATE).edit().putBoolean("remastered",enabled).apply();updateVisualModeLabel();}
 public boolean dispatchKeyEvent(KeyEvent event){if(event.getKeyCode()==KeyEvent.KEYCODE_BUTTON_SELECT||event.getKeyCode()==KeyEvent.KEYCODE_TAB){if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)toggleVisualMode();return true;}if(host!=null&&host.platform()!=null&&(event.getAction()==KeyEvent.ACTION_DOWN||event.getAction()==KeyEvent.ACTION_UP)&&EngineInput.key(host.platform(),event.getKeyCode(),event.getAction()==KeyEvent.ACTION_DOWN))return true;return super.dispatchKeyEvent(event);}
 private void copy(String relative,Path root)throws IOException{
  AssetManager assets=getAssets();String[] children=assets.list(relative);
  if(children!=null && children.length>0){Files.createDirectories(root.resolve(relative));for(String child:children)copy(relative+"/"+child,root);}
  else{Path target=root.resolve(relative);Files.createDirectories(target.getParent());try(InputStream in=assets.open(relative)){Files.copy(in,target,StandardCopyOption.REPLACE_EXISTING);}}
 }
 private void installEngineAssets(Path root)throws IOException{
  String expected;try(InputStream in=getAssets().open("engine-assets.sha256")){expected=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();}
  Path marker=root.resolve("engine-assets.sha256");
  if(Files.isRegularFile(marker)&&legend.core.TextFiles.readString(marker).equals(expected)&&Files.isRegularFile(root.resolve("gfx/fonts/default.json"))&&Files.isRegularFile(root.resolve("gfx/fonts/smooth.json"))&&Files.isRegularFile(root.resolve("gfx/ui/title_menu.png"))&&Files.isRegularFile(root.resolve("android-shaders/tmd.gsh"))&&Files.isDirectory(root.resolve("resources/lod_core/lang")))return;
  for(String dir:new String[]{"gfx","patches","resources","android-shaders"})copy(dir,root);
  RetiredBackgroundAssets.remove(root);
  legend.core.TextFiles.writeString(marker,expected);
 }
 public void surfaceCreated(SurfaceHolder holder){android.util.Log.i("DragoonLifecycle","surfaceCreated");surfaceReady=true;if(host==null)host=new AndroidEngineHost();host.setSurface(holder.getSurface());host.setFocused(resumed);if(engineStarted)return;engineStarted=true;final AndroidEngineHost engine=host;new Thread(()->{
  Path root=getFilesDir().toPath().resolve("game");
  try{Files.createDirectories(root);installEngineAssets(root);Files.createDirectories(root.resolve("lang"));PalDataInstaller.install(getAssets(),root,(done,total)->runOnUiThread(()->status.setText("Preparando juego… "+(done*100L/total)+"%")));runOnUiThread(()->status.setText("Iniciando el juego…"));
   engine.run(holder.getSurface(),root);runOnUiThread(()->status.setText("Motor detenido"));
  }catch(Throwable t){StringWriter sw=new StringWriter();t.printStackTrace(new PrintWriter(sw));String error=sw.toString();android.util.Log.e("DragoonEngine",error);try{legend.core.TextFiles.writeString(getFilesDir().toPath().resolve("engine-error.txt"),error);}catch(IOException ignored){}runOnUiThread(()->{status.setVisibility(View.VISIBLE);status.setText(error);});checkFailedStartup();}
 },"DragoonEngine").start();}
 private void checkFailedStartup(){new Thread(()->{
  android.os.SystemClock.sleep(2000);
  String report;
  try{
   Class<?> core=Class.forName("legend.core.GameEngine",false,getClassLoader());Object audio=core.getField("AUDIO_THREAD").get(null);
   java.lang.reflect.Field initialized=audio.getClass().getDeclaredField("initialized");initialized.setAccessible(true);
   int unpackers=0;for(StackTraceElement[] stack:Thread.getAllStackTraces().values())for(StackTraceElement frame:stack)if(frame.getClassName().equals("legend.game.unpacker.Unpacker")){unpackers++;break;}
   boolean audioOpen=initialized.getBoolean(audio);
   report=(!audioOpen&&unpackers==0?"PASS":"FAIL")+": failed startup cleanup: audioInitialized="+audioOpen+", unpackerThreads="+unpackers;
  }catch(Throwable checkError){report="UNVERIFIED: "+checkError;}
  android.util.Log.i("DragoonCleanup",report);
  try{legend.core.TextFiles.writeString(getFilesDir().toPath().resolve("startup-cleanup.txt"),report);}catch(IOException ignored){}
 },"DragoonCleanupCheck").start();}
 public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){AndroidEngineHost h=host;if(h!=null&&h.platform()!=null)h.platform().submitResize(width,height);}
 public void surfaceDestroyed(SurfaceHolder holder){android.util.Log.i("DragoonLifecycle","surfaceDestroyed");surfaceReady=false;if(host!=null)host.setSurface(null);}
 protected void onPause(){android.util.Log.i("DragoonLifecycle","onPause");resumed=false;super.onPause();if(controls!=null)controls.releaseAll();if(host!=null)host.setFocused(false);}
 protected void onDestroy(){if(isFinishing()&&host!=null)host.requestClose();super.onDestroy();}
 private void hideSystemBars(){WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null){controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);controller.hide(WindowInsets.Type.systemBars());}}
 public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)hideSystemBars();}
 protected void onResume(){android.util.Log.i("DragoonLifecycle","onResume");super.onResume();resumed=true;hideSystemBars();if(host!=null)host.setFocused(true);}
}
