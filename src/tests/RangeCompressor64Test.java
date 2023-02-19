import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RangeCompressor64Test {

	private DataStat calcWeights(String s)
	{
		DataStat ds = new DataStat();

		long[] freq = new long[256];
		byte[] b = s.getBytes();
		for (byte value : b)
		{
			freq[Byte.toUnsignedInt(value)]++; // обходим знаковость байта
		}

		int nonzero = 0;
		for (long l : freq)
			if(l > 0) nonzero++;

		ds.symbols = new int[nonzero];
		ds.weights = new long[nonzero];

		int j = 0;
		for (int i = 0; i < freq.length; i++)
		{
			if(freq[i] > 0)
			{
				ds.symbols[j] = i;
				ds.weights[j] = freq[i];
				j++;
			}
		}

		// preparing data for compressor/uncompressor
		ds.cumFreq = new long[ds.weights.length + 1];
		ds.cumFreq[0] = 0;
		for (int i = 0; i < ds.weights.length; i++)
		{
			ds.cumFreq[i+1] = ds.cumFreq[i] + ds.weights[i];
		}
		ds.symbol_to_freq_index = new int[256];
		for (int i = 0; i < ds.symbols.length; i++)
		{
			ds.symbol_to_freq_index[ds.symbols[i]] = i + 1;
		}

		return ds;
	}

	@Test
	public void compress1() throws IOException
	{
		String s = "zxcvbMMMbvc";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(11, s.length());
		assertEquals(11, s.getBytes().length);
		assertEquals(10, data.sizeCompressed);
		assertEquals(10, sout.size());

		byte[] b = {(byte)0xFC, (byte)0xFE, (byte)0x87, (byte)0x23, (byte)0xAB, (byte)0xA5, 0x0D, 0x5F, (byte)0x99, 0x00};
		//byte[] b = {(byte)0x23, (byte)0xAB, (byte)0xA5, (byte)0x0D, (byte)0x5F, (byte)0x99, 0x0D};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress2() throws IOException
	{
		String s = "zxcvbMMMb";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(9, s.length());
		assertEquals(9, s.getBytes().length);
		assertEquals(9, data.sizeCompressed);
		assertEquals(9, sout.size());

		//byte[] b = {(byte)0xFB, (byte)0xAE, (byte)0x9E, 0x35, (byte)0x2D, (byte)0xAA};
		byte[] b = {(byte)0xFB, (byte)0xAE, (byte)0x9E, 0x38, (byte)0xE3, (byte)0xEb, (byte)0xBE, (byte)0x33, 0x70};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress3() throws IOException
	{
		String s = "MMMMMMMMMMMMMMMMMMMM";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(20, s.length());
		assertEquals(20, s.getBytes().length);
		assertEquals(7, data.sizeCompressed);
		assertEquals(7, sout.size());

		byte[] b = {0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xF8};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress4() throws IOException
	{
		String s = "MMMMMMMMMMAAAAAAAAAA";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(20, s.length());
		assertEquals(20, s.getBytes().length);
		assertEquals(8, data.sizeCompressed);
		assertEquals(8, sout.size());

		//byte[] b = {(byte)0xFF, (byte)0xC0, 7, (byte)0xF6, (byte)0x7F};
		byte[] b = {(byte)0xFF, (byte)0xC0, 0x07, (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xCB, (byte)0xFD};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress5() throws IOException
	{
		String s = "MMMMAAAzxcvbMMMAAAA";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(19, s.length());
		assertEquals(19, s.getBytes().length);
		assertEquals(11, data.sizeCompressed);
		assertEquals(11, sout.size());

		//byte[] b = {(byte)0x92, (byte)0xD1, 0x30, (byte)0xE8, 0x15, (byte)0x29, (byte)0x43, (byte)0xE1};
		byte[] b = {(byte)0x92, (byte)0xD1, (byte)0x30, (byte)0xEc, (byte)0x61, (byte)0xDB, (byte)0xAC, (byte)0x09, (byte)0xCA, (byte)0x5D, (byte)0x39};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress6() throws IOException
	{
		String s = "ababababababababababababababababa";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(33, s.length());
		assertEquals(33, s.getBytes().length);
		assertEquals(10, data.sizeCompressed);
		assertEquals(10, sout.size());

		//byte[] b = {0x5A, (byte)0x8E, (byte)0x41, (byte)0xC8, (byte)0x62, (byte)0xF0, (byte)0xAC, 0};
		byte[] b = {0x5A, (byte)0x8E, (byte)0x41, (byte)0xCD, (byte)0x24, (byte)0xD7, (byte)0xE0, (byte)0x88, (byte)0x46, (byte)0x64};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress7() throws IOException
	{
		String s = "1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(146, s.length());
		assertEquals(146, s.getBytes().length);
		assertEquals(146, data.sizeUncompressed);
		assertEquals(119, data.sizeCompressed);
		assertEquals(119, sout.size());

	//	byte[] b = {7,34,(byte)195,(byte)241,(byte)186,(byte)158,87,(byte)232,(byte)199,64,(byte)203,22,(byte)246,(byte)209,42,48,(byte)133,82,97,7,121,(byte)154,57,(byte)194,83,46,7,100,17,63,73,(byte)222,114,34,(byte)203,46,120,98,33};
	//	assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress8() throws IOException
	{
		String s = "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345MMMMAAAzz89,20009999999999,";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(143, s.length());
		assertEquals(143, s.getBytes().length);
		assertEquals(70, data.sizeCompressed);
		assertEquals(70, sout.size());

//		byte[] b = {127, (byte)224, 0, 0, 1};
//		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress9() throws IOException
	{
		String s = "1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;1234567890qwertyuioplkjhgfdsazxcvbnm,.";///QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(111, s.length());
		assertEquals(111, s.getBytes().length);
		assertEquals(111, data.sizeUncompressed);
		assertEquals(91, data.sizeCompressed);
		assertEquals(91, sout.size());

		//	byte[] b = {7,34,(byte)195,(byte)241,(byte)186,(byte)158,87,(byte)232,(byte)199,64,(byte)203,22,(byte)246,(byte)209,42,48,(byte)133,82,97,7,121,(byte)154,57,(byte)194,83,46,7,100,17,63,73,(byte)222,114,34,(byte)203,46,120,98,33};
		//	assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress10() throws IOException
	{
		String s = "MMMMMMMMMMMMMMMMMMMMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(50, s.length());
		assertEquals(50, s.getBytes().length);
		assertEquals(13, data.sizeCompressed);
		assertEquals(13, sout.size());

		//byte[] b = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xCA, (byte)0xC0, (byte)0xBB, 0x23, (byte)0x93, (byte)0x8A};
		byte[] b = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xD0, (byte)0xC6, (byte)0xBD, (byte)0x8D, (byte)0xD8, (byte)0xD9, 0x0C, 0x26, (byte)0xBF, (byte)0x00};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress11() throws IOException
	{
		String s = "M"+ "A".repeat(1000);

		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(1001, s.length());
		assertEquals(1001, s.getBytes().length);
		assertEquals(8, data.sizeCompressed);
		assertEquals(8, sout.size());

		//byte[] b = {0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress12() throws IOException
	{
		String s = "A".repeat(1000) + "M";

		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(1001, s.length());
		assertEquals(1001, s.getBytes().length);
		assertEquals(8, data.sizeCompressed);
		assertEquals(8, sout.size());

		//byte[] b = {0x5E, (byte)0x2D, (byte)0x57, (byte)0xDC, 0};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress13() throws IOException
	{
		String s = "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(48, s.length());
		assertEquals(48, s.getBytes().length);
		assertEquals(16, data.sizeCompressed);
		assertEquals(16, sout.size());

		//byte[] b = {0x31, (byte)0x3B, (byte)0x13, (byte)0xAe, (byte)0x7D, (byte)0xFD, (byte)0xB9, 0x4B, 0x57, (byte)0xE5, (byte)0xC5, 0x75,0};
		byte[] b = {0x31, (byte)0x3B, (byte)0x13, (byte)0xB1, (byte)0x3B, (byte)0x13, (byte)0x8C, (byte)0x19, (byte)0x1C, (byte)0xC7, (byte)0x13,(byte)0x84,(byte)0x23, (byte)0x01, 0x06,(byte)0xF8};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress14() throws IOException
	{
		String s = "яяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяя";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.getBytes().length);

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor64();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(39, s.length());
		assertEquals(78, s.getBytes().length);
		assertEquals(16, data.sizeCompressed);
		assertEquals(16, sout.size());

		//byte[] b = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xA5, (byte)0x55, (byte)0x55, (byte)0x55, 0x55, 0x55, (byte)0x54, (byte)0xAA, (byte)0xAA,(byte)0xAc};
		byte[] b = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xAA,(byte)0xAA,(byte)0xAA,(byte)0x5A, (byte)0xFF, (byte)0xC7, (byte)0x1B, (byte)0xFF, (byte)0xFF, (byte)0xF2, (byte)0x16,(byte)0x03,(byte)0xE0};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor64();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.getBytes().length, out2.size());
		assertEquals(s.getBytes().length, uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}
}
