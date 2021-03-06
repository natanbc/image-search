package com.github.darkryu550.imagesearch.magnitude;

import com.github.darkryu550.imagesearch.Tagger;
import com.github.darkryu550.imagesearch.TaggingException;

import java.awt.image.BufferedImage;
import java.sql.SQLType;
import java.util.Optional;

/** Find the Haralick maximum probability for an image. */
public class HaralickMaximumProbability implements Tagger {
    @Override
    public Optional<Object> tag(BufferedImage image) throws TaggingException {
        var haralick = new Haralick(
            image,
            Haralick.DEFAULT_LEVELS,
            Haralick.DEFAULT_DX,
            Haralick.DEFAULT_DY);

        Double max = null;
        for(int i = 0; i < haralick.matrix.size(); ++i) {
            var c = haralick.matrix.get(i);
            if(max == null || c > max) max = c;
        }

        return Optional.ofNullable(max);
    }

    @Override
    public Optional<Double> getTagDistance(Object a, Object b) {
        if(a == null || b == null)
            return Optional.empty();

        try {
            Double l = (Double) a;
            Double r = (Double) b;

            return Optional.of(Math.abs(l - r));
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
