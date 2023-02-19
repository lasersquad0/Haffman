import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class RLEUncompressorTest {

	@Test
	public void uncompress1() throws IOException
	{
		String s = "zxcvbMMMbvc";
		byte[] b = {4,'z','x','c','v','b',(byte)129,'M',2,'b','v','c'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(12, b.length);
		assertEquals(12, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress2() throws IOException
	{
		String s = "zxcvbMMMbv";
		byte[] b = {4,'z','x','c','v','b',(byte)129,'M',1,'b','v'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(11, b.length);
		assertEquals(11, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress3() throws IOException
	{
		String s = "zxcvbMMMb";
		byte[] b = {4,'z','x','c','v','b',(byte)129,'M',0,'b'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(10, b.length);
		assertEquals(10, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress4() throws IOException
	{
		String s = "zxcvbMMb";
		byte[] b = {7,'z','x','c','v','b','M','M','b'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(9, b.length);
		assertEquals(9, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress5() throws IOException
	{
		String s = "MMMzxcvbMMM123456";
		byte[] b = {(byte)129,'M',4,'z','x','c','v','b',(byte)129,'M',5,'1','2','3','4','5','6'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(17, b.length);
		assertEquals(17, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress6() throws IOException
	{
		String s = "MMMMAAAzxcvbMMMb";
		byte[] b = {(byte)130,'M',(byte)129,'A',4,'z','x','c','v','b',(byte)129,'M',0,'b'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(14, b.length);
		assertEquals(14, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress7() throws IOException
	{
		String s = "MMMMnAAAvbMMMb";
		byte[] b = {(byte)130,'M',0,'n',(byte)129,'A',1,'v','b',(byte)129,'M',0,'b'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(13, b.length);
		assertEquals(13, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress8() throws IOException
	{
		String s = "MMMMAAAzxcvbMMM";
		byte[] b = {(byte)130,'M',(byte)129,'A',4,'z','x','c','v','b',(byte)129,'M'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(12, b.length);
		assertEquals(12, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress9() throws IOException
	{
		String s = "MMMMAAAzxcvbMMMAAAA";
		byte[] b = {(byte)130,'M',(byte)129,'A',4,'z','x','c','v','b',(byte)129,'M', (byte)130,'A'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(14, b.length);
		assertEquals(14, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress10() throws IOException
	{
		String s = "ababababababababababababababababa";
		byte[] b = {32,'a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(34, b.length);
		assertEquals(34, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress11() throws IOException
	{
		String s = "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM";
		byte[] b = {(byte)255,'M',(byte)137,'M'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(4, b.length);
		assertEquals(4, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress12() throws IOException
	{
		String s = "MMM";
		byte[] b = {(byte)129,'M'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(2, b.length);
		assertEquals(2, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress13() throws IOException
	{
		String s = "MM";
		byte[] b = {1,'M','M'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(3, b.length);
		assertEquals(3, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress14() throws IOException
	{
		String s = "b";
		byte[] b = {0,'b'};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(2, b.length);
		assertEquals(2, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress15() throws IOException
	{
		String s = "я";
		byte[] b = {1,(byte)209,(byte)143};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(3, b.length);
		assertEquals(3, uData.sizeCompressed);
		assertEquals(s.getBytes().length, sout.size());

		assertEquals(s, sout.toString());
	}

	@Test
	public void uncompress16() throws IOException
	{
		String s = "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345MMMMAAAzz89,20009999999999,";
		byte[] b = {115,'0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9',
				    '0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5','6','7','8','9',
					'0','1','2','3','4','5','6','7','8','9','0','1','2','3','4','5',
					(byte)130,'M',(byte)129,'A',1,'z','z',3,0x38,0x39,0x2C,0x32,(byte)0x81,0x30,(byte)0x88,0x39,0,0x2C};
		var sin = new ByteArrayInputStream(b);
		var sout = new ByteArrayOutputStream(b.length*2);

		var uData = new UncompressData(sin, sout, b.length, s.length());
		var uncomp = new RLEUncompressor();
		uncomp.uData = uData;

		uncomp.uncompressInternal();

		assertEquals(135, b.length);
		assertEquals(135, uData.sizeCompressed);
		assertEquals(s.length(), sout.size());

		assertEquals(s, sout.toString());
	}
}