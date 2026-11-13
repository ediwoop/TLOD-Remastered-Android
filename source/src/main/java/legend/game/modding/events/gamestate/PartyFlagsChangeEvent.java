package legend.game.modding.events.gamestate;

import legend.game.characters.CharacterData2c;
import legend.game.types.GameState52c;

/**
 * Override scripted party flag changes
 */
public class PartyFlagsChangeEvent extends GameStateEvent {
  /** The index of the character in {@link GameState52c#charData_32c} */
  public int characterIndex;

  /** See {@link CharacterData2c#partyFlags_04} */
  public int partyFlags;

  public PartyFlagsChangeEvent(final GameState52c gameState, final int characterIndex, final int partyFlags) {
    super(gameState);
    this.characterIndex = characterIndex;
    this.partyFlags = partyFlags;
  }
}
