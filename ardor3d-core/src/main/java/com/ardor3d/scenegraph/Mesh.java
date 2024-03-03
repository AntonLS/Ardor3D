/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.bounding.CollisionTree;
import com.ardor3d.bounding.CollisionTreeManager;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.FloatBufferData;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.intersection.Pickable;
import com.ardor3d.intersection.PrimitiveKey;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.RenderMatrixType;
import com.ardor3d.renderer.RenderPhase;
import com.ardor3d.renderer.Renderable;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.renderer.material.MaterialTechnique;
import com.ardor3d.renderer.material.TechniquePass;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.Constants;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * A Mesh is a spatial describing a renderable geometric object. Data about the mesh is stored
 * locally using MeshData.
 */
public class Mesh extends Spatial implements Renderable, Pickable {

  /** Actual buffer representation of the mesh */
  protected MeshData _meshData = new MeshData();

  /** Local model bounding volume */
  protected BoundingVolume _modelBound = new BoundingSphere(Double.POSITIVE_INFINITY, Vector3.ZERO);

  /**
   * The compiled list of renderstates for this mesh, taking into account ancestors states - updated
   * with updateRenderStates()
   */
  protected final EnumMap<RenderState.StateType, RenderState> _states = new EnumMap<>(RenderState.StateType.class);

  /** Visibility setting that can be used after the scenegraph hierarchical culling */
  protected boolean _isVisible = true;

  /** Number of instances drawn for this Mesh. */
  protected int _instanceCount = 1;

  /**
   * Constructs a new Mesh.
   */
  public Mesh() {
    super();
  }

  /**
   * Constructs a new <code>Mesh</code> with a given name.
   *
   * @param name
   *          the name of the mesh. This is required for identification purposes.
   */
  public Mesh(final String name) {
    super(name);
  }

  /**
   * Retrieves the mesh data object used by this mesh.
   *
   * @return the mesh data object
   */
  public MeshData getMeshData() { return _meshData; }

  /**
   * Sets the mesh data object for this mesh.
   *
   * @param meshData
   *          the mesh data object
   */
  public void setMeshData(final MeshData meshData) {
    // invalidate collision tree cache
    CollisionTreeManager.INSTANCE.removeCollisionTree(this);
    _meshData = meshData;
  }

  /**
   * Retrieves the local bounding volume for this mesh.
   *
   * @param store
   *          the bounding volume
   */
  public BoundingVolume getModelBound() { return _modelBound; }

  /**
   * Retrieves a copy of the local bounding volume for this mesh.
   *
   * @param store
   *          the bounding volume
   */
  public BoundingVolume getModelBound(final BoundingVolume store) {
    if (_modelBound == null) {
      return null;
    }
    return _modelBound.clone(store);
  }

  /**
   * Sets the local bounding volume for this mesh to the given bounds, updated to fit the vertices of
   * this Mesh. Marks the spatial as having dirty world bounds.
   *
   * @param modelBound
   *          the bounding volume - only type is used, actual values are replaced.
   */
  public void setModelBound(final BoundingVolume modelBound) {
    setModelBound(modelBound, true);
  }

  /**
   * Sets the local bounding volume for this mesh to the given bounding volume. If autoCompute is true
   * (default, if not given) then we will modify the given modelBound to fit the current vertices of
   * this mesh. This will also mark the spatial as having dirty world bounds.
   *
   * @param modelBound
   *          the bounding volume
   * @param autoCompute
   *          if true, update the given modelBound to fit the vertices of this Mesh.
   */
  public void setModelBound(final BoundingVolume modelBound, final boolean autoCompute) {
    _modelBound = modelBound != null ? modelBound.clone(_modelBound) : null;
    if (autoCompute) {
      updateModelBound();
    }
    markDirty(DirtyType.Bounding);
  }

  /**
   * Recalculate the local bounding volume of this Mesh to fit its vertices.
   */
  public void updateModelBound() {
    if (_modelBound != null && _meshData.getVertexBuffer() != null) {
      // using duplicate to allow walking through buffer without altering current position, etc.
      // NB: this maintains a measure of thread safety when using shared meshdata.
      _modelBound.computeFromPoints(_meshData.getVertexBuffer().duplicate());
      markDirty(DirtyType.Bounding);
    }
  }

  @Override
  public void updateWorldBound(final boolean recurse) {
    if (_modelBound != null) {
      _worldBound = _modelBound.transform(_worldTransform, _worldBound);
    } else {
      _worldBound = null;
    }
    clearDirty(DirtyType.Bounding);
  }

  /**
   *
   * @return number of instances drawn for this Mesh. By default, this is 1.
   */
  public int getInstanceCount() { return _instanceCount; }

  /**
   * Set how many instances of this Mesh to draw during render. This is accomplished via Instanced
   * rendering, and often requires setting up vertex attributes appropriately in order to give each
   * instance a unique transform, color, look, etc.
   *
   * @param instanceCount
   *          number of instances to render. Default value is 1.
   */
  public void setInstanceCount(final int instanceCount) { _instanceCount = instanceCount; }

  /**
   * translates/rotates and scales the vectors of this Mesh to world coordinates based on its world
   * settings. The results are stored in the given FloatBuffer. If given FloatBuffer is null, one is
   * created.
   *
   * @param store
   *          the FloatBuffer to store the results in, or null if you want one created.
   * @return store or new FloatBuffer if store == null.
   */
  public FloatBuffer getWorldVectors(FloatBuffer store) {
    final FloatBuffer vertBuf = _meshData.getVertexBuffer();
    if (store == null || store.capacity() != vertBuf.limit()) {
      store = BufferUtils.createFloatBuffer(vertBuf.limit());
    }

    final Vector3 compVect = Vector3.fetchTempInstance();
    for (int v = 0, vSize = store.capacity() / 3; v < vSize; v++) {
      BufferUtils.populateFromBuffer(compVect, vertBuf, v);
      _worldTransform.applyForward(compVect);
      BufferUtils.setInBuffer(compVect, store, v);
    }
    Vector3.releaseTempInstance(compVect);
    return store;
  }

  /**
   * rotates the normals of this Mesh to world normals based on its world settings. The results are
   * stored in the given FloatBuffer. If given FloatBuffer is null, one is created.
   *
   * @param store
   *          the FloatBuffer to store the results in, or null if you want one created.
   * @return store or new FloatBuffer if store == null.
   */
  public FloatBuffer getWorldNormals(FloatBuffer store) {
    final FloatBuffer normBuf = _meshData.getNormalBuffer();
    if (store == null || store.capacity() != normBuf.limit()) {
      store = BufferUtils.createFloatBuffer(normBuf.limit());
    }

    final Vector3 compVect = Vector3.fetchTempInstance();
    for (int v = 0, vSize = store.capacity() / 3; v < vSize; v++) {
      BufferUtils.populateFromBuffer(compVect, normBuf, v);
      _worldTransform.applyForwardVector(compVect);
      BufferUtils.setInBuffer(compVect, store, v);
    }
    Vector3.releaseTempInstance(compVect);
    return store;
  }

  @Override
  public boolean render(final Renderer renderer) {
    final Camera camera = Camera.getCurrentCamera();
    if (camera.checkLayerPasses(getLayer()) && isVisible()) {
      return render(renderer, getMeshData());
    }

    return false;
  }

  protected boolean render(final Renderer renderer, final MeshData meshData) {
    final RenderContext context = ContextManager.getCurrentContext();
    final RenderPhase phase = context.getRenderPhase();

    if (phase == RenderPhase.ShadowTexture && !LightProperties.isShadowCaster(this)) {
      return false;
    }

    // Grab our proper RenderTechnique
    final MaterialTechnique technique = MaterialManager.INSTANCE.chooseTechnique(this, phase);

    // No technique? Can't render.
    if (technique == null) {
      return false;
    }

    // Set our model matrix
    renderer.setMatrix(RenderMatrixType.Model, getWorldTransform());
    renderer.computeNormalMatrix(getWorldTransform().isUniformScale());
    if (phase == RenderPhase.Scene && LightProperties.isLightReceiver(this)) {
      SceneIndexer.getCurrent().getLightManager().sortLightsFor(this);
    }

    // Walk through the passes of the technique and draw this mesh for each
    for (final TechniquePass pass : technique.getPasses()) {
      // setup for drawing this pass - shaders, data, states, etc.
      pass.setupForDraw(renderer, this, meshData);

      // Now we draw ourselves
      final IndexMode[] modes = meshData.getIndexModes();
      final int[] indexLengths = meshData.getIndexLengths();
      final IndexBufferData<?> indices = meshData.getIndices();

      if (indexLengths == null) {
        if (indices != null) {
          renderer.drawElements(indices, 0, indices.getBufferLimit(), modes[0], _instanceCount);
        } else {
          renderer.drawArrays(0, meshData.getVertexCount(), modes[0], _instanceCount);
        }
      } else {
        int offset = 0;
        int modeIndex = 0;
        for (int i = 0; i < indexLengths.length; i++) {
          final int count = indexLengths[i];

          if (indices != null) {
            renderer.drawElements(indices, offset, count, modes[modeIndex], _instanceCount);
          } else {
            renderer.drawArrays(offset, count, modes[modeIndex], _instanceCount);
          }

          offset += count;

          if (modeIndex < modes.length - 1) {
            modeIndex++;
          }
        }
      }
    }

    if (Constants.stats) {
      StatCollector.addStat(StatType.STAT_VERTEX_COUNT, meshData.getVertexCount());
      StatCollector.addStat(StatType.STAT_MESH_COUNT, 1);
    }

    // final InstancingManager instancing = glsl != null ? meshData.getInstancingManager() : null;

    // final ContextCapabilities caps = context.getCapabilities();

    // if (instancing == null) {
    // final boolean transformed = renderer.doTransforms(_worldTransform);

    // // Apply shader states here for the ability to retrieve mesh matrices
    // renderer.applyState(StateType.Shader, _states.get(StateType.Shader));

    // renderVBO(renderer, meshData, -1);

    // if (transformed) {
    // renderer.undoTransforms(_worldTransform);
    // }

    // } else {
    // while (instancing.apply(this, renderer, glsl)) {
    // // Apply shader states here for the ability to retrieve mesh matrices
    // renderer.applyState(StateType.Shader, _states.get(StateType.Shader));
    //
    // renderVBO(renderer, meshData, instancing.getPrimitiveCount());
    // }
    // }

    return true;
  }

  @Override
  protected void applyWorldRenderStates(final boolean recurse, final RenderState.StateStack stack) {
    // start with a blank slate
    _states.clear();

    // Go through each state stack and apply to our states list.
    stack.extract(_states, this);
  }

  public boolean isVisible() { return _isVisible; }

  public void setVisible(final boolean isVisible) { _isVisible = isVisible; }

  /**
   *
   */
  @Override
  public void draw(final Renderer r) {
    if (!r.isProcessingQueue()) {
      if (r.checkAndAdd(this)) {
        return;
      }
    }

    r.draw((Renderable) this);
  }

  /**
   * @param type
   *          StateType of RenderState we want to grab
   * @return the compiled RenderState for this Mesh, either from RenderStates applied locally or those
   *         inherited from this Mesh's ancestors. May be null if a state of the given type was never
   *         applied in either place.
   */
  @SuppressWarnings("unchecked")
  public <T extends RenderState> T getWorldRenderState(final StateType type) {
    return (T) _states.get(type);
  }

  /**
   * <code>setSolidColor</code> sets the color array of this geometry to a single color. For greater
   * efficiency, try setting the the ColorBuffer to null and using DefaultColor instead.
   *
   * @param color
   *          the color to set.
   */
  public void setSolidColor(final ReadOnlyColorRGBA color) {
    FloatBuffer colorBuf = _meshData.getColorBuffer();
    if (colorBuf == null) {
      colorBuf = BufferUtils.createColorBuffer(_meshData.getVertexCount());
      _meshData.setColorBuffer(colorBuf);
    }

    colorBuf.rewind();
    for (int x = 0, cLength = colorBuf.remaining(); x < cLength; x += 4) {
      colorBuf.put(color.getRed());
      colorBuf.put(color.getGreen());
      colorBuf.put(color.getBlue());
      colorBuf.put(color.getAlpha());
    }
    colorBuf.flip();
  }

  /**
   * Sets every color of this geometry's color array to a random color.
   */
  public void setRandomColors() {
    FloatBuffer colorBuf = _meshData.getColorBuffer();
    if (colorBuf == null || colorBuf.limit() < _meshData.getVertexCount() * 4) {
      colorBuf = BufferUtils.createColorBuffer(_meshData.getVertexCount());
      _meshData.setColorBuffer(colorBuf);
    }

    colorBuf.rewind();
    for (int x = 0, cLength = colorBuf.limit(); x < cLength; x += 4) {
      colorBuf.put(MathUtils.nextRandomFloat());
      colorBuf.put(MathUtils.nextRandomFloat());
      colorBuf.put(MathUtils.nextRandomFloat());
      colorBuf.put(1);
    }
    colorBuf.flip();
    _meshData.markBufferDirty(MeshData.KEY_ColorCoords);
  }

  // PICKABLE INTERFACE

  @Override
  public boolean supportsBoundsIntersectionRecord() {
    return true;
  }

  @Override
  public boolean supportsPrimitivesIntersectionRecord() {
    return true;
  }

  @Override
  public boolean intersectsWorldBound(final Ray3 ray) {
    // should throw NPE if no bound.
    return getWorldBound().intersects(ray);
  }

  @Override
  public IntersectionRecord intersectsWorldBoundsWhere(final Ray3 ray) {
    // should throw NPE if no bound.
    return getWorldBound().intersectsWhere(ray);
  }

  @Override
  public IntersectionRecord intersectsPrimitivesWhere(final Ray3 ray) {
    final List<PrimitiveKey> primitives = new ArrayList<>();

    // What about Lines and Points?
    final CollisionTree ct = CollisionTreeManager.getInstance().getCollisionTree(this);
    if (ct != null) {
      ct.getBounds().transform(getWorldTransform(), ct.getWorldBounds());
      ct.intersect(ray, primitives);
    }

    if (primitives.isEmpty()) {
      return null;
    }

    Vector3[] vertices = null;
    final double[] distances = new double[primitives.size()];
    for (int i = 0; i < primitives.size(); i++) {
      final PrimitiveKey key = primitives.get(i);
      vertices = getMeshData().getPrimitiveVertices(key.getPrimitiveIndex(), key.getSection(), vertices);
      // convert to world coord space
      final int max = getMeshData().getIndexMode(key.getSection()).getVertexCount();
      for (int j = 0; j < max; j++) {
        if (vertices[j] != null) {
          getWorldTransform().applyForward(vertices[j]);
        }
      }
      final double triDistanceSq = ray.getDistanceToPrimitive(vertices);
      distances[i] = triDistanceSq;
    }

    // FIXME: optimize! ugly bubble sort for now
    boolean sorted = false;
    while (!sorted) {
      sorted = true;
      for (int sort = 0; sort < distances.length - 1; sort++) {
        if (distances[sort] > distances[sort + 1]) {
          // swap
          sorted = false;
          final double temp = distances[sort + 1];
          distances[sort + 1] = distances[sort];
          distances[sort] = temp;

          // swap primitives too
          final PrimitiveKey temp2 = primitives.get(sort + 1);
          primitives.set(sort + 1, primitives.get(sort));
          primitives.set(sort, temp2);
        }
      }
    }

    final Vector3[] positions = new Vector3[distances.length];
    for (int i = 0; i < distances.length; i++) {
      positions[i] = ray.getDirection().multiply(distances[i], new Vector3()).addLocal(ray.getOrigin());
    }
    return new IntersectionRecord(distances, positions, primitives);
  }

  @Override
  public Mesh makeCopy(final boolean shareGeometricData) {
    // get copy of basic spatial info
    final Mesh mesh = (Mesh) super.makeCopy(shareGeometricData);

    // if we are sharing, just reuse meshdata
    if (shareGeometricData) {
      mesh.setMeshData(_meshData);
    } else {
      // make a copy of our data
      mesh.setMeshData(_meshData.makeCopy());
    }

    // copy our basic properties
    mesh.setModelBound(_modelBound != null ? _modelBound.clone(null) : null);
    mesh.setVisible(_isVisible);
    mesh.setRenderMaterial(_material);
    mesh.setOccluderMaterial(_occluderMaterial);
    mesh._properties.putAll(_properties);

    // return
    return mesh;
  }

  /**
   * Let this mesh know we want to change its indices to the provided new order. Override this to
   * provide extra functionality for sub types as needed.
   *
   * @param newIndices
   *          the IntBufferData to switch to.
   * @param modes
   *          the new segment modes to use.
   * @param lengths
   *          the new lengths to use.
   */
  public void reorderIndices(final IndexBufferData<?> newIndices, final IndexMode[] modes, final int[] lengths) {
    _meshData.setIndices(newIndices);
    _meshData.setIndexModes(modes);
    _meshData.setIndexLengths(lengths);
  }

  /**
   * Swap around the order of the vertex data in this Mesh. This is usually called by a tool that has
   * attempted to determine a more optimal order for vertex data.
   *
   * @param newVertexOrder
   *          a mapping to the desired new order, where the current location of a vertex is the index
   *          into this array and the value at that location in the array is the new location to store
   *          the vertex data.
   */
  public void reorderVertexData(final int[] newVertexOrder) {
    reorderVertexData(newVertexOrder, _meshData);
  }

  /**
   * Swap around the order of the vertex data in the given MeshData. Override to provide specific
   * behavior to the Mesh object.
   *
   * @param newVertexOrder
   *          a mapping to the desired new order, where the current location of a vertex is the index
   *          into this array and the value at that location in the array is the new location to store
   *          the vertex data.
   * @param meshData
   *          the meshData object to work against.
   */
  protected void reorderVertexData(final int[] newVertexOrder, final MeshData meshData) {
    // must be non-null
    final FloatBufferData verts = meshData.getVertexCoords().makeCopy();

    final FloatBufferData norms = meshData.getNormalBuffer() != null ? meshData.getVertexCoords().makeCopy() : null;
    final FloatBufferData colors = meshData.getColorBuffer() != null ? meshData.getColorCoords().makeCopy() : null;
    final FloatBufferData tangents =
        meshData.getTangentBuffer() != null ? meshData.getTangentCoords().makeCopy() : null;
    final FloatBufferData[] uvs = new FloatBufferData[meshData.getMaxTextureUnitUsed() + 1];
    for (int k = 0; k < uvs.length; k++) {
      final FloatBufferData tex = meshData.getTextureCoords(k);
      if (tex != null) {
        uvs[k] = tex.makeCopy();
      }
    }

    int vert;
    for (int i = 0; i < meshData.getVertexCount(); i++) {
      vert = newVertexOrder[i];
      if (vert == -1) {
        vert = i;
      }
      BufferUtils.copy(meshData.getVertexBuffer(), i * verts.getValuesPerTuple(), verts.getBuffer(),
          vert * verts.getValuesPerTuple(), verts.getValuesPerTuple());
      if (norms != null) {
        BufferUtils.copy(meshData.getNormalBuffer(), i * norms.getValuesPerTuple(), norms.getBuffer(),
            vert * norms.getValuesPerTuple(), norms.getValuesPerTuple());
      }
      if (colors != null) {
        BufferUtils.copy(meshData.getColorBuffer(), i * colors.getValuesPerTuple(), colors.getBuffer(),
            vert * colors.getValuesPerTuple(), colors.getValuesPerTuple());
      }
      if (tangents != null) {
        BufferUtils.copy(meshData.getTangentBuffer(), i * tangents.getValuesPerTuple(), tangents.getBuffer(),
            vert * tangents.getValuesPerTuple(), tangents.getValuesPerTuple());
      }
      for (int k = 0; k < uvs.length; k++) {
        if (uvs[k] != null) {
          BufferUtils.copy(meshData.getTextureBuffer(k), i * uvs[k].getValuesPerTuple(), uvs[k].getBuffer(),
              vert * uvs[k].getValuesPerTuple(), uvs[k].getValuesPerTuple());
        }
      }
    }
    meshData.setVertexCoords(verts);
    meshData.setNormalCoords(norms);
    meshData.setColorCoords(colors);
    meshData.setTangentCoords(tangents);
    for (int k = 0; k < uvs.length; k++) {
      if (uvs[k] != null) {
        meshData.setTextureCoords(uvs[k], k);
      }
    }
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends Mesh> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_meshData, "meshData", null);
    capsule.write(_modelBound, "modelBound", null);
    capsule.write(_isVisible, "visible", true);
    capsule.write(_instanceCount, "instanceCount", 1);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _meshData = capsule.readSavable("meshData", null);
    _modelBound = capsule.readSavable("modelBound", null);
    _isVisible = capsule.readBoolean("visible", true);
    _instanceCount = capsule.readInt("instanceCount", 1);
  }
}
