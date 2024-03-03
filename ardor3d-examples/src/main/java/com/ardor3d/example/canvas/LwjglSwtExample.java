/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.canvas;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.lwjgl.opengl.swt.GLData;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.BasicScene;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.framework.lwjgl3.swt.Lwjgl3SwtCanvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.ardor3d.input.gesture.event.LongPressGestureEvent;
import com.ardor3d.input.gesture.event.RotateGestureEvent;
import com.ardor3d.input.gesture.event.SwipeGestureEvent;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.ardor3d.input.logical.GestureEventCondition;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonClickedCondition;
import com.ardor3d.input.logical.MouseButtonLongPressedCondition;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.GrabbedState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseCursor;
import com.ardor3d.input.swt.SwtFocusWrapper;
import com.ardor3d.input.swt.SwtGestureWrapper;
import com.ardor3d.input.swt.SwtKeyboardWrapper;
import com.ardor3d.input.swt.SwtMouseManager;
import com.ardor3d.input.swt.SwtMouseWrapper;
import com.ardor3d.math.Matrix4;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.lwjgl3.Lwjgl3CanvasCallback;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * This examples demonstrates how to render OpenGL (via LWJGL) on a SWT canvas.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.canvas.LwjglSwtExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/canvas_LwjglSwtExample.jpg", //
    maxHeapMemory = 64)
public class LwjglSwtExample {
  static MouseCursor _cursor1;
  static MouseCursor _cursor2;

  static Map<Canvas, Boolean> _showCursor1 = new HashMap<>();

  private static final Logger logger = Logger.getLogger(LwjglSwtExample.class.toString());
  private static int i = 0;
  private static RotatingCubeGame game;

  public static void main(final String[] args) {
    System.setProperty("ardor3d.useMultipleContexts", "true");
    AWTImageLoader.registerLoader();
    ExampleBase.addDefaultResourceLocators();

    final Timer timer = new Timer();
    final FrameHandler frameWork = new FrameHandler(timer);
    final LogicalLayer logicalLayer = new LogicalLayer();

    final AtomicBoolean exit = new AtomicBoolean(false);
    final BasicScene scene = new BasicScene();
    game = new RotatingCubeGame(scene, exit, logicalLayer, Key.T);

    frameWork.addUpdater(game);

    // INIT SWT STUFF
    final Display display = new Display();
    final Shell shell = new Shell(display);
    shell.setText("Lwjgl SWT Example");
    shell.setLayout(new FillLayout());

    // This is our tab folder, it will be accepting our 3d canvases
    final TabFolder tabFolder = new TabFolder(shell, SWT.BORDER);

    // Add a menu item that will create and add a new canvas.
    final Menu bar = new Menu(shell, SWT.BAR);
    shell.setMenuBar(bar);

    final MenuItem fileItem = new MenuItem(bar, SWT.CASCADE);
    fileItem.setText("&Tasks");

    final Menu submenu = new Menu(shell, SWT.DROP_DOWN);
    fileItem.setMenu(submenu);
    final MenuItem item = new MenuItem(submenu, SWT.PUSH);
    item.addListener(SWT.Selection,
        e -> Display.getDefault().asyncExec(() -> addNewCanvas(tabFolder, scene, frameWork, logicalLayer)));
    item.setText("Add &3d Canvas");
    item.setAccelerator(SWT.MOD1 + '3');

    addNewCanvas(tabFolder, scene, frameWork, logicalLayer);

    shell.open();

    game.init();
    // frameWork.init();

    while (!shell.isDisposed() && !exit.get()) {
      display.readAndDispatch();
      frameWork.updateFrame();
      Thread.yield();

      // using the below way makes things really jerky. Not sure how to handle that.

      // if (display.readAndDispatch()) {
      // frameWork.updateFrame();
      // }
      // else {
      // display.sleep();
      // }
    }

    display.dispose();
    System.exit(0);
  }

  private static void addNewCanvas(final TabFolder tabFolder, final BasicScene scene, final FrameHandler frameWork,
      final LogicalLayer logicalLayer) {
    i++;
    logger.info("Adding canvas");

    // Add a new tab to hold our canvas
    final TabItem item = new TabItem(tabFolder, SWT.NONE);
    item.setText("Canvas #" + i);
    tabFolder.setSelection(item);
    final Composite canvasParent = new Composite(tabFolder, SWT.NONE);
    canvasParent.setLayout(new FillLayout());
    item.setControl(canvasParent);

    final GLData data = new GLData();
    data.depthSize = 8;
    data.doubleBuffer = true;
    data.profile = GLData.Profile.CORE;
    data.majorVersion = 3;
    data.minorVersion = 3;
    data.samples = 4; // 4x multisampling
    data.swapInterval = 1; // for enabling v-sync (swapbuffers sync'ed to monitor refresh)

    final SashForm splitter = new SashForm(canvasParent, SWT.HORIZONTAL);

    final SashForm splitterLeft = new SashForm(splitter, SWT.VERTICAL);
    final Composite topLeft = new Composite(splitterLeft, SWT.NONE);
    topLeft.setLayout(new FillLayout());
    final Composite bottomLeft = new Composite(splitterLeft, SWT.NONE);
    bottomLeft.setLayout(new FillLayout());

    final SashForm splitterRight = new SashForm(splitter, SWT.VERTICAL);
    final Composite topRight = new Composite(splitterRight, SWT.NONE);
    topRight.setLayout(new FillLayout());
    final Composite bottomRight = new Composite(splitterRight, SWT.NONE);
    bottomRight.setLayout(new FillLayout());

    canvasParent.layout();

    final Lwjgl3SwtCanvas canvas1 = new Lwjgl3SwtCanvas(topLeft, SWT.NONE, data);
    final Lwjgl3CanvasRenderer lwjglCanvasRenderer1 = new Lwjgl3CanvasRenderer(scene);
    lwjglCanvasRenderer1.getRenderContext().getSceneIndexer().addSceneRoot(scene.getRoot());
    addCallback(canvas1, lwjglCanvasRenderer1);
    canvas1.setCanvasRenderer(lwjglCanvasRenderer1);
    frameWork.addCanvas(canvas1);
    addResizeHandler(canvas1, lwjglCanvasRenderer1);
    canvas1.setFocus();

    final Lwjgl3SwtCanvas canvas2 = new Lwjgl3SwtCanvas(bottomLeft, SWT.NONE, data);
    final Lwjgl3CanvasRenderer lwjglCanvasRenderer2 = new Lwjgl3CanvasRenderer(scene);
    lwjglCanvasRenderer2.getRenderContext().getSceneIndexer().addSceneRoot(scene.getRoot());
    addCallback(canvas2, lwjglCanvasRenderer2);
    canvas2.setCanvasRenderer(lwjglCanvasRenderer2);
    frameWork.addCanvas(canvas2);
    addResizeHandler(canvas2, lwjglCanvasRenderer2);

    final Lwjgl3SwtCanvas canvas3 = new Lwjgl3SwtCanvas(topRight, SWT.NONE, data);
    final Lwjgl3CanvasRenderer lwjglCanvasRenderer3 = new Lwjgl3CanvasRenderer(scene);
    lwjglCanvasRenderer3.getRenderContext().getSceneIndexer().addSceneRoot(scene.getRoot());
    addCallback(canvas3, lwjglCanvasRenderer3);
    canvas3.setCanvasRenderer(lwjglCanvasRenderer3);
    frameWork.addCanvas(canvas3);
    addResizeHandler(canvas3, lwjglCanvasRenderer3);

    final Lwjgl3SwtCanvas canvas4 = new Lwjgl3SwtCanvas(bottomRight, SWT.NONE, data);
    final Lwjgl3CanvasRenderer lwjglCanvasRenderer4 = new Lwjgl3CanvasRenderer(scene);
    lwjglCanvasRenderer4.getRenderContext().getSceneIndexer().addSceneRoot(scene.getRoot());
    addCallback(canvas4, lwjglCanvasRenderer4);
    canvas4.setCanvasRenderer(lwjglCanvasRenderer4);
    frameWork.addCanvas(canvas4);
    addResizeHandler(canvas4, lwjglCanvasRenderer4);

    final SwtKeyboardWrapper keyboardWrapper = new SwtKeyboardWrapper(canvas1);
    final SwtMouseWrapper mouseWrapper = new SwtMouseWrapper(canvas1);
    final SwtFocusWrapper focusWrapper = new SwtFocusWrapper(canvas1);
    final SwtMouseManager mouseManager = new SwtMouseManager(canvas1);
    canvas1.setMouseManager(mouseManager);
    final SwtGestureWrapper gestureWrapper = new SwtGestureWrapper(canvas1, mouseWrapper, true);

    final PhysicalLayer pl = new PhysicalLayer.Builder() //
        .with((KeyboardWrapper) keyboardWrapper) //
        .with((CharacterInputWrapper) keyboardWrapper) //
        .with(mouseWrapper) //
        .with(gestureWrapper) //
        .with(focusWrapper)//
        .build();

    logicalLayer.registerInput(canvas1, pl);

    final Predicate<TwoInputStates> clickLeftOrRight =
        new MouseButtonClickedCondition(MouseButton.LEFT).or(new MouseButtonClickedCondition(MouseButton.RIGHT));

    logicalLayer.registerTrigger(new InputTrigger(clickLeftOrRight, (source, inputStates, tpf) -> System.err
        .println("clicked: " + inputStates.getCurrent().getMouseState().getClickCounts())));

    logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H), (source, inputStates, tpf) -> {
      if (source != canvas1) {
        return;
      }

      if (_showCursor1.get(canvas1)) {
        mouseManager.setCursor(_cursor1);
      } else {
        mouseManager.setCursor(_cursor2);
      }

      _showCursor1.put(canvas1, !_showCursor1.get(canvas1));
    }));
    logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), (source, inputStates, tpf) -> {
      if (source != canvas1) {
        return;
      }

      mouseManager.setCursor(MouseCursor.SYSTEM_DEFAULT);
    }));
    logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.G), (source, inputStates, tpf) -> {
      if (source != canvas1) {
        return;
      }

      mouseManager.setGrabbed(
          mouseManager.getGrabbed() == GrabbedState.NOT_GRABBED ? GrabbedState.GRABBED : GrabbedState.NOT_GRABBED);
    }));

    final Matrix4 matrix = new Matrix4(Matrix4.IDENTITY);
    final Matrix4 rotate = new Matrix4(Matrix4.IDENTITY);
    final Matrix4 pivot = new Matrix4(Matrix4.IDENTITY).applyTranslationPost(-0.5, -0.5, 0);
    final Matrix4 pivotInv = new Matrix4(Matrix4.IDENTITY).applyTranslationPost(0.5, 0.5, 0);
    logicalLayer.registerTrigger(
        new InputTrigger(new GestureEventCondition(RotateGestureEvent.class), (source, inputStates, tpf) -> {
          final RotateGestureEvent event = inputStates.getCurrent().getGestureState().first(RotateGestureEvent.class);
          rotate.applyRotationZ(-event.getDeltaRadians());
          pivotInv.multiply(rotate, null).multiply(pivot, matrix).transposeLocal();
          game.getBox().setProperty(Texture.KEY_TextureMatrix0, matrix);
        }));

    logicalLayer.registerTrigger(
        new InputTrigger(new GestureEventCondition(SwipeGestureEvent.class), (source, inputStates, tpf) -> {
          final SwipeGestureEvent event = inputStates.getCurrent().getGestureState().first(SwipeGestureEvent.class);
          System.err.println(event);
        }));

    logicalLayer.registerTrigger(new InputTrigger( //
        new MouseButtonLongPressedCondition(MouseButton.LEFT, 500, 5)
            .or(new GestureEventCondition(LongPressGestureEvent.class)),
        (source, inputStates, tpf) -> game.toggleRotation()));

    final AWTImageLoader awtImageLoader = new AWTImageLoader();
    try {
      _cursor1 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/wait_cursor.png");
      _cursor2 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/movedata.gif");
    } catch (final IOException ioe) {
      ioe.printStackTrace();
    }

    _showCursor1.put(canvas1, true);
  }

  private static void addCallback(final Lwjgl3SwtCanvas canvas, final Lwjgl3CanvasRenderer renderer) {
    renderer.setCanvasCallback(new Lwjgl3CanvasCallback() {
      @Override
      public void makeCurrent(final boolean force) {
        canvas.setCurrent();
      }

      @Override
      public void releaseContext(final boolean force) {

      }

      @Override
      public void doSwap() {
        canvas.swapBuffers();
      }
    });
  }

  private static MouseCursor createMouseCursor(final AWTImageLoader awtImageLoader, final String resourceName)
      throws IOException {
    final com.ardor3d.image.Image image = awtImageLoader
        .load(ResourceLocatorTool.getClassPathResourceAsStream(LwjglSwtExample.class, resourceName), false);

    return new MouseCursor("cursor1", image, 0, image.getHeight() - 1);
  }

  static void addResizeHandler(final Lwjgl3SwtCanvas swtCanvas, final CanvasRenderer canvasRenderer) {
    swtCanvas.addListener((final int w, final int h) -> {
      if ((w == 0) || (h == 0)) {
        return;
      }

      final float aspect = (float) w / (float) h;
      final Camera camera = canvasRenderer.getCamera();
      if (camera != null) {
        final double fovY = camera.getFovY();
        final double near = camera.getFrustumNear();
        final double far = camera.getFrustumFar();
        camera.setFrustumPerspective(fovY, aspect, near, far);
        camera.resize(w, h);
      }
    });
  }
}

// class LwjglSwtModule extends AbstractModule {
// public LwjglSwtModule() {}
//
// @Override
// protected void configure() {
// // enforce a single instance of SwtKeyboardWrapper will handle both the KeyListener and the
// KeyboardWrapper
// // interfaces
// bind(SwtKeyboardWrapper.class).in(Scopes.SINGLETON);
// bind(KeyboardWrapper.class).to(SwtKeyboardWrapper.class);
// bind(KeyListener.class).to(SwtKeyboardWrapper.class);
//
// bind(MouseWrapper.class).to(SwtMouseWrapper.class);
// }
// }
