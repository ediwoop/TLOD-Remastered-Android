package legend.core.audio.opus;
/** Interleaved PCM16 decoding at 48kHz; owns its compressed-data lifetime. */
public interface OpusReader extends AutoCloseable {
 int channels();
 long totalSamples();
 int read(short[] destination);
 void close();
}
