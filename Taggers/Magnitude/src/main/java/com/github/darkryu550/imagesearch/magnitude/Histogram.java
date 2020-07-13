package com.github.darkryu550.imagesearch.magnitude;

import com.github.darkryu550.imagesearch.Tagger;
import com.github.darkryu550.imagesearch.TaggingException;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.sql.SQLType;
import java.util.ArrayList;
import java.util.Optional;


public class Histogram implements Tagger {
    public static final int BINS = 256;

    @Override
    public Optional<Object> tag(BufferedImage image) throws TaggingException {
        ArrayList<Long> histogram = new ArrayList<>(Histogram.BINS);
        for(int i = 0; i < Histogram.BINS; ++i)
            histogram.add(0L);

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

        /* Collect the values into the histogram. */
        for(int i = 0; i < grayscale.getHeight(); ++i) {
            for (int j = 0; j < grayscale.getWidth(); ++j) {
                var value = values[i * grayscale.getWidth() + j];
                assert (value >= 0.0);
                assert (value <= 1.0);

                var bucket = (int) Math.floor(value);
                if (bucket < 0)
                    throw new TaggingException("Image pixel value is below 0: " + bucket);
                else if (bucket > 255)
                    throw new TaggingException("Image pixel value is over 255: " + bucket);

                var curr = histogram.listIterator(bucket);
                curr.next();
                curr.set(histogram.get(bucket) + 1);
            }
        }

        return Optional.of(histogram);
    }

    @Override
    public SQLType getType() {
        return new SQLType() {
            @Override
            public String getName() {
                return "ARRAY[NUMBER]";
            }

            @Override
            public String getVendor() {
                return null;
            }

            @Override
            public Integer getVendorTypeNumber() {
                return null;
            }
        };
    }
}
