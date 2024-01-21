package fluke.worleycaves.util;

import java.util.BitSet;

public class BitArray2D {

    private final BitSet array;
    public final int width, height;

    public BitArray2D(int width, int height) {
        this(null, width, height);
    }

    public BitArray2D(BitSet array, int width, int height) {
        if (array == null) {
            array = new BitSet(width * height);
        } else if (array.length() < width * height) {
            throw new IllegalArgumentException("Array is too small!");
        }

        this.array = array;
        this.width = width;
        this.height = height;
    }

    public boolean get(int x, int y) {
        return this.array.get(x * this.height + y);
    }

    public void set(int x, int y, boolean value) {
        this.array.set(x * this.height + y, value);
    }

    public BitSet getArray() {
        return array;
    }
}
