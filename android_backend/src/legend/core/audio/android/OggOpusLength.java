package legend.core.audio.android;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Length of a complete, single logical Ogg Opus stream with a zero sample origin. */
final class OggOpusLength {
  private OggOpusLength() { }

  static long samples(final byte[] bytes, final int channels) {
    final ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    int offset = 0, sequence = 0, serial = 0, preSkip = -1;
    long finalGranule = -1;
    boolean ended = false;
    while(offset < bytes.length) {
      if(ended || bytes.length - offset < 27 || data.getInt(offset) != 0x5367674f || bytes[offset + 4] != 0) {
        throw new IllegalArgumentException("Truncated, chained or invalid Ogg stream");
      }
      final int flags = bytes[offset + 5] & 0xff;
      final int pageSerial = data.getInt(offset + 14);
      if(data.getInt(offset + 18) != sequence || (sequence > 0 && (pageSerial != serial || (flags & 2) != 0))) {
        throw new IllegalArgumentException("Multiplexed or discontinuous Ogg stream");
      }
      final int segments = bytes[offset + 26] & 0xff;
      final int payload = offset + 27 + segments;
      if(payload > bytes.length) { throw new IllegalArgumentException("Truncated Ogg lacing table"); }
      int size = 0;
      for(int i = 0; i < segments; i++) { size += bytes[offset + 27 + i] & 0xff; }
      if(size > bytes.length - payload) { throw new IllegalArgumentException("Truncated Ogg page"); }
      if(sequence == 0) {
        if(flags != 2 || size != 19 || segments != 1 || data.getLong(payload) != 0x646165487375704fL ||
            (bytes[payload + 8] & 0xff) > 15 || (bytes[payload + 9] & 0xff) != channels || bytes[payload + 18] != 0) {
          throw new IllegalArgumentException("Unsupported Opus identification header");
        }
        serial = pageSerial;
        preSkip = data.getShort(payload + 10) & 0xffff;
      }
      if((flags & 4) != 0) { finalGranule = data.getLong(offset + 6); ended = true; }
      offset = payload + size;
      sequence++;
    }
    if(!ended || preSkip < 0 || finalGranule < preSkip) { throw new IllegalArgumentException("Missing Opus end granule"); }
    return Math.multiplyExact(finalGranule - preSkip, channels);
  }
}
