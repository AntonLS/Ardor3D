/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.effect;

import java.nio.FloatBuffer;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.light.LightProperties;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * A demonstration of texturing Points.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.effect.PointSpritesExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_PointSpritesExample.jpg", //
    maxHeapMemory = 64)
public class PointSpritesExample extends ExampleBase {
  private final int _spriteCount = 60000;
  private Point _points;

  private final Matrix3 rotation = new Matrix3();

  public static void main(final String[] args) {
    start(PointSpritesExample.class);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    _points.setProperty("time", (float) timer.getTimeInSeconds());

    rotation.fromAngles(0.0 * timer.getTimeInSeconds(), 0.1 * timer.getTimeInSeconds(), 0.0 * timer.getTimeInSeconds());
    _points.setRotation(rotation);
  }

  @Override
  protected void initExample() {
    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
    final RenderContext renderContext = canvasRenderer.getRenderContext();
    final Renderer renderer = canvasRenderer.getRenderer();
    GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(() -> {
      renderer.setBackgroundColor(ColorRGBA.BLACK);
      return null;
    });
    _canvas.setVSyncEnabled(true);
    _canvas.setTitle("PointSprites");
    cam.setLocation(new Vector3(0, 30, 40));
    cam.lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);

    buildPointSprites();
  }

  private void buildPointSprites() {
    _points = new Point();
    _points.setPointSize(20);

    LightProperties.setLightReceiver(_points, false);

    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("images/flare.png", Texture.MinificationFilter.NearestNeighborNoMipMaps,
        TextureStoreFormat.GuessCompressedFormat, true));
    ts.getTexture().setWrap(WrapMode.EdgeClamp);
    ts.setEnabled(true);
    _points.setRenderState(ts);

    final ZBufferState zb = new ZBufferState();
    zb.setWritable(false);
    _points.setRenderState(zb);

    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    blend.setEnabled(true);
    blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
    blend.setDestinationFunction(BlendState.DestinationFunction.One);
    _points.setRenderState(blend);

    final FloatBuffer vBuf = BufferUtils.createVector3Buffer(_spriteCount);
    final FloatBuffer cBuf = BufferUtils.createVector4Buffer(_spriteCount);
    final Vector3 position = new Vector3();
    double x, y, r;
    for (int i = 0; i < _spriteCount; i++) {
      random(20, position);
      x = position.getX();
      y = position.getY();
      r = Math.sqrt(x * x + y * y);
      vBuf.put((float) x).put((float) (12 - 0.5 * r) * i / _spriteCount).put((float) y);
      final float rnd = (float) Math.random();
      final float rnd2 = rnd * (0.8f - 0.2f * (float) Math.random());
      cBuf.put((float) (20 - 0.9 * r) / 20 * (rnd + (1 - rnd) * i / _spriteCount)) //
          .put((float) (20 - 0.9 * r) / 20 * (rnd2 + (1 - rnd2) * i / _spriteCount)) //
          .put((float) (20 - 0.9 * r) / 20 * (0.2f + 0.2f * i / _spriteCount)) //
          .put((float) r);
    }
    _points.getMeshData().setVertexBuffer(vBuf);
    _points.getMeshData().setColorBuffer(cBuf);

    _points.setRenderMaterial("point_sprites_example.yaml");

    _root.attachChild(_points);
  }

  public static void random(final float factor, final Vector3 store) {
    double x, y, z, len;

    do {
      x = 2 * Math.random() - 1.0;
      y = 2 * Math.random() - 1.0;
      z = 2 * Math.random() - 1.0;
      len = x * x + y * y + z * z;
    } while (len > 1);

    len = factor / Math.sqrt(len);
    store.set(x, y, z);
    store.multiplyLocal(len);
  }
}
