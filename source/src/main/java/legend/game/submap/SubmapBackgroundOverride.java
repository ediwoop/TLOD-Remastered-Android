package legend.game.submap;

import legend.core.GamePaths;
import legend.core.gpu.Rect4i;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import legend.core.image.DecodedImage;
import legend.core.image.ImageCodecs;

/** An optional background plate with bounds in the original scene's coordinates. */
public final class SubmapBackgroundOverride {
  final Path image;
  final Path readyImage;
  final java.util.BitSet behindActorLayers;
  record ForegroundGroup(Path image,Rect4i bounds,int count) {}
  final ForegroundGroup foregroundGroup;
  final Rect4i bounds;
  final boolean maskedForegrounds;
  final Path surroundImage;
  final Rect4i displayBounds;
  private final float registrationScale,registrationU,registrationV;

  private SubmapBackgroundOverride(final Path image, final Rect4i bounds, final boolean maskedForegrounds,
                                  final Path surroundImage, final Path readyImage, final ForegroundGroup foregroundGroup, final java.util.BitSet behindActorLayers, final Rect4i displayBounds,final float registrationScale,final float registrationU,final float registrationV) {
    this.image = image;
    this.readyImage = readyImage;
    this.behindActorLayers = behindActorLayers;
    this.foregroundGroup = foregroundGroup;
    this.bounds = bounds;
    this.maskedForegrounds = maskedForegrounds;
    this.surroundImage = surroundImage;
    this.displayBounds = displayBounds;
    this.registrationScale=registrationScale;this.registrationU=registrationU;this.registrationV=registrationV;
  }

  DecodedImage decodeImage() {
    try {
      // Bundled plates are finished offline: one decode, no synthesis or joining on scene entry.
      if(this.readyImage != null) {
        if(this.readyImage.getFileName().toString().equals("675-ready.png")) {
          try(var bundled = SubmapBackgroundOverride.class.getResourceAsStream("/remastered/submaps/675.jpg")) {
            if(bundled != null) {
              final byte[] bytes = bundled.readAllBytes();
              final var encoded = java.nio.ByteBuffer.allocateDirect(bytes.length);
              encoded.put(bytes).flip();
              return ImageCodecs.decode(encoded);
            }
          }
        }
        return ImageCodecs.decode(legend.core.IoHelper.pathToByteBuffer(this.readyImage));
      }
      if(this.surroundImage == null) return ImageCodecs.decode(legend.core.IoHelper.pathToByteBuffer(this.image));
      try(final DecodedImage base=ImageCodecs.decode(legend.core.IoHelper.pathToByteBuffer(this.image))) {
        final DecodedImage surround=ImageCodecs.decode(legend.core.IoHelper.pathToByteBuffer(this.surroundImage));
        try {
          if(this.registrationScale>0) {
            try(surround) { return SubmapPlateAssembly.registered(base,this.bounds,surround,this.displayBounds,this.registrationScale,this.registrationU,this.registrationV); }
          }
          SubmapPlateAssembly.assemble(base,this.bounds,surround,this.displayBounds);
          return surround;
        } catch(final RuntimeException|Error failure) { surround.close();throw failure; }
      }
    } catch(final IOException error) {
      throw new IllegalStateException("Cannot decode remastered background " + this.image, error);
    }
  }

  public static boolean isUsable(final int cut) {
    return find(cut) != null;
  }

  /** The viewer uses exactly the same finished plate as gameplay. */
  public static legend.core.renderer.Texture previewTexture(final int cut) {
    final SubmapBackgroundOverride plate=find(cut);
    if(plate==null)return null;
    try(final DecodedImage image=plate.decodeImage()) {
      return legend.core.renderer.Texture.create("Remastered submap preview "+cut,builder->{
        builder.data(image.pixels,image.width,image.height);builder.minFilter(true);builder.magFilter(true);builder.wrapS(false);builder.wrapT(false);
      });
    }
  }

  static boolean hasUniformScale(final int imageWidth, final int imageHeight, final Rect4i bounds) {
    // Allow at most one texture pixel of dimension rounding.
    return imageWidth > 0 && imageHeight > 0 && bounds.w > 0 && bounds.h > 0 &&
      Math.abs((long)imageWidth * bounds.h - (long)imageHeight * bounds.w) <= Math.max(bounds.w, bounds.h);
  }

  /** Read only the fixed PNG header here. Decoding is done once, by decodeImage(). */
  private static int[] dimensions(final Path image) throws IOException {
    try(var in = new java.io.DataInputStream(Files.newInputStream(image))) {
      if(in.readLong()!=0x89504e470d0a1a0aL || in.readInt()!=13 || in.readInt()!=0x49484452)
        throw new IOException("Invalid PNG IHDR: "+image);
      final int w=in.readInt(),h=in.readInt();
      if(w<=0||h<=0||w>16384||h>16384)throw new IOException("Invalid PNG dimensions: "+image);
      return new int[]{w,h};
    }
  }

  private static boolean validImage(final Path image, final Rect4i bounds) {
    try {
      final int[] size=dimensions(image);
      return hasUniformScale(size[0],size[1],bounds);
    } catch(final IOException | RuntimeException error) {
      System.err.println("Ignoring invalid remaster plate " + image + ": " + error);
      return false;
    }
  }

  private static boolean validRegisteredImage(final Path image,final Rect4i canvas,final float scale,final float u,final float v) {
    try {
      final int[] size=dimensions(image);
      return canvas.x*scale+u>=-0.5f && canvas.y*scale+v>=-0.5f &&
        canvas.right()*scale+u<=size[0]+0.5f && canvas.bottom()*scale+v<=size[1]+0.5f;
    } catch(final IOException | RuntimeException error) {return false;}
  }

  static SubmapBackgroundOverride find(final int cut) {
    final Path folder = GamePaths.resolve("gfx/remaster/submaps");
    final Path image = folder.resolve(cut + ".png");
    final Path metadata = folder.resolve(cut + ".properties");
    if(!Files.isRegularFile(metadata)) {
      return null;
    }

    final Properties properties = new Properties();
    try(var reader = Files.newBufferedReader(metadata)) {
      properties.load(reader);
    } catch(final IOException error) {
      throw new IllegalStateException("Cannot read remastered submap " + cut, error);
    }

    // Explicit user approval enables a candidate without claiming completed engine review.
    final String validation = properties.getProperty("validation");
    if(!"engine-reviewed".equals(validation) && !"user-approved".equals(validation)) return null;

    final Rect4i bounds = new Rect4i(
      Integer.parseInt(properties.getProperty("x")),
      Integer.parseInt(properties.getProperty("y")),
      Integer.parseInt(properties.getProperty("width")),
      Integer.parseInt(properties.getProperty("height"))
    );
    if(bounds.w <= 0 || bounds.h <= 0) {
      throw new IllegalArgumentException("Invalid remastered submap bounds: " + cut);
    }
    final boolean ready=Boolean.parseBoolean(properties.getProperty("ready"));
    final Path readyImage=ready?folder.resolve(cut+"-ready.png"):null;
    Path surroundImage=null;
    Rect4i displayBounds=bounds;
    if(Boolean.parseBoolean(properties.getProperty("surround"))) {
      if(!ready)surroundImage=folder.resolve(cut+"-surround.png");
      if(!ready && !Files.isRegularFile(surroundImage)) throw new IllegalArgumentException("Remastered surround is missing: "+surroundImage);
      displayBounds=new Rect4i(Integer.parseInt(properties.getProperty("surround.x")),
        Integer.parseInt(properties.getProperty("surround.y")),Integer.parseInt(properties.getProperty("surround.width")),
        Integer.parseInt(properties.getProperty("surround.height")));
      if(displayBounds.w<=0||displayBounds.h<=0||displayBounds.x>bounds.x||displayBounds.y>bounds.y||
         displayBounds.right()<bounds.right()||displayBounds.bottom()<bounds.bottom()) {
        throw new IllegalArgumentException("Surround must contain the aligned plate");
      }
    }
    final float registrationScale=Float.parseFloat(properties.getProperty("surround.pixelsPerUnit","0"));
    final float registrationU=Float.parseFloat(properties.getProperty("surround.originU","0"));
    final float registrationV=Float.parseFloat(properties.getProperty("surround.originV","0"));
    if(!Float.isFinite(registrationScale)||!Float.isFinite(registrationU)||!Float.isFinite(registrationV)||registrationScale<0)throw new IllegalArgumentException("Invalid uniform registration");
    if(ready ? !validImage(readyImage,displayBounds) : !validImage(image, bounds) || surroundImage != null && (registrationScale==0 ? !validImage(surroundImage, displayBounds) : !validRegisteredImage(surroundImage,displayBounds,registrationScale,registrationU,registrationV))) {
      System.err.println("Ignoring non-uniform remaster scale for cut " + cut);
      return null;
    }
    ForegroundGroup foregroundGroup=null;
    final int groupCount=Integer.parseInt(properties.getProperty("foreground.group.count","0"));
    if(groupCount>0) {
      final Rect4i groupBounds=new Rect4i(Integer.parseInt(properties.getProperty("foreground.group.x")),
        Integer.parseInt(properties.getProperty("foreground.group.y")),Integer.parseInt(properties.getProperty("foreground.group.width")),
        Integer.parseInt(properties.getProperty("foreground.group.height")));
      final Path groupImage=folder.resolve(cut+"-foreground.png");
      if(groupCount>32||!validImage(groupImage,groupBounds))throw new IllegalArgumentException("Invalid foreground group "+cut);
      foregroundGroup=new ForegroundGroup(groupImage,groupBounds,groupCount);
    }
    final var behindActors=new java.util.BitSet();
    for(final String value:properties.getProperty("foreground.behindActors", "").split(","))if(!value.isBlank()) {
      final int index=Integer.parseInt(value.trim());
      if(index<0 || index>=32)throw new IllegalArgumentException("Invalid foreground depth layer");
      behindActors.set(index);
    }
    return new SubmapBackgroundOverride(image, bounds, "masked".equals(properties.getProperty("foregrounds")),surroundImage,readyImage,foregroundGroup,behindActors,displayBounds,registrationScale,registrationU,registrationV);
  }
}
