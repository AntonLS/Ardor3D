
package com.ardor3d.image.loader.hdr;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This file contains code to read and write four byte rgbe file format developed by Greg Ward. It
 * handles the conversions between rgbe and pixels consisting of floats. The data is assumed to be
 * an array of floats. By default there are three floats per pixel in the order red, green, blue.
 * (RGBE_DATA_??? values control this.) Only the mimimal header reading and writing is implemented.
 * Each routine does error checking and will return a status value as defined below. This code is
 * intended as a skeleton so feel free to modify it to suit your needs.
 * <P>
 *
 * Ported to Java and restructured by Kenneth Russell. <BR>
 * posted to http://www.graphics.cornell.edu/~bjw/ <BR>
 * written by Bruce Walter (bjw@graphics.cornell.edu) 5/26/95 <BR>
 * based on code written by Greg Ward <BR>
 */

public class RGBE {
  // Flags indicating which fields in a Header are valid
  private static final int VALID_PROGRAMTYPE = 0x01;
  private static final int VALID_GAMMA = 0x02;
  private static final int VALID_EXPOSURE = 0x04;

  private static final String gammaString = "GAMMA=";
  private static final String exposureString = "EXPOSURE=";

  private static final Pattern widthHeightPattern = Pattern.compile("-Y (\\d+) \\+X (\\d+)");

  public static class Header {
    // Indicates which fields are valid
    private final int valid;

    // Listed at beginning of file to identify it after "#?".
    // Defaults to "RGBE"
    private final String programType;

    // Image has already been gamma corrected with given gamma.
    // Defaults to 1.0 (no correction)
    private final float gamma;

    // A value of 1.0 in an image corresponds to <exposure>
    // watts/steradian/m^2. Defaults to 1.0.
    private final float exposure;

    // Width and height of image
    private final int width;
    private final int height;

    private Header(final int valid, final String programType, final float gamma, final float exposure, final int width,
      final int height) {
      this.valid = valid;
      this.programType = programType;
      this.gamma = gamma;
      this.exposure = exposure;
      this.width = width;
      this.height = height;
    }

    public boolean isProgramTypeValid() { return ((valid & VALID_PROGRAMTYPE) != 0); }

    public boolean isGammaValid() { return ((valid & VALID_GAMMA) != 0); }

    public boolean isExposureValid() { return ((valid & VALID_EXPOSURE) != 0); }

    public String getProgramType() { return programType; }

    public float getGamma() { return gamma; }

    public float getExposure() { return exposure; }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    @Override
    public String toString() {
      final StringBuilder buf = new StringBuilder();
      if (isProgramTypeValid()) {
        buf.append(" Program type: ");
        buf.append(getProgramType());
      }
      buf.append(" Gamma");
      if (isGammaValid()) {
        buf.append(" [valid]");
      }
      buf.append(": ");
      buf.append(getGamma());
      buf.append(" Exposure");
      if (isExposureValid()) {
        buf.append(" [valid]");
      }
      buf.append(": ");
      buf.append(getExposure());
      buf.append(" Width: ");
      buf.append(getWidth());
      buf.append(" Height: ");
      buf.append(getHeight());
      return buf.toString();
    }
  }

  @SuppressWarnings("deprecation")
  public static Header readHeader(final DataInputStream in) throws IOException {
    int valid = 0;
    String programType = null;
    float gamma = 1.0f;
    float exposure = 1.0f;
    int width = 0;
    int height = 0;

    String buf = in.readLine();
    if (buf == null) {
      throw new IOException("Unexpected EOF reading magic token");
    }
    if (buf.charAt(0) == '#' && buf.charAt(1) == '?') {
      valid |= VALID_PROGRAMTYPE;
      programType = buf.substring(2);
      buf = in.readLine();
      if (buf == null) {
        throw new IOException("Unexpected EOF reading line after magic token");
      }
    }

    boolean foundFormat = false;
    boolean done = false;
    while (!done) {
      if (buf.equals("FORMAT=32-bit_rle_rgbe")) {
        foundFormat = true;
      } else if (buf.startsWith(gammaString)) {
        valid |= VALID_GAMMA;
        gamma = Float.parseFloat(buf.substring(gammaString.length()));
      } else if (buf.startsWith(exposureString)) {
        valid |= VALID_EXPOSURE;
        exposure = Float.parseFloat(buf.substring(exposureString.length()));
      } else {
        final Matcher m = widthHeightPattern.matcher(buf);
        if (m.matches()) {
          width = Integer.parseInt(m.group(2));
          height = Integer.parseInt(m.group(1));
          done = true;
        }
      }

      if (!done) {
        buf = in.readLine();
        if (buf == null) {
          throw new IOException("Unexpected EOF reading header");
        }
      }
    }

    if (!foundFormat) {
      throw new IOException("No FORMAT specifier found");
    }

    return new Header(valid, programType, gamma, exposure, width, height);
  }

  /** Simple read routine. Will not correctly handle run length encoding. */
  public static void readPixels(final DataInputStream in, final float[] data, int numpixels) throws IOException {
    final byte[] rgbe = new byte[4];
    final float[] rgb = new float[3];
    int offset = 0;

    while (numpixels-- > 0) {
      final int numRead = in.read(rgbe);
      if (numRead < 4) {
        throw new IOException("Only able to read " + numRead + " out of expected " + rgbe.length + " bytes");
      }
      rgbe2float(rgb, rgbe, 0);
      data[offset++] = rgb[0];
      data[offset++] = rgb[1];
      data[offset++] = rgb[2];
    }
  }

  public static void readPixelsRaw(final DataInputStream in, final byte[] data, final int offset, final int numpixels)
      throws IOException {
    final int numExpected = 4 * numpixels;
    final int numRead = in.read(data, offset, numExpected);
    if (numRead < numExpected) {
      throw new IOException("Error reading raw pixels: got " + numRead + " bytes, expected " + numExpected);
    }
  }

  public static void readPixelsRawRLE(final DataInputStream in, final byte[] data, int offset, final int scanline_width,
      int num_scanlines) throws IOException {
    final byte[] rgbe = new byte[4];
    byte[] scanline_buffer = null;
    int ptr, ptr_end;
    int count;
    final byte[] buf = new byte[2];

    if ((scanline_width < 8) || (scanline_width > 0x7fff)) {
      /* run length encoding is not allowed so read flat */
      readPixelsRaw(in, data, offset, scanline_width * num_scanlines);
    }

    /* read in each successive scanline */
    while (num_scanlines > 0) {
      if (in.read(rgbe) < rgbe.length) {
        throw new IOException("Error reading bytes: expected " + rgbe.length);
      }

      if ((rgbe[0] != 2) || (rgbe[1] != 2) || ((rgbe[2] & 0x80) != 0)) {
        /* this file is not run length encoded */
        data[offset++] = rgbe[0];
        data[offset++] = rgbe[1];
        data[offset++] = rgbe[2];
        data[offset++] = rgbe[3];
        readPixelsRaw(in, data, offset, scanline_width * num_scanlines - 1);
      }

      if ((((rgbe[2] & 0xFF) << 8) | (rgbe[3] & 0xFF)) != scanline_width) {
        throw new IOException(
            "Wrong scanline width " + (((rgbe[2] & 0xFF) << 8) | (rgbe[3] & 0xFF)) + ", expected " + scanline_width);
      }

      if (scanline_buffer == null) {
        scanline_buffer = new byte[4 * scanline_width];
      }

      ptr = 0;
      /* read each of the four channels for the scanline into the buffer */
      for (int i = 0; i < 4; i++) {
        ptr_end = (i + 1) * scanline_width;
        while (ptr < ptr_end) {
          if (in.read(buf) < buf.length) {
            throw new IOException("Error reading 2-byte buffer");
          }
          if ((buf[0] & 0xFF) > 128) {
            /* a run of the same value */
            count = (buf[0] & 0xFF) - 128;
            if ((count == 0) || (count > ptr_end - ptr)) {
              throw new IOException("Bad scanline data");
            }
            while (count-- > 0) {
              scanline_buffer[ptr++] = buf[1];
            }
          } else {
            /* a non-run */
            count = buf[0] & 0xFF;
            if ((count == 0) || (count > ptr_end - ptr)) {
              throw new IOException("Bad scanline data");
            }
            scanline_buffer[ptr++] = buf[1];
            if (--count > 0) {
              if (in.read(scanline_buffer, ptr, count) < count) {
                throw new IOException("Error reading non-run data");
              }
              ptr += count;
            }
          }
        }
      }
      /* copy byte data to output */
      for (int i = 0; i < scanline_width; i++) {
        data[offset++] = scanline_buffer[i];
        data[offset++] = scanline_buffer[i + scanline_width];
        data[offset++] = scanline_buffer[i + 2 * scanline_width];
        data[offset++] = scanline_buffer[i + 3 * scanline_width];
      }
      num_scanlines--;
    }
  }

  /** Standard conversion from float pixels to rgbe pixels. */
  public static void float2rgbe(final byte[] rgbe, final float red, final float green, final float blue) {
    float v;

    v = red;
    if (green > v) {
      v = green;
    }
    if (blue > v) {
      v = blue;
    }
    if (v < 1e-32f) {
      rgbe[0] = rgbe[1] = rgbe[2] = rgbe[3] = 0;
    } else {
      final FracExp fe = frexp(v);
      v = (float) (fe.getFraction() * 256.0 / v);
      rgbe[0] = (byte) (red * v);
      rgbe[1] = (byte) (green * v);
      rgbe[2] = (byte) (blue * v);
      rgbe[3] = (byte) (fe.getExponent() + 128);
    }
  }

  /**
   * Standard conversion from rgbe to float pixels. Note: Ward uses ldexp(col+0.5,exp-(128+8)).
   * However we wanted pixels in the range [0,1] to map back into the range [0,1].
   */
  public static void rgbe2float(final float[] rgb, final byte[] rgbe, final int startRGBEOffset) {
    float f;

    if (rgbe[startRGBEOffset + 3] != 0) { /* nonzero pixel */
      f = (float) ldexp(1.0, (rgbe[startRGBEOffset + 3] & 0xFF) - (128 + 8));
      rgb[0] = (rgbe[startRGBEOffset + 0] & 0xFF) * f;
      rgb[1] = (rgbe[startRGBEOffset + 1] & 0xFF) * f;
      rgb[2] = (rgbe[startRGBEOffset + 2] & 0xFF) * f;
    } else {
      rgb[0] = 0;
      rgb[1] = 0;
      rgb[2] = 0;
    }
  }

  /**
   * Standard conversion from rgbe to float pixels. Note: Ward uses ldexp(col+0.5,exp-(128+8)).
   * However we wanted pixels in the range [0,1] to map back into the range [0,1].
   */
  public static void rgbe2float(final FloatBuffer store, final byte[] rgbe, final int startRGBEOffset) {
    float f;

    if (rgbe[startRGBEOffset + 3] != 0) { /* nonzero pixel */
      f = (float) ldexp(1.0, (rgbe[startRGBEOffset + 3] & 0xFF) - (128 + 8));
      store.put((rgbe[startRGBEOffset + 0] & 0xFF) * f);
      store.put((rgbe[startRGBEOffset + 1] & 0xFF) * f);
      store.put((rgbe[startRGBEOffset + 2] & 0xFF) * f);
    } else {
      store.put(0).put(0).put(0);
    }
  }

  public static void rgbe2float(final FloatBuffer store, final byte[] scanline) {
    for (int i = 0; i < scanline.length; i += 4) {
      rgbe2float(store, scanline, i);
    }
  }

  public static double ldexp(double value, final int exp) {
    if (!finite(value) || value == 0.0) {
      return value;
    }
    value = scalbn(value, exp);
    // No good way to indicate errno (want to avoid throwing
    // exceptions because don't know about stability of calculations)
    // if(!finite(value)||value==0.0) errno = ERANGE;
    return value;
  }

  // ----------------------------------------------------------------------
  // Internals only below this point
  //

  // ----------------------------------------------------------------------
  // Math routines, some fdlibm-derived
  //

  static class FracExp {
    private final double fraction;
    private final int exponent;

    public FracExp(final double fraction, final int exponent) {
      this.fraction = fraction;
      this.exponent = exponent;
    }

    public double getFraction() { return fraction; }

    public int getExponent() { return exponent; }
  }

  private static final double two54 = 1.80143985094819840000e+16; /* 43500000 00000000 */
  private static final double twom54 = 5.55111512312578270212e-17; /* 0x3C900000 0x00000000 */
  private static final double huge = 1.0e+300;
  private static final double tiny = 1.0e-300;

  private static int hi(final double x) {
    final long bits = Double.doubleToRawLongBits(x);
    return (int) (bits >>> 32);
  }

  private static int lo(final double x) {
    final long bits = Double.doubleToRawLongBits(x);
    return (int) bits;
  }

  private static double fromhilo(final int hi, final int lo) {
    return Double.longBitsToDouble((((long) hi) << 32) | ((lo) & 0xFFFFFFFFL));
  }

  private static FracExp frexp(double x) {
    int hx = hi(x);
    int ix = 0x7fffffff & hx;
    int lx = lo(x);
    int e = 0;
    if (ix >= 0x7ff00000 || ((ix | lx) == 0)) {
      return new FracExp(x, e); /* 0,inf,nan */
    }
    if (ix < 0x00100000) { /* subnormal */
      x *= two54;
      hx = hi(x);
      ix = hx & 0x7fffffff;
      e = -54;
    }
    e += (ix >> 20) - 1022;
    hx = (hx & 0x800fffff) | 0x3fe00000;
    lx = lo(x);
    return new FracExp(fromhilo(hx, lx), e);
  }

  private static boolean finite(final double x) {
    int hx;
    hx = hi(x);
    return (((hx & 0x7fffffff) - 0x7ff00000) >> 31) != 0;
  }

  /**
   * copysign(double x, double y) <BR>
   * copysign(x,y) returns a value with the magnitude of x and with the sign bit of y.
   */
  private static double copysign(final double x, final double y) {
    return fromhilo((hi(x) & 0x7fffffff) | (hi(y) & 0x80000000), lo(x));
  }

  /**
   * scalbn (double x, int n) <BR>
   * scalbn(x,n) returns x* 2**n computed by exponent manipulation rather than by actually performing
   * an exponentiation or a multiplication.
   */
  private static double scalbn(double x, final int n) {
    int hx = hi(x);
    final int lx = lo(x);
    int k = (hx & 0x7ff00000) >> 20; /* extract exponent */
    if (k == 0) { /* 0 or subnormal x */
      if ((lx | (hx & 0x7fffffff)) == 0) {
        return x; /* +-0 */
      }
      x *= two54;
      hx = hi(x);
      k = ((hx & 0x7ff00000) >> 20) - 54;
      if (n < -50000) {
        return tiny * x; /* underflow */
      }
    }
    if (k == 0x7ff) {
      return x + x; /* NaN or Inf */
    }
    k = k + n;
    if (k > 0x7fe) {
      return huge * copysign(huge, x); /* overflow */
    }
    if (k > 0) {
      /* normal result */
      return fromhilo((hx & 0x800fffff) | (k << 20), lo(x));
    }
    if (k <= -54) {
      if (n > 50000) {
        /* in case integer overflow in n+k */
        return huge * copysign(huge, x); /* overflow */
      } else {
        return tiny * copysign(tiny, x); /* underflow */
      }
    }
    k += 54; /* subnormal result */
    x = fromhilo((hx & 0x800fffff) | (k << 20), lo(x));
    return x * twom54;
  }
}
