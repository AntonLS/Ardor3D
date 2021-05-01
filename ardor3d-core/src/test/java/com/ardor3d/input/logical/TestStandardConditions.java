/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.logical;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.EnumSet;

import org.junit.Test;

import com.ardor3d.input.InputState;
import com.ardor3d.input.character.CharacterInputState;
import com.ardor3d.input.controller.ControllerState;
import com.ardor3d.input.gesture.GestureState;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyEvent;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;

public class TestStandardConditions {
  final KeyboardState ks = new KeyboardState(EnumSet.noneOf(Key.class), KeyEvent.NOTHING);
  final MouseState ms =
      new MouseState(0, 0, 0, 0, 0, MouseButton.makeMap(ButtonState.UP, ButtonState.UP, ButtonState.UP), null);
  final ControllerState cs = new ControllerState();
  final GestureState gs = new GestureState();
  final CharacterInputState cis = new CharacterInputState();
  InputState is1, is2, is3, is4, is5;

  KeyboardState aDown = new KeyboardState(EnumSet.of(Key.A), KeyEvent.NOTHING);
  KeyboardState bDown = new KeyboardState(EnumSet.of(Key.B), KeyEvent.NOTHING);

  EnumMap<MouseButton, ButtonState> bothUp = MouseButton.makeMap(ButtonState.UP, ButtonState.UP, ButtonState.UP);
  EnumMap<MouseButton, ButtonState> upDown = MouseButton.makeMap(ButtonState.UP, ButtonState.DOWN, ButtonState.UP);
  EnumMap<MouseButton, ButtonState> downUp = MouseButton.makeMap(ButtonState.DOWN, ButtonState.UP, ButtonState.UP);
  EnumMap<MouseButton, ButtonState> bothDown = MouseButton.makeMap(ButtonState.DOWN, ButtonState.DOWN, ButtonState.UP);

  @Test
  public void testKeyHeld1() throws Exception {
    final KeyHeldCondition kh = new KeyHeldCondition(Key.A);

    is1 = new InputState(ks, ms, cs, gs, cis);
    is2 = new InputState(aDown, ms, cs, gs, cis);
    is3 = new InputState(bDown, ms, cs, gs, cis);

    assertFalse("not down", kh.test(new TwoInputStates(is1, is1)));
    assertTrue("down", kh.test(new TwoInputStates(is1, is2)));
    assertFalse("not down", kh.test(new TwoInputStates(is1, is3)));
    assertFalse("not down", kh.test(new TwoInputStates(is2, is3)));
    assertTrue("not down", kh.test(new TwoInputStates(is2, is2)));

    assertFalse("empty1", kh.test(new TwoInputStates(InputState.EMPTY, InputState.EMPTY)));
    assertFalse("empty2", kh.test(new TwoInputStates(is1, InputState.EMPTY)));
    assertFalse("empty3", kh.test(new TwoInputStates(InputState.EMPTY, is1)));
    assertTrue("empty4", kh.test(new TwoInputStates(InputState.EMPTY, is2)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testKeyHeld2() throws Exception {
    new KeyHeldCondition(null);
  }

  @Test
  public void testKeyPressed() throws Exception {
    final KeyPressedCondition kh = new KeyPressedCondition(Key.A);

    is1 = new InputState(ks, ms, cs, gs, cis);
    is2 = new InputState(aDown, ms, cs, gs, cis);
    is3 = new InputState(bDown, ms, cs, gs, cis);

    assertFalse("not down", kh.test(new TwoInputStates(is1, is1)));
    assertTrue("down", kh.test(new TwoInputStates(is1, is2)));
    assertFalse("not down", kh.test(new TwoInputStates(is1, is3)));
    assertFalse("not down", kh.test(new TwoInputStates(is2, is3)));
    assertFalse("not down", kh.test(new TwoInputStates(is2, is2)));

    assertFalse("empty1", kh.test(new TwoInputStates(InputState.EMPTY, InputState.EMPTY)));
    assertFalse("empty2", kh.test(new TwoInputStates(is1, InputState.EMPTY)));
    assertFalse("empty3", kh.test(new TwoInputStates(InputState.EMPTY, is1)));
    assertTrue("empty4", kh.test(new TwoInputStates(InputState.EMPTY, is2)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testKeyPressedNull() throws Exception {
    new KeyPressedCondition(null);
  }

  @Test
  public void testKeyReleased() throws Exception {
    final KeyReleasedCondition kh = new KeyReleasedCondition(Key.A);

    is1 = new InputState(ks, ms, cs, gs, cis);
    is2 = new InputState(aDown, ms, cs, gs, cis);
    is3 = new InputState(bDown, ms, cs, gs, cis);

    assertFalse("not down", kh.test(new TwoInputStates(is1, is1)));
    assertFalse("not down", kh.test(new TwoInputStates(is1, is2)));
    assertFalse("not down", kh.test(new TwoInputStates(is1, is3)));
    assertTrue("not down", kh.test(new TwoInputStates(is2, is3)));
    assertFalse("not down", kh.test(new TwoInputStates(is2, is2)));

    assertFalse("empty1", kh.test(new TwoInputStates(InputState.EMPTY, InputState.EMPTY)));
    assertFalse("empty2", kh.test(new TwoInputStates(is1, InputState.EMPTY)));
    assertFalse("empty3", kh.test(new TwoInputStates(InputState.EMPTY, is1)));
    assertFalse("empty4", kh.test(new TwoInputStates(InputState.EMPTY, is2)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testKeyReleasedNull() throws Exception {
    new KeyReleasedCondition(null);
  }

  @Test
  public void testMouseMove() throws Exception {
    final MouseMovedCondition mm = TriggerConditions.mouseMoved();

    final MouseState ms2 = new MouseState(1, 0, 1, 0, 0, bothUp, null);
    final MouseState ms3 = new MouseState(1, 0, 0, 0, 0, bothDown, null);
    final MouseState ms4 = new MouseState(3, 1, 2, 1, 0, bothDown, null);
    final MouseState ms5 = new MouseState(3, 0, 0, -1, 0, bothDown, null);

    is1 = new InputState(ks, ms, cs, gs, cis);
    is2 = new InputState(ks, ms2, cs, gs, cis);
    is3 = new InputState(ks, ms3, cs, gs, cis);
    is4 = new InputState(ks, ms4, cs, gs, cis);
    is5 = new InputState(ks, ms5, cs, gs, cis);

    assertFalse("mm1", mm.test(new TwoInputStates(is1, is1)));
    assertTrue("mm2", mm.test(new TwoInputStates(is1, is2)));
    assertFalse("mm3", mm.test(new TwoInputStates(is2, is3)));
    assertTrue("mm4", mm.test(new TwoInputStates(is3, is4)));
    assertTrue("mm5", mm.test(new TwoInputStates(is4, is5)));
    assertFalse("mm6", mm.test(new TwoInputStates(is2, is2)));

    assertFalse("empty1", mm.test(new TwoInputStates(InputState.EMPTY, InputState.EMPTY)));
    assertFalse("empty2", mm.test(new TwoInputStates(is1, InputState.EMPTY)));
    assertFalse("empty3", mm.test(new TwoInputStates(InputState.EMPTY, is1)));
    assertTrue("empty4", mm.test(new TwoInputStates(InputState.EMPTY, is2)));
  }

  @Test
  public void testMouseButton1() throws Exception {
    final MouseButtonCondition mm = TriggerConditions.leftButtonDown();

    final MouseState ms2 = new MouseState(1, 0, 1, 0, 0, bothUp, null);
    final MouseState ms3 = new MouseState(1, 0, 0, 0, 0, bothDown, null);
    final MouseState ms4 = new MouseState(3, 1, 2, 1, 0, upDown, null);
    final MouseState ms5 = new MouseState(3, 0, 0, -1, 0, downUp, null);

    is1 = new InputState(ks, ms, cs, gs, cis);
    is2 = new InputState(ks, ms2, cs, gs, cis);
    is3 = new InputState(ks, ms3, cs, gs, cis);
    is4 = new InputState(ks, ms4, cs, gs, cis);
    is5 = new InputState(ks, ms5, cs, gs, cis);

    assertFalse("mm1", mm.test(new TwoInputStates(is1, is1)));
    assertFalse("mm2", mm.test(new TwoInputStates(is1, is2)));
    assertTrue("mm3", mm.test(new TwoInputStates(is2, is3)));
    assertFalse("mm4", mm.test(new TwoInputStates(is3, is4)));
    assertTrue("mm5", mm.test(new TwoInputStates(is4, is5)));

    assertFalse("empty1", mm.test(new TwoInputStates(InputState.EMPTY, InputState.EMPTY)));
    assertFalse("empty2", mm.test(new TwoInputStates(is1, InputState.EMPTY)));
    assertFalse("empty3", mm.test(new TwoInputStates(InputState.EMPTY, is1)));
    assertTrue("empty4", mm.test(new TwoInputStates(InputState.EMPTY, is3)));
  }

  @Test
  public void testMouseButton2() throws Exception {
    final MouseButtonCondition mm = TriggerConditions.rightButtonDown();

    final MouseState ms2 = new MouseState(1, 0, 1, 0, 0, bothUp, null);
    final MouseState ms3 = new MouseState(1, 0, 0, 0, 0, bothDown, null);
    final MouseState ms4 = new MouseState(3, 1, 2, 1, 0, upDown, null);
    final MouseState ms5 = new MouseState(3, 0, 0, -1, 0, downUp, null);

    is1 = new InputState(ks, ms, cs, gs, cis);
    is2 = new InputState(ks, ms2, cs, gs, cis);
    is3 = new InputState(ks, ms3, cs, gs, cis);
    is4 = new InputState(ks, ms4, cs, gs, cis);
    is5 = new InputState(ks, ms5, cs, gs, cis);

    assertFalse("mm1", mm.test(new TwoInputStates(is1, is1)));
    assertFalse("mm2", mm.test(new TwoInputStates(is1, is2)));
    assertTrue("mm3", mm.test(new TwoInputStates(is2, is3)));
    assertTrue("mm4", mm.test(new TwoInputStates(is3, is4)));
    assertFalse("mm5", mm.test(new TwoInputStates(is4, is5)));

    assertFalse("empty1", mm.test(new TwoInputStates(InputState.EMPTY, InputState.EMPTY)));
    assertFalse("empty2", mm.test(new TwoInputStates(is1, InputState.EMPTY)));
    assertFalse("empty3", mm.test(new TwoInputStates(InputState.EMPTY, is1)));
    assertTrue("empty4", mm.test(new TwoInputStates(InputState.EMPTY, is3)));
  }
}
