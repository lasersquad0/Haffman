import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(value = Parameterized.class)
public class CompressorTest {

	private final String filename;
	private final int filesize;
	private final int filecsizeRange;
	private final int filecsizeARange32;
	private final int filecsizeARange64;
	private final int filecsizeRange32;
	private final int filecsizeRange64;
	private final int filecsizeRLE;
	private final int filecsizeAHUF;
	private final int filecsizeHUF;
	private final int filecsizeBLOCKAHUF;
	private final int filecsizeBLOCKRLE;

	public CompressorTest(String file, int fsize, int fcsizeR, int fcsizeAR32, int fcsizeAR64, int fcsizeR32, int fcsizeR64, int fcsizeRLE, int fcsizeAHUF, int fcsizeHUF, int fcsizeBLOCKAHUF, int fcsizeBLOCKRLE) {
		filename = file;
		filesize = fsize;
		filecsizeRange = fcsizeR;
		filecsizeARange32 = fcsizeAR32;
		filecsizeARange64 = fcsizeAR64;
		filecsizeRange32 = fcsizeR32;
		filecsizeRange64 = fcsizeR64;
		filecsizeRLE = fcsizeRLE;
		filecsizeAHUF = fcsizeAHUF;
		filecsizeHUF = fcsizeHUF;
		filecsizeBLOCKAHUF = fcsizeBLOCKAHUF;
		filecsizeBLOCKRLE = fcsizeBLOCKRLE;
	}

	@Parameterized.Parameters
	public static List<Object[]> data2()
	{
		Object[][] data = new Object[][]{{"src/tests/testdata/dna-tiny.txt", 41, 37,39,42,15,18,42,18,12,30,35}, {"src/tests/testdata/dna-small.txt", 247_911, 61_216,61479,61420,60948,60856,244385,67983,67963,68492,241388},
				{"src/tests/testdata/dna-5KB.txt", 5002, 1394,1679,1681,1211,1214,4770,1363,1354,2024,4798},	{"src/tests/testdata/dna-10KB.txt", 10_002, 2568,2921,2924,2355,2356,9212,2607,2596,3790,9428},
				{"src/tests/testdata/dna-50KB.txt", 50_002, 12304,12308,12304,12032,12018,46878,13513,13488,18649,47736}, {"src/tests/testdata/dna-100KB.txt", 100_002, 24328,24907,24903,24041,24012,93874,27042,27019,27333,95545},
				{"src/tests/testdata/dna-500KB.txt", 500_002, 121122,121514,121122,120927,120726,470666,135835,135824,136507,476092}, {"src/tests/testdata/dna-1MB.txt", 1_000_002, 247200,248075,247200,247158,246777,961199,274651,274641,273713,948913},
			//	{"src/tests/testdata/dna-2MB.txt", 2_000_002, 491459,493510,491459,491794,491012,1912643,548240,548213,548458,11}, {"src/tests/testdata/dna-5MB.txt", 5_000_002, 1221197,1225860,1221197,1221971,1220683,4752541,1366081,1366075,1366620,11},
				{"src/tests/testdata/war.txt", 1_543_417, 958972,954201,958972,959177,958638,1555443,965297,965070,532328,1056730},{"src/tests/testdata/oneletter.txt", 7381, 207,204,207,4,7,116,924,923,925,118},
				{"src/tests/testdata/oneletterHUGE.txt", 250_954, 369,514,369,4,7,3892,31371,31370,925,11},
		};
		return Arrays.asList(data);
	}


	@Test
	public void compressRange() throws IOException
	{
		System.out.println(filename);

		var f = new File(filename);
		var sin = new FileInputStream(f);
		var sout = new ByteArrayOutputStream();

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, f.length(), model);
		c.compress(data);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeRange, data.sizeCompressed);
		assertEquals(filecsizeRange, sout.size());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(filecsizeRange + 100);

		var uc = new RangeUncompressor();
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed);
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		uc.uncompress(uData, model);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), uData.sizeUncompressed);
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}

	@Test
	public void compressRange32() throws IOException
	{
		var f = new File(filename);
		var sin = new FileInputStream(f);
		var sout = new ByteArrayOutputStream();

		var c = new RangeCompressor32();
		var model = new ModelOrder0Fixed();
		model.calcWeights(filename);
		model.rescaleTo(c.BOTTOM);
		var data = new CompressData(sin, sout, f.length(), model);

		c.compress(data);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeRange32, data.sizeCompressed);
		assertEquals(filecsizeRange32, sout.size());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(5000);

		var uc = new RangeCompressor32();
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, model);
		uc.uncompress(uData);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), uData.sizeUncompressed);
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}

	@Test
	public void compressRange64() throws IOException
	{
		var f = new File(filename);
		var sin = new FileInputStream(f);
		var sout = new ByteArrayOutputStream();

		var c = new RangeCompressor32(Utils.MODE.MODE64);
		var model = new ModelOrder0Fixed();
		model.calcWeights(filename);
		model.rescaleTo(c.BOTTOM);
		var data = new CompressData(sin, sout, f.length(), model);

		c.compress(data);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeRange64, data.sizeCompressed);
		assertEquals(filecsizeRange64, sout.size());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(5000);

		var uc = new RangeCompressor32(Utils.MODE.MODE64);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, model);
		uc.uncompress(uData);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), uData.sizeUncompressed);
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}

	@Test
	public void compressAdaptRange32() throws IOException
	{
		var f = new File(filename);
		var sin = new FileInputStream(f);
		var sout = new ByteArrayOutputStream();

		var c = new RangeAdaptCompressor();
		var model = new ModelOrder0Adapt(c.BOTTOM);
		var data = new CompressData(sin, sout, f.length(), model);
		c.compress(data);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeARange32, data.sizeCompressed);
		assertEquals(filecsizeARange32, sout.size());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(5000);

		var uc = new RangeAdaptCompressor();
		model = new ModelOrder0Adapt(uc.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, model);
		uc.uncompress(uData);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), uData.sizeUncompressed);
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}
	@Test
	public void compressAdaptRange64() throws IOException
	{
		var f = new File(filename);
		var sin = new FileInputStream(f);
		var sout = new ByteArrayOutputStream();

		var c = new RangeAdaptCompressor(Utils.MODE.MODE64);
		var model = new ModelOrder0Adapt(c.BOTTOM);
		var data = new CompressData(sin, sout, f.length(), model);
		c.compress(data);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeARange64, data.sizeCompressed);
		assertEquals(filecsizeARange64, sout.size());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(5000);

		var uc = new RangeAdaptCompressor(Utils.MODE.MODE64);
		model = new ModelOrder0Adapt(uc.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, model);
		uc.uncompress(uData);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), uData.sizeUncompressed);
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}


	@Test
	public void compressRLE() throws IOException
	{
		var f = new File(filename);
		var sin = new FileInputStream(f);
		var sout = new ByteArrayOutputStream();

		var c = new RLECompressor();
		var data = new CompressData(sin, sout, f.length());
		c.compress(data);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeRLE, data.sizeCompressed);
		assertEquals(filecsizeRLE, sout.size());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(5000);

		var uc = new RLECompressor();
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed);
		uc.uncompress(uData);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), uData.sizeUncompressed);
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}

	@Test
	public void compressAdaptHuffman() throws IOException
	{
		var f = new File(filename);
		var sin = new FileInputStream(f);
		var sout = new ByteArrayOutputStream();

		var c = new AdaptHuffman();
		var data = new CompressData(sin, sout, f.length());
		c.compress(data);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeAHUF, data.sizeCompressed);
		assertEquals(filecsizeAHUF, sout.size());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(5000);

		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed);
		c.uncompress(uData);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), uData.sizeUncompressed);
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}

	@Test
	public void compressHuffman() throws IOException
	{
		var f = new File(filename);

		var sinTree = new FileInputStream(f);
		var tree = new HFTree();
		tree.buildFromStream(sinTree);
		sinTree.close();

		var sin = new FileInputStream(f);
		var sout = new ByteArrayOutputStream();

		var c = new HFCompressor();
		var cdata = new CompressData(sin, sout, f.length(), tree);
		c.compress(cdata);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeHUF, cdata.sizeCompressed); //sout.size() differs from data.sizeCompressed because sout.size() is always aligned to four bytes while data.sizeCompressed not
		assertEquals(((filecsizeHUF-1)/4)*4 + 4, sout.size()); // HUF alg writes output data by ints, i.e. aligned by 4 bytes.

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(5000);

		var uc = new HFCompressor();
		var uData = new CompressData(in2, out2, cdata.sizeCompressed, cdata.sizeUncompressed, tree);
		uc.uncompress(uData);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), uData.sizeUncompressed);
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}

	@Test
	public void compressAdaptHuffmanBlock() throws IOException
	{
		var f = new File(filename);
		var sout = new ByteArrayOutputStream();

		var fr = new HFFileRec();
		fr.origFilename = filename;
		fr.fileName = filename;
		fr.fileSize = f.length();
		fr.blockCount = 0;
		fr.blockSize = Utils.BLOCK_SIZE;

		var c = new AdaptHuffman();
		var bm = new BlockManager();
		bm.compressFile(fr, sout, c);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeBLOCKAHUF, fr.compressedSize);
		assertEquals((int)(filecsizeBLOCKAHUF + fr.blockCount * BlockManager.BLOCK_HEADER_SIZE), (int)sout.size());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(5000);
		var udata = new CompressData(in2, out2, fr.compressedSize, fr.fileSize);
		bm.uncompressFile(fr, udata, c);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), fr.fileSize); // TODO needs redo
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}

	/*
	@Test
	public void compressRLEBlock() throws IOException
	{
		var f = new File(filename);
		var sout = new ByteArrayOutputStream();

		var fr = new HFFileRec();
		fr.origFilename = filename;
		fr.fileName = filename;
		fr.fileSize = f.length();

		var c = new RLECompressor();
		var bm = new BlockManager();
		bm.compressFile(fr, sout, c);

		assertEquals(filesize, f.length());
		assertEquals(filecsizeBLOCKRLE, fr.compressedSize);
		assertEquals((int)(filecsizeBLOCKRLE + fr.blockCount * BlockManager.BLOCK_HEADER_SIZE), (int)sout.size());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(5000);

		var uc = new RLECompressor();
		bm.uncompressFile(fr, in2, out2, uc);

		assertEquals(f.length(), out2.size());
		assertEquals(f.length(), fr.fileSize);
		assertArrayEquals(new FileInputStream(f).readAllBytes(), out2.toByteArray());
	}
*/
}