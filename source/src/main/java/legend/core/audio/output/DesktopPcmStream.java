package legend.core.audio.output;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_SEC_OFFSET;

/** OpenAL queue; allocation ownership stays separate from the reusable buffer stack. */
public final class DesktopPcmStream implements PcmStream {
  private final int[] ownedBuffers;
  private final int[] availableBuffers;
  private int available, source;
  private IntBuffer tmp;

  public DesktopPcmStream(final int count) {
    if(count < 1) { throw new IllegalArgumentException("Invalid queue size"); }
    this.ownedBuffers = new int[count];
    this.source = alGenSources();
    this.tmp = MemoryUtil.memAllocInt(1);
    alGenBuffers(this.ownedBuffers);
    this.availableBuffers = this.ownedBuffers.clone();
    this.available = count;
  }

  public boolean canBuffer() { return this.source != 0 && this.available > 0; }
  public void pump() {
    if(this.source == 0) { return; }
    alGetSourcei(this.source, AL_BUFFERS_PROCESSED, this.tmp);
    for(int count = this.tmp.get(0); count > 0; count--) {
      this.availableBuffers[this.available++] = alSourceUnqueueBuffers(this.source);
    }
  }
  public void write(final int format, final ByteBuffer data, final int rate) {
    if(this.canBuffer()) { int buffer = this.availableBuffers[--this.available]; alBufferData(buffer, format, data, rate); alSourceQueueBuffers(this.source, buffer); }
  }
  public void write(final int format, final short[] data, final int rate) {
    if(this.canBuffer()) { int buffer = this.availableBuffers[--this.available]; alBufferData(buffer, format, data, rate); alSourceQueueBuffers(this.source, buffer); }
  }
  public void write(final int format, final float[] data, final int rate) {
    if(this.canBuffer()) { int buffer = this.availableBuffers[--this.available]; alBufferData(buffer, format, data, rate); alSourceQueueBuffers(this.source, buffer); }
  }
  public void play() { if(this.source != 0 && alGetSourcei(this.source, AL_SOURCE_STATE) != AL_PLAYING) { alSourcePlay(this.source); } }
  public void stop() { if(this.source != 0) { alSourceStop(this.source); this.pump(); } }
  public float position() { return this.source == 0 ? 0 : alGetSourcef(this.source, AL_SEC_OFFSET); }
  public void close() {
    if(this.source == 0) { return; }
    alSourceStop(this.source);
    alDeleteSources(this.source);
    this.source = 0;
    alDeleteBuffers(this.ownedBuffers);
    MemoryUtil.memFree(this.tmp);
    this.tmp = null;
  }
}
