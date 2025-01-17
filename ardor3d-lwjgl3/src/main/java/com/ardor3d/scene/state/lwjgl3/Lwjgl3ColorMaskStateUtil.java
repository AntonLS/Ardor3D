/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scene.state.lwjgl3;

import org.lwjgl.opengl.GL11C;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.ColorMaskStateRecord;

public abstract class Lwjgl3ColorMaskStateUtil {

  public static void apply(final ColorMaskState state) {
    // ask for the current state record
    final RenderContext context = ContextManager.getCurrentContext();
    final ColorMaskStateRecord record = (ColorMaskStateRecord) context.getStateRecord(StateType.ColorMask);
    context.setCurrentState(StateType.ColorMask, state);

    if (state.isEnabled()) {
      if (!record.isValid() || !record.is(state.getRed(), state.getGreen(), state.getBlue(), state.getAlpha())) {
        GL11C.glColorMask(state.getRed(), state.getGreen(), state.getBlue(), state.getAlpha());
        record.set(state.getRed(), state.getGreen(), state.getBlue(), state.getAlpha());
      }
    } else if (!record.isValid() || !record.is(true, true, true, true)) {
      GL11C.glColorMask(true, true, true, true);
      record.set(true, true, true, true);
    }

    if (!record.isValid()) {
      record.validate();
    }
  }
}
