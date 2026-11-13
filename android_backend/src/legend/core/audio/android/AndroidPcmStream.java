package legend.core.audio.android;
import android.media.*;
import java.nio.*;import java.util.*;
import legend.core.audio.output.PcmStream;

/** Bounded per-source PCM stream. Android's mixer combines music, effects and video tracks. */
public final class AndroidPcmStream implements PcmStream {
 private final int capacity;
 private final ArrayDeque<ByteBuffer> queue=new ArrayDeque<>();
 private AudioTrack track;private int format,rate,channels,encoding;private boolean playing,closed;
 private long wraps,lastHead;
 public AndroidPcmStream(int buffers){if(buffers<1)throw new IllegalArgumentException("Invalid queue size");capacity=buffers;}
 public boolean canBuffer(){return !closed&&queue.size()<capacity;}
 private void configure(int value,int sampleRate){
  if(closed)throw new IllegalStateException("Stream closed");
  if(sampleRate<=0)throw new IllegalArgumentException("Invalid sample rate");
  if(track!=null){if(value!=format||sampleRate!=rate)throw new IllegalArgumentException("Stop stream before changing PCM format");return;}
  channels=value==0x1101?1:2;encoding=value==0x10011?AudioFormat.ENCODING_PCM_FLOAT:AudioFormat.ENCODING_PCM_16BIT;
  if(value!=0x1101&&value!=0x1103&&value!=0x10011)throw new IllegalArgumentException("Unsupported PCM format: "+value);
  int mask=channels==2?AudioFormat.CHANNEL_OUT_STEREO:AudioFormat.CHANNEL_OUT_MONO;
  int min=AudioTrack.getMinBufferSize(sampleRate,mask,encoding);if(min<=0)throw new IllegalStateException("AudioTrack buffer size: "+min);
  int frameBytes=channels*(encoding==AudioFormat.ENCODING_PCM_FLOAT?4:2);
  AudioTrack created=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(mask).setEncoding(encoding).build()).setBufferSizeInBytes(Math.max(min,sampleRate/20*frameBytes)).setTransferMode(AudioTrack.MODE_STREAM).build();
  if(created.getState()!=AudioTrack.STATE_INITIALIZED){created.release();throw new IllegalStateException("AudioTrack initialization failed");}
  track=created;format=value;rate=sampleRate;wraps=lastHead=0;if(playing)track.play();
 }
 private void enqueue(int format,ByteBuffer data,int sampleRate){if(!canBuffer())throw new IllegalStateException("PCM queue is full");configure(format,sampleRate);int stride=channels*(encoding==AudioFormat.ENCODING_PCM_FLOAT?4:2);if(data.remaining()%stride!=0)throw new IllegalArgumentException("Incomplete PCM frame");queue.add(data);pump();}
 public void write(int format,ByteBuffer data,int sampleRate){ByteBuffer copy=ByteBuffer.allocateDirect(data.remaining()).order(ByteOrder.nativeOrder());copy.put(data.duplicate()).flip();enqueue(format,copy,sampleRate);}
 public void write(int format,short[] data,int sampleRate){if(format!=0x1101&&format!=0x1103)throw new IllegalArgumentException("Expected PCM16");ByteBuffer copy=ByteBuffer.allocateDirect(Math.multiplyExact(data.length,2)).order(ByteOrder.nativeOrder());copy.asShortBuffer().put(data);enqueue(format,copy,sampleRate);}
 public void write(int format,float[] data,int sampleRate){if(format!=0x10011)throw new IllegalArgumentException("Expected stereo float PCM");ByteBuffer copy=ByteBuffer.allocateDirect(Math.multiplyExact(data.length,4)).order(ByteOrder.nativeOrder());copy.asFloatBuffer().put(data);enqueue(format,copy,sampleRate);}
 public void pump(){if(track==null||closed)return;while(!queue.isEmpty()){ByteBuffer data=queue.peek();int n=track.write(data,data.remaining(),AudioTrack.WRITE_NON_BLOCKING);if(n<0)throw new IllegalStateException("AudioTrack write failed: "+n);if(!data.hasRemaining())queue.remove();if(n==0)return;}}
 public void play(){if(closed)return;playing=true;if(track!=null&&track.getPlayState()!=AudioTrack.PLAYSTATE_PLAYING)track.play();pump();}
 public void stop(){playing=false;queue.clear();if(track!=null){try{track.pause();track.flush();}finally{track.release();track=null;}}wraps=lastHead=0;}
 public float position(){if(track==null)return 0;long head=track.getPlaybackHeadPosition()&0xffffffffL;if(head<lastHead)wraps+=1L<<32;lastHead=head;return (float)((wraps+head)/(double)rate);}
 public void close(){if(closed)return;stop();closed=true;}
}
