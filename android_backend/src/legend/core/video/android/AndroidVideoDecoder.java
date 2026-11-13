package legend.core.video.android;

import android.media.*;
import android.graphics.Rect;
import java.io.IOException;
import java.nio.*;
import java.nio.file.Path;
import legend.core.video.VideoDecoder;

/** Android platform AAC/H264 (and other installed codecs), without JavaCV or desktop JNI. */
public final class AndroidVideoDecoder implements VideoDecoder {
  private final Path path;
  private final int width, height, rate, channels;
  private final long duration;
  private Track track;
  private boolean videoPass, closed;
  private long decodedFrames;

  public AndroidVideoDecoder(Path path) throws IOException {
    this.path=path;
    MediaExtractor metadata=new MediaExtractor();
    int w=0,h=0,r=0,c=0;long length=0;
    try {
      metadata.setDataSource(path.toString());
      for(int i=0;i<metadata.getTrackCount();i++) {
        MediaFormat f=metadata.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);
        if(mime.startsWith("video/")){w=f.getInteger(MediaFormat.KEY_WIDTH);h=f.getInteger(MediaFormat.KEY_HEIGHT);length=f.getLong(MediaFormat.KEY_DURATION,0);}
        else if(mime.startsWith("audio/")){r=f.getInteger(MediaFormat.KEY_SAMPLE_RATE);c=f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);}
      }
    } finally {metadata.release();}
    if(w<=0||h<=0||r<=0||(c!=1&&c!=2)||length<=0)throw new IOException("Unsupported movie tracks");
    width=w;height=h;rate=r;channels=c;duration=length;
    track=new Track(path,"audio/");
    android.util.Log.i("DragoonVideo", "Opened Android movie "+width+"x"+height+" durationUs="+duration);
  }
  public int width(){return width;} public int height(){return height;} public int sampleRate(){return rate;} public int channels(){return channels;} public long durationMicros(){return duration;}
  public short[] readAudio() throws IOException {
    if(closed||videoPass)throw new IOException("Audio pass is closed");
    try(Output output=track.next()) {
      if(output==null)return null;
      MediaFormat f=track.format;
      if(f.getInteger(MediaFormat.KEY_SAMPLE_RATE)!=rate||f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)!=channels||f.getInteger(MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT)!=AudioFormat.ENCODING_PCM_16BIT)throw new IOException("Unexpected movie PCM format");
      if(output.info.size%(2*channels)!=0)throw new IOException("Partial movie audio frame");
      ByteBuffer b=track.codec.getOutputBuffer(output.index).duplicate().order(ByteOrder.nativeOrder());b.position(output.info.offset).limit(output.info.offset+output.info.size);
      short[] result=new short[output.info.size/2];b.asShortBuffer().get(result);return result;
    }
  }
  public void rewindVideo() throws IOException {if(closed)throw new IOException("Decoder closed");track.close();track=new Track(path,"video/");videoPass=true;}
  public VideoDecoder.Frame readVideo() throws IOException {
    if(closed||!videoPass)throw new IOException("Video pass is not open");
    try(Output output=track.next()) {
      if(output==null)return null;
      try(Image image=track.codec.getOutputImage(output.index)) {
        if(image==null||image.getFormat()!=android.graphics.ImageFormat.YUV_420_888)throw new IOException("Decoder did not provide flexible YUV420");
        Rect crop=image.getCropRect();if(crop.width()!=width||crop.height()!=height)throw new IOException("Unexpected movie crop: "+crop);
        Image.Plane[] planes=image.getPlanes();
        byte[][] samples=new byte[3][];int[] rows=new int[3],steps=new int[3];
        for(int i=0;i<3;i++){ByteBuffer b=planes[i].getBuffer().duplicate();samples[i]=new byte[b.remaining()];b.get(samples[i]);rows[i]=planes[i].getRowStride();steps[i]=planes[i].getPixelStride();}
        byte[] rgb=new byte[Math.multiplyExact(Math.multiplyExact(width,height),3)];int dest=0;
        boolean bt709=track.format.getInteger(MediaFormat.KEY_COLOR_STANDARD,0)==MediaFormat.COLOR_STANDARD_BT709;
        boolean full=track.format.getInteger(MediaFormat.KEY_COLOR_RANGE,0)==MediaFormat.COLOR_RANGE_FULL;
        for(int row=0;row<height;row++)for(int col=0;col<width;col++) {
          int x=col+crop.left,z=row+crop.top;
          int yy=samples[0][z*rows[0]+x*steps[0]]&255;
          int uu=(samples[1][(z/2)*rows[1]+(x/2)*steps[1]]&255)-128;
          int vv=(samples[2][(z/2)*rows[2]+(x/2)*steps[2]]&255)-128;
          int red,green,blue;
          if(full){red=yy+((bt709?403:359)*vv+128>>8);green=yy-(((bt709?48:88)*uu+(bt709?120:183)*vv+128)>>8);blue=yy+((bt709?475:454)*uu+128>>8);}
          else{int luma=298*(yy-16);red=(luma+(bt709?459:409)*vv+128)>>8;green=(luma-(bt709?55:100)*uu-(bt709?136:208)*vv+128)>>8;blue=(luma+(bt709?541:516)*uu+128)>>8;}
          rgb[dest++]=(byte)clamp(red);rgb[dest++]=(byte)clamp(green);rgb[dest++]=(byte)clamp(blue);
        }
        ByteBuffer pixels=ByteBuffer.allocateDirect(rgb.length);pixels.put(rgb).flip();decodedFrames++;if(decodedFrames==1||decodedFrames%60==0)android.util.Log.i("DragoonVideo","Decoded video frame "+decodedFrames+" timestamp="+output.info.presentationTimeUs);return new VideoDecoder.Frame(pixels,output.info.presentationTimeUs);
      }
    }
  }
  private static int clamp(int v){return Math.max(0,Math.min(255,v));}
  public void close(){if(closed)return;closed=true;android.util.Log.i("DragoonVideo","Closed movie frames="+decodedFrames);if(track!=null){track.close();track=null;}}

  private static final class Track implements AutoCloseable {
    private final MediaExtractor extractor=new MediaExtractor();
    private MediaCodec codec;private MediaFormat format;private boolean inputEnded,outputEnded;
    Track(Path path,String kind)throws IOException {
      try {
        extractor.setDataSource(path.toString());
        for(int i=0;i<extractor.getTrackCount();i++)if(extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).startsWith(kind)){format=extractor.getTrackFormat(i);extractor.selectTrack(i);break;}
        if(format==null)throw new IOException("Missing "+kind+" track");
        if(kind.equals("video/"))format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        else format.setInteger(MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT);
        codec=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));codec.configure(format,null,null,0);codec.start();
      } catch(IOException|RuntimeException e){close();throw e;}
    }
    Output next()throws IOException {
      if(outputEnded)return null;
      long deadline=System.nanoTime()+10_000_000_000L;
      while(System.nanoTime()<deadline) {
        if(!inputEnded){int index=codec.dequeueInputBuffer(0);if(index>=0){ByteBuffer b=codec.getInputBuffer(index);b.clear();int size=extractor.readSampleData(b,0);if(size<0){inputEnded=true;codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);}else{codec.queueInputBuffer(index,0,size,extractor.getSampleTime(),0);extractor.advance();}}}
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();int index=codec.dequeueOutputBuffer(info,10_000);
        if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){format=codec.getOutputFormat();continue;}
        if(index>=0){outputEnded=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;if(info.size>0)return new Output(this,index,info);codec.releaseOutputBuffer(index,false);if(outputEnded)return null;}
      }
      throw new IOException("Movie decoder timed out");
    }
    public void close(){try{if(codec!=null){codec.release();codec=null;}}finally{extractor.release();}}
  }
  private static final class Output implements AutoCloseable {
    final Track track;final int index;final MediaCodec.BufferInfo info;
    Output(Track track,int index,MediaCodec.BufferInfo info){this.track=track;this.index=index;this.info=info;}
    public void close(){track.codec.releaseOutputBuffer(index,false);}
  }
}
