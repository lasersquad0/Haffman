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
	private final String NO_FILES_TO_COMPRESS = "There are no files to compress. Exiting...";

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

		Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
			String[] args = {};
			ar.compressFiles(args);
		});

		assertEquals(NO_FILES_TO_COMPRESS, thrown.getMessage());
	}

	@Test
	public void compressFile2()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
			String[] args = {"1"};
			ar.compressFiles(args);
		});

		assertEquals(NO_FILES_TO_COMPRESS, thrown.getMessage());
	}
/*
	@Test
	public void compressFile2_2() throws IOException
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			String[] args = {"1", "1"};
			ar.compressFile2(args);
		});

		assertEquals("1 (The system cannot find the file specified)", thrown.getMessage());
	}*/

	@Test
	public void compressFile3()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
			String[] args = {"1.txt"};
			ar.compressFiles(args);
		});

		assertEquals(NO_FILES_TO_COMPRESS, thrown.getMessage());
	}
/*
	@Test
	public void compressFile3_3()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			String[] args = {"1.txt", "1.txt"};
			ar.compressFile2(args);
		});

		assertEquals("1.txt (The system cannot find the file specified)", thrown.getMessage());
	}
*/
	@Test
	public void compressFile4()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
			String[] args = {};
			ar.compressFiles(args);
		});

		assertEquals(NO_FILES_TO_COMPRESS, thrown.getMessage());
	}

	@Test
	public void compressFile5()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
			String[] args = {"1.txt", "4"};
			ar.compressFiles(args);
		});

		assertEquals(NO_FILES_TO_COMPRESS, thrown.getMessage());
	}

	@Test
	public void compressFile6()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(IllegalArgumentException.class, () -> {
			String[] args = {"1.txt", "5.hf"};
			ar.compressFiles(args);
		});

		assertEquals(NO_FILES_TO_COMPRESS, thrown.getMessage());
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
		in1.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "д";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 1236;
		fd.files.add(fr);

		fd.saveHeader(out);

		CompressData cData = new CompressData(in2, out, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertEquals(156, s.length());
		assertEquals(56, fr.compressedSize);
		assertEquals("д", fr.fileName);
		assertEquals(109, out.size());
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
		in1.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "]";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(out);

		CompressData cData = new CompressData(in2, out, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertEquals(1, s.length());
		assertEquals(1, fr.compressedSize);
		assertEquals("]", fr.fileName);
		assertEquals(57, out.size());
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "m";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		CompressData cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();
		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(157, s.length());
		assertEquals(56, fr.compressedSize);
		assertEquals(153, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals("m", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "z";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(155, s.length());
		assertEquals(55, fr.compressedSize);
		assertEquals(153, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals("z", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "1";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(109, s.length());
		assertEquals(14, fr.compressedSize);
		assertEquals(77, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals("1", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "1";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(137, s.length());
		assertEquals(18, fr.compressedSize);
		assertEquals(87, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals("1", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "43";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(135, s.length());
		assertEquals(29, fr.compressedSize);
		assertEquals(107, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals("43", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(160, s.length());
		assertEquals(20, fr.compressedSize);
		assertEquals(517, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals("1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "1234567890";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();
		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(172, s.length());
		assertEquals(119, fr.compressedSize);
		assertEquals(451, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals("1234567890", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = ",";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(1, s.length());
		assertEquals(1, fr.compressedSize);
		assertEquals(65, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals(",", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "ячпапоукатлоптклпитедплптмт апткплтпл клпотклопекпо какку";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(2, s.length());
		assertEquals(1, fr.compressedSize);
		assertEquals(177, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals("ячпапоукатлоптклпитедплптмт апткплтпл клпотклопекпо какку", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "яч";
		fr.fileSize = s.length();
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize,tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(2, s.length());
		assertEquals(1, fr.compressedSize);
		assertEquals(73, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.length(), out3.size());
		assertEquals(s.length(), fr.fileSize);
		assertEquals("яч", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "яч";
		fr.fileSize = s.getBytes().length;
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(303, s.length());
		assertEquals(447, s.getBytes().length);
		assertEquals(215, fr.compressedSize);
		assertEquals(855, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.getBytes().length, out3.size());
		//assertEquals(s.length(), out3.size());
		assertEquals(s.getBytes().length, fr.fileSize);
		assertEquals("яч", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "яч";
		fr.fileSize = s.getBytes().length;
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		var cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(98, s.length());
		assertEquals(193, s.getBytes().length);
		assertEquals(72, fr.compressedSize);
		assertEquals(375, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(500);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.getBytes().length, out3.size());
		//assertEquals(s.length(), out3.size());
		assertEquals(s.getBytes().length, fr.fileSize);
		assertEquals("яч", fr.fileName);
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
		inForTree.close();

		HFArchiveHeader fd = new HFArchiveHeader();
		HFFileRec fr = new HFFileRec();
		fr.fileName = "яч";
		fr.fileSize = s.getBytes().length;
		fr.CRC32Value = tree.CRC32Value;
		fr.modifiedDate = 123456;
		fd.files.add(fr);

		fd.saveHeader(outForCompress);

		tree.saveTable(outForCompress);

		CompressData cData = new CompressData(inForCompress, outForCompress, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(97, s.length());
		assertEquals(191, s.getBytes().length);
		assertEquals(71, fr.compressedSize);
		assertEquals(369, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		var uc = new HFCompressor();

		fd.loadHeader(in3);

		tree = new HFTree();
		tree.loadTable(in3);

		var uData = new CompressData(in3, out3, fr.compressedSize, fr.fileSize, tree);
		uc.uncompress(uData);

		logger.info(s);
		logger.info(out3.toString());

		assertEquals(s.getBytes().length, out3.size());
		//assertEquals(s.length(), out3.size());
		assertEquals(s.getBytes().length, fr.fileSize);
		assertEquals("яч", fr.fileName);
		assertArrayEquals(fv, fd.fileVersion);
		assertArrayEquals(fs, fd.fileSignature);
		assertEquals(s, out3.toString());
	}
}
