package legend.core.audio.opus;
import legend.core.audio.AudioSource;
import legend.game.modding.coremod.CoreMod;
import legend.game.unpacker.FileData;
import java.util.Arrays;
import static legend.core.GameEngine.CONFIG;

public final class XaPlayer extends AudioSource {
 private final Object playbackLock=new Object();
 private OpusReader reader;private int channels=1,format=0x1101;
 private final int samplesPerTick=480;private short[] pcm=new short[480];
 private float playerVolume;
 public XaPlayer(){super(8);playerVolume=CONFIG.getConfig(CoreMod.SFX_VOLUME_CONFIG.get())*CONFIG.getConfig(CoreMod.MASTER_VOLUME_CONFIG.get());}
 public void setPlayerVolume(float volume){playerVolume=volume;}
 public void loadXa(FileData data){synchronized(playbackLock){stop();unloadOpusFile();OpusReader next=OpusReaders.open(data.getBytes());
  int count=next.channels();if(count!=1&&count!=2){next.close();throw new IllegalArgumentException("Unsupported Opus channel count");}
  if(next.totalSamples()>=0&&next.totalSamples()<samplesPerTick*count*4L){next.close();throw new IllegalArgumentException("XA file is shorter than 40ms");}
  reader=next;channels=count;format=count==2?0x1103:0x1101;pcm=new short[samplesPerTick*count];setActive(true);
  for(int i=0;i<4&&isActive()&&canBuffer();i++){if(readFile())bufferOutput(format,pcm,48000);}
  if(isActive())play();
 }}
 public void tick(){synchronized(playbackLock){if(reader==null){setActive(false);return;}if(readFile())bufferOutput(format,pcm,48000);super.tick();}}
 private boolean readFile(){Arrays.fill(pcm,(short)0);int samples=reader.read(pcm);if(samples<0||samples>pcm.length)throw new IllegalStateException("Invalid decoded sample count");if(samples==0){setActive(false);unloadOpusFile();return false;}for(int i=0;i<samples;i++)pcm[i]=(short)(pcm[i]*playerVolume);return true;}
 public void unloadOpusFile(){synchronized(playbackLock){if(reader!=null){reader.close();reader=null;}}}
 protected void destroy(){synchronized(playbackLock){super.destroy();}}
}
