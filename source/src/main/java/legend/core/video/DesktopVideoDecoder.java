package legend.core.video;
import org.bytedeco.javacv.*;import org.bytedeco.ffmpeg.global.avutil;
import java.nio.*;import java.nio.file.Path;import java.io.IOException;
/** Desktop JavaCV binding isolated from the shared movie player. */
public final class DesktopVideoDecoder implements VideoDecoder {
 private final FFmpegFrameGrabber decoder;
 public DesktopVideoDecoder(Path path)throws IOException{decoder=new FFmpegFrameGrabber(path.toFile());decoder.setPixelFormat(avutil.AV_PIX_FMT_RGB24);decoder.start();}
 public int width(){return decoder.getImageWidth();}public int height(){return decoder.getImageHeight();}public int sampleRate(){return decoder.getSampleRate();}public int channels(){return decoder.getAudioChannels();}public long durationMicros(){return decoder.getLengthInTime();}
 public short[] readAudio()throws IOException{org.bytedeco.javacv.Frame f;while((f=decoder.grabFrame())!=null)if(f.samples!=null){ShortBuffer b=((ShortBuffer)f.samples[0]).duplicate();b.position(0);short[] out=new short[b.remaining()];b.get(out);return out;}return null;}
 public void rewindVideo()throws IOException{decoder.setFrameNumber(0);}
 public VideoDecoder.Frame readVideo()throws IOException{org.bytedeco.javacv.Frame f=decoder.grabImage();if(f==null)return null;return new VideoDecoder.Frame(((ByteBuffer)f.image[0]).duplicate(),f.timestamp);}
 public void close()throws IOException{decoder.stop();decoder.release();}
}
