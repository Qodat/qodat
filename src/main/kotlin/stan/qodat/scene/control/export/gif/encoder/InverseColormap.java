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
            int prgb = palette[i];
            int dr = r - (prgb >>> 16);
            int dg = g - ((prgb >>> 8) & 0xFF);
            int db = b - (prgb & 0xFF);
            int dist = dr * dr + dg * dg + db * db;
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
}
