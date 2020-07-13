package com.github.darkryu550.imagesearch.magnitude;

import com.github.darkryu550.imagesearch.TaggingException;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.util.ArrayList;

/** Computes the Haralick descriptors given an image and an offset pair. */
public final class Haralick {
    public static final int DEFAULT_LEVELS = 64;
    public static final int DEFAULT_DX = 1;
    public static final int DEFAULT_DY = 0;

    /* Matrix we use as the basis for our computations. */
    protected final ArrayList<Double> matrix;

    /* Number of levels of gray we're dealing with. */
    protected final int levels;

    /** Compute a new Haralick matrix based on the given image, which will first
     * be converted to a grayscale, with G = (dx, dy).
     *
     * @param image Source image for the haralick computations.
     * @param levels Number of gray levels this image will be reduced to. Keep
     *               in mind that Haralick has a worst-case spacial complexity
     *               of {@code O(levels^2)}, so be mindful of what you put here.
     *               A good value for this parameter would be {@code 64}.
     * @param dx The neighbouring pixel offset in the horizontal direction.
     * @param dy The neighbouring pixel offset in the vertical direction.
     * @throws IllegalArgumentException When {@code levels} is greater than
     * {@code 0xb504}, which is the square root of 2^31.
     */
    public Haralick(BufferedImage image, int levels, int dx, int dy) throws TaggingException {
        if (levels > 0xb504)
            throw new IllegalArgumentException("Storing the relationships " +
                "between the given amount of levels would require more than" +
                "0x7fffffff elements in the worst case space complexity");

        /* "worst-case", in this context means me rushing to get this done */
        this.levels = levels;
        this.matrix = new ArrayList<>(levels * levels);
        for (int i = 0; i < levels * levels; ++i)
            matrix.add(0.0);

        /* Convert to grayscale using a high quality pixel-based conversion. */
        var grayscale = new ColorConvertOp(
            ColorSpace.getInstance(ColorSpace.CS_GRAY),
            null
        ).filter(image, null);

        var values = grayscale.getData().getPixels(
            grayscale.getMinX(),
            grayscale.getMinY(),
            grayscale.getWidth(),
            grayscale.getHeight(),
            (double[]) null);

        /* Reduce the color space to the given amount of levels. Keep in mind
         * that when you're reducing colors for presentation, you'd ideally use
         * techniques that result in the highest level of visual fidelity like
         * dithering and gamma-correction. For this use, however, this is not
         * what we want, so we use a linear correlation and don't do any sort
         * of dithering. */
        int[][] levelData = new int[grayscale.getHeight()][grayscale.getWidth()];
        for (int i = 0; i < grayscale.getHeight(); ++i)
            for (int j = 0; j < grayscale.getWidth(); ++j) {
                assert(values.length == 1);
                var value = values[i * grayscale.getWidth() + j];
                assert(value >= 0.0);
                assert(value <= 1.0);

                var transformed = value / 255.0 * levels;
                if(transformed >= levels)
                    /* Clamp. */
                    transformed -= 1;
                if(transformed >= levels)
                    throw new TaggingException("Got level value larger than max after reduction");

                levelData[i][j] = (int) Math.floor(transformed);
            }

        /* Calculate the matrix and find its maximum value. */
        var processed = 0;
        for(int i = Math.max(0, -dy); i < Math.min(levelData.length, levelData.length - dy); ++i)
            for(int j = Math.max(0, -dx); j < Math.min(levelData[i].length, levelData[i].length - dx); ++j) {
                var a = levelData[i][j];
                var b = levelData[i + dy][j + dx];

                var c = this.matrix.get(a * levels + b);

                var iterator = this.matrix.listIterator(a * levels + b);
                iterator.next();
                iterator.set(c + 1);

                processed++;
            }

        /* Normalize over the number of processed elements so we get a
         * probability distribution. */
        for(int i = 0; i < levels * levels; ++i) {
            var c = this.matrix.get(i);

            var iterator = this.matrix.listIterator(i);
            iterator.next();
            iterator.set((double)i / (double) processed);
        }
    }

    /** Mean of the rows. */
    public double rowMean() {
        var sum = 0.0;
        for(int i = 0; i < this.levels; ++i) {
            var inner = 0.0;
            for(int j = 0; j < this.levels; ++j)
                inner += this.matrix.get(i * this.levels + j);
            sum += i * inner;
        }
        return sum;
    }

    /** Mean of the columns. */
    public double columnMean() {
        var sum = 0.0;
        for(int j = 0; j < this.levels; ++j) {
            var inner = 0.0;
            for(int i = 0; i < this.levels; ++i)
                inner += this.matrix.get(i * this.levels + j);
            sum += j * inner;
        }
        return sum;
    }

    /** Variance of the rows. */
    public double rowVariance() {
        var mr = this.rowMean();

        var sum = 0.0;
        for(int i = 0; i < this.levels; ++i) {
            var inner = 0.0;
            for(int j = 0; j < this.levels; ++j)
                inner += this.matrix.get(i * this.levels + j);

            sum += Math.pow((double)i - mr, 2) * inner;
        }
        return sum;
    }

    /** Variance of the columns. */
    public double columnVariance() {
        var mc = this.columnMean();

        var sum = 0.0;
        for(int j = 0; j < this.levels; ++j) {
            var inner = 0.0;
            for(int i = 0; i < this.levels; ++i)
                inner += this.matrix.get(i * this.levels + j);

            sum += Math.pow((double)j - mc, 2) * inner;
        }
        System.out.printf("%f\n", sum);
        return sum;
    }
}
