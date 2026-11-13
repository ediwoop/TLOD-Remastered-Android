package legend.core.video;
import java.io.*;import java.nio.*;import java.nio.file.Path;import java.nio.file.Files;
import legend.core.spu.XaAdpcm;import legend.game.fmv.IkiFrameDecoder;
/** Raw PS1 movie sectors. Timestamp each frame by its sector position at double CD speed. */
public final class IkiVideoDecoder implements VideoDecoder {
 private final Path path;private final long length;private long position;
 private DataInputStream file;private final byte[] sector=new byte[2352];
 private int width,height,rate,firstVideo=-1,audioCoding=-1;private long audioFrames,duration;private boolean videoPass,closed;
 public IkiVideoDecoder(Path path)throws IOException{
  this.path=path;length=Files.size(path);try{
   if(length%2352!=0)throw new IOException("IKI must contain raw2352 sectors");
   rewind();
   int index=0;while(readSector()){
    int type=sector[18]&14;
    if(type==4){int coding=sector[19]&255;if((coding&16)!=0)throw new IOException("8-bit XA is not supported");if(audioCoding<0){audioCoding=coding;rate=(coding&4)!=0?18900:37800;}if(coding!=audioCoding)throw new IOException("Changing XA coding in movie");audioFrames+=(coding&1)!=0?2016:4032;}
    else if(type==8&&i32(sector,24)==0x80010160&&u16(sector,28)==0&&firstVideo<0){firstVideo=index;width=u16(sector,60);height=u16(sector,62);}
    index++;
   }
   if(width<1||height<1||width>640||height>512||rate==0||audioFrames==0)throw new IOException("Missing IKI audio/video");
   duration=audioFrames*1_000_000L/rate;rewind();XaAdpcm.reset();
  }catch(IOException|RuntimeException e){try{close();}catch(IOException cleanup){e.addSuppressed(cleanup);}throw e;}
 }
 private void rewind()throws IOException{if(file!=null)file.close();file=new DataInputStream(new BufferedInputStream(Files.newInputStream(path),2352*128));position=0;}
 private boolean readSector()throws IOException{if(position==length)return false;file.readFully(sector);position+=2352;return true;}
 private static int u16(byte[]b,int p){return(b[p]&255)|((b[p+1]&255)<<8);}
 private static int i32(byte[]b,int p){return u16(b,p)|(u16(b,p+2)<<16);}
 public int width(){return width;}public int height(){return height;}public int sampleRate(){return rate;}public int channels(){return 2;}public long durationMicros(){return duration;}
 public short[] readAudio()throws IOException{if(videoPass||closed)throw new IOException("Audio pass closed");while(readSector())if((sector[18]&14)==4)return XaAdpcm.decode(sector,sector[19]);return null;}
 public void rewindVideo()throws IOException{if(closed)throw new IOException("Movie closed");rewind();videoPass=true;}
 public Frame readVideo()throws IOException{
  if(!videoPass||closed)throw new IOException("Video pass closed");
  byte[] demux=null;int expected=0,frame=-1,size=0;long seen=0,timestamp=0;
  while(readSector())if((sector[18]&14)==8&&i32(sector,24)==0x80010160){
   int part=u16(sector,28),parts=u16(sector,30),number=i32(sector,32);
   if(parts<1||parts>32||part>=parts)throw new IOException("Invalid IKI chunk table");
   if(demux==null){if(part!=0)throw new IOException("IKI frame starts mid-packet");frame=number;expected=parts;size=i32(sector,36);if(size<10||size>parts*2016)throw new IOException("Invalid IKI frame size");demux=new byte[parts*2016];timestamp=(position/2352-1-firstVideo)*1_000_000L/150;}
   if(frame!=number||parts!=expected||(seen&(1L<<part))!=0)throw new IOException("Discontinuous IKI frame");
   System.arraycopy(sector,56,demux,part*2016,2016);seen|=1L<<part;
   if(Long.bitCount(seen)==expected){int[] pixels=IkiFrameDecoder.decode(demux,size);if(pixels.length!=width*height)throw new IOException("Changing movie dimensions");byte[] rgb=new byte[pixels.length*3];for(int i=0,p=0;i<pixels.length;i++){int colour=pixels[i];rgb[p++]=(byte)colour;rgb[p++]=(byte)(colour>>>8);rgb[p++]=(byte)(colour>>>16);}ByteBuffer buffer=ByteBuffer.allocateDirect(rgb.length);buffer.put(rgb).flip();return new Frame(buffer,timestamp);}
  }
  if(demux!=null)throw new IOException("Truncated IKI frame");return null;
 }
 public void close()throws IOException{if(closed)return;closed=true;if(file!=null){file.close();file=null;}}
}
