package legend.game.unpacker.scripts;

import org.legendofdragoon.scripting.tokens.Script;

public interface ScriptOpMatcher {
  boolean matches(Script script, int offset);
}
