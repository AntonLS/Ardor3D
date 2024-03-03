/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.keyboard;

import java.io.Serial;

/**
 * Thrown when an attempt at fetching a {@link Key} instance for an invalid/unknown key code is
 * made.
 */
public class KeyNotFoundException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public KeyNotFoundException(final int keyCode) {
    super("No Key enum value found for code: " + keyCode);
  }
}
