import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class RangeCompressor32Test {

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
	public void compress1() throws Exception
	{
		String s = "zxcvbMMMbvc";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(11, s.length());
		assertEquals(11, s.getBytes().length);
		assertEquals(7, data.sizeCompressed);
		assertEquals(7, sout.size());

		//byte[] b = {(byte)0x79, (byte)0xFD, (byte)0x75, (byte)0x7F, (byte)0xB8, (byte)0x8E, (byte)0x8D, (byte)0x05, (byte)0xBF, (byte)0x9D, 0x7E, (byte)0x84,(byte)0xAC, (byte)0xD6, 0x0};
		byte[] b = {(byte)0xFC, (byte)0xFE, (byte)0x87, (byte)0x0A, (byte)0x14, (byte)0x88, 0x00};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}


	@Test
	public void compress2() throws Exception
	{
		String s = "zxcvbMMMb";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(s);

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(9, s.length());
		assertEquals(9, s.getBytes().length);
		assertEquals(6, data.sizeCompressed);
		assertEquals(6, sout.size());

		//byte[] b = {(byte)0xFB, (byte)0xAE, (byte)0x9E, 0x35, (byte)0x2D, (byte)0xAA};
		byte[] b = {(byte)0xFB, (byte)0xAE, (byte)0x9B, (byte)0xD7, (byte)0x86, (byte)0xFE};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(20, s.length());
		assertEquals(20, s.getBytes().length);
		assertEquals(4, data.sizeCompressed);
		assertEquals(4, sout.size());

		byte[] b = {0x00, (byte)0x00, (byte)0x00, (byte)0x00};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(20, s.length());
		assertEquals(20, s.getBytes().length);
		assertEquals(5, data.sizeCompressed);
		assertEquals(5, sout.size());

		//byte[] b = {(byte)0xFF, (byte)0xC0, 7, (byte)0xF6, (byte)0x7F};
		byte[] b = {(byte)0xFF, (byte)0xBF, (byte)0xFF,(byte)0xCC,(byte)0x00};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(19, s.length());
		assertEquals(19, s.getBytes().length);
		assertEquals(8, data.sizeCompressed);
		assertEquals(8, sout.size());

		//byte[] b = {(byte)0x92, (byte)0xD1, 0x30, (byte)0xE8, 0x15, (byte)0x29, (byte)0x43, (byte)0xE1};
		byte[] b = {(byte)0x92, (byte)0xD1, (byte)0x30, (byte)0xDA, (byte)0x06, (byte)0xB8, (byte)0xE6, (byte)0x00};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(33, s.length());
		assertEquals(33, s.getBytes().length);
		assertEquals(7, data.sizeCompressed);
		assertEquals(7, sout.size());

		//byte[] b = {0x5A, (byte)0x8E, (byte)0x41, (byte)0xC8, (byte)0x62, (byte)0xF0, (byte)0xAC, 0};
		byte[] b = {0x5A, (byte)0x8E, (byte)0x41, (byte)0x88, (byte)0xE5, (byte)0xFE, (byte)0xBD};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(146, s.length());
		assertEquals(146, s.getBytes().length);
		assertEquals(146, data.sizeUncompressed);
		assertEquals(116, data.sizeCompressed);
		assertEquals(116, sout.size());

	//	byte[] b = {7,34,(byte)195,(byte)241,(byte)186,(byte)158,87,(byte)232,(byte)199,64,(byte)203,22,(byte)246,(byte)209,42,48,(byte)133,82,97,7,121,(byte)154,57,(byte)194,83,46,7,100,17,63,73,(byte)222,114,34,(byte)203,46,120,98,33};
	//	assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(143, s.length());
		assertEquals(143, s.getBytes().length);
		assertEquals(67, data.sizeCompressed);
		assertEquals(67, sout.size());

//		byte[] b = {127, (byte)224, 0, 0, 1};
//		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress8_1() throws IOException
	{
		String ss = "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345MMMMAAAzz89,20009999999999,";
		String s = "012345678901234567890123456789";//01234567890123456789012345678901234567890123456789012345678901234567890123456789012345MMMMAAAzz89,20009999999999,";

		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(ss);

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(30, s.length());
		assertEquals(30, s.getBytes().length);
		assertEquals(17, data.sizeCompressed);
		assertEquals(17, sout.size());

		byte[] b = {0x07,(byte)0x4C,(byte)0x10,(byte)0xE3,(byte)0x30,(byte)0xB9,(byte)0x4A,(byte)0x66,0x14,(byte)0x32,(byte)0x89,(byte)0xFF,(byte)0xFD,(byte)0xBE,(byte)0xB9,(byte)0x74,(byte)0x20};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress8_2() throws IOException
	{
		String ss = "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345MMMMAAAzz89,20009999999999,";
		//String s = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
		String s = "0123456789012345MMMMAAAzz89,20009999999999,";

		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		DataStat ds = calcWeights(ss); // частоты вычисляем по другой строке

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(43, s.length());
		assertEquals(43, s.getBytes().length);
		assertEquals(24, data.sizeCompressed);
		assertEquals(24, sout.size());

		//byte[] b = {0x07,(byte)0x4C,(byte)0x10,(byte)0xE3,(byte)0x30,(byte)0xB9,(byte)0x4A,(byte)0x66,0x14,(byte)0x32,(byte)0x89,(byte)0xFF,(byte)0xFD,(byte)0xBE,0x5E,(byte)0xDB,(byte)0xC0};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(111, s.length());
		assertEquals(111, s.getBytes().length);
		assertEquals(111, data.sizeUncompressed);
		assertEquals(88, data.sizeCompressed);
		assertEquals(88, sout.size());

		//	byte[] b = {7,34,(byte)195,(byte)241,(byte)186,(byte)158,87,(byte)232,(byte)199,64,(byte)203,22,(byte)246,(byte)209,42,48,(byte)133,82,97,7,121,(byte)154,57,(byte)194,83,46,7,100,17,63,73,(byte)222,114,34,(byte)203,46,120,98,33};
		//	assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream();

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(50, s.length());
		assertEquals(50, s.getBytes().length);
		assertEquals(10, data.sizeCompressed);
		assertEquals(10, sout.size());

		//byte[] b = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xCA, (byte)0xC0, (byte)0xBB, 0x23, (byte)0x93, (byte)0x8A};
		byte[] b = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x66, (byte)0x8A, (byte)0x5A, (byte)0x00, (byte)0x00, (byte)0x00, 0x00};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(1001, s.length());
		assertEquals(1001, s.getBytes().length);
		assertEquals(5, data.sizeCompressed);
		assertEquals(5, sout.size());

		//byte[] b = {0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(1001, s.length());
		assertEquals(1001, s.getBytes().length);
		assertEquals(5, data.sizeCompressed);
		assertEquals(5, sout.size());

		//byte[] b = {0x5E, (byte)0x2D, (byte)0x57, (byte)0xDC, 0};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(48, s.length());
		assertEquals(48, s.getBytes().length);
		assertEquals(13, data.sizeCompressed);
		assertEquals(13, sout.size());

		//byte[] b = {0x31, (byte)0x3B, (byte)0x13, (byte)0xAe, (byte)0x7D, (byte)0xFD, (byte)0xB9, 0x4B, 0x57, (byte)0xE5, (byte)0xC5, 0x75,0};
		byte[] b = {0x31, (byte)0x3B, (byte)0x13, (byte)0x74, (byte)0xCB, (byte)0x7E, (byte)0x06, (byte)0x08, (byte)0x4F, (byte)0xD3, (byte)0x1F,(byte)0x90,(byte)0x00};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
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

		var c = new RangeCompressor32();
		var data = new RangeCompressData(sin, sout, s.getBytes().length, ds.cumFreq, ds.weights, ds.symbol_to_freq_index);
		c.compress(data);

		assertEquals(39, s.length());
		assertEquals(78, s.getBytes().length);
		assertEquals(12, data.sizeCompressed);
		assertEquals(12, sout.size());

		byte[] b = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0x5A, (byte)0xFF, (byte)0xC7, (byte)0x0E, (byte)0x87, (byte)0xE5, (byte)0xFE, (byte)0x38, (byte)0x64};
		//byte[] b = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0x5A,(byte)0xFF,(byte)0xC7,(byte)0x0E, (byte)0x87, (byte)0xE5, (byte)0xFE, (byte)0x38, (byte)0x64};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		var uc = new RangeUncompressor32();
		var uData = new RangeUncompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, ds.cumFreq, ds.symbols);
		uc.uncompress(uData);

		assertEquals(s.getBytes().length, out2.size());
		assertEquals(s.getBytes().length, uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

}
