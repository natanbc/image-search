package com.github.darkryu550.imagesearch.magnitude;

import com.github.darkryu550.imagesearch.Tagger;
import com.github.darkryu550.imagesearch.TaggingException;

import java.awt.image.BufferedImage;
import java.sql.SQLType;
import java.util.Optional;

/** Find the Haralick maximum probability for an image. */
public class HaralickEntropy implements Tagger {
    @Override
    public Optional<Object> tag(BufferedImage image) throws TaggingException {
        var haralick = new Haralick(
            image,
            Haralick.DEFAULT_LEVELS,
            Haralick.DEFAULT_DX,
            Haralick.DEFAULT_DY);

        var sum = 0.0;
        for(int i = 0; i < haralick.levels; ++i)
            for(int j = 0; j < haralick.levels; ++j) {
                var p = haralick.matrix.get(i * haralick.levels + j);
                if(p == 0) continue;

                sum += p * Math.log(p) / Math.log(2);
            }

        return Optional.of(-sum);
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