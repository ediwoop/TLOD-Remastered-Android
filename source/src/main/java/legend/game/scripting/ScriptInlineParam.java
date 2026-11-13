package legend.game.scripting;

import legend.core.QueuePool;

public class ScriptInlineParam extends Param {
  private final QueuePool<Param> pool;
  private ScriptState<?> state;
  private int offset;

  public ScriptInlineParam(final QueuePool<Param> pool) {
    this.pool = pool;
  }

  protected ScriptInlineParam init(final ScriptState<?> state, final int offset) {
    this.state = state;
    this.offset = offset;
    return this;
  }

  @Override
  public void jump(final RunningScript<?> script) {
    script.scriptState_04.replaceFrame(this.state.frame().copy()).offset = this.offset;
    script.commandOffset_0c = this.offset;
  }

  @Override
  public void jump(final ScriptState<?> state) {
    state.replaceFrame(this.state.frame().copy()).offset = this.offset;
  }

  @Override
  public int get() {
    return this.state.frame().file.getOp(this.offset);
  }

  @Override
  public Param set(final int val) {
    // Apparently this is possible
    this.state.frame().file.setOp(this.offset, val);
    return this;
  }

  @Override
  public Param array(final int index) {
    return this.pool.acquire(ScriptInlineParam.class).init(this.state, this.offset + index);
  }

  @Override
  public String toString() {
    return "script[%d].inl[0x%x] 0x%x".formatted(this.state.index, this.offset * 4, this.get());
  }
}
