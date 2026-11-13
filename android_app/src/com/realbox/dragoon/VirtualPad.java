package com.realbox.dragoon;

import java.util.*;
import java.util.function.BiConsumer;

/** Logical Kula canvas coordinates and finger ownership, independent of Android Views. */
final class VirtualPad {
  static final float LOWER = 112;
  static final float DPAD_X = 147, DPAD_Y = 455 + LOWER;
  enum Key { LEFT, UP, DOWN, RIGHT, CROSS, CIRCLE, SQUARE, TRIANGLE, L1, L2, R1, R2, SELECT, START, VIEW }
  record Button(Key key, float x, float y, float w, float h) {
    boolean contains(float px, float py) { return px >= x && py >= y && px < x+w && py < y+h; }
  }
  static final List<Button> BUTTONS = List.of(
    new Button(Key.LEFT,20,414+LOWER,82,82), new Button(Key.UP,106,328+LOWER,82,82),
    new Button(Key.DOWN,106,500+LOWER,82,82), new Button(Key.RIGHT,192,414+LOWER,82,82),
    new Button(Key.CROSS,1107,500+LOWER,82,82), new Button(Key.CIRCLE,1193,414+LOWER,82,82),
    new Button(Key.SQUARE,1021,414+LOWER,82,82), new Button(Key.TRIANGLE,1107,328+LOWER,82,82),
    new Button(Key.L1,52,244+LOWER,68,56), new Button(Key.L2,52,178+LOWER,68,56),
    new Button(Key.R1,1160,244+LOWER,68,56), new Button(Key.R2,1160,178+LOWER,68,56),
    new Button(Key.VIEW,792,18,180,42), new Button(Key.SELECT,512,650,112,42),
    new Button(Key.START,656,650,112,42));

  static int[] fit(int width, int height) {
    return fit(width, height, true);
  }

  static int[] fit(int width, int height, boolean remastered) {
    if(width <= 0 || height <= 0) return new int[]{0, 0};
    int ratioWidth = remastered ? 16 : 4;
    int ratioHeight = remastered ? 9 : 3;
    if((long)width * ratioHeight > (long)height * ratioWidth) {
      return new int[]{Math.max(1, Math.round((float)height * ratioWidth / ratioHeight)), height};
    }
    return new int[]{width, Math.max(1, Math.round((float)width * ratioHeight / ratioWidth))};
  }

  static Set<Key> hit(float x, float y) {
    // The cross also supports one-finger diagonals between its cardinal buttons.
    float dx=x-DPAD_X, dy=y-DPAD_Y;
    if(Math.abs(dx)<=127 && Math.abs(dy)<=127) {
      EnumSet<Key> result=EnumSet.noneOf(Key.class);
      if(dx < -34) result.add(Key.LEFT); else if(dx > 34) result.add(Key.RIGHT);
      if(dy < -34) result.add(Key.UP); else if(dy > 34) result.add(Key.DOWN);
      return result;
    }
    for(Button button:BUTTONS) if(button.contains(x,y)) return EnumSet.of(button.key());
    return EnumSet.noneOf(Key.class);
  }

  private final Map<Integer,Set<Key>> fingers = new HashMap<>();
  private final EnumMap<Key,Integer> owners = new EnumMap<>(Key.class);
  private final BiConsumer<Key,Boolean> changes;
  VirtualPad(BiConsumer<Key,Boolean> changes) { this.changes=changes; }
  boolean held(Key key) { return owners.getOrDefault(key,0)>0; }

  void move(int finger,float x,float y) {
    Set<Key> next=hit(x,y), prior=fingers.getOrDefault(finger,Set.of());
    for(Key key:prior) if(!next.contains(key)) release(key);
    for(Key key:next) if(!prior.contains(key)) {
      int count=owners.getOrDefault(key,0); owners.put(key,count+1);
      if(count==0) changes.accept(key,true);
    }
    fingers.put(finger,next);
  }
  void up(int finger) {
    Set<Key> prior=fingers.remove(finger);
    if(prior!=null) for(Key key:prior) release(key);
  }
  void clear() { for(Integer finger:new ArrayList<>(fingers.keySet())) up(finger); }
  private void release(Key key) {
    int remaining=owners.get(key)-1;
    if(remaining==0) { owners.remove(key); changes.accept(key,false); }
    else owners.put(key,remaining);
  }
}
