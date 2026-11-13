package com.realbox.dragoon;
import java.nio.*;import java.nio.file.*;import java.util.zip.*;import java.io.*;
import legend.core.video.*;import legend.core.video.android.*;
/** Android movie codec check using the actual project intro packaged in the APK. */
public final class VideoCheck {
 public static void main(String[] args){try{check(args);System.exit(0);}catch(Throwable t){t.printStackTrace();System.exit(1);}}
 private static void check(String[] args)throws Exception{
  Path movie=Path.of(args[1]);try(ZipFile zip=new ZipFile(args[0]);InputStream in=zip.getInputStream(zip.getEntry("assets/gfx/intro.mp4"))){Files.copy(in,movie,StandardCopyOption.REPLACE_EXISTING);}
  long samples=0,energy=0,frames=0,last=-1,nonBlack=0;
  VideoDecoders.install(AndroidVideoDecoder::new);
  try(VideoDecoder d=VideoDecoders.open(movie)){
   if(d.width()!=320||d.height()!=180||d.sampleRate()!=48000||d.channels()!=2)throw new AssertionError("Unexpected intro format");
   short[] pcm;while((pcm=d.readAudio())!=null){samples+=pcm.length;for(short s:pcm)energy+=(long)s*s;if(samples>2000000)throw new AssertionError("Audio EOF");}
   if(samples<800000||energy==0)throw new AssertionError("Missing intro audio: "+samples);
   System.out.println("PASS: Android movie audio "+samples+" non-silent PCM samples");
   d.rewindVideo();VideoDecoder.Frame f;while((f=d.readVideo())!=null){if(f.timestampMicros()<last)throw new AssertionError("Video timestamps");last=f.timestampMicros();ByteBuffer rgb=f.pixels();if(rgb.remaining()!=320*180*3)throw new AssertionError("RGB size");for(int i=0;i<rgb.remaining();i+=127)nonBlack+=rgb.get(i)&255;frames++;if(frames>400)throw new AssertionError("Video EOF");}
   if(frames<200||last<8000000||nonBlack==0)throw new AssertionError("Missing intro frames: "+frames+" time="+last);
   System.out.println("PASS: Android movie video "+frames+" RGB frames to EOF, last timestamp "+last+", non-black pixels="+nonBlack);
  }
 }
}
