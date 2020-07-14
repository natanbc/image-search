package com.github.darkryu550.imagesearch.magnitude;

import com.github.darkryu550.imagesearch.Tagger;
import com.github.darkryu550.imagesearch.TaggingException;

import java.awt.image.BufferedImage;
import java.sql.SQLType;
import java.util.Optional;

/** Big Haralick energy. */
public class HaralickEnergy implements Tagger {
    @Override
    public Optional<Object> tag(BufferedImage image) throws TaggingException {
        var haralick = new Haralick(
            image,
            Haralick.DEFAULT_LEVELS,
            Haralick.DEFAULT_DX,
            Haralick.DEFAULT_DY);

        var sum = 0.0;
        for(int i = 0; i < haralick.levels; ++i)
            for(int j = 0; j < haralick.levels; ++j)
                sum += Math.pow(haralick.matrix.get(i * haralick.levels + j), 2);

        return Optional.of(sum);
    }

    @Override
    public Optional<Double> getTagDistance(Object a, Object b) {
        try {
            Double l = (Double) a;
            Double r = (Double) b;

            return Optional.of(l - r);
        } catch(ClassCastException e) {
            throw new IllegalArgumentException("Invalid argument has been passed", e);
        }
    }

    @Override
    public Object getTagFromString(String value) {
        try {
            return Double.parseDouble(value);
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Expected a real number literal", e);
        }
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
