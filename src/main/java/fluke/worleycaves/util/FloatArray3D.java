package fluke.worleycaves.util;

public class FloatArray3D {

    private final float[] array;
    public final int width, length, height;

    public FloatArray3D(int width, int height, int length) {
        this(null, width, height, length);
    }

    public FloatArray3D(float[] array, int width, int height, int length) {
        if (array == null) {
            array = new float[width * height * length];
        } else if (array.length < width * height * length) {
            throw new IllegalArgumentException("Array is too small!");
        }

        this.array = array;
        this.width = width;
        this.height = height;
        this.length = length;
    }

    public float get(int x, int y, int z) {
        return this.array[(x * this.length + z) * this.height + y];
    }

    public void set(int x, int y, int z, float value) {
        this.array[(x * this.length + z) * this.height + y] = value;
    }

    public float[] getArray() {
        return array;
    }
}
