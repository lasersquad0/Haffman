import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class RLECompressorTest {

	@Test
	public void compress1() throws IOException
	{
		// unequal - equal - unequal
		// all seqs 3 and more symbols long

		String s = "zxcvbMMMbvc";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(11, s.length());
		assertEquals(12, data.sizeCompressed);
		assertEquals(12, sout.size());

		byte[] b = {4,'z','x','c','v','b',(byte)129,'M',2,'b','v','c'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress2() throws IOException
	{
		// Ul - El - Us
		// 2 first seqs more than 3 symbols long, last seq is shortened
		String s = "zxcvbMMMbv";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(10, s.length());
		assertEquals(11, data.sizeCompressed);
		assertEquals(11, sout.size());

		byte[] b = {4,'z','x','c','v','b',(byte)129,'M',1,'b','v'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress3() throws IOException
	{
		// Ul - El - Uss
		// 2 first seqs more than 3 symbols long, last seq is shortened
		String s = "zxcvbMMMb";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(9, s.length());
		assertEquals(10, data.sizeCompressed);
		assertEquals(10, sout.size());

		byte[] b = {4,'z','x','c','v','b',(byte)129,'M',0,'b'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress4() throws IOException
	{
		// Ul
		// there are two equal symbols M which are treated as UNequal
		String s = "zxcvbMMb";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(8, s.length());
		assertEquals(9, data.sizeCompressed);
		assertEquals(9, sout.size());

		byte[] b = {7,'z','x','c','v','b','M','M','b'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress5() throws IOException
	{
		// El-Ul-El-Ul
		String s = "MMMzxcvbMMM123456";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(17, s.length());
		assertEquals(17, data.sizeCompressed);
		assertEquals(17, sout.size());

		byte[] b = {(byte)129,'M',4,'z','x','c','v','b',(byte)129,'M',5,'1','2','3','4','5','6'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress6() throws IOException
	{
		// El-El-Ul-El-Uss
		String s = "MMMMAAAzxcvbMMMb";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(16, s.length());
		assertEquals(14, data.sizeCompressed);
		assertEquals(14, sout.size());

		byte[] b = {(byte)130,'M',(byte)129,'A',4,'z','x','c','v','b',(byte)129,'M',0,'b'};
		assertArrayEquals(b, sout.toByteArray());

	}

	@Test
	public void compress7() throws IOException
	{
		// El-Uss-El-Uss-El-Uss
		String s = "MMMMnAAAvbMMMb";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(14, s.length());
		assertEquals(13, data.sizeCompressed);
		assertEquals(13, sout.size());

		byte[] b = {(byte)130,'M',0,'n',(byte)129,'A',1,'v','b',(byte)129,'M',0,'b'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress8() throws IOException
	{
		String s = "MMMMAAAzxcvbMMM";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(15, s.length());
		assertEquals(12, data.sizeCompressed);
		assertEquals(12, sout.size());

		byte[] b = {(byte)130,'M',(byte)129,'A',4,'z','x','c','v','b',(byte)129,'M'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress81() throws IOException
	{
		String s = "MMMMAAAzxcvbMMMAAAA";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(19, s.length());
		assertEquals(14, data.sizeCompressed);
		assertEquals(14, sout.size());

		byte[] b = {(byte)130,'M',(byte)129,'A',4,'z','x','c','v','b',(byte)129,'M', (byte)130,'A'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress9() throws IOException
	{
		String s = "ababababababababababababababababa";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(33, s.length());
		assertEquals(34, data.sizeCompressed);
		assertEquals(34, sout.size());

		byte[] b = {32,'a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress10() throws IOException
	{
		// More than 129 equal symbols. Test how it is splitted into two groups.
		String s = "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(140, s.length());
		assertEquals(4, data.sizeCompressed);
		assertEquals(4, sout.size());

		byte[] b = {(byte)255,'M',(byte)137,'M'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress11() throws IOException
	{
		// More than 128 unequal symbols. Test how it is splitted into two groups
		String s = "1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(146, s.length());
		assertEquals(148, data.sizeCompressed);
		assertEquals(148, sout.size());

		byte[] b = new byte[148];
		String s2 = "1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LK";
		b[0] = (byte)127;
		byte[] sb = s2.getBytes();

		assertEquals(128, s2.length());
		assertEquals(128, sb.length);

		System.arraycopy(sb, 0, b, 1, sb.length);

		String s3 = "JHGFDSAZXCVBNM<>?;";
		sb = s3.getBytes();
		b[129] = 17;
		System.arraycopy(sb, 0, b, 130, 18);

		assertArrayEquals(b, sout.toByteArray());

	}

	@Test
	public void compress12() throws IOException
	{
		// group of only one type (equal)
		String s = "MMM";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(3, s.length());
		assertEquals(2, data.sizeCompressed);
		assertEquals(2, sout.size());

		byte[] b = {(byte)129,'M'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress13() throws IOException
	{
		// this is Unequal short group
		String s = "MM";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(2, s.length());
		assertEquals(3, data.sizeCompressed);
		assertEquals(3, sout.size());

		byte[] b = {1,'M','M'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress14() throws IOException
	{
		//the simples test for one symbol long
		String s = "b";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(1, s.length());
		assertEquals(2, data.sizeCompressed);
		assertEquals(2, sout.size());

		byte[] b = {0,'b'};
		assertArrayEquals(b, sout.toByteArray());
	}

	@Test
	public void compress15() throws IOException
	{
		// test with single russian symbol that converted into 2 bytes by s.getBytes()
		String s = "я";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new HFCompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(1, s.length());
		assertEquals(2, s.getBytes().length);
		assertEquals(3, data.sizeCompressed);
		assertEquals(3, sout.size());

		byte[] b = {1,(byte)209,(byte)143};
		assertArrayEquals(b, sout.toByteArray());
	}
}