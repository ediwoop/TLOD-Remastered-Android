package legend.game.textures;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static legend.core.IoHelper.pathToByteBuffer;

public class Image {
  public final byte[] data;
  public final int width;
  public final int height;

  public Image(final byte[] data, final int width, final int height) {
    this.data = data;
    this.width = width;
    this.height = height;
  }

  public static Image load(final Path path) {
    final ByteBuffer imageBuffer;
    try {
      imageBuffer = pathToByteBuffer(path);
    } catch(final IOException e) {
      throw new RuntimeException(e);
    }

    try(final legend.core.image.DecodedImage image = legend.core.image.ImageCodecs.decode(imageBuffer)) {
      final byte[] pixels = new byte[image.width * image.height * 4];
      image.pixels.duplicate().get(pixels);
      return new Image(pixels, image.width, image.height);
    }
  }
}
