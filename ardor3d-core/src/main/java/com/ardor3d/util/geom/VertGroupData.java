/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.geom;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.ardor3d.util.geom.GeometryTool.MatchCondition;

public class VertGroupData {

  public static final int DEFAULT_GROUP = 0;

  private final Map<Long, EnumSet<MatchCondition>> _groupConditions = new HashMap<>();
  private int[] _vertGroups = null;

  public VertGroupData() {}

  public void setGroupConditions(final long groupNumber, final EnumSet<MatchCondition> conditions) {
    _groupConditions.put(groupNumber, conditions);
  }

  public EnumSet<MatchCondition> getGroupConditions(final long groupNumber) {
    return _groupConditions.get(groupNumber);
  }

  public int getGroupForVertex(final int index) {
    if (_vertGroups != null) {
      return _vertGroups[index];
    }
    return DEFAULT_GROUP;
  }

  public void setVertGroups(final int[] vertGroupMap) { _vertGroups = vertGroupMap; }
}
