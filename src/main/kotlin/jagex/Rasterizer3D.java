package jagex;

public final class Rasterizer3D extends Rasterizer2D {

    public static final int[] __et_p;

    public static int Rasterizer3D_alpha;

    static int[] Rasterizer3D_rowOffsets;

    public static int[] Rasterizer3D_sine;
    public static int[] Rasterizer3D_cosine;

    /**
     * Contains the pixel index onsets for each row
     * that together comprise the raster.
     */
    public static int[] scanOffsets;

    public static boolean triangleIsOutOfBounds;
    public static boolean notTextured = true;
    public static int alpha;

    /**
     * Raster center x
     */
    public static int originViewX;

    /**
     * Raster center y
     */
    public static int originViewY;
    public static int[] hslToRgb = new int[0x10000];
    private static boolean opaque;
    private static int[] __et_r;
    public static int[] Rasterizer3D_colorPalette;
    public static int Rasterizer3D_zoom;

    static {
        __et_r = new int[512];
        __et_p = new int[2048];
        Rasterizer3D_rowOffsets = new int[1024];
        Rasterizer3D_sine = new int[2048];
        Rasterizer3D_cosine = new int[2048];
        Rasterizer3D_colorPalette = new int[65536];
        Rasterizer3D_zoom = 512;
        Rasterizer3D_alpha = 0;

        for (int i = 1; i < 512; i++) {
            __et_r[i] = 32768 / i;
        }
        for (int j = 1; j < 2048; j++) {
            __et_p[j] = 65536 / j;
        }
        for(int var0 = 0; var0 < 2048; ++var0) {
            Rasterizer3D_sine[var0] = (int)(65536.0D * Math.sin((double)var0 * 0.0030679615D));
            Rasterizer3D_cosine[var0] = (int)(65536.0D * Math.cos((double)var0 * 0.0030679615D));
        }
    }

    /**
     * Nullifies all fields in this class.
     */
    public static void clear() {
        __et_r = null;
        Rasterizer3D_sine = null;
        Rasterizer3D_cosine = null;
        scanOffsets = null;
        hslToRgb = null;
    }

    /**
     * Calculates the pixel row onsets based on the raster height and width.
     */
    public static void useViewport() {
        scanOffsets = new int[Rasterizer2D.Rasterizer2D_height];
        for (int y = 0; y < Rasterizer2D.Rasterizer2D_height; y++)
            scanOffsets[y] = Rasterizer2D.Rasterizer2D_width * y;

        originViewX = Rasterizer2D.Rasterizer2D_width / 2;
        originViewY = Rasterizer2D.Rasterizer2D_height / 2;
    }

    /**
     * Recalculates the hsl to rgb mapping and the texture palettes.
     *
     * @param brightness the new brightness value (ranging from 0.0 to 1.0)
     */
    public static void changeBrightness(double brightness) {
        int j = 0;
        for (int k = 0; k < 512; k++) {
            double d1 = (double) (k / 8) / 64D + 0.0078125D;
            double d2 = (double) (k & 7) / 8D + 0.0625D;
            for (int k1 = 0; k1 < 128; k1++) {
                double d3 = (double) k1 / 128D;
                double r = d3;
                double g = d3;
                double b = d3;
                if (d2 != 0.0D) {
                    double d7;
                    if (d3 < 0.5D)
                        d7 = d3 * (1.0D + d2);
                    else
                        d7 = (d3 + d2) - d3 * d2;
                    double d8 = 2D * d3 - d7;
                    double d9 = d1 + 0.33333333333333331D;
                    if (d9 > 1.0D)
                        d9--;
                    double d11 = d1 - 0.33333333333333331D;
                    if (d11 < 0.0D)
                        d11++;
                    if (6D * d9 < 1.0D)
                        r = d8 + (d7 - d8) * 6D * d9;
                    else if (2D * d9 < 1.0D)
                        r = d7;
                    else if (3D * d9 < 2D)
                        r = d8 + (d7 - d8) * (0.66666666666666663D - d9) * 6D;
                    else
                        r = d8;
                    if (6D * d1 < 1.0D)
                        g = d8 + (d7 - d8) * 6D * d1;
                    else if (2D * d1 < 1.0D)
                        g = d7;
                    else if (3D * d1 < 2D)
                        g = d8 + (d7 - d8) * (0.66666666666666663D - d1) * 6D;
                    else
                        g = d8;
                    if (6D * d11 < 1.0D)
                        b = d8 + (d7 - d8) * 6D * d11;
                    else if (2D * d11 < 1.0D)
                        b = d7;
                    else if (3D * d11 < 2D)
                        b = d8 + (d7 - d8) * (0.66666666666666663D - d11) * 6D;
                    else
                        b = d8;
                }
                int byteR = (int) (r * 256D);
                int byteG = (int) (g * 256D);
                int byteB = (int) (b * 256D);
                int rgb = (byteR << 16) + (byteG << 8) + byteB;
                rgb = Rasterizer3D_brighten(rgb, brightness);
                if (rgb == 0)
                    rgb = 1;
                hslToRgb[j++] = rgb;
            }
        }
    }

    public static int Rasterizer3D_brighten(int var0, double var1) {
        double var3 = (double)(var0 >> 16) / 256.0D;
        double var5 = (double)(var0 >> 8 & 255) / 256.0D;
        double var7 = (double)(var0 & 255) / 256.0D;
        var3 = Math.pow(var3, var1);
        var5 = Math.pow(var5, var1);
        var7 = Math.pow(var7, var1);
        int var9 = (int)(var3 * 256.0D);
        int var10 = (int)(var5 * 256.0D);
        int var11 = (int)(var7 * 256.0D);
        return var11 + (var10 << 8) + (var9 << 16);
    }

    public static void Rasterizer3D_setBrightness(double brightness) {
        Rasterizer3D_buildPalette(brightness);
    }

    static void Rasterizer3D_buildPalette(double brightness) {
        int var4 = 0;

        for(int var5 = 0; var5 < 512; ++var5) {
            double var6 = (double)(var5 >> 3) / 64.0D + 0.0078125D;
            double var8 = (double)(var5 & 7) / 8.0D + 0.0625D;

            for(int var10 = 0; var10 < 128; ++var10) {
                double var11 = (double)var10 / 128.0D;
                double var13 = var11;
                double var15 = var11;
                double var17 = var11;
                if(var8 != 0.0D) {
                    double var19;
                    if(var11 < 0.5D) {
                        var19 = var11 * (1.0D + var8);
                    } else {
                        var19 = var11 + var8 - var11 * var8;
                    }

                    double var21 = 2.0D * var11 - var19;
                    double var23 = var6 + 0.3333333333333333D;
                    if(var23 > 1.0D) {
                        --var23;
                    }

                    double var27 = var6 - 0.3333333333333333D;
                    if(var27 < 0.0D) {
                        ++var27;
                    }

                    if(6.0D * var23 < 1.0D) {
                        var13 = var21 + (var19 - var21) * 6.0D * var23;
                    } else if(2.0D * var23 < 1.0D) {
                        var13 = var19;
                    } else if(3.0D * var23 < 2.0D) {
                        var13 = var21 + (var19 - var21) * (0.6666666666666666D - var23) * 6.0D;
                    } else {
                        var13 = var21;
                    }

                    if(6.0D * var6 < 1.0D) {
                        var15 = var21 + (var19 - var21) * 6.0D * var6;
                    } else if(2.0D * var6 < 1.0D) {
                        var15 = var19;
                    } else if(3.0D * var6 < 2.0D) {
                        var15 = var21 + (var19 - var21) * (0.6666666666666666D - var6) * 6.0D;
                    } else {
                        var15 = var21;
                    }

                    if(6.0D * var27 < 1.0D) {
                        var17 = var21 + (var19 - var21) * 6.0D * var27;
                    } else if(2.0D * var27 < 1.0D) {
                        var17 = var19;
                    } else if(3.0D * var27 < 2.0D) {
                        var17 = var21 + (var19 - var21) * (0.6666666666666666D - var27) * 6.0D;
                    } else {
                        var17 = var21;
                    }
                }

                int var29 = (int)(var13 * 256.0D);
                int var20 = (int)(var15 * 256.0D);
                int var30 = (int)(var17 * 256.0D);
                int var22 = var30 + (var20 << 8) + (var29 << 16);
                var22 = Rasterizer3D_brighten(var22, brightness);
                if(var22 == 0) {
                    var22 = 1;
                }

                Rasterizer3D_colorPalette[var4++] = var22;
            }
        }

    }

    public static int method3039(int x, int z, int var2, int var3) {
        return x * var2 + var3 * z >> 16;
    }
    public static int method3004(int var0, int var1, int var2, int var3) {
        return var2 * var1 - var3 * var0 >> 16;
    }
    public static int method3005(int var0, int var1, int var2, int var3) {
        return var0 * var2 - var3 * var1 >> 16;
    }
    public static int method3006(int var0, int var1, int var2, int var3) {
        return var3 * var0 + var2 * var1 >> 16;
    }
    public static int method3007(int var0, int var1, int var2, int var3) {
        return var0 * var2 + var3 * var1 >> 16;
    }
    public static int method3008(int var0, int var1, int var2, int var3) {
        return var2 * var1 - var3 * var0 >> 16;
    }

    /**
     * This will not render anything.
     *
     * @param x_a x coordinate of vertex a
     * @param x_b x coordinate of vertex b
     * @param x_c x coordinate of vertex c
     * @param y_a y coordinate of vertex a
     * @param y_b y coordinate of vertex b
     * @param y_c y coordinate of vertex c
     * @param z_a z coordinate of vertex a
     * @param z_b z coordinate of vertex b
     * @param z_c z coordinate of vertex c
     */
    @Deprecated
    public static void drawDepthTriangle(int x_a, int x_b, int x_c, int y_a, int y_b, int y_c, float z_a, float z_b, float z_c) {
        if (true)
            return;
        int a_to_b = 0, b_to_c = 0, c_to_a = 0;

        if (y_b != y_a) {
            a_to_b = (x_b - x_a << 16) / (y_b - y_a);
        }
        if (y_c != y_b) {
            b_to_c = (x_c - x_b << 16) / (y_c - y_b);
        }
        if (y_c != y_a) {
            c_to_a = (x_a - x_c << 16) / (y_a - y_c);
        }

        float b_aX = x_b - x_a;
        float b_aY = y_b - y_a;
        float c_aX = x_c - x_a;
        float c_aY = y_c - y_a;
        float b_aZ = z_b - z_a;
        float c_aZ = z_c - z_a;

        float div = b_aX * c_aY - c_aX * b_aY;
        float depth_slope = (b_aZ * c_aY - c_aZ * b_aY) / div;
        float depth_increment = (c_aZ * b_aX - b_aZ * c_aX) / div;

        //B    /|
        //    / |
        //   /  |
        //  /   |
        //A ----- C
        if (y_a <= y_b && y_a <= y_c) {
            if (y_a < Rasterizer2D_yClipEnd) {
                if (y_b > Rasterizer2D_yClipEnd)
                    y_b = Rasterizer2D_yClipEnd;
                if (y_c > Rasterizer2D_yClipEnd)
                    y_c = Rasterizer2D_yClipEnd;
                z_a = z_a - depth_slope * x_a + depth_slope;
                if (y_b < y_c) {
                    x_c = x_a <<= 16;
                    if (y_a < 0) {
                        x_c -= c_to_a * y_a;
                        x_a -= a_to_b * y_a;
                        z_a -= depth_increment * y_a;
                        y_a = 0;
                    }
                    x_b <<= 16;
                    if (y_b < 0) {
                        x_b -= b_to_c * y_b;
                        y_b = 0;
                    }
                    if (y_a != y_b && c_to_a < a_to_b || y_a == y_b && c_to_a > b_to_c) {
                        y_c -= y_b;
                        y_b -= y_a;
                        y_a = Rasterizer3D.scanOffsets[y_a];
                        while (--y_b >= 0) {
                            drawDepthScanLine(y_a, x_c >> 16, x_a >> 16, z_a, depth_slope);
                            x_c += c_to_a;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D_width;
                        }
                        while (--y_c >= 0) {
                            drawDepthScanLine(y_a, x_c >> 16, x_b >> 16, z_a, depth_slope);
                            x_c += c_to_a;
                            x_b += b_to_c;
                            z_a += depth_increment;
                            y_a += Rasterizer2D_width;
                        }
                    } else {
                        y_c -= y_b;
                        y_b -= y_a;
                        y_a = Rasterizer3D.scanOffsets[y_a];
                        while (--y_b >= 0) {
                            drawDepthScanLine(y_a, x_a >> 16, x_c >> 16, z_a, depth_slope);
                            x_c += c_to_a;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D_width;
                        }
                        while (--y_c >= 0) {
                            drawDepthScanLine(y_a, x_b >> 16, x_c >> 16, z_a, depth_slope);
                            x_c += c_to_a;
                            x_b += b_to_c;
                            z_a += depth_increment;
                            y_a += Rasterizer2D_width;
                        }
                    }
                } else {
                    x_b = x_a <<= 16;
                    if (y_a < 0) {
                        x_b -= c_to_a * y_a;
                        x_a -= a_to_b * y_a;
                        z_a -= depth_increment * y_a;
                        y_a = 0;
                    }
                    x_c <<= 16;
                    if (y_c < 0) {
                        x_c -= b_to_c * y_c;
                        y_c = 0;
                    }
                    if (y_a != y_c && c_to_a < a_to_b || y_a == y_c && b_to_c > a_to_b) {
                        y_b -= y_c;
                        y_c -= y_a;
                        y_a = Rasterizer3D.scanOffsets[y_a];
                        while (--y_c >= 0) {
                            drawDepthScanLine(y_a, x_b >> 16, x_a >> 16, z_a, depth_slope);
                            x_b += c_to_a;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D_width;
                        }
                        while (--y_b >= 0) {
                            drawDepthScanLine(y_a, x_c >> 16, x_a >> 16, z_a, depth_slope);
                            x_c += b_to_c;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D_width;
                        }
                    } else {
                        y_b -= y_c;
                        y_c -= y_a;
                        y_a = Rasterizer3D.scanOffsets[y_a];
                        while (--y_c >= 0) {
                            drawDepthScanLine(y_a, x_a >> 16, x_b >> 16, z_a, depth_slope);
                            x_b += c_to_a;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D_width;
                        }
                        while (--y_b >= 0) {
                            drawDepthScanLine(y_a, x_a >> 16, x_c >> 16, z_a, depth_slope);
                            x_c += b_to_c;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D_width;
                        }
                    }
                }
            }
        } else if (y_b <= y_c) {
            //A |\
            //  | \
            //  |  \
            //  |   \
            //C ----- B
            if (y_b < Rasterizer2D_yClipEnd) {
                if (y_c > Rasterizer2D_yClipEnd)
                    y_c = Rasterizer2D_yClipEnd;
                if (y_a > Rasterizer2D_yClipEnd)
                    y_a = Rasterizer2D_yClipEnd;
                z_b = z_b - depth_slope * x_b + depth_slope;
                if (y_c < y_a) {
                    x_a = x_b <<= 16;
                    if (y_b < 0) {
                        x_a -= a_to_b * y_b;
                        x_b -= b_to_c * y_b;
                        z_b -= depth_increment * y_b;
                        y_b = 0;
                    }
                    x_c <<= 16;
                    if (y_c < 0) {
                        x_c -= c_to_a * y_c;
                        y_c = 0;
                    }
                    if (y_b != y_c && a_to_b < b_to_c || y_b == y_c && a_to_b > c_to_a) {
                        y_a -= y_c;
                        y_c -= y_b;
                        y_b = Rasterizer3D.scanOffsets[y_b];
                        while (--y_c >= 0) {
                            drawDepthScanLine(y_b, x_a >> 16, x_b >> 16, z_b, depth_slope);
                            x_a += a_to_b;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D_width;
                        }
                        while (--y_a >= 0) {
                            drawDepthScanLine(y_b, x_a >> 16, x_c >> 16, z_b, depth_slope);
                            x_a += a_to_b;
                            x_c += c_to_a;
                            z_b += depth_increment;
                            y_b += Rasterizer2D_width;
                        }
                    } else {
                        y_a -= y_c;
                        y_c -= y_b;
                        y_b = Rasterizer3D.scanOffsets[y_b];
                        while (--y_c >= 0) {
                            drawDepthScanLine(y_b, x_b >> 16, x_a >> 16, z_b, depth_slope);
                            x_a += a_to_b;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D_width;
                        }
                        while (--y_a >= 0) {
                            drawDepthScanLine(y_b, x_c >> 16, x_a >> 16, z_b, depth_slope);
                            x_a += a_to_b;
                            x_c += c_to_a;
                            z_b += depth_increment;
                            y_b += Rasterizer2D_width;
                        }
                    }
                } else {
                    x_c = x_b <<= 16;
                    if (y_b < 0) {
                        x_c -= a_to_b * y_b;
                        x_b -= b_to_c * y_b;
                        z_b -= depth_increment * y_b;
                        y_b = 0;
                    }
                    x_a <<= 16;
                    if (y_a < 0) {
                        x_a -= c_to_a * y_a;
                        y_a = 0;
                    }
                    if (a_to_b < b_to_c) {
                        y_c -= y_a;
                        y_a -= y_b;
                        y_b = Rasterizer3D.scanOffsets[y_b];
                        while (--y_a >= 0) {
                            drawDepthScanLine(y_b, x_c >> 16, x_b >> 16, z_b, depth_slope);
                            x_c += a_to_b;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D_width;
                        }
                        while (--y_c >= 0) {
                            drawDepthScanLine(y_b, x_a >> 16, x_b >> 16, z_b, depth_slope);
                            x_a += c_to_a;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D_width;
                        }
                    } else {
                        y_c -= y_a;
                        y_a -= y_b;
                        y_b = Rasterizer3D.scanOffsets[y_b];
                        while (--y_a >= 0) {
                            drawDepthScanLine(y_b, x_b >> 16, x_c >> 16, z_b, depth_slope);
                            x_c += a_to_b;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D_width;
                        }
                        while (--y_c >= 0) {
                            drawDepthScanLine(y_b, x_b >> 16, x_a >> 16, z_b, depth_slope);
                            x_a += c_to_a;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D_width;
                        }
                    }
                }
            }
        } else if (y_c < Rasterizer2D_yClipEnd) {
            if (y_a > Rasterizer2D_yClipEnd)
                y_a = Rasterizer2D_yClipEnd;
            if (y_b > Rasterizer2D_yClipEnd)
                y_b = Rasterizer2D_yClipEnd;
            z_c = z_c - depth_slope * x_c + depth_slope;
            if (y_a < y_b) {
                x_b = x_c <<= 16;
                if (y_c < 0) {
                    x_b -= b_to_c * y_c;
                    x_c -= c_to_a * y_c;
                    z_c -= depth_increment * y_c;
                    y_c = 0;
                }
                x_a <<= 16;
                if (y_a < 0) {
                    x_a -= a_to_b * y_a;
                    y_a = 0;
                }
                if (b_to_c < c_to_a) {
                    y_b -= y_a;
                    y_a -= y_c;
                    y_c = Rasterizer3D.scanOffsets[y_c];
                    while (--y_a >= 0) {
                        drawDepthScanLine(y_c, x_b >> 16, x_c >> 16, z_c, depth_slope);
                        x_b += b_to_c;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D_width;
                    }
                    while (--y_b >= 0) {
                        drawDepthScanLine(y_c, x_b >> 16, x_a >> 16, z_c, depth_slope);
                        x_b += b_to_c;
                        x_a += a_to_b;
                        z_c += depth_increment;
                        y_c += Rasterizer2D_width;
                    }
                } else {
                    y_b -= y_a;
                    y_a -= y_c;
                    y_c = Rasterizer3D.scanOffsets[y_c];
                    while (--y_a >= 0) {
                        drawDepthScanLine(y_c, x_c >> 16, x_b >> 16, z_c, depth_slope);
                        x_b += b_to_c;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D_width;
                    }
                    while (--y_b >= 0) {
                        drawDepthScanLine(y_c, x_a >> 16, x_b >> 16, z_c, depth_slope);
                        x_b += b_to_c;
                        x_a += a_to_b;
                        z_c += depth_increment;
                        y_c += Rasterizer2D_width;
                    }
                }
            } else {
                x_a = x_c <<= 16;
                if (y_c < 0) {
                    x_a -= b_to_c * y_c;
                    x_c -= c_to_a * y_c;
                    z_c -= depth_increment * y_c;
                    y_c = 0;
                }
                x_b <<= 16;
                if (y_b < 0) {
                    x_b -= a_to_b * y_b;
                    y_b = 0;
                }
                if (b_to_c < c_to_a) {
                    y_a -= y_b;
                    y_b -= y_c;
                    y_c = Rasterizer3D.scanOffsets[y_c];
                    while (--y_b >= 0) {
                        drawDepthScanLine(y_c, x_a >> 16, x_c >> 16, z_c, depth_slope);
                        x_a += b_to_c;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D_width;
                    }
                    while (--y_a >= 0) {
                        drawDepthScanLine(y_c, x_b >> 16, x_c >> 16, z_c, depth_slope);
                        x_b += a_to_b;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D_width;
                    }
                } else {
                    y_a -= y_b;
                    y_b -= y_c;
                    y_c = Rasterizer3D.scanOffsets[y_c];
                    while (--y_b >= 0) {
                        drawDepthScanLine(y_c, x_c >> 16, x_a >> 16, z_c, depth_slope);
                        x_a += b_to_c;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D_width;
                    }
                    while (--y_a >= 0) {
                        drawDepthScanLine(y_c, x_c >> 16, x_b >> 16, z_c, depth_slope);
                        x_b += a_to_b;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D_width;
                    }
                }
            }
        }
    }

    private static void drawDepthScanLine(int dest_off, int start_x, int end_x, float depth, float depth_slope) {
        int dbl = depthBuffer.length;
        if (Rasterizer3D.triangleIsOutOfBounds) {
            if (end_x > Rasterizer2D_width)
                end_x = Rasterizer2D_width;
            if (start_x < 0)
                start_x = 0;
        }
        if (start_x >= end_x)
            return;
        dest_off += start_x - 1;
        int loops = end_x - start_x >> 2;
        depth += depth_slope * (float) start_x;

        if (Rasterizer3D.alpha == 0) {
            while (--loops >= 0) {

                dest_off++;
                if (dest_off >= 0 && dest_off < dbl && true) {
                    depthBuffer[dest_off] = depth;
                }

                depth += depth_slope;
                dest_off++;
                if (dest_off >= 0 && dest_off < dbl && true) {
                    depthBuffer[dest_off] = depth;
                }
                depth += depth_slope;
                dest_off++;
                if (dest_off >= 0 && dest_off < dbl && true) {
                    depthBuffer[dest_off] = depth;
                }
                depth += depth_slope;
                dest_off++;
                if (dest_off >= 0 && dest_off < dbl && true) {
                    depthBuffer[dest_off] = depth;
                }
                depth += depth_slope;

            }
            for (loops = end_x - start_x & 3; --loops >= 0;) {
                dest_off++;
                if (dest_off >= 0 && dest_off < dbl && true) {
                    depthBuffer[dest_off] = depth;
                }
                depth += depth_slope;
            }
            return;
        }
        while (--loops >= 0) {
            dest_off++;
            dest_off++;
            dest_off++;
            dest_off++;
        }
        for (loops = end_x - start_x & 3; --loops >= 0;) {
            dest_off++;
        }
    }

    /**
     * @param x1   x coordinate of vertex a
     * @param x2   x coordinate of vertex b
     * @param x3   x coordinate of vertex c
     * @param y1   y coordinate of vertex a
     * @param y2   y coordinate of vertex b
     * @param y3   y coordinate of vertex c
     * @param z1   z coordinate of vertex a
     * @param z2   z coordinate of vertex b
     * @param z3   z coordinate of vertex c
     * @param hsl1 hsl color value at vertex a
     * @param hsl2 hsl color value at vertex b
     * @param hsl3 hsl color value at vertex c
     */
    public static void drawGouraudTriangle(int x1, int x2, int x3, int y1, int y2, int y3, float z1, float z2, float z3, int hsl1, int hsl2, int hsl3) {
        if (z1 < 0 || z2 < 0 || z3 < 0)
            return;
        int distance_v1_v2_x = 0;
        int distance_v1_v2_hsl = 0;
        if (y2 != y1) {
            distance_v1_v2_x = (x2 - x1 << 16) / (y2 - y1);
            distance_v1_v2_hsl = (hsl2 - hsl1 << 15) / (y2 - y1);
        }
        int distance_v2_v3_x = 0;
        int distance_v2_v3_hsl = 0;
        if (y3 != y2) {
            distance_v2_v3_x = (x3 - x2 << 16) / (y3 - y2);
            distance_v2_v3_hsl = (hsl3 - hsl2 << 15) / (y3 - y2);
        }
        int distance_v1_v3_x = 0;
        int distance_v1_v3_hsl = 0;
        if (y3 != y1) {
            distance_v1_v3_x = (x1 - x3 << 16) / (y1 - y3);
            distance_v1_v3_hsl = (hsl1 - hsl3 << 15) / (y1 - y3);
        }
        float dx_21 = x2 - x1;
        float dy_21 = y2 - y1;
        float dx_31 = x3 - x1;
        float dy_31 = y3 - y1;
        float dz_21 = z2 - z1;
        float dz_31 = z3 - z1;

        float div = dx_21 * dy_31 - dx_31 * dy_21;
        float depth_slope = (dz_21 * dy_31 - dz_31 * dy_21) / div;
        float depth_increment = (dz_31 * dx_21 - dz_21 * dx_31) / div;
        if (y1 <= y2 && y1 <= y3) {
            if (y1 >= Rasterizer2D.Rasterizer2D_yClipEnd)
                return;
            if (y2 > Rasterizer2D.Rasterizer2D_yClipEnd)
                y2 = Rasterizer2D.Rasterizer2D_yClipEnd;
            if (y3 > Rasterizer2D.Rasterizer2D_yClipEnd)
                y3 = Rasterizer2D.Rasterizer2D_yClipEnd;
            z1 = z1 - depth_slope * x1 + depth_slope;
            if (y2 < y3) {
                x3 = x1 <<= 16;
                hsl3 = hsl1 <<= 15;
                if (y1 < 0) {
                    x3 -= distance_v1_v3_x * y1;
                    x1 -= distance_v1_v2_x * y1;
                    hsl3 -= distance_v1_v3_hsl * y1;
                    hsl1 -= distance_v1_v2_hsl * y1;
                    z1 -= depth_increment * y1;
                    y1 = 0;
                }
                x2 <<= 16;
                hsl2 <<= 15;
                if (y2 < 0) {
                    x2 -= distance_v2_v3_x * y2;
                    hsl2 -= distance_v2_v3_hsl * y2;
                    y2 = 0;
                }
                if (y1 != y2 && distance_v1_v3_x < distance_v1_v2_x || y1 == y2 && distance_v1_v3_x > distance_v2_v3_x) {
                    y3 -= y2;
                    y2 -= y1;
                    for (y1 = scanOffsets[y1]; --y2 >= 0; y1 += Rasterizer2D.Rasterizer2D_width) {
                        drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y1, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7, z1, depth_slope);
                        x3 += distance_v1_v3_x;
                        x1 += distance_v1_v2_x;
                        hsl3 += distance_v1_v3_hsl;
                        hsl1 += distance_v1_v2_hsl;
                        z1 += depth_increment;
                    }

                    while (--y3 >= 0) {
                        drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y1, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, z1, depth_slope);
                        x3 += distance_v1_v3_x;
                        x2 += distance_v2_v3_x;
                        hsl3 += distance_v1_v3_hsl;
                        hsl2 += distance_v2_v3_hsl;
                        y1 += Rasterizer2D.Rasterizer2D_width;
                        z1 += depth_increment;
                    }
                    return;
                }
                y3 -= y2;
                y2 -= y1;
                for (y1 = scanOffsets[y1]; --y2 >= 0; y1 += Rasterizer2D.Rasterizer2D_width) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y1, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, z1, depth_slope);
                    x3 += distance_v1_v3_x;
                    x1 += distance_v1_v2_x;
                    hsl3 += distance_v1_v3_hsl;
                    hsl1 += distance_v1_v2_hsl;
                    z1 += depth_increment;
                }

                while (--y3 >= 0) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y1, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, z1, depth_slope);
                    x3 += distance_v1_v3_x;
                    x2 += distance_v2_v3_x;
                    hsl3 += distance_v1_v3_hsl;
                    hsl2 += distance_v2_v3_hsl;
                    y1 += Rasterizer2D.Rasterizer2D_width;
                    z1 += depth_increment;
                }
                return;
            }
            x2 = x1 <<= 16;
            hsl2 = hsl1 <<= 15;
            if (y1 < 0) {
                x2 -= distance_v1_v3_x * y1;
                x1 -= distance_v1_v2_x * y1;
                hsl2 -= distance_v1_v3_hsl * y1;
                hsl1 -= distance_v1_v2_hsl * y1;
                z1 -= depth_increment * y1;
                y1 = 0;
            }
            x3 <<= 16;
            hsl3 <<= 15;
            if (y3 < 0) {
                x3 -= distance_v2_v3_x * y3;
                hsl3 -= distance_v2_v3_hsl * y3;
                y3 = 0;
            }
            if (y1 != y3 && distance_v1_v3_x < distance_v1_v2_x || y1 == y3 && distance_v2_v3_x > distance_v1_v2_x) {
                y2 -= y3;
                y3 -= y1;
                for (y1 = scanOffsets[y1]; --y3 >= 0; y1 += Rasterizer2D.Rasterizer2D_width) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y1, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7, z1, depth_slope);
                    x2 += distance_v1_v3_x;
                    x1 += distance_v1_v2_x;
                    hsl2 += distance_v1_v3_hsl;
                    hsl1 += distance_v1_v2_hsl;
                    z1 += depth_increment;
                }

                while (--y2 >= 0) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y1, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7, z1, depth_slope);
                    x3 += distance_v2_v3_x;
                    x1 += distance_v1_v2_x;
                    hsl3 += distance_v2_v3_hsl;
                    hsl1 += distance_v1_v2_hsl;
                    y1 += Rasterizer2D.Rasterizer2D_width;
                    z1 += depth_increment;
                }
                return;
            }
            y2 -= y3;
            y3 -= y1;
            for (y1 = scanOffsets[y1]; --y3 >= 0; y1 += Rasterizer2D.Rasterizer2D_width) {
                drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y1, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, z1, depth_slope);
                x2 += distance_v1_v3_x;
                x1 += distance_v1_v2_x;
                hsl2 += distance_v1_v3_hsl;
                hsl1 += distance_v1_v2_hsl;
                z1 += depth_increment;
            }

            while (--y2 >= 0) {
                drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y1, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, z1, depth_slope);
                x3 += distance_v2_v3_x;
                x1 += distance_v1_v2_x;
                hsl3 += distance_v2_v3_hsl;
                hsl1 += distance_v1_v2_hsl;
                y1 += Rasterizer2D.Rasterizer2D_width;
                z1 += depth_increment;
            }
            return;
        }
        if (y2 <= y3) {
            if (y2 >= Rasterizer2D.Rasterizer2D_yClipEnd)
                return;
            if (y3 > Rasterizer2D.Rasterizer2D_yClipEnd)
                y3 = Rasterizer2D.Rasterizer2D_yClipEnd;
            if (y1 > Rasterizer2D.Rasterizer2D_yClipEnd)
                y1 = Rasterizer2D.Rasterizer2D_yClipEnd;
            z2 = z2 - depth_slope * x2 + depth_slope;
            if (y3 < y1) {
                x1 = x2 <<= 16;
                hsl1 = hsl2 <<= 15;
                if (y2 < 0) {
                    x1 -= distance_v1_v2_x * y2;
                    x2 -= distance_v2_v3_x * y2;
                    hsl1 -= distance_v1_v2_hsl * y2;
                    hsl2 -= distance_v2_v3_hsl * y2;
                    z2 -= depth_increment * y2;
                    y2 = 0;
                }
                x3 <<= 16;
                hsl3 <<= 15;
                if (y3 < 0) {
                    x3 -= distance_v1_v3_x * y3;
                    hsl3 -= distance_v1_v3_hsl * y3;
                    y3 = 0;
                }
                if (y2 != y3 && distance_v1_v2_x < distance_v2_v3_x || y2 == y3 && distance_v1_v2_x > distance_v1_v3_x) {
                    y1 -= y3;
                    y3 -= y2;
                    for (y2 = scanOffsets[y2]; --y3 >= 0; y2 += Rasterizer2D.Rasterizer2D_width) {
                        drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y2, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, z2, depth_slope);
                        x1 += distance_v1_v2_x;
                        x2 += distance_v2_v3_x;
                        hsl1 += distance_v1_v2_hsl;
                        hsl2 += distance_v2_v3_hsl;
                        z2 += depth_increment;
                    }

                    while (--y1 >= 0) {
                        drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y2, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, z2, depth_slope);
                        x1 += distance_v1_v2_x;
                        x3 += distance_v1_v3_x;
                        hsl1 += distance_v1_v2_hsl;
                        hsl3 += distance_v1_v3_hsl;
                        y2 += Rasterizer2D.Rasterizer2D_width;
                        z2 += depth_increment;
                    }
                    return;
                }
                y1 -= y3;
                y3 -= y2;
                for (y2 = scanOffsets[y2]; --y3 >= 0; y2 += Rasterizer2D.Rasterizer2D_width) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y2, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7, z2, depth_slope);
                    x1 += distance_v1_v2_x;
                    x2 += distance_v2_v3_x;
                    hsl1 += distance_v1_v2_hsl;
                    hsl2 += distance_v2_v3_hsl;
                    z2 += depth_increment;
                }

                while (--y1 >= 0) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y2, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7, z2, depth_slope);
                    x1 += distance_v1_v2_x;
                    x3 += distance_v1_v3_x;
                    hsl1 += distance_v1_v2_hsl;
                    hsl3 += distance_v1_v3_hsl;
                    y2 += Rasterizer2D.Rasterizer2D_width;
                    z2 += depth_increment;
                }
                return;
            }
            x3 = x2 <<= 16;
            hsl3 = hsl2 <<= 15;
            if (y2 < 0) {
                x3 -= distance_v1_v2_x * y2;
                x2 -= distance_v2_v3_x * y2;
                hsl3 -= distance_v1_v2_hsl * y2;
                hsl2 -= distance_v2_v3_hsl * y2;
                z2 -= depth_increment * y2;
                y2 = 0;
            }
            x1 <<= 16;
            hsl1 <<= 15;
            if (y1 < 0) {
                x1 -= distance_v1_v3_x * y1;
                hsl1 -= distance_v1_v3_hsl * y1;
                y1 = 0;
            }
            if (distance_v1_v2_x < distance_v2_v3_x) {
                y3 -= y1;
                y1 -= y2;
                for (y2 = scanOffsets[y2]; --y1 >= 0; y2 += Rasterizer2D.Rasterizer2D_width) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y2, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, z2, depth_slope);
                    x3 += distance_v1_v2_x;
                    x2 += distance_v2_v3_x;
                    hsl3 += distance_v1_v2_hsl;
                    hsl2 += distance_v2_v3_hsl;
                    z2 += depth_increment;
                }

                while (--y3 >= 0) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y2, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, z2, depth_slope);
                    x1 += distance_v1_v3_x;
                    x2 += distance_v2_v3_x;
                    hsl1 += distance_v1_v3_hsl;
                    hsl2 += distance_v2_v3_hsl;
                    y2 += Rasterizer2D.Rasterizer2D_width;
                    z2 += depth_increment;
                }
                return;
            }
            y3 -= y1;
            y1 -= y2;
            for (y2 = scanOffsets[y2]; --y1 >= 0; y2 += Rasterizer2D.Rasterizer2D_width) {
                drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y2, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, z2, depth_slope);
                x3 += distance_v1_v2_x;
                x2 += distance_v2_v3_x;
                hsl3 += distance_v1_v2_hsl;
                hsl2 += distance_v2_v3_hsl;
                z2 += depth_increment;
            }

            while (--y3 >= 0) {
                drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y2, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7, z2, depth_slope);
                x1 += distance_v1_v3_x;
                x2 += distance_v2_v3_x;
                hsl1 += distance_v1_v3_hsl;
                hsl2 += distance_v2_v3_hsl;
                y2 += Rasterizer2D.Rasterizer2D_width;
                z2 += depth_increment;
            }
            return;
        }
        if (y3 >= Rasterizer2D.Rasterizer2D_yClipEnd)
            return;
        if (y1 > Rasterizer2D.Rasterizer2D_yClipEnd)
            y1 = Rasterizer2D.Rasterizer2D_yClipEnd;
        if (y2 > Rasterizer2D.Rasterizer2D_yClipEnd)
            y2 = Rasterizer2D.Rasterizer2D_yClipEnd;
        z3 = z3 - depth_slope * x3 + depth_slope;
        if (y1 < y2) {
            x2 = x3 <<= 16;
            hsl2 = hsl3 <<= 15;
            if (y3 < 0) {
                x2 -= distance_v2_v3_x * y3;
                x3 -= distance_v1_v3_x * y3;
                hsl2 -= distance_v2_v3_hsl * y3;
                hsl3 -= distance_v1_v3_hsl * y3;
                z3 -= depth_increment * y3;
                y3 = 0;
            }
            x1 <<= 16;
            hsl1 <<= 15;
            if (y1 < 0) {
                x1 -= distance_v1_v2_x * y1;
                hsl1 -= distance_v1_v2_hsl * y1;
                y1 = 0;
            }
            if (distance_v2_v3_x < distance_v1_v3_x) {
                y2 -= y1;
                y1 -= y3;
                for (y3 = scanOffsets[y3]; --y1 >= 0; y3 += Rasterizer2D.Rasterizer2D_width) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y3, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, z3, depth_slope);
                    x2 += distance_v2_v3_x;
                    x3 += distance_v1_v3_x;
                    hsl2 += distance_v2_v3_hsl;
                    hsl3 += distance_v1_v3_hsl;
                    z3 += depth_increment;
                }

                while (--y2 >= 0) {
                    drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y3, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7, z3, depth_slope);
                    x2 += distance_v2_v3_x;
                    x1 += distance_v1_v2_x;
                    hsl2 += distance_v2_v3_hsl;
                    hsl1 += distance_v1_v2_hsl;
                    y3 += Rasterizer2D.Rasterizer2D_width;
                    z3 += depth_increment;
                }
                return;
            }
            y2 -= y1;
            y1 -= y3;
            for (y3 = scanOffsets[y3]; --y1 >= 0; y3 += Rasterizer2D.Rasterizer2D_width) {
                drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y3, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, z3, depth_slope);
                x2 += distance_v2_v3_x;
                x3 += distance_v1_v3_x;
                hsl2 += distance_v2_v3_hsl;
                hsl3 += distance_v1_v3_hsl;
                z3 += depth_increment;
            }

            while (--y2 >= 0) {
                drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y3, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, z3, depth_slope);
                x2 += distance_v2_v3_x;
                x1 += distance_v1_v2_x;
                hsl2 += distance_v2_v3_hsl;
                hsl1 += distance_v1_v2_hsl;
                y3 += Rasterizer2D.Rasterizer2D_width;
                z3 += depth_increment;
            }
            return;
        }
        x1 = x3 <<= 16;
        hsl1 = hsl3 <<= 15;
        if (y3 < 0) {
            x1 -= distance_v2_v3_x * y3;
            x3 -= distance_v1_v3_x * y3;
            hsl1 -= distance_v2_v3_hsl * y3;
            hsl3 -= distance_v1_v3_hsl * y3;
            z3 -= depth_increment * y3;
            y3 = 0;
        }
        x2 <<= 16;
        hsl2 <<= 15;
        if (y2 < 0) {
            x2 -= distance_v1_v2_x * y2;
            hsl2 -= distance_v1_v2_hsl * y2;
            y2 = 0;
        }
        if (distance_v2_v3_x < distance_v1_v3_x) {
            y1 -= y2;
            y2 -= y3;
            for (y3 = scanOffsets[y3]; --y2 >= 0; y3 += Rasterizer2D.Rasterizer2D_width) {
                drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y3, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, z3, depth_slope);
                x1 += distance_v2_v3_x;
                x3 += distance_v1_v3_x;
                hsl1 += distance_v2_v3_hsl;
                hsl3 += distance_v1_v3_hsl;
                z3 += depth_increment;
            }

            while (--y1 >= 0) {
                drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y3, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, z3, depth_slope);
                x2 += distance_v1_v2_x;
                x3 += distance_v1_v3_x;
                hsl2 += distance_v1_v2_hsl;
                hsl3 += distance_v1_v3_hsl;
                y3 += Rasterizer2D.Rasterizer2D_width;
                z3 += depth_increment;
            }
            return;
        }
        y1 -= y2;
        y2 -= y3;
        for (y3 = scanOffsets[y3]; --y2 >= 0; y3 += Rasterizer2D.Rasterizer2D_width) {
            drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y3, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7, z3, depth_slope);
            x1 += distance_v2_v3_x;
            x3 += distance_v1_v3_x;
            hsl1 += distance_v2_v3_hsl;
            hsl3 += distance_v1_v3_hsl;
            z3 += depth_increment;
        }

        while (--y1 >= 0) {
            drawGouraudScanline(Rasterizer2D.Rasterizer2D_pixels, y3, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, z3, depth_slope);
            x2 += distance_v1_v2_x;
            x3 += distance_v1_v3_x;
            hsl2 += distance_v1_v2_hsl;
            hsl3 += distance_v1_v3_hsl;
            y3 += Rasterizer2D.Rasterizer2D_width;
            z3 += depth_increment;
        }
    }

    private static void drawGouraudScanline(int[] pixelBuffer, int offset, int x1, int x2, int hsl1, int hsl2, float depth, float depth_slope) {
        int rgb;
        int k;
        if (notTextured) {
            int l1;
            if (triangleIsOutOfBounds) {
                if (x2 - x1 > 3)
                    l1 = (hsl2 - hsl1) / (x2 - x1);
                else
                    l1 = 0;
                if (x2 > Rasterizer2D.lastX)
                    x2 = Rasterizer2D.lastX;
                if (x1 < 0) {
                    hsl1 -= x1 * l1;
                    x1 = 0;
                }
                if (x1 >= x2)
                    return;
                offset += x1;
                depth += depth_slope * (float) x1;
                k = x2 - x1 >> 2;
                l1 <<= 2;
            } else {
                if (x1 >= x2)
                    return;
                offset += x1;
                depth += depth_slope * (float) x1;
                k = x2 - x1 >> 2;
                if (k > 0)
                    l1 = (hsl2 - hsl1) * __et_r[k] >> 15;
                else
                    l1 = 0;
            }
            if (alpha == 0) {
                while (--k >= 0) {
                    rgb = hslToRgb[hsl1 >> 8];
                    hsl1 += l1;
                    pixelBuffer[offset] = rgb;
                    depthBuffer[offset] = depth;
                    depth += depth_slope;
                    offset++;
                    pixelBuffer[offset] = rgb;
                    depthBuffer[offset] = depth;
                    depth += depth_slope;
                    offset++;
                    pixelBuffer[offset] = rgb;
                    depthBuffer[offset] = depth;
                    depth += depth_slope;
                    offset++;
                    pixelBuffer[offset] = rgb;
                    depthBuffer[offset] = depth;
                    depth += depth_slope;
                    offset++;
                }
                k = x2 - x1 & 3;
                if (k > 0) {
                    rgb = hslToRgb[hsl1 >> 8];
                    do {
                        pixelBuffer[offset] = rgb;
                        depthBuffer[offset] = depth;
                        depth += depth_slope;
                        offset++;
                    }
                    while (--k > 0);
                    return;
                }
            } else {
                int a1 = alpha;
                int a2 = 256 - alpha;
                while (--k >= 0) {
                    rgb = hslToRgb[hsl1 >> 8];
                    hsl1 += l1;
                    rgb = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
                    pixelBuffer[offset] = rgb + ((pixelBuffer[offset] & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((pixelBuffer[offset] & 0xff00) * a1 >> 8 & 0xff00);
                    offset++;
                    pixelBuffer[offset] = rgb + ((pixelBuffer[offset] & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((pixelBuffer[offset] & 0xff00) * a1 >> 8 & 0xff00);
                    offset++;
                    pixelBuffer[offset] = rgb + ((pixelBuffer[offset] & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((pixelBuffer[offset] & 0xff00) * a1 >> 8 & 0xff00);
                    offset++;
                    pixelBuffer[offset] = rgb + ((pixelBuffer[offset] & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((pixelBuffer[offset] & 0xff00) * a1 >> 8 & 0xff00);
                    offset++;
                }
                k = x2 - x1 & 3;
                if (k > 0) {
                    rgb = hslToRgb[hsl1 >> 8];
                    rgb = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
                    do {
                        pixelBuffer[offset] = rgb + ((pixelBuffer[offset] & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((pixelBuffer[offset] & 0xff00) * a1 >> 8 & 0xff00);
                        offset++;
                    }
                    while (--k > 0);
                }
            }
            return;
        }
        if (x1 >= x2)
            return;
        int i2 = (hsl2 - hsl1) / (x2 - x1);
        if (triangleIsOutOfBounds) {
            if (x2 > Rasterizer2D.lastX)
                x2 = Rasterizer2D.lastX;
            if (x1 < 0) {
                hsl1 -= x1 * i2;
                x1 = 0;
            }
            if (x1 >= x2)
                return;
        }
        offset += x1;
        depth += depth_slope * (float) x1;
        k = x2 - x1;
        if (alpha == 0) {
            do {
                pixelBuffer[offset] = hslToRgb[hsl1 >> 8];
                depthBuffer[offset] = depth;
                depth += depth_slope;
                offset++;
                hsl1 += i2;
            } while (--k > 0);
            return;
        }
        int a1 = alpha;
        int a2 = 256 - alpha;
        do {
            rgb = hslToRgb[hsl1 >> 8];
            hsl1 += i2;
            rgb = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
            pixelBuffer[offset] = rgb + ((pixelBuffer[offset] & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((pixelBuffer[offset] & 0xff00) * a1 >> 8 & 0xff00);
            offset++;
        } while (--k > 0);
    }

    /**
     * @param x_a x coordinate of vertex a
     * @param x_b x coordinate of vertex b
     * @param x_c x coordinate of vertex c
     * @param y_a y coordinate of vertex a
     * @param y_b y coordinate of vertex b
     * @param y_c y coordinate of vertex c
     * @param z_a z coordinate of vertex a
     * @param z_b z coordinate of vertex b
     * @param z_c z coordinate of vertex c
     * @param rgb the rgb color value of the triangle
     */
    public static void drawFlatTriangle(int x_a, int x_b, int x_c, int y_a, int y_b, int y_c, float z_a, float z_b, float z_c, int rgb) {
        if (z_a < 0 || z_b < 0 || z_c < 0) return;
        int a_to_b = 0;
        if (y_b != y_a) {
            a_to_b = (x_b - x_a << 16) / (y_b - y_a);
        }
        int b_to_c = 0;
        if (y_c != y_b) {
            b_to_c = (x_c - x_b << 16) / (y_c - y_b);
        }
        int c_to_a = 0;
        if (y_c != y_a) {
            c_to_a = (x_a - x_c << 16) / (y_a - y_c);
        }
        float b_aX = x_b - x_a;
        float b_aY = y_b - y_a;
        float c_aX = x_c - x_a;
        float c_aY = y_c - y_a;
        float b_aZ = z_b - z_a;
        float c_aZ = z_c - z_a;

        float div = b_aX * c_aY - c_aX * b_aY;
        float depth_slope = (b_aZ * c_aY - c_aZ * b_aY) / div;
        float depth_increment = (c_aZ * b_aX - b_aZ * c_aX) / div;
        if (y_a <= y_b && y_a <= y_c) {
            if (y_a >= Rasterizer2D.Rasterizer2D_yClipEnd)
                return;
            if (y_b > Rasterizer2D.Rasterizer2D_yClipEnd)
                y_b = Rasterizer2D.Rasterizer2D_yClipEnd;
            if (y_c > Rasterizer2D.Rasterizer2D_yClipEnd)
                y_c = Rasterizer2D.Rasterizer2D_yClipEnd;
            z_a = z_a - depth_slope * x_a + depth_slope;
            if (y_b < y_c) {
                x_c = x_a <<= 16;
                if (y_a < 0) {
                    x_c -= c_to_a * y_a;
                    x_a -= a_to_b * y_a;
                    z_a -= depth_increment * y_a;
                    y_a = 0;
                }
                x_b <<= 16;
                if (y_b < 0) {
                    x_b -= b_to_c * y_b;
                    y_b = 0;
                }
                if (y_a != y_b && c_to_a < a_to_b || y_a == y_b && c_to_a > b_to_c) {
                    y_c -= y_b;
                    y_b -= y_a;
                    for (y_a = scanOffsets[y_a]; --y_b >= 0; y_a += Rasterizer2D.Rasterizer2D_width) {
                        drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_a, rgb, x_c >> 16, x_a >> 16, z_a, depth_slope);
                        x_c += c_to_a;
                        x_a += a_to_b;
                        z_a += depth_increment;
                    }

                    while (--y_c >= 0) {
                        drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_a, rgb, x_c >> 16, x_b >> 16, z_a, depth_slope);
                        x_c += c_to_a;
                        x_b += b_to_c;
                        y_a += Rasterizer2D.Rasterizer2D_width;
                        z_a += depth_increment;
                    }
                    return;
                }
                y_c -= y_b;
                y_b -= y_a;
                for (y_a = scanOffsets[y_a]; --y_b >= 0; y_a += Rasterizer2D.Rasterizer2D_width) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_a, rgb, x_a >> 16, x_c >> 16, z_a, depth_slope);
                    x_c += c_to_a;
                    x_a += a_to_b;
                    z_a += depth_increment;
                }

                while (--y_c >= 0) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_a, rgb, x_b >> 16, x_c >> 16, z_a, depth_slope);
                    x_c += c_to_a;
                    x_b += b_to_c;
                    y_a += Rasterizer2D.Rasterizer2D_width;
                    z_a += depth_increment;
                }
                return;
            }
            x_b = x_a <<= 16;
            if (y_a < 0) {
                x_b -= c_to_a * y_a;
                x_a -= a_to_b * y_a;
                z_a -= depth_increment * y_a;
                y_a = 0;

            }
            x_c <<= 16;
            if (y_c < 0) {
                x_c -= b_to_c * y_c;
                y_c = 0;
            }
            if (y_a != y_c && c_to_a < a_to_b || y_a == y_c && b_to_c > a_to_b) {
                y_b -= y_c;
                y_c -= y_a;
                for (y_a = scanOffsets[y_a]; --y_c >= 0; y_a += Rasterizer2D.Rasterizer2D_width) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_a, rgb, x_b >> 16, x_a >> 16, z_a, depth_slope);
                    x_b += c_to_a;
                    x_a += a_to_b;
                    z_a += depth_increment;
                }

                while (--y_b >= 0) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_a, rgb, x_c >> 16, x_a >> 16, z_a, depth_slope);
                    x_c += b_to_c;
                    x_a += a_to_b;
                    y_a += Rasterizer2D.Rasterizer2D_width;
                    z_a += depth_increment;
                }
                return;
            }
            y_b -= y_c;
            y_c -= y_a;
            for (y_a = scanOffsets[y_a]; --y_c >= 0; y_a += Rasterizer2D.Rasterizer2D_width) {
                drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_a, rgb, x_a >> 16, x_b >> 16, z_a, depth_slope);
                x_b += c_to_a;
                x_a += a_to_b;
                z_a += depth_increment;
            }

            while (--y_b >= 0) {
                drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_a, rgb, x_a >> 16, x_c >> 16, z_a, depth_slope);
                x_c += b_to_c;
                x_a += a_to_b;
                y_a += Rasterizer2D.Rasterizer2D_width;
                z_a += depth_increment;
            }
            return;
        }
        if (y_b <= y_c) {
            if (y_b >= Rasterizer2D.Rasterizer2D_yClipEnd)
                return;
            if (y_c > Rasterizer2D.Rasterizer2D_yClipEnd)
                y_c = Rasterizer2D.Rasterizer2D_yClipEnd;
            if (y_a > Rasterizer2D.Rasterizer2D_yClipEnd)
                y_a = Rasterizer2D.Rasterizer2D_yClipEnd;
            z_b = z_b - depth_slope * x_b + depth_slope;
            if (y_c < y_a) {
                x_a = x_b <<= 16;
                if (y_b < 0) {
                    x_a -= a_to_b * y_b;
                    x_b -= b_to_c * y_b;
                    z_b -= depth_increment * y_b;
                    y_b = 0;
                }
                x_c <<= 16;
                if (y_c < 0) {
                    x_c -= c_to_a * y_c;
                    y_c = 0;
                }
                if (y_b != y_c && a_to_b < b_to_c || y_b == y_c && a_to_b > c_to_a) {
                    y_a -= y_c;
                    y_c -= y_b;
                    for (y_b = scanOffsets[y_b]; --y_c >= 0; y_b += Rasterizer2D.Rasterizer2D_width) {
                        drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_b, rgb, x_a >> 16, x_b >> 16, z_b, depth_slope);
                        x_a += a_to_b;
                        x_b += b_to_c;
                        z_b += depth_increment;
                    }

                    while (--y_a >= 0) {
                        drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_b, rgb, x_a >> 16, x_c >> 16, z_b, depth_slope);
                        x_a += a_to_b;
                        x_c += c_to_a;
                        y_b += Rasterizer2D.Rasterizer2D_width;
                        z_b += depth_increment;
                    }
                    return;
                }
                y_a -= y_c;
                y_c -= y_b;
                for (y_b = scanOffsets[y_b]; --y_c >= 0; y_b += Rasterizer2D.Rasterizer2D_width) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_b, rgb, x_b >> 16, x_a >> 16, z_b, depth_slope);
                    x_a += a_to_b;
                    x_b += b_to_c;
                    z_b += depth_increment;
                }

                while (--y_a >= 0) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_b, rgb, x_c >> 16, x_a >> 16, z_b, depth_slope);
                    x_a += a_to_b;
                    x_c += c_to_a;
                    y_b += Rasterizer2D.Rasterizer2D_width;
                    z_b += depth_increment;
                }
                return;
            }
            x_c = x_b <<= 16;
            if (y_b < 0) {
                x_c -= a_to_b * y_b;
                x_b -= b_to_c * y_b;
                z_b -= depth_increment * y_b;
                y_b = 0;
            }
            x_a <<= 16;
            if (y_a < 0) {
                x_a -= c_to_a * y_a;
                y_a = 0;
            }
            if (a_to_b < b_to_c) {
                y_c -= y_a;
                y_a -= y_b;
                for (y_b = scanOffsets[y_b]; --y_a >= 0; y_b += Rasterizer2D.Rasterizer2D_width) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_b, rgb, x_c >> 16, x_b >> 16, z_b, depth_slope);
                    x_c += a_to_b;
                    x_b += b_to_c;
                    z_b += depth_increment;
                }

                while (--y_c >= 0) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_b, rgb, x_a >> 16, x_b >> 16, z_b, depth_slope);
                    x_a += c_to_a;
                    x_b += b_to_c;
                    y_b += Rasterizer2D.Rasterizer2D_width;
                    z_b += depth_increment;
                }
                return;
            }
            y_c -= y_a;
            y_a -= y_b;
            for (y_b = scanOffsets[y_b]; --y_a >= 0; y_b += Rasterizer2D.Rasterizer2D_width) {
                drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_b, rgb, x_b >> 16, x_c >> 16, z_b, depth_slope);
                x_c += a_to_b;
                x_b += b_to_c;
                z_b += depth_increment;
            }

            while (--y_c >= 0) {
                drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_b, rgb, x_b >> 16, x_a >> 16, z_b, depth_slope);
                x_a += c_to_a;
                x_b += b_to_c;
                y_b += Rasterizer2D.Rasterizer2D_width;
                z_b += depth_increment;
            }
            return;
        }
        if (y_c >= Rasterizer2D.Rasterizer2D_yClipEnd)
            return;
        if (y_a > Rasterizer2D.Rasterizer2D_yClipEnd)
            y_a = Rasterizer2D.Rasterizer2D_yClipEnd;
        if (y_b > Rasterizer2D.Rasterizer2D_yClipEnd)
            y_b = Rasterizer2D.Rasterizer2D_yClipEnd;
        z_c = z_c - depth_slope * x_c + depth_slope;
        if (y_a < y_b) {
            x_b = x_c <<= 16;
            if (y_c < 0) {
                x_b -= b_to_c * y_c;
                x_c -= c_to_a * y_c;
                z_c -= depth_increment * y_c;
                y_c = 0;
            }
            x_a <<= 16;
            if (y_a < 0) {
                x_a -= a_to_b * y_a;
                y_a = 0;
            }
            if (b_to_c < c_to_a) {
                y_b -= y_a;
                y_a -= y_c;
                for (y_c = scanOffsets[y_c]; --y_a >= 0; y_c += Rasterizer2D.Rasterizer2D_width) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_c, rgb, x_b >> 16, x_c >> 16, z_c, depth_slope);
                    x_b += b_to_c;
                    x_c += c_to_a;
                    z_c += depth_increment;
                }

                while (--y_b >= 0) {
                    drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_c, rgb, x_b >> 16, x_a >> 16, z_c, depth_slope);
                    x_b += b_to_c;
                    x_a += a_to_b;
                    y_c += Rasterizer2D.Rasterizer2D_width;
                    z_c += depth_increment;
                }
                return;
            }
            y_b -= y_a;
            y_a -= y_c;
            for (y_c = scanOffsets[y_c]; --y_a >= 0; y_c += Rasterizer2D.Rasterizer2D_width) {
                drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_c, rgb, x_c >> 16, x_b >> 16, z_c, depth_slope);
                x_b += b_to_c;
                x_c += c_to_a;
                z_c += depth_increment;
            }

            while (--y_b >= 0) {
                drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_c, rgb, x_a >> 16, x_b >> 16, z_c, depth_slope);
                x_b += b_to_c;
                x_a += a_to_b;
                y_c += Rasterizer2D.Rasterizer2D_width;
                z_c += depth_increment;
            }
            return;
        }
        x_a = x_c <<= 16;
        if (y_c < 0) {
            x_a -= b_to_c * y_c;
            x_c -= c_to_a * y_c;
            z_c -= depth_increment * y_c;
            y_c = 0;
        }
        x_b <<= 16;
        if (y_b < 0) {
            x_b -= a_to_b * y_b;
            y_b = 0;
        }
        if (b_to_c < c_to_a) {
            y_a -= y_b;
            y_b -= y_c;
            for (y_c = scanOffsets[y_c]; --y_b >= 0; y_c += Rasterizer2D.Rasterizer2D_width) {
                drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_c, rgb, x_a >> 16, x_c >> 16, z_c, depth_slope);
                x_a += b_to_c;
                x_c += c_to_a;
                z_c += depth_increment;
            }

            while (--y_a >= 0) {
                drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_c, rgb, x_b >> 16, x_c >> 16, z_c, depth_slope);
                x_b += a_to_b;
                x_c += c_to_a;
                y_c += Rasterizer2D.Rasterizer2D_width;
                z_c += depth_increment;
            }
            return;
        }
        y_a -= y_b;
        y_b -= y_c;
        for (y_c = scanOffsets[y_c]; --y_b >= 0; y_c += Rasterizer2D.Rasterizer2D_width) {
            drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_c, rgb, x_c >> 16, x_a >> 16, z_c, depth_slope);
            x_a += b_to_c;
            x_c += c_to_a;
            z_c += depth_increment;
        }

        while (--y_a >= 0) {
            drawFlatScanline(Rasterizer2D.Rasterizer2D_pixels, y_c, rgb, x_c >> 16, x_b >> 16, z_c, depth_slope);
            x_b += a_to_b;
            x_c += c_to_a;
            y_c += Rasterizer2D.Rasterizer2D_width;
            z_c += depth_increment;
        }
    }

    private static void drawFlatScanline(int[] dest, int offset, int rgb, int x1, int x2, float depth, float depth_slope) {
        if (triangleIsOutOfBounds) {
            if (x2 > Rasterizer2D.lastX) {
                x2 = Rasterizer2D.lastX;
            }
            if (x1 < 0) {
                x1 = 0;
            }
        }
        if (x1 >= x2) {
            return;
        }
        offset += x1;
        int pos = x2 - x1 >> 2;
        depth += depth_slope * (float) x1;
        if (alpha == 0) {
            while (--pos >= 0) {
                for (int i = 0; i < 4; i++) {
                    dest[offset] = rgb;
                    depthBuffer[offset] = depth;
                    depth += depth_slope;
                    offset++;
                }
            }
            for (pos = x2 - x1 & 3; --pos >= 0; ) {
                dest[offset] = rgb;
                depthBuffer[offset] = depth;
                depth += depth_slope;
                offset++;
            }
            return;
        }
        int a1 = alpha;
        int a2 = 256 - alpha;
        rgb = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
        while (--pos >= 0) {
            for (int i = 0; i < 4; i++) {
                dest[offset] = rgb + ((dest[offset] & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dest[offset] & 0xff00) * a1 >> 8 & 0xff00);
                offset++;
            }
        }
        for (pos = x2 - x1 & 3; --pos >= 0; ) {
            dest[offset] = rgb + ((dest[offset] & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dest[offset] & 0xff00) * a1 >> 8 & 0xff00);
            offset++;
        }
    }

}
