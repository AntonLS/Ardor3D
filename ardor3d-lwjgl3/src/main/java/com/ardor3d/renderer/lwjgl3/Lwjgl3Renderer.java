/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.lwjgl3;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Logger;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GLUtil;

import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.AbstractRenderer;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.DrawBufferTarget;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderable;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.OffsetState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.StencilState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3BlendStateUtil;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3ColorMaskStateUtil;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3CullStateUtil;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3OffsetStateUtil;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3StencilStateUtil;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3TextureStateUtil;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3WireframeStateUtil;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3ZBufferStateUtil;
import com.ardor3d.scene.state.lwjgl3.util.Lwjgl3ScissorUtils;
import com.ardor3d.scene.state.lwjgl3.util.Lwjgl3ShaderUtils;
import com.ardor3d.scene.state.lwjgl3.util.Lwjgl3TextureUtils;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * <code>LwjglRenderer</code> provides an implementation of the <code>Renderer</code> interface
 * using LWJGL version 3.
 *
 * @see com.ardor3d.renderer.Renderer
 */
public class Lwjgl3Renderer extends AbstractRenderer {
  private static final Logger logger = Logger.getLogger(Lwjgl3Renderer.class.getName());

  protected Lwjgl3ShaderUtils _shaderUtils;
  protected Lwjgl3TextureUtils _textureUtils;
  protected Lwjgl3ScissorUtils _scissorUtils;

  /**
   * Constructor instantiates a new <code>Lwjgl3Renderer</code> object.
   */
  public Lwjgl3Renderer() {

    final int major = GL11C.glGetInteger(GL30C.GL_MAJOR_VERSION);
    final int minor = GL11C.glGetInteger(GL30C.GL_MINOR_VERSION);

    logger.info(String.format("Lwjgl3Renderer created with a %d.%d context.", major, minor));
    _shaderUtils = new Lwjgl3ShaderUtils(this);
    _textureUtils = new Lwjgl3TextureUtils();
    _scissorUtils = new Lwjgl3ScissorUtils();
    if (Constants.logOpenGLDebug) {
      GLUtil.setupDebugMessageCallback();
    }

    // Right now, we are deciding that we always want this. If someone comes up with a reason they don't
    // want this,
    // we can look into making it part of Texture/TextureState.
    GL11C.glEnable(GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS);
  }

  @Override
  public void renderBuckets() {
    renderBuckets(true, true);
  }

  @Override
  public void renderBuckets(final boolean doSort, final boolean doClear) {
    _processingQueue = true;
    if (doSort && doClear) {
      _queue.renderBuckets(this);
    } else {
      if (doSort) {
        _queue.sortBuckets();
      }
      _queue.renderOnly(this);
      if (doClear) {
        _queue.clearBuckets();
      }
    }
    _processingQueue = false;
  }

  @Override
  public void setBackgroundColor(final ReadOnlyColorRGBA color) {
    _backgroundColor.set(color);
    GL11C.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
        _backgroundColor.getAlpha());

  }

  /**
   * clear the render queue
   */
  @Override
  public void clearQueue() {
    _queue.clearBuckets();
  }

  @Override
  public void clearBuffers(final int buffers) {
    clearBuffers(buffers, false);
  }

  @Override
  public void clearBuffers(final int buffers, final boolean strict) {

    int clear = 0;

    if ((buffers & Renderer.BUFFER_COLOR) != 0) {
      clear |= GL11C.GL_COLOR_BUFFER_BIT;
    }

    if ((buffers & Renderer.BUFFER_DEPTH) != 0) {
      clear |= GL11C.GL_DEPTH_BUFFER_BIT;

      // make sure no funny business is going on in the z before clearing.
      if (defaultStateList.containsKey(RenderState.StateType.ZBuffer)) {
        defaultStateList.get(RenderState.StateType.ZBuffer).setNeedsRefresh(true);
        doApplyState(defaultStateList.get(RenderState.StateType.ZBuffer));
      }
    }

    if ((buffers & Renderer.BUFFER_STENCIL) != 0) {
      clear |= GL11C.GL_STENCIL_BUFFER_BIT;

      GL11C.glClearStencil(_stencilClearValue);
      GL11C.glStencilMask(~0);
      GL11C.glClear(GL11C.GL_STENCIL_BUFFER_BIT);
    }

    final RenderContext context = ContextManager.getCurrentContext();
    final RendererRecord record = context.getRendererRecord();

    if (strict) {
      // grab our camera to get width and height info.
      final Camera cam = Camera.getCurrentCamera();

      GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
      GL11C.glScissor(0, 0, cam.getWidth(), cam.getHeight());
      record.setClippingTestEnabled(true);
    }

    GL11C.glClear(clear);

    if (strict) {
      // put us back.
      Lwjgl3ScissorUtils.applyScissors(record);
    }
  }

  // Note that we will NOT ever swap - canvas will need to do that.
  @Override
  public void flushFrame(final boolean doSwap) {
    renderBuckets();

    GL11C.glFlush();
    if (doSwap) {
      doApplyState(defaultStateList.get(RenderState.StateType.ColorMask));
    }

    if (Constants.stats) {
      StatCollector.addStat(StatType.STAT_FRAMES, 1);
    }
  }

  @Override
  public void grabScreenContents(final ByteBuffer store, final ImageDataFormat format, final PixelDataType type,
      final int x, final int y, final int w, final int h) {
    final int pixFormat = Lwjgl3TextureUtils.getGLPixelFormat(format);
    final int pixDataType = Lwjgl3TextureUtils.getGLPixelDataType(type);
    GL11C.glReadPixels(x, y, w, h, pixFormat, pixDataType, store);
  }

  @Override
  public void draw(final Spatial s) {
    if (s != null) {
      s.onDraw(this);
    }
  }

  @Override
  public void flushGraphics() {
    GL11C.glFlush();
  }

  @Override
  public void finishGraphics() {
    GL11C.glFinish();
  }

  @Override
  public boolean checkAndAdd(final Spatial s) {
    final RenderBucketType bucketType = s.getSceneHints().getRenderBucketType();
    final Camera camera = Camera.getCurrentCamera();
    if (bucketType != RenderBucketType.Skip && camera.checkLayerPasses(s.getLayer())) {
      getQueue().addToQueue(s, bucketType);
      return true;
    }
    return false;
  }

  @Override
  public void checkCardError() throws Ardor3dException {
    // We're using GLUtil with its callback for this
  }

  @Override
  public void draw(final Renderable renderable) {
    if (renderable != null) {
      renderable.render(this);
    }
  }

  @Override
  public void setViewport(final int x, final int y, final int width, final int height) {
    GL11C.glViewport(x, y, width, height);
  }

  @Override
  public void setDepthRange(final double depthRangeNear, final double depthRangeFar) {
    GL11C.glDepthRange(depthRangeNear, depthRangeFar);
  }

  @Override
  public void setDrawBuffer(final DrawBufferTarget target) {
    final RendererRecord record = ContextManager.getCurrentContext().getRendererRecord();
    if (record.getDrawBufferTarget() != target) {
      int buffer = GL11C.GL_BACK;
      switch (target) {
        case Back:
          break;
        case Front:
          buffer = GL11C.GL_FRONT;
          break;
        case BackLeft:
          buffer = GL11C.GL_BACK_LEFT;
          break;
        case BackRight:
          buffer = GL11C.GL_BACK_RIGHT;
          break;
        case FrontLeft:
          buffer = GL11C.GL_FRONT_LEFT;
          break;
        case FrontRight:
          buffer = GL11C.GL_FRONT_RIGHT;
          break;
        case FrontAndBack:
          buffer = GL11C.GL_FRONT_AND_BACK;
          break;
        case Left:
          buffer = GL11C.GL_LEFT;
          break;
        case Right:
          buffer = GL11C.GL_RIGHT;
          break;
        case None:
          buffer = GL11C.GL_NONE;
          break;
      }

      GL11C.glDrawBuffer(buffer);
      record.setDrawBufferTarget(target);
    }
  }

  @Override
  public void setPointSize(final boolean shaderSizeControl, final float staticPointSize) {
    final RenderContext context = ContextManager.getCurrentContext();
    final RendererRecord record = context.getRendererRecord();

    if (record.isShaderPointSize() != shaderSizeControl) {
      record.setShaderPointSize(shaderSizeControl);
      if (shaderSizeControl) {
        GL11C.glEnable(GL32C.GL_PROGRAM_POINT_SIZE);
      } else {
        GL11C.glDisable(GL32C.GL_PROGRAM_POINT_SIZE);
      }
    }

    if (!shaderSizeControl) {
      GL11C.glPointSize(staticPointSize);
    }
  }

  @Override
  protected void doApplyState(final RenderState state) {
    if (state == null) {
      logger.warning("tried to apply a null state.");
      return;
    }
    switch (state.getType()) {
      case Texture:
        Lwjgl3TextureStateUtil.apply((TextureState) state);
        return;
      case Blend:
        Lwjgl3BlendStateUtil.apply((BlendState) state);
        return;
      case ColorMask:
        Lwjgl3ColorMaskStateUtil.apply((ColorMaskState) state);
        return;
      case Cull:
        Lwjgl3CullStateUtil.apply((CullState) state);
        return;
      case Offset:
        Lwjgl3OffsetStateUtil.apply(this, (OffsetState) state);
        return;
      case Stencil:
        Lwjgl3StencilStateUtil.apply((StencilState) state);
        return;
      case Wireframe:
        Lwjgl3WireframeStateUtil.apply(this, (WireframeState) state);
        return;
      case ZBuffer:
        Lwjgl3ZBufferStateUtil.apply((ZBufferState) state);
        return;
    }
    throw new IllegalArgumentException("Unknown state: " + state);
  }

  @Override
  public void drawArrays(final int start, final int count, final IndexMode mode, final int instanceCount) {
    final int glIndexMode = getGLIndexMode(mode);

    if (instanceCount == 1) {
      GL11C.glDrawArrays(glIndexMode, start, count);
    } else {
      GL31C.glDrawArraysInstanced(glIndexMode, start, count, instanceCount);
    }

    if (Constants.stats) {
      addStats(mode, count * (instanceCount > 0 ? instanceCount : 1));
    }
  }

  @Override
  public void drawElements(final IndexBufferData<?> indices, final int start, final int count, final IndexMode mode,
      final int instanceCount) {
    final int type = getGLDataType(indices);
    final int byteSize = indices.getByteCount();
    final int glIndexMode = getGLIndexMode(mode);

    if (instanceCount == 1) {
      GL11C.glDrawElements(glIndexMode, count, type, (long) start * byteSize);
    } else {
      GL31C.glDrawElementsInstanced(glIndexMode, count, type, (long) start * byteSize, instanceCount);
    }

    if (Constants.stats) {
      addStats(mode, count * (instanceCount > 0 ? instanceCount : 1));
    }
  }

  private int getGLIndexMode(final IndexMode indexMode) {
    int glMode = GL11C.GL_TRIANGLES;
    switch (indexMode) {
      case Triangles:
        glMode = GL11C.GL_TRIANGLES;
        break;
      case TriangleStrip:
        glMode = GL11C.GL_TRIANGLE_STRIP;
        break;
      case TriangleFan:
        glMode = GL11C.GL_TRIANGLE_FAN;
        break;
      case TrianglesAdjacency:
        glMode = GL32C.GL_TRIANGLES_ADJACENCY;
        break;
      case TriangleStripAdjacency:
        glMode = GL32C.GL_TRIANGLE_STRIP_ADJACENCY;
        break;
      case Lines:
        glMode = GL11C.GL_LINES;
        break;
      case LineStrip:
        glMode = GL11C.GL_LINE_STRIP;
        break;
      case LineLoop:
        glMode = GL11C.GL_LINE_LOOP;
        break;
      case LinesAdjacency:
        glMode = GL32C.GL_LINES_ADJACENCY;
        break;
      case LineStripAdjacency:
        glMode = GL32C.GL_LINE_STRIP_ADJACENCY;
        break;
      case Points:
        glMode = GL11C.GL_POINTS;
        break;
    }
    return glMode;
  }

  private int getGLDataType(final IndexBufferData<?> indices) {
    if (indices.getBuffer() instanceof ByteBuffer) {
      return GL11C.GL_UNSIGNED_BYTE;
    } else if (indices.getBuffer() instanceof ShortBuffer) {
      return GL11C.GL_UNSIGNED_SHORT;
    } else if (indices.getBuffer() instanceof IntBuffer) {
      return GL11C.GL_UNSIGNED_INT;
    }

    throw new IllegalArgumentException("Unknown buffer type: " + indices.getBuffer());
  }

  @Override
  public Lwjgl3ShaderUtils getShaderUtils() { return _shaderUtils; }

  @Override
  public Lwjgl3TextureUtils getTextureUtils() { return _textureUtils; }

  @Override
  public Lwjgl3ScissorUtils getScissorUtils() { return _scissorUtils; }

  @Override
  public Lwjgl3TextureRenderer createTextureRenderer(final int width, final int height, final int depthBits,
      final int samples) {
    return new Lwjgl3TextureRenderer(width, height, depthBits, samples, this,
        ContextManager.getCurrentContext().getCapabilities());
  }
}
