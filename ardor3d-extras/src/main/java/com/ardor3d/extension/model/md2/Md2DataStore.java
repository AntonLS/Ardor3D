/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.md2;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.model.util.KeyframeController;
import com.ardor3d.scenegraph.Mesh;

public class Md2DataStore {

  private final Mesh _mainMesh;
  private final KeyframeController<Mesh> _controller;

  private final List<String> _frameNames = new ArrayList<>();

  private final List<String> _skinNames = new ArrayList<>();

  public Md2DataStore(final Mesh mainMesh, final KeyframeController<Mesh> controller) {
    _mainMesh = mainMesh;
    _controller = controller;
  }

  public Mesh getScene() { return _mainMesh; }

  public KeyframeController<Mesh> getController() { return _controller; }

  public List<String> getFrameNames() { return _frameNames; }

  public int getFrameIndex(final String frameName) {
    return _frameNames.indexOf(frameName);
  }

  public List<String> getSkinNames() { return _skinNames; }
}
