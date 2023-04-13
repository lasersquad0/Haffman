import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class RangeCompressorTest {

	private static int numTest = 0;
	@Test
	public void compress1() throws IOException
	{
		numTest++;

		String s = "zxcvbMMMbvc";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);

		c.compress(data);

		assertEquals(11, s.length());
		assertEquals(11, s.getBytes().length);
		assertEquals(16, data.sizeCompressed);
		assertEquals(16, sout.size());

		//byte[] b = {(byte)0xFC, (byte)0xFE, (byte)0x87, (byte)0x20, (byte)0x16, 0x3B, 0};
		byte[] b = {(byte)0x7A, (byte)0x77, (byte)0xEA, (byte)0xC5, (byte)0xD7, (byte)0xB8, (byte)0xF2,(byte)0xC6,0x3D,(byte)0x3B,(byte)0xFA,0x4E, 0x25, (byte)0x99,0x48, (byte)0x3A};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress2() throws IOException
	{
		numTest++;

		String s = "zxcvbMMMb";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);

		c.compress(data);

		assertEquals(9, s.length());
		assertEquals(9, s.getBytes().length);
		assertEquals(15, data.sizeCompressed);
		assertEquals(15, sout.size());

		//byte[] b = {(byte)0xFB, (byte)0xAE, (byte)0x9E, 0x35, (byte)0x2D, (byte)0xAA};
		byte[] b = {(byte)0x7A,(byte)0x77,(byte)0xEA,(byte)0xC5,(byte)0xD7,(byte)0xB8,(byte)0xF2,(byte)0xC6,(byte)0x3D,(byte)0x89,(byte)0xC1,0x3D,(byte)0x42,(byte)0xB0, 0x0};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, model);

		uc.uncompress(uData, model);

		assertEquals(s.length(), out2.size());
		assertEquals(s.length(), uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

	@Test
	public void compress3() throws IOException
	{
		numTest++;

		String s = "MMMMMMMMMMMMMMMMMMMM";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);
		c.compress(data);

		assertEquals(20, s.length());
		assertEquals(20, s.getBytes().length);
		assertEquals(19, data.sizeCompressed);
		assertEquals(19, sout.size());

		byte[] b = {0x4D,0x4D,0x4D,0x4D,0x4D,(byte)0x4C,(byte)0xFF,(byte)0xB3,(byte)0xDD,0x17,0x19,(byte)0x1E,(byte)0x80,(byte)0x9E,(byte)0xC5,0x2B,0x29,0X02,0x00};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);

		c.compress(data);

		assertEquals(20, s.length());
		assertEquals(20, s.getBytes().length);
		assertEquals(21, data.sizeCompressed);
		assertEquals(21, sout.size());

		//byte[] b = {(byte)0xFF, (byte)0xC0, 7, (byte)0xF6, (byte)0x7F};
		byte[] b = {0x4D,0x4D,0x4D,0x4D,0x4D,(byte)0x4C,(byte)0xFF,(byte)0xB1, (byte)0x39,(byte)0xC0, 0x2C, 0x36,0x6F,0x07,0x12,0x69,0x39,(byte)0xD8,0x70,0x41,0x00};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length,model);

		c.compress(data);

		assertEquals(19, s.length());
		assertEquals(19, s.getBytes().length);
		assertEquals(23, data.sizeCompressed);
		assertEquals(23, sout.size());

		//byte[] b = {(byte)0x92, (byte)0xD1, 0x30, (byte)0xE8, 0x15, (byte)0x29, (byte)0x43, (byte)0xE1};
		byte[] b = {0x4D,0x4D,0x4D,0x4C,(byte)0x1B,(byte)0x35,(byte)0xCF,(byte)0xEA,(byte)0xF0,(byte)0xD1,(byte)0xB0,(byte)0xCF,(byte)0xEA, 0x65, (byte)0x87, 0x57, (byte)0x98, (byte)0xDF,(byte)0x63,(byte)0xE3,(byte)0x66,(byte)0xCD,(byte)0x80};
		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length,model);

		c.compress(data);

		assertEquals(33, s.length());
		assertEquals(33, s.getBytes().length);
		assertEquals(28, data.sizeCompressed);
		assertEquals(28, sout.size());

		//byte[] b = {0x5A, (byte)0x8E, (byte)0x41, (byte)0xC8, (byte)0x62, (byte)0xF0, (byte)0xAC, 0};
		//byte[] b = {0x5A, (byte)0x8E, (byte)0x41, (byte)0xC7, (byte)0xB4, (byte)0x3B, (byte)0xEd, (byte)0x80};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length,model);

		c.compress(data);

		assertEquals(146, s.length());
		assertEquals(146, s.getBytes().length);
		assertEquals(146, data.sizeUncompressed);
		assertEquals(150, data.sizeCompressed);
		assertEquals(150, sout.size());

	//	byte[] b = {7,34,(byte)195,(byte)241,(byte)186,(byte)158,87,(byte)232,(byte)199,64,(byte)203,22,(byte)246,(byte)209,42,48,(byte)133,82,97,7,121,(byte)154,57,(byte)194,83,46,7,100,17,63,73,(byte)222,114,34,(byte)203,46,120,98,33};
	//	assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);

		c.compress(data);

		assertEquals(143, s.length());
		assertEquals(143, s.getBytes().length);
		assertEquals(112, data.sizeCompressed);
		assertEquals(112, sout.size());

//		byte[] b = {127, (byte)224, 0, 0, 1};
//		assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);

		c.compress(data);

		assertEquals(111, s.length());
		assertEquals(111, s.getBytes().length);
		assertEquals(111, data.sizeUncompressed);
		assertEquals(116, data.sizeCompressed);
		assertEquals(116, sout.size());

		//	byte[] b = {7,34,(byte)195,(byte)241,(byte)186,(byte)158,87,(byte)232,(byte)199,64,(byte)203,22,(byte)246,(byte)209,42,48,(byte)133,82,97,7,121,(byte)154,57,(byte)194,83,46,7,100,17,63,73,(byte)222,114,34,(byte)203,46,120,98,33};
		//	assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed, model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);

		c.compress(data);

		assertEquals(50, s.length());
		assertEquals(50, s.getBytes().length);
		assertEquals(36, data.sizeCompressed);
		assertEquals(36, sout.size());

		//byte[] b = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xCA, (byte)0xC0, (byte)0xBB, 0x23, (byte)0x93, (byte)0x8A};
		//byte[] b = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xC9, (byte)0xC3, (byte)0xBA, (byte)0xF1, (byte)0x93, (byte)0x85};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);

		c.compress(data);

		assertEquals(1001, s.length());
		assertEquals(1001, s.getBytes().length);
		assertEquals(122, data.sizeCompressed);
		assertEquals(122, sout.size());

		//byte[] b = {0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);

		c.compress(data);

		assertEquals(1001, s.length());
		assertEquals(1001, s.getBytes().length);
		assertEquals(121, data.sizeCompressed);
		assertEquals(121, sout.size());

		//byte[] b = {0x5E, (byte)0x2D, (byte)0x57, (byte)0xDC, 0};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length,model);

		c.compress(data);

		assertEquals(48, s.length());
		assertEquals(48, s.getBytes().length);
		assertEquals(38, data.sizeCompressed);
		assertEquals(38, sout.size());

		//byte[] b = {0x31, (byte)0x3B, (byte)0x13, (byte)0xAe, (byte)0x7D, (byte)0xFD, (byte)0xB9, 0x4B, 0x57, (byte)0xE5, (byte)0xC5, 0x75,0};
		//byte[] b = {0x31, (byte)0x3B, (byte)0x13, (byte)0xAe, (byte)0x4A, (byte)0xCD, (byte)0x6B, 0x56, (byte)0x91, (byte)0x64, (byte)0x13,(byte)0xF4,(byte)0xF0};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

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

		var c = new RangeCompressor();
		var model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var data = new CompressData(sin, sout, s.getBytes().length, model);

		c.compress(data);

		assertEquals(39, s.length());
		assertEquals(78, s.getBytes().length);
		assertEquals(48, data.sizeCompressed);
		assertEquals(48, sout.size());

		//byte[] b = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xA5, (byte)0x55, (byte)0x55, (byte)0x55, 0x55, 0x55, (byte)0x54, (byte)0xAA, (byte)0xAA,(byte)0xAc};
		//byte[] b = {(byte)0xAA, (byte)0xAA, (byte)0xAA, (byte)0xA9, (byte)0xFE, (byte)0xFF, (byte)0xA7, 0x54, (byte)0xAA, (byte)0x54, (byte)0x55, (byte)0x01,(byte)0xAF};
		//assertArrayEquals(b, sout.toByteArray());

		ByteArrayInputStream in2 = new ByteArrayInputStream(sout.toByteArray());
		ByteArrayOutputStream out2 = new ByteArrayOutputStream(200);

		RangeUncompressor uc = new RangeUncompressor();
		model = new ModelOrder0Adapt(RangeCompressor.BOTTOM);
		var uData = new CompressData(in2, out2, data.sizeCompressed, data.sizeUncompressed,model);

		uc.uncompress(uData, model);

		assertEquals(s.getBytes().length, out2.size());
		assertEquals(s.getBytes().length, uData.sizeUncompressed);
		assertEquals(s, out2.toString());
	}

}
