package stan.qodat.scene.control.export.gif.encoder;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * GIF LZW compressor. Uses an open-addressed prefix table instead of
 * {@code Map<List<Integer>, Integer>} keys.
 */
final class FastLzwEncoder {

    private static final int MAX_CODE_TABLE_SIZE = 1 << 12;
    private static final int HASH_SIZE = 5003;

    private final int minimumCodeSize;
    private final int clearCode;
    private final int eoiCode;

    FastLzwEncoder(int colorTableSize) {
        if (!GifMath.isPowerOfTwo(colorTableSize)) {
            throw new IllegalArgumentException("Color table size must be a power of 2");
        }
        int size = 2;
        while (colorTableSize > 1 << size) {
            ++size;
        }
        this.minimumCodeSize = size;
        this.clearCode = 1 << minimumCodeSize;
        this.eoiCode = clearCode + 1;
    }

    int getMinimumCodeSize() {
        return minimumCodeSize;
    }

    byte[] encode(byte[] indices) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(indices.length);
        CodeTables tables = new CodeTables();

        BitSink sink = new BitSink(out);
        int codeSize = minimumCodeSize + 1;
        int nextCode = eoiCode + 1;

        sink.write(clearCode, codeSize);

        int prefix = indices[0] & 0xFF;
        for (int i = 1; i < indices.length; i++) {
            int suffix = indices[i] & 0xFF;
            int existing = tables.find(prefix, suffix);
            if (existing >= 0) {
                prefix = existing;
                continue;
            }
            sink.write(prefix, codeSize);
            if (nextCode < MAX_CODE_TABLE_SIZE) {
                tables.put(prefix, suffix, nextCode);
                if (nextCode == (1 << codeSize) && codeSize < 12) {
                    codeSize++;
                }
                nextCode++;
            } else {
                sink.write(clearCode, codeSize);
                tables.reset();
                nextCode = eoiCode + 1;
                codeSize = minimumCodeSize + 1;
            }
            prefix = suffix;
        }
        sink.write(prefix, codeSize);
        sink.write(eoiCode, codeSize);
        sink.flush();
        return out.toByteArray();
    }

    private static final class CodeTables {
        final int[] htab = new int[HASH_SIZE];
        final int[] codetab = new int[HASH_SIZE];

        CodeTables() {
            reset();
        }

        void reset() {
            Arrays.fill(htab, -1);
        }

        int find(int prefix, int suffix) {
            int key = (prefix << 8) | suffix;
            int hash = hash(prefix, suffix);
            while (htab[hash] != -1) {
                if (htab[hash] == key) {
                    return codetab[hash];
                }
                hash++;
                if (hash >= HASH_SIZE) {
                    hash = 0;
                }
            }
            return -1;
        }

        void put(int prefix, int suffix, int code) {
            int hash = hash(prefix, suffix);
            while (htab[hash] != -1) {
                hash++;
                if (hash >= HASH_SIZE) {
                    hash = 0;
                }
            }
            htab[hash] = (prefix << 8) | suffix;
            codetab[hash] = code;
        }

        private static int hash(int prefix, int suffix) {
            int hash = ((suffix << 4) ^ prefix) % HASH_SIZE;
            return hash < 0 ? hash + HASH_SIZE : hash;
        }
    }

    private static final class BitSink {
        private final ByteArrayOutputStream out;
        private long bitBuffer;
        private int bitCount;

        BitSink(ByteArrayOutputStream out) {
            this.out = out;
        }

        void write(int code, int codeSize) {
            bitBuffer |= ((long) code) << bitCount;
            bitCount += codeSize;
            while (bitCount >= 8) {
                out.write((int) (bitBuffer & 0xFF));
                bitBuffer >>>= 8;
                bitCount -= 8;
            }
        }

        void flush() {
            if (bitCount > 0) {
                out.write((int) (bitBuffer & 0xFF));
                bitBuffer = 0;
                bitCount = 0;
            }
        }
    }
}
