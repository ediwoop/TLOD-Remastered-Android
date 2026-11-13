package legend.game.inventory.screens;

import legend.core.lang.RawText;
import legend.game.inventory.screens.controls.Background;
import legend.game.inventory.screens.controls.Button;
import static legend.game.FullScreenEffects.startFadeEffect;
import static legend.game.modding.coremod.CoreMod.INPUT_ACTION_MENU_BACK;

/** Title-only hub; child screens return here instead of unloading the title menu. */
public class TitleOptionsScreen extends VerticalLayoutScreen {
  public TitleOptionsScreen(final Runnable unload) {
    this.addControl(new Background());
    this.addHotkey(new RawText("Back"), INPUT_ACTION_MENU_BACK, unload);
    this.addEntry("Settings", () -> this.getStack().pushScreen(new OptionsPresetsScreen((index, presets) -> this.returnFromChild())));
    this.addEntry("Character Viewer", () -> this.getStack().pushScreen(new VisorScreen(VisorScreen.Kind.CHARACTERS, this::returnFromChild)));
    this.addEntry("Background Viewer", () -> this.getStack().pushScreen(new VisorScreen(VisorScreen.Kind.BACKGROUNDS, this::returnFromChild)));
    this.addEntry("About", () -> this.getStack().pushScreen(new AboutScreen(this::returnFromChild)));
    this.addEntry("Back", unload);
    startFadeEffect(2, 10);
  }

  private void addEntry(final String title, final Runnable action) {
    final Button button = new Button(new RawText(title));
    this.addRow(RawText.BLANK, button);
    button.onPressed(() -> this.deferAction(action));
  }

  private void returnFromChild() {
    this.getStack().popScreen();
    startFadeEffect(2, 10);
  }

  @Override protected void render() { }
}
