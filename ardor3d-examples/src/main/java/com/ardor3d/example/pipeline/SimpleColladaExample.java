/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.pipeline;

import java.io.IOException;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.MaterialUtil;

/**
 * Simplest example of loading a Collada model.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.pipeline.SimpleColladaExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_SimpleColladaExample.jpg", //
    maxHeapMemory = 64)
public class SimpleColladaExample extends ExampleBase {
  public static void main(final String[] args) {
    ExampleBase.start(SimpleColladaExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Ardor3D - Simple Collada Example");
    _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 5, 20));

    // Load the collada scene
    try {
      final ColladaStorage storage = new ColladaImporter().load("collada/sony/Seymour.dae");
      _root.attachChild(storage.getScene());
      MaterialUtil.autoMaterials(_root);
    } catch (final IOException ex) {
      ex.printStackTrace();
    }
  }
}
