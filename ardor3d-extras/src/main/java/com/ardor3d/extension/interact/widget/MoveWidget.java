/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.MouseCursor;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Line3;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.ZBufferState.TestFunction;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.MaterialUtil;

public class MoveWidget extends AbstractInteractWidget {

  public static double DEFAULT_SCALE = 1.0;
  public static double MOUSEOVER_SCALE = 1.1;

  protected InteractArrow _lastArrow = null;

  protected InteractArrow _xArrow = null;
  protected InteractArrow _yArrow = null;
  protected InteractArrow _zArrow = null;

  protected ColorRGBA _xColor = new ColorRGBA(1, 0, 0, .65f);
  protected ColorRGBA _yColor = new ColorRGBA(0, 1, 0, .65f);
  protected ColorRGBA _zColor = new ColorRGBA(0, 0, 1, .65f);

  public static MouseCursor DEFAULT_CURSOR = null;

  public MoveWidget() {
    _handle = new Node("moveHandle");
    LightProperties.setLightReceiver(_handle, false);

    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    _handle.setRenderState(blend);

    final ZBufferState zstate = new ZBufferState();
    zstate.setFunction(TestFunction.LessThanOrEqualTo);
    _handle.setRenderState(zstate);

    _handle.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
    _handle.updateGeometricState(0);

    if (MoveWidget.DEFAULT_CURSOR != null) {
      setMouseOverCallback(new SetCursorCallback(MoveWidget.DEFAULT_CURSOR));
    }
  }

  public MoveWidget withXAxis() {
    return withXAxis(_xColor);
  }

  public MoveWidget withXAxis(final ReadOnlyColorRGBA color) {
    return withXAxis(color, 1.0, 0.15, 0, 0);
  }

  public MoveWidget withXAxis(final ReadOnlyColorRGBA color, final double scale, final double width,
      final double lengthGap, final double tipGap) {
    if (_xArrow != null) {
      _xArrow.removeFromParent();
    }
    _xColor.set(color);
    _xArrow = new InteractArrow("xMoveArrow", scale, width, lengthGap, tipGap);
    _xArrow.setDefaultColor(color);
    final Quaternion rotate = new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_Y);
    _xArrow.setRotation(rotate);
    _handle.attachChild(_xArrow);
    MaterialUtil.autoMaterials(_xArrow);
    return this;
  }

  public MoveWidget withYAxis() {
    return withYAxis(_yColor);
  }

  public MoveWidget withYAxis(final ReadOnlyColorRGBA color) {
    return withYAxis(color, 1.0, 0.15, 0, 0);
  }

  public MoveWidget withYAxis(final ReadOnlyColorRGBA color, final double scale, final double width,
      final double lengthGap, final double tipGap) {
    if (_yArrow != null) {
      _yArrow.removeFromParent();
    }
    _yColor.set(color);
    _yArrow = new InteractArrow("yMoveArrow", scale, width, lengthGap, tipGap);
    _yArrow.setDefaultColor(color);
    final Quaternion rotate = new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.NEG_UNIT_X);
    _yArrow.setRotation(rotate);
    _handle.attachChild(_yArrow);
    MaterialUtil.autoMaterials(_yArrow);
    return this;
  }

  public MoveWidget withZAxis() {
    return withZAxis(new ColorRGBA(0, 0, 1, .65f));
  }

  public MoveWidget withZAxis(final ReadOnlyColorRGBA color) {
    return withZAxis(color, 1.0, 0.15, 0, 0);
  }

  public MoveWidget withZAxis(final ReadOnlyColorRGBA color, final double scale, final double width,
      final double lengthGap, final double tipGap) {
    if (_zArrow != null) {
      _zArrow.removeFromParent();
    }
    _zColor.set(color);
    _zArrow = new InteractArrow("zMoveArrow", scale, width, lengthGap, tipGap);
    _zArrow.setDefaultColor(color);
    _handle.attachChild(_zArrow);
    MaterialUtil.autoMaterials(_zArrow);
    return this;
  }

  @Override
  public void targetDataUpdated(final InteractManager manager) {
    final Spatial target = manager.getSpatialTarget();
    if (target == null) {
      _handle.setRotation(Matrix3.IDENTITY);
    } else {
      target.updateGeometricState(0);

      // update arrow rotations from target
      if (_interactMatrix == InteractMatrix.Local) {
        _handle.setRotation(target.getWorldRotation());
      } else {
        _handle.setRotation(Matrix3.IDENTITY);
      }
    }

    _handle.setScale(calculateHandleScale(manager));
  }

  @Override
  protected double calculateHandleScale(final InteractManager manager) {
    return super.calculateHandleScale(manager) * (_mouseOver ? MoveWidget.MOUSEOVER_SCALE : MoveWidget.DEFAULT_SCALE);
  }

  @Override
  public void render(final Renderer renderer, final InteractManager manager) {
    final Spatial spat = manager.getSpatialTarget();
    if (spat == null) {
      return;
    }

    _handle.setTranslation(spat.getWorldTranslation());
    _handle.updateGeometricState(0);

    renderer.draw(_handle);
  }

  @Override
  public void processInput(final Canvas source, final TwoInputStates inputStates, final AtomicBoolean inputConsumed,
      final InteractManager manager) {

    final Camera camera = source.getCanvasRenderer().getCamera();
    final MouseState current = inputStates.getCurrent().getMouseState();
    final MouseState previous = inputStates.getPrevious().getMouseState();

    // first process mouse over state
    checkMouseOver(source, current, manager);

    // Now check drag status
    if (!checkShouldDrag(camera, current, previous, inputConsumed, manager)) {
      return;
    }

    // act on drag
    if (_lastDragSpatial != null && _lastDragSpatial.getParent() instanceof InteractArrow arrow) {
      _lastArrow = arrow;
      final Vector2 oldMouse = new Vector2(previous.getX(), previous.getY());
      final Vector3 loc = getNewOffset(arrow, oldMouse, current, camera, manager);
      final Transform transform = manager.getSpatialState().getTransform();
      transform.setTranslation(loc.addLocal(transform.getTranslation()));

      // apply our filters, if any, now that we've made updates.
      applyFilters(manager);
    }
  }

  protected Vector3 getNewOffset(final InteractArrow arrow, final Vector2 oldMouse, final MouseState current,
      final Camera camera, final InteractManager manager) {

    // calculate a plane running through the Arrow and facing the camera.
    _calcVec3A.set(_handle.getWorldTranslation());
    _calcVec3B.set(_calcVec3A).addLocal(camera.getLeft());
    _calcVec3C.set( //
        arrow == _xArrow ? Vector3.UNIT_X : //
            arrow == _yArrow ? Vector3.UNIT_Y : //
                Vector3.UNIT_Z);

    // rotate to arrow plane
    _handle.getRotation().applyPost(_calcVec3C, _calcVec3C);
    final Line3 arrowLine = new Line3(_calcVec3A, _calcVec3C.normalize(_calcVec3D));

    // make plane object
    final Plane pickPlane = new Plane().setPlanePoints(_calcVec3A, _calcVec3B, _calcVec3C.addLocal(_calcVec3A));

    // find out where we were hitting the plane before
    getPickRay(oldMouse, camera);
    if (!_calcRay.intersectsPlane(pickPlane, _calcVec3A)) {
      return _calcVec3A.zero();
    }

    // find out where we are hitting the plane now
    getPickRay(new Vector2(current.getX(), current.getY()), camera);
    if (!_calcRay.intersectsPlane(pickPlane, _calcVec3B)) {
      return _calcVec3A.zero();
    }

    // Cast us to the line along our arrow
    arrowLine.distanceSquared(_calcVec3A, _calcVec3C);
    arrowLine.distanceSquared(_calcVec3B, _calcVec3D);

    // convert to target coord space
    final Node parent = manager.getSpatialTarget().getParent();
    if (parent != null) {
      parent.getWorldTransform().applyInverse(_calcVec3C);
      parent.getWorldTransform().applyInverse(_calcVec3D);
    }

    return _calcVec3D.subtractLocal(_calcVec3C);
  }

  public InteractArrow getXArrow() { return _xArrow; }

  public InteractArrow getYRing() { return _yArrow; }

  public InteractArrow getZRing() { return _zArrow; }
}
