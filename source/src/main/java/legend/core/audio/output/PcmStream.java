package legend.core.audio.output;
import java.nio.ByteBuffer;
/** Bounded PCM output stream. Called under its owning AudioSource lock. */
public interface PcmStream extends AutoCloseable {
 boolean canBuffer();
 void pump();
 void write(int format,ByteBuffer data,int sampleRate);
 void write(int format,short[] data,int sampleRate);
 void write(int format,float[] data,int sampleRate);
 void play();
 void stop();
 float position();
 void close();
}
