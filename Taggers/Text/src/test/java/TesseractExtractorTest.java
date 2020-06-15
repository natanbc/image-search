import com.github.darkryu550.textextractor.TesseractExtractor;
import com.github.darkryu550.textextractor.TextBlock;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;

import java.io.File;

public class TesseractExtractorTest {
	@Test
	public void helloWorld() throws Throwable {
		var ex = new TesseractExtractor();
		var file = getClass().getClassLoader().getResource("hello.png");
		Assert.assertNotNull(file);

		var blk = ex.extractTextBlocks(ImageIO.read(new File(file.toURI()))).toArray(new TextBlock[]{});
		var txt = blk[0].getText();

		Assert.assertEquals("Hello, world!_\n", txt);
	}
}