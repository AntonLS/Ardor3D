/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.logical;

import java.util.function.Predicate;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;

/**
 * A condition that is true if a given button was clicked (has a click count) when going from the
 * previous input state to the current one.
 */
@Immutable
public final class MouseButtonClickedCondition implements Predicate<TwoInputStates> {
  private final MouseButton _button;

  /**
   * Construct a new MouseButtonClickedCondition.
   *
   * @param button
   *          the button that should be "clicked" to trigger this condition
   * @throws IllegalArgumentException
   *           if the button is null
   */
  public MouseButtonClickedCondition(final MouseButton button) {
    if (button == null) {
      throw new IllegalArgumentException("button was null");
    }

    _button = button;
  }

  @Override
  public boolean test(final TwoInputStates states) {
    final MouseState currentState = states.getCurrent().getMouseState();
    final MouseState previousState = states.getPrevious().getMouseState();

    return !currentState.getButtonsReleasedSince(previousState).isEmpty()
        && currentState.getButtonsClicked().contains(_button);
  }
}
