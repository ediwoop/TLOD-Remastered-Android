package legend.core.audio;
import java.nio.ByteBuffer;
import legend.core.audio.output.*;

/** PCM production shared by desktop and Android; device operations belong to PcmStream. */
public abstract class AudioSource {
 private final int bufferCount;private volatile PcmStream output;private volatile boolean active;
 public AudioSource(int buffers){if(buffers<1)throw new IllegalArgumentException("Invalid buffer count");bufferCount=buffers;}
 protected boolean isInitialized(){return output!=null;}
 protected void init(){if(output!=null)throw new IllegalStateException("Source already initialized");output=AudioOutputs.create(bufferCount);}
 protected void destroy(){active=false;if(output!=null){output.close();output=null;}}
 public void tick(){if(active)play();}
 public boolean canBuffer(){return active&&output!=null&&output.canBuffer();}
 protected void handleProcessedBuffers(){if(output!=null)output.pump();}
 protected void bufferOutput(int format,ByteBuffer buffer,int rate){synchronized(this){if(output!=null&&output.canBuffer())output.write(format,buffer,rate);}}
 protected void bufferOutput(int format,short[] buffer,int rate){synchronized(this){if(output!=null&&output.canBuffer())output.write(format,buffer,rate);}}
 protected void bufferOutput(int format,float[] buffer,int rate){synchronized(this){if(output!=null&&output.canBuffer())output.write(format,buffer,rate);}}
 protected void play(){if(output!=null)output.play();}
 protected void stop(){active=false;if(output!=null)output.stop();}
 protected void setActive(boolean value){active=value;}
 public boolean isActive(){return active;}
 public float getPosition(){synchronized(this){return output==null?0:output.position();}}
}
