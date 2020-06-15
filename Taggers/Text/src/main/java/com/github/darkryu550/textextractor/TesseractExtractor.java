package com.github.darkryu550.textextractor;

import net.sourceforge.tess4j.Tesseract;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TesseractExtractor implements Extractor {
	protected Tesseract tesseract;
	public TesseractExtractor() {
		this.tesseract = new Tesseract();
	}

	@Override
	public List<TextBlock> extractTextBlocks(BufferedImage image) throws Throwable {
		TextBlock b = new TextBlock();
		b.text = this.tesseract.doOCR(image);
		b.x = 0;
		b.y = 0;

		var x = new ArrayList<TextBlock>();
		x.add(b);
		return x;
	}


}
