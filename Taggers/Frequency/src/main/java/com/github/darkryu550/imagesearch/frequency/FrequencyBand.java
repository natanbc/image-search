package com.github.darkryu550.imagesearch.frequency;

import com.github.darkryu550.imagesearch.Tagger;
import com.github.darkryu550.imagesearch.TaggingException;
import org.jtransforms.fft.DoubleFFT_2D;

import javax.swing.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.sql.SQLType;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.IntStream;

public class FrequencyBand implements Tagger {
    @Override
    public Optional<Object> tag(BufferedImage image) {
        /* Convert to grayscale using a high quality pixel-based conversion. */
        var grayscale = new ColorConvertOp(
            ColorSpace.getInstance(ColorSpace.CS_GRAY),
            null
        ).filter(image, null);

        /* Collect it into the correct format. */
        double[][] buffer = new double[grayscale.getHeight()][];
        IntStream.range(0, grayscale.getHeight())
            .forEach((index) -> {
                double[] row = new double[grayscale.getWidth() * 2];
                row = grayscale.getData().getPixels(0, index, grayscale.getWidth(), 1, row);

                buffer[index] = row;
            });

        var fft = new DoubleFFT_2D(image.getHeight(), image.getWidth());
        if(Integer.bitCount(image.getHeight()) != 1 || Integer.bitCount(image.getWidth()) != 1)
            fft.realForwardFull(buffer);
        else
            fft.realForward(buffer);

        double[][] magnitude = Utils.magnitude(buffer);

        /* O(n^2) implementation. Not as bad as the O(n^3) it could be, bad nonetheless. */
        int bandCount = Math.min(grayscale.getHeight() / 2, grayscale.getWidth() / 2);
        int centerI = grayscale.getHeight() / 2;
        int centerJ = grayscale.getWidth()  / 2;

        double[] bands = new double[bandCount];
        for(int i = 0; i < bandCount; ++i) {
            for(int j = i - 1; j > -i; --j)
                bands[i] += magnitude[centerI + j][centerJ + i];
            for(int j = i; j > -i; --j)
                bands[i] += magnitude[centerI - i][centerJ + j];
            for(int j = i - 1; j > -i; --j)
                bands[i] += magnitude[centerI + j][centerJ - i];
            for(int j = i; j > -i; --j)
                bands[i] += magnitude[centerI + i][centerJ + j];
        }

        int    band0 = 1, band1 = 2;
        double max   = 0;
        int    mband0 = 1, mband1 = 2;
        double total = 0;
        for(; band0 < bandCount; ++band0) {
            total += bands[band0];
            if(band0 % 2 == 1){
                for(; band1 > band0; --band1) {
                    total -= bands[band1];
                    if(total / area(band0, band1) > max) {
                        max = total;
                        mband0 = band0;
                        mband1 = band1;
                    }
                }
            } else {
                for(band1 = band0 + 1; band1 < bandCount; ++band1) {
                    total += bands[band1];
                    if(total / area(band0, band1) > max) {
                        max = total;
                        mband0 = band0;
                        mband1 = band1;
                    }
                }
                --band1;
            }
            total -= bands[band0];
        }

        ArrayList<Integer> ints = new ArrayList<>();
        ints.add(mband0);
        ints.add(mband1);

        return Optional.of(ints);
    }

    private static double area(double a, double b) {
        assert(a < b);
        return Math.PI * (b * b - a * a);
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
