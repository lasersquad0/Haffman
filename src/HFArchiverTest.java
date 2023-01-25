import org.junit.Test;

import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class HFArchiverTest {

	private static final byte[] fs = new byte[]{'R', 'O', 'M', 'A'};
	private static Logger logger;
	private final byte[] fv = new byte[]{'0', '1'};

	public HFArchiverTest() throws IOException
	{
		if(logger == null)
		{
			// Настраиваем логгер что бы тесты писались в другой файл. Одной конфигураций в JUL такое настроить нельзя. Поэтому настраиваем кодом.
			logger = Logger.getLogger(getClass().getName());
			logger.setLevel(Level.ALL);
			FileHandler fh = new FileHandler("huffmantests%g.log", 1_000_000, 10, true);
			fh.setLevel(Level.ALL);
			logger.addHandler(fh);
			logger.fine("HFArchiveTest logger is initialised.");
		}
	}

	@Test
	public void compressFile1()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("");
		});

		assertEquals(" (The system cannot find the path specified)", thrown.getMessage());
	}

	@Test
	public void compressFile2()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("1");
		});

		assertEquals("1 (The system cannot find the file specified)", thrown.getMessage());
	}

	@Test
	public void compressFile3()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("1.txt");
		});

		assertEquals("1.txt (The system cannot find the file specified)", thrown.getMessage());
	}

	@Test
	public void compressFile4()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("");
		});

		assertEquals(" (The system cannot find the path specified)", thrown.getMessage());
	}

	@Test
	public void compressFile5()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("4");
		});

		assertEquals("4 (The system cannot find the file specified)", thrown.getMessage());
	}

	@Test
	public void compressFile6()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("5.hf");
		});

		assertEquals("5.hf (The system cannot find the file specified)", thrown.getMessage());
	}

	@Test
	public void compressFile7() throws IOException
	{
		String s = "iiiiiiiiiiiiiiiiiiiiiiffffffffffffffffffftttttttttttttttttttttt5555555555555555555555555yyyyyyyyyyyyyyyyyyyyyooooooooooooooooooooooooooddddddddddddddddddddd";
		ByteArrayInputStream in1 = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream in2 = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sout = out;
		fd.sin = in2;
		fd.fnUncompressed = "д";
		fd.fzUncompressed = fd.fnUncompressed.length();
		fd.fnCompressed = "r";
		ar.compressFileInternal(in1, fd);

		assertEquals(156, s.length());
		assertEquals(56, fd.fzCompressed);
		assertEquals(135, out.size());
		assertEquals(26, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

	}

	@Test
	public void compressFile8() throws IOException
	{
		String s = "i";
		ByteArrayInputStream in1 = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream in2 = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sout = out;
		fd.sin = in2;
		fd.fnUncompressed = "]";
		fd.fzUncompressed = fd.fnUncompressed.length();
		fd.fnCompressed = "[";
		ar.compressFileInternal(in1, fd);

		assertEquals(1, s.length());
		assertEquals(1, fd.fzCompressed);
		assertEquals("[", fd.fnCompressed);
		assertEquals("]", fd.fnUncompressed);
		assertEquals(47, out.size());
		assertEquals(1, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

	}


	@Test
	public void compressUncompress1() throws IOException
	{
		String s = "iiiiiiiiiiiiiiiiiiiii5ffffffffffffffffffftttttttttttttttttttttti555555555555555555555555iyyyyyyyyyyyyyyyyyyyyy5oooooooooooooooooooooooo5ddddddddddddddddddddi";
		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = "m";
		fd.fnCompressed = "e";
		ar.compressFileInternal(inForTree, fd);

		assertEquals(157, s.length());
		assertEquals(56, fd.fzCompressed);
		assertEquals(135, outForCompress.size());
		assertEquals(28, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals("e", fd.fnCompressed);
		assertEquals("m", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress1_1() throws IOException
	{
		String s = "iiiiiiiiiiiiiiiiiiii5ffffffffffffffffffftttttttttttttttttttttti555555555555555555555555iyyyyyyyyyyyyyyyyyyyyy5ooooooooooooooooooooooo5ddddddddddddddddddddi";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = "z";
		fd.fnCompressed = "/";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(155, s.length());
		assertEquals(55, fd.fzCompressed);
		assertEquals(135, outForCompress.size());
		assertEquals(22, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals("/", fd.fnCompressed);
		assertEquals("z", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress2() throws IOException
	{
		String s = "UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU";
		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = "1";
		fd.fnCompressed = "2";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(109, s.length());
		assertEquals(14, fd.fzCompressed);
		assertEquals(59, outForCompress.size());
		assertEquals(13, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);


		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals("2", fd.fnCompressed);
		assertEquals("1", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress3() throws IOException
	{
		String s = "XTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = "1";
		fd.fnCompressed = "2";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(137, s.length());
		assertEquals(18, fd.fzCompressed);
		assertEquals(69, outForCompress.size());
		assertEquals(9, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);


		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals("2", fd.fnCompressed);
		assertEquals("1", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress4() throws IOException
	{
		String s = "123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = "1";
		fd.fnCompressed = "2";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(135, s.length());
		assertEquals(29, fd.fzCompressed);
		assertEquals(87, outForCompress.size());
		assertEquals(1, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);


		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals("2", fd.fnCompressed);
		assertEquals("1", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress5() throws IOException
	{
		String s = "[][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][]";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = "1111";
		fd.fnCompressed = "2222";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(160, s.length());
		assertEquals(20, fd.fzCompressed);
		assertEquals(75, outForCompress.size());
		assertEquals(32, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals("2222", fd.fnCompressed);
		assertEquals("1111", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress6() throws IOException
	{
		String s = "1234567890qwertyuiopasdfghjklzxcvbnm,./;'[]1234567890qwertyuiopasdfghjklzxcvbnm,./;'[]1234567890qwertyuiopasdfghjklzxcvbnm,./;'[]1234567890qwertyuiopasdfghjklzxcvbnm,./;'[]";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = "1234567890";
		fd.fnCompressed = "0987654321";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(172, s.length());
		assertEquals(119, fd.fzCompressed);
		assertEquals(433, outForCompress.size());
		assertEquals(20, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);


		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals("0987654321", fd.fnCompressed);
		assertEquals("1234567890", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress7() throws IOException
	{
		String s = ",";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = ",";
		fd.fnCompressed = ",,";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(1, s.length());
		assertEquals(1, fd.fzCompressed);
		assertEquals(47, outForCompress.size());
		assertEquals(1, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals(",,", fd.fnCompressed);
		assertEquals(",", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress8() throws IOException
	{
		String s = ",,";

		compressUncompressString(s);
	}

	private void compressUncompressString(String s) throws IOException
	{
		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = ",";
		fd.fnCompressed = ",,";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(2, s.length());
		assertEquals(1, fd.fzCompressed);
		assertEquals(47, outForCompress.size());
		assertEquals(2, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals(",,", fd.fnCompressed);
		assertEquals(",", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress9() throws IOException
	{
		String s = ",.";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = ",";
		fd.fnCompressed = ",,";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(2, s.length());
		assertEquals(1, fd.fzCompressed);
		assertEquals(53, outForCompress.size());
		assertEquals(2, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals(",,", fd.fnCompressed);
		assertEquals(",", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress10() throws IOException
	{
		String s = "`1234567890-=][poiuytrewq  ASDFGHJKL;'/.,MNBVCXZё!№;%:?*()_+ъхХЪзщшгнекуцйфывапролджэ.юбьтимсчяБЮ,Ээээээээээээээээээээээээээээээээээээээээээээээээээээээээээээ" +
				"эээээээээээээээээээээээээээээээээээээээээээээ5555555555555555555555555555555555555555555555555555555555555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = ",";
		fd.fnCompressed = ",,";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(303, s.length());
		assertEquals(447, s.getBytes().length);
		assertEquals(215, fd.fzCompressed);
		assertEquals(835, outForCompress.size());
		assertEquals(20, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.getBytes().length, out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals(",,", fd.fnCompressed);
		assertEquals(",", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress11() throws IOException
	{
		String s = "xхХЪзщшгнекуцйфывапролджэ.юбьтимсчяБЮ,Ээээээээээээээээээээээээээээээээээээээээээээээээээээээээээээ";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = ",";
		fd.fnCompressed = ",,";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(98, s.length());
		assertEquals(193, s.getBytes().length);
		assertEquals(72, fd.fzCompressed);
		assertEquals(355, outForCompress.size());
		assertEquals(28, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.getBytes().length, out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals(",,", fd.fnCompressed);
		assertEquals(",", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}

	@Test
	public void compressUncompress12() throws IOException
	{
		String s = "xХЪзщшгнекуцйфывапролджэ.юбьтимсчяБЮ,Ээээээээээээээээээээээээээээээээээээээээээээээээээээээээээээ";

		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fzUncompressed = s.length();
		fd.fnUncompressed = ",";
		fd.fnCompressed = ",,";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(97, s.length());
		assertEquals(191, s.getBytes().length);
		assertEquals(71, fd.fzCompressed);
		assertEquals(349, outForCompress.size());
		assertEquals(18, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.getBytes().length, out3.size());
		assertEquals(s.length(), fd.fzUncompressed);
		assertEquals(",,", fd.fnCompressed);
		assertEquals(",", fd.fnUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}
}
