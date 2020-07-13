package com.github.darkryu550.imagesearch.magnitude;

import com.github.darkryu550.imagesearch.Tagger;
import com.github.darkryu550.imagesearch.TaggingException;

import java.awt.image.BufferedImage;
import java.sql.SQLType;
import java.util.Optional;

/** Find the Haralick maximum probability for an image. */
public class HaralickCorrelation implements Tagger {
    @Override
    public Optional<Object> tag(BufferedImage image) throws TaggingException {
        var haralick = new Haralick(
            image,
            Haralick.DEFAULT_LEVELS,
            Haralick.DEFAULT_DX,
            Haralick.DEFAULT_DY);

        double mr = haralick.rowMean();
        double mc = haralick.columnMean();
        double sr = haralick.rowVariance();
        double sc = haralick.columnVariance();
        double mm = mr * mc;

        if(sc == 0 || sr == 0)
            /* Bail out of values that will result in division by zero. */
            return Optional.empty();

        var sum = 0.0;
        for(int i = 0; i < haralick.levels; ++i)
            for(int j = 0; j < haralick.levels; ++j)
                sum += i * j * haralick.matrix.get(i * haralick.levels + j) - mm;

        sum /= sr * sc;

        return Optional.of(sum);
    }

    @Override
    public SQLType getType() {
        return new SQLType() {
            @Override
            public String getName() {
                return "REAL";
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
