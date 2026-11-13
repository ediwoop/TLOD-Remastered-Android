package legend.game.modding.events.gamestate;

import legend.game.types.GameState52c;

/**
 * Override scripted active party changes
 */
public class PrimaryPartyChangeEvent extends GameStateEvent {
  /** The active party array index */
  public int activePartySlot;

  /** The index of the character in {@link GameState52c#charData_32c} or -1 to remove */
  public int characterIndex;

  public PrimaryPartyChangeEvent(final GameState52c gameState, final int activePartySlot, final int characterIndex) {
    super(gameState);
    this.activePartySlot = activePartySlot;
    this.characterIndex = characterIndex;
  }
}
