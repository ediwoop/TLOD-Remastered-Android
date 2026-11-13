package legend.game.unpacker.scripts;

import org.legendofdragoon.scripting.OpType;
import org.legendofdragoon.scripting.ParameterType;
import org.legendofdragoon.scripting.resolution.ResolvedValue;
import org.legendofdragoon.scripting.tokens.Entry;
import org.legendofdragoon.scripting.tokens.Op;
import org.legendofdragoon.scripting.tokens.Param;
import org.legendofdragoon.scripting.tokens.Script;

import java.util.List;
import java.util.function.Predicate;

public class ScriptOpMatcherZeroable implements ScriptOpMatcher {
  private final OpType opTypeStandard;
  private final OpType opTypeZero;
  private final List<Predicate<Param>> params;

  @SafeVarargs
  public ScriptOpMatcherZeroable(final OpType opTypeStandard, final OpType opTypeZero, final Predicate<Param>... params) {
    this.opTypeStandard = opTypeStandard;
    this.opTypeZero = opTypeZero;
    this.params = List.of(params);
  }

  @Override
  public boolean matches(final Script script, final int offset) {
    final Entry entry = script.entries[offset];

    if(!(entry instanceof final Op op)) {
      return false;
    }

    if(op.type != this.opTypeStandard && op.type != this.opTypeZero) {
      return false;
    }

    int paramIndex = 0;
    if(op.type == this.opTypeZero) {
      if(!this.params.get(paramIndex++).test(new Param(entry.address, ParameterType.INLINE_1, new int[] {0}, ResolvedValue.of(0), null))) {
        return false;
      }
    }

    for(int i = 0; i < op.params.length; i++) {
      if(!this.params.get(paramIndex++).test(op.params[i])) {
        return false;
      }
    }

    return true;
  }
}
