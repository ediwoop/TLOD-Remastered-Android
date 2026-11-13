package com.realbox.dragoon;
import android.view.KeyEvent;
import legend.core.platform.AndroidPlatformManager;
import legend.game.modding.coremod.CoreMod;
import legend.lodmod.LodMod;
final class EngineInput {
 static boolean key(AndroidPlatformManager p,int code,boolean down){String name=switch(code){
  case KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_A->"←";case KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_D->"→";
  case KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_W->"↑";case KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_S->"↓";
  case KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_SPACE->"X";
  case KeyEvent.KEYCODE_BUTTON_X->"Square";case KeyEvent.KEYCODE_BUTTON_Y->"Triangle";
  case KeyEvent.KEYCODE_BUTTON_L1->"L1";case KeyEvent.KEYCODE_BUTTON_R1->"R1";case KeyEvent.KEYCODE_BUTTON_L2->"L2";case KeyEvent.KEYCODE_BUTTON_R2->"R2";
  case KeyEvent.KEYCODE_BUTTON_B,KeyEvent.KEYCODE_BACK,KeyEvent.KEYCODE_ESCAPE->"O";case KeyEvent.KEYCODE_BUTTON_START->"Start";default->null;};
  if(name==null)return false;button(p,name,down);return true;
 }
 static void button(AndroidPlatformManager p,String name,boolean down){if(p==null)return;p.submitCommand(()->{
  switch(name){
   case "←"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_LEFT.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_GENERAL_MOVE_LEFT.get(),down,false);p.submitAxis(LodMod.INPUT_ACTION_GENERAL_MOVE_LEFT.get(),down?-1:0);}
   case "→"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_RIGHT.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_GENERAL_MOVE_RIGHT.get(),down,false);p.submitAxis(LodMod.INPUT_ACTION_GENERAL_MOVE_RIGHT.get(),down?1:0);}
   case "↑"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_UP.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_GENERAL_MOVE_UP.get(),down,false);p.submitAxis(LodMod.INPUT_ACTION_GENERAL_MOVE_UP.get(),down?-1:0);}
   case "↓"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_DOWN.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_GENERAL_MOVE_DOWN.get(),down,false);p.submitAxis(LodMod.INPUT_ACTION_GENERAL_MOVE_DOWN.get(),down?1:0);}
   case "X"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_CONFIRM.get(),down,false);p.submitButton(CoreMod.INPUT_ACTION_MENU_TEXTBOX_CONFIRM.get(),down,false);p.submitButton(CoreMod.INPUT_ACTION_FMV_SKIP.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_SMAP_INTERACT.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_BTTL_ATTACK.get(),down,false);}
   case "O"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_BACK.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_GENERAL_RUN.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_BTTL_COUNTER.get(),down,false);}
   case "Square"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_DELETE.get(),down,false);p.submitButton(CoreMod.INPUT_ACTION_MENU_UNEQUIP.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_WMAP_QUEEN_FURY_COOLON.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_WMAP_SERVICES.get(),down,false);}
   case "Triangle"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_SORT.get(),down,false);p.submitButton(CoreMod.INPUT_ACTION_MENU_MODS.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_GENERAL_OPEN_INVENTORY.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_BTTL_ADDITIONS.get(),down,false);}
   case "L1"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_PAGE_UP.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_WMAP_ROTATE_LEFT.get(),down,false);}
   case "R1"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_PAGE_DOWN.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_WMAP_ROTATE_RIGHT.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_SMAP_TOGGLE_INDICATORS.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_BTTL_ROTATE_CAMERA.get(),down,false);}
   case "L2"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_HOME.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_WMAP_ZOOM_OUT.get(),down,false);p.submitAxis(LodMod.INPUT_ACTION_WMAP_ZOOM_OUT.get(),down?1:0);}
   case "R2"->{p.submitButton(CoreMod.INPUT_ACTION_MENU_END.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_WMAP_ZOOM_IN.get(),down,false);p.submitAxis(LodMod.INPUT_ACTION_WMAP_ZOOM_IN.get(),down?1:0);}
   case "Start"->{p.submitButton(LodMod.INPUT_ACTION_GENERAL_OPEN_INVENTORY.get(),down,false);p.submitButton(CoreMod.INPUT_ACTION_MENU_HELP.get(),down,false);p.submitButton(LodMod.INPUT_ACTION_BTTL_OPTIONS.get(),down,false);}
  }
 });}
}
