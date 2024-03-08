/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import java.util.Iterator;

/**
 * An iterator that allows the client to peek at the next element in the iteration, without advancing the iteration.
 *
 * @param <E> the type of elements returned by this iterator
 */
public interface PeekingIterator<E> extends Iterator<E> {
  /**
   * @return the next element in the iteration, without advancing the iteration
   */
  E peek();

  @Override
  E next();

  @Override
  void remove();
}