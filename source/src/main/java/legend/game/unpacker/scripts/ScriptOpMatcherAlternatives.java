package legend.game.unpacker.scripts;

import org.legendofdragoon.scripting.tokens.Script;

public class ScriptOpMatcherAlternatives implements ScriptOpMatcher {
  private final ScriptOpMatcher[] alternatives;

  public ScriptOpMatcherAlternatives(final  ScriptOpMatcher... alternatives) {
    this.alternatives = alternatives;
  }

  @Override
  public boolean matches(final Script script, final int offset) {
    for(final ScriptOpMatcher matcher : this.alternatives) {
      if(matcher.matches(script, offset)) {
        return true;
      }
    }

    return false;
  }
}
