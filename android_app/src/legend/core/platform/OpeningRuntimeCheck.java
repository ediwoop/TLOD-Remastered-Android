package legend.core.platform;

import android.util.Log;
import java.util.Deque;
import java.util.function.BiConsumer;
import legend.core.Config;
import legend.core.GamePaths;
import legend.game.EngineStates;
import legend.game.SItem;
import legend.game.fmv.Fmv;
import legend.game.inventory.screens.NewCampaignScreen;
import legend.game.modding.coremod.CoreMod;
import legend.game.submap.SMap;
import legend.game.submap.SubmapState;
import legend.game.title.Ttle;
import org.joml.Vector3f;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;

/** Optional internal device check, enabled only by the verify_opening Activity extra. */
public final class OpeningRuntimeCheck {
  private OpeningRuntimeCheck() { }
  public static void install(AndroidPlatformManager platform, int speed, BiConsumer<String, Boolean> button) {
    try {
      int originalSpeed=Config.getGameSpeedMultiplier();
      Config.setGameSpeedMultiplier(Math.max(1,Math.min(8,speed)));
      var screens = SItem.menuStack.getClass().getDeclaredField("screens");
      screens.setAccessible(true);
      var titleSelection=Ttle.class.getDeclaredField("selectedMenuOption");
      titleSelection.setAccessible(true);
      platform.addAction(new Action(new Runnable() {
        long lastFrame, nextInput;
        int titleFrames, campaignStep, loadedFrames, freeFrames, direction, movementFrames;
        boolean done;
        String heldDirection;
        SMap movementMap;
        final Vector3f movementStart=new Vector3f();
        final String[] directions={"→","↓","←","↑"};
        void finish(String report) throws java.io.IOException {
          if(heldDirection!=null)button.accept(heldDirection,false);
          Config.setGameSpeedMultiplier(originalSpeed);
          done=true;
          legend.core.TextFiles.writeString(GamePaths.resolve("interaction-runtime-check.txt"),report);
          Log.i("DragoonOpeningCheck",report);
        }
        public void run() {
          if(done || !(platform.getLastWindow() instanceof AndroidWindow window))return;
          long frame=window.framesDrawn();
          if(frame==lastFrame)return;
          lastFrame=frame;
          try {
            Object top=((Deque<?>)screens.get(SItem.menuStack)).peek();
            var state=EngineStates.currentEngineState_8004dd04;
            boolean controlsBlocked=gameState_800babc8==null || gameState_800babc8.indicatorsDisabled_4e3;
            if(frame%120==0)Log.i("DragoonOpeningCheck","frame="+frame+" state="+state+" menu="+top+" fmv="+Fmv.isPlaying+" controlsBlocked="+controlsBlocked);
            boolean loaded=state instanceof SMap smap && !Fmv.isPlaying && smap.smapLoadingStage_800cb430==SubmapState.RENDER_SUBMAP_12 && smap.submapControllerState_800c6740!=null && smap.submap!=null && !smap.submap.objects.isEmpty();
            if(loaded && ++loadedFrames==300) {
              String report="PASS: Android Surface rendered 300 loaded opening-submap frames; cut="+legend.game.Scus94491BpeSegment_8005.submapCut_80052c30+". Verification skips FMVs; movement is checked separately.";
              legend.core.TextFiles.writeString(GamePaths.resolve("opening-runtime-check.txt"),report);
              Log.i("DragoonOpeningCheck",report);
            }
            if(heldDirection!=null) {
              if(!loaded || state!=movementMap || controlsBlocked) {
                button.accept(heldDirection,false);heldDirection=null;freeFrames=0;
              } else if(++movementFrames>=24) {
                button.accept(heldDirection,false);heldDirection=null;
                Vector3f end=movementMap.sobjs_800c6880[0].innerStruct_00.model_00.coord2_14.coord.transfer;
                if(end.distanceSquared(movementStart)>1.0f) {
                  finish("PASS: native Android player moved through the same input path as the touch controls; cut="+legend.game.Scus94491BpeSegment_8005.submapCut_80052c30+" direction="+directions[direction]+" from="+movementStart+" to="+end+". FMVs skipped; verification speed="+speed+".");
                  return;
                }
                direction++;
                if(direction==directions.length){finish("UNVERIFIED: all four movement directions were submitted without observed displacement.");return;}
                freeFrames=0;
              }
              return;
            }
            if(loaded && !controlsBlocked) {
              if(++freeFrames>=30) {
                SMap smap=(SMap)state;
                if(smap.sobjs_800c6880[0]!=null) {
                  movementMap=smap;movementStart.set(smap.sobjs_800c6880[0].innerStruct_00.model_00.coord2_14.coord.transfer);
                  movementFrames=0;heldDirection=directions[direction];button.accept(heldDirection,true);
                  Log.i("DragoonOpeningCheck","Testing touch movement "+heldDirection+" from="+movementStart);
                  return;
                }
              }
            } else freeFrames=0;
            if(frame>=nextInput) {
              if(Fmv.isPlaying) {
                button.accept("X",true);button.accept("X",false);
                nextInput=frame+60;
              } else if(top instanceof NewCampaignScreen) {
                window.events().onInputActionPressed(campaignStep++==0?CoreMod.INPUT_ACTION_MENU_BOTTOM.get():CoreMod.INPUT_ACTION_MENU_CONFIRM.get(),false);
                nextInput=frame+30;
              } else if(top==null && state instanceof Ttle && ++titleFrames>=60) {
                if(titleSelection.getInt(state)>0) {
                  window.events().onInputActionPressed(CoreMod.INPUT_ACTION_MENU_UP.get(),false);
                  nextInput=frame+30;
                } else {
                  button.accept("X",true);button.accept("X",false);
                  nextInput=frame+60;
                }
              } else if(loaded) {
                button.accept("X",true);button.accept("X",false);
                nextInput=frame+60;
              }
            }
            if(loadedFrames>24000)finish("UNVERIFIED: opening rendered 24000 loaded frames without verified free movement.");
          } catch(Exception failure) {
            if(heldDirection!=null)button.accept(heldDirection,false);
            Config.setGameSpeedMultiplier(originalSpeed);
            done=true;
            Log.e("DragoonOpeningCheck","Opening check failed",failure);
          }
        }
      },60));
      Log.i("DragoonOpeningCheck","Installed optional opening check; speed="+speed);
    } catch(ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
  }
}
