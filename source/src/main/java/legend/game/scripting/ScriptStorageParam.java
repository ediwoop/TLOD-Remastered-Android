package legend.game.scripting;

import legend.core.QueuePool;

public class ScriptStorageParam extends Param {
  private final QueuePool<Param> paramPool;
  private ScriptState<?> state;
  private int index;

  public ScriptStorageParam(final QueuePool<Param> paramPool) {
    this.paramPool = paramPool;
  }

  public ScriptStorageParam init(final ScriptState<?> state, final int index) {
    this.state = state;
    this.index = index;
    return this;
  }

  @Override
  public int get() {
    return this.state.getStor(this.index);
  }

  @Override
  public Param set(final int val) {
    this.state.setStor(this.index, val);
    return this;
  }

  @Override
  public Param array(final int index) {
    return this.paramPool.acquire(ScriptStorageParam.class).init(this.state, this.index + index);
  }

  @Override
  public float getFloat() {
    return this.state.getStorFloat(this.index);
  }

  @Override
  public Param set(final float val) {
    this.state.setStor(this.index, val);
    return this;
  }

  @Override
  public boolean isFloat() {
    return this.state.isStorFloat(this.index);
  }

  @Override
  public String toString() {
    if(this.isFloat()) {
      return "script[%d].stor[%d] %.2f".formatted(this.state.index, this.index, this.getFloat());
    }

    return "script[%d].stor[%d] 0x%x".formatted(this.state.index, this.index, this.get());
  }
}
