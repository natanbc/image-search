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
