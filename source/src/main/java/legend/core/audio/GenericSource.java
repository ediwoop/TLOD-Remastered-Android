package legend.core.audio;

import java.nio.ByteBuffer;

public class GenericSource extends AudioSource {
  private final int format;
  private final int sampleRate;
  private volatile boolean ready;

  public GenericSource(final int format, final int sampleRate) {
    this(format,sampleRate,true);
  }

  public GenericSource(final int format, final int sampleRate, final boolean autoStart) {
    super(16);
    this.ready=autoStart;
    this.format = format;
    this.sampleRate = sampleRate;
  }

  public synchronized void startPlayback(){this.ready=true;this.setActive(true);}

  @Override
  protected void init() {
    this.setActive(this.ready);
    super.init();
  }

  public void bufferOutput(final ByteBuffer buffer) {
    this.setActive(this.ready);
    super.bufferOutput(this.format, buffer, this.sampleRate);
  }

  public void bufferOutput(final short[] buffer) {
    this.setActive(this.ready);
    super.bufferOutput(this.format, buffer, this.sampleRate);
  }
}
