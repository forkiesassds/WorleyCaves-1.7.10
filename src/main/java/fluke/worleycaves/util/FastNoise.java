// FastNoise.java
//
// MIT License
//
// Copyright(c) 2017 Jordan Peck
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// The developer's email is jorzixdan.me2@gzixmail.com (for great email, take
// off every 'zix'.)
//

package fluke.worleycaves.util;

public class FastNoise {

    private int m_seed;
    private float m_frequency = (float) 0.01;

    public FastNoise() {
        this(1337);
    }

    public FastNoise(int seed) {
        m_seed = seed;
    }

    // Sets frequency for all noise types
    // Default: 0.01
    public void setFrequency(float frequency) {
        m_frequency = frequency;
    }

    private static class Float2 {
        public final float x, y;

        public Float2(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    // Returns the seed used by this object
    public int getSeed() {
        return m_seed;
    }

    // Sets seed used for all noise types
    // Default: 1337
    public void setSeed(int seed) {
        m_seed = seed;
    }

    private static final Float2[] GRAD_2D = { new Float2(-1, -1), new Float2(1, -1), new Float2(-1, 1),
        new Float2(1, 1), new Float2(0, -1), new Float2(-1, 0), new Float2(0, 1), new Float2(1, 0), };

    public static int fastFloor(float f) {
        return (f >= 0 ? (int) f : (int) f - 1);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private static float interpQuinticFunc(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    // Hashing
    private final static int X_PRIME = 1619;
    private final static int Y_PRIME = 31337;

    private static float gradCoord2D(int seed, int x, int y, float xd, float yd) {
        int hash = seed;
        hash ^= X_PRIME * x;
        hash ^= Y_PRIME * y;

        hash = hash * hash * hash * 60493;
        hash = (hash >> 13) ^ hash;

        Float2 g = GRAD_2D[hash & 7];

        return xd * g.x + yd * g.y;
    }

    public float getNoise(float x, float y) {
        x *= m_frequency;
        y *= m_frequency;

        return singlePerlin(m_seed, x, y);
    }

    // White Noise

    private float singlePerlin(int seed, float x, float y) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float xs, ys;
        xs = interpQuinticFunc(x - x0);
        ys = interpQuinticFunc(y - y0);

        float xd0 = x - x0;
        float yd0 = y - y0;
        float xd1 = xd0 - 1;
        float yd1 = yd0 - 1;

        float xf0 = lerp(gradCoord2D(seed, x0, y0, xd0, yd0), gradCoord2D(seed, x1, y0, xd1, yd0), xs);
        float xf1 = lerp(gradCoord2D(seed, x0, y1, xd0, yd1), gradCoord2D(seed, x1, y1, xd1, yd1), xs);

        return lerp(xf0, xf1, ys);
    }
}
