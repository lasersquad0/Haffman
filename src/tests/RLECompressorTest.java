import org.junit.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class RLECompressorTest {

	static class ArrayHolder
	{
		byte[] b;

		ArrayHolder(byte[] bb)
		{
			b = bb;
		}
	}

	static class MyArgumentsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception
		{

			return Stream.of(
					Arguments.of("zxcvbMMMbvc", 11, 11, 12, new ArrayHolder(new byte[]{4,'z','x','c','v','b',(byte)129,'M',2,'b','v','c'}) ),
					Arguments.of("zxcvbMMMbv", 10, 10, 11, new ArrayHolder(new byte[]{4,'z','x','c','v','b',(byte)129,'M',1,'b','v'}) ),
					Arguments.of("zxcvbMMMb", 9, 9, 10, new ArrayHolder(new byte[]{4,'z','x','c','v','b',(byte)129,'M',0,'b'}) ),
					Arguments.of("zxcvbMMb", 8, 8, 9, new ArrayHolder(new byte[]{7,'z','x','c','v','b','M','M','b'}) ),
					Arguments.of("MMMzxcvbMMM123456", 17, 17, 17, new ArrayHolder(new byte[]{(byte)129,'M',4,'z','x','c','v','b',(byte)129,'M',5,'1','2','3','4','5','6'}) ),
					Arguments.of("MMMMAAAzxcvbMMMb", 16, 16, 14, new ArrayHolder(new byte[]{(byte)130,'M',(byte)129,'A',4,'z','x','c','v','b',(byte)129,'M',0,'b'}) ),
					Arguments.of("MMMMnAAAvbMMMb", 14, 14, 13, new ArrayHolder(new byte[]{(byte)130,'M',0,'n',(byte)129,'A',1,'v','b',(byte)129,'M',0,'b'}) ),
					Arguments.of("MMMMAAAzxcvbMMM", 15, 15, 12, new ArrayHolder(new byte[]{(byte)130,'M',(byte)129,'A',4,'z','x','c','v','b',(byte)129,'M'}) ),
					Arguments.of("MMMMAAAzxcvbMMMAAAA", 19, 19, 14, new ArrayHolder(new byte[]{(byte)130,'M',(byte)129,'A',4,'z','x','c','v','b',(byte)129,'M', (byte)130,'A'}) ),
					Arguments.of("ababababababababababababababababa", 33, 33, 34, new ArrayHolder(new byte[]{32,'a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a','b','a'}) ),
					Arguments.of("M".repeat(140), 140, 140, 4, new ArrayHolder(new byte[]{(byte)255,'M',(byte)137,'M'}) ),
					Arguments.of("M".repeat(126), 126, 126, 2, new ArrayHolder(new byte[]{(byte)252,'M'}) ),
					Arguments.of("M".repeat(127), 127, 127, 2, new ArrayHolder(new byte[]{(byte)253,'M'}) ),
					Arguments.of("M".repeat(128), 128, 128, 2, new ArrayHolder(new byte[]{(byte)254,'M'}) ),
					Arguments.of("M".repeat(129), 129, 129, 2, new ArrayHolder(new byte[]{(byte)255,'M'}) ),
					Arguments.of("M".repeat(130), 130, 130, 4, new ArrayHolder(new byte[]{(byte)255,'M',(byte)0,'M'}) ),
					Arguments.of("M".repeat(131), 131, 131, 5, new ArrayHolder(new byte[]{(byte)255,'M',(byte)1,'M','M'}) ),
					Arguments.of("M".repeat(132), 132, 132, 4, new ArrayHolder(new byte[]{(byte)255,'M',(byte)129,'M'}) ),
					Arguments.of("MMM", 3, 3, 2, new ArrayHolder(new byte[]{(byte)129,'M'}) ),
					Arguments.of("MM",  2, 2, 3, new ArrayHolder(new byte[]{1,'M','M'}) ),
					Arguments.of("b",   1, 1, 2, new ArrayHolder(new byte[]{0,'b'}) ),
					Arguments.of("я",   1, 2, 3, new ArrayHolder(new byte[]{1,(byte)209,(byte)143}) ),
					Arguments.of("яя",  2, 4, 5, new ArrayHolder(new byte[]{3,(byte)209,(byte)143,(byte)209,(byte)143}) ),
					Arguments.of("яяя", 3, 6, 7, new ArrayHolder(new byte[]{5,(byte)209,(byte)143,(byte)209,(byte)143,(byte)209,(byte)143}) )
			);
		}
	}
	@ParameterizedTest(name = "dataset - {index}")
	@ArgumentsSource(MyArgumentsProvider.class)
	public void compress1(String s, int strlen, int blen, int clen, ArrayHolder carr) throws IOException
	{
		// unequal - equal - unequal
		// all seqs 3 and more symbols long

		//String s = "zxcvbMMMbvc";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new CompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(strlen, s.length());
		assertEquals(blen, s.getBytes().length);
		assertEquals(clen, data.sizeCompressed);
		assertEquals(clen, sout.size());

		//byte[] b = {4,'z','x','c','v','b',(byte)129,'M',2,'b','v','c'};
		assertArrayEquals(carr.b, sout.toByteArray());
	}

	/*
	@Test
	public void compress2() throws IOException
	{
		// Ul - El - Us
		// 2 first seqs more than 3 symbols long, last seq is shortened
		String s = "zxcvbMMMbv";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(140, s.length());
		assertEquals(4, data.sizeCompressed);
		assertEquals(4, sout.size());

		byte[] b = {(byte)255,'M',(byte)137,'M'};
		assertArrayEquals(b, sout.toByteArray());
	}
*/
	@Test
	public void compress11() throws IOException
	{
		// More than 128 unequal symbols. Test how it is splitted into two groups
		String s = "1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;1234567890qwertyuioplkjhgfdsazxcvbnm,./QWERTYUIOP{}|:LKJHGFDSAZXCVBNM<>?;";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new CompressData(sin, sout, s.getBytes().length);
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
/*
	@Test
	public void compress12() throws IOException
	{
		// group of only one type (equal)
		String s = "MMM";
		var sin = new ByteArrayInputStream(s.getBytes());
		var sout = new ByteArrayOutputStream(s.length());

		var c = new RLECompressor();
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
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
		var data = new CompressData(sin, sout, s.getBytes().length);
		c.compress(data);

		assertEquals(1, s.length());
		assertEquals(2, s.getBytes().length);
		assertEquals(3, data.sizeCompressed);
		assertEquals(3, sout.size());

		byte[] b = {1,(byte)209,(byte)143};
		assertArrayEquals(b, sout.toByteArray());
	}
	*/

}