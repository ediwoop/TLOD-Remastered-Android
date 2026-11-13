package legend.core.audio.android;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaDataSource;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import legend.core.audio.opus.OpusReader;

/** Ogg Opus to 48 kHz PCM16 through Android's extractor and decoder. */
public final class AndroidOpusReader implements OpusReader {
  private final MediaExtractor extractor = new MediaExtractor();
  private final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
  private MediaCodec decoder;
  private MediaDataSource data;
  private int channels;
  private long samples = -1;
  private long emitted;
  private boolean inputEnded, outputEnded, closed;
  private short[] pending = new short[0];
  private int pendingOffset;

  public AndroidOpusReader(final byte[] encoded) {
    try {
      this.data = new MediaDataSource() {
        @Override public long getSize() { return encoded.length; }
        @Override public int readAt(long position, byte[] target, int offset, int count) throws IOException {
          if(position < 0) { throw new IOException("Negative media position"); }
          if(count == 0) { return 0; }
          if(position >= encoded.length) { return -1; }
          final int available = (int)Math.min(count, encoded.length - position);
          System.arraycopy(encoded, (int)position, target, offset, available);
          return available;
        }
        @Override public void close() { }
      };
      this.extractor.setDataSource(this.data);
      MediaFormat selected = null;
      for(int i = 0; i < this.extractor.getTrackCount(); i++) {
        final MediaFormat candidate = this.extractor.getTrackFormat(i);
        if("audio/opus".equals(candidate.getString(MediaFormat.KEY_MIME))) {
          this.extractor.selectTrack(i);
          selected = candidate;
          break;
        }
      }
      if(selected == null) { throw new IllegalArgumentException("No Ogg Opus track"); }
      this.channels = selected.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
      if(this.channels != 1 && this.channels != 2) { throw new IllegalArgumentException("Opus must be mono or stereo"); }
      this.samples = OggOpusLength.samples(encoded, this.channels);
      selected.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
      this.decoder = MediaCodec.createDecoderByType("audio/opus");
      this.decoder.configure(selected, null, null, 0);
      this.decoder.start();
    } catch(final IOException | RuntimeException error) {
      this.close();
      throw new IllegalArgumentException("Android could not open Opus stream", error);
    }
  }

  @Override public int channels() { return this.channels; }
  @Override public long totalSamples() { return this.samples; }

  @Override public int read(final short[] destination) {
    if(this.closed) { throw new IllegalStateException("Opus reader closed"); }
    if(destination.length % this.channels != 0) { throw new IllegalArgumentException("Incomplete PCM frame"); }
    Arrays.fill(destination, (short)0);
    int written = 0;
    final long deadline = System.nanoTime() + 5_000_000_000L;
    while(written < destination.length && this.emitted < this.samples) {
      final int count = (int)Math.min(this.samples - this.emitted, Math.min(destination.length - written, this.pending.length - this.pendingOffset));
      if(count > 0) {
        System.arraycopy(this.pending, this.pendingOffset, destination, written, count);
        this.pendingOffset += count;
        written += count;
        this.emitted += count;
        continue;
      }
      if(this.outputEnded) { throw new IllegalStateException("Android Opus decoder ended before the final granule"); }
      if(System.nanoTime() > deadline) { throw new IllegalStateException("Android Opus decoder timed out"); }
      this.decodeNext();
    }
    return written;
  }

  private void decodeNext() {
    if(!this.inputEnded) {
      final int inputIndex = this.decoder.dequeueInputBuffer(0);
      if(inputIndex >= 0) {
        final ByteBuffer input = this.decoder.getInputBuffer(inputIndex);
        input.clear();
        final int size = this.extractor.readSampleData(input, 0);
        if(size < 0) {
          this.decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
          this.inputEnded = true;
        } else {
          this.decoder.queueInputBuffer(inputIndex, 0, size, this.extractor.getSampleTime(), 0);
          this.extractor.advance();
        }
      }
    }
    final int outputIndex = this.decoder.dequeueOutputBuffer(this.info, 10_000);
    if(outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
      final MediaFormat output = this.decoder.getOutputFormat();
      if(output.getInteger(MediaFormat.KEY_CHANNEL_COUNT) != this.channels || output.getInteger(MediaFormat.KEY_SAMPLE_RATE) != 48_000 ||
          output.getInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT) != AudioFormat.ENCODING_PCM_16BIT) {
        throw new IllegalStateException("Unexpected Android Opus PCM format: " + output);
      }
    } else if(outputIndex >= 0) {
      try {
        if(this.info.size % (this.channels * 2) != 0) { throw new IllegalStateException("Partial Opus PCM frame"); }
        this.pending = new short[this.info.size / 2];
        this.pendingOffset = 0;
        if(this.info.size > 0) {
          final ByteBuffer output = this.decoder.getOutputBuffer(outputIndex).duplicate().order(ByteOrder.nativeOrder());
          output.position(this.info.offset).limit(this.info.offset + this.info.size);
          output.asShortBuffer().get(this.pending);
        }
        this.outputEnded = (this.info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
      } finally {
        this.decoder.releaseOutputBuffer(outputIndex, false);
      }
    }
  }

  @Override public void close() {
    if(this.closed) { return; }
    this.closed = true;
    try {
      if(this.decoder != null) { this.decoder.release(); this.decoder = null; }
    } finally {
      this.extractor.release();
      if(this.data != null) { try { this.data.close(); } catch(final IOException ignored) { } this.data = null; }
      this.pending = new short[0];
    }
  }
}
