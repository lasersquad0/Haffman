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

		HFTree tree = new HFTree();
		tree.buildFromStream(in1);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "д";
		fd.sizeUncompressed = fd.fnameUncompressed.length();
		fd.fnameCompressed = "r";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(out);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(in2, out);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(156, s.length());
		assertEquals(56, fd.sizeCompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(in1);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "]";
		fd.sizeUncompressed = fd.fnameUncompressed.length();
		fd.fnameCompressed = "[";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(out);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(in2, out);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(1, s.length());
		assertEquals(1, fd.sizeCompressed);
		assertEquals("[", fd.fnameCompressed);
		assertEquals("]", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "m";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "e";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(157, s.length());
		assertEquals(56, fd.sizeCompressed);
		assertEquals(135, outForCompress.size());
		assertEquals(28, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("e", fd.fnameCompressed);
		assertEquals("m", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "z";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "/";
		fd.CRC32Value = tree.CRC32Value;

		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(155, s.length());
		assertEquals(55, fd.sizeCompressed);
		assertEquals(135, outForCompress.size());
		assertEquals(22, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("/", fd.fnameCompressed);
		assertEquals("z", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "1";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "2";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;


		assertEquals(109, s.length());
		assertEquals(14, fd.sizeCompressed);
		assertEquals(59, outForCompress.size());
		assertEquals(13, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);


		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("2", fd.fnameCompressed);
		assertEquals("1", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "1";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "2";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(137, s.length());
		assertEquals(18, fd.sizeCompressed);
		assertEquals(69, outForCompress.size());
		assertEquals(9, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);


		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("2", fd.fnameCompressed);
		assertEquals("1", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "43";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "22";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(135, s.length());
		assertEquals(29, fd.sizeCompressed);
		assertEquals(89, outForCompress.size());
		assertEquals(1, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);


		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("22", fd.fnameCompressed);
		assertEquals("43", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "яч";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;


		assertEquals(160, s.length());
		assertEquals(20, fd.sizeCompressed);
		assertEquals(71, outForCompress.size());
		assertEquals(32, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm", fd.fnameCompressed);
		assertEquals("яч", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "1234567890";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "0987654321";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;


		assertEquals(172, s.length());
		assertEquals(119, fd.sizeCompressed);
		assertEquals(433, outForCompress.size());
		assertEquals(20, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);


		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("0987654321", fd.fnameCompressed);
		assertEquals("1234567890", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = ",";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = ",,";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(1, s.length());
		assertEquals(1, fd.sizeCompressed);
		assertEquals(47, outForCompress.size());
		assertEquals(1, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals(",,", fd.fnameCompressed);
		assertEquals(",", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "ячпапоукатлоптклпитедплптмт апткплтпл клпотклопекпо какку";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "купар";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;


		assertEquals(2, s.length());
		assertEquals(1, fd.sizeCompressed);
		assertEquals(159, outForCompress.size());
		assertEquals(2, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("купар", fd.fnameCompressed);
		assertEquals("ячпапоукатлоптклпитедплптмт апткплтпл клпотклопекпо какку", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "яч";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "купаруапукпапа апапап цваукаа укаукаа";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;


		assertEquals(2, s.length());
		assertEquals(1, fd.sizeCompressed);
		assertEquals(55, outForCompress.size());
		assertEquals(2, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("купаруапукпапа апапап цваукаа укаукаа", fd.fnameCompressed);
		assertEquals("яч", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "яч";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "купар";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(303, s.length());
		assertEquals(447, s.getBytes().length);
		assertEquals(215, fd.sizeCompressed);
		assertEquals(837, outForCompress.size());
		assertEquals(20, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.getBytes().length, out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("купар", fd.fnameCompressed);
		assertEquals("яч", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "яч";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "купар";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(98, s.length());
		assertEquals(193, s.getBytes().length);
		assertEquals(72, fd.sizeCompressed);
		assertEquals(357, outForCompress.size());
		assertEquals(28, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.getBytes().length, out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("купар", fd.fnameCompressed);
		assertEquals("яч", fd.fnameUncompressed);
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

		HFTree tree = new HFTree();
		tree.buildFromStream(inForTree);

		HFArchiveHeader fd = new HFArchiveHeader();
		fd.fnameUncompressed = "яч";
		fd.sizeUncompressed = s.length();
		fd.fnameCompressed = "купар";
		fd.CRC32Value = tree.CRC32Value;
		fd.saveHeader(outForCompress);

		HFArchiver ar = new HFArchiver();
		HFCompressData cData = new HFCompressData(inForCompress, outForCompress);

		ar.compressFileInternal(tree, cData);

		fd.sizeCompressed = cData.sizeCompressed;
		fd.lastBits = cData.lastBits;

		assertEquals(97, s.length());
		assertEquals(191, s.getBytes().length);
		assertEquals(71, fd.sizeCompressed);
		assertEquals(351, outForCompress.size());
		assertEquals(18, fd.lastBits);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.sizeCompressed;
		byte tmp2 = fd.lastBits;
		fd.loadHeader(in3);
		fd.sizeCompressed = tmp1;
		fd.lastBits = tmp2;

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new HFUncompressData(in3, out3, fd.sizeCompressed, fd.lastBits);
		uc.uncompress(tree, uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.getBytes().length, out3.size());
		assertEquals(s.length(), fd.sizeUncompressed);
		assertEquals("купар", fd.fnameCompressed);
		assertEquals("яч", fd.fnameUncompressed);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}
}
