package legend.game.unpacker.scripts;

import org.legendofdragoon.scripting.OpType;
import org.legendofdragoon.scripting.ParameterType;
import org.legendofdragoon.scripting.tokens.Entry;
import org.legendofdragoon.scripting.tokens.Op;
import org.legendofdragoon.scripting.tokens.Param;
import org.legendofdragoon.scripting.tokens.Script;

import java.util.List;
import java.util.function.Predicate;

public class ScriptOpMatcherBasic implements ScriptOpMatcher {
  private final OpType opType;
  private final List<Predicate<Param>> params;

  @SafeVarargs
  public ScriptOpMatcherBasic(final OpType opType, final Predicate<Param>... params) {
    this.opType = opType;
    this.params = List.of(params);
  }

  @Override
  public boolean matches(final Script script, final int offset) {
    final Entry entry = script.entries[offset];

    if(!(entry instanceof final Op op)) {
      return false;
    }

    if(op.type != this.opType) {
      return false;
    }

    for(int i = 0; i < this.params.size(); i++) {
      if(!this.params.get(i).test(op.params[i])) {
        return false;
      }
    }

    return true;
  }

  public static Predicate<Param> any() {
    return param -> true;
  }

  public static Predicate<Param> imm(final int val) {
    return param -> (param.type == ParameterType.IMMEDIATE || param.type == ParameterType.NEXT_IMMEDIATE) && param.resolvedValue.isPresent() && param.resolvedValue.get() == val;
  }

  public static Predicate<Param> var() {
    return param -> param.type == ParameterType.GAMEVAR_1 || param.type == ParameterType.GAMEVAR_2 || param.type == ParameterType.GAMEVAR_3 || param.type == ParameterType.GAMEVAR_ARRAY_1 || param.type == ParameterType.GAMEVAR_ARRAY_2 || param.type == ParameterType.GAMEVAR_ARRAY_3 || param.type == ParameterType.GAMEVAR_ARRAY_4 || param.type == ParameterType.GAMEVAR_ARRAY_5 || param.type == ParameterType.GAMEVAR_INL;
  }
}
