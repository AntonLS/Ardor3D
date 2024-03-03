/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.layout.UILayout;

/**
 * The most basic implementation of a UI container.
 */
public class UIPanel extends UIContainer {

  public UIPanel() {}

  public UIPanel(final String name) {
    setName(name);
  }

  public UIPanel(final UILayout layout) {
    setLayout(layout);
  }

  public UIPanel(final String name, final UILayout layout) {
    setName(name);
    setLayout(layout);
  }
}
