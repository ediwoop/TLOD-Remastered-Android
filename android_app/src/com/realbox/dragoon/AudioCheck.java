package com.realbox.dragoon;
import legend.core.*;import legend.core.audio.*;import legend.core.audio.output.*;import legend.core.audio.android.*;import android.os.SystemClock;
/** Android PCM output integration; does not load music or effects from the disc. */
public final class AudioCheck {
 public static void main(String[] args){try{check(args);System.exit(0);}catch(Throwable error){error.printStackTrace(System.err);System.exit(1);}}
 private static void checkOpus(String apk)throws Exception{
  try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(apk)){
   for(String name:new String[]{"mono","stereo"}){
    byte[] bytes;try(java.io.InputStream in=zip.getInputStream(zip.getEntry("assets/audio-check/"+name+".opus"))){bytes=in.readAllBytes();}
    long samples=0,energy=0;int channels;
    try(AndroidOpusReader reader=new AndroidOpusReader(bytes)){
     channels=reader.channels();short[] pcm=new short[480*channels];int count;
     while((count=reader.read(pcm))>0){samples+=count;for(int i=0;i<count;i++)energy+=(long)pcm[i]*pcm[i];if(samples>200000)throw new AssertionError("Opus never reaches EOF");}
     if(samples!=48000L*channels)throw new AssertionError("Opus sample count "+samples+" vs "+48000*channels);
     if(energy==0)throw new AssertionError("Opus decoded only silence");
    }
    System.out.println("PASS: Android Opus "+name+" decodes "+samples+" non-silent interleaved samples and reaches EOF");
   }
  }
 }
 private static void check(String[] args)throws Exception{
  checkOpus(args[0]);
  AudioOutputs.install(AndroidPcmStream::new);legend.core.audio.opus.OpusReaders.install(AndroidOpusReader::new);CoreCheck.main(args);
  GameEngine.AUDIO_THREAD.init();GameEngine.SPU.init();System.out.println("PASS: real engine audio sources initialize without OpenAL");
  GameEngine.openalThread.start();
  try{
   java.lang.reflect.Field field=AudioThread.class.getDeclaredField("xaPlayer");field.setAccessible(true);legend.core.audio.opus.XaPlayer xa=(legend.core.audio.opus.XaPlayer)field.get(GameEngine.AUDIO_THREAD);
   try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(args[0])){
    for(String name:new String[]{"mono","stereo"}){
     byte[] bytes;try(java.io.InputStream in=zip.getInputStream(zip.getEntry("assets/audio-check/"+name+".opus"))){bytes=in.readAllBytes();}
     GameEngine.AUDIO_THREAD.loadXa(new legend.game.unpacker.FileData(bytes));
     long deadline=SystemClock.elapsedRealtime()+8000;
     while((xa.isActive()||xa.getPosition()<0.95f)&&SystemClock.elapsedRealtime()<deadline){SystemClock.sleep(20);}
     if(xa.isActive()||xa.getPosition()<0.95f)throw new AssertionError("Engine XA did not drain: "+name+" active="+xa.isActive()+" position="+xa.getPosition());
     GameEngine.AUDIO_THREAD.stopXa();if(xa.getPosition()!=0)throw new AssertionError("XA stop did not reset output");
     System.out.println("PASS: real engine XA "+name+" decodes, queues, drains and stops through Android AudioTrack");
    }
   }
   GameEngine.SPU.tick();System.out.println("PASS: real SPU tick reaches Android PCM output");
   GenericSource source=GameEngine.AUDIO_THREAD.addSource(new GenericSource(0x1103,48000));
   short[] silence=new short[96000];source.bufferOutput(silence);source.tick();float first=source.getPosition();SystemClock.sleep(80);float later=source.getPosition();if(later<=0)throw new AssertionError("PCM playback position did not advance");System.out.println("PASS: Android PCM16 playback head advances: "+first+" -> "+later);GameEngine.AUDIO_THREAD.removeSource(source);
   AndroidPcmStream floating=new AndroidPcmStream(4);try{floating.write(0x10011,new float[96000],48000);floating.play();for(int i=0;i<100&&floating.position()<=0;i++){floating.pump();SystemClock.sleep(20);}if(floating.position()<=0)throw new AssertionError("Float playback position");System.out.println("PASS: Android stereo float PCM accepted and playback position advances");}finally{floating.close();}
  }finally{GameEngine.AUDIO_THREAD.destroy();GameEngine.openalThread.join(2000);if(GameEngine.openalThread.isAlive())throw new AssertionError("Audio thread did not stop");}
 }
}
