/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.ui;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.data.SpatialState;
import com.ardor3d.extension.interact.filter.AbstractDragDelayFilter;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.extension.interact.widget.MovePlanarWidget;
import com.ardor3d.extension.interact.widget.MovePlanarWidget.MovePlane;
import com.ardor3d.extension.ui.Orientation;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UIComboBox;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIPieMenu;
import com.ardor3d.extension.ui.UIPieMenuItem;
import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.extension.ui.backdrop.EmptyBackdrop;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.model.DefaultComboBoxModel;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.extension.ui.util.UIArc;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.input.InputState;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.KeyReleasedCondition;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.GrabbedState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.Pickable;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Tube;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * An example illustrating the use of the interact framework.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.interact.InteractUIExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/interact_InteractUIExample.jpg", //
    maxHeapMemory = 64)
public class InteractUIExample extends ExampleBase {

  private UIHud hud;
  private InteractManager manager;
  private MovePlanarWidget moveWidget;
  private InsertMarkerUIWidget insertWidget;
  private ColorSelectUIWidget colorWidget;
  private PulseControlUIWidget pulseWidget;

  final Vector3 tempVec = new Vector3();

  public static void main(final String[] args) {
    start(InteractUIExample.class);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    manager.update(timer);
    hud.updateGeometricState(timer.getTimePerFrame());
  }

  @Override
  protected void renderExample(final Renderer renderer) {
    super.renderExample(renderer);
    manager.render(renderer);
    renderer.renderBuckets();
    hud.draw(renderer);
  }

  @Override
  protected void updateLogicalLayer(final ReadOnlyTimer timer) {
    hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Interact Example");
    hud = new UIHud(_canvas);

    final Camera camera = _canvas.getCanvasRenderer().getCamera();
    camera.setLocation(15, 11, -9);
    camera.lookAt(0, 0, 0, Vector3.UNIT_Y);

    // setup our interact controls
    addControls();

    // create a floor to act as a reference.
    addFloor();

    // create a few way-markers to start things off
    initPath();

  }

  private void addFloor() {
    final Box floor = new Box("floor", Vector3.ZERO, 100, 5, 100);
    floor.setTranslation(0, -5, 0);
    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("models/obj/pitcher.jpg", Texture.MinificationFilter.Trilinear, true));
    floor.setRenderState(ts);
    floor.getSceneHints().setPickingHint(PickingHint.Pickable, false);
    floor.setModelBound(new BoundingBox());
    floor.setRenderMaterial("lit/textured/basic_phong.yaml");
    _root.attachChild(floor);
    _root.updateGeometricState(0);
  }

  LinkedList<Spatial> path = new LinkedList<>();

  private void initPath() {
    final Spatial marker1 = createMarker();
    marker1.setName("marker1");
    final Spatial marker2 = createMarkerAfter(marker1);
    marker2.setName("marker2");
    createMarkerBefore(marker2).setName("marker3");

    // auto select the joint
    _root.updateGeometricState(0);
    manager.setSpatialTarget(marker1);
  }

  private void removeMarker(final Spatial ref) {
    final int index = path.indexOf(ref);
    if (path.remove(ref)) {
      ref.removeFromParent();
      manager.setSpatialTarget(null);
      if (path.size() == 0) {
        manager.setSpatialTarget(null);
      } else if (path.size() <= index) {
        manager.setSpatialTarget(path.get(index - 1));
      } else {
        manager.setSpatialTarget(path.get(index));
      }
    }
  }

  private Spatial createMarker() {
    final Tube t = new Tube("marker", 1, 0.25, .25);
    t.setModelBound(new BoundingBox());
    t.updateGeometricState(0);
    t.addTranslation(0, .25, 0);
    t.getSceneHints().setPickingHint(PickingHint.Pickable, true);
    final MarkerData data = new MarkerData();
    t.setProperty("markerData", data);
    t.addController(new SpatialController<>() {
      private double _scaleTime = 0;

      @Override
      public void update(final double time, final Spatial caller) {
        // update our rotation
        final MarkerData data = t.getProperty("markerData", null);
        final double pulseSpeed = data.pulseSpeed;
        if (pulseSpeed != 0.0) {
          _scaleTime = _scaleTime + (_timer.getTimePerFrame() * pulseSpeed);
          final double scale = Math.sin(_scaleTime) * .99 + 1.0;
          t.setScale(scale);
        } else {
          t.setScale(1.0);
        }
      }
    });
    t.setRenderMaterial("lit/untextured/basic_phong.yaml");
    _root.attachChild(t);
    path.add(t);
    return t;
  }

  private Spatial createMarkerAfter(final Spatial ref) {
    final Spatial marker = createMarker();

    // copy transform (orientation and position) of ref
    marker.setTranslation(ref.getTranslation());
    marker.setRotation(ref.getRotation());

    // check if we're moving into place between two points, or just after last point
    final int indexOfRef = path.indexOf(ref);
    if (indexOfRef == path.size() - 2) {
      // we're adding after last point, so no need to move in list
      // just translate us in the forward z direction of the last node
      final Vector3 fwd = marker.getRotation().applyPost(Vector3.UNIT_Z, null);
      marker.addTranslation(fwd.multiplyLocal(8.0));
    } else {
      // we're adding a point between two others - get our other ref
      final Spatial postRef = path.get(indexOfRef + 1);

      // move new marker into list between the other two points
      path.remove(marker);
      path.add(indexOfRef + 1, marker);

      // translate and orient between points
      marker.setTranslation(ref.getTranslation().add(postRef.getTranslation(), null).divideLocal(2.0));

      final Quaternion rotHelper1 = new Quaternion();
      rotHelper1.fromRotationMatrix(ref.getRotation());
      final Quaternion rotHelper2 = new Quaternion();
      rotHelper2.fromRotationMatrix(postRef.getRotation());
      marker.setRotation(rotHelper1.slerp(rotHelper2, .5, null));
    }

    manager.setSpatialTarget(marker);

    return marker;
  }

  private Spatial createMarkerBefore(final Spatial ref) {
    final int indexOfRef = path.indexOf(ref);
    if (indexOfRef <= 0) {
      return null;
    }

    return createMarkerAfter(path.get(indexOfRef - 1));
  }

  private void addControls() {
    // create our manager
    manager = new InteractManager(new MarkerState());
    manager.setupInput(_canvas, _physicalLayer, _logicalLayer);

    hud.setupInput(_physicalLayer, manager.getLogicalLayer());
    hud.setMouseManager(_mouseManager);

    // add some widgets.
    insertWidget = new InsertMarkerUIWidget();
    manager.addWidget(insertWidget);

    colorWidget = new ColorSelectUIWidget();
    manager.addWidget(colorWidget);

    pulseWidget = new PulseControlUIWidget();
    manager.addWidget(pulseWidget);

    moveWidget = new MovePlanarWidget().withPlane(MovePlane.XZ).withDefaultHandle(.33, .33, ColorRGBA.YELLOW);
    manager.addWidget(moveWidget);

    // set the default as current
    manager.setActiveWidget(moveWidget);

    // add triggers to change which widget is active
    manager.getLogicalLayer().registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.ONE),
        (source, inputStates, tpf) -> manager.setActiveWidget(moveWidget)));
    manager.getLogicalLayer().registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.TWO),
        (source, inputStates, tpf) -> manager.setActiveWidget(insertWidget)));
    manager.getLogicalLayer().registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.THREE),
        (source, inputStates, tpf) -> manager.setActiveWidget(colorWidget)));
    manager.getLogicalLayer().registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.FOUR),
        (source, inputStates, tpf) -> manager.setActiveWidget(pulseWidget)));

    _logicalLayer.registerTrigger(
        new InputTrigger(new MouseButtonPressedCondition(MouseButton.MIDDLE), (source, inputStates, tpf) -> {
          _mouseManager.setGrabbed(GrabbedState.NOT_GRABBED);
          showMenu(inputStates.getCurrent());
        }));

    manager.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE),
        (source, inputStates, tpf) -> showMenu(inputStates.getCurrent())));
    manager.getLogicalLayer().registerTrigger(
        new InputTrigger(new KeyReleasedCondition(Key.SPACE), (source, inputStates, tpf) -> hideMenu()));

    moveWidget.addFilter(new AbstractDragDelayFilter(.5f, .25f) {
      @Override
      protected void showTimerViz(final InteractManager manager, final AbstractInteractWidget widget,
          final MouseState current) {
        if (timerArc == null) {
          timerArc = new UIArc("timer", MathUtils.TWO_PI / 60, 1, 0.5);
          timerArc.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
          timerArc.setRenderMaterial("unlit/untextured/basic.yaml");
        }
        clearTimerViz(manager, widget, current, true);
        timerArc.setDefaultColor(ColorRGBA.LIGHT_GRAY);
        updateTimerViz(manager, widget, current, 0.0f);
        if (timerArc.getParent() == null) {
          hud.attachChild(timerArc);
        }
      }

      @Override
      protected void updateTimerViz(final InteractManager manager, final AbstractInteractWidget widget,
          final MouseState current, final float percent) {
        timerArc.resetGeometry(0, MathUtils.TWO_PI * percent, 30, 15, null, true);
        timerArc.setTranslation(current.getX(), current.getY(), 0);
        timerArc.updateGeometricState(0);
      }

      @Override
      protected void clearTimerViz(final InteractManager manager, final AbstractInteractWidget widget,
          final MouseState current, final boolean immediate) {
        if (timerArc.getParent() == null) {
          return;
        }

        if (immediate) {
          timerArc.removeFromParent();
          return;
        }

        clearTime = 0f;
        timerArc.addController((dt, caller) -> {
          clearTime += dt;
          timerArc.setDefaultColor(ColorRGBA.WHITE);
          if (clearTime > 0.25f) {
            timerArc.clearControllers();
            timerArc.removeFromParent();
          }
        });
      }
    });
  }

  UIArc timerArc;
  float clearTime;

  UIPieMenu menu;

  protected void showMenu(final InputState state) {
    if (menu == null) {
      menu = new UIPieMenu(hud, 70, 200);
      menu.setTotalArcLength(MathUtils.PI);
      menu.setStartAngle(-90 * MathUtils.DEG_TO_RAD);
      // menu.setRotation(new Matrix3().fromAngleAxis(MathUtils.DEG_TO_RAD * 60, Vector3.UNIT_Z));

      final UIPieMenu addPie = new UIPieMenu(hud);
      menu.addItem(new UIPieMenuItem("Add Node...", null, addPie, 100));
      addPie.addItem(new UIPieMenuItem("Before", null, true, event -> {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          return;
        }
        createMarkerBefore(spat);
      }));
      addPie.addItem(new UIPieMenuItem("After", null, true, event -> {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          return;
        }
        createMarkerAfter(spat);
      }));

      final UIPieMenu remPie = new UIPieMenu(hud);
      remPie.addItem(new UIPieMenuItem("Node", null, true, event -> {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          return;
        }
        removeMarker(spat);
      }));

      menu.addItem(new UIPieMenuItem("Delete...", null, remPie, 100));

      menu.setCenterItem(new UIPieMenuItem("Cancel", null, true, event -> {}));

      menu.addItem(new UIPieMenuItem("Reset Node", null, true, event -> {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          return;
        }
      }));
      menu.updateMinimumSizeFromContents();
      menu.layout();
    }

    hud.closePopupMenus();

    final Spatial spat = manager.getSpatialTarget();
    if (spat == null) {
      return;
    }

    hud.showSubPopupMenu(menu);

    tempVec.zero();
    tempVec.set(Camera.getCurrentCamera().getScreenCoordinates(spat.getWorldTransform().applyForward(tempVec)));
    tempVec.setZ(0);
    menu.showAt((int) tempVec.getX(), (int) tempVec.getY());
    _mouseManager.setGrabbed(GrabbedState.NOT_GRABBED);
    _mouseManager.setPosition((int) tempVec.getX(), (int) tempVec.getY());
    if (menu.getCenterItem() != null) {
      menu.getCenterItem().mouseEntered((int) tempVec.getX(), (int) tempVec.getY(), state);
    }
  }

  protected void hideMenu() {
    hud.closePopupMenus();
  }

  @Override
  protected void processPicks(final PrimitivePickResults pickResults) {
    final PickData pick = pickResults.findFirstIntersectingPickData();
    if (pick != null) {
      final Pickable target = pick.getTarget();
      if (target instanceof Spatial) {
        manager.setSpatialTarget((Spatial) target);
        System.out.println("Setting target to: " + ((Spatial) target).getName());
        return;
      }
    }
    manager.setSpatialTarget(null);
  }

  class InsertMarkerUIWidget extends AbstractInteractWidget {

    UIPanel uiPanel;

    public InsertMarkerUIWidget() {
      createFrame();
    }

    private void createFrame() {

      final RowLayout rowLay = new RowLayout(true);
      final UIPanel centerPanel = new UIPanel(rowLay);
      centerPanel.setBackdrop(new EmptyBackdrop());
      centerPanel.setLayoutData(BorderLayoutData.CENTER);

      AddButton(centerPanel, "+", event -> {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          return;
        }
        createMarkerBefore(spat);
      });

      AddButton(centerPanel, "-", event -> {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          return;
        }
        removeMarker(spat);
      });

      AddButton(centerPanel, "+", event -> {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          return;
        }
        createMarkerAfter(spat);
      });

      uiPanel = new UIPanel();
      uiPanel.add(centerPanel);
      uiPanel.pack();

      _handle = uiPanel;
    }

    private void AddButton(final UIContainer parent, final String label, final ActionListener actionListener) {
      final UIButton button = new UIButton(label);
      button.setPadding(Insets.EMPTY);
      button.setMargin(Insets.EMPTY);
      button.setMaximumContentSize(22, 22);
      button.setMinimumContentSize(22, 22);
      button.addActionListener(actionListener);
      parent.add(button);
    }

    @Override
    public void render(final Renderer renderer, final InteractManager manager) {
      final Spatial spat = manager.getSpatialTarget();
      if (spat == null) {
        return;
      }

      tempVec.zero();
      tempVec.set(Camera.getCurrentCamera().getScreenCoordinates(spat.getWorldTransform().applyForward(tempVec)));
      tempVec.setZ(0);
      tempVec.subtractLocal(uiPanel.getContentWidth() / 2, -10, 0);
      _handle.setTranslation(tempVec);
      _handle.updateWorldTransform(true);
    }

    @Override
    public void receivedControl(final InteractManager manager) {
      super.receivedControl(manager);
      final Spatial spat = manager.getSpatialTarget();
      if (spat != null) {
        hud.add(uiPanel);
      }
    }

    @Override
    public void lostControl(final InteractManager manager) {
      super.lostControl(manager);
      hud.remove(uiPanel);
    }

    @Override
    public void targetChanged(final InteractManager manager) {
      super.targetChanged(manager);
      if (manager.getActiveWidget() == this) {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          hud.remove(uiPanel);
        } else {
          hud.add(uiPanel);
        }
      }
    }
  }

  class ColorSelectUIWidget extends AbstractInteractWidget {

    UIPanel uiPanel;
    ColorRGBA unconsumedColor;

    public ColorSelectUIWidget() {
      createFrame();
    }

    private void createFrame() {

      final UIPanel centerPanel = new UIPanel();
      centerPanel.setBackdrop(new EmptyBackdrop());
      centerPanel.setLayoutData(BorderLayoutData.CENTER);

      final UIComboBox combo = new UIComboBox(
          new DefaultComboBoxModel("White", "Black", "Red", "Green", "Blue", "Yellow", "Magenta", "Cyan"));
      combo.setMinimumContentWidth(100);
      combo.addSelectionListener((component, newValue) -> {
        try {
          final Field field = ColorRGBA.class.getField(newValue.toString().toUpperCase());
          final ColorRGBA color = (ColorRGBA) field.get(null);
          if (manager.getSpatialState() instanceof MarkerState) {
            unconsumedColor = color;
          }

        } catch (final Exception ex) {
          ex.printStackTrace();
        }
      });
      centerPanel.add(combo);

      uiPanel = new UIPanel();
      uiPanel.add(centerPanel);
      uiPanel.pack();

      _handle = uiPanel;
    }

    @Override
    public void render(final Renderer renderer, final InteractManager manager) {
      final Spatial spat = manager.getSpatialTarget();
      if (spat == null) {
        return;
      }

      tempVec.zero();
      tempVec.set(Camera.getCurrentCamera().getScreenCoordinates(spat.getWorldTransform().applyForward(tempVec)));
      tempVec.setZ(0);
      tempVec.subtractLocal(uiPanel.getContentWidth() / 2, -20, 0);
      _handle.setTranslation(tempVec);
      _handle.updateWorldTransform(true);
    }

    @Override
    public void receivedControl(final InteractManager manager) {
      super.receivedControl(manager);
      final Spatial spat = manager.getSpatialTarget();
      if (spat != null) {
        hud.add(uiPanel);
      }
    }

    @Override
    public void lostControl(final InteractManager manager) {
      super.lostControl(manager);
      hud.remove(uiPanel);
    }

    @Override
    public void processInput(final Canvas source, final TwoInputStates inputStates, final AtomicBoolean inputConsumed,
        final InteractManager manager) {
      super.processInput(source, inputStates, inputConsumed, manager);
      if (unconsumedColor != null) {
        ((MarkerState) manager.getSpatialState()).data.color.set(unconsumedColor);
        inputConsumed.set(true);
        unconsumedColor = null;
      }
    }

    @Override
    public void targetChanged(final InteractManager manager) {
      super.targetChanged(manager);
      if (manager.getActiveWidget() == this) {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          hud.remove(uiPanel);
        } else {
          hud.add(uiPanel);
        }
      }
    }
  }

  class PulseControlUIWidget extends AbstractInteractWidget {

    UIPanel uiPanel;
    Double unconsumedPulse;

    public PulseControlUIWidget() {
      createFrame();
    }

    private void createFrame() {

      final UIPanel centerPanel = new UIPanel();
      centerPanel.setBackdrop(new EmptyBackdrop());
      centerPanel.setLayoutData(BorderLayoutData.CENTER);

      final UISlider slider = new UISlider(Orientation.Horizontal, 0, 100, 0);
      slider.addActionListener(event -> {
        if (manager.getSpatialTarget() == null) {
          return;
        }
        if (manager.getSpatialState() instanceof MarkerState) {
          unconsumedPulse = slider.getValue() * 0.05;
        }
      });
      slider.setMinimumContentWidth(100);
      centerPanel.add(slider);

      uiPanel = new UIPanel();
      uiPanel.add(centerPanel);
      uiPanel.pack();

      _handle = uiPanel;
    }

    @Override
    public void render(final Renderer renderer, final InteractManager manager) {
      final Spatial spat = manager.getSpatialTarget();
      if (spat == null) {
        return;
      }

      tempVec.zero();
      tempVec.set(Camera.getCurrentCamera().getScreenCoordinates(spat.getWorldTransform().applyForward(tempVec)));
      tempVec.setZ(0);
      tempVec.subtractLocal(uiPanel.getContentWidth() / 2, -20, 0);
      _handle.setTranslation(tempVec);
      _handle.updateWorldTransform(true);
    }

    @Override
    public void receivedControl(final InteractManager manager) {
      super.receivedControl(manager);
      final Spatial spat = manager.getSpatialTarget();
      if (spat != null) {
        hud.add(uiPanel);
      }
    }

    @Override
    public void lostControl(final InteractManager manager) {
      super.lostControl(manager);
      hud.remove(uiPanel);
    }

    @Override
    public void processInput(final Canvas source, final TwoInputStates inputStates, final AtomicBoolean inputConsumed,
        final InteractManager manager) {
      super.processInput(source, inputStates, inputConsumed, manager);
      if (unconsumedPulse != null) {
        ((MarkerState) manager.getSpatialState()).data.pulseSpeed = unconsumedPulse;
        inputConsumed.set(true);
        unconsumedPulse = null;
      }
    }

    @Override
    public void targetChanged(final InteractManager manager) {
      super.targetChanged(manager);
      if (manager.getActiveWidget() == this) {
        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
          hud.remove(uiPanel);
        } else {
          hud.add(uiPanel);
        }
      }
    }
  }

  class MarkerData {
    public ColorRGBA color = new ColorRGBA(ColorRGBA.WHITE);
    public double pulseSpeed;

    public void copy(final MarkerData source) {
      color.set(source.color);
      pulseSpeed = source.pulseSpeed;
    }
  }

  class MarkerState extends SpatialState {
    public final MarkerData data = new MarkerData();

    @Override
    public void applyState(final Spatial target) {
      super.applyState(target);
      if (target.hasLocalProperty("markerData")) {
        final MarkerData tData = target.getLocalProperty("markerData", null);
        if (tData.pulseSpeed != data.pulseSpeed) {
          tData.pulseSpeed = data.pulseSpeed;
        }

        if (!tData.color.equals(data.color)) {
          tData.color.set(data.color);
          target.acceptVisitor((final Spatial spatial) -> {
            if (spatial instanceof Mesh mesh) {
              mesh.setDefaultColor(tData.color);
              mesh.setSolidColor(tData.color);
            }
          }, true);
        }
      }
    }

    @Override
    public void copyState(final Spatial source) {
      super.copyState(source);
      if (source.hasLocalProperty("markerData")) {
        final MarkerData tData = source.getLocalProperty("markerData", null);
        data.copy(tData);
      }
    }
  }
}
