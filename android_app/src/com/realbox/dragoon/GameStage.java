package com.realbox.dragoon;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/** Centres the game surface and touch controls in the active aspect ratio. */
final class GameStage extends FrameLayout {
  private final FrameLayout content;

  GameStage(Context context) {
    super(context);
    setBackgroundColor(0xff000000);
    content = new FrameLayout(context);
    addView(content);
  }

  FrameLayout content() { return content; }

  void updateAspectRatio() {
    requestLayout();
  }

  @Override protected void onMeasure(int widthSpec, int heightSpec) {
    int width = View.MeasureSpec.getSize(widthSpec);
    int height = View.MeasureSpec.getSize(heightSpec);
    int[] fit = VirtualPad.fit(width, height, legend.core.renderer.RemasterMode.isEnabled());
    content.measure(MeasureSpec.makeMeasureSpec(fit[0], MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(fit[1], MeasureSpec.EXACTLY));
    setMeasuredDimension(width, height);
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int x = (getMeasuredWidth() - content.getMeasuredWidth()) / 2;
    int y = (getMeasuredHeight() - content.getMeasuredHeight()) / 2;
    content.layout(x, y, x + content.getMeasuredWidth(), y + content.getMeasuredHeight());
  }
}
