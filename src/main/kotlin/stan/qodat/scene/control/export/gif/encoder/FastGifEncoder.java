package stan.qodat.scene.control.export.gif.encoder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * GIF writer that indexes and LZW-compresses frames independently so they can
 * be produced on a worker pool and written in order.
 */
public final class FastGifEncoder {

    public static final class EncodedFrame {
        final int delayCentiSeconds;
        final int minimumCodeSize;
        final byte[] lzwData;

        EncodedFrame(int delayCentiSeconds, int minimumCodeSize, byte[] lzwData) {
            this.delayCentiSeconds = delayCentiSeconds;
            this.minimumCodeSize = minimumCodeSize;
            this.lzwData = lzwData;
        }
    }

    private final OutputStream outputStream;
    private final int width;
    private final int height;
    private final FastPalette palette;
    private final InverseColormap colormap;
    private final boolean dither;

    public FastGifEncoder(
            OutputStream outputStream,
            int width,
            int height,
            int loopCount,
            FastPalette palette,
            boolean dither
    ) throws IOException {
        this.outputStream = outputStream instanceof BufferedOutputStream
                ? outputStream
                : new BufferedOutputStream(outputStream, 64 * 1024);
        this.width = width;
        this.height = height;
        this.palette = palette;
        this.colormap = new InverseColormap(palette.rgb, palette.transparentIndex);
        this.dither = dither;

        HeaderBlock.write(this.outputStream);
        int tableSizeField = colorTableSizeField(palette.paddedSize);
        LogicalScreenDescriptorBlock.write(
                this.outputStream, width, height, true, 7, false, tableSizeField, palette.transparentIndex, 0);
        writeColorTable(this.outputStream, palette.rgb);
        NetscapeLoopingExtensionBlock.write(this.outputStream, loopCount);
    }

    public static FastPalette buildPalette(int[][] sampleFrames) {
        return FastPalette.fromFrames(sampleFrames, FastPalette.MAX_COLORS);
    }

    /**
     * Index and compress one ARGB frame. Thread-safe with respect to other
     * {@link #encodeFrame} calls; does not write to the output stream.
     */
    public EncodedFrame encodeFrame(int[] argb, int delayCentiSeconds) {
        if (argb.length != width * height) {
            throw new IllegalArgumentException("Frame size does not match encoder screen.");
        }
        byte[] indices = new byte[argb.length];
        if (dither) {
            floydSteinberg(argb, indices);
        } else {
            exactIndex(argb, indices);
        }
        FastLzwEncoder lzw = new FastLzwEncoder(palette.paddedSize);
        return new EncodedFrame(Math.max(delayCentiSeconds, 2), lzw.getMinimumCodeSize(), lzw.encode(indices));
    }

    public synchronized void writeFrame(EncodedFrame frame) throws IOException {
        GraphicsControlExtensionBlock.write(
                outputStream,
                DisposalMethod.DO_NOT_DISPOSE,
                false,
                true,
                frame.delayCentiSeconds,
                palette.transparentIndex);
        ImageDescriptorBlock.write(outputStream, 0, 0, width, height, false, false, false, 0);
        ImageDataBlock.write(outputStream, frame.minimumCodeSize, frame.lzwData);
    }

    public synchronized void finish() throws IOException {
        outputStream.write(0x3B);
        outputStream.flush();
    }

    private void exactIndex(int[] argb, byte[] indices) {
        for (int i = 0; i < argb.length; i++) {
            int rgb = argb[i] & 0xFFFFFF;
            if (rgb == FastPalette.TRANSPARENT_RGB) {
                indices[i] = (byte) palette.transparentIndex;
            } else {
                indices[i] = (byte) colormap.indexOf(rgb);
            }
        }
    }

    // TODO(perf): allocate 3 int[] + 1 boolean[] per frame. Reuse thread-local scratch for animation export.
    private void floydSteinberg(int[] argb, byte[] indices) {
        int pixelCount = argb.length;
        int[] red = new int[pixelCount];
        int[] green = new int[pixelCount];
        int[] blue = new int[pixelCount];
        boolean[] transparent = new boolean[pixelCount];
        for (int i = 0; i < pixelCount; i++) {
            int rgb = argb[i] & 0xFFFFFF;
            if (rgb == FastPalette.TRANSPARENT_RGB) {
                transparent[i] = true;
                indices[i] = (byte) palette.transparentIndex;
            } else {
                red[i] = rgb >>> 16;
                green[i] = (rgb >>> 8) & 0xFF;
                blue[i] = rgb & 0xFF;
            }
        }
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int i = row + x;
                if (transparent[i]) {
                    continue;
                }
                int r = clamp(red[i]);
                int g = clamp(green[i]);
                int b = clamp(blue[i]);
                int idx = colormap.indexOf((r << 16) | (g << 8) | b);
                indices[i] = (byte) idx;
                int prgb = palette.rgb[idx];
                int errR = r - (prgb >>> 16);
                int errG = g - ((prgb >>> 8) & 0xFF);
                int errB = b - (prgb & 0xFF);
                distribute(red, green, blue, transparent, x + 1, y, errR, errG, errB, 7);
                distribute(red, green, blue, transparent, x - 1, y + 1, errR, errG, errB, 3);
                distribute(red, green, blue, transparent, x, y + 1, errR, errG, errB, 5);
                distribute(red, green, blue, transparent, x + 1, y + 1, errR, errG, errB, 1);
            }
        }
    }

    private void distribute(
            int[] red, int[] green, int[] blue, boolean[] transparent,
            int x, int y, int errR, int errG, int errB, int fraction) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        int i = y * width + x;
        if (transparent[i]) {
            return;
        }
        red[i] += errR * fraction / 16;
        green[i] += errG * fraction / 16;
        blue[i] += errB * fraction / 16;
    }

    private static int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return value;
    }

    private static void writeColorTable(OutputStream out, int[] rgb) throws IOException {
        for (int color : rgb) {
            Streams.writeRgb(out, color);
        }
    }

    private static int colorTableSizeField(int actualTableSize) {
        int size = 0;
        while (1 << (size + 1) < actualTableSize) {
            ++size;
        }
        return size;
    }
}
