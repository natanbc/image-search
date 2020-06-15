package com.github.darkryu550.textextractor;

import java.awt.image.BufferedImage;
import java.util.List;

public interface Extractor {
	List<TextBlock> extractTextBlocks(BufferedImage image) throws Throwable;
}
