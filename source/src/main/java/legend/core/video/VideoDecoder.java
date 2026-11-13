package legend.core.video;
import java.nio.ByteBuffer;
import java.io.IOException;
/** Two-pass movie reader: PCM16 audio first, then timestamped RGB24 video. */
public interface VideoDecoder extends AutoCloseable {
 record Frame(ByteBuffer pixels,long timestampMicros){}
 int width();int height();int sampleRate();int channels();long durationMicros();
 short[] readAudio() throws IOException;
 void rewindVideo() throws IOException;
 Frame readVideo() throws IOException;
 void close() throws IOException;
}
