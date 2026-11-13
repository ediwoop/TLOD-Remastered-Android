package legend.core.audio.opus;
import java.nio.*;import java.util.Arrays;
import legend.core.memory.BufferUtils;
import org.lwjgl.system.MemoryStack;import org.lwjgl.util.opus.OpusFile;
/** Desktop-only libopusfile binding. */
public final class DesktopOpusReader implements OpusReader {
 private final ByteBuffer encoded;private long file;private final int channels;private final long samples;private ShortBuffer pcm;
 public DesktopOpusReader(byte[] bytes){encoded=BufferUtils.createByteBuffer(bytes.length);encoded.put(bytes).flip();try(MemoryStack stack=MemoryStack.stackPush()){IntBuffer error=stack.mallocInt(1);file=OpusFile.op_open_memory(encoded,error);if(file==0||error.get(0)!=0)throw new IllegalArgumentException("Invalid Opus file: "+error.get(0));}
  channels=OpusFile.op_channel_count(file,-1);samples=OpusFile.op_pcm_total(file,-1)*channels;
 }
 public int channels(){return channels;}
 public long totalSamples(){return samples;}
 public int read(short[] out){if(file==0)throw new IllegalStateException("Reader closed");if(pcm==null||pcm.capacity()!=out.length)pcm=BufferUtils.createShortBuffer(out.length);pcm.clear();int frames=OpusFile.op_read(file,pcm,null);if(frames<0)throw new IllegalStateException("Opus read failed: "+frames);int n=frames*channels;pcm.position(0);pcm.get(out,0,n);Arrays.fill(out,n,out.length,(short)0);return n;}
 public void close(){if(file!=0){OpusFile.op_free(file);file=0;}}
}
