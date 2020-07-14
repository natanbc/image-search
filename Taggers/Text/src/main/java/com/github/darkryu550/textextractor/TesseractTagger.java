package com.github.darkryu550.textextractor;

import com.github.darkryu550.imagesearch.Tagger;
import com.github.darkryu550.imagesearch.TaggingException;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.image.BufferedImage;
import java.sql.SQLType;
import java.util.Optional;

public class TesseractTagger implements Tagger {
    @Override
    public Optional<Object> tag(BufferedImage image) throws TaggingException {
        var tesseract = new Tesseract();
        try {
            return Optional.ofNullable(tesseract.doOCR(image));
        } catch (TesseractException e) {
            throw new TaggingException("Could not perform OCR extraction", e);
        } catch(Error e) {
            /* This is generally not good practice, but Tess4j offers us no
             * cleaner way of catching the case in which the trained data
             * files aren't present. */
            return Optional.empty();
        }
    }

    @Override
    public Object getTagFromString(String value) {
        /* Ha ha, you thought this was gonna have more to it,
         * BUT IT'S ALREADY A STRING! */
        return value;
    }

    @Override
    public Optional<Double> getTagDistance(Object a, Object b) {
        if(a == null || b == null)
            return Optional.empty();

        try {
            String l = (String) a;
            String r = (String) b;
            var size = Math.min(l.length(), r.length());

            /* Calculate a Levenshtein distance between our strings. */
            int levenshtein = Math.max(l.length(), r.length()) - size;

            for(int i = 0; i < size; ++i)
                if(l.charAt(i) != r.charAt(i))
                    ++levenshtein;

            return Optional.of((double) levenshtein);
        } catch(ClassCastException e) {
            throw new IllegalArgumentException("Invalid argument has been passed", e);
        }
    }

    @Override
    public SQLType getType() {
        return new SQLType() {
            @Override
            public String getName() {
                return "STRING";
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
