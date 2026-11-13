package legend.core.renderer.android;
import android.opengl.GLES32;
import static android.opengl.GLES32.*;
import legend.core.gpu.Rect4i;
import legend.core.renderer.*;
import legend.game.EngineState;
import legend.game.modding.coremod.CoreMod;
import static legend.core.GameEngine.CONFIG;
import java.io.IOException;
import java.nio.*;
import java.nio.file.Path;
import java.util.function.*;

public final class AndroidRenderApi implements RenderApi {
 private final AndroidShader.Source shaderSource;
 private static volatile String graphicsDiagnostic="El motor todavia no ha iniciado el renderizado.";
 public static String graphicsDiagnostic(){return graphicsDiagnostic;}
 public AndroidRenderApi(AndroidShader.Source shaderSource){this.shaderSource=java.util.Objects.requireNonNull(shaderSource);}
 public void init(){
  int[] major=new int[1],minor=new int[1],attributes=new int[1];glGetIntegerv(GL_MAJOR_VERSION,major,0);glGetIntegerv(GL_MINOR_VERSION,minor,0);glGetIntegerv(GL_MAX_VERTEX_ATTRIBS,attributes,0);
  if(major[0]<3||(major[0]==3&&minor[0]<2))throw new IllegalStateException("The current render path requires GLES 3.2");
  if(attributes[0]<11)throw new IllegalStateException("The mobile TMD path requires eleven vertex attributes");
  String probe=TmdRenderProbe.run(this);
  graphicsDiagnostic="Dragoon 0.9-rose-refined\n"+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+"\nAndroid "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\nGPU: "+glGetString(GL_RENDERER)+"\nProveedor: "+glGetString(GL_VENDOR)+"\n"+glGetString(GL_VERSION)+"\nTMD: triangulos GLES, sin geometry shader\nPrueba 3D: "+probe;
  android.util.Log.i("DragoonGL",graphicsDiagnostic);
  try{legend.core.TextFiles.writeString(legend.core.GamePaths.resolve("graphics-diagnostics.txt"),graphicsDiagnostic);}catch(java.io.IOException ignored){}
 }

  private boolean backfaceCulling;

  private RenderBatch batch;
  private boolean widescreen;
  private float w;
  private float h;
  private int renderWidth;
  private int renderHeight;

  private final Rect4i tempScissorRect = new Rect4i();
  private final Rect4i activeScissorRect = new Rect4i();

  private boolean depthTest;
  private int depthComparator;

  private Translucency translucency;

  private boolean debugEnabled;

  @Override
  public void resize(final int renderWidth, final int renderHeight) {
    this.renderWidth = renderWidth;
    this.renderHeight = renderHeight;

    glLineWidth(Math.max(1, renderHeight / 480.0f));
  }

  @Override
  public Mesh makeMesh(final String name, final VertexOrder vertexOrder, final float[] vertexData, final int[] indices) {
    return new AndroidMesh(name, vertexOrder, vertexData, indices, false, false, null, BufferUsage.STATIC, vertexOrder==VertexOrder.TRIANGLES_ADJACENCY);
  }

  @Override
  public Mesh makeMesh(final String name, final VertexOrder vertexOrder, final float[] vertexData, final int[] indices, final boolean textured, final boolean translucent, final Translucency translucencyMode, final BufferUsage bufferUsage) {
    return new AndroidMesh(name, vertexOrder, vertexData, indices, textured, translucent, translucencyMode, bufferUsage, vertexOrder==VertexOrder.TRIANGLES_ADJACENCY);
  }

  @Override
  public Mesh makeMesh(final String name, final VertexOrder vertexOrder, final float[] vertexData, final int vertexCount) {
    return new AndroidMesh(name, vertexOrder, vertexData, vertexCount, false, false, null, BufferUsage.STATIC, vertexOrder==VertexOrder.TRIANGLES_ADJACENCY);
  }

  @Override
  public Mesh makeMesh(final String name, final VertexOrder vertexOrder, final float[] vertexData, final int vertexCount, final boolean textured, final boolean translucent, final Translucency translucencyMode, final BufferUsage bufferUsage) {
    return new AndroidMesh(name, vertexOrder, vertexData, vertexCount, textured, translucent, translucencyMode, bufferUsage, vertexOrder==VertexOrder.TRIANGLES_ADJACENCY);
  }

  @Override
  public Texture makeTexture(final Buffer buffer, final String name, final int w, final int h, final TextureInternalFormat internalFormat, final TextureDataFormat dataFormat, final TextureDataType dataType, final boolean minFilter, final boolean magFilter, final boolean wrapS, final boolean wrapT) {
    return new AndroidTexture(buffer, name, w, h, internalFormat, dataFormat, dataType, minFilter, magFilter, wrapS, wrapT);
  }

  @Override
  public FrameBuffer makeFrameBuffer(final String name, final FrameBufferAttachment[] attachments) {
    return new AndroidFrameBuffer(name, attachments);
  }

  @Override
  public <Options extends ShaderOptions> Shader<Options> makeShader(final String name, final Path vert, final Path frag, final Function<Shader<Options>, Supplier<Options>> options) throws IOException {
    return new AndroidShader<>(name, vert, frag, shaderSource, options);
  }

  @Override
  public <Options extends ShaderOptions> Shader<Options> makeShader(final String name, final Path vert, final Path geom, final Path frag, final Function<Shader<Options>, Supplier<Options>> options) throws IOException {
    if(geom.getFileName().toString().equals("tmd.gsh")) {
      String base=vert.getFileName().toString().equals("battle_tmd.vsh")?"compat_battle_tmd":"compat_tmd";
      return new AndroidShader<>(name,vert.resolveSibling(base+".vsh"),frag.resolveSibling(base+".fsh"),shaderSource,options);
    }
    return new AndroidShader<>(name,vert,geom,frag,shaderSource,options);
  }

  @Override
  public ShaderUniformBuffer makeUniformBuffer(final long size, final int binding) {
    return new AndroidUniformBuffer(size, binding);
  }

  @Override
  public void clear(final boolean colour, final boolean depth, final boolean stencil) {
    int mask = 0;

    if(colour) {
      mask |= GL_COLOR_BUFFER_BIT;
    }

    if(depth) {
      mask |= GL_DEPTH_BUFFER_BIT;
    }

    if(stencil) {
      mask |= GL_STENCIL_BUFFER_BIT;
    }

    glClear(mask);
  }

  @Override
  public void clearColour(final float r, final float g, final float b) {
    glClearColor(r, g, b, 1.0f);
  }

  @Override
  public void viewport(final int x, final int y, final int w, final int h) {
    glViewport(x, y, w, h);
  }

  @Override
  public void unbindFramebuffer() {
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
  }

  @Override
  public void unbindTexture() {
    AndroidTexture.unbind();
  }

  @Override
  public void initBatch(final RenderBatch batch) {
    this.batch = batch;
    this.widescreen = batch.getRenderMode() == EngineState.RenderMode.PERSPECTIVE && CoreMod.ALLOW_WIDESCREEN_CONFIG.isValid() && CONFIG.getConfig(CoreMod.ALLOW_WIDESCREEN_CONFIG.get()) || batch.getRenderMode() == EngineState.RenderMode.LEGACY && CoreMod.LEGACY_WIDESCREEN_MODE_CONFIG.isValid() && CONFIG.getConfig(CoreMod.LEGACY_WIDESCREEN_MODE_CONFIG.get()) == SubmapWidescreenMode.EXPANDED;
    this.w = (float)this.renderWidth / batch.nativeWidth;
    this.h = (float)this.renderHeight / batch.nativeHeight;

    this.backfaceCulling(false);
  }

  @Override
  public void backfaceCulling(final boolean enable) {
    if(this.backfaceCulling != enable) {
      this.backfaceCulling = enable;

      if(enable) {
        glEnable(GL_CULL_FACE);
      } else {
        glDisable(GL_CULL_FACE);
      }
    }
  }

  @Override
  public void enableDepthTest(final DepthComparator comparator) {
    if(!this.depthTest) {
      glEnable(GL_DEPTH_TEST);
      this.depthTest = true;
    }

    final int value = switch(comparator) {
      case NEVER -> GL_NEVER;
      case LESS -> GL_LESS;
      case EQUAL -> GL_EQUAL;
      case LESS_THAN_OR_EQUAL -> GL_LEQUAL;
      case GREATER -> GL_GREATER;
      case NOT_EQUAL -> GL_NOTEQUAL;
      case GREATER_OR_EQUAL -> GL_GEQUAL;
      case ALWAYS -> GL_ALWAYS;
    };

    if(this.depthComparator != value) {
      glDepthFunc(value);
      this.depthComparator = value;
    }
  }

  @Override
  public void disableDepthTest() {
    if(this.depthTest) {
      glDisable(GL_DEPTH_TEST);
      this.depthTest = false;
    }
  }

  @Override
  public void scissor(final QueuedModel<?, ?> model, final FloatBuffer scissorBuffer, final ShaderUniformBuffer scissorUniform) {
    final Rect4i worldScissor = model.worldScissor();
    final Rect4i modelScissor = model.modelScissor();

    this.tempScissorRect.set(worldScissor.x, this.renderHeight - (worldScissor.y + worldScissor.h), worldScissor.w, worldScissor.h);

    if(modelScissor.w != 0 || modelScissor.h != 0) {
      if(this.widescreen) {
        this.tempScissorRect.subregion(Math.round((modelScissor.x + this.batch.widescreenOrthoOffsetX) * this.h * ((float)this.batch.expectedWidth / this.batch.nativeWidth)), this.renderHeight - Math.round((modelScissor.y + modelScissor.h) * this.h), Math.round(modelScissor.w * this.h * ((float)this.batch.expectedWidth / this.batch.nativeWidth)), Math.round(modelScissor.h * this.h));
      } else {
        final float offset;
        final float w;

        if(this.batch.getRenderMode() == EngineState.RenderMode.LEGACY && CONFIG.getConfig(CoreMod.LEGACY_WIDESCREEN_MODE_CONFIG.get()) == SubmapWidescreenMode.FORCED_4_3) {
          final float ratio = (float)this.renderWidth / this.renderHeight;
          final float adjustedW = this.batch.nativeHeight * ratio;
          offset = (adjustedW - this.batch.nativeWidth) / 2.0f;
          w = this.h;
        } else {
          offset = this.batch.widescreenOrthoOffsetX;
          w = this.w;
        }

        this.tempScissorRect.subregion(Math.round((modelScissor.x + offset) * w), this.renderHeight - Math.round((modelScissor.y + modelScissor.h) * this.h), Math.round(modelScissor.w * w), Math.round(modelScissor.h * this.h));
      }
    }

    this.applyScissor(scissorBuffer, scissorUniform);
  }

  @Override
  public void translucency(final Translucency translucency) {
    if(this.translucency != translucency) {
      if(this.translucency == null) {
        // Do not update the depth mask so that we don't prevent things further away than this from rendering
        glDepthMask(false);
        glEnable(GL_BLEND);
      } else if(translucency == null) {
        // Update the depth mask so nothing further away than this will render
        glDepthMask(true);
        glDisable(GL_BLEND);
      }

      if(translucency != null) switch(translucency) {
        case HALF_B_PLUS_HALF_F -> {
          glBlendEquation(GL_FUNC_ADD);
          glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        case B_PLUS_F -> {
          glBlendEquation(GL_FUNC_ADD);
          glBlendFunc(GL_ONE, GL_ONE);
        }

        case B_MINUS_F -> {
          glBlendEquation(GL_FUNC_REVERSE_SUBTRACT);
          glBlendFunc(GL_ONE, GL_ONE);
        }


        default -> throw new RuntimeException(translucency + " not yet supported");
      }

      this.translucency = translucency;
    }
  }

  @Override
  public void wireframe(final boolean enable) {
    if(enable) {
      android.util.Log.w("DragoonRender", "Wireframe is not available in GLES");
    }
  }

  @Override
  public boolean debugEnabled() {
    return this.debugEnabled;
  }

  private void applyScissor(final FloatBuffer scissorBuffer, final ShaderUniformBuffer scissorUniform) {
    if(!this.activeScissorRect.equals(this.tempScissorRect)) {
      scissorBuffer.put(0, this.tempScissorRect.x);
      scissorBuffer.put(1, this.tempScissorRect.y);
      scissorBuffer.put(2, this.tempScissorRect.w);
      scissorBuffer.put(3, this.tempScissorRect.h);
      scissorUniform.set(scissorBuffer);
      this.activeScissorRect.set(this.tempScissorRect);
    }
  }
}
