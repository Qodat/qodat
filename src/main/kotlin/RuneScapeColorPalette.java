public class RuneScapeColorPalette {

    public static int[] colorPalette = new int[65536];

    static {
        method2093(0.8, 0, 512);
    }

    static final void method2093(double brightness, int var2, int var3) {
        int var4 = var2 * 128;

        for(int var5 = var2; var5 < var3; ++var5) {
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
                var22 = method2151(var22, brightness);
                if(var22 == 0) {
                    var22 = 1;
                }

                colorPalette[var4++] = var22;
            }
        }
    }

    static int method2151(int var0, double var1) {
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
}
