package legend.game.fmv;

import legend.core.Config;
import legend.core.audio.GenericSource;
import legend.core.gpu.Bpp;
import legend.core.platform.WindowEvents;
import legend.core.renderer.Obj;
import legend.core.renderer.QuadBuilder;
import legend.core.renderer.QueuedModelStandard;
import legend.core.renderer.Texture;
import legend.core.renderer.TextureDataFormat;
import legend.core.renderer.TextureDataType;
import legend.core.renderer.TextureInternalFormat;
import legend.game.EngineState;
import legend.game.modding.coremod.CoreMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3i;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;

import static legend.core.GameEngine.AUDIO_THREAD;
import static legend.core.GameEngine.CONFIG;
import static legend.core.GameEngine.DISCORD;
import static legend.core.GameEngine.PLATFORM;
import static legend.core.GameEngine.RENDERER;
import static legend.game.Graphics.clearBlue_800babc0;
import static legend.game.Graphics.clearGreen_800bb104;
import static legend.game.Graphics.clearRed_8007a3a8;
import static legend.game.modding.coremod.CoreMod.ALLOW_WIDESCREEN_CONFIG;
import legend.core.video.VideoDecoder;
import legend.core.video.VideoDecoders;
import legend.core.memory.BufferUtils;

public final class VideoPlayer {
  private VideoPlayer() { }

  private static final Logger LOGGER = LogManager.getFormatterLogger(VideoPlayer.class);

  private static Runnable oldRenderer;
  private static int oldFps;
  private static boolean oldAllowWidescreen;

  private static VideoDecoder grabber;
  private static VideoDecoder.Frame currentFrame;
  private static long audioDurationMicros;
  private static boolean audioStarted;

  private static int videoWidth;
  private static int videoHeight;

  private static GenericSource source;
  private static ByteBuffer pcmBuffer;
  /** Used to end video when buffered audio loops */
  private static float lastPosition;

  private static WindowEvents.KeyPressed keyPress;
  private static WindowEvents.ButtonPressed buttonPressed;
  private static WindowEvents.Click click;
  private static WindowEvents.InputActionPressed inputAction;
  private static boolean shouldStop;

  private static Obj texturedObj;
  private static Texture displayTexture;
  private static final Vector2i oldProjectionSize = new Vector2i();
  private static EngineState.RenderMode oldRenderMode;
  private static final Vector3i oldClearColour = new Vector3i();
  private static final Matrix4f transforms = new Matrix4f();

  private static Runnable onRender;
  private static Runnable onFinish;

  public static void play(final Path video, @Nullable final Runnable onRender, @Nullable final Runnable onFinish) throws IOException {
    LOGGER.info("Playing FMV %s", video);

    VideoPlayer.onRender = onRender;
    VideoPlayer.onFinish = onFinish;

    shouldStop = false;

    grabber = VideoDecoders.open(video);
    videoWidth = grabber.width();
    videoHeight = grabber.height();

    LOGGER.info("Video size %dx%d", videoWidth, videoHeight);

    displayTexture = Texture.create("Video", builder -> {
      builder.size(videoWidth, videoHeight);
      builder.internalFormat(TextureInternalFormat.RGB_8);
      builder.dataFormat(TextureDataFormat.RGB);
      builder.minFilter(true);
      builder.magFilter(true);
    });

    oldAllowWidescreen = CONFIG.getConfig(ALLOW_WIDESCREEN_CONFIG.get());
    oldFps = RENDERER.window().getFpsLimit();
    oldProjectionSize.set(RENDERER.getNativeWidth(), RENDERER.getNativeHeight());
    oldRenderMode = RENDERER.getRenderMode();
    oldClearColour.set(clearRed_8007a3a8, clearGreen_800bb104, clearBlue_800babc0);

    CONFIG.setConfig(ALLOW_WIDESCREEN_CONFIG.get(), true);
    RENDERER.setRenderMode(EngineState.RenderMode.PERSPECTIVE);
    RENDERER.setProjectionSize(320, 240);
    RENDERER.api().clearColour(0.0f, 0.0f, 0.0f);

    keyPress = RENDERER.events().onKeyPress((window, key, scancode, mods, repeat) -> shouldStop = true);
    buttonPressed = RENDERER.events().onButtonPress((window, action, repeat) -> shouldStop = true);
    click = RENDERER.events().onMouseRelease((window, x, y, button, mods) -> shouldStop = true);

    final int sampleRate = grabber.sampleRate();
    final int channels = grabber.channels();
    if(sampleRate <= 0 || (channels != 1 && channels != 2)) { grabber.close(); throw new IOException("Movie must have mono/stereo PCM audio"); }
    inputAction = RENDERER.events().onInputActionPressed((window,action,repeat)->{if(action==CoreMod.INPUT_ACTION_FMV_SKIP.get()&&!repeat)shouldStop=true;});
    source = AUDIO_THREAD.addSource(new GenericSource(0x1103, sampleRate, false));
    final float volume = CONFIG.getConfig(CoreMod.FMV_VOLUME_CONFIG.get()) * CONFIG.getConfig(CoreMod.MASTER_VOLUME_CONFIG.get());
    final long maxBytes = grabber.durationMicros() * sampleRate / 1_000_000L * 4 + 65536;
    if(maxBytes <= 0 || maxBytes > 128L * 1024 * 1024) { throw new IOException("Movie audio duration exceeds the buffering limit"); }
    pcmBuffer = BufferUtils.createByteBuffer((int)maxBytes);
    short[] samples;
    short[] mixed = new short[0];
    while((samples = grabber.readAudio()) != null) {
      mixed = appendPcm(pcmBuffer, samples, channels, volume, mixed);
    }
    pcmBuffer.flip();
    audioDurationMicros = pcmBuffer.remaining() / 4L * 1_000_000L / sampleRate;
    audioStarted = false;
    source.bufferOutput(pcmBuffer);
    lastPosition = 0.0f;
    grabber.rewindVideo();
    currentFrame = grabber.readVideo();
    if(currentFrame == null) { throw new IOException("Movie has no video frames"); }
    LOGGER.info("Movie ready: %s, audioUs=%d", video, audioDurationMicros);

    oldRenderer = RENDERER.setRenderCallback(() -> {
      if(onRender != null) {
        onRender.run();
      }

      if(shouldStop) {
        stop();
        return;
      }

      RENDERER.window().setFpsLimit(60 * Config.getGameSpeedMultiplier());
      PLATFORM.setInputTickRate(60 * Config.getGameSpeedMultiplier());

      if(!audioStarted){source.startPlayback();audioStarted=true;}
      // We pin video playback to audio playback
      final long audioTimeMicro = (long)(source.getPosition() * 1_000_000L);
      if(audioTimeMicro >= audioDurationMicros - 15_000L) { stop(); return; }
      final long toleranceMicro = 15_000L; // 15ms tolerance

      try {
        // Keep rendering and input alive when decoding is slower than the audio clock.
        currentFrame = advanceVideo(grabber, currentFrame, audioTimeMicro, toleranceMicro);
        if(currentFrame == null) { stop(); return; }
        if(currentFrame.timestampMicros() <= audioTimeMicro + toleranceMicro) {
          final ByteBuffer buffer = currentFrame.pixels();
          displayTexture.data(0, 0, videoWidth, videoHeight, TextureDataType.UBYTE, buffer);
        }
      } catch(final Exception e) {
        LOGGER.warn("Error while playing video", e);
      }

      if(texturedObj == null) {
        texturedObj = new QuadBuilder("FMV")
          .bpp(Bpp.BITS_24)
          .size(1.0f, 1.0f)
          .build();
      }

      displayTexture.use();

      final float windowHeight = RENDERER.getNativeHeight();
      final float windowWidth = windowHeight * RENDERER.getRenderAspectRatio();

      final float scaleW = windowWidth / 320.0f;
      final float scaleH = windowHeight / videoHeight;
      final float scale = Math.min(scaleW, scaleH);

      final float w = videoWidth * scale;
      final float h = videoHeight * scale;

      final float l = (windowWidth - w) / 2.0f;
      final float t = (windowHeight - h) / 2.0f;

      transforms
        .translation(l, t, 100.0f)
        .scale(w, h, 1.0f)
      ;

      RENDERER.queueOrthoModel(texturedObj, transforms, QueuedModelStandard.class)
        .texture(displayTexture)
      ;

      DISCORD.tick();

      if(!source.isActive() || source.getPosition() < lastPosition) {
        stop();
      }

      lastPosition = source.getPosition();
    });
  }

  static VideoDecoder.Frame advanceVideo(final VideoDecoder decoder, VideoDecoder.Frame frame, final long audioMicros, final long toleranceMicros) throws IOException {
    final long start = System.nanoTime();
    int decoded = 0;
    while(frame.timestampMicros() < audioMicros - toleranceMicros && decoded < 4) {
      frame = decoder.readVideo();
      if(frame == null) return null;
      decoded++;
      if(System.nanoTime() - start >= 8_000_000L) break;
    }
    return frame;
  }

  /** Batch direct-buffer writes rather than crossing into native memory once per sample. */
  static short[] appendPcm(final ByteBuffer output, final short[] samples, final int channels, final float volume, short[] mixed) throws IOException {
    if(channels != 1 && channels != 2) throw new IOException("Movie audio must be mono or stereo");
    if(samples.length % channels != 0) throw new IOException("Partial movie PCM frame");
    final int count = Math.multiplyExact(samples.length / channels, 2);
    if(count > output.remaining() / 2) throw new IOException("Movie audio exceeds declared duration");
    if(mixed.length < count) mixed = new short[count];
    for(int i = 0, out = 0; i < samples.length; i += channels) {
      mixed[out++] = (short)(samples[i] * volume);
      mixed[out++] = (short)(samples[i + channels - 1] * volume);
    }
    output.asShortBuffer().put(mixed, 0, count);
    output.position(output.position() + count * 2);
    return mixed;
  }

  public static void stop() {
    RENDERER.setRenderCallback(() -> {
      if(texturedObj != null) {
        texturedObj.delete();
        texturedObj = null;
      }

      if(displayTexture != null) {
        displayTexture.delete();
        displayTexture = null;
      }

      if(keyPress != null) {
        RENDERER.events().removeKeyPress(keyPress);
        keyPress = null;
      }

      if(click != null) {
        RENDERER.events().removeMouseRelease(click);
        click = null;
      }

      if(inputAction != null){RENDERER.events().removeInputActionPressed(inputAction);inputAction=null;}
      if(buttonPressed != null) {
        RENDERER.events().removeButtonPress(buttonPressed);
        buttonPressed = null;
      }

      CONFIG.setConfig(ALLOW_WIDESCREEN_CONFIG.get(), oldAllowWidescreen);
      RENDERER.setRenderCallback(oldRenderer);
      RENDERER.window().setFpsLimit(oldFps);
      PLATFORM.setInputTickRate(oldFps);
      RENDERER.setRenderMode(oldRenderMode);
      RENDERER.setProjectionSize(oldProjectionSize.x, oldProjectionSize.y);
      clearRed_8007a3a8 = oldClearColour.x;
      clearGreen_800bb104 = oldClearColour.y;
      clearBlue_800babc0 = oldClearColour.z;

      oldRenderer = null;

      if(grabber != null) {
        try {
          grabber.close();
        } catch(final IOException e) {
          LOGGER.warn("Failed to clean up ffmpeg", e);
        }
      }

      if(onRender != null) {
        onRender.run();
      }

      if(onFinish != null) {
        onFinish.run();
      }

      onRender = null;
      onFinish = null;

      if(pcmBuffer != null) {
        pcmBuffer = null;
      }

      AUDIO_THREAD.removeSource(source);
      source = null;
    });
  }
}
