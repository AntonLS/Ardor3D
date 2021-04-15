/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scene.state.lwjgl3.util;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.EXTTextureCompressionLATC;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL30C;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.DepthTextureCompareFunc;
import com.ardor3d.image.Texture.DepthTextureCompareMode;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.ImageUtils;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.texture.ITextureUtils;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3TextureStateUtil;
import com.ardor3d.util.Ardor3dException;

public class Lwjgl3TextureUtils implements ITextureUtils {

  private static final Logger logger = Logger.getLogger(Lwjgl3TextureUtils.class.getName());

  @Override
  public void loadTexture(final Texture texture, final int unit) {
    Lwjgl3TextureStateUtil.load(texture, unit);
  }

  @Override
  public void deleteTexture(final Texture texture) {
    Lwjgl3TextureStateUtil.deleteTexture(texture);
  }

  @Override
  public void deleteTextureIds(final Collection<Integer> ids) {
    Lwjgl3TextureStateUtil.deleteTextureIds(ids);
  }

  @Override
  public void updateTexture1DSubImage(final Texture1D destination, final int dstOffsetX, final int dstWidth,
      final ByteBuffer source, final int srcOffsetX) {
    updateTexSubImage(destination, dstOffsetX, 0, 0, dstWidth, 0, 0, source, srcOffsetX, 0, 0, 0, 0, null);
  }

  @Override
  public void updateTexture2DSubImage(final Texture2D destination, final int dstOffsetX, final int dstOffsetY,
      final int dstWidth, final int dstHeight, final ByteBuffer source, final int srcOffsetX, final int srcOffsetY,
      final int srcTotalWidth) {
    updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX, srcOffsetY, 0,
        srcTotalWidth, 0, null);
  }

  @Override
  public void updateTexture3DSubImage(final Texture3D destination, final int dstOffsetX, final int dstOffsetY,
      final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
      final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
      final int srcTotalHeight) {
    updateTexSubImage(destination, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth, dstHeight, dstDepth, source,
        srcOffsetX, srcOffsetY, srcOffsetZ, srcTotalWidth, srcTotalHeight, null);
  }

  @Override
  public void updateTextureCubeMapSubImage(final TextureCubeMap destination, final Face dstFace, final int dstOffsetX,
      final int dstOffsetY, final int dstWidth, final int dstHeight, final ByteBuffer source, final int srcOffsetX,
      final int srcOffsetY, final int srcTotalWidth) {
    updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX, srcOffsetY, 0,
        srcTotalWidth, 0, dstFace);
  }

  private void updateTexSubImage(final Texture destination, final int dstOffsetX, final int dstOffsetY,
      final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
      final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
      final int srcTotalHeight, final Face dstFace) {

    // Ignore textures that do not have an id set
    if (destination.getTextureIdForContext(ContextManager.getCurrentContext()) == 0) {
      logger.warning("Attempting to update a texture that is not currently on the card.");
      return;
    }

    // Determine the original texture configuration, so that this method can
    // restore the texture configuration to its original state.
    final int origAlignment = GL11C.glGetInteger(GL11C.GL_UNPACK_ALIGNMENT);
    final int origRowLength = 0;
    final int origImageHeight = 0;
    final int origSkipPixels = 0;
    final int origSkipRows = 0;
    final int origSkipImages = 0;

    final int alignment = 1;

    int rowLength;
    if (srcTotalWidth == dstWidth) {
      // When the row length is zero, then the width parameter is used.
      // We use zero in these cases in the hope that we can avoid two
      // unnecessary calls to glPixelStorei.
      rowLength = 0;
    } else {
      // The number of pixels in a row is different than the number of
      // pixels in the region to be uploaded to the texture.
      rowLength = srcTotalWidth;
    }

    int imageHeight;
    if (srcTotalHeight == dstHeight) {
      // When the image height is zero, then the height parameter is used.
      // We use zero in these cases in the hope that we can avoid two
      // unnecessary calls to glPixelStorei.
      imageHeight = 0;
    } else {
      // The number of pixels in a row is different than the number of
      // pixels in the region to be uploaded to the texture.
      imageHeight = srcTotalHeight;
    }

    // Grab pixel format
    final int pixelFormat;
    if (destination.getImage() != null) {
      pixelFormat = Lwjgl3TextureUtils.getGLPixelFormat(destination.getImage().getDataFormat());
    } else {
      pixelFormat = Lwjgl3TextureUtils.getGLPixelFormatFromStoreFormat(destination.getTextureStoreFormat());
    }

    // bind...
    Lwjgl3TextureStateUtil.doTextureBind(destination, 0, false);

    // Update the texture configuration (when necessary).

    if (origAlignment != alignment) {
      GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, alignment);
    }
    if (origRowLength != rowLength) {
      GL11C.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, rowLength);
    }
    if (origSkipPixels != srcOffsetX) {
      GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_PIXELS, srcOffsetX);
    }
    // NOTE: The below will be skipped for texture types that don't support them because we are passing
    // in 0's.
    if (origSkipRows != srcOffsetY) {
      GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_ROWS, srcOffsetY);
    }
    if (origImageHeight != imageHeight) {
      GL11C.glPixelStorei(GL12C.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
    }
    if (origSkipImages != srcOffsetZ) {
      GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_IMAGES, srcOffsetZ);
    }

    // Upload the image region into the texture.
    try {
      switch (destination.getType()) {
        case TwoDimensional:
          GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, dstOffsetX, dstOffsetY, dstWidth, dstHeight, pixelFormat,
              GL11C.GL_UNSIGNED_BYTE, source);
          break;
        case OneDimensional:
          GL11C.glTexSubImage1D(GL11C.GL_TEXTURE_1D, 0, dstOffsetX, dstWidth, pixelFormat, GL11C.GL_UNSIGNED_BYTE,
              source);
          break;
        case ThreeDimensional:
          GL12C.glTexSubImage3D(GL12C.GL_TEXTURE_3D, 0, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth, dstHeight,
              dstDepth, pixelFormat, GL11C.GL_UNSIGNED_BYTE, source);
          break;
        case CubeMap:
          GL11C.glTexSubImage2D(Lwjgl3TextureStateUtil.getGLCubeMapFace(dstFace), 0, dstOffsetX, dstOffsetY, dstWidth,
              dstHeight, pixelFormat, GL11C.GL_UNSIGNED_BYTE, source);
          break;
        default:
          throw new Ardor3dException("Unsupported type for updateTextureSubImage: " + destination.getType());
      }
    } finally {
      // Restore the texture configuration (when necessary)...
      // Restore alignment.
      if (origAlignment != alignment) {
        GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, origAlignment);
      }
      // Restore row length.
      if (origRowLength != rowLength) {
        GL11C.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, origRowLength);
      }
      // Restore skip pixels.
      if (origSkipPixels != srcOffsetX) {
        GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_PIXELS, origSkipPixels);
      }
      // Restore skip rows.
      if (origSkipRows != srcOffsetY) {
        GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_ROWS, origSkipRows);
      }
      // Restore image height.
      if (origImageHeight != imageHeight) {
        GL11C.glPixelStorei(GL12C.GL_UNPACK_IMAGE_HEIGHT, origImageHeight);
      }
      // Restore skip images.
      if (origSkipImages != srcOffsetZ) {
        GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_IMAGES, origSkipImages);
      }
    }
  }

  @Override
  public ByteBuffer readTextureContents(final Texture texture, final int level, final int baseWidth,
      final int baseHeight, final ImageDataFormat imageFormat, final PixelDataType pixelType, final ByteBuffer store) {
    var rVal = store;

    // make sure texture is current
    Lwjgl3TextureStateUtil.doTextureBind(texture, 0, true);

    // make sure our buffer is big enough
    final int width = baseWidth >> level;
    final int height = baseHeight >> level;
    final int size = width * height * ImageUtils.getPixelByteSize(imageFormat, pixelType);
    if (rVal == null || rVal.capacity() < size) {
      rVal = BufferUtils.createByteBuffer(size);
    } else {
      rVal.limit(size);
      rVal.rewind();
    }

    // grab texture data in the specified format
    GL11C.glGetTexImage(//
        Lwjgl3TextureStateUtil.getGLType(texture.getType()), // type
        level, // level
        getGLPixelFormat(imageFormat), //
        getGLPixelDataType(pixelType), //
        rVal // buffer
    );

    return rVal;
  }

  public static int getGLInternalFormat(final TextureStoreFormat format) {
    switch (format) {
      // first some frequently used formats
      case RGBA8:
        return GL11C.GL_RGBA8;
      case RGB8:
        return GL11C.GL_RGB8;
      case CompressedRGBA:
        return GL13C.GL_COMPRESSED_RGBA;
      case CompressedRGB:
        return GL13C.GL_COMPRESSED_RGB;
      case CompressedRG:
        return GL30C.GL_COMPRESSED_RG;
      case CompressedRed:
        return GL30C.GL_COMPRESSED_RED;
      case NativeDXT1:
        return EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
      case NativeDXT1A:
        return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
      case NativeDXT3:
        return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
      case NativeDXT5:
        return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
      case NativeLATC_L:
        return EXTTextureCompressionLATC.GL_COMPRESSED_LUMINANCE_LATC1_EXT;
      case NativeLATC_LA:
        return EXTTextureCompressionLATC.GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;

      // The rest...
      case R3G3B2:
        return GL11C.GL_R3_G3_B2;
      case RGB4:
        return GL11C.GL_RGB4;
      case RGB5:
        return GL11C.GL_RGB5;
      case RGB10:
        return GL11C.GL_RGB10;
      case RGB12:
        return GL11C.GL_RGB12;
      case RGB16:
        return GL11C.GL_RGB16;
      case RGBA2:
        return GL11C.GL_RGBA2;
      case RGBA4:
        return GL11C.GL_RGBA4;
      case RGB5A1:
        return GL11C.GL_RGB5_A1;
      case RGB10A2:
        return GL11C.GL_RGB10_A2;
      case RGBA12:
        return GL11C.GL_RGBA12;
      case RGBA16:
        return GL11C.GL_RGBA16;
      case Depth:
        return GL11C.GL_DEPTH_COMPONENT;
      case Depth16:
        return GL14C.GL_DEPTH_COMPONENT16;
      case Depth24:
        return GL14C.GL_DEPTH_COMPONENT24;
      case Depth32:
        return GL14C.GL_DEPTH_COMPONENT32;
      case Depth32F:
        return GL30C.GL_DEPTH_COMPONENT32F;
      case RGB16F:
        return GL30C.GL_RGB16F;
      case RGB32F:
        return GL30C.GL_RGB32F;
      case RGBA16F:
        return GL30C.GL_RGBA16F;
      case RGBA32F:
        return GL30C.GL_RGBA32F;
      case R8:
        return GL30C.GL_R8;
      case R16:
        return GL30C.GL_R16;
      case RG8:
        return GL30C.GL_RG8;
      case RG16:
        return GL30C.GL_RG16;
      case R16F:
        return GL30C.GL_R16F;
      case R32F:
        return GL30C.GL_R32F;
      case RG16F:
        return GL30C.GL_RG16F;
      case RG32F:
        return GL30C.GL_RG32F;
      case R8I:
        return GL30C.GL_R8I;
      case R8UI:
        return GL30C.GL_R8UI;
      case R16I:
        return GL30C.GL_R16I;
      case R16UI:
        return GL30C.GL_R16UI;
      case R32I:
        return GL30C.GL_R32I;
      case R32UI:
        return GL30C.GL_R32UI;
      case RG8I:
        return GL30C.GL_RG8I;
      case RG8UI:
        return GL30C.GL_RG8UI;
      case RG16I:
        return GL30C.GL_RG16I;
      case RG16UI:
        return GL30C.GL_RG16UI;
      case RG32I:
        return GL30C.GL_RG32I;
      case RG32UI:
        return GL30C.GL_RG32UI;
      default:
        break;
    }
    throw new IllegalArgumentException("Incorrect format set: " + format);
  }

  public static int getGLPixelDataType(final PixelDataType type) {
    switch (type) {
      case Byte:
        return GL11C.GL_BYTE;
      case Float:
        return GL11C.GL_FLOAT;
      case HalfFloat:
        return GL30C.GL_HALF_FLOAT;
      case Short:
        return GL11C.GL_SHORT;
      case UnsignedShort:
        return GL11C.GL_UNSIGNED_SHORT;
      case Int:
        return GL11C.GL_INT;
      case UnsignedInt:
        return GL11C.GL_UNSIGNED_INT;
      case UnsignedByte:
        return GL11C.GL_UNSIGNED_BYTE;
      case UnsignedByte_3_3_2:
        return GL12C.GL_UNSIGNED_BYTE_3_3_2;
      case UnsignedByte_2_3_3_Rev:
        return GL12C.GL_UNSIGNED_BYTE_2_3_3_REV;
      case UnsignedShort_5_6_5:
        return GL12C.GL_UNSIGNED_SHORT_5_6_5;
      case UnsignedShort_5_6_5_Rev:
        return GL12C.GL_UNSIGNED_SHORT_5_6_5_REV;
      case UnsignedShort_4_4_4_4:
        return GL12C.GL_UNSIGNED_SHORT_4_4_4_4;
      case UnsignedShort_4_4_4_4_Rev:
        return GL12C.GL_UNSIGNED_SHORT_4_4_4_4_REV;
      case UnsignedShort_5_5_5_1:
        return GL12C.GL_UNSIGNED_SHORT_5_5_5_1;
      case UnsignedShort_1_5_5_5_Rev:
        return GL12C.GL_UNSIGNED_SHORT_1_5_5_5_REV;
      case UnsignedInt_8_8_8_8:
        return GL12C.GL_UNSIGNED_INT_8_8_8_8;
      case UnsignedInt_8_8_8_8_Rev:
        return GL12C.GL_UNSIGNED_INT_8_8_8_8_REV;
      case UnsignedInt_10_10_10_2:
        return GL12C.GL_UNSIGNED_INT_10_10_10_2;
      case UnsignedInt_2_10_10_10_Rev:
        return GL12C.GL_UNSIGNED_INT_2_10_10_10_REV;
      default:
        throw new Error("Unhandled type: " + type);
    }
  }

  public static int getGLPixelFormat(final ImageDataFormat format) {
    switch (format) {
      case RGBA:
        return GL11C.GL_RGBA;
      case RGB:
        return GL11C.GL_RGB;
      case RG:
        return GL30C.GL_RG;
      case Alpha:
        return GL11C.GL_ALPHA;
      case Depth:
        return GL11C.GL_DEPTH_COMPONENT;
      case BGR:
        return GL12C.GL_BGR;
      case BGRA:
        return GL12C.GL_BGRA;
      case Red:
        return GL11C.GL_RED;
      case Blue:
        return GL11C.GL_BLUE;
      case Green:
        return GL11C.GL_GREEN;
      case StencilIndex:
        return GL11C.GL_STENCIL_INDEX;
      default:
        break;
    }
    throw new IllegalArgumentException("Incorrect format set: " + format);
  }

  public static int getGLPixelFormatFromStoreFormat(final TextureStoreFormat format) {
    switch (format) {
      case RGBA2:
      case RGBA4:
      case RGBA8:
      case RGB5A1:
      case RGB10A2:
      case RGBA12:
      case RGBA16:
      case CompressedRGBA:
      case NativeDXT1A:
      case NativeDXT3:
      case NativeDXT5:
      case RGBA16F:
      case RGBA32F:
        return GL11C.GL_RGBA;
      case R3G3B2:
      case RGB4:
      case RGB5:
      case RGB8:
      case RGB10:
      case RGB12:
      case RGB16:
      case CompressedRGB:
      case NativeDXT1:
      case RGB16F:
      case RGB32F:
        return GL11C.GL_RGB;
      case NativeLATC_L:
        return GL11.GL_LUMINANCE; // XXX: Not sure about this in core
      case NativeLATC_LA:
        return GL11.GL_LUMINANCE_ALPHA; // XXX: Not sure about this in core
      case Depth:
      case Depth16:
      case Depth24:
      case Depth32:
      case Depth32F:
        return GL11C.GL_DEPTH_COMPONENT;
      case R8:
      case R16:
      case R16F:
      case R32F:
      case R8I:
      case R8UI:
      case R16I:
      case R16UI:
      case R32I:
      case R32UI:
      case CompressedRed:
        return GL11C.GL_RED;
      case RG8:
      case RG16:
      case RG16F:
      case RG32F:
      case RG8I:
      case RG8UI:
      case RG16I:
      case RG16UI:
      case RG32I:
      case RG32UI:
      case CompressedRG:
        return GL30C.GL_RG;
      default:
        break;
    }
    throw new IllegalArgumentException("Incorrect format set: " + format);
  }

  public static int getGLDepthTextureCompareMode(final DepthTextureCompareMode mode) {
    switch (mode) {
      case RtoTexture:
        return ARBShadow.GL_COMPARE_R_TO_TEXTURE_ARB;
      case None:
      default:
        return GL11C.GL_NONE;
    }
  }

  public static int getGLDepthTextureCompareFunc(final DepthTextureCompareFunc func) {
    switch (func) {
      case GreaterThanEqual:
        return GL11C.GL_GEQUAL;
      case LessThanEqual:
      default:
        return GL11C.GL_LEQUAL;
    }
  }

  public static int getGLMagFilter(final MagnificationFilter magFilter) {
    switch (magFilter) {
      case Bilinear:
        return GL11C.GL_LINEAR;
      case NearestNeighbor:
      default:
        return GL11C.GL_NEAREST;

    }
  }

  public static int getGLMinFilter(final MinificationFilter filter) {
    switch (filter) {
      case BilinearNoMipMaps:
        return GL11C.GL_LINEAR;
      case Trilinear:
        return GL11C.GL_LINEAR_MIPMAP_LINEAR;
      case BilinearNearestMipMap:
        return GL11C.GL_LINEAR_MIPMAP_NEAREST;
      case NearestNeighborNoMipMaps:
        return GL11C.GL_NEAREST;
      case NearestNeighborNearestMipMap:
        return GL11C.GL_NEAREST_MIPMAP_NEAREST;
      case NearestNeighborLinearMipMap:
        return GL11C.GL_NEAREST_MIPMAP_LINEAR;
    }
    throw new IllegalArgumentException("invalid MinificationFilter type: " + filter);
  }
}
