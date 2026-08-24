package stan.qodat.scene.control.export.gif.encoder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Builds a shared RGB palette (at most 256 colors) from one or more ARGB frames.
 * Black is reserved at index 0 for GIF transparency of the snapshot background.
 */
public final class FastPalette {

    static final int MAX_COLORS = 256;
    static final int TRANSPARENT_RGB = 0x000000;
    static final int TRANSPARENT_INDEX = 0;

    final int[] rgb;
    final int paddedSize;
    final int transparentIndex;
    final boolean reduced;

    public boolean isReduced() {
        return reduced;
    }

    private FastPalette(int[] rgb, int paddedSize, int transparentIndex, boolean reduced) {
        this.rgb = rgb;
        this.paddedSize = paddedSize;
        this.transparentIndex = transparentIndex;
        this.reduced = reduced;
    }

    // TODO(perf): HashMap over every pixel. Histogram in a 32k/64k table when sampling GIF frames.
    static FastPalette fromFrames(int[][] frames, int maxColors) {
        if (maxColors < 2 || maxColors > MAX_COLORS) {
            throw new IllegalArgumentException("maxColors must be in [2, 256]");
        }
        Map<Integer, Integer> counts = new HashMap<>();
        for (int[] frame : frames) {
            for (int argb : frame) {
                int color = argb & 0xFFFFFF;
                counts.merge(color, 1, Integer::sum);
            }
        }
        counts.remove(TRANSPARENT_RGB);

        int maxOpaque = maxColors - 1;
        int[] opaque;
        boolean reduced;
        if (counts.size() <= maxOpaque) {
            opaque = new int[counts.size()];
            int i = 0;
            for (int color : counts.keySet()) {
                opaque[i++] = color;
            }
            reduced = false;
        } else {
            opaque = medianCut(counts, maxOpaque);
            reduced = true;
        }

        int unpadded = opaque.length + 1;
        int padded = Math.max(GifMath.roundUpToPowerOfTwo(unpadded), 2);
        int[] palette = new int[padded];
        palette[TRANSPARENT_INDEX] = TRANSPARENT_RGB;
        System.arraycopy(opaque, 0, palette, 1, opaque.length);
        return new FastPalette(palette, padded, TRANSPARENT_INDEX, reduced);
    }

    private static int[] medianCut(Map<Integer, Integer> counts, int maxColors) {
        int n = counts.size();
        int[] colors = new int[n];
        int[] weights = new int[n];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            colors[i] = entry.getKey();
            weights[i] = entry.getValue();
            i++;
        }

        PriorityQueue<Box> boxes = new PriorityQueue<>(Comparator.comparingDouble((Box b) -> -b.spread));
        boxes.add(new Box(colors, weights, 0, n));

        while (boxes.size() < maxColors) {
            Box widest = boxes.poll();
            if (widest == null || widest.end - widest.start < 2) {
                if (widest != null) {
                    boxes.add(widest);
                }
                break;
            }
            Box[] split = widest.split();
            boxes.add(split[0]);
            boxes.add(split[1]);
        }

        int[] centroids = new int[boxes.size()];
        int index = 0;
        for (Box box : boxes) {
            centroids[index++] = box.centroid();
        }
        return centroids;
    }

    private static final class Box {
        final int[] colors;
        final int[] weights;
        final int start;
        final int end;
        final double spread;
        final int channel;

        Box(int[] colors, int[] weights, int start, int end) {
            this.colors = colors;
            this.weights = weights;
            this.start = start;
            this.end = end;
            int minR = 255, maxR = 0, minG = 255, maxG = 0, minB = 255, maxB = 0;
            for (int i = start; i < end; i++) {
                int rgb = colors[i];
                int r = rgb >>> 16;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (r < minR) minR = r;
                if (r > maxR) maxR = r;
                if (g < minG) minG = g;
                if (g > maxG) maxG = g;
                if (b < minB) minB = b;
                if (b > maxB) maxB = b;
            }
            int spreadR = maxR - minR;
            int spreadG = maxG - minG;
            int spreadB = maxB - minB;
            if (spreadR >= spreadG && spreadR >= spreadB) {
                channel = 0;
                spread = spreadR;
            } else if (spreadG >= spreadB) {
                channel = 1;
                spread = spreadG;
            } else {
                channel = 2;
                spread = spreadB;
            }
        }

        Box[] split() {
            final int shift = channel == 0 ? 16 : channel == 1 ? 8 : 0;
            sortSpan(colors, weights, start, end, shift);
            int mid = start + (end - start) / 2;
            if (mid == start) {
                mid = start + 1;
            }
            return new Box[]{
                    new Box(colors, weights, start, mid),
                    new Box(colors, weights, mid, end)
            };
        }

        int centroid() {
            long r = 0, g = 0, b = 0, n = 0;
            for (int i = start; i < end; i++) {
                int w = weights[i];
                int rgb = colors[i];
                r += (long) (rgb >>> 16) * w;
                g += (long) ((rgb >>> 8) & 0xFF) * w;
                b += (long) (rgb & 0xFF) * w;
                n += w;
            }
            if (n == 0) {
                return colors[start];
            }
            return (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
        }
    }

    private static void sortSpan(int[] colors, int[] weights, int start, int end, int shift) {
        int length = end - start;
        Integer[] order = new Integer[length];
        for (int i = 0; i < length; i++) {
            order[i] = start + i;
        }
        Arrays.sort(order, Comparator.comparingInt(idx -> (colors[idx] >>> shift) & 0xFF));
        int[] sortedColors = new int[length];
        int[] sortedWeights = new int[length];
        for (int i = 0; i < length; i++) {
            int src = order[i];
            sortedColors[i] = colors[src];
            sortedWeights[i] = weights[src];
        }
        System.arraycopy(sortedColors, 0, colors, start, length);
        System.arraycopy(sortedWeights, 0, weights, start, length);
    }
}
