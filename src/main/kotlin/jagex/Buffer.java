package jagex;

public class Buffer  {
    public byte[] array;
    public int offset;

    public Buffer(byte[] var1) {
        this.array = var1; // L: 61
        this.offset = 0; // L: 62
    } // L: 63


    public byte readByte() {
        return array[offset++];
    }

    public int readUnsignedByte() {
        return readByte() & 255; // L: 242
    }

    public int readShort() {
        this.offset += 2; // L: 255
        int var1 = (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 256
        if (var1 > 32767) { // L: 257
            var1 -= 65536;
        }
        return var1; // L: 258
    }

    public int readUnsignedShort() {
        this.offset += 2; // L: 250
        return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 251
    }

    public int readShortSmart() {
        int var1 = this.array[this.offset] & 255; // L: 369
        return var1 < 128 ? this.readUnsignedByte() - 64 : this.readUnsignedShort() - 49152; // L: 370 371
    }

    public int readInt() {
        this.offset += 4; // L: 267
        return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 24); // L: 268
    }

    public float readFloat() {
        return Float.intBitsToFloat(this.readInt()); // L: 278
    }

    public float readIntAsFloat() {
        return Float.intBitsToFloat(this.readInt());
    }
}
