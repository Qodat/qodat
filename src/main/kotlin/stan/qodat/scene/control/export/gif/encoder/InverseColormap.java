package stan.qodat.scene.control.export.gif.encoder;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps packed 0xRRGGBB colors to the nearest palette index.
 * Exact hits (palette colors and previously seen pixels) are cached.
 * The cache is concurrent so frames can be indexed on a worker pool.
 */
final class InverseColormap {

    private final int[] palette;
    private final int opaqueStart;
    private final ConcurrentHashMap<Integer, Integer> exact = new ConcurrentHashMap<>();

    InverseColormap(int[] palette, int transparentIndex) {
        this.palette = palette;
        this.opaqueStart = transparentIndex == 0 ? 1 : 0;
        for (int i = 0; i < palette.length; i++) {
            exact.put(palette[i] & 0xFFFFFF, i);
        }
    }

    int indexOf(int rgb) {
        return exact.computeIfAbsent(rgb & 0xFFFFFF, this::nearest);
    }

    private int nearest(int rgb) {
        int r = rgb >>> 16;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        int best = opaqueStart < palette.length ? opaqueStart : 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = opaqueStart; i < palette.length; i++) {
            int dist = distanceSq(palette[i], r, g, b);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
                if (dist == 0) {
                    return i;
                }
            }
        }
        return best;
    }

    private static int distanceSq(int rgb, int r, int g, int b) {
        int dr = (rgb >>> 16) - r;
        int dg = ((rgb >>> 8) & 0xFF) - g;
        int db = (rgb & 0xFF) - b;
        return dr * dr + dg * dg + db * db;
    }
}
