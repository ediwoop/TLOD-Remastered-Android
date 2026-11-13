package legend.game.textures;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

/** RGBA8 PNG encoding through standard Java APIs, also available on Android. */
public final class PngWriter {
  private PngWriter() { }

  public static byte[] compress(final ByteBuffer pixels, final int width, final int height) {
    if(width <= 0 || height <= 0) { throw new IllegalArgumentException("Invalid PNG dimensions"); }
    final int rowBytes = Math.multiplyExact(width, 4);
    final int totalBytes = Math.multiplyExact(rowBytes, height);
    if(pixels.remaining() < totalBytes) { throw new IllegalArgumentException("Incomplete RGBA pixel buffer"); }
    final ByteBuffer input = pixels.duplicate();
    try {
      final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
      try(final DeflaterOutputStream zlib = new DeflaterOutputStream(compressed)) {
        final byte[] row = new byte[rowBytes];
        for(int y = 0; y < height; y++) {
          zlib.write(0); // PNG row filter: none
          input.get(row);
          zlib.write(row);
        }
      }
      final ByteArrayOutputStream png = new ByteArrayOutputStream();
      final DataOutputStream output = new DataOutputStream(png);
      output.write(new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10});
      final ByteArrayOutputStream header = new ByteArrayOutputStream(13);
      final DataOutputStream ihdr = new DataOutputStream(header);
      ihdr.writeInt(width); ihdr.writeInt(height);
      ihdr.write(new byte[] {8, 6, 0, 0, 0}); // 8-bit RGBA, no interlace
      chunk(output, "IHDR", header.toByteArray());
      chunk(output, "IDAT", compressed.toByteArray());
      chunk(output, "IEND", new byte[0]);
      return png.toByteArray();
    } catch(final IOException impossibleForMemoryStreams) {
      throw new UncheckedIOException(impossibleForMemoryStreams);
    }
  }

  private static void chunk(final DataOutputStream output, final String type, final byte[] bytes) throws IOException {
    final byte[] name = type.getBytes(StandardCharsets.US_ASCII);
    final CRC32 crc = new CRC32(); crc.update(name); crc.update(bytes);
    output.writeInt(bytes.length); output.write(name); output.write(bytes); output.writeInt((int)crc.getValue());
  }

  public static void write(final Path path, final ByteBuffer pixels, final int width, final int height) throws IOException {
    Files.write(path, compress(pixels, width, height), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
}
