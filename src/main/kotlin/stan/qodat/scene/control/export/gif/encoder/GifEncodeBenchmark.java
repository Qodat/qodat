package stan.qodat.scene.control.export.gif.encoder;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Repeatable encode-path benchmark: legacy per-frame Color[][] + median-cut + Floyd–Steinberg
 * versus the shared-palette FastGifEncoder path used by animation GIF export.
 */
public final class GifEncodeBenchmark {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 360;
    private static final int FRAMES = 40;
    private static final int DELAY_CS = 5;
    private static final int WARMUP = 1;
    private static final int ITERATIONS = 3;

    public static void main(String[] args) throws Exception {
        System.out.println("GIF encode benchmark: " + FRAMES + " frames of " + WIDTH + "x" + HEIGHT);
        runScenario("low-color (fits in 256, typical RS model)", generateLowColorFrames(), FRAMES);
        runScenario("high-color (forces quantization / dither)", generateHighColorFrames(), FRAMES);
    }

    private static void runScenario(String name, int[][] allFrames, int frameCount) throws Exception {
        int[][] frames = Arrays.copyOf(allFrames, frameCount);
        System.out.println();
        System.out.println("=== " + name + " (" + frameCount + " frames) ===");
        byte[] legacyGif = encodeLegacy(frames);
        byte[] fastGif = encodeFast(frames, true);
        assertGif(legacyGif, "legacy");
        assertGif(fastGif, "fast");
        int iterations = frameCount < FRAMES ? 1 : ITERATIONS;
        int warmup = frameCount < FRAMES ? 0 : WARMUP;
        long legacy = median(time(iterations, warmup, () -> encodeLegacy(frames)));
        long fastSeq = median(time(iterations, warmup, () -> encodeFast(frames, false)));
        long fastPar = median(time(iterations, warmup, () -> encodeFast(frames, true)));
        System.out.printf("legacy GifEncoder (per-frame quantize): %d ms  (%d bytes)%n", legacy, legacyGif.length);
        System.out.printf("FastGifEncoder sequential:              %d ms%n", fastSeq);
        System.out.printf("FastGifEncoder parallel:                %d ms  (%d bytes)%n", fastPar, fastGif.length);
        if (legacy > 0) {
            System.out.printf("speedup vs legacy (parallel):           %.2fx%n", legacy / (double) Math.max(fastPar, 1));
        }
    }

    private static byte[] encodeLegacy(int[][] frames) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GifEncoder encoder = new GifEncoder(out, WIDTH, HEIGHT, 0);
        ImageOptions options = new ImageOptions();
        options.setDelay(DELAY_CS * 10L, TimeUnit.MILLISECONDS);
        options.setTransparencyColor(Color.BLACK.getRgbInt());
        options.setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE);
        for (int[] frame : frames) {
            encoder.addImage(frame, WIDTH, options);
        }
        encoder.finishEncoding();
        return out.toByteArray();
    }

    private static byte[] encodeFast(int[][] frames, boolean parallel) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FastPalette palette = FastGifEncoder.buildPalette(frames);
        FastGifEncoder encoder = new FastGifEncoder(out, WIDTH, HEIGHT, 0, palette, palette.isReduced());
        FastGifEncoder.EncodedFrame[] encoded = new FastGifEncoder.EncodedFrame[frames.length];
        if (parallel) {
            int workers = Runtime.getRuntime().availableProcessors();
            ExecutorService pool = Executors.newFixedThreadPool(workers);
            for (int i = 0; i < frames.length; i++) {
                final int index = i;
                pool.execute(() -> encoded[index] = encoder.encodeFrame(frames[index], DELAY_CS));
            }
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.MINUTES)) {
                pool.shutdownNow();
                throw new IllegalStateException("parallel encode timed out");
            }
        } else {
            for (int i = 0; i < frames.length; i++) {
                encoded[i] = encoder.encodeFrame(frames[i], DELAY_CS);
            }
        }
        for (FastGifEncoder.EncodedFrame frame : encoded) {
            encoder.writeFrame(frame);
        }
        encoder.finish();
        return out.toByteArray();
    }

    private static int[][] generateLowColorFrames() {
        int[][] frames = new int[FRAMES][WIDTH * HEIGHT];
        int[] modelColors = new int[80];
        for (int i = 0; i < modelColors.length; i++) {
            modelColors[i] = hslLike(i * 3, 8, 12 + (i % 20));
        }
        for (int f = 0; f < FRAMES; f++) {
            int[] pixels = frames[f];
            Arrays.fill(pixels, 0xFF000000);
            int offset = f * 3;
            for (int y = 80; y < 280; y++) {
                for (int x = 140; x < 340; x++) {
                    int color = modelColors[Math.floorMod((x / 8) + (y / 10) + offset, modelColors.length)];
                    pixels[y * WIDTH + x] = 0xFF000000 | color;
                }
            }
        }
        return frames;
    }

    private static int[][] generateHighColorFrames() {
        int[][] frames = new int[FRAMES][WIDTH * HEIGHT];
        for (int f = 0; f < FRAMES; f++) {
            int[] pixels = frames[f];
            Arrays.fill(pixels, 0xFF000000);
            for (int y = 40; y < 320; y++) {
                for (int x = 60; x < 420; x++) {
                    int r = (x + f * 5) & 0xF0;
                    int g = (y + f * 3) & 0xF0;
                    int b = ((x * y / 64) + f * 7) & 0xF0;
                    pixels[y * WIDTH + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
        return frames;
    }

    private static int hslLike(int hue, int sat, int light) {
        int r = (hue * 3 + light * 8) & 0xFF;
        int g = (sat * 12 + light * 6) & 0xFF;
        int b = (hue * 5 + sat * 4) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    private static long[] time(int iterations, int warmup, ThrowingRunnable action) throws Exception {
        for (int i = 0; i < warmup; i++) {
            action.run();
        }
        long[] samples = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            action.run();
            samples[i] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        }
        return samples;
    }

    private static long median(long[] samples) {
        long[] copy = samples.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static void assertGif(byte[] data, String label) {
        if (data.length < 16) {
            throw new IllegalStateException(label + " GIF is too small: " + data.length);
        }
        String header = new String(data, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"GIF89a".equals(header)) {
            throw new IllegalStateException(label + " GIF missing GIF89a header: " + header);
        }
        if (data[data.length - 1] != 0x3B) {
            throw new IllegalStateException(label + " GIF missing trailer");
        }
        String ascii = new String(data, java.nio.charset.StandardCharsets.ISO_8859_1);
        if (!ascii.contains("NETSCAPE2.0")) {
            throw new IllegalStateException(label + " GIF missing Netscape looping extension");
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
